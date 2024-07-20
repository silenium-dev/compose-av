//
// Created by silenium-dev on 7/15/24.
//

#include "EGL.h"

#include <jni.h>
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <va/va.h>
#include <va/va_drmcommon.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <cstring>
#include <unistd.h>

#define STB_IMAGE_IMPLEMENTATION
#include <stb/stb_image.h>
#undef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include <stb/stb_image_write.h>
#undef STB_IMAGE_WRITE_IMPLEMENTATION

#define CASE_STR(value) case value: return #value;

const char *eglGetErrorString(const long error) {
    switch (error) {
        CASE_STR(EGL_SUCCESS)
        CASE_STR(EGL_NOT_INITIALIZED)
        CASE_STR(EGL_BAD_ACCESS)
        CASE_STR(EGL_BAD_ALLOC)
        CASE_STR(EGL_BAD_ATTRIBUTE)
        CASE_STR(EGL_BAD_CONTEXT)
        CASE_STR(EGL_BAD_CONFIG)
        CASE_STR(EGL_BAD_CURRENT_SURFACE)
        CASE_STR(EGL_BAD_DISPLAY)
        CASE_STR(EGL_BAD_SURFACE)
        CASE_STR(EGL_BAD_MATCH)
        CASE_STR(EGL_BAD_PARAMETER)
        CASE_STR(EGL_BAD_NATIVE_PIXMAP)
        CASE_STR(EGL_BAD_NATIVE_WINDOW)
        CASE_STR(EGL_CONTEXT_LOST)
        default:
            return "Unknown";
    }
}

#undef CASE_STR


typedef void (EGLAPIENTRYP PFNEGLIMAGETARGETTEXTURE2DOESPROC)(EGLenum target, void *image);

