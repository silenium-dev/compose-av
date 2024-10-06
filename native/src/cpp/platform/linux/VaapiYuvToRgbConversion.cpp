//
// Created by silenium-dev on 8/1/24.
//

#include "data/FramePadMetadata.hpp"
#include "helper/errors.hpp"
#include "helper/rationals.hpp"
#include "helper/va.hpp"

#include <cinttypes>
#include <iostream>
#include <unistd.h>

extern "C" {
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>

struct VaapiYuvToRgbConversionContext {
    AVBufferRef *inputFramesRef{nullptr};
    AVBufferRef *outputFramesRef{nullptr};
    AVFilterGraph *filterGraph{nullptr};
    AVFilterContext *bufferSrc{nullptr};
    AVFilterContext *bufferSink{nullptr};
};

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiYuvToRgbConversionKt_createN(JNIEnv *env, jclass clazz, jobject _inputMetadata, const jlong _deviceRef, const jlong _inputFramesContext, const jlong _outputFramesContext) {
    const auto deviceRef = reinterpret_cast<AVBufferRef *>(_deviceRef);
    const auto inputFramesRef = reinterpret_cast<AVBufferRef *>(_inputFramesContext);
    const auto outputFramesRef = reinterpret_cast<AVBufferRef *>(_outputFramesContext);
    const FramePadMetadata inputMetadata{env, _inputMetadata};
    const auto timeBase = inputMetadata.timeBase();

    char filterString[2048];
    snprintf(filterString, sizeof(filterString),
             "buffer=video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d [in_1];"
             "[in_1] scale_vaapi=w=%d:h=%d:format=rgb0 [out_1];"
             "[out_1] buffersink",
             inputMetadata.width(),
             inputMetadata.height(),
             AV_PIX_FMT_VAAPI,
             timeBase.num,
             timeBase.den,
             inputMetadata.sample_aspect_ratio().num,
             inputMetadata.sample_aspect_ratio().den,
             inputMetadata.width(),
             inputMetadata.height());

    auto filterGraph = avfilter_graph_alloc();
    AVFilterInOut *filterIn{nullptr};
    AVFilterInOut *filterOut{nullptr};
    auto ret = avfilter_graph_parse2(filterGraph, filterString, &filterIn, &filterOut);
    if (ret < 0) {
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
    srcParams.width = inputMetadata.width();
    srcParams.height = inputMetadata.height();
    srcParams.hw_frames_ctx = av_buffer_ref(inputFramesRef);
    srcParams.color_range = inputMetadata.colorRange();
    srcParams.color_space = inputMetadata.colorSpace();
    ret = av_buffersrc_parameters_set(bufferSrc, &srcParams);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        av_buffer_unref(&outLink->hw_frames_ctx);
        av_buffer_unref(&inLink->hw_frames_ctx);
        av_buffer_unref(&bufferSrc->hw_device_ctx);
        av_buffer_unref(&bufferSink->hw_device_ctx);
        avfilter_graph_free(&filterGraph);
        return avResultFailure(env, "set buffer source parameters", ret);
    }

    ret = avfilter_graph_config(filterGraph, nullptr);
    if (ret < 0) {
        av_buffer_unref(&srcParams.hw_frames_ctx);
        av_buffer_unref(&outLink->hw_frames_ctx);
        av_buffer_unref(&inLink->hw_frames_ctx);
        av_buffer_unref(&bufferSrc->hw_device_ctx);
        av_buffer_unref(&bufferSink->hw_device_ctx);
        avfilter_graph_free(&filterGraph);
        return avResultFailure(env, "config filter graph", ret);
    }

    const auto vaapiYuvToRgbConversionContext = new VaapiYuvToRgbConversionContext{
            .inputFramesRef = av_buffer_ref(inputFramesRef),
            .outputFramesRef = av_buffer_ref(outputFramesRef),
            .filterGraph = filterGraph,
            .bufferSrc = bufferSrc,
            .bufferSink = bufferSink,
    };
    return resultSuccess(env, reinterpret_cast<jlong>(vaapiYuvToRgbConversionContext));
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiYuvToRgbConversionKt_destroyN(JNIEnv *env, jclass clazz, const jlong _context) {
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

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiYuvToRgbConversionKt_submitN(JNIEnv *env, jclass clazz, const jlong _context, const jlong _inputFrame) {
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

        hwFrame->best_effort_timestamp = frame->best_effort_timestamp;
        hwFrame->pts = frame->pts;
        hwFrame->duration = frame->duration;
        hwFrame->flags = frame->flags;
        hwFrame->color_primaries = frame->color_primaries;
        hwFrame->color_trc = frame->color_trc;
        hwFrame->colorspace = frame->colorspace;
        hwFrame->color_range = frame->color_range;
        hwFrame->sample_aspect_ratio = frame->sample_aspect_ratio;

        av_frame_free(&frame);
        frame = hwFrame;
    } else {
        if (frame->format != AV_PIX_FMT_VAAPI) {
            return avResultFailure(env, "input frame format is not VAAPI", AVERROR(EINVAL));
        }
        const auto hwFrame = av_frame_clone(frame);
        if (hwFrame == nullptr) {
            return avResultFailure(env, "allocating hw frame", AVERROR(ENOMEM));
        }

        // auto ret = mapFrameToDifferentContext(hwFrame, inputFrame, ctx->inputFramesRef);
        // if (ret.code != 0) {
            // av_frame_free(&hwFrame);
            // return avResultFailure(env, ret.message, ret.code);
        // }

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

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiYuvToRgbConversionKt_receiveN(JNIEnv *env, jclass clazz, const jlong _context) {
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
