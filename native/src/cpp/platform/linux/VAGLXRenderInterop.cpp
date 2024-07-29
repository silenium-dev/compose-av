//
// Created by silenium-dev on 7/15/24.
//

#include "VAGLXInteropImage.hpp"
#include "helper/errors.hpp"
#include "render/GLInteropImage.hpp"

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GL/gl.h>
#include <GL/glx.h>
#include <algorithm>
#include <drm_fourcc.h>
#include <jni.h>
#include <map>
#include <unistd.h>
#include <va/va_drmcommon.h>
#include <va/va_glx.h>
#include <va/va_x11.h>
#include <vector>
#include <va/va_backend.h>
#include <va/va_backend_glx.h>

#include "helper/drm_mapping.hpp"
#include "helper/va.hpp"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>
}

extern "C" {
JNIEXPORT jlong JNICALL
Java_dev_silenium_compose_av_platform_linux_VAGLXRenderInteropKt_getVADisplayN(
    JNIEnv *env, jobject thiz, const jlong frame) {
    const auto avFrame = reinterpret_cast<AVFrame *>(frame);
    const auto deviceCtx = reinterpret_cast<AVHWFramesContext *>(avFrame->hw_frames_ctx->data)->device_ctx;
    const auto vaContext = static_cast<AVVAAPIDeviceContext *>(deviceCtx->hwctx);
    return reinterpret_cast<jlong>(vaContext->display);
}

JNIEXPORT jobject JNICALL
Java_dev_silenium_compose_av_platform_linux_VAGLXRenderInteropKt_mapN(JNIEnv *env, jobject thiz,
                                                                      const jlong vaSurface_, const jlong vaDisplay_,
                                                                      const jlong frame_, const jlong codecContext_) {
    const auto vaDisplay = reinterpret_cast<VADisplay>(vaDisplay_);
    const auto vaSurface = static_cast<VASurfaceID>(vaSurface_);
    const auto frame = reinterpret_cast<AVFrame *>(frame_);
    std::cout << "VA Surface: " << vaSurface << std::endl;

    auto ret = vaSyncSurface(vaDisplay, vaSurface);
    if (ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaSyncSurface", ret);
    }

    //   struct VAOpenGLVTable {
    //     PFNGLXCREATEPIXMAPPROC              glx_create_pixmap;
    //     PFNGLXDESTROYPIXMAPPROC             glx_destroy_pixmap;
    //     PFNGLXBINDTEXIMAGEEXTPROC           glx_bind_tex_image;
    //     PFNGLXRELEASETEXIMAGEEXTPROC        glx_release_tex_image;
    //     PFNGLGENFRAMEBUFFERSEXTPROC         gl_gen_framebuffers;
    //     PFNGLDELETEFRAMEBUFFERSEXTPROC      gl_delete_framebuffers;
    //     PFNGLBINDFRAMEBUFFEREXTPROC         gl_bind_framebuffer;
    //     PFNGLGENRENDERBUFFERSEXTPROC        gl_gen_renderbuffers;
    //     PFNGLDELETERENDERBUFFERSEXTPROC     gl_delete_renderbuffers;
    //     PFNGLBINDRENDERBUFFEREXTPROC        gl_bind_renderbuffer;
    //     PFNGLRENDERBUFFERSTORAGEEXTPROC     gl_renderbuffer_storage;
    //     PFNGLFRAMEBUFFERRENDERBUFFEREXTPROC gl_framebuffer_renderbuffer;
    //     PFNGLFRAMEBUFFERTEXTURE2DEXTPROC    gl_framebuffer_texture_2d;
    //     PFNGLCHECKFRAMEBUFFERSTATUSEXTPROC  gl_check_framebuffer_status;
    // };
    //
    // struct VADriverContextGLX {
    //     struct VADriverVTableGLX    vtable;
    //     struct VAOpenGLVTable       gl_vtable;
    //     unsigned int                is_initialized  : 1;
    // };
    //
    auto displayContext = static_cast<VADisplayContextP>(vaDisplay);
    auto driverContext = static_cast<VADriverContextP>(displayContext->pDriverContext);
    // auto glxContext = static_cast<VADriverContextGLX*>(driverContext->glx);
    // std::cout << "glx initialized: " << glxContext->is_initialized << std::endl;

    // auto pixmap = XCreatePixmap(
        // glXGetCurrentDisplay(),
        // XRootWindow(glXGetCurrentDisplay(), XDefaultScreen(glXGetCurrentDisplay())),
        // frame->width,
        // frame->height,
        // 24
    // );
    std::cout << "Drawable: " << glXGetCurrentDrawable() << std::endl;
    ret = vaPutSurface(vaDisplay, vaSurface, glXGetCurrentDrawable(),
                       0, 0, frame->width, frame->height,
                       0, 0, frame->width, frame->height,
                       nullptr, 0, 0);

    // TODO: Copy from https://github.com/intel/libva/blob/master/va/glx/va_glx_impl.c#L1050
    // TODO: Try va surface -> EGL image -> render to fbo with bound egl image wrapped pixmap -> bind as texture in glx context

    // std::cout << "Pixmap: " << pixmap << std::endl;
    if (ret != VA_STATUS_SUCCESS) {
        // XFreePixmap(glXGetCurrentDisplay(), pixmap);
        return vaResultFailure(env, "vaPutSurface", ret);
    }
    // if (pixmap != None) {
        // XFreePixmap(glXGetCurrentDisplay(), pixmap);
    // }

    void *glxSurface{};
    GLuint texture{};
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WIDTH, frame->width);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_HEIGHT, frame->height);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BORDER, 0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_INTERNAL_FORMAT, GL_RGBA);
    ret = vaCreateSurfaceGLX(vaDisplay, GL_TEXTURE_2D, texture, &glxSurface);
    if (ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaCreateSurfaceGLX", ret);
    }

    ret = vaCopySurfaceGLX(vaDisplay, glxSurface, vaSurface, 0);
    if (ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaCopySurfaceGLX", ret);
    }

    const auto interopImage = new VAGLXInteropImage(vaDisplay, glxSurface, texture, {});

    return resultSuccess(env, reinterpret_cast<jlong>(interopImage));
}
}
