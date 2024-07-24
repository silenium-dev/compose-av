//
// Created by silenium-dev on 7/21/24.
//

#include "helper/rationals.hpp"
#include <jni.h>
#include <string>
#include <iostream>

extern "C" {
#include <libavformat/avformat.h>
JNIEXPORT jint JNICALL Java_dev_silenium_compose_av_demux_StreamKt_indexN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->index;
}

JNIEXPORT jint JNICALL Java_dev_silenium_compose_av_demux_StreamKt_codecIdN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->codecpar->codec_id;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_StreamKt_durationN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->duration;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_StreamKt_bitRateN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->codecpar->bit_rate;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_demux_StreamKt_avgFrameRateN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return toJava(env, stream->avg_frame_rate);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_demux_StreamKt_timeBaseN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return toJava(env, stream->time_base);
}

JNIEXPORT jint JNICALL Java_dev_silenium_compose_av_demux_StreamKt_typeN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    constexpr auto typeEnumName = "dev/silenium/compose/av/demux/Stream$Type";
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->codecpar->codec_type;
}
}
