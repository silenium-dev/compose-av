//
// Created by silenium-dev on 7/26/24.
//

#include "SoftwareGLInteropImage.hpp"
#include "helper/errors.hpp"
#include "helper/formats.hpp"
#include <GLES3/gl3.h>
#include <algorithm>
#include <cstring>
#include <iostream>
#include <jni.h>
#include <vector>

extern "C" {
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/imgutils.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_render_SoftwareGLRenderInteropKt_mapN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    auto avFrame = reinterpret_cast<AVFrame *>(frame);

    if (avFrame->hw_frames_ctx) {
        auto swFrame = av_frame_alloc();
        if (swFrame == nullptr) {
            return avResultFailure(env, "allocating software frame", AVERROR(ENOMEM));
        }

        auto ret = av_hwframe_map(swFrame, avFrame, AV_HWFRAME_MAP_READ | AV_HWFRAME_MAP_DIRECT);
        if (ret < 0) {
            ret = av_hwframe_map(swFrame, avFrame, AV_HWFRAME_MAP_READ);
            if (ret < 0) {
                ret = av_hwframe_transfer_data(swFrame, avFrame, 0);
            }
        }
        if (ret < 0) {
            av_frame_free(&swFrame);
            return avResultFailure(env, "mapping frame", ret);
        }
        avFrame = swFrame;
    }

    const auto planeCount = AV_NUM_DATA_POINTERS - std::count(avFrame->data, avFrame->data + AV_NUM_DATA_POINTERS, nullptr);

    std::vector<GLuint> textures(planeCount);
    std::vector<Swizzles> swizzles{static_cast<size_t>(planeCount)};

    glGenTextures(textures.size(), textures.data());
    auto fractions = planeFractions[static_cast<AVPixelFormat>(avFrame->format)];
    auto glFormats = planeTextureFormats[static_cast<AVPixelFormat>(avFrame->format)];
    auto components = planeComponents[static_cast<AVPixelFormat>(avFrame->format)];
    // std::cout << "Format: " << av_get_pix_fmt_name(static_cast<AVPixelFormat>(avFrame->format)) << std::endl;

    for (int i = 0; i < planeCount; ++i) {
        const auto data = new uint8_t[avFrame->width * avFrame->height / fractions[i].first / fractions[i].second * components[i]];
        for (int y = 0; y < avFrame->height / fractions[i].second; ++y) {
            std::memcpy(data + y * avFrame->width / fractions[i].first * components[i],
                        avFrame->data[i] + y * avFrame->linesize[i],
                        avFrame->width / fractions[i].first * components[i]);
        }
        glBindTexture(GL_TEXTURE_2D, textures[i]);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                glFormats[i].first,
                avFrame->width / fractions[i].first,
                avFrame->height / fractions[i].second,
                0,
                glFormats[i].second,
                GL_UNSIGNED_BYTE,
                data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        swizzles[i] = {};
    }

    if (const auto originalFrame = reinterpret_cast<AVFrame *>(frame); originalFrame != avFrame) {
        av_frame_free(&avFrame);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(new SoftwareGLInteropImage(textures, swizzles)));
}
}
