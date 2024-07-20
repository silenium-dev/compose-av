//
// Created by silenium-dev on 7/20/24.
//

#include <iostream>
#include <jni.h>

extern "C" {
#include <libavformat/avformat.h>

struct FileDemuxerContext {
    AVFormatContext *formatContext{nullptr};
};

enum class Errors: int {
    EndOfFile = -1,
    EAgain = -2,
    ReadPacketFailed = -3,
};

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_demux_FileDemuxerKt_releaseNativeContextN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    avformat_close_input(&fileDemuxerContext->formatContext);
    delete fileDemuxerContext;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_demux_FileDemuxerKt_nextPacketN(
    JNIEnv *env,
    jobject thiz,
    jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    AVPacket *packet = av_packet_alloc();
    const auto ret = av_read_frame(fileDemuxerContext->formatContext, packet);
    if (ret == AVERROR_EOF) {
        std::cout << "EOF" << std::endl;
        av_packet_free(&packet);
        return static_cast<jlong>(Errors::EndOfFile);
    }
    if (ret == AVERROR(EAGAIN)) {
        std::cout << "EAGAIN" << std::endl;
        av_packet_free(&packet);
        return static_cast<jlong>(Errors::EAgain);
    }
    if (ret < 0) {
        std::cerr << "Failed to read packet: " << av_err2str(ret) << std::endl;
        av_packet_free(&packet);
        return static_cast<jlong>(Errors::ReadPacketFailed);
    }
    return reinterpret_cast<jlong>(packet);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_demux_FileDemuxerKt_initializeNativeContextN(
    JNIEnv *env,
    jobject thiz
    // jstring path
) {
    // const auto pathChars = env->GetStringUTFChars(path, nullptr);
    AVFormatContext *formatContext{nullptr};
    auto ret = avformat_open_input(&formatContext, "video.webm", nullptr, nullptr);
    // env->ReleaseStringUTFChars(path, pathChars);
    if (ret < 0) {
        std::cerr << "Failed to open input file: " << av_err2str(ret) << std::endl;
        return ret;
    }
    ret = avformat_find_stream_info(formatContext, nullptr);
    if (ret < 0) {
        std::cerr << "Failed to find stream info: " << av_err2str(ret) << std::endl;
        return ret;
    }

    auto context = new FileDemuxerContext{
        .formatContext = formatContext,
    };
    return reinterpret_cast<jlong>(context);
}
}
