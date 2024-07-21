//
// Created by silenium-dev on 7/15/24.
//

#include "EGL.h"
#include "errors.h"

#include <jni.h>
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <va/va.h>
#include <va/va_drmcommon.h>
#include <fcntl.h>
#include <unistd.h>
#include <drm_fourcc.h>

#define STB_IMAGE_IMPLEMENTATION
#include <stb/stb_image.h>
#undef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include <array>
#include <map>
#include <vector>
#include <libavutil/hwcontext.h>
#include <stb/stb_image_write.h>

#include "drm_mapping.h"
#undef STB_IMAGE_WRITE_IMPLEMENTATION

typedef void (EGLAPIENTRYP PFNEGLIMAGETARGETTEXTURE2DOESPROC)(EGLenum target, void *image);

extern "C" {
struct EGLVASurface {
    std::array<EGLImageKHR, 4> eglImages{EGL_NO_IMAGE_KHR};
    std::array<GLuint, 4> textures{0};
};

void checkVAError(const int ret, const std::string &operation = "libva call") {
    if (ret != VA_STATUS_SUCCESS) {
        throw std::runtime_error{vaErrorStr(ret)};
    }
}

void closeDrm(VADRMPRIMESurfaceDescriptor drm) {
    for (int i = 0; i < drm.num_objects; ++i) {
        close(drm.objects[i].fd);
    }
}

#include <libavutil/hwcontext_vaapi.h>
#include <libavutil/frame.h>

JNIEXPORT jlong JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_getVADisplayN(JNIEnv *env, jobject thiz, const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto deviceCtx = reinterpret_cast<AVHWFramesContext *>(avFrame->hw_frames_ctx->data)->device_ctx;
    const auto vaContext = static_cast<AVVAAPIDeviceContext *>(deviceCtx->hwctx);
    return reinterpret_cast<jlong>(vaContext->display);
}

std::map<AVPixelFormat, std::map<int, std::pair<int, int> > > planeFractions{
    {AV_PIX_FMT_NV12, {{0, {1, 1}}, {1, {2, 2}}}},
    {AV_PIX_FMT_P010LE, {{0, {1, 1}}, {1, {2, 2}}}},
    {AV_PIX_FMT_P010BE, {{0, {1, 1}}, {1, {2, 2}}}},
    {AV_PIX_FMT_YUV420P, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
    {AV_PIX_FMT_YUV420P10LE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
    {AV_PIX_FMT_YUV420P10BE, {{0, {1, 1}}, {1, {2, 2}}, {2, {2, 2}}}},
    {AV_PIX_FMT_YUV422P, {{0, {1, 1}}, {1, {2, 1}}, {2, {2, 1}}}},
    {AV_PIX_FMT_YUV444P, {{0, {1, 1}}, {1, {1, 1}}, {2, {1, 1}}}},
};

JNIEXPORT jobject JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_createTextureFromSurfaceN(JNIEnv *env, jobject thiz,
                                                                  const jint pixelFormat,
                                                                  const jlong vaSurface_, const jlong vaDisplay_,
                                                                  const jlong eglDisplay_) {
    const auto format = static_cast<AVPixelFormat>(pixelFormat);
    const auto vaDisplay = reinterpret_cast<VADisplay>(vaDisplay_);
    const auto vaSurface = static_cast<VASurfaceID>(vaSurface_);
    const auto eglDisplay = reinterpret_cast<EGLDisplay>(eglDisplay_);
//    std::cout << "VASurface: " << vaSurface << std::endl;
//    std::cout << "VADisplay: " << vaDisplay << std::endl;
//    std::cout << "EGLDisplay: " << eglDisplay << std::endl;

    const auto eglCreateImageKHR = getFunc<PFNEGLCREATEIMAGEKHRPROC>("eglCreateImageKHR");
    if (!eglCreateImageKHR) {
        std::cerr << "Failed to get eglCreateImageKHR" << std::endl;
        return eglResultFailure(env, "get eglCreateImageKHR", -1);
    }

    const auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    if (!eglDestroyImageKHR) {
        std::cerr << "Failed to get eglDestroyImageKHR" << std::endl;
        return eglResultFailure(env, "get eglDestroyImageKHR", -1);
    }

    const auto glEGLImageTargetTexture2DOES = getFunc<
        PFNEGLIMAGETARGETTEXTURE2DOESPROC>("glEGLImageTargetTexture2DOES");
    if (!glEGLImageTargetTexture2DOES) {
        std::cerr << "Failed to get glEGLImageTargetTexture2DOES" << std::endl;
        return eglResultFailure(env, "get glEGLImageTargetTexture2DOES", -1);
    }

    VADRMPRIMESurfaceDescriptor drm{};
    auto ret = vaExportSurfaceHandle(
        vaDisplay,
        vaSurface,
        VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2,
        VA_EXPORT_SURFACE_READ_WRITE | VA_EXPORT_SURFACE_SEPARATE_LAYERS,
        &drm);
    if (ret != VA_STATUS_SUCCESS) {
        std::cerr << "Failed to export surface handle: " << vaErrorStr(ret) << std::endl;
        return vaResultFailure(env, "vaExportSurfaceHandle", ret);
    }
    ret = vaSyncSurface(vaDisplay, vaSurface);
    if (ret != VA_STATUS_SUCCESS) {
        std::cerr << "Failed to sync surface: " << vaErrorStr(ret) << std::endl;
        closeDrm(drm);
        return vaResultFailure(env, "vaSyncSurface", ret);
    }

//    std::cout << "drm.num_objects: " << drm.num_objects << std::endl;
//    for (int i = 0; i < drm.num_objects; ++i) {
//        std::cout << "Exported DRM object " << i << ": fd=" << drm.objects[i].fd << ", extent=" << drm.objects[i].size
//                << std::endl;
//    }

    const auto eglGetError = getFunc<PFNEGLGETERRORPROC>("eglGetError");

    GLint prevTexture;
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &prevTexture);

    auto *eglVASurface = new EGLVASurface();
    for (int layer = 0; layer < drm.num_layers; ++layer) {
        const auto [drm_format, num_planes, object_index, offset, pitch] = drm.layers[layer];
//        std::cout << "layer[" << layer << "]: drm_format: " << drmFourccToString(drm_format) << std::endl;
//        std::cout << "layer[" << layer << "]: num_planes: " << num_planes << std::endl;
        const auto [fd, size, drm_format_modifier] = drm.objects[object_index[0]];

//        std::cout << "layer[" << layer << "]: fd: " << fd << std::endl;
//        std::cout << "layer[" << layer << "]: size: " << size << std::endl;
//        std::cout << "layer[" << layer << "]: offset: " << offset[0] << std::endl;
//        std::cout << "layer[" << layer << "]: pitch: " << pitch[0] << std::endl;

        std::pair<int, int> fraction;
        if (planeFractions.contains(format)) {
            fraction = planeFractions[format][layer];
        } else {
            fraction = {1, 1};
        }
//        std::cout << "layer[" << layer << "]: fraction: " << fraction.first << "/" << fraction.second << std::endl;
        EGLint attribs[]{
            EGL_WIDTH, static_cast<EGLint>(drm.width / fraction.first),
            EGL_HEIGHT, static_cast<EGLint>(drm.height / fraction.second),
            EGL_LINUX_DRM_FOURCC_EXT, static_cast<EGLint>(drm_format),
            EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT, static_cast<EGLint>(drm_format_modifier >> 32),
            EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, static_cast<EGLint>(drm_format_modifier & 0xffffffff),
            EGL_DMA_BUF_PLANE0_FD_EXT, fd,
            EGL_DMA_BUF_PLANE0_OFFSET_EXT, static_cast<EGLint>(offset[0]),
            EGL_DMA_BUF_PLANE0_PITCH_EXT, static_cast<EGLint>(pitch[0]),
            EGL_NONE
        };

        EGLImageKHR eglImage = eglCreateImageKHR(eglDisplay, EGL_NO_CONTEXT, EGL_LINUX_DMA_BUF_EXT, nullptr, attribs);
//        std::cout << "eglImage: " << eglImage << std::endl;
        long error = eglGetError();
        if (eglImage == EGL_NO_IMAGE_KHR || error != EGL_SUCCESS) {
            closeDrm(drm);
            return eglResultFailure(env, "eglCreateImageKHR", error);
        }

        GLuint texture;
        glGenTextures(1, &texture);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, eglImage);
//        std::cout << "bound egl image to texture" << std::endl;
        error = glGetError();
        if (error != GL_NO_ERROR) {
            std::cerr << "Failed to bind egl image to texture: " << error << std::endl;
            eglDestroyImageKHR(eglDisplay, eglImage);
            closeDrm(drm);
            return glResultFailure(env, "glEGLImageTargetTexture2DOES", error);
        }
        error = eglGetError();
        if (error != EGL_SUCCESS) {
            eglDestroyImageKHR(eglDisplay, eglImage);
            closeDrm(drm);
            return eglResultFailure(env, "glEGLImageTargetTexture2DOES", error);
        }

        eglVASurface->eglImages[layer] = eglImage;
        eglVASurface->textures[layer] = texture;
    }
//    std::cout << "eglVASurface: " << eglVASurface << std::endl;

    closeDrm(drm);
    glBindTexture(GL_TEXTURE_2D, prevTexture);

    return resultSuccess(env, reinterpret_cast<jlong>(eglVASurface));
}

JNIEXPORT jobjectArray JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_planeTexturesN(JNIEnv *env, jobject thiz, jlong surface) {
    const auto eglVASurface = reinterpret_cast<EGLVASurface *>(surface);
    int planes = 0;
    std::vector<jint> textures{};
    for (const auto &image: eglVASurface->eglImages) {
        if (image != EGL_NO_IMAGE_KHR) {
            textures.push_back(eglVASurface->textures[planes++]);
        }
    }
    const auto texturesArray = env->NewObjectArray(planes, env->FindClass("java/lang/Integer"), boxedInt(env, 0));
    for (int i = 0; i < planes; ++i) {
        env->SetObjectArrayElement(texturesArray, i, boxedInt(env, textures[i]));
    }
    return texturesArray;
}

JNIEXPORT void JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_destroySurfaceN(JNIEnv *env, jobject thiz, jlong surface) {
    if (surface == 0L) return;
    const auto eglVASurface = reinterpret_cast<EGLVASurface *>(surface);
    const auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    if (!eglDestroyImageKHR) {
        std::cerr << "Failed to get eglDestroyImageKHR" << std::endl;
        return;
    }
    for (auto &eglImage: eglVASurface->eglImages) {
        if (eglImage != EGL_NO_IMAGE_KHR) {
            eglDestroyImageKHR(eglGetCurrentDisplay(), eglImage);
        }
    }
    for (auto &texture: eglVASurface->textures) {
        if (texture != 0) {
            glDeleteTextures(1, &texture);
        }
    }
    delete eglVASurface;
}
}
