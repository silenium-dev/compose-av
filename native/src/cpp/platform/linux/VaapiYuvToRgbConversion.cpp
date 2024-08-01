//
// Created by silenium-dev on 8/1/24.
//

#include "helper/errors.hpp"
#include "helper/rationals.hpp"
#include <cinttypes>
#include <iostream>
#include <unistd.h>

extern "C" {
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>

struct VaapiYuvToRgbConversionContext {
    AVBufferRef *inputFramesRef{nullptr};
    AVBufferRef *outputFramesRef{nullptr};
    AVFilterGraph *filterGraph{nullptr};
    AVFilterContext *bufferSrc{nullptr};
    AVFilterContext *bufferSink{nullptr};
};

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiYuvToRgbConversionKt_createN(JNIEnv *env, jclass clazz, const jlong _deviceRef, const jlong _inputFrame, jobject _timeBase) {
    const auto deviceRef = reinterpret_cast<AVBufferRef *>(_deviceRef);
    const auto inputFrame = reinterpret_cast<AVFrame *>(_inputFrame);
    const auto timeBase = fromJava(env, _timeBase);
    AVBufferRef *inputFramesRef = nullptr;
    if (inputFrame->hw_frames_ctx) {
        inputFramesRef = av_buffer_ref(inputFrame->hw_frames_ctx);
    } else {
        inputFramesRef = av_hwframe_ctx_alloc(deviceRef);
        if (inputFramesRef == nullptr) {
            return avResultFailure(env, "allocating hw frame context", AVERROR(ENOMEM));
        }

        auto hwFramesContext = reinterpret_cast<AVHWFramesContext *>(inputFramesRef->data);
        hwFramesContext->format = AV_PIX_FMT_VAAPI;
        hwFramesContext->sw_format = static_cast<AVPixelFormat>(inputFrame->format);
        hwFramesContext->width = inputFrame->width;
        hwFramesContext->height = inputFrame->height;
        hwFramesContext->initial_pool_size = 1;
        if (const auto ret = av_hwframe_ctx_init(inputFramesRef); ret < 0) {
            av_buffer_unref(&inputFramesRef);
            return avResultFailure(env, "initializing hw frame context", ret);
        }
    }
    AVBufferRef *outputFramesRef = av_hwframe_ctx_alloc(deviceRef);
    if (outputFramesRef == nullptr) {
        av_buffer_unref(&inputFramesRef);
        return avResultFailure(env, "allocating hw frame context", AVERROR(ENOMEM));
    }

    {
        auto hwFramesContext = reinterpret_cast<AVHWFramesContext *>(outputFramesRef->data);
        hwFramesContext->format = AV_PIX_FMT_VAAPI;
        hwFramesContext->sw_format = AV_PIX_FMT_RGB0;
        hwFramesContext->width = inputFrame->width;
        hwFramesContext->height = inputFrame->height;
        hwFramesContext->initial_pool_size = 1;
        if (const auto ret = av_hwframe_ctx_init(outputFramesRef); ret < 0) {
            av_buffer_unref(&inputFramesRef);
            av_buffer_unref(&outputFramesRef);
            return avResultFailure(env, "initializing hw frame context", ret);
        }
    }

    char filterString[2048];
    snprintf(filterString, sizeof(filterString),
             "buffer=video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d [in_1];"
             "[in_1] scale_vaapi=w=%d:h=%d:format=rgb0 [out_1];"
             "[out_1] buffersink",
             inputFrame->width,
             inputFrame->height,
             AV_PIX_FMT_VAAPI,
             timeBase.num,
             timeBase.den,
             inputFrame->sample_aspect_ratio.num,
             inputFrame->sample_aspect_ratio.den,
             inputFrame->width,
             inputFrame->height);

    auto filterGraph = avfilter_graph_alloc();
    AVFilterInOut *filterIn{nullptr};
    AVFilterInOut *filterOut{nullptr};
    auto ret = avfilter_graph_parse2(filterGraph, filterString, &filterIn, &filterOut);
    if (ret < 0) {
        av_buffer_unref(&inputFramesRef);
        av_buffer_unref(&outputFramesRef);
        avfilter_graph_free(&filterGraph);
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
    inLink->hw_frames_ctx = av_buffer_ref(inputFramesRef);
    AVFilterLink *outLink = scale->outputs[0];
    std::cout << "Out link: " << outLink << std::endl;
    outLink->hw_frames_ctx = av_buffer_ref(outputFramesRef);

    AVBufferSrcParameters srcParams{};
    srcParams.format = AV_PIX_FMT_VAAPI;
    srcParams.time_base = timeBase;
    srcParams.width = inputFrame->width;
    srcParams.height = inputFrame->height;
    srcParams.hw_frames_ctx = av_buffer_ref(inputFramesRef);
    srcParams.color_range = inputFrame->color_range;
    srcParams.color_space = inputFrame->colorspace;
    ret = av_buffersrc_parameters_set(bufferSrc, &srcParams);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        av_buffer_unref(&inputFramesRef);
        av_buffer_unref(&outputFramesRef);
        av_buffer_unref(&outLink->hw_frames_ctx);
        av_buffer_unref(&inLink->hw_frames_ctx);
        avfilter_graph_free(&filterGraph);
        return avResultFailure(env, "set buffer source parameters", ret);
    }

    ret = avfilter_graph_config(filterGraph, nullptr);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        av_buffer_unref(&inputFramesRef);
        av_buffer_unref(&outputFramesRef);
        av_buffer_unref(&outLink->hw_frames_ctx);
        av_buffer_unref(&inLink->hw_frames_ctx);
        avfilter_graph_free(&filterGraph);
        return avResultFailure(env, "config filter graph", ret);
    }

    const auto vaapiYuvToRgbConversionContext = new VaapiYuvToRgbConversionContext{
            .inputFramesRef = inputFramesRef,
            .outputFramesRef = outputFramesRef,
            .filterGraph = filterGraph,
            .bufferSrc = bufferSrc,
            .bufferSink = bufferSink,
    };
    return resultSuccess(env, reinterpret_cast<jlong>(vaapiYuvToRgbConversionContext));
}

