//
// Created by silenium-dev on 7/21/24.
//

#ifndef ERRORS_H
#define ERRORS_H

#include <jni.h>
#include <string>

std::string avErrorString(int error);

jobject boxedLong(JNIEnv *env, long value);

jobject boxedInt(JNIEnv *env, int value);

jobject resultSuccess(JNIEnv *env, long value);
jobject resultUnit(JNIEnv *env);

jobject avResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject eglResultFailure(JNIEnv *env, const char *operation, long returnCode);
jobject glResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject vaResultFailure(JNIEnv *env, const char *operation, int returnCode);

#endif //ERRORS_H
