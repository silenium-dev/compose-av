//
// Created by silenium-dev on 8/4/24.
//

#include "DecoderContext.h"

#include "helper/errors.hpp"

#include <iostream>

DecoderContext::DecoderContext() = default;

DecoderContext::DecoderContext(DecoderContext &&other) noexcept {
    codec = other.codec;
    context = other.context;
    other.codec = nullptr;
    other.context = nullptr;
}

DecoderContext &DecoderContext::operator=(DecoderContext &&other) noexcept {
    if (this != &other) {
        codec = other.codec;
        context = other.context;
        other.codec = nullptr;
        other.context = nullptr;
    }
    return *this;
}

DecoderContext::~DecoderContext() {
    if (context) {
        avcodec_free_context(&context);
    }
}

jobject DecoderContext::configure(JNIEnv *env, jobject thiz, const AVCodecParameters *parameters) {
    codec = avcodec_find_decoder(parameters->codec_id);
    if (!codec) {
        return avResultFailure(env, "find decoder", AVERROR_DECODER_NOT_FOUND);
    }

    context = avcodec_alloc_context3(codec);
    if (!context) {
        return avResultFailure(env, "allocate context", AVERROR(ENOMEM));
    }

    if (avcodec_parameters_to_context(context, parameters) < 0) {
        return avResultFailure(env, "parameters to context", AVERROR(EINVAL));
    }

    if (avcodec_open2(context, codec, nullptr) < 0) {
        return avResultFailure(env, "open codec", AVERROR(EINVAL));
    }

    return resultUnit(env);
}

jobject DecoderContext::submit(JNIEnv *env, const jlong packet) const {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    fflush(stdout);
    const auto prevLogLevel = av_log_get_level();
    av_log_set_level(AV_LOG_FATAL);
    if (const auto ret = avcodec_send_packet(context, avPacket); ret < 0) {
        av_log_set_level(prevLogLevel);
        return avResultFailure(env, "send packet", ret);
    }
    av_log_set_level(prevLogLevel);
    return resultUnit(env);
}

jobject DecoderContext::receive(JNIEnv *env) const {
    auto frame = av_frame_alloc();
    const auto prevLogLevel = av_log_get_level();
    av_log_set_level(AV_LOG_FATAL);
    if (const auto result = avcodec_receive_frame(context, frame); result < 0) {
        av_log_set_level(prevLogLevel);
        av_frame_free(&frame);
        return avResultFailure(env, "receive frame", result);
    }
    av_log_set_level(prevLogLevel);
    return resultSuccess(env, reinterpret_cast<jlong>(frame));
}

void DecoderContext::flush(JNIEnv *env) const {
    avcodec_flush_buffers(context);
}
