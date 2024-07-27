//
// Created by silenium-dev on 7/21/24.
//

#include <jni.h>

extern "C" {
#include <libavutil/error.h>

JNIEXPORT jstring JNICALL Java_dev_silenium_compose_av_util_ErrorsKt_avErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jint error
) {
    char errorStr[AV_ERROR_MAX_STRING_SIZE];
    av_make_error_string(errorStr, AV_ERROR_MAX_STRING_SIZE, error);
    return env->NewStringUTF(errorStr);
}
}
