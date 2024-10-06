//
// Created by silenium-dev on 7/29/24.
//

#ifndef VA_HPP
#define VA_HPP

extern "C" {
#include <libavutil/frame.h>
}
#include <va/va_drmcommon.h>

struct Result {
    int code;
    const char *message;
};

void closeDrm(const VADRMPRIMESurfaceDescriptor &drm);
Result mapFrameToDifferentContext(AVFrame *dst, const AVFrame *src, AVBufferRef *targetContext);

#endif //VA_HPP
