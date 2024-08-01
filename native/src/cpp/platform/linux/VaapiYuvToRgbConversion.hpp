//
// Created by silenium-dev on 8/1/24.
//

#ifndef COMPOSE_AV_VAAPIYUVTORGBCONVERSION_HPP
#define COMPOSE_AV_VAAPIYUVTORGBCONVERSION_HPP

#include <jni.h>

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_compose_av_VaapiYuvToRgbConversion_createN(JNIEnv *env, jclass clazz, jlong _deviceRef, jlong _inputFrame);
};

#endif//COMPOSE_AV_VAAPIYUVTORGBCONVERSION_HPP
