#include "mpv/instance.hpp"
#include "mpv/renderer.hpp"

#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createRenderN(
    JNIEnv *env, jobject thiz, const jlong handle, const jboolean advancedControl) {
    INSTANCE(handle);
    (void) instance;
    return nullptr;
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyRenderN(
    JNIEnv *env, jobject thiz, const jlong rendererHandle) {
    RENDERER(rendererHandle);
    (void) renderer;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_renderN(
    JNIEnv *env, jobject thiz, const jlong rendererHandle,
    const jint fbo, const jint width, const jint height, const jint glInternalFormat) {
    RENDERER(rendererHandle);
    (void) renderer;
    return nullptr;
}
}
