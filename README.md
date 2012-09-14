# VimTouch

## About
VimTouch is a open source VIM port on Android. It supports full vim syntax and finger touch gestures to help VIM much more usable with touch screens. 

- https://play.google.com/store/apps/details?id=net.momodalo.app.vimtouch
- https://github.com/momodalo/vimtouch
- https://groups.google.com/forum/?fromgroups=#!forum/vimtouch-general

## Features
- Touch to move cursor
- Fling to scroll
- Swipe to zoom
- Two-fingers gesture to zoom in/out
- Unicode and multi-byte
- Single tap to send "ESC"
- Read email attachments
- Single instance to open multiple files in vim windows
- Real VIM runtime
- Customizable VIM runtime (ex: can install syntax, doc, plugins)

## Screenshot
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2012-08-28-191757.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2012-08-28-191719.png)

## Issue
https://github.com/momodalo/vimtouch/issues

## Developer
### How to compile the source code?
The project is compiling with the Android source tree. Please setup the AOSP repo first. http://source.android.com/source/index.html (Please use gingerbread or above)

Get the source code

<pre><code>git clone https://code.google.com/p/vimtouch/</code></pre>

Get the vim source code from http://www.vim.org/sources.php and extract it under vimtouch/jni/vim

<pre><code>cd vimtouch/jni
wget ftp://ftp.vim.org/pub/vim/unix/vim-7.3.tar.bz2
tar jxvf vim-7.3.tar.bz2
mv vim73 vim
rm vim/src/auto/config.h
rm vim/src/feature.h
wget http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz
mv libiconv-1.14 libiconv
</code></pre>

Get the libncurses under vimtouch/jni/libncurses

<pre><code>git clone https://github.com/CyanogenMod/android_external_libncurses.git libncurses
</code></pre>

Setup the Android NDK and SDK build environment
<pre><code>cd [path to vimtouch]
android update project -p .
</code></pre>

Compile it!
<pre><code>cd [path to vimtouch]
ndk-build
ant debug
</code></pre>