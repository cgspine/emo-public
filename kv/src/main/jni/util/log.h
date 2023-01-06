#ifndef EMO_LOG_H
#define EMO_LOG_H


#include <android/log.h>

#ifndef TAG
#define TAG "EmoKV"
#endif

#ifndef LOG
#define LOG(priority, tag, ...) \
    __android_log_print(ANDROID_##priority, tag, __VA_ARGS__)
#endif

#ifndef LOG_V
#define LOG_V(...) ((void)LOG(LOG_VERBOSE, TAG, __VA_ARGS__))
#endif

#ifndef LOG_I
#define LOG_I(...) ((void)LOG(LOG_INFO, TAG, __VA_ARGS__))
#endif

#ifndef LOG_W
#define LOG_W(...) ((void)LOG(LOG_WARN, TAG, __VA_ARGS__))
#endif

#ifndef LOG_D
#define LOG_D(...) ((void)LOG(LOG_DEBUG, TAG, __VA_ARGS__))
#endif

#ifndef LOG_E
#define LOG_E(...) ((void)LOG(LOG_ERROR, TAG, __VA_ARGS__))
#endif

#endif //EMO_KV_LOG_H
