//
// Created by silenium-dev on 7/20/24.
//

#include <iostream>
#include <jni.h>

#include "helper/errors.hpp"

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

JNIEXPORT void JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_releaseNativeContextN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    avformat_close_input(&fileDemuxerContext->formatContext);
    delete fileDemuxerContext;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_positionN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return fileDemuxerContext->formatContext->pb->pos;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_durationN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return fileDemuxerContext->formatContext->duration;
}

JNIEXPORT jint JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_seekN(
    JNIEnv *env,
    jobject thiz,
    const jlong context,
    const jlong positionUs
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return avformat_seek_file(fileDemuxerContext->formatContext, -1, INT64_MIN, positionUs, INT64_MAX, 0);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_nextPacketN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    AVPacket *packet = av_packet_alloc();
    if (const auto ret = av_read_frame(fileDemuxerContext->formatContext, packet); ret < 0) {
        av_packet_free(&packet);
        return ret;
    }
    return reinterpret_cast<jlong>(packet);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_initializeNativeContextN(
    JNIEnv *env,
    jobject thiz,
    const jstring url
) {
    const auto urlChars = env->GetStringUTFChars(url, nullptr);
    AVFormatContext *formatContext{nullptr};
    auto ret = avformat_open_input(&formatContext, urlChars, nullptr, nullptr);
    env->ReleaseStringUTFChars(url, urlChars);
    if (ret < 0) {
        std::cerr << "Failed to open input file: " << avErrorString(ret) << std::endl;
        return ret;
    }
    ret = avformat_find_stream_info(formatContext, nullptr);
    if (ret < 0) {
        std::cerr << "Failed to find stream info: " << avErrorString(ret) << std::endl;
        return ret;
    }

    auto context = new FileDemuxerContext{
        .formatContext = formatContext,
    };
    return reinterpret_cast<jlong>(context);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_streamCountN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return fileDemuxerContext->formatContext->nb_streams;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_streamN(
    JNIEnv *env,
    jobject thiz,
    const jlong context,
    const jlong index
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return reinterpret_cast<jlong>(fileDemuxerContext->formatContext->streams[index]);
}

JNIEXPORT jboolean JNICALL Java_dev_silenium_compose_av_demux_FileDemuxerKt_isSeekableN(
    JNIEnv *env,
    jobject thiz,
    const jlong context
) {
    const auto fileDemuxerContext = reinterpret_cast<FileDemuxerContext *>(context);
    return fileDemuxerContext->formatContext->pb->seekable;
}
}
