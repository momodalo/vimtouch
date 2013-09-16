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

/*
 * Copy from os_unix.c to import mch_exit
 */

static void exit_scroll __ARGS((void));

/*
 * Output a newline when exiting.
 * Make sure the newline goes to the same stream as the text.
 */
    static void
exit_scroll()
{
    if (silent_mode)
	return;
    if (newline_on_exit || msg_didout)
    {
	if (msg_use_printf())
	{
	    if (info_message)
		mch_msg("\n");
	    else
		mch_errmsg("\r\n");
	}
	else
	    out_char('\n');
    }
    else
    {
	restore_cterm_colors();		/* get original colors back */
	msg_clr_eos_force();		/* clear the rest of the display */
	windgoto((int)Rows - 1, 0);	/* may have moved the cursor */
    }
}

    void
android_mch_exit(r)
    int r;
{
    exiting = TRUE;

#if defined(FEAT_X11) && defined(FEAT_CLIPBOARD)
    x11_export_final_selection();
#endif

#ifdef FEAT_GUI
    if (!gui.in_use)
#endif
    {
	settmode(TMODE_COOK);
#ifdef FEAT_TITLE
	mch_restore_title(3);	/* restore xterm title and icon name */
#endif
	/*
	 * When t_ti is not empty but it doesn't cause swapping terminal
	 * pages, need to output a newline when msg_didout is set.  But when
	 * t_ti does swap pages it should not go to the shell page.  Do this
	 * before stoptermcap().
	 */
	if (swapping_screen() && !newline_on_exit)
	    exit_scroll();

	/* Stop termcap: May need to check for T_CRV response, which
	 * requires RAW mode. */
	stoptermcap();

	/*
	 * A newline is only required after a message in the alternate screen.
	 * This is set to TRUE by wait_return().
	 */
	if (!swapping_screen() || newline_on_exit)
	    exit_scroll();

	/* Cursor may have been switched off without calling starttermcap()
	 * when doing "vim -u vimrc" and vimrc contains ":q". */
	if (full_screen)
	    cursor_on();
    }
    out_flush();
    ml_close_all(TRUE);		/* remove all memfiles */
    //may_core_dump();
#ifdef FEAT_GUI
    if (gui.in_use)
	gui_exit(r);
#endif

#ifdef MACOS_CONVERT
    mac_conv_cleanup();
#endif

#ifdef __QNX__
    /* A core dump won't be created if the signal handler
     * doesn't return, so we can't call exit() */
    if (deadly_signal != 0)
	return;
#endif

#ifdef FEAT_NETBEANS_INTG
    netbeans_send_disconnect();
#endif

#ifdef EXITFREE
    free_all_mem();
#endif

}
