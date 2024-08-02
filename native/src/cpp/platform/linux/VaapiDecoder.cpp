//
// Created by silenium-dev on 7/21/24.
//

#include <iostream>

#include "helper/errors.hpp"
#include <jni.h>
#include <map>
#include <unistd.h>
#include <va/va_glx.h>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavformat/avformat.h>
#include <libavutil/hwcontext_vaapi.h>
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

AVPixelFormat getFormat(AVCodecContext *codec_context, const AVPixelFormat *fmts) {
    for (auto fmt = fmts; *fmt != AV_PIX_FMT_NONE; fmt++) {
        if (*fmt == AV_PIX_FMT_VAAPI) {
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
        const jlong stream,
        const jlong _deviceRef) {
    const auto deviceRef = reinterpret_cast<AVBufferRef *>(_deviceRef);
    const auto avStream = reinterpret_cast<AVStream *>(stream);

    auto framesRef = av_hwframe_ctx_alloc(deviceRef);
    //    std::cout << "Frames: " << framesRef << std::endl;
    if (!framesRef) {
        //        printf("Failed to allocate hw frame context\n");
        //        fflush(stdout);
        return avResultFailure(env, "allocate hw frame context", AVERROR(ENOMEM));
    }
    const auto framesCtx = reinterpret_cast<AVHWFramesContext *>(framesRef->data);
    framesCtx->format = AV_PIX_FMT_VAAPI;
    framesCtx->sw_format = mapToNative(static_cast<AVPixelFormat>(avStream->codecpar->format));
    framesCtx->width = avStream->codecpar->width;
    framesCtx->height = avStream->codecpar->height;
    framesCtx->initial_pool_size = 20;
    auto ret = av_hwframe_ctx_init(framesRef);
    if (ret < 0) {
        //        printf("Failed to initialize hw frame context\n");
        //        fflush(stdout);
        av_buffer_unref(&framesRef);
        return avResultFailure(env, "initialize hw frame context", ret);
    }

    const auto codec = avcodec_find_decoder(avStream->codecpar->codec_id);
    //    std::cout << "Codec: " << codec << std::endl;
    auto avCodecContext = avcodec_alloc_context3(codec);
    //    std::cout << "Codec context: " << avCodecContext << std::endl;
    avcodec_parameters_to_context(avCodecContext, avStream->codecpar);

    avCodecContext->hw_device_ctx = av_buffer_ref(deviceRef);
    avCodecContext->hw_frames_ctx = framesRef;
    avCodecContext->get_format = &getFormat;

    ret = avcodec_open2(avCodecContext, codec, nullptr);
    if (ret < 0) {
        //        printf("Failed to open codec context\n");
        //        fflush(stdout);
        av_buffer_unref(&framesRef);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "open codec context", ret);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(avCodecContext));
}
}
