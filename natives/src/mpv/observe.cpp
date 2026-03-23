#include "instance.hpp"
#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyStringN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyLongN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyDoubleN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyFlagN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unobservePropertyN(
    JNIEnv *env, jobject thiz, jlong handle, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
}
}
