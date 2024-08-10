//
// Created by silenium-dev on 7/20/24.
//

#include "helper/buffers.hpp"


#include <cstring>
#include <iostream>
#include <jni.h>

#include "helper/errors.hpp"
#include "helper/rationals.hpp"

extern "C" {
#include <libavformat/avformat.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_releaseFrameN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    auto avFrame = reinterpret_cast<AVFrame *>(frame);
    av_frame_free(&avFrame);
}
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_timeBaseN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return toJava(env, avFrame->time_base);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_sampleAspectRatioN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return toJava(env, avFrame->sample_aspect_ratio);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_widthN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->width;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_heightN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->height;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_formatN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->format;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_colorSpaceN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->colorspace;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_colorRangeN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->color_range;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_colorPrimariesN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->color_primaries;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_colorTrcN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->color_trc;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_swFormatN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    if (avFrame->hw_frames_ctx == nullptr) return -1;
    const auto hwFramesCtx = reinterpret_cast<AVHWFramesContext *>(avFrame->hw_frames_ctx->data);
    return hwFramesCtx->sw_format;
}

JNIEXPORT jboolean JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_keyFrameN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return (avFrame->flags & AV_FRAME_FLAG_KEY) > 0;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_ptsN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->pts;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_bestEffortTimestampN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->best_effort_timestamp;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_durationN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->duration;
}

JNIEXPORT jboolean JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_isHWN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->hw_frames_ctx != nullptr;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_hwFramesContextN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    if (!avFrame->hw_frames_ctx) {
        return avResultFailure(env, "frame has no hw frames context", AVERROR(EINVAL));
    }
    return resultSuccess(env, reinterpret_cast<long>(av_buffer_ref(avFrame->hw_frames_ctx)));
}

JNIEXPORT jobjectArray JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_bufN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto result = env->NewObjectArray(8, env->FindClass("dev/silenium/multimedia/core/data/AVBufferRef"), nullptr);
    for (int i = 0; i < 8; i++) {
        if (avFrame->buf[i] == nullptr) {
            continue;
        }
        const auto buf = bufferToJava(env, avFrame->buf[i]);
        env->SetObjectArrayElement(result, i, buf);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_dataN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto result = env->NewObjectArray(8, env->FindClass("java/lang/Long"), nullptr);
    for (int i = 0; i < 8; i++) {
        const auto boxed = boxedLong(env, reinterpret_cast<jlong>(avFrame->data[i]));
        env->SetObjectArrayElement(result, i, boxed);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_pitchN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto result = env->NewObjectArray(8, env->FindClass("java/lang/Integer"), nullptr);
    for (int i = 0; i < 8; i++) {
        if (avFrame->data[i] == nullptr) {
            continue;
        }
        const auto boxed = boxedInt(env, avFrame->linesize[i]);
        env->SetObjectArrayElement(result, i, boxed);
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_cloneN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    auto clone = av_frame_clone(avFrame);
    if (clone == nullptr) {
        return avResultFailure(env, "clone frame", AVERROR(ENOMEM));
    }
    return resultSuccess(env, reinterpret_cast<jlong>(clone));
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_FrameKt_transferToSWN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    if (avFrame->hw_frames_ctx == nullptr) {
        return nullptr;
    }
    auto swFrame = av_frame_alloc();
    if (const auto ret = av_hwframe_transfer_data(swFrame, avFrame, 0); ret < 0) {
        av_frame_free(&swFrame);
        return avResultFailure(env, "transfer frame data", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(swFrame));
}
}
