//
// Created by silenium-dev on 7/21/24.
//

#ifndef ERRORS_H
#define ERRORS_H

#include <jni.h>
#include <string>
#include <GL/gl.h>

std::string avErrorString(int error);

jobject boxedLong(JNIEnv *env, jlong value);

jobject boxedInt(JNIEnv *env, jint value);

jobject pair(JNIEnv *env, jobject first, jobject second);
jobject resultSuccess(JNIEnv *env, jlong value, jlong secondValue);
jobject resultSuccess(JNIEnv *env, jlong value);
jobject resultSuccess(JNIEnv *env, const char *value);
jobject resultSuccess(JNIEnv *env, jdouble value);
jobject resultSuccess(JNIEnv *env, jboolean value);
jobject resultSuccess(JNIEnv *env);
jobject resultSuccessNull();

jobject eglResultFailure(JNIEnv *env, const char *operation, long returnCode);
jobject glResultFailure(JNIEnv *env, const char *operation, GLenum returnCode);

jobject vaResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject mpvResultFailure(JNIEnv *env, const char *operation, int returnCode);

#endif //ERRORS_H
