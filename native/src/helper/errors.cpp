//
// Created by silenium-dev on 7/21/24.
//

#include "errors.hpp"
#include <iostream>

extern "C" {
#include <libavutil/error.h>
}

std::string avErrorString(const int error) {
    char errorString[AV_ERROR_MAX_STRING_SIZE];
    av_strerror(error, errorString, AV_ERROR_MAX_STRING_SIZE);
    return {errorString};
}

jobject boxedLong(JNIEnv *env, const long value) {
    const auto boxedClass = env->FindClass("java/lang/Long");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(J)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject boxedInt(JNIEnv *env, const int value) {
    const auto boxedClass = env->FindClass("java/lang/Integer");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(I)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject resultSuccess(JNIEnv *env, const long value) {
    const auto boxed = boxedLong(env, value);
    return boxed;
}

jobject avResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/util/AVException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject eglResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/util/EGLException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject glResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/util/GLException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject vaResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/util/VAException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}
