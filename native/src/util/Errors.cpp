//
// Created by silenium-dev on 7/21/24.
//
extern "C" {
#include <libavutil/error.h>
#include <jni.h>
JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_avErrorStringN(
    JNIEnv *env,
    jobject thiz,
    jint error
) {
    char errorStr[AV_ERROR_MAX_STRING_SIZE];
    av_make_error_string(errorStr, AV_ERROR_MAX_STRING_SIZE, error);
    return env->NewStringUTF(errorStr);
}
}
