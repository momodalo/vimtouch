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
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-03-11-231916.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-03-11-232100.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2012-08-28-191719.png)

## Issues
https://github.com/momodalo/vimtouch/issues

## Developer
### How to compile the source code?
Before starting, you need the following prerequisites:
-   Android SDK: http://developer.android.com/sdk/index.html
-   Android NDK: http://developer.android.com/tools/sdk/ndk/index.html
-   Apache ant: http://ant.apache.org/

    (Use `sudo apt-get install ant` on a proper OS)

-   Git: http://git-scm.com/

    (Use `sudo apt-get install git-core` on a proper OS)

Get the source code:

    git clone git://github.com/momodalo/vimtouch.git && cd vimtouch

Now prepare the development environment:

    ANDROID_SDK_HOME=~/path/to/android/sdk && ./prepare-clean-checkout.sh

You're ready to compile it!

    ant debug

(Note: the build script uses some magic to discover NDK and call `ndk-build`.
Since magic is prone to failures, your build might fail with complaints about
Android NDK. If that happens, make sure you export `ANDROID_NDK_ROOT`
environment variable or read the `run` script to discover what heuristics it
uses to deduce your NDK location.)
