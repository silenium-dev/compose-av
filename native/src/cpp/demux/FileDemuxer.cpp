//
// Created by silenium-dev on 7/20/24.
//

#include "Demuxer.hpp"


#include <iostream>
#include <jni.h>

#include "helper/errors.hpp"

extern "C" {
#include <libavformat/avformat.h>

class FileDemuxer final : public Demuxer {
public:
    explicit FileDemuxer(AVFormatContext *formatContext) : Demuxer(formatContext) {}
    ~FileDemuxer() override = default;
};

enum class Errors : int {
    EndOfFile = -1,
    EAgain = -2,
    ReadPacketFailed = -3,
};

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_demux_FileDemuxerKt_releaseNativeContextN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxer *>(context);
    delete fileDemuxerContext;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_demux_FileDemuxerKt_initializeNativeContextN(
        JNIEnv *env,
        jobject thiz,
        jstring url) {
    const auto urlChars = env->GetStringUTFChars(url, nullptr);
    AVFormatContext *formatContext{nullptr};
    auto ret = avformat_open_input(&formatContext, urlChars, nullptr, nullptr);
    env->ReleaseStringUTFChars(url, urlChars);
    if (ret < 0) {
        std::cerr << "Failed to open input file: " << avErrorString(ret) << std::endl;
        return avResultFailure(env, "open format input", ret);
    }
    ret = avformat_find_stream_info(formatContext, nullptr);
    if (ret < 0) {
        std::cerr << "Failed to find stream info: " << avErrorString(ret) << std::endl;
        return avResultFailure(env, "find stream info", ret);
    }

    auto context = new FileDemuxer{formatContext};
    return resultSuccess(env, reinterpret_cast<jlong>(context));
}
}
