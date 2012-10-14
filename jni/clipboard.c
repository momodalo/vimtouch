
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
    android_clip_set_selection(cbd); 
}

void clip_mch_request_selection(VimClipboard *cbd)
{
    android_clip_request_selection(cbd); 
}
