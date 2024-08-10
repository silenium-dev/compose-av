//
// Created by silenium-dev on 7/21/24.
//

#include "decode/DecoderContext.h"

#include "helper/buffers.hpp"
#include "helper/errors.hpp"
#include <iostream>
#include <jni.h>
#include <map>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavfilter/avfilter.h>
#include <libavformat/avformat.h>
}

static std::map<AVPixelFormat, AVPixelFormat> nativeMapping{
        {AV_PIX_FMT_YUV420P, AV_PIX_FMT_NV12},
        {AV_PIX_FMT_YUV420P10BE, AV_PIX_FMT_P010BE},
        {AV_PIX_FMT_YUV420P10LE, AV_PIX_FMT_P010LE},
};

AVPixelFormat mapToNative(const AVPixelFormat format) {
    if (nativeMapping.contains(format)) {
        return nativeMapping[format];
    }
    return format;
}

AVPixelFormat getFormat(AVCodecContext *codec_context, const AVPixelFormat *fmts);

class VaapiDecoderContext final : public DecoderContext {
public:
    explicit VaapiDecoderContext(AVBufferRef *deviceRef) : deviceRef(deviceRef) {}

    ~VaapiDecoderContext() override {
        av_buffer_unref(&framesRef);
        av_buffer_unref(&deviceRef);
    }

    jobject configure(JNIEnv *env, jobject thiz, const AVCodecParameters *parameters) override {
        const auto thizClass = env->GetObjectClass(thiz);
        const auto framesContextField = env->GetFieldID(thizClass, "framesContext", "Ldev/silenium/multimedia/core/hw/FramesContext;");
        const auto framesContext = env->GetObjectField(thiz, framesContextField);
        const auto framesContextClass = env->GetObjectClass(framesContext);
        const auto framesContextBufferRefField = env->GetFieldID(framesContextClass, "bufferRef", "Ldev/silenium/multimedia/core/data/AVBufferRef;");
        const auto framesContextBufferRef = env->GetObjectField(framesContext, framesContextBufferRefField);

        framesRef = av_buffer_ref(bufferFromJava(env, framesContextBufferRef));
        if (!framesRef) {
            return avResultFailure(env, "frames context bufferRef not found", AVERROR(EINVAL));
        }

        deviceRef = av_buffer_ref(reinterpret_cast<AVHWFramesContext *>(framesRef->data)->device_ref);

        codec = avcodec_find_decoder(parameters->codec_id);
        context = avcodec_alloc_context3(codec);

        avcodec_parameters_to_context(context, parameters);

        context->opaque = reinterpret_cast<void *>(this);
        context->get_format = &getFormat;

        if (const auto ret = avcodec_open2(context, codec, nullptr); ret < 0) {
            av_buffer_unref(&framesRef);
            avcodec_free_context(&context);
            return avResultFailure(env, "open codec context", ret);
        }

        return resultUnit(env);
    }

    AVBufferRef *deviceRef{nullptr};
    AVBufferRef *framesRef{nullptr};
};

AVPixelFormat getFormat(AVCodecContext *codec_context, const AVPixelFormat *fmts) {
    const auto opaque = static_cast<VaapiDecoderContext *>(codec_context->opaque);
    for (auto fmt = fmts; *fmt != AV_PIX_FMT_NONE; fmt++) {
        if (*fmt == AV_PIX_FMT_VAAPI) {
            codec_context->hw_frames_ctx = av_buffer_ref(opaque->framesRef);
            return AV_PIX_FMT_VAAPI;
        }
    }
    std::cerr << "Failed to get a supported format" << std::endl;
    return AV_PIX_FMT_NONE;
}


extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiDecoderKt_createN(
        JNIEnv *env,
        jobject thiz,
        const jlong _deviceRef) {
    const auto deviceRef = reinterpret_cast<AVBufferRef *>(_deviceRef);

    auto ctx = new VaapiDecoderContext{deviceRef};
    return resultSuccess(env, reinterpret_cast<jlong>(ctx));
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiDecoderKt_mapToNativeN(
        JNIEnv *env,
        jobject thiz,
        const jint _format) {
    const auto format = static_cast<AVPixelFormat>(_format);
    return mapToNative(format);
}
}
