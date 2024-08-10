//
// Created by silenium-dev on 8/4/24.
//

#ifndef DECODER_H
#define DECODER_H
#include <jni.h>

extern "C" {
#include <libavcodec/avcodec.h>
}

class DecoderContext {
public:
    explicit DecoderContext();
    DecoderContext(const DecoderContext &) = delete;
    DecoderContext &operator=(const DecoderContext &) = delete;
    DecoderContext(DecoderContext &&) noexcept;
    DecoderContext &operator=(DecoderContext &&) noexcept;

    virtual ~DecoderContext();

    virtual jobject configure(JNIEnv *env, jobject thiz, const AVCodecParameters *parameters);
    virtual jobject submit(JNIEnv *env, jlong packet) const;
    virtual jobject receive(JNIEnv *env) const;
    virtual void flush(JNIEnv *env) const;

protected:
    const AVCodec *codec{};
    AVCodecContext *context{};
};

#endif//DECODER_H
