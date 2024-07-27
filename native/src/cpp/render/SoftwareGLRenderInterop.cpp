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
#include <libavutil/imgutils.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_render_SoftwareGLRenderInteropKt_mapN(
        JNIEnv *env,
        jobject thiz,
        const jlong frame) {
    auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto planeCount = AV_NUM_DATA_POINTERS - std::count(avFrame->data, avFrame->data + AV_NUM_DATA_POINTERS, nullptr);

    std::vector<GLuint> textures(planeCount);
    std::vector<Swizzles> swizzles{static_cast<size_t>(planeCount)};

    glGenTextures(textures.size(), textures.data());
    auto fractions = planeFractions[static_cast<AVPixelFormat>(avFrame->format)];
    std::cout << "Format: " << av_get_pix_fmt_name(static_cast<AVPixelFormat>(avFrame->format)) << std::endl;

    for (int i = 0; i < planeCount; ++i) {
        const auto data = new uint8_t[avFrame->width * avFrame->height / fractions[i].first / fractions[i].second];
        for (int y = 0; y < avFrame->height / fractions[i].second; ++y) {
            std::memcpy(data + y * avFrame->width / fractions[i].first,
                        avFrame->data[i] + y * avFrame->linesize[i],
                        avFrame->width / fractions[i].first);
        }
        glBindTexture(GL_TEXTURE_2D, textures[i]);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RED,
                avFrame->width / fractions[i].first,
                avFrame->height / fractions[i].second,
                0,
                GL_RED,
                GL_UNSIGNED_BYTE,
                data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        Swizzles channels{};
        switch (avFrame->format) {
            case AV_PIX_FMT_P010BE:
            case AV_PIX_FMT_YUV420P10BE:
            case AV_PIX_FMT_P010LE:
            case AV_PIX_FMT_YUV420P10LE:
                switch (i) {
                    case 1:
                        channels.r = Swizzle::USE_GREEN;
                        channels.g = Swizzle::USE_RED;
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        swizzles[i] = channels;
    }

    return resultSuccess(env, reinterpret_cast<jlong>(new SoftwareGLInteropImage(textures, swizzles)));
}
}
