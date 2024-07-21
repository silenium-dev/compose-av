//
// Created by silenium-dev on 7/21/24.
//

#ifndef ERRORS_H
#define ERRORS_H

#include <jni.h>

jobject boxedLong(JNIEnv *env, long value);

jobject avResultFailure(JNIEnv *env, const char *operation, int returnCode);

jobject avResultSuccess(JNIEnv *env, long value);

#endif //ERRORS_H