JNIEXPORT void JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiYuvToRgbConversionKt_destroyN(JNIEnv *env, jclass clazz, const jlong _context) {
    const auto vaapiYuvToRgbConversionContext = reinterpret_cast<VaapiYuvToRgbConversionContext *>(_context);
    std::cout << "Destroying context: " << vaapiYuvToRgbConversionContext << std::endl;
    avfilter_graph_free(&vaapiYuvToRgbConversionContext->filterGraph);
    std::cout << "Filter graph freed" << std::endl;
    av_buffer_unref(&vaapiYuvToRgbConversionContext->inputFramesRef);
    std::cout << "Input frames ref freed" << std::endl;
    av_buffer_unref(&vaapiYuvToRgbConversionContext->outputFramesRef);
    std::cout << "Output frames ref freed" << std::endl;
    delete vaapiYuvToRgbConversionContext;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiYuvToRgbConversionKt_submitN(JNIEnv *env, jclass clazz, const jlong _context, const jlong _inputFrame) {
    const auto ctx = reinterpret_cast<VaapiYuvToRgbConversionContext *>(_context);
    const auto inputFrame = reinterpret_cast<AVFrame *>(_inputFrame);

    auto frame = av_frame_clone(inputFrame);
    if (!frame->hw_frames_ctx) {
        AVFrame *hwFrame = av_frame_alloc();
        if (hwFrame == nullptr) {
            return avResultFailure(env, "allocating hw frame", AVERROR(ENOMEM));
        }

        auto ret = av_hwframe_get_buffer(ctx->inputFramesRef, hwFrame, 0);
        if (ret < 0) {
            av_frame_free(&hwFrame);
            return avResultFailure(env, "getting hw frame buffer", ret);
        }
        ret = av_hwframe_transfer_data(hwFrame, inputFrame, 0);
        if (ret < 0) {
            av_frame_free(&hwFrame);
            return avResultFailure(env, "transfer frame data", ret);
        }
        av_frame_free(&frame);
        frame = hwFrame;
    }

    auto ret = av_buffersrc_add_frame(ctx->bufferSrc, frame);
    if (ret < 0) {
        av_frame_free(&frame);
        return avResultFailure(env, "add frame", ret);
    }

    return resultUnit(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiYuvToRgbConversionKt_receiveN(JNIEnv *env, jclass clazz, const jlong _context) {
    const auto ctx = reinterpret_cast<VaapiYuvToRgbConversionContext *>(_context);
    auto frame = av_frame_alloc();
    auto ret = av_buffersink_get_frame(ctx->bufferSink, frame);
    if (ret < 0) {
        av_frame_free(&frame);
        return avResultFailure(env, "get frame", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(frame));
}
}
