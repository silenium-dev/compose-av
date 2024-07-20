//
// Created by silenium-dev on 7/20/24.
//

#include <jni.h>

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
}
