#ifndef COMMON_H
#define COMMON_H

#ifndef LOG_TAG
#define LOG_TAG "VimTouch-native"
#endif

#include <android/log.h>

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define ARRLEN(x) (sizeof(x) / sizeof(x[0]))

#endif //COMMON_H
