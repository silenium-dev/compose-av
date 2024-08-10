//
// Created by silenium-dev on 8/1/24.
//

#include <jni.h>

extern "C" {
#include <libavutil/buffer.h>

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_destroyAVBufferN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    av_buffer_unref(&avBuffer);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_cloneAVBufferN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    const auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    return reinterpret_cast<jlong>(av_buffer_ref(avBuffer));
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_byteBufferN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    const auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    return env->NewDirectByteBuffer(avBuffer->data, static_cast<jlong>(avBuffer->size));
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_bufferSizeN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    const auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    return static_cast<jlong>(avBuffer->size);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_bufferDataPtrN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    const auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    return reinterpret_cast<jlong>(avBuffer->data);
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_data_AVBufferRefKt_refCountN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    const auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    return av_buffer_get_ref_count(avBuffer);
}
}
