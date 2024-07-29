//
// Created by silenium-dev on 7/29/24.
//

#include <iostream>
#include <vector>
#include <X11/Xlib.h>
#include <va/va.h>
#include <va/va_glx.h>
#include <va/va_x11.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/hwcontext_vaapi.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
}

int main(int argc, char *argv[]) {
    const auto display = XOpenDisplay(nullptr);
    AVBufferRef *deviceRef = av_hwdevice_ctx_alloc(AV_HWDEVICE_TYPE_VAAPI);
    const auto vaDisplay = vaGetDisplayGLX(display);
    if (!vaDisplay) {
        std::cerr << "Failed to get VA display" << std::endl;
        return -1;
    }
    int major, minor;
    if (const auto ret = vaInitialize(vaDisplay, &major, &minor); ret != VA_STATUS_SUCCESS) {
        std::cerr << "Failed to initialize VA display" << std::endl;
        return -1;
    }
    std::cout << "VA API version: " << major << "." << minor << std::endl;
    auto formatCount = vaMaxNumImageFormats(vaDisplay);
    if (formatCount <= 0) {
        std::cerr << "Failed to get image formats" << std::endl;
        return -1;
    }

    std::vector<VAImageFormat> imageFormats{static_cast<size_t>(formatCount)};
    if (const auto ret = vaQueryImageFormats(vaDisplay, imageFormats.data(), &formatCount); ret != VA_STATUS_SUCCESS) {
        std::cerr << "Failed to query image formats" << std::endl;
        return -1;
    }

    av_log_set_level(AV_LOG_VERBOSE);
    static_cast<AVVAAPIDeviceContext*>(reinterpret_cast<AVHWDeviceContext*>(deviceRef->data)->hwctx)->display = vaDisplay;
    //    std::cout << "Device: " << deviceRef << std::endl;
    if (const auto ret = av_hwdevice_ctx_init(deviceRef); ret < 0) {
        //        printf("Failed to create hw device context: %s\n", av_err2str(ret));
        // fflush(stdout);
        std::cerr << "Failed to create hw device context: " << av_err2str(ret) << std::endl;
        return -1;
    }

    std::cout << "Device: " << deviceRef << std::endl;

    return 0;
}
