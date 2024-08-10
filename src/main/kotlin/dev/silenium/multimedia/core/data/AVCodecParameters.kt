package dev.silenium.multimedia.core.data

data class AVCodecParameters(override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long, cleanup: Boolean = true) :
            this(pointer.asNativePointer { if (cleanup) releaseCodecParameters(pointer) })

    val width: Int by lazy { widthN(nativePointer.address) }
    val height: Int by lazy { heightN(nativePointer.address) }
    val format: AVPixelFormat by lazy { fromId(formatN(nativePointer.address)) }

    val colorSpace: AVColorSpace by lazy { colorSpaceN(nativePointer.address).let(::fromId) }
    val colorPrimaries: AVColorPrimaries by lazy { colorPrimariesN(nativePointer.address).let(::fromId) }
    val colorRange: AVColorRange by lazy { colorRangeN(nativePointer.address).let(::fromId) }
    val colorTrc: AVColorTransferCharacteristic by lazy { colorTrcN(nativePointer.address).let(::fromId) }

    val codec: AVCodecID by lazy { codecIdN(nativePointer.address).let(::fromId) }
    val bitRate: Long by lazy { bitRateN(nativePointer.address) }
    val frameRate: Rational by lazy { frameRateN(nativePointer.address) }
}

private external fun releaseCodecParameters(pointer: Long)

private external fun codecIdN(pointer: Long): Int
private external fun heightN(pointer: Long): Int
private external fun widthN(pointer: Long): Int
private external fun formatN(pointer: Long): Int
private external fun bitRateN(pointer: Long): Long
private external fun frameRateN(pointer: Long): Rational

private external fun colorSpaceN(pointer: Long): Int
private external fun colorPrimariesN(pointer: Long): Int
private external fun colorRangeN(pointer: Long): Int
private external fun colorTrcN(pointer: Long): Int
