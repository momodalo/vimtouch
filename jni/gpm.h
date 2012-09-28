/*
 * gpm.h - public include file for gpm-Linux
 *
 * Copyright 1994   rubini@ipvvis.unipv.it (Alessandro Rubini)
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 ********/

#ifndef _GPM_H_
#define _GPM_H_

/*....................................... Xtermish stuff */
#define GPM_XTERM_ON \
  printf("%c[?1001s",27), fflush(stdout), /* save old hilit tracking */ \
  printf("%c[?1000h",27), fflush(stdout) /* enable mouse tracking */

#define GPM_XTERM_OFF \
  printf("%c[?10001",27), fflush(stdout), /* disable mouse tracking */ \
  printf("%c[?1001r",27), fflush(stdout) /* restore old hilittracking */

/*....................................... Cfg pathnames */

#define GPM_NODE_DIR      "/tmp"
#define GPM_NODE_DIR_MODE 0777
#define GPM_NODE_PID      GPM_NODE_DIR "/gpmpid"
#define GPM_NODE_LOG      GPM_NODE_DIR "/gpmlog"
#define GPM_NODE_CTL      GPM_NODE_DIR "/gpmctl"

/*....................................... Cfg buttons */

#define GPM_B_LEFT      4
#define GPM_B_MIDDLE    2
#define GPM_B_RIGHT     1

/*....................................... The event types */

enum Gpm_Etype {
  GPM_MOVE=1,
  GPM_DRAG=2,   /* exactly one in four is active at a time */
  GPM_DOWN=4,
  GPM_UP=  8,

#define GPM_BARE_EVENTS(ev) ((ev)&0xF)

  GPM_SINGLE=16,            /* at most one in three is set */
  GPM_DOUBLE=32,
  GPM_TRIPLE=64,            /* WARNING: I depend on the values */

  GPM_MFLAG=128,            /* motion during click? */
  GPM_HARD=256             /* if set in the defaultMask, force an already
			       used event to pass over to another handler */
};

#define Gpm_StrictSingle(type) (((type)&GPM_SINGLE) && !((type)&GPM_MFLAG))
#define Gpm_AnySingle(type)     ((type)&GPM_SINGLE)
#define Gpm_StrictDouble(type) (((type)&GPM_DOUBLE) && !((type)&GPM_MFLAG))
#define Gpm_AnyDouble(type)     ((type)&GPM_DOUBLE)
#define Gpm_StrictTriple(type) (((type)&GPM_TRIPLE) && !((type)&GPM_MFLAG))
#define Gpm_AnyTriple(type)     ((type)&GPM_TRIPLE)

/*....................................... The event data structure */

enum Gpm_Margin {GPM_TOP=1, GPM_BOT=2, GPM_LFT=4, GPM_RGT=8};


typedef struct Gpm_Event {
  unsigned char buttons, modifiers;  /* try to be a multiple of 4 */
  unsigned short vc;
  short dx, dy, x, y;
  enum Gpm_Etype type;
  int clicks;
  enum Gpm_Margin margin;
}              Gpm_Event;

/*....................................... The connection data structure */

#define GPM_MAGIC 0x47706D4C /* "GpmL" */
typedef struct Gpm_Connect {
  unsigned short eventMask, defaultMask;
  unsigned short minMod, maxMod;
  int pid;
  int vc;
}              Gpm_Connect;


/*....................................... Global variables for the client */

extern int gpm_flag, gpm_ctlfd, gpm_fd, gpm_hflag, gpm_morekeys;

typedef int Gpm_Handler(Gpm_Event *event, void *clientdata);

extern Gpm_Handler *gpm_handler;
extern void *gpm_data;

extern int gpm_zerobased;
extern int gpm_visiblepointer;
extern int gpm_mx, gpm_my; /* max x and y to fit margins */
extern struct timeval gpm_timeout;

extern unsigned char    _gpm_buf[];
extern unsigned short * _gpm_arg;


/*....................................... Prototypes for the client       */
/*                                          all of them return 0 or errno */

#include <stdio.h>      /* needed to get FILE */
#include <sys/ioctl.h>  /* to get the prototype for ioctl() */

/* liblow.c */
extern int Gpm_Open(Gpm_Connect *, int);
extern int Gpm_Close(void);
extern int Gpm_GetEvent(Gpm_Event *);
extern int Gpm_Getc(FILE *);
#define    Gpm_Getchar() Gpm_Getc(stdin)
extern int Gpm_Repeat(int millisec);
extern int Gpm_FitValuesM(int *x, int *y, int margin);
#define    Gpm_FitValues(x,y) Gpm_FitValuesM((int *)(x),(int *)(y),-1);
#define    Gpm_FitEvent(ePtr) \
                        ((ePtr)->margin \
                         ? Gpm_FitValuesM((int *)(&((ePtr)->x)), \
					  (int *)(&((ePtr)->y)), \
                                (ePtr)->margin) \
                         : 0)

/* the following is a (progn ...) form */

#define Gpm_DrawPointer(x, y, fd) \
                       (_gpm_buf[sizeof(short)-1] = 2, \
                        _gpm_arg[0] = _gpm_arg[2] = \
                                (unsigned short)(x)+gpm_zerobased, \
                        _gpm_arg[1] = _gpm_arg[3] = \
                                (unsigned short)(y)+gpm_zerobased, \
                        _gpm_arg[4] = (unsigned short)3, \
                        ioctl(fd, TIOCLINUX, _gpm_buf+sizeof(short)-1))

/* the following is a heavy thing ... */
extern int gpm_consolefd; /* liblow.c */

/* #define GPM_DRAWPOINTER(event) \
 *                      ((gpm_consolefd=open("/dev/console",O_RDWR))>=0 && \
 *                      Gpm_DrawPointer((event)->x,(event)->y,gpm_consolefd), \
 *                      close(gpm_consolefd))
 */

#define GPM_DRAWPOINTER(ePtr) \
                         (Gpm_DrawPointer((ePtr)->x,(ePtr)->y,gpm_consolefd))



/* libhigh.c */
extern int Gpm_Callback(int eventmask, int (*fun)(Gpm_Event *));


/* libcurses.c */
/* #include <curses.h>  Hmm... seems risky */

extern int Gpm_Wgetch();
#define Gpm_Getch() (Gpm_Wgetch(NULL))

#define VIM_EVENT_TYPE_GPM 0
#define VIM_EVENT_TYPE_CMD 1
#define VIM_EVENT_TYPE_UPDATE 2
#define VIM_EVENT_TYPE_SCROLL 3
#define VIM_EVENT_TYPE_SETCOL 4
#define VIM_EVENT_TYPE_RELINE 5

#include "vim.h"

typedef struct VimEvnet{
    int type;
    union {
        Gpm_Event gpm;
        char cmd[MAXPATHL];
        int num;
    }event;
} VimEvent;

#endif /* _GPM_H_ */



