#include "instance.hpp"
#include "helper/results.hpp"

#include <jni.h>

#include "MPVException.hpp"
#include "helper/JniMpvCallback.hpp"


extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createN(
    JNIEnv *env, jobject thiz) {
    CATCHING(
        const auto handle = mpv_create();
        return resultSuccess(env, reinterpret_cast<jlong>(handle));
    )
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyN(
    JNIEnv *env, jobject thiz, jlong handle) {
    INSTANCE(handle);
    delete instance;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setOptionStringN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jstring value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const auto valueChars = env->GetStringUTFChars(value, nullptr);
    const std::string nameStr(nameChars);
    const std::string valueStr(valueChars);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(value, valueChars);
    CATCHING(
        instance->setOption(nameStr, valueStr);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_initializeN(
    JNIEnv *env, jobject thiz, jlong handle) {
    INSTANCE(handle);
    CATCHING(
        instance->initialize();
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setCallbackN(
    JNIEnv *env, jobject thiz, jlong handle, jobject listener) {
    INSTANCE(handle);
    CATCHING(
        instance->setCallback(std::make_unique<JniMpvCallback>(env, listener));
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unsetCallbackN(
    JNIEnv *env, jobject thiz, jlong handle) {
    INSTANCE(handle);
    CATCHING(
        instance->unsetCallback();
        return resultSuccess(env);
    )
}
}
