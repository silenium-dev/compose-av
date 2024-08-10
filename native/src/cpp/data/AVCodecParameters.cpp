//
// Created by silenium-dev on 8/4/24.
//

#include "helper/rationals.hpp"
#include <jni.h>

extern "C" {
#include <libavcodec/codec_par.h>

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_codecIdN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->codec_id;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_widthN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->width;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_heightN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->height;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_formatN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->format;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_colorSpaceN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->color_space;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_colorPrimariesN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->color_primaries;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_colorRangeN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->color_range;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_colorTrcN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->color_trc;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_frameRateN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return toJava(env, codecParameters->framerate);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_AVCodecParametersKt_bitRateN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto codecParameters = reinterpret_cast<AVCodecParameters *>(context);
    return codecParameters->bit_rate;
}
}
