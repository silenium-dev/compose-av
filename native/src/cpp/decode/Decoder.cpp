//
// Created by silenium-dev on 7/21/24.
//

#include "helper/rationals.hpp"
#include <jni.h>

#include "helper/errors.hpp"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_releaseDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    avcodec_free_context(&avCodecContext);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_submitN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext,
    const jlong packet
) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avcodec_send_packet(avCodecContext, avPacket);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_receiveN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    auto frame = av_frame_alloc();
    if (const auto result = avcodec_receive_frame(avCodecContext, frame); result != 0) {
        av_frame_free(&frame);
        return avResultFailure(env, "receive frame", result);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(frame));
}
}
