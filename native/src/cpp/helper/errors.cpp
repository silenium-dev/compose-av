//
// Created by silenium-dev on 7/21/24.
//

#include "errors.hpp"

#include <EGL/egl.h>
#include <GL/gl.h>
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

jobject boxedDouble(JNIEnv *env, const double value) {
    const auto boxedClass = env->FindClass("java/lang/Double");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(D)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject boxedBool(JNIEnv *env, const bool value) {
    const auto boxedClass = env->FindClass("java/lang/Boolean");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(Z)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject boxedInt(JNIEnv *env, const int value) {
    const auto boxedClass = env->FindClass("java/lang/Integer");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(I)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject pair(JNIEnv *env, jobject first, jobject second) {
    const auto pairClass = env->FindClass("kotlin/Pair");
    const auto constructor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    return env->NewObject(pairClass, constructor, first, second);
}

jobject resultSuccess(JNIEnv *env, const long value, const long secondValue) {
    const auto boxed = boxedLong(env, value);
    const auto boxed2 = boxedLong(env, secondValue);
    return pair(env, boxed, boxed2);
}

jobject resultSuccess(JNIEnv *env, const long value) {
    const auto boxed = boxedLong(env, value);
    return boxed;
}

jobject resultSuccess(JNIEnv *env, const char *value) {
    return env->NewStringUTF(value);
}

jobject resultSuccess(JNIEnv *env, const double value) {
    const auto boxed = boxedDouble(env, value);
    return boxed;
}

jobject resultSuccess(JNIEnv *env, const bool value) {
    const auto boxed = boxedBool(env, value);
    return boxed;
}

jobject resultSuccess(JNIEnv *env) {
    const auto unitClass = env->FindClass("kotlin/Unit");
    const auto instanceField = env->GetStaticFieldID(unitClass, "INSTANCE", "Lkotlin/Unit;");
    const auto instance = env->GetStaticObjectField(unitClass, instanceField);

    return instance;
}

jobject resultSuccessNull() {
    return nullptr;
}

jobject eglResultFailure(JNIEnv *env, const char *operation, const long returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/core/util/EGLException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;J)V");
    std::cout << "errorConstructor: " << errorConstructor << std::endl;
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    std::cout << "error: " << error << std::endl;
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject glResultFailure(JNIEnv *env, const char *operation, const GLenum returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/core/util/GLException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    std::cout << "errorConstructor: " << errorConstructor << std::endl;
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    std::cout << "error: " << error << std::endl;
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}
jobject vaResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/core/util/VAException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject mpvResultFailure(JNIEnv *env, const char *operation, const int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/core/util/MPVException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}
