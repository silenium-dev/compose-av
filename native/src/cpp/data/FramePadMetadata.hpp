//
// Created by silenium-dev on 8/10/24.
//

#ifndef FRAMEPADMETADATA_HPP
#define FRAMEPADMETADATA_HPP

#include <jni.h>
#include <libavutil/rational.h>
#include <optional>
extern "C" {
#include <libavutil/pixfmt.h>
}

class FramePadMetadata {
public:
    explicit FramePadMetadata(JNIEnv *env, jobject metadata);

    [[nodiscard]] int width() const;
    [[nodiscard]] int height() const;
    [[nodiscard]] AVPixelFormat format() const;
    [[nodiscard]] std::optional<AVPixelFormat> swFormat() const;
    [[nodiscard]] bool isHW() const;

    [[nodiscard]] AVColorSpace colorSpace() const;
    [[nodiscard]] AVColorPrimaries colorPrimaries() const;
    [[nodiscard]] AVColorRange colorRange() const;
    [[nodiscard]] AVColorTransferCharacteristic colorTransfer() const;

    [[nodiscard]] AVRational timeBase() const;
    [[nodiscard]] AVRational sample_aspect_ratio() const;

private:
    JNIEnv *env;
    jobject metadata;
};

#endif//FRAMEPADMETADATA_HPP
