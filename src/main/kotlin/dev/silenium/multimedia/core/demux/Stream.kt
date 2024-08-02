package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.*

data class Stream(val index: Int, val type: AVMediaType, override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long) : this(
        pointer.asNativePointer { /* no cleanup, AVStream is owned by AVFormatContext */ },
    )

    constructor(nativePointer: NativePointer) : this(
        indexN(nativePointer.address),
        typeN(nativePointer.address).let(::fromId),
        nativePointer,
    )

    val codec: AVCodecID by lazy { codecIdN(nativePointer.address).let(::fromId) }
    val timeBase: Rational by lazy { timeBaseN(nativePointer.address) }
    val duration: Long by lazy { durationN(nativePointer.address) }
    val bitRate: Long by lazy { bitRateN(nativePointer.address) }
    val avgFrameRate: Rational by lazy { avgFrameRateN(nativePointer.address) }
    val format: Int by lazy { formatN(nativePointer.address) }
}

private external fun indexN(pointer: Long): Int
private external fun typeN(pointer: Long): Int
private external fun codecIdN(pointer: Long): Int
private external fun timeBaseN(pointer: Long): Rational
private external fun durationN(pointer: Long): Long
private external fun bitRateN(pointer: Long): Long
private external fun avgFrameRateN(pointer: Long): Rational
private external fun formatN(pointer: Long): Int