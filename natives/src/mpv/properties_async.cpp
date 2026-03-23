#include "instance.hpp"
#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jstring value, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong value, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jdouble value, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jboolean value, jlong subscriptionId) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}
}
