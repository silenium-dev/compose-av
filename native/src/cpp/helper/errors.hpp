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

jobject pair(JNIEnv *env, jobject first, jobject second);
jobject resultSuccess(JNIEnv *env, long value, long secondValue);
jobject resultSuccess(JNIEnv *env, long value);
jobject resultSuccess(JNIEnv *env, const char *value);
jobject resultSuccess(JNIEnv *env, double value);
jobject resultSuccess(JNIEnv *env, bool value);
jobject resultSuccess(JNIEnv *env);

jobject avResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject eglResultFailure(JNIEnv *env, const char *operation, long returnCode);
jobject glResultFailure(JNIEnv *env, const char *operation, uint returnCode);

jobject vaResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject mpvResultFailure(JNIEnv *env, const char *operation, int returnCode);

#endif //ERRORS_H
