/* Copyright (C) 2000-2009, 2011 Free Software Foundation, Inc.
   This file is part of the GNU LIBICONV Library.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

#include "vim.h"
#include <gui.h>
#define LOG_TAG "GUI_VIM"
#include <common.h>

extern int gpm_fd;

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
static char_u *msg_show_console_dialog __ARGS((char_u *message, char_u *buttons, int dfltbutton));
static int	confirm_msg_used = FALSE;	/* displaying confirm_msg */
static char_u	*confirm_msg = NULL;		/* ":confirm" message */
static char_u	*confirm_msg_tail;		/* tail of confirm_msg */

    int
gui_mch_dialog(
	int	type,
	char_u	*title,
	char_u	*message,
	char_u	*buttons,
	int	dfltbutton,
	char_u	*textfield)
{
    if(gpm_fd < 0){
        int		oldState;
        int		retval = 0;
        char_u	*hotkeys;
        int		c;
        int		i;

        oldState = State;
        State = CONFIRM;

        /*
         * Since we wait for a keypress, don't make the
         * user press RETURN as well afterwards.
         */
        ++no_wait_return;
        hotkeys = msg_show_console_dialog(message, buttons, dfltbutton);

        if (hotkeys != NULL)
        {
            for (;;)
            {
                /* Get a typed character directly from the user. */
                c = get_keystroke();
                switch (c)
                {
                    case CAR:		/* User accepts default option */
                    case NL:
                        retval = dfltbutton;
                        break;
                    case Ctrl_C:	/* User aborts/cancels */
                    case ESC:
                        retval = 0;
                        break;
                    default:		/* Could be a hotkey? */
                        if (c < 0)	/* special keys are ignored here */
                            continue;
                        /* Make the character lowercase, as chars in "hotkeys" are. */
                        c = MB_TOLOWER(c);
                        retval = 1;
                        for (i = 0; hotkeys[i]; ++i)
                        {
                                if (hotkeys[i] == c)
                                    break;
                            ++retval;
                        }
                        if (hotkeys[i])
                            break;
                        /* No hotkey match, so keep waiting */
                        continue;
                }
                break;
            }

            vim_free(hotkeys);
        }

        State = oldState;
        --no_wait_return;
        msg_end_prompt();


        return retval;
    }

    if(title == NULL){
        title = "VimTouch";
    }
    vimtouch_show_dialog(type,title, message, buttons, dfltbutton, textfield);
    return vimtouch_dialog_result();
}

    void
msg_end_prompt()
{
    need_wait_return = FALSE;
    emsg_on_display = FALSE;
    cmdline_row = msg_row;
    msg_col = 0;
    msg_clr_eos();
}

static int copy_char __ARGS((char_u *from, char_u *to, int lowercase));

/*
 * Copy one character from "*from" to "*to", taking care of multi-byte
 * characters.  Return the length of the character in bytes.
 */
    static int
copy_char(from, to, lowercase)
    char_u	*from;
    char_u	*to;
    int		lowercase;	/* make character lower case */
{
    {
	if (lowercase)
	    *to = (char_u)TOLOWER_LOC(*from);
	else
	    *to = *from;
	return 1;
    }
}

    void
display_confirm_msg()
{
    /* avoid that 'q' at the more prompt truncates the message here */
    ++confirm_msg_used;
    if (confirm_msg != NULL)
	msg_puts_attr(confirm_msg, hl_attr(HLF_M));
    --confirm_msg_used;
}

    static char_u *
