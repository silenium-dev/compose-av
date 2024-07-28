//
// Created by silenium-dev on 7/21/24.
//

#include <GL/gl.h>
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

JNIEXPORT jstring JNICALL Java_dev_silenium_compose_av_util_ErrorsKt_glErrorStringN(
        JNIEnv *env,
        jobject thiz,
        const jint error
) {
    switch (error) {
        case GL_NO_ERROR:
            return env->NewStringUTF("GL_NO_ERROR");
        case GL_INVALID_ENUM:
            return env->NewStringUTF("GL_INVALID_ENUM");
        case GL_INVALID_VALUE:
            return env->NewStringUTF("GL_INVALID_VALUE");
        case GL_INVALID_OPERATION:
            return env->NewStringUTF("GL_INVALID_OPERATION");
        case GL_INVALID_FRAMEBUFFER_OPERATION:
            return env->NewStringUTF("GL_INVALID_FRAMEBUFFER_OPERATION");
        case GL_OUT_OF_MEMORY:
            return env->NewStringUTF("GL_OUT_OF_MEMORY");
        case GL_STACK_UNDERFLOW:
            return env->NewStringUTF("GL_STACK_UNDERFLOW");
        case GL_STACK_OVERFLOW:
            return env->NewStringUTF("GL_STACK_OVERFLOW");
        default:
            return env->NewStringUTF("Unknown");
    }
}
}
