//
// Created by silenium-dev on 7/21/24.
//

#include "../rationals.h"
#include <jni.h>
#include <string>
#include <iostream>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_decode_DecoderKt_releaseDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    avcodec_free_context(&avCodecContext);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_decode_DecoderKt_submitN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext,
    const jlong packet
) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avcodec_send_packet(avCodecContext, avPacket);
}

    JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_decode_DecoderKt_receiveN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    auto frame = av_frame_alloc();
    const auto result = avcodec_receive_frame(avCodecContext, frame);
    if (result != 0) {
        av_frame_free(&frame);
        return result;
    }
    return reinterpret_cast<jlong>(frame);
}
}
