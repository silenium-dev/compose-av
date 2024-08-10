//
// Created by silenium-dev on 7/20/24.
//

#include <iostream>
#include <jni.h>

#include "Demuxer.hpp"
#include "helper/errors.hpp"

Demuxer::Demuxer(AVFormatContext *formatContext) : formatContext(formatContext) {}
Demuxer::~Demuxer() {
    if (formatContext) {
        avformat_close_input(&formatContext);
    }
}


extern "C" {
#include <libavformat/avformat.h>

enum class Errors : int {
    EndOfFile = -1,
    EAgain = -2,
    ReadPacketFailed = -3,
};

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_positionN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    return fileDemuxerContext->formatContext->pb->pos;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_durationN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    return fileDemuxerContext->formatContext->duration;
}

JNIEXPORT jint JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_seekN(
        JNIEnv *env,
        jobject thiz,
        const jlong context,
        const jlong positionUs) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    return avformat_seek_file(fileDemuxerContext->formatContext, -1, INT64_MIN, positionUs, INT64_MAX, 0);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_nextPacketN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    AVPacket *packet = av_packet_alloc();
    if (const auto ret = av_read_frame(fileDemuxerContext->formatContext, packet); ret < 0) {
        av_packet_free(&packet);
        return avResultFailure(env, "read frame", ret);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(packet));
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_streamCountN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    return fileDemuxerContext->formatContext->nb_streams;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_streamN(
        JNIEnv *env,
        jobject thiz,
        const jlong context,
        const jlong index) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    if (fileDemuxerContext->formatContext->nb_streams <= index) {
        return avResultFailure(env, "stream index out of bounds", AVERROR(EINVAL));
    }
    return resultSuccess(env, reinterpret_cast<jlong>(fileDemuxerContext->formatContext->streams[index]));
}

JNIEXPORT jboolean JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_isSeekableN(
        JNIEnv *env,
        jobject thiz,
        const jlong context) {
    const auto fileDemuxerContext = reinterpret_cast<Demuxer *>(context);
    return fileDemuxerContext->formatContext->pb->seekable;
}

JNIEXPORT jlong JNICALL Java_dev_silenium_multimedia_core_demux_DemuxerKt_streamIndexN(
        JNIEnv *env,
        jobject thiz,
        const jlong packet) {
    const auto avPacket = reinterpret_cast<AVPacket *>(packet);
    return avPacket->stream_index;
}
}