msg_show_console_dialog(message, buttons, dfltbutton)
    char_u	*message;
    char_u	*buttons;
    int		dfltbutton;
{
    int		len = 0;
#ifdef FEAT_MBYTE
# define HOTK_LEN (has_mbyte ? MB_MAXBYTES : 1)
#else
# define HOTK_LEN 1
#endif
    int		lenhotkey = HOTK_LEN;	/* count first button */
    char_u	*hotk = NULL;
    char_u	*msgp = NULL;
    char_u	*hotkp = NULL;
    char_u	*r;
    int		copy;
#define HAS_HOTKEY_LEN 30
    char_u	has_hotkey[HAS_HOTKEY_LEN];
    int		first_hotkey = FALSE;	/* first char of button is hotkey */
    int		idx;

    has_hotkey[0] = FALSE;

    /*
     * First loop: compute the size of memory to allocate.
     * Second loop: copy to the allocated memory.
     */
    for (copy = 0; copy <= 1; ++copy)
    {
	r = buttons;
	idx = 0;
	while (*r)
	{
	    if (*r == DLG_BUTTON_SEP)
	    {
		if (copy)
		{
		    *msgp++ = ',';
		    *msgp++ = ' ';	    /* '\n' -> ', ' */

		    /* advance to next hotkey and set default hotkey */
#ifdef FEAT_MBYTE
		    if (has_mbyte)
			hotkp += STRLEN(hotkp);
		    else
#endif
			++hotkp;
		    hotkp[copy_char(r + 1, hotkp, TRUE)] = NUL;
		    if (dfltbutton)
			--dfltbutton;

		    /* If no hotkey is specified first char is used. */
		    if (idx < HAS_HOTKEY_LEN - 1 && !has_hotkey[++idx])
			first_hotkey = TRUE;
		}
		else
		{
		    len += 3;		    /* '\n' -> ', '; 'x' -> '(x)' */
		    lenhotkey += HOTK_LEN;  /* each button needs a hotkey */
		    if (idx < HAS_HOTKEY_LEN - 1)
			has_hotkey[++idx] = FALSE;
		}
	    }
	    else if (*r == DLG_HOTKEY_CHAR || first_hotkey)
	    {
		if (*r == DLG_HOTKEY_CHAR)
		    ++r;
		first_hotkey = FALSE;
		if (copy)
		{
		    if (*r == DLG_HOTKEY_CHAR)		/* '&&a' -> '&a' */
			*msgp++ = *r;
		    else
		    {
			/* '&a' -> '[a]' */
			*msgp++ = (dfltbutton == 1) ? '[' : '(';
			msgp += copy_char(r, msgp, FALSE);
			*msgp++ = (dfltbutton == 1) ? ']' : ')';

			/* redefine hotkey */
			hotkp[copy_char(r, hotkp, TRUE)] = NUL;
		    }
		}
		else
		{
		    ++len;	    /* '&a' -> '[a]' */
		    if (idx < HAS_HOTKEY_LEN - 1)
			has_hotkey[idx] = TRUE;
		}
	    }
	    else
	    {
		/* everything else copy literally */
		if (copy)
		    msgp += copy_char(r, msgp, FALSE);
	    }

	    /* advance to the next character */
	    mb_ptr_adv(r);
	}

	if (copy)
	{
	    *msgp++ = ':';
	    *msgp++ = ' ';
	    *msgp = NUL;
	}
	else
	{
	    len += (int)(STRLEN(message)
			+ 2			/* for the NL's */
			+ STRLEN(buttons)
			+ 3);			/* for the ": " and NUL */
	    lenhotkey++;			/* for the NUL */

	    /* If no hotkey is specified first char is used. */
	    if (!has_hotkey[0])
	    {
		first_hotkey = TRUE;
		len += 2;		/* "x" -> "[x]" */
	    }

	    /*
	     * Now allocate and load the strings
	     */
	    vim_free(confirm_msg);
	    confirm_msg = alloc(len);
	    if (confirm_msg == NULL)
		return NULL;
	    *confirm_msg = NUL;
	    hotk = alloc(lenhotkey);
	    if (hotk == NULL)
		return NULL;

	    *confirm_msg = '\n';
	    STRCPY(confirm_msg + 1, message);

	    msgp = confirm_msg + 1 + STRLEN(message);
	    hotkp = hotk;

	    /* Define first default hotkey.  Keep the hotkey string NUL
	     * terminated to avoid reading past the end. */
	    hotkp[copy_char(buttons, hotkp, TRUE)] = NUL;

	    /* Remember where the choices start, displaying starts here when
	     * "hotkp" typed at the more prompt. */
	    confirm_msg_tail = msgp;
	    *msgp++ = '\n';
	}
    }

    display_confirm_msg();
    return hotk;
}
#endif

#if defined( FEAT_GUI_TABLINE )

int gui_tab_showing = 1;

    void
gui_mch_show_tabline(int showit)
{
    gui_tab_showing = showit;
    vimtouch_show_tab(showit);
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

    vimtouch_set_tab_labels(tabLabels, numTabs);
    vimtouch_set_curtab(curtabidx-1);
}

    void
gui_mch_set_curtab(int nr)
{
    vimtouch_set_curtab(nr-1);
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
