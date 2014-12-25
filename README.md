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
- Quick buttons
- Unicode and multi-byte
- Read email attachments
- Single instance to open multiple files in vim windows
- Real VIM runtime
- Customizable VIM runtime (ex: can install syntax, doc, plugins)
- Sliding File chooser
- Backup/Restore your VIM runtime
- Command History GUI by long press quick buttons

## Screenshot
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-04-25-045255.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-04-24-204724.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-04-24-204822.png)
![Screenshot](https://raw.github.com/momodalo/vimtouch/master/images/device-2013-04-24-205921.png )

## Issues
https://github.com/momodalo/vimtouch/issues

## Developer
### How to compile the source code?
Before starting, you need the following prerequisites:

-   Android SDK: http://developer.android.com/sdk/index.html
-   Android NDK: http://developer.android.com/tools/sdk/ndk/index.html
-   Git: http://git-scm.com/

    (Use `sudo apt-get install git-core` on a proper OS)

Get the source code:

    git clone git://github.com/nwf/vimtouch.git && cd vimtouch

Now prepare the development environment:

    ANDROID_SDK_HOME=~/path/to/android/sdk
    ANDROID_NDK_ROOT=~/path/to/android/ndk
    ./prepare-clean-checkout.sh

You're ready to compile it!

   ${ANDROID_SDK_HOME}/tools/templates/gradle/wrapper/gradlew assembleDebug
   ${ANDROID_NDK_ROOT}/ndk-build;
   for i in libs/*; do cp $i/vim $i/libvim.so; done
   ${ANDROID_SDK_HOME}/tools/templates/gradle/wrapper/gradlew assembleDebug

Yes, we need to run the build loop twice at the moment.  It is less than
ideal, but it does produce an executable that can hobble along at least.
