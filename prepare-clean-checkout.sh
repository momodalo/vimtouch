#!/bin/sh

cd jni
rm -r vim
rm -r libiconv
wget ftp://ftp.vim.org/pub/vim/unix/vim-7.3.tar.bz2
tar jxvf vim-7.4.tar.bz2
mv vim73 vim
rm vim/src/auto/config.h
rm vim/src/feature.h
rm vim-7.3.tar.bz2
#echo Applying integration.patch...
#patch -i ./integration.patch -p0
wget http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz
tar xzvf libiconv-1.14.tar.gz
mv libiconv-1.14 libiconv
rm libiconv-1.14.tar.gz
git clone git@github.com:momodalo/android_external_libncurses.git -b vimtouch libncurses
cd ..

