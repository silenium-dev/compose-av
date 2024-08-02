//
// Created by silenium-dev on 7/21/24.
//

#include <jni.h>

extern "C" {
#include <libavutil/pixfmt.h>
#include <libavutil/pixdesc.h>

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_core_data_AVPixelFormatKt_descriptionN(
    JNIEnv *env,
    jobject thiz,
    const jint format
) {
    return env->NewStringUTF(av_get_pix_fmt_name(static_cast<AVPixelFormat>(format)));
}
}
