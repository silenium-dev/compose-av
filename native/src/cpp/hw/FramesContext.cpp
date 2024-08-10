//
// Created by silenium-dev on 8/10/24.
//

#include "helper/errors.hpp"


#include <jni.h>

extern "C" {
#include <libavutil/hwcontext.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_createN(
        JNIEnv *env, jobject thiz,
        const jlong deviceContext,
        const jint width,
        const jint height,
        const jint format,
        const jint swFormat,
        const jint initialPoolSize) {
    const auto device = reinterpret_cast<AVBufferRef *>(deviceContext);
    auto *framesContextRef = av_hwframe_ctx_alloc(device);
    if (!framesContextRef) {
        return avResultFailure(env, "allocating hw frame context", AVERROR(ENOMEM));
    }
    auto *framesContext = reinterpret_cast<AVHWFramesContext *>(framesContextRef->data);
    framesContext->width = width;
    framesContext->height = height;
    framesContext->format = static_cast<AVPixelFormat>(format);
    framesContext->sw_format = static_cast<AVPixelFormat>(swFormat);
    framesContext->initial_pool_size = initialPoolSize;
    if (const auto ret = av_hwframe_ctx_init(framesContextRef); ret < 0) {
        av_buffer_unref(&framesContextRef);
        return avResultFailure(env, "initializing hw frame context", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(framesContextRef));
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_widthN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return reinterpret_cast<AVHWFramesContext *>(frames->data)->width;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_heightN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return reinterpret_cast<AVHWFramesContext *>(frames->data)->height;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_formatN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return static_cast<jint>(reinterpret_cast<AVHWFramesContext *>(frames->data)->format);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_swFormatN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return static_cast<jint>(reinterpret_cast<AVHWFramesContext *>(frames->data)->sw_format);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_initialPoolSizeN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return reinterpret_cast<AVHWFramesContext *>(frames->data)->initial_pool_size;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_hw_FramesContextKt_deviceContextN(
        JNIEnv *env, jobject thiz,
        const jlong framesContext) {
    const auto frames = reinterpret_cast<AVBufferRef *>(framesContext);
    return reinterpret_cast<jlong>(reinterpret_cast<AVHWFramesContext *>(frames->data)->device_ref);
}
}
