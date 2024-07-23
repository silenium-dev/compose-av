//
// Created by silenium-dev on 7/21/24.
//

#ifndef RATIONALS_H
#define RATIONALS_H

#include <jni.h>

extern "C" {
#include <libavutil/rational.h>
}

jobject toJava(JNIEnv *env, const AVRational &rational);
AVRational fromJava(JNIEnv *env, jobject rational);

#endif //RATIONALS_H
