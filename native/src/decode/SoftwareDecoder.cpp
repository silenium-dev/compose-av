//
// Created by silenium-dev on 7/21/24.
//

#include "../util/errors.hpp"
#include <jni.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_decode_SoftwareDecoderKt_createDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong stream
) {
    const auto avStream = reinterpret_cast<AVStream *>(stream);
    const auto codec = avcodec_find_decoder(avStream->codecpar->codec_id);
    const auto avCodecContext = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(avCodecContext, avStream->codecpar);
    const auto ret = avcodec_open2(avCodecContext, codec, nullptr);
    if (ret < 0) {
        return avResultFailure(env, "open codec context", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(avCodecContext));
}
}
