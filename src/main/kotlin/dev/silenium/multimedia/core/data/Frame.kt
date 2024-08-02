package dev.silenium.multimedia.core.data

import dev.silenium.multimedia.core.util.Natives
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Frame(override val nativePointer: NativePointer, val timeBase: Rational) : NativeCleanable {
    constructor(pointer: Long, timeBase: Rational) : this(pointer.asNativePointer(::releaseFrameN), timeBase)

    val width: Int by lazy { widthN(nativePointer.address) }
    val height: Int by lazy { heightN(nativePointer.address) }
    val format: AVPixelFormat by lazy { fromId<AVPixelFormat>(formatN(nativePointer.address)) }
    val swFormat: AVPixelFormat? by lazy { swFormatN(nativePointer.address).takeIf { it >= 0 }?.let(::fromId) }
    val keyFrame: Boolean by lazy { keyFrameN(nativePointer.address) }
    val pts: Duration by lazy { (ptsN(nativePointer.address) * timeBase.asDouble).seconds }
    val bestEffortTimestamp: Duration by lazy { (bestEffortTimestampN(nativePointer.address) * timeBase.asDouble).seconds }
    val duration: Duration by lazy { (durationN(nativePointer.address) * timeBase.asDouble).seconds }
    val buf: Array<ByteBuffer?> by lazy { dataN(nativePointer.address) }
    val rawData: Array<Long> by lazy { rawDataN(nativePointer.address) }
    val pitch: Array<Int> by lazy { pitchN(nativePointer.address) }

    val colorSpace: AVColorSpace by lazy { fromId(colorSpaceN(nativePointer.address)) }
    val colorPrimaries: AVColorPrimaries by lazy { fromId(colorPrimariesN(nativePointer.address)) }
    val colorRange: AVColorRange by lazy { fromId(colorRangeN(nativePointer.address)) }
    val colorTrc: AVColorTransferCharacteristic by lazy { fromId(colorTrcN(nativePointer.address)) }

    val isHW: Boolean by lazy { isHWN(nativePointer.address) }
    fun transferToSW(): Result<Frame> {
        return transferToSWN(nativePointer.address).map { Frame(it, timeBase) }
    }
    fun clone(): Result<Frame> {
        return cloneN(nativePointer.address).map { Frame(it, timeBase) }
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun timeBaseN(frame: Long): Rational
private external fun widthN(frame: Long): Int
private external fun heightN(frame: Long): Int
private external fun formatN(frame: Long): Int
private external fun colorSpaceN(frame: Long): Int
private external fun colorPrimariesN(frame: Long): Int
private external fun colorRangeN(frame: Long): Int
private external fun colorTrcN(frame: Long): Int
private external fun swFormatN(frame: Long): Int
private external fun keyFrameN(frame: Long): Boolean
private external fun ptsN(frame: Long): Long
private external fun bestEffortTimestampN(frame: Long): Long
private external fun durationN(frame: Long): Long
private external fun isHWN(frame: Long): Boolean
private external fun dataN(frame: Long): Array<ByteBuffer?>
private external fun rawDataN(frame: Long): Array<Long>
private external fun pitchN(frame: Long): Array<Int>

private external fun transferToSWN(frame: Long): Result<Long>
private external fun cloneN(frame: Long): Result<Long>

private external fun releaseFrameN(frame: Long)
