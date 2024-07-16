//
// Created by silenium-dev on 7/15/24.
//

#include "EGL.h"

#include <jni.h>
#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <va/va.h>
#include <va/va_drm.h>
#include <va/va_drmcommon.h>
#include <drm_fourcc.h>
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

#include <stb/stb_image_write.h>

#define CASE_STR(value) case value: return #value;

const char *eglGetErrorString(long error) {
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
#include <libavutil/avutil.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>

struct EGLVASurface {
    EGLImageKHR eglImage{EGL_NO_IMAGE_KHR};
    VADRMPRIMESurfaceDescriptor drm{};
    AVFrame *hwFrame{nullptr};
    AVBufferRef *hwFramesRef{nullptr}, *drmFramesRef{nullptr};
    AVBufferRef *hwDeviceRef{nullptr}, *drmDeviceRef{nullptr};
};

void checkAVError(int ret, const std::string &operation = "libav call") {
    if (ret < 0) {
        char buf[AV_ERROR_MAX_STRING_SIZE];
        av_make_error_string(buf, AV_ERROR_MAX_STRING_SIZE, ret);
        throw std::runtime_error{buf};
    }
}

void checkVAError(int ret, const std::string &operation = "libva call") {
    if (ret != VA_STATUS_SUCCESS) {
        throw std::runtime_error{vaErrorStr(ret)};
    }
}

AVFrame *convert(uint8_t *pixels, int width, int height, AVPixelFormat from, AVPixelFormat to) {
    auto srcFrame = av_frame_alloc();
    srcFrame->width = width;
    srcFrame->height = height;
    srcFrame->format = from;
    av_frame_get_buffer(srcFrame, 0);
    av_image_fill_arrays(srcFrame->data, srcFrame->linesize, pixels, from, width, height, 0);
    srcFrame->linesize[0] = width * 4;

    stbi_write_png("rgb.png", width, height, 4, srcFrame->data[0], srcFrame->linesize[0]);

    auto dstFrame = av_frame_alloc();
    dstFrame->width = width;
    dstFrame->height = height;
    dstFrame->format = to;
    av_frame_get_buffer(dstFrame, 0);
    auto swsContext = sws_getContext(width, height, from, width, height, to, SWS_BICUBIC, nullptr, nullptr, nullptr);
    sws_scale(swsContext, srcFrame->data, srcFrame->linesize, 0, height, dstFrame->data, dstFrame->linesize);
    sws_freeContext(swsContext);

    stbi_write_png("nv12.png", width, height, 4, dstFrame->data[0], dstFrame->linesize[0]);

    av_frame_free(&srcFrame);
    return dstFrame;
}

JNIEXPORT jlong JNICALL
Java_dev_silenium_va_VA_createTextureFromSurface(JNIEnv *env, jobject thiz, jint texture) {
    auto eglGetCurrentDisplay = getFunc<PFNEGLGETCURRENTDISPLAYPROC>("eglGetCurrentDisplay");
    if (!eglGetCurrentDisplay) {
        std::cerr << "Failed to get eglGetCurrentDisplay" << std::endl;
        return -1;
    }
    auto eglDisplay = eglGetCurrentDisplay();
    if (eglDisplay == EGL_NO_DISPLAY) {
        std::cerr << "Failed to get current egl display" << std::endl;
        return -1;
    }

    auto eglCreateImageKHR = getFunc<PFNEGLCREATEIMAGEKHRPROC>("eglCreateImageKHR");
    if (!eglCreateImageKHR) {
        std::cerr << "Failed to get eglCreateImageKHR" << std::endl;
        return -1;
    }

    auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    if (!eglDestroyImageKHR) {
        std::cerr << "Failed to get eglDestroyImageKHR" << std::endl;
        return -1;
    }

    int image_width, image_height, image_channels;
    const auto pixels = stbi_load("image.png", &image_width, &image_height, &image_channels, STBI_rgb_alpha);

    AVBufferRef *deviceContextRef;
    auto ret = av_hwdevice_ctx_create(&deviceContextRef, AV_HWDEVICE_TYPE_VAAPI, nullptr, nullptr, 0);
    checkAVError(ret, "create hw device context");

    auto rgb0 = convert(pixels, image_width, image_height, AV_PIX_FMT_RGBA, AV_PIX_FMT_RGB0);
    stbi_image_free(pixels);

    auto framesContextRef = av_hwframe_ctx_alloc(deviceContextRef);
    auto *framesContext = reinterpret_cast<AVHWFramesContext *>(framesContextRef->data);
    framesContext->format = AV_PIX_FMT_VAAPI;
    framesContext->sw_format = AV_PIX_FMT_RGB0;
    framesContext->width = image_width;
    framesContext->height = image_height;
    framesContext->initial_pool_size = 2;
    ret = av_hwframe_ctx_init(framesContextRef);
    checkAVError(ret, "create hw frames context");
    std::cout << "image size: " << image_width << "x" << image_height << std::endl;

    auto frame = av_frame_alloc();
    ret = av_hwframe_get_buffer(framesContextRef, frame, 0);
    checkAVError(ret, "allocate hw frame");
    VASurfaceID vaSurface = reinterpret_cast<uintptr_t>(frame->data[3]);
    std::cout << "VASurface: " << vaSurface << std::endl;

    const auto vaDeviceContext = reinterpret_cast<AVVAAPIDeviceContext *>(reinterpret_cast<AVHWFramesContext *>(frame->hw_frames_ctx->data)->device_ctx->hwctx);
    const auto vaDisplay = vaDeviceContext->display;
    std::cout << "VADisplay: " << vaDisplay << std::endl;

    VADRMPRIMESurfaceDescriptor drm{0};
    ret = vaExportSurfaceHandle(
            vaDisplay,
            vaSurface,
            VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2,
            VA_EXPORT_SURFACE_READ_WRITE | VA_EXPORT_SURFACE_SEPARATE_LAYERS,
            &drm);
    vaSyncSurface(vaDisplay, vaSurface);
    checkVAError(ret, "export surface as drm");
    std::cout << "Exported DRM object: fd=" << drm.objects[0].fd << ", extent=" << drm.objects[0].size << std::endl;

    ret = av_hwframe_transfer_data(frame, rgb0, 0);
    checkAVError(ret, "transfer data to hw frame");
    std::cout << "transferred data to hw frame" << std::endl;

    auto swFrame = av_frame_alloc();
    swFrame->format = framesContext->sw_format;
    ret = av_hwframe_transfer_data(swFrame, frame, 0);
    checkAVError(ret, "transfer data to sw frame");
    std::cout << "transferred data to sw frame" << std::endl;

    stbi_write_png("output.png", swFrame->width, swFrame->height, 1, swFrame->data[0], swFrame->linesize[0]);
    av_frame_free(&swFrame);
    std::cout << "saved data from sw frame" << std::endl;

    printf("hwFrame->data[3]: %p\n", frame->data[3]);
    fflush(stdout);

    av_frame_free(&swFrame);
    av_frame_free(&rgb0);

    std::cout << "Freed sw frame and rgb0" << std::endl;

    std::cout << "drm.num_objects: " << drm.num_objects << std::endl;

    const auto layer = drm.layers[0];
    const auto obj_idx = layer.object_index[0];
    const auto obj = drm.objects[obj_idx];

    std::cout << "obj.fd: " << obj.fd << std::endl;
    std::cout << "obj.size: " << obj.size << std::endl;
    std::cout << "layer.drm_format: " << layer.drm_format << std::endl;
    std::cout << "layer.num_planes: " << layer.num_planes << std::endl;
    std::cout << "layer.offset[0]: " << layer.offset[0] << std::endl;
    std::cout << "layer.pitch[0]: " << layer.pitch[0] << std::endl;

//    auto data = reinterpret_cast<uint8_t *>(mmap(nullptr, obj.size, PROT_READ | PROT_WRITE,
//                                                 MAP_SHARED, obj.fd, layer.offset[0]));
//    std::cout << "data: " << (void *) data << std::endl;
//    if (data == MAP_FAILED) {
//        std::cerr << "Failed to map va surface: " << strerror(errno) << std::endl;
//        vaDestroySurfaces(vaDpy, vaSurface, 1);
//        vaTerminate(vaDpy);
//        return -1;
//    }
//
//    for (int i = 0; i < obj.size; ++i) {
//        data[i] = 0xff;
//    }
//
//    munmap(data, obj.size);

    auto eglGetError = getFunc<PFNEGLGETERRORPROC>("eglGetError");

    EGLint attribs[] = {
            EGL_WIDTH, image_width,
            EGL_HEIGHT, image_height,
            EGL_LINUX_DRM_FOURCC_EXT, static_cast<EGLint>(layer.drm_format),
            EGL_DMA_BUF_PLANE0_MODIFIER_HI_EXT, static_cast<EGLint>(obj.drm_format_modifier >> 32),
            EGL_DMA_BUF_PLANE0_MODIFIER_LO_EXT, static_cast<EGLint>(obj.drm_format_modifier & 0xffffffff),
            EGL_DMA_BUF_PLANE0_FD_EXT, obj.fd,
            EGL_DMA_BUF_PLANE0_OFFSET_EXT, static_cast<EGLint>(layer.offset[0]),
            EGL_DMA_BUF_PLANE0_PITCH_EXT, static_cast<EGLint>(layer.pitch[0]),
            EGL_NONE
    };
    EGLImageKHR eglImage = eglCreateImageKHR(eglDisplay, EGL_NO_CONTEXT, EGL_LINUX_DMA_BUF_EXT, nullptr, attribs);
    std::cout << "eglImage: " << eglImage << std::endl;
    long error = eglGetError();
    if (eglImage == EGL_NO_IMAGE_KHR || error != EGL_SUCCESS) {
        std::cerr << "Failed to create egl image: " << eglGetErrorString(error) << std::endl;
        av_frame_free(&frame);
        av_buffer_unref(&framesContextRef);
        av_buffer_unref(&deviceContextRef);
        return -1;
    }

    // use egl image as texture
    glBindTexture(GL_TEXTURE_2D, texture);
    auto glEGLImageTargetTexture2DOES = getFunc<PFNEGLIMAGETARGETTEXTURE2DOESPROC>("glEGLImageTargetTexture2DOES");
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, eglImage);
    std::cout << "bound egl image to texture" << std::endl;
    error = glGetError();
    if (error != GL_NO_ERROR) {
        std::cerr << "Failed to bind egl image to texture: " << error << std::endl;
        eglDestroyImageKHR(eglDisplay, eglImage);
        av_frame_free(&frame);
        av_buffer_unref(&framesContextRef);
        av_buffer_unref(&deviceContextRef);
        return -1;
    }
    error = eglGetError();
    if (error != EGL_SUCCESS) {
        std::cerr << "Failed to bind egl image to texture: " << eglGetErrorString(error) << std::endl;
        eglDestroyImageKHR(eglDisplay, eglImage);
        av_frame_free(&frame);
        av_buffer_unref(&framesContextRef);
        av_buffer_unref(&deviceContextRef);
        return -1;
    }

//    GLint previousDrawFbo = 0, previousReadFbo = 0;
//    glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &previousDrawFbo);
//    glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &previousReadFbo);
//
//    GLuint fbo = 0;
//    glGenFramebuffers(1, &fbo);
//    glBindFramebuffer(GL_FRAMEBUFFER, fbo);
//    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
//    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
//        std::cerr << "Failed to create framebuffer" << std::endl;
//        glDeleteFramebuffers(1, &fbo);
//        eglDestroyImageKHR(eglDisplay, eglImage);
//        av_frame_free(&hwFrame);
//        av_frame_free(&swFrame);
//        av_buffer_unref(&hwFramesRef);
//        av_buffer_unref(&hwDeviceRef);
//        return -1;
//    }
//
//    glClearColor(0.f, 0.f, 1.f, 1.f);
//    glClear(GL_COLOR_BUFFER_BIT);
//    glFinish();
//
//    auto pixels = new uint8_t[1920 * 1080 * 4];
//    glReadPixels(0, 0, 1920, 1080, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
//    stbi_write_png("texture.png", 1920, 1080, 4, pixels, 1920 * 4);
//    delete[] pixels;
//
//    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousDrawFbo);
//    glBindFramebuffer(GL_READ_FRAMEBUFFER, previousReadFbo);
//
//    glDeleteFramebuffers(1, &fbo);

    auto data = reinterpret_cast<uint8_t *>(mmap(nullptr, obj.size, PROT_READ,
                                                 MAP_SHARED, obj.fd, layer.offset[0]));
    std::cout << "data: " << (void *) data << std::endl;
    if (data == MAP_FAILED) {
        std::cerr << "Failed to map va surface: " << strerror(errno) << std::endl;
//        glDeleteFramebuffers(1, &fbo);
        eglDestroyImageKHR(eglDisplay, eglImage);
        av_frame_free(&frame);
        av_buffer_unref(&framesContextRef);
        av_buffer_unref(&deviceContextRef);
        return -1;
    }
    auto copy = new uint8_t[obj.size];
    std::memcpy(copy, data, obj.size);
    munmap(data, obj.size);

    stbi_write_png("va_surface.png", 1920, 1080, 4, copy, layer.pitch[0]);
    std::cout << "written va_surface.png" << std::endl;
    delete[] copy;

    auto eglVASurface = new EGLVASurface();
    eglVASurface->eglImage = eglImage;
    eglVASurface->hwFrame = frame;
    eglVASurface->drm = drm;
    eglVASurface->hwFramesRef = framesContextRef;
    eglVASurface->hwDeviceRef = deviceContextRef;

    for (int i = 0; i < drm.num_objects; ++i) {
        close(drm.objects[i].fd);
    }

    std::cout << "eglVASurface: " << eglVASurface << std::endl;

    return reinterpret_cast<jlong>(eglVASurface);
}

