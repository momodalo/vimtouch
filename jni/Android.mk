#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This makefile supplies the rules for building a library of JNI code for
# use by our example of how to bundle a shared library with an APK.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

# This is the target being built.
LOCAL_MODULE:= libvimtouch

$(shell if [ ! -f $(LOCAL_PATH)/libiconv/include/iconv.h ]; then cp $(LOCAL_PATH)/iconv_h/include/iconv.h $(LOCAL_PATH)/libiconv/include/iconv.h; fi )
$(shell if [ ! -f $(LOCAL_PATH)/libiconv/lib/config.h ]; then cp $(LOCAL_PATH)/iconv_h/lib/config.h $(LOCAL_PATH)/libiconv/lib/config.h; fi )
$(shell if [ ! -f $(LOCAL_PATH)/libiconv/libcharset/config.h ]; then cp $(LOCAL_PATH)/iconv_h/libcharset/config.h $(LOCAL_PATH)/libiconv/libcharset/config.h; fi )
$(shell if [ ! -f $(LOCAL_PATH)/libiconv/libcharset/include/localcharset.h ]; then cp $(LOCAL_PATH)/iconv_h/libcharset/include/localcharset.h $(LOCAL_PATH)/libiconv/libcharset/include/localcharset.h; fi )


# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
  termExec.cpp \
  android_main.c\
  fakegpm.c\
  gui_android.c\
  clipboard.c\
  exit.c\
  misc.c\
  libiconv/lib/iconv.c \
  libiconv/libcharset/lib/localcharset.c \
  libiconv/lib/relocatable.c \
  vim/src/buffer.c\
  vim/src/blowfish.c\
  vim/src/charset.c\
  vim/src/diff.c\
  vim/src/digraph.c\
  vim/src/edit.c\
  vim/src/eval.c\
  vim/src/ex_cmds.c\
  vim/src/ex_cmds2.c\
  vim/src/ex_docmd.c\
  vim/src/ex_eval.c\
  vim/src/ex_getln.c\
  vim/src/fileio.c\
  vim/src/fold.c\
  vim/src/getchar.c\
  vim/src/hardcopy.c\
  vim/src/hashtab.c\
  vim/src/if_cscope.c\
  vim/src/if_xcmdsrv.c\
  vim/src/mark.c\
  vim/src/memline.c\
  vim/src/menu.c\
  vim/src/message.c\
  vim/src/misc1.c\
  vim/src/misc2.c\
  vim/src/move.c\
  vim/src/mbyte.c\
  vim/src/normal.c\
  vim/src/ops.c\
  vim/src/option.c\
  vim/src/os_unix.c\
  vim/src/popupmnu.c\
  vim/src/quickfix.c\
  vim/src/regexp.c\
  vim/src/screen.c\
  vim/src/search.c\
  vim/src/sha256.c\
  vim/src/spell.c\
  vim/src/syntax.c\
  vim/src/tag.c\
  vim/src/term.c\
  vim/src/ui.c\
  vim/src/undo.c\
  vim/src/version.c\
  vim/src/window.c\
  vim/src/netbeans.c\
  vim/src/memfile.c

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libutils libdl libncurses

LOCAL_LDLIBS := -llog

# No static libraries.
LOCAL_STATIC_LIBRARIES :=

# Also need the JNI headers.
LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/libiconv/include \
                   $(LOCAL_PATH)/libiconv/libcharset \
                   $(LOCAL_PATH)/libiconv/libcharset/include \
                   $(LOCAL_PATH)/../gen

# No special compiler flags.
LOCAL_CFLAGS += -I$(LOCAL_PATH) -I$(LOCAL_PATH)/vim/src/ -I$(LOCAL_PATH)/vim/src/proto -I$(LOCAL_PATH)/libncurses/include -DUNIX -DHAVE_CONFIG_H
LOCAL_CFLAGS += -DLIBDIR=\"\" -DTARGET_ARCH_ABI=\"$(TARGET_ARCH_ABI)\"

# Don't prelink this library.  For more efficient code, you may want
# to add this library to the prelink map and set this to true. However,
# it's difficult to do this for applications that are not supplied as
# part of a system image.

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/libncurses/Android.mk
