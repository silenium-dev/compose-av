//
// Created by silenium-dev on 7/20/24.
//

#include <jni.h>
#include <cstring>

extern "C" {
#include <libavformat/avformat.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_data_PacketKt_releasePacketN(
    JNIEnv *env,
    jobject thiz,
    const jlong packet
) {
    auto avPacket = reinterpret_cast<AVPacket *>(packet);
    av_packet_free(&avPacket);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_data_PacketKt_sizeN(
    JNIEnv *env,
    jobject thiz,
    const jlong packet
) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avPacket->size;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_data_PacketKt_dataN(
    JNIEnv *env,
    jobject thiz,
    const jlong packet,
    const jobject buf
) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    const auto bufPtr = env->GetDirectBufferAddress(buf);
    std::memcpy(bufPtr, avPacket->data, avPacket->size);
    return buf;
}
}
