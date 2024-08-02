//
// Created by silenium-dev on 7/15/24.
//

#include "VAGLXInteropImage.hpp"
#include "helper/errors.hpp"

#include <GL/gl.h>
#include <jni.h>

extern "C" {
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
}

extern "C" {
JNIEXPORT jobject JNICALL
Java_dev_silenium_multimedia_core_platform_linux_VAGLXRenderInteropKt_mapN(JNIEnv *env, jobject thiz, const jlong frame_) {
    const auto frame = reinterpret_cast<AVFrame *>(frame_);

    auto swFrame = av_frame_alloc();
    if (swFrame == nullptr) {
        return avResultFailure(env, "allocating software frame", AVERROR(ENOMEM));
    }

    auto ret = av_hwframe_map(swFrame, frame, AV_HWFRAME_MAP_READ | AV_HWFRAME_MAP_DIRECT);
    if (ret < 0) {
        ret = av_hwframe_map(swFrame, frame, AV_HWFRAME_MAP_READ);
        if (ret < 0) {
            ret = av_hwframe_transfer_data(swFrame, frame, 0);
        }
    }
    if (ret < 0) {
        av_frame_free(&swFrame);
        return avResultFailure(env, "mapping frame", ret);
    }

    GLuint texture{};
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, swFrame->width, swFrame->height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                 swFrame->data[0]);

    av_frame_free(&swFrame);

    const auto interopImage = new VAGLXInteropImage(texture, {});

    return resultSuccess(env, reinterpret_cast<jlong>(interopImage));
}
}
