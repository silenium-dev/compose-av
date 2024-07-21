//
// Created by silenium-dev on 7/21/24.
//

#include "errors.h"
#include <iostream>

jobject boxedLong(JNIEnv *env, long value) {
    const auto boxedClass = env->FindClass("java/lang/Long");
    const auto constructor = env->GetMethodID(boxedClass, "<init>", "(J)V");
    return env->NewObject(boxedClass, constructor, value);
}

jobject avResultFailure(JNIEnv *env, const char *operation, int returnCode) {
    const auto resultClass = env->FindClass("kotlin/Result$Failure");
    const auto errorClass = env->FindClass("dev/silenium/multimedia/util/AVException");
    const auto errorConstructor = env->GetMethodID(errorClass, "<init>", "(Ljava/lang/String;I)V");
    const auto error = env->NewObject(errorClass, errorConstructor, env->NewStringUTF(operation), returnCode);
    const auto resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/Throwable;)V");
    const auto errorResult = env->NewObject(resultClass, resultConstructor, error);
    return errorResult;
}

jobject avResultSuccess(JNIEnv *env, const long value) {
    const auto boxed = boxedLong(env, value);
    return boxed;
}
