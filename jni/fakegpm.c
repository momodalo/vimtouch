
#include "gpm.h"
#include <unistd.h>
#include <fcntl.h>
#include <vim.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <string.h>
#include <sys/un.h>
#include "common.h"


#define LOCAL_SOCKET_SERVER_NAME "/tmp/vimtouch"

int gpm_flag = 0;
int gpm_fd = 0;

char* gpm_socket_name = NULL;

int Gpm_Open(Gpm_Connect *c, int a){
    /*
    gpm_fd = fake_gpm_fd[0];
    */
    int sk, result;
    int count = 1;
    int err;

    char *buffer = malloc(8);

    int i;
    for(i = 0; i<8; i++){
        buffer[i] = (i+1);
    }

    struct sockaddr_un addr;
    socklen_t len;
    addr.sun_family = AF_LOCAL;
    /* use abstract namespace for socket path */
    addr.sun_path[0] = '\0';
    sprintf(&addr.sun_path[1],"%s/%s", LOCAL_SOCKET_SERVER_NAME,gpm_socket_name);
    len = offsetof(struct sockaddr_un, sun_path) + 1 + strlen(&addr.sun_path[1]);

    sk = socket(PF_LOCAL, SOCK_STREAM, TCP_NODELAY);
    if (sk < 0) {
        err = errno;
        errno = err;
        return;
    }

    if (connect(sk, (struct sockaddr *) &addr, len) < 0) {
        err = errno;
        close(sk);
        errno = err;
        return;
    }

    gpm_fd = sk;

    fcntl(gpm_fd , F_SETFL, O_NONBLOCK);
    gpm_flag = 1;
    return 1;
}

int Gpm_Close(){
    return 0;
}

int Gpm_GetEvent(Gpm_Event *e){
    VimEvent event;

    int n = read(gpm_fd, (void*)&event, sizeof(VimEvent));
    if(event.type == VIM_EVENT_TYPE_GPM) {
        memcpy(e, (void*)&event.event.gpm, sizeof(Gpm_Event));
        return 1;
    }else if(event.type == VIM_EVENT_TYPE_CMD) {
        do_cmdline_cmd((char_u *) event.event.cmd);
    }else if(event.type == VIM_EVENT_TYPE_RELINE) {
        char* str = event.event.cmd;
        ml_replace(curwin->w_cursor.lnum,(char_u*)str, TRUE);
        changed_lines(curwin->w_cursor.lnum, 0, curwin->w_cursor.lnum, 1L);
    }else if(event.type == VIM_EVENT_TYPE_UPDATE){
        //vimtouch_lock();
        update_screen(0);
        setcursor();
        out_flush();
        //vimtouch_unlock();
    }else if(event.type == VIM_EVENT_TYPE_CURSOR){
        mouse_col = event.event.nums[0];
        mouse_row = event.event.nums[1];

        jump_to_mouse(MOUSE_DID_MOVE, NULL, 0);
    }else if(event.type == VIM_EVENT_TYPE_SETCOL){
        curwin->w_cursor.col = event.event.num;
    }else if(event.type == VIM_EVENT_TYPE_SCROLL){
        int do_scroll = event.event.num;
        scroll_redraw(do_scroll > 0, do_scroll>0?do_scroll:-do_scroll);
    }else if(event.type == VIM_EVENT_TYPE_RESIZE){
        out_flush();
        shell_resized_check();
        redraw_later(CLEAR);
        out_flush();
    }else if(event.type == VIM_EVENT_TYPE_SETTAB){
        int nr = event.event.nums[0];

        if (nr != tabpage_index(curtab)){
            current_tab = nr;
            if (current_tab == 255)     /* -1 in a byte gives 255 */
            current_tab = -1;
            goto_tabpage(current_tab);
            update_screen(CLEAR);
            out_flush();
        }
    }else if(event.type == VIM_EVENT_TYPE_HISTORY){
        char buf[1024];
        int i = 1;
        for(i = 1; i <= 10; i++){
            sprintf(buf, "HISTORY:%d,%s\n",i-1,get_history_entry(HIST_CMD,i));
            write(gpm_fd,buf,strlen(buf));
        }
    }
    vimtouch_sync();
    return 0;
}

