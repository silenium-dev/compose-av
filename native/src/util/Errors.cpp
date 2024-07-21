//
// Created by silenium-dev on 7/21/24.
//

#include <EGL/egl.h>
#include <va/va.h>

#define CASE_STR(value) case value: return #value;

const char *eglGetErrorString(const long error) {
    switch (error) {
        CASE_STR(EGL_SUCCESS)
        CASE_STR(EGL_NOT_INITIALIZED)
        CASE_STR(EGL_BAD_ACCESS)
        CASE_STR(EGL_BAD_ALLOC)
        CASE_STR(EGL_BAD_ATTRIBUTE)
        CASE_STR(EGL_BAD_CONTEXT)
        CASE_STR(EGL_BAD_CONFIG)
        CASE_STR(EGL_BAD_CURRENT_SURFACE)
        CASE_STR(EGL_BAD_DISPLAY)
        CASE_STR(EGL_BAD_SURFACE)
        CASE_STR(EGL_BAD_MATCH)
        CASE_STR(EGL_BAD_PARAMETER)
        CASE_STR(EGL_BAD_NATIVE_PIXMAP)
        CASE_STR(EGL_BAD_NATIVE_WINDOW)
        CASE_STR(EGL_CONTEXT_LOST)
        default:
            return "Unknown";
    }
}

#undef CASE_STR

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

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_eglErrorStringN(
    JNIEnv *env,
    jobject thiz,
    jint error
) {
    return env->NewStringUTF(eglGetErrorString(error));
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_vaErrorStringN(
    JNIEnv *env,
    jobject thiz,
    jint error
) {
    return env->NewStringUTF(vaErrorStr(error));
}
}
