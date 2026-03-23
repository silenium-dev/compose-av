#include "instance.hpp"
#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandN(
    JNIEnv *env, jobject thiz, jlong handle, jobjectArray command) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandStringN(
    JNIEnv *env, jobject thiz, jlong handle, jstring command) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jobjectArray command, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}
}
