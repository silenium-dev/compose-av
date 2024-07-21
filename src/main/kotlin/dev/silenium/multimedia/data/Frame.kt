package dev.silenium.multimedia.data

import dev.silenium.multimedia.demux.Stream
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Frame(override val nativePointer: NativePointer, val stream: Stream) : NativeCleanable {
    constructor(pointer: Long, stream: Stream) : this(pointer.asNativePointer(::releaseFrameN), stream)

    val width: Int by lazy { widthN(nativePointer.address) }
    val height: Int by lazy { heightN(nativePointer.address) }
    val format: PixelFormat by lazy { PixelFormat(formatN(nativePointer.address)) }
    val swFormat: PixelFormat? by lazy { swFormatN(nativePointer.address).takeIf { it >= 0 }?.let(::PixelFormat) }
    val keyFrame: Boolean by lazy { keyFrameN(nativePointer.address) }
    val pts: Duration by lazy { (ptsN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val bestEffortTimestamp: Duration by lazy { (bestEffortTimestampN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val duration: Duration by lazy { (durationN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val buf: Array<ByteBuffer?> by lazy { dataN(nativePointer.address) }
    val rawData: Array<Long> by lazy { rawDataN(nativePointer.address) }
    val pitch: Array<Int> by lazy { pitchN(nativePointer.address) }

    val isHW: Boolean by lazy { isHWN(nativePointer.address) }
    fun transferToSW(): Result<Frame> {
        return transferToSWN(nativePointer.address).map { Frame(it, stream) }
    }
}

private external fun widthN(frame: Long): Int
private external fun heightN(frame: Long): Int
private external fun formatN(frame: Long): Int
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

private external fun releaseFrameN(frame: Long)
