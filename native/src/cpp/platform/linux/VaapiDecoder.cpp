//
// Created by silenium-dev on 7/21/24.
//

#include <iostream>

#include "helper/errors.hpp"
#include <jni.h>
#include <map>
#include <unistd.h>
#include <vector>
#include <va/va_glx.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/hwcontext_vaapi.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
}

struct VaapiDecodeContext {
    AVCodecContext *codecContext;
    AVFilterGraph *filterGraph;
    AVFilterInOut *filterIn;
    AVFilterInOut *filterOut;
    AVFilterContext *bufferSrc;
    AVFilterContext *bufferSink;
};

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

jobject createDecoder(
    JNIEnv *env,
    jobject thiz,
    const jlong stream,
    AVBufferRef *deviceRef
) {
    const auto avStream = reinterpret_cast<AVStream *>(stream);

    auto framesRef = av_hwframe_ctx_alloc(deviceRef);
    //    std::cout << "Frames: " << framesRef << std::endl;
    if (!framesRef) {
        //        printf("Failed to allocate hw frame context\n");
        //        fflush(stdout);
        av_buffer_unref(&deviceRef);
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
        av_buffer_unref(&deviceRef);
        return avResultFailure(env, "initialize hw frame context", ret);
    }

    const auto codec = avcodec_find_decoder(avStream->codecpar->codec_id);
    //    std::cout << "Codec: " << codec << std::endl;
    auto avCodecContext = avcodec_alloc_context3(codec);
    //    std::cout << "Codec context: " << avCodecContext << std::endl;
    avcodec_parameters_to_context(avCodecContext, avStream->codecpar);

    avCodecContext->hw_device_ctx = deviceRef;
    avCodecContext->hw_frames_ctx = framesRef;
    avCodecContext->get_format = &getFormat;
    avCodecContext->extra_hw_frames = 16;

    ret = avcodec_open2(avCodecContext, codec, nullptr);
    if (ret < 0) {
        //        printf("Failed to open codec context\n");
        //        fflush(stdout);
        av_buffer_unref(&framesRef);
        av_buffer_unref(&deviceRef);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "open codec context", ret);
    }

    char filterString[2048];
    snprintf(filterString, sizeof(filterString),
             "buffer=video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d [in_1];"
             "[in_1] scale_vaapi=w=%d:h=%d:format=rgb0 [out_1];"
             "[out_1] buffersink",
             avCodecContext->width,
             avCodecContext->height,
             AV_PIX_FMT_VAAPI,
             avStream->time_base.num,
             avStream->time_base.den,
             avCodecContext->sample_aspect_ratio.num,
             avCodecContext->sample_aspect_ratio.den,
             avCodecContext->width,
             avCodecContext->height
    );

    auto filterGraph = avfilter_graph_alloc();
    AVFilterInOut *filterIn{nullptr};
    AVFilterInOut *filterOut{nullptr};
    ret = avfilter_graph_parse2(filterGraph, filterString, &filterIn, &filterOut);
    if (ret < 0) {
        avfilter_graph_free(&filterGraph);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "parse filter graph", ret);
    }

    for (int i = 0; i < filterGraph->nb_filters; ++i) {
        std::cout << filterGraph->filters[i]->name << std::endl;
    }
    AVFilterContext *bufferSrc = avfilter_graph_get_filter(filterGraph, "Parsed_buffer_0");
    AVFilterContext *bufferSink = avfilter_graph_get_filter(filterGraph, "Parsed_buffersink_2");
    AVFilterContext *scale = avfilter_graph_get_filter(filterGraph, "Parsed_scale_vaapi_1");
    bufferSrc->hw_device_ctx = av_buffer_ref(deviceRef);
    bufferSink->hw_device_ctx = av_buffer_ref(deviceRef);
    std::cout << "Scale filter: " << scale << std::endl;
    AVFilterLink *inLink = bufferSrc->outputs[0];
    std::cout << "In link: " << inLink << std::endl;
    inLink->hw_frames_ctx = av_buffer_ref(framesRef);
    AVFilterLink *outLink = scale->outputs[0];

    AVBufferSrcParameters srcParams{};
    srcParams.format = AV_PIX_FMT_VAAPI;
    srcParams.time_base = avStream->time_base;
    srcParams.width = avCodecContext->width;
    srcParams.height = avCodecContext->height;
    srcParams.frame_rate = avStream->avg_frame_rate;
    srcParams.hw_frames_ctx = av_buffer_ref(framesRef);
    srcParams.color_range = avStream->codecpar->color_range;
    srcParams.color_space = avStream->codecpar->color_space;
    ret = av_buffersrc_parameters_set(bufferSrc, &srcParams);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        avfilter_graph_free(&filterGraph);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "set buffer source parameters", ret);
    }

    AVBufferRef *outFramesRef = av_hwframe_ctx_alloc(deviceRef);
    if (!outFramesRef) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        avfilter_graph_free(&filterGraph);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "allocate hw frame context", ret);
    }
    auto outFramesCtx = reinterpret_cast<AVHWFramesContext *>(outFramesRef->data);
    outFramesCtx->format = AV_PIX_FMT_VAAPI;
    outFramesCtx->sw_format = AV_PIX_FMT_RGB0;
    outFramesCtx->width = avCodecContext->width;
    outFramesCtx->height = avCodecContext->height;
    outFramesCtx->initial_pool_size = 20;
    ret = av_hwframe_ctx_init(outFramesRef);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        av_buffer_unref(&outFramesRef);
        avfilter_graph_free(&filterGraph);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "initialize hw frame context", ret);
    }
    outLink->hw_frames_ctx = outFramesRef;

    ret = avfilter_graph_config(filterGraph, nullptr);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        avfilter_graph_free(&filterGraph);
        avcodec_free_context(&avCodecContext);
        return avResultFailure(env, "config filter graph", ret);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(new VaapiDecodeContext{
                             .codecContext = avCodecContext,
                             .filterGraph = filterGraph,
                             .filterIn = filterIn,
                             .filterOut = filterOut,
                             .bufferSrc = bufferSrc,
                             .bufferSink = bufferSink,
                         }));
}


extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_createGLXDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong stream,
    const jlong glxDisplay_
) {
    AVBufferRef *deviceRef = av_hwdevice_ctx_alloc(AV_HWDEVICE_TYPE_VAAPI);
    const auto glxDisplay = reinterpret_cast<Display *>(glxDisplay_);
    const auto vaDisplay = vaGetDisplayGLX(glxDisplay);
    if (!vaDisplay) {
        return vaResultFailure(env, "vaGetDisplayGLX", VA_STATUS_ERROR_UNKNOWN);
    }
    int major, minor;
    if (const auto ret = vaInitialize(vaDisplay, &major, &minor); ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaInitialize", ret);
    }
    static_cast<AVVAAPIDeviceContext *>(reinterpret_cast<AVHWDeviceContext *>(deviceRef->data)->hwctx)->display =
            vaDisplay;
    auto formatCount = vaMaxNumImageFormats(vaDisplay);
    if (formatCount <= 0) {
        std::cerr << "Failed to get image formats" << std::endl;
        return vaResultFailure(env, "vaMaxNumImageFormats", formatCount);
    }

    std::vector<VAImageFormat> imageFormats{static_cast<size_t>(formatCount)};
    if (const auto ret = vaQueryImageFormats(vaDisplay, imageFormats.data(), &formatCount); ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaQueryImageFormats", ret);
    }

    av_log_set_level(AV_LOG_VERBOSE);

    //    std::cout << "Device: " << deviceRef << std::endl;
    if (const auto ret = av_hwdevice_ctx_init(deviceRef); ret < 0) {
        //        printf("Failed to create hw device context: %s\n", av_err2str(ret));
        // fflush(stdout);
        return avResultFailure(env, "create hw device context", ret);
    }

    return createDecoder(env, thiz, stream, deviceRef);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_createDRMDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong stream,
    jstring drmDevice
) {
    AVBufferRef *deviceRef{nullptr};
    const auto stringChars = env->GetStringUTFChars(drmDevice, nullptr);
    auto ret = av_hwdevice_ctx_create(&deviceRef, AV_HWDEVICE_TYPE_VAAPI, stringChars, nullptr, 0);
    env->ReleaseStringUTFChars(drmDevice, stringChars);

    std::cout << "Device: " << deviceRef << std::endl;
    if (ret < 0) {
        //        printf("Failed to create hw device context: %s\n", av_err2str(ret));
        // fflush(stdout);
        return avResultFailure(env, "create hw device context", ret);
    }

    return createDecoder(env, thiz, stream, deviceRef);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_getVADisplayN(
    JNIEnv *env, jobject thiz, const jlong codecContext) {
    const auto avCodecContext = reinterpret_cast<AVCodecContext *>(codecContext);
    const auto deviceCtx = reinterpret_cast<AVHWFramesContext *>(avCodecContext->hw_frames_ctx->data)->device_ctx;
    const auto vaContext = static_cast<AVVAAPIDeviceContext *>(deviceCtx->hwctx);
    return reinterpret_cast<jlong>(vaContext->display);
}

JNIEXPORT void JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_releaseDecoderN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    const auto ctx = reinterpret_cast<VaapiDecodeContext *>(codecContext);
    avcodec_free_context(&ctx->codecContext);
    avfilter_inout_free(&ctx->filterIn);
    avfilter_inout_free(&ctx->filterOut);
    avfilter_graph_free(&ctx->filterGraph);
    delete ctx;
}

JNIEXPORT jint JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_submitN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext,
    const jlong packet
) {
    const auto ctx = reinterpret_cast<VaapiDecodeContext *>(codecContext);
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avcodec_send_packet(ctx->codecContext, avPacket);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDecoderKt_receiveN(
    JNIEnv *env,
    jobject thiz,
    const jlong codecContext
) {
    const auto ctx = reinterpret_cast<VaapiDecodeContext *>(codecContext);
    auto frame = av_frame_alloc();
    if (const auto result = avcodec_receive_frame(ctx->codecContext, frame); result != 0) {
        av_frame_free(&frame);
        return avResultFailure(env, "receive frame", result);
    }

    auto ret = av_buffersrc_add_frame(ctx->bufferSrc, frame);
    if (ret < 0) {
        av_frame_free(&frame);
        return avResultFailure(env, "add frame", ret);
    }
    auto filteredFrame = av_frame_alloc();
    ret = av_buffersink_get_frame(ctx->bufferSink, filteredFrame);
    while (ret == AVERROR(EAGAIN)) {
        usleep(100);
        ret = av_buffersink_get_frame(ctx->bufferSink, filteredFrame);
    }
    if (ret < 0) {
        av_frame_free(&filteredFrame);
        return avResultFailure(env, "get filtered frame", ret);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(filteredFrame));
}
}
