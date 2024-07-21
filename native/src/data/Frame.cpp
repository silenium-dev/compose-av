//
// Created by silenium-dev on 7/20/24.
//

#include <jni.h>
#include <cstring>
#include <iostream>

#include "../errors.h"

extern "C" {
#include <libavformat/avformat.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_data_FrameKt_releaseFrameN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    auto avFrame = reinterpret_cast<AVFrame *>(frame);
    av_frame_free(&avFrame);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_data_FrameKt_widthN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->width;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_data_FrameKt_heightN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->height;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_data_FrameKt_formatN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->format;
}

JNIEXPORT jboolean JNICALL Java_dev_silenium_multimedia_data_FrameKt_keyFrameN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->flags & AV_FRAME_FLAG_KEY > 0;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_data_FrameKt_ptsN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->pts;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_data_FrameKt_bestEffortTimestampN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->best_effort_timestamp;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_data_FrameKt_durationN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    return avFrame->duration;
}

JNIEXPORT jobjectArray JNICALL Java_dev_silenium_multimedia_data_FrameKt_dataN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto result = env->NewObjectArray(8, env->FindClass("java/nio/ByteBuffer"), nullptr);
    for (int i = 0; i < 8; i++) {
        if (avFrame->buf[i] == nullptr) {
            continue;
        }
        const auto buf = env->NewDirectByteBuffer(avFrame->buf[i]->data, avFrame->buf[i]->size);
        env->SetObjectArrayElement(result, i, buf);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_dev_silenium_multimedia_data_FrameKt_rawDataN(
    JNIEnv *env,
    jobject thiz,
    const jlong frame
) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto result = env->NewObjectArray(8, env->FindClass("java/lang/Long"), nullptr);
    for (int i = 0; i < 8; i++) {
        if (avFrame->data[i] == nullptr) {
            continue;
        }
        const auto boxed = boxedLong(env, reinterpret_cast<jlong>(avFrame->data[i]));
        env->SetObjectArrayElement(result, i, boxed);
    }
    return result;
}
}
