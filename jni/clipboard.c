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

#include "common.h"
#include "vim.h"

int clip_mch_own_selection(VimClipboard *cbd)
{
    return FALSE;
}

void clip_mch_lose_selection(VimClipboard *cbd)
{
}

void clip_mch_set_selection(VimClipboard *cbd)
{
    vimtouch_set_clipboard(cbd); 
}

void clip_mch_request_selection(VimClipboard *cbd)
{
    vimtouch_get_clipboard(cbd); 
}