int synced_state = -1;
int synced_col = -1;
int synced_lnum = -1;

void vimtouch_sync() {
    if( synced_state != State || 
        synced_col != curwin->w_cursor.col ||
        synced_lnum != curwin->w_cursor.lnum ){
        char buf[128];

        synced_state = State;
        synced_col = curwin->w_cursor.col;
        synced_lnum = curwin->w_cursor.lnum;

        sprintf(buf, "SYNC   :%d,%d,%d", synced_state, synced_col, synced_lnum);
        write(gpm_fd, buf, strlen(buf));
    }
}

int vimtouch_eventloop( int type, VimEvent* event) {
    int state = -1;

    while(state < 0) {
        usleep(100000);
        int n = read(gpm_fd, (void*)event, sizeof(VimEvent));
        if(n > 0 && (type < 0 || type == event->type)){
            return n;
        }
    }
}

int vimtouch_dialog_result(){
    VimEvent event;
    int n = -1;
    n = vimtouch_eventloop(VIM_EVENT_TYPE_DIALOG, &event);
    if(n > 0)
        return event.event.num;
}

void vimtouch_get_clipboard( VimClipboard *cbd ){
    VimEvent event;
    int n = -1;

    write(gpm_fd, "GETCLIP:", 8);

    n = vimtouch_eventloop(VIM_EVENT_TYPE_CLIPBOARD, &event);
    if(n > 0){
        char* clip_text = event.event.cmd;
        int clip_length = strlen(clip_text);
        if(clip_text && clip_length > 0){
            clip_yank_selection( MLINE, (unsigned char*)clip_text, clip_length, cbd );
        }
    }
}

void vimtouch_set_clipboard(VimClipboard *cbd) {
    long_u  len;
    char_u *str = NULL;

    /* Prevent recursion from clip_get_selection() */
    if (cbd->owned == TRUE)
        return;

    cbd->owned = TRUE;
    clip_get_selection(cbd);
    cbd->owned = FALSE;

    int type = clip_convert_selection(&str, &len, cbd);

    if (type >= 0 && str)
    {
        str[len] = '\0';
        char buf[8192];
        sprintf(buf, "SETCLIP:%s",str);
        write(gpm_fd, buf, strlen(buf));
    }

    vim_free(str);
}

void vimtouch_set_curtab(int nr){
    char buf[1024];
    sprintf(buf, "SETCTAB:%d",nr);
    write(gpm_fd, buf, strlen(buf));
}

void vimtouch_set_tab_labels(u_char** labels, int num){
    char buf[8192];
    char* ptr;
    int i = 0;
    char* c;

    sprintf(buf, "SETLBLS:");
    ptr = buf;
    for (i = 0; i < num; i++){
        c = labels[i];
        while(c && *c){
            if(*c == ',') *c = '_';
            c++;
        }
        ptr = strcat(ptr, labels[i]); 
        if(i < num-1)ptr = strcat(ptr, ","); 
    }
    write(gpm_fd, buf, strlen(buf));
}

void vimtouch_show_tab(int showit){
    char buf[1024];
    sprintf(buf, "SHOWTAB:%d",showit);
    write(gpm_fd, buf, strlen(buf));
}

void vimtouch_show_dialog(
	int	type,
	char_u	*title,
	char_u	*message,
	char_u	*buttons,
	int	default_button,
	char_u	*textfield){
    char buf[8192];
    sprintf(buf, "SDIALOG:%d,%s,%s,%s,%d,%s",type, title, message, buttons, default_button, textfield);
    write(gpm_fd, buf, strlen(buf));
}

