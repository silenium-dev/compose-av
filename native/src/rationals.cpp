//
// Created by silenium-dev on 7/21/24.
//

#include "rationals.h"

jobject toJava(JNIEnv *env, const AVRational &rational) {
    const auto clazz = env->FindClass("dev/silenium/multimedia/util/Rational");
    const auto constructor = env->GetMethodID(clazz, "<init>", "(II)V");
    return env->NewObject(clazz, constructor, rational.num, rational.den);
}

AVRational fromJava(JNIEnv *env, jobject rational) {
    const auto clazz = env->FindClass("dev/silenium/multimedia/util/Rational");
    const auto numeratorField = env->GetFieldID(clazz, "num", "I");
    const auto denominatorField = env->GetFieldID(clazz, "den", "I");
    return {env->GetIntField(rational, numeratorField), env->GetIntField(rational, denominatorField)};
}
