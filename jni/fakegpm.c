
#include "gpm.h"
#include <unistd.h>
#include <fcntl.h>
#include <vim.h>

int gpm_flag = 0;
int gpm_fd = 0;

int fake_gpm_fd[2] = {-1,-1};

int Gpm_Open(Gpm_Connect *c, int a){
    gpm_fd = fake_gpm_fd[0];
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
        vimtouch_lock();
        update_screen(0);
        setcursor();
        out_flush();
        vimtouch_unlock();
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
    }
    return 0;
}
