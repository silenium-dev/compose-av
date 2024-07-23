//
// Created by silenium-dev on 7/21/24.
//

#include <iostream>

#include "helper/errors.hpp"
#include <jni.h>
#include <map>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/hwcontext_vaapi.h>

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

AVPixelFormat getFormat(AVCodecContext *codec_context, const AVPixelFormat *fmts) {
    for (auto fmt = fmts; *fmt != AV_PIX_FMT_NONE; fmt++) {
        if (*fmt == AV_PIX_FMT_VAAPI) {
            return AV_PIX_FMT_VAAPI;
        }
    }
    std::cerr << "Failed to get a supported format" << std::endl;
    return AV_PIX_FMT_NONE;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_decode_VaapiDecoderKt_createDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong stream,
    const jstring device
) {
    const auto avStream = reinterpret_cast<AVStream *>(stream);

    AVBufferRef *deviceRef{nullptr};
    auto ret = av_hwdevice_ctx_create(&deviceRef,
                                      AV_HWDEVICE_TYPE_VAAPI, env->GetStringUTFChars(device, nullptr),
                                      nullptr, 0);
    if (ret < 0) {
        return avResultFailure(env, "create hw device context", ret);
    }
    auto framesRef = av_hwframe_ctx_alloc(deviceRef);
    if (!framesRef) {
        av_buffer_unref(&deviceRef);
        return avResultFailure(env, "allocate hw frame context", ret);
    }
    const auto framesCtx = reinterpret_cast<AVHWFramesContext *>(framesRef->data);
    framesCtx->format = AV_PIX_FMT_VAAPI;
    framesCtx->sw_format = mapToNative(static_cast<AVPixelFormat>(avStream->codecpar->format));
    framesCtx->width = avStream->codecpar->width;
    framesCtx->height = avStream->codecpar->height;
    framesCtx->initial_pool_size = 20;
    ret = av_hwframe_ctx_init(framesRef);
    if (ret < 0) {
        av_buffer_unref(&framesRef);
        av_buffer_unref(&deviceRef);
        return avResultFailure(env, "initialize hw frame context", ret);
    }

    const auto codec = avcodec_find_decoder(avStream->codecpar->codec_id);
    auto avCodecContext = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(avCodecContext, avStream->codecpar);

    avCodecContext->hw_device_ctx = deviceRef;
    avCodecContext->hw_frames_ctx = framesRef;
    avCodecContext->get_format = &getFormat;
    avCodecContext->extra_hw_frames = 16;

    ret = avcodec_open2(avCodecContext, codec, nullptr);
    if (ret < 0) {
        av_buffer_unref(&framesRef);
        av_buffer_unref(&deviceRef);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "open codec context", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(avCodecContext));
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_decode_VaapiDecoderKt_getVADisplayN(
    JNIEnv *env, jobject thiz, const jlong codecContext) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    const auto deviceCtx = reinterpret_cast<AVHWFramesContext *>(avCodecContext->hw_frames_ctx->data)->device_ctx;
    const auto vaContext = static_cast<AVVAAPIDeviceContext *>(deviceCtx->hwctx);
    return reinterpret_cast<jlong>(vaContext->display);
}
}