extern "C" {
struct EGLVASurface {
    EGLImageKHR eglImage{EGL_NO_IMAGE_KHR};
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

JNIEXPORT jlong JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_getVADisplayN(JNIEnv *env, jobject thiz, const jlong deviceCtx) {
    const auto vaContext = reinterpret_cast<AVVAAPIDeviceContext *>(deviceCtx);
    return reinterpret_cast<jlong>(vaContext->display);
}

JNIEXPORT jlong JNICALL
Java_dev_silenium_multimedia_vaapi_VAKt_createTextureFromSurfaceN(JNIEnv *env, jobject thiz,
                                                                  const jint texture, const jlong vaSurface_,
                                                                  const jlong vaDisplay_, const jlong eglDisplay_) {
    const auto vaDisplay = reinterpret_cast<VADisplay>(vaDisplay_);
    const auto vaSurface = static_cast<VASurfaceID>(vaSurface_);
    const auto eglDisplay = reinterpret_cast<EGLDisplay>(eglDisplay_);
    std::cout << "VASurface: " << vaSurface << std::endl;
    std::cout << "VADisplay: " << vaDisplay << std::endl;
    std::cout << "EGLDisplay: " << eglDisplay << std::endl;

    const auto eglCreateImageKHR = getFunc<PFNEGLCREATEIMAGEKHRPROC>("eglCreateImageKHR");
    if (!eglCreateImageKHR) {
        std::cerr << "Failed to get eglCreateImageKHR" << std::endl;
        return -1;
    }

    const auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    if (!eglDestroyImageKHR) {
        std::cerr << "Failed to get eglDestroyImageKHR" << std::endl;
        return -1;
    }

    const auto glEGLImageTargetTexture2DOES = getFunc<
        PFNEGLIMAGETARGETTEXTURE2DOESPROC>("glEGLImageTargetTexture2DOES");
    if (!glEGLImageTargetTexture2DOES) {
        std::cerr << "Failed to get glEGLImageTargetTexture2DOES" << std::endl;
        return -1;
    }

    VADRMPRIMESurfaceDescriptor drm{0};
    const auto ret = vaExportSurfaceHandle(
        vaDisplay,
        vaSurface,
        VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2,
        VA_EXPORT_SURFACE_READ_WRITE | VA_EXPORT_SURFACE_SEPARATE_LAYERS,
        &drm);
    vaSyncSurface(vaDisplay, vaSurface);
    checkVAError(ret, "export surface as drm");
    std::cout << "drm.num_objects: " << drm.num_objects << std::endl;
    for (int i = 0; i < drm.num_objects; ++i) {
        std::cout << "Exported DRM object " << i << ": fd=" << drm.objects[i].fd << ", extent=" << drm.objects[i].size
                <<
                std::endl;
    }

    const auto [drm_format, num_planes, object_index, offset, pitch] = drm.layers[0];
    const auto obj_idx = object_index[0];
    const auto [fd, size, drm_format_modifier] = drm.objects[obj_idx];

    std::cout << "obj.fd: " << fd << std::endl;
    std::cout << "obj.size: " << size << std::endl;
    std::cout << "layer.drm_format: " << drm_format << std::endl;
    std::cout << "layer.num_planes: " << num_planes << std::endl;
    std::cout << "layer.offset[0]: " << offset[0] << std::endl;
    std::cout << "layer.pitch[0]: " << pitch[0] << std::endl;

    const auto eglGetError = getFunc<PFNEGLGETERRORPROC>("eglGetError");

    const EGLint attribs[] = {
        EGL_WIDTH, static_cast<EGLint>(drm.width),
        EGL_HEIGHT, static_cast<EGLint>(drm.height),
        EGL_LINUX_DRM_FOURCC_EXT, static_cast<EGLint>(drm_format),
        EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT, static_cast<EGLint>(drm_format_modifier >> 32),
        EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, static_cast<EGLint>(drm_format_modifier & 0xffffffff),
        EGL_DMA_BUF_PLANE0_FD_EXT, fd,
        EGL_DMA_BUF_PLANE0_OFFSET_EXT, static_cast<EGLint>(offset[0]),
        EGL_DMA_BUF_PLANE0_PITCH_EXT, static_cast<EGLint>(pitch[0]),
        EGL_NONE
    };
    EGLImageKHR eglImage = eglCreateImageKHR(eglDisplay, EGL_NO_CONTEXT, EGL_LINUX_DMA_BUF_EXT, nullptr, attribs);
    std::cout << "eglImage: " << eglImage << std::endl;
    long error = eglGetError();
    if (eglImage == EGL_NO_IMAGE_KHR || error != EGL_SUCCESS) {
        std::cerr << "Failed to create egl image: " << eglGetErrorString(error) << std::endl;
        closeDrm(drm);
        return -1;
    }

    GLint prevTexture;
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &prevTexture);

    glBindTexture(GL_TEXTURE_2D, texture);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, eglImage);
    std::cout << "bound egl image to texture" << std::endl;
    error = glGetError();
    if (error != GL_NO_ERROR) {
        std::cerr << "Failed to bind egl image to texture: " << error << std::endl;
        eglDestroyImageKHR(eglDisplay, eglImage);
        closeDrm(drm);
        return -1;
    }
    error = eglGetError();
    if (error != EGL_SUCCESS) {
        std::cerr << "Failed to bind egl image to texture: " << eglGetErrorString(error) << std::endl;
        eglDestroyImageKHR(eglDisplay, eglImage);
        closeDrm(drm);
        return -1;
    }

    glBindTexture(GL_TEXTURE_2D, prevTexture);

    const auto data = static_cast<uint8_t *>(mmap(nullptr, size, PROT_READ,
                                                  MAP_SHARED, fd, offset[0]));
    std::cout << "data: " << static_cast<void *>(data) << std::endl;
    if (data == MAP_FAILED) {
        std::cerr << "Failed to map va surface: " << strerror(errno) << std::endl;
        //        glDeleteFramebuffers(1, &fbo);
        eglDestroyImageKHR(eglDisplay, eglImage);
        closeDrm(drm);
        return -1;
    }
    const auto copy = new uint8_t[size];
    std::memcpy(copy, data, size);
    munmap(data, size);

    stbi_write_png("va_surface.png", 1920, 1080, 4, copy, static_cast<int>(pitch[0]));
    std::cout << "written va_surface.png" << std::endl;
    delete[] copy;

    auto eglVASurface = new EGLVASurface();
    eglVASurface->eglImage = eglImage;
    std::cout << "eglVASurface: " << eglVASurface << std::endl;

    closeDrm(drm);

    return reinterpret_cast<jlong>(eglVASurface);
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
    eglDestroyImageKHR(eglGetCurrentDisplay(), eglVASurface->eglImage);
    delete eglVASurface;
}
}
