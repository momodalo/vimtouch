#include "vim.h"
#include <gui.h>
#define LOG_TAG "GUI_VIM"
#include <common.h>

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
    return state;
}
#endif

#if defined( FEAT_GUI_TABLINE )

int gui_tab_showing = 1;

    void
gui_mch_show_tabline(int showit)
{
    gui_tab_showing = showit;
    vimtouch_Exec_showTab(showit);
}

    int
gui_mch_showing_tabline(void)
{
    return gui_tab_showing;
}

static u_char **tabLabels = NULL;
static int tabLabelsSize = 0;

    static int
getTabCount(void)
{
    tabpage_T	*tp;
    int		numTabs = 0;

    for (tp = first_tabpage; tp != NULL; tp = tp->tp_next)
	++numTabs;
    return numTabs;
}

    void
gui_mch_update_tabline(void)
{
    tabpage_T	*tp;
    int		numTabs = getTabCount();
    int		nr = 1;
    int		curtabidx = 1;

    // adjust data browser
    if (tabLabels != NULL)
    {
	int i;

	for (i = 0; i < tabLabelsSize; ++i)
	    free(tabLabels[i]);
	free(tabLabels);
    }
    tabLabels = (u_char**)malloc(numTabs * sizeof(u_char*));
    tabLabelsSize = numTabs;

    for (tp = first_tabpage; tp != NULL; tp = tp->tp_next, ++nr)
    {
	if (tp == curtab)
	    curtabidx = nr;
    get_tabline_label(tp, FALSE);
	tabLabels[nr-1] = strdup(NameBuff);
    }

    vimtouch_Exec_setTabLabels(tabLabels, numTabs);
    vimtouch_Exec_setCurTab(curtabidx-1);
}

    void
gui_mch_set_curtab(int nr)
{
    vimtouch_Exec_setCurTab(nr-1);
}

/*
 * Return TRUE if the GUI is taking care of the tabline.
 * It may still be hidden if 'showtabline' is zero.
 */
    int
gui_use_tabline()
{
    return TRUE;
}

/*
 * Return TRUE if the GUI is showing the tabline.
 * This uses 'showtabline'.
 */
    static int
gui_has_tabline()
{
    if (!gui_use_tabline()
        || p_stal == 0
        || (p_stal == 1 && first_tabpage->tp_next == NULL))
        return FALSE;
     return TRUE;
}


/*
 * Update the tabline.
 * This may display/undisplay the tabline and update the labels.
 */
    void
gui_update_tabline()
{
    int	    showit = gui_has_tabline();
    int	    shown = gui_mch_showing_tabline();

	/* Updating the tabline uses direct GUI commands, flush
	 * outstanding instructions first. (esp. clear screen) */
	out_flush();

	if (!showit != !shown)
	    gui_mch_show_tabline(showit);
	if (showit != 0)
	    gui_mch_update_tabline();

	/* When the tabs change from hidden to shown or from shown to
	 * hidden the size of the text area should remain the same. */
	//if (!showit != !shown)
	//    set_shellsize(FALSE, showit, RESIZE_VERT);
}

/* FIXME copy from gui.c
 * Get the label or tooltip for tab page "tp" into NameBuff[].
 */
    void
get_tabline_label(tp, tooltip)
    tabpage_T	*tp;
    int		tooltip;	/* TRUE: get tooltip */
{
    int		modified = FALSE;
    char_u	buf[40];
    int		wincount;
    win_T	*wp;
    char_u	**opt;

    /* Use 'guitablabel' or 'guitabtooltip' if it's set. */
    //opt = (tooltip ? &p_gtt : &p_gtl);

    /* If 'guitablabel'/'guitabtooltip' is not set or the result is empty then
     * use a default label. */
	/* Get the buffer name into NameBuff[] and shorten it. */
	get_trans_bufname(tp == curtab ? curbuf : tp->tp_curwin->w_buffer);
	if (!tooltip)
	    shorten_dir(NameBuff);

	wp = (tp == curtab) ? firstwin : tp->tp_firstwin;
	for (wincount = 0; wp != NULL; wp = wp->w_next, ++wincount)
	    if (bufIsChanged(wp->w_buffer))
		modified = TRUE;
	if (modified || wincount > 1)
	{
	    if (wincount > 1)
		vim_snprintf((char *)buf, sizeof(buf), "%d", wincount);
	    else
		buf[0] = NUL;
	    if (modified)
		STRCAT(buf, "+");
	    STRCAT(buf, " ");
	    STRMOVE(NameBuff + STRLEN(buf), NameBuff);
	    mch_memmove(NameBuff, buf, STRLEN(buf));
	}
}

#endif
