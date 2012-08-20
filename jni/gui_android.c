#include "vim.h"
#include <gui.h>
#include <common.h>

#define LOG_TAG "GUI_VIM"

#if defined( FEAT_BROWSE ) || defined( PROTO )
/*
 * Put up a file requester.
 * Returns the selected name in allocated memory, or NULL for Cancel.
 * saving,	    select file to write
 * title	    title for the window
 * default_name	    default name (well duh!)
 * ext		    not used (extension added)
 * initdir	    initial directory, NULL for current dir
 * filter	    not used (file name filter)
 */
    char_u *
gui_mch_browse(
	int saving,
	char_u *title,
	char_u *default_name,
	char_u *ext,
	char_u *initdir,
	char_u *filter)
{
}
#endif

#if defined( FEAT_GUI_DIALOG ) || defined( PROTO )
int vimtouch_Exec_getDialogState();
void vimtouch_Exec_showDialog(
	int	type,
	char_u	*title,
	char_u	*message,
	char_u	*buttons,
	int	default_button,
	char_u	*textfield);

    int
gui_mch_dialog(
	int	type,
	char_u	*title,
	char_u	*message,
	char_u	*buttons,
	int	default_button,
	char_u	*textfield)
{
    if(title == NULL){
        title = "VimTouch";
    }
    vimtouch_Exec_showDialog(type,title, message, buttons, default_button, textfield);
    int state = vimtouch_Exec_getDialogState();
    while(state < 0) {
        usleep(100000);
        state = vimtouch_Exec_getDialogState();
    }
    return state+1;
}
#endif
