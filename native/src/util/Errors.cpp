//
// Created by silenium-dev on 7/21/24.
//

#include <GL/gl.h>
#include <jni.h>
#include <mpv/client.h>

extern "C" {
JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_core_util_ErrorsKt_mpvErrorStringN(
        JNIEnv *env,
        jobject thiz,
        const jint error) {
    return env->NewStringUTF(mpv_error_string(error));
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_core_util_ErrorsKt_glErrorStringN(
        JNIEnv *env,
        jobject thiz,
        const jint error) {
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
