#!/bin/sh

SDK=$ANDROID_SDK_HOME

if [ -f $SDK/tools/android ] ; then
    echo Android SDK found, OK
else
    echo Android SDK not found. Please export ANDROID_SDK_HOME. Exiting.
    exit
fi

cd jni
wget ftp://ftp.vim.org/pub/vim/unix/vim-7.3.tar.bz2
tar jxvf vim-7.3.tar.bz2
mv vim73 vim
rm vim/src/auto/config.h
rm vim/src/feature.h
rm vim-7.3.tar.bz2
wget http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz
tar xzvf libiconv-1.14.tar.gz
mv libiconv-1.14 libiconv
rm libiconv-1.14.tar.gz
git clone https://github.com/CyanogenMod/android_external_libncurses.git libncurses
cd ..

# extract latest platform:
android_platform=`ls -1 $SDK/platforms | sort -t- -k2n | tail -1`
echo $android_platform

$SDK/tools/android update lib-project -p libraries/emulatorview/ -t $android_platform
$SDK/tools/android update lib-project -p libraries/FileExplorer/ -t $android_platform
$SDK/tools/android update project -p . -t $android_platform

ant config

# You're now ready to `$NDK/ndk-build` and `ant debug`
