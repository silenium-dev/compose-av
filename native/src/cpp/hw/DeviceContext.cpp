//
// Created by silenium-dev on 8/10/24.
//

#include "helper/errors.hpp"


#include <jni.h>

extern "C" {
#include <libavutil/hwcontext.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_hw_DeviceContextKt_createN(
        JNIEnv *env, jobject thiz, const jint _type, jstring device) {
    const auto type = static_cast<AVHWDeviceType>(_type);
    const char *deviceStr{nullptr};
    if (device != nullptr) {
        deviceStr = env->GetStringUTFChars(device, nullptr);
    }
    AVBufferRef *deviceRef;
    auto ret = av_hwdevice_ctx_create(&deviceRef, type, deviceStr, nullptr, 0);
    if (deviceStr != nullptr) {
        env->ReleaseStringUTFChars(device, deviceStr);
    }
    if (ret < 0) {
        return avResultFailure(env, "creating hw device context", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(deviceRef));
}
}
