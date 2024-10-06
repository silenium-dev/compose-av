//
// Created by silenium-dev on 7/29/24.
//

#include "va.hpp"

#include <iostream>
#include <unistd.h>

extern "C" {
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>
#include <libavutil/pixdesc.h>
}

void closeDrm(const VADRMPRIMESurfaceDescriptor &drm) {
    for (int i = 0; i < drm.num_objects; ++i) {
        close(drm.objects[i].fd);
    }
}

/**
 *
 * @param dst target frame, freshly allocated or will be unreffed before usage
 * @param src source frame
 * @param targetContext target context
 * @return result
 */
Result mapFrameToDifferentContext(AVFrame *dst, const AVFrame *src, AVBufferRef *targetContext) {
    const auto sourceFrames = reinterpret_cast<AVHWFramesContext *>(src->hw_frames_ctx->data);
    const auto sourceDevice = sourceFrames->device_ctx;
    const auto sourceVaDevice = static_cast<AVVAAPIDeviceContext *>(sourceDevice->hwctx);
    const auto targetFrames = reinterpret_cast<AVHWFramesContext *>(targetContext->data);
    const auto targetDevice = targetFrames->device_ctx;
    const auto targetVaDevice = static_cast<AVVAAPIDeviceContext *>(targetDevice->hwctx);
    if (sourceVaDevice->display != targetVaDevice->display) {
        return {AVERROR(ENOTSUP), "source and target devices are not the same"};
    }

    av_frame_unref(dst);
    auto ret = av_hwframe_get_buffer(targetContext, dst, 0);
    if (ret < 0) {
        return {ret, "getting hw frame buffer"};
    }
    ret = av_hwframe_map(dst, src, AV_HWFRAME_MAP_READ);
    if (ret < 0) {
        return {ret, "mapping frame"};
    }
    // const VASurfaceID srcSurface = reinterpret_cast<intptr_t>(src->data[3]);
    // const VASurfaceID dstSurface = reinterpret_cast<intptr_t>(dst->data[3]);
    //
    // std::cout << "Copying surface " << srcSurface << " to " << dstSurface << std::endl;
    // std::cout << "Source display: " << sourceVaDevice->display
    //           << ", target display: " << targetVaDevice->display
    //           << std::endl;
    // std::cout << "Source format: " << av_get_pix_fmt_name(sourceFrames->sw_format)
    //           << ", target format: " << av_get_pix_fmt_name(targetFrames->sw_format)
    //           << std::endl;
    //
    // VACopyObject srcObject{
    //         .obj_type = VACopyObjectSurface,
    //         .object = {
    //                 .surface_id = srcSurface,
    //         },
    // };
    // VACopyObject dstObject{
    //         .obj_type = VACopyObjectSurface,
    //         .object = {
    //                 .surface_id = dstSurface,
    //         },
    // };
    // ret = vaCopy(targetVaDevice->display, &srcObject, &dstObject, VACopyOption{});
    // if (ret != VA_STATUS_SUCCESS) {
    //     std::cout << "vaCopy failed: " << vaErrorStr(ret) << std::endl;
    //     return {AVERROR(EIO), vaErrorStr(ret)};
    // }
    // ret = vaSyncSurface(targetVaDevice->display, dstSurface);
    // if (ret != VA_STATUS_SUCCESS) {
    //     std::cout << "vaSyncSurface failed: " << vaErrorStr(ret) << std::endl;
    //     return {AVERROR(EIO), vaErrorStr(ret)};
    // }
    return {0, nullptr};
}
