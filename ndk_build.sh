#!/bin/sh

ARCH=$1

cp jni/Application.mk.$ARCH jni/Application.mk

$NDK/ndk-build
cp libs/$ARCH/vim libs/$ARCH/libvimexec.so
mkdir -p native/$ARCH
cp libs/$ARCH/*.so native/$ARCH/

