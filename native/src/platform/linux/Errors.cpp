//
// Created by silenium-dev on 2024-07-23.
//

#include "EGL.hpp"
#include <va/va.h>
#include <jni.h>

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
