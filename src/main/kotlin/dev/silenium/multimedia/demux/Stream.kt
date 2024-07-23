package dev.silenium.multimedia.demux

import dev.silenium.multimedia.data.*

data class Stream(val index: Int, val type: Type, override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long) : this(
        pointer.asNativePointer { /* no cleanup, AVStream is owned by AVFormatContext */ },
    )

    constructor(nativePointer: NativePointer) : this(
        indexN(nativePointer.address),
        typeN(nativePointer.address),
        nativePointer,
    )

    enum class Type {
        AUDIO,
        VIDEO,
        SUBTITLE,
        ATTACHMENT,
        DATA,
        NB,
        UNKNOWN;
    }

    val codec: AVCodecID by lazy { codecIdN(nativePointer.address).let(::fromId) }
    val timeBase: Rational by lazy { timeBaseN(nativePointer.address) }
    val duration: Long by lazy { durationN(nativePointer.address) }
    val bitRate: Long by lazy { bitRateN(nativePointer.address) }
    val avgFrameRate: Rational by lazy { avgFrameRateN(nativePointer.address) }
}

private external fun indexN(pointer: Long): Int
private external fun typeN(pointer: Long): Stream.Type
private external fun codecIdN(pointer: Long): Int
private external fun timeBaseN(pointer: Long): Rational
private external fun durationN(pointer: Long): Long
private external fun bitRateN(pointer: Long): Long
private external fun avgFrameRateN(pointer: Long): Rational
