#include "instance.hpp"
#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jstring value) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jlong value) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jdouble value) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagN(
    JNIEnv *env, jobject thiz, jlong handle, jstring name, jboolean value) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}
}
