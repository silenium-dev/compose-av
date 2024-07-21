//
// Created by silenium-dev on 7/21/24.
//

#include "../rationals.h"
#include <jni.h>
#include <string>
#include <iostream>

extern "C" {
#include <libavformat/avformat.h>
JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_demux_StreamKt_indexN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->index;
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_demux_StreamKt_codecNameN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    const auto codec = avcodec_get_name(stream->codecpar->codec_id);
    return env->NewStringUTF(codec);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_demux_StreamKt_codecIdN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->codecpar->codec_id;
}

JNIEXPORT jdouble JNICALL Java_dev_silenium_multimedia_demux_StreamKt_durationN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->duration * av_q2d(stream->time_base);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_demux_StreamKt_bitRateN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return stream->codecpar->bit_rate;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_demux_StreamKt_avgFrameRateN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return toJava(env, stream->avg_frame_rate);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_demux_StreamKt_timeBaseN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto stream = reinterpret_cast<AVStream *>(context);
    return toJava(env, stream->time_base);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_demux_StreamKt_typeN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    constexpr auto typeEnumName = "dev/silenium/multimedia/demux/Stream$Type";
    const auto stream = reinterpret_cast<AVStream *>(context);
    const auto type = stream->codecpar->codec_type;
    const auto typeEnum = env->FindClass(typeEnumName);
    std::string name;
    switch (type) {
        case AVMEDIA_TYPE_UNKNOWN:
            name = "UNKNOWN";
            break;
        case AVMEDIA_TYPE_VIDEO:
            name = "VIDEO";
            break;
        case AVMEDIA_TYPE_AUDIO:
            name = "AUDIO";
            break;
        case AVMEDIA_TYPE_DATA:
            name = "DATA";
            break;
        case AVMEDIA_TYPE_SUBTITLE:
            name = "SUBTITLE";
            break;
        case AVMEDIA_TYPE_ATTACHMENT:
            name = "ATTACHMENT";
            break;
        case AVMEDIA_TYPE_NB:
            name = "NB";
            break;
    }
    const auto field = env->GetStaticFieldID(typeEnum, name.c_str(), (std::string("L") + typeEnumName + ";").c_str());
    return env->GetStaticObjectField(typeEnum, field);
}
}
