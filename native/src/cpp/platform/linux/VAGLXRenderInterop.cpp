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
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>
}

// From ffmpeg: libavcodec/vaapi_decode.h
struct VAAPIDecodeContext {
    VAConfigID va_config;
    VAContextID va_context;

    AVHWDeviceContext *device;
    AVVAAPIDeviceContext *hwctx;

    AVHWFramesContext *frames;
    AVVAAPIFramesContext *hwfc;

    enum AVPixelFormat surface_format;
    int surface_count;

    VASurfaceAttrib pixel_format_attribute;
};

// From ffmpeg: libavcodec/internal.h
struct AVCodecInternal {
    /**
     * When using frame-threaded decoding, this field is set for the first
     * worker thread (e.g. to decode extradata just once).
     */
    int is_copy;

    /**
     * Audio encoders can set this flag during init to indicate that they
     * want the small last frame to be padded to a multiple of pad_samples.
     */
    int pad_samples;

    struct FramePool *pool;

    void *thread_ctx;

    /**
     * This packet is used to hold the packet given to decoders
     * implementing the .decode API; it is unused by the generic
     * code for decoders implementing the .receive_frame API and
     * may be freely used (but not freed) by them with the caveat
     * that the packet will be unreferenced generically in
     * avcodec_flush_buffers().
     */
    AVPacket *in_pkt;
    struct AVBSFContext *bsf;

    /**
     * Properties (timestamps+side data) extracted from the last packet passed
     * for decoding.
     */
    AVPacket *last_pkt_props;

    /**
     * temporary buffer used for encoders to store their bitstream
     */
    uint8_t *byte_buffer;
    unsigned int byte_buffer_size;

    void *frame_thread_encoder;

    /**
     * The input frame is stored here for encoders implementing the simple
     * encode API.
     *
     * Not allocated in other cases.
     */
    AVFrame *in_frame;

    /**
     * When the AV_CODEC_FLAG_RECON_FRAME flag is used. the encoder should store
     * here the reconstructed frame corresponding to the last returned packet.
     *
     * Not allocated in other cases.
     */
    AVFrame *recon_frame;

    /**
     * If this is set, then FFCodec->close (if existing) needs to be called
     * for the parent AVCodecContext.
     */
    int needs_close;

    /**
     * Number of audio samples to skip at the start of the next decoded frame
     */
    int skip_samples;

    /**
     * hwaccel-specific private data
     */
    void *hwaccel_priv_data;

    /**
     * checks API usage: after codec draining, flush is required to resume operation
     */
    int draining;

    /**
     * Temporary buffers for newly received or not yet output packets/frames.
     */
    AVPacket *buffer_pkt;
    AVFrame *buffer_frame;
    int draining_done;

#if FF_API_DROPCHANGED
    /* used when avctx flag AV_CODEC_FLAG_DROPCHANGED is set */
    int changed_frames_dropped;
    int initial_format;
    int initial_width, initial_height;
    int initial_sample_rate;
    AVChannelLayout initial_ch_layout;
#endif

#if CONFIG_LCMS2
    FFIccContext icc; /* used to read and write embedded ICC profiles */
#endif

