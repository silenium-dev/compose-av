//
// Created by silenium-dev on 8/10/24.
//

#include "FramePadMetadata.hpp"

FramePadMetadata::FramePadMetadata(JNIEnv *env, jobject metadata) : env(env), metadata(metadata) {
}

int FramePadMetadata::width() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getWidth", "()I");
    return env->CallIntMethod(metadata, methodId);
}

int FramePadMetadata::height() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getHeight", "()I");
    return env->CallIntMethod(metadata, methodId);
}

AVPixelFormat FramePadMetadata::format() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getFormat", "()Ldev/silenium/multimedia/core/data/AVPixelFormat;");
    const auto format = env->CallObjectMethod(metadata, methodId);
    const auto formatClazz = env->GetObjectClass(format);
    const auto idMethod = env->GetMethodID(formatClazz, "getId", "()I");
    return static_cast<AVPixelFormat>(env->CallIntMethod(format, idMethod));
}

std::optional<AVPixelFormat> FramePadMetadata::swFormat() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getSwFormat", "()Ldev/silenium/multimedia/core/data/AVPixelFormat;");
    const auto format = env->CallObjectMethod(metadata, methodId);
    if (format == nullptr) {
        return std::nullopt;
    }
    const auto formatClazz = env->GetObjectClass(format);
    const auto idMethod = env->GetMethodID(formatClazz, "getId", "()I");
    return {static_cast<AVPixelFormat>(env->CallIntMethod(format, idMethod))};
}

bool FramePadMetadata::isHW() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "isHW", "()Z");
    return env->CallBooleanMethod(metadata, methodId);
}

AVColorSpace FramePadMetadata::colorSpace() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getColorSpace", "()Ldev/silenium/multimedia/core/data/AVColorSpace;");
    const auto colorSpace = env->CallObjectMethod(metadata, methodId);
    const auto colorSpaceClazz = env->GetObjectClass(colorSpace);
    const auto idMethod = env->GetMethodID(colorSpaceClazz, "getId", "()I");
    return static_cast<AVColorSpace>(env->CallIntMethod(colorSpace, idMethod));
}

AVColorPrimaries FramePadMetadata::colorPrimaries() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getColorPrimaries", "()Ldev/silenium/multimedia/core/data/AVColorPrimaries;");
    const auto colorPrimaries = env->CallObjectMethod(metadata, methodId);
    const auto colorPrimariesClazz = env->GetObjectClass(colorPrimaries);
    const auto idMethod = env->GetMethodID(colorPrimariesClazz, "getId", "()I");
    return static_cast<AVColorPrimaries>(env->CallIntMethod(colorPrimaries, idMethod));
}

AVColorRange FramePadMetadata::colorRange() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getColorRange", "()Ldev/silenium/multimedia/core/data/AVColorRange;");
    const auto colorRange = env->CallObjectMethod(metadata, methodId);
    const auto colorRangeClazz = env->GetObjectClass(colorRange);
    const auto idMethod = env->GetMethodID(colorRangeClazz, "getId", "()I");
    return static_cast<AVColorRange>(env->CallIntMethod(colorRange, idMethod));
}

AVColorTransferCharacteristic FramePadMetadata::colorTransfer() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getColorTransfer", "()Ldev/silenium/multimedia/core/data/AVColorTransferCharacteristic;");
    const auto colorTransfer = env->CallObjectMethod(metadata, methodId);
    const auto colorTransferClazz = env->GetObjectClass(colorTransfer);
    const auto idMethod = env->GetMethodID(colorTransferClazz, "getId", "()I");
    return static_cast<AVColorTransferCharacteristic>(env->CallIntMethod(colorTransfer, idMethod));
}

AVRational FramePadMetadata::timeBase() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getTimeBase", "()Ldev/silenium/multimedia/core/data/Rational;");
    const auto timeBase = env->CallObjectMethod(metadata, methodId);
    const auto timeBaseClazz = env->GetObjectClass(timeBase);
    const auto numMethod = env->GetMethodID(timeBaseClazz, "getNum", "()I");
    const auto denMethod = env->GetMethodID(timeBaseClazz, "getDen", "()I");
    return {env->CallIntMethod(timeBase, numMethod), env->CallIntMethod(timeBase, denMethod)};
}

AVRational FramePadMetadata::sample_aspect_ratio() const {
    const auto clazz = env->GetObjectClass(metadata);
    const auto methodId = env->GetMethodID(clazz, "getSampleAspectRatio", "()Ldev/silenium/multimedia/core/data/Rational;");
    const auto timeBase = env->CallObjectMethod(metadata, methodId);
    const auto timeBaseClazz = env->GetObjectClass(timeBase);
    const auto numMethod = env->GetMethodID(timeBaseClazz, "getNum", "()I");
    const auto denMethod = env->GetMethodID(timeBaseClazz, "getDen", "()I");
    return {env->CallIntMethod(timeBase, numMethod), env->CallIntMethod(timeBase, denMethod)};
}
