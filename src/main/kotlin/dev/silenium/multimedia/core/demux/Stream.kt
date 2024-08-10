package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.*

data class Stream(val index: UInt, val type: AVMediaType, override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long) : this(
        pointer.asNativePointer { /* no cleanup, AVStream is owned by AVFormatContext */ },
    )

    constructor(nativePointer: NativePointer) : this(
        indexN(nativePointer.address).toUInt(),
        typeN(nativePointer.address).let(::fromId),
        nativePointer,
    )

    val codecParameters: AVCodecParameters by lazy { AVCodecParameters(codecParametersN(nativePointer.address)) }
    val timeBase: Rational by lazy { timeBaseN(nativePointer.address) }
    val duration: Long by lazy { durationN(nativePointer.address) }
    val avgFrameRate: Rational by lazy { avgFrameRateN(nativePointer.address) }
    val sampleAspectRatio: Rational by lazy { sampleAspectRatioN(nativePointer.address) }
}

private external fun indexN(pointer: Long): Int
private external fun typeN(pointer: Long): Int
private external fun timeBaseN(pointer: Long): Rational
private external fun durationN(pointer: Long): Long
private external fun avgFrameRateN(pointer: Long): Rational
private external fun sampleAspectRatioN(pointer: Long): Rational
private external fun codecParametersN(packet: Long): Long
