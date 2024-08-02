//
// Created by silenium-dev on 8/1/24.
//

#include <jni.h>

extern "C" {
#include <libavutil/buffer.h>
JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_util_AVBufferKt_destroyAVBufferN(
        JNIEnv *env,
        jobject thiz,
        const jlong buffer) {
    auto avBuffer = reinterpret_cast<AVBufferRef *>(buffer);
    av_buffer_unref(&avBuffer);
}
}
