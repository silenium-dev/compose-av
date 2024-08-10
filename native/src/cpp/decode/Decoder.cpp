//
// Created by silenium-dev on 7/21/24.
//

#include "DecoderContext.h"
#include "helper/errors.hpp"

#include <jni.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_createN(
        JNIEnv *env,
        jobject thiz) {
    return resultSuccess(env, reinterpret_cast<jlong>(new DecoderContext));
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_releaseN(
        JNIEnv *env,
        jobject thiz,
        const jlong ctx) {
    const auto context = reinterpret_cast<DecoderContext *>(ctx);
    delete context;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_configureN(
        JNIEnv *env,
        jobject thiz,
        jobject _instance,
        const jlong _context,
        const jlong codecParameters) {
    const auto ctx = reinterpret_cast<DecoderContext *>(_context);
    return ctx->configure(env, _instance, reinterpret_cast<AVCodecParameters *>(codecParameters));
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_submitN(
        JNIEnv *env,
        jobject thiz,
        const jlong ctx,
        const jlong packet) {
    const auto context = reinterpret_cast<DecoderContext *>(ctx);
    return context->submit(env, packet);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_decode_DecoderKt_receiveN(
        JNIEnv *env,
        jobject thiz,
        const jlong ctx) {
    const auto context = reinterpret_cast<DecoderContext *>(ctx);
    return context->receive(env);
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_platform_linux_VaapiDecoderKt_flushN(
        JNIEnv *env,
        jobject thiz,
        const jlong codecContext) {
    const auto context = reinterpret_cast<DecoderContext *>(codecContext);
    context->flush(env);
}
}
