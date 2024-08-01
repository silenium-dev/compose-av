//
// Created by silenium-dev on 8/1/24.
//

#include <fcntl.h>
#include <jni.h>
#include <unistd.h>

#include "helper/errors.hpp"
#include <iostream>
#include <va/va_drm.h>
#include <va/va_x11.h>

extern "C" {
#include <libavutil/hwcontext.h>
#include <libavutil/hwcontext_vaapi.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDeviceContextKt_createDrmN(JNIEnv *env, jclass clazz, jstring _device) {
    AVBufferRef *deviceRef;

    const auto device = env->GetStringUTFChars(_device, nullptr);
    auto ret = av_hwdevice_ctx_create(&deviceRef, AV_HWDEVICE_TYPE_VAAPI, device, nullptr, 0);
    env->ReleaseStringUTFChars(_device, device);
    if (ret < 0) {
        return avResultFailure(env, "initializing hw device context", ret);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(deviceRef));
}

JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDeviceContextKt_createGlxN(JNIEnv *env, jclass clazz, jlong glxDisplay) {
    const auto display = reinterpret_cast<Display *>(glxDisplay);
    const auto vaDisplay = vaGetDisplay(display);
    if (vaDisplay == nullptr) {
        return avResultFailure(env, "getting VA display", AVERROR_UNKNOWN);
    }

    int majorVersion, minorVersion;
    if (vaInitialize(vaDisplay, &majorVersion, &minorVersion) != VA_STATUS_SUCCESS) {
        return avResultFailure(env, "initializing VA display", AVERROR_UNKNOWN);
    }
    std::cout << "VA-API version: " << majorVersion << "." << minorVersion << std::endl;

    auto deviceRef = av_hwdevice_ctx_alloc(AV_HWDEVICE_TYPE_VAAPI);
    if (deviceRef == nullptr) {
        return avResultFailure(env, "allocating hw device context", AVERROR(ENOMEM));
    }

    const auto vaapi = static_cast<AVVAAPIDeviceContext *>(reinterpret_cast<AVHWDeviceContext *>(deviceRef->data)->hwctx);
    vaapi->display = vaDisplay;

    auto ret = av_hwdevice_ctx_init(deviceRef);
    if (ret < 0) {
        av_buffer_unref(&deviceRef);
        return avResultFailure(env, "initializing hw device context", ret);
    }

    return resultSuccess(env, reinterpret_cast<jlong>(deviceRef));
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_platform_linux_VaapiDeviceContextKt_getDisplayN(JNIEnv *env, jclass clazz, const jlong _deviceRef) {
    const auto deviceRef = reinterpret_cast<AVBufferRef *>(_deviceRef);
    return reinterpret_cast<jlong>(static_cast<AVVAAPIDeviceContext *>(reinterpret_cast<AVHWDeviceContext *>(deviceRef->data)->hwctx)->display);
}
}