    /**
     * Set when the user has been warned about a failed allocation from
     * a fixed frame pool.
     */
    int warned_on_failed_allocation_from_fixed_pool;
};

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
    std::cout << "VA Surface: " << vaSurface << std::endl;
    const auto frame = reinterpret_cast<AVFrame *>(frame_);
    const auto codecContext = reinterpret_cast<AVCodecContext *>(codecContext_);
    const auto vaapiContext = reinterpret_cast<VAAPIDecodeContext *>(codecContext->internal->hwaccel_priv_data);
    std::cout << "Priv Data: " << vaapiContext << std::endl;
    std::cout << "VA Context: " << vaapiContext->va_context << std::endl;
    std::cout << "VA surface_count: " << vaapiContext->surface_count << std::endl;
    std::cout << "VA pixel_format_attribute: " << vaapiContext->pixel_format_attribute.value.value.i << std::endl;
    //    std::cout << "VASurface: " << vaSurface << std::endl;
    //    std::cout << "VADisplay: " << vaDisplay << std::endl;

    std::vector<VASurfaceAttrib>
            rgbAttribs{
                    {
                            .type = VASurfaceAttribPixelFormat,
                            .flags = VA_SURFACE_ATTRIB_SETTABLE,
                            .value = {
                                    .type = VAGenericValueTypeInteger,
                                    .value = {.i = VA_FOURCC_BGRX},
                            },
                    },
            };

    VAImage vaImage{};
    auto ret = vaDeriveImage(vaDisplay, vaSurface, &vaImage);
    if (ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaDeriveImage", ret);
    }
    std::cout << "VA Image: " << vaImage.image_id << std::endl;
    const auto width = vaImage.width;
    const auto height = vaImage.height;
    const auto num_planes = vaImage.num_planes;
    unsigned int pitches[3];
    unsigned int offsets[3];
    memcpy(pitches, vaImage.pitches, sizeof(pitches));
    memcpy(offsets, vaImage.offsets, sizeof(offsets));
    vaDestroyImage(vaDisplay, vaImage.image_id);

    vaSyncSurface(vaDisplay, vaSurface);
    std::cout << "Width: " << width << std::endl;
    std::cout << "Height: " << height << std::endl;

    const auto alignedWidth = width + (16 - width % 16);
    const auto alignedHeight = height + (16 - height % 16);
    VASurfaceID rgbSurface{};
    ret = vaCreateSurfaces(vaDisplay, VA_RT_FORMAT_RGB32, alignedWidth, alignedHeight, &rgbSurface, 1, rgbAttribs.data(), rgbAttribs.size());
    if (ret != VA_STATUS_SUCCESS) {
        return vaResultFailure(env, "vaCreateSurfaces", ret);
    }
    std::cout << "RGB Surface: " << rgbSurface << std::endl;

    VAContextID vaContextID{};
    ret = vaCreateContext(vaDisplay, vaapiContext->va_config, width, height, VA_PROGRESSIVE, &rgbSurface, 1, &vaContextID);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaCreateContext", ret);
    }
    std::cout << "VA Context ID: " << vaContextID << std::endl;

    ret = vaBeginPicture(vaDisplay, vaContextID, rgbSurface);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaBeginPicture", ret);
    }
    std::cout << "Begin Picture" << std::endl;
    VABufferID vaBuffer{};
    ret = vaCreateBuffer(vaDisplay, vaContextID, VABufferType::VAProcPipelineParameterBufferType, sizeof(VAProcPipelineParameterBuffer), 1, nullptr, &vaBuffer);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaCreateBuffer", ret);
    }
    std::cout << "Create Buffer" << std::endl;
    VAProcPipelineParameterBuffer *pipelineParameterBuffer;
    ret = vaMapBuffer(vaDisplay, vaBuffer, reinterpret_cast<void **>(&pipelineParameterBuffer));
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroyBuffer(vaDisplay, vaBuffer);
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaMapBuffer", ret);
    }
    pipelineParameterBuffer->surface = vaSurface;
    pipelineParameterBuffer->surface_region = nullptr;
    pipelineParameterBuffer->output_region = nullptr;
    pipelineParameterBuffer->filter_flags = VA_FILTER_SCALING_FAST;
    pipelineParameterBuffer->filters = nullptr;
    vaUnmapBuffer(vaDisplay, vaBuffer);
    std::cout << "Configured Buffer" << std::endl;

    ret = vaRenderPicture(vaDisplay, vaContextID, &vaBuffer, 1);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroyBuffer(vaDisplay, vaBuffer);
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaRenderPicture", ret);
    }
    std::cout << "Render Picture" << std::endl;
    ret = vaEndPicture(vaDisplay, vaContextID);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroyBuffer(vaDisplay, vaBuffer);
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaEndPicture", ret);
    }
    std::cout << "End Picture" << std::endl;
    vaDestroyBuffer(vaDisplay, vaBuffer);
    vaDestroyContext(vaDisplay, vaContextID);

    void *glxSurface{};
    GLuint texture{};
    glGenTextures(1, &texture);
    ret = vaCreateSurfaceGLX(vaDisplay, GL_TEXTURE_2D, texture, &glxSurface);
    if (ret != VA_STATUS_SUCCESS) {
        vaDestroySurfaces(vaDisplay, &rgbSurface, 1);
        return vaResultFailure(env, "vaCreateSurfaceGLX", ret);
    }

    const auto interopImage = new VAGLXInteropImage(vaDisplay, rgbSurface, glxSurface, texture, Swizzles::Identity);

    return resultSuccess(env, reinterpret_cast<jlong>(interopImage));
}
}