JNIEXPORT void JNICALL Java_dev_silenium_va_VA_destroySurface(JNIEnv *env, jobject thiz, jlong surface) {
    auto eglGetCurrentDisplay = getFunc<PFNEGLGETCURRENTDISPLAYPROC>("eglGetCurrentDisplay");
    if (!eglGetCurrentDisplay) {
        return;
    }
    auto eglGetCurrentContext = getFunc<PFNEGLGETCURRENTCONTEXTPROC>("eglGetCurrentContext");
    if (!eglGetCurrentContext) {
        return;
    }
    auto eglDestroyImageKHR = getFunc<PFNEGLDESTROYIMAGEKHRPROC>("eglDestroyImageKHR");
    if (!eglDestroyImageKHR) {
        return;
    }

    auto eglDisplay = eglGetCurrentDisplay();
    if (eglDisplay == EGL_NO_DISPLAY) {
        return;
    }
    auto eglContext = eglGetCurrentContext();
    if (eglContext == EGL_NO_CONTEXT) {
        return;
    }

    auto eglVASurface = reinterpret_cast<EGLVASurface *>(surface);
    if (!eglVASurface) {
        return;
    }
    if (eglVASurface->eglImage != EGL_NO_IMAGE_KHR) {
        eglDestroyImageKHR(eglDisplay, eglVASurface->eglImage);
    }
    if (eglVASurface->hwFrame) {
        av_frame_free(&eglVASurface->hwFrame);
    }
    if (eglVASurface->hwFramesRef) {
        av_buffer_unref(&eglVASurface->hwFramesRef);
    }
    if (eglVASurface->hwDeviceRef) {
        av_buffer_unref(&eglVASurface->hwDeviceRef);
    }
    delete eglVASurface;
}
}
