//
// Created by silenium-dev on 7/21/24.
//

#include "EGL.hpp"
#include <va/va.h>
#include <jni.h>

extern "C" {
#include <libavutil/error.h>

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_avErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jint error
) {
    char errorStr[AV_ERROR_MAX_STRING_SIZE];
    av_make_error_string(errorStr, AV_ERROR_MAX_STRING_SIZE, error);
    return env->NewStringUTF(errorStr);
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_eglErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jlong error
) {
    return env->NewStringUTF(eglGetErrorString(error));
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_vaErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jint error
) {
    return env->NewStringUTF(vaErrorStr(error));
}
}
