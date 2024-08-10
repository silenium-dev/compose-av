//
// Created by silenium-dev on 7/20/24.
//

#include "helper/errors.hpp"


#include <cstring>
#include <jni.h>

extern "C" {
#include <libavformat/avformat.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_data_PacketKt_releasePacketN(
        JNIEnv *env,
        jobject thiz,
        const jlong packet) {
    auto avPacket = reinterpret_cast<AVPacket *>(packet);
    av_packet_free(&avPacket);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_PacketKt_clonePacketN(
        JNIEnv *env,
        jobject thiz,
        const jlong packet) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    const auto cloned = av_packet_clone(avPacket);
    if (!cloned) {
        return avResultFailure(env, "clone packet", AVERROR(ENOMEM));
    }
    return resultSuccess(env, reinterpret_cast<jlong>(cloned));
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_PacketKt_sizeN(
        JNIEnv *env,
        jobject thiz,
        const jlong packet) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avPacket->size;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_PacketKt_dataN(
        JNIEnv *env,
        jobject thiz,
        const jlong packet) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return env->NewDirectByteBuffer(avPacket->data, avPacket->size);
}
}
