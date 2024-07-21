package dev.silenium.multimedia.data

import dev.silenium.multimedia.demux.Stream
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Frame(override val nativePointer: NativePointer, val stream: Stream) : NativeCleanable {
    constructor(pointer: Long, stream: Stream) : this(pointer.asNativePointer(::releaseFrameN), stream)

    val width: Int by lazy { widthN(nativePointer.address) }
    val height: Int by lazy { heightN(nativePointer.address) }
    val format: Int by lazy { formatN(nativePointer.address) }
    val keyFrame: Boolean by lazy { keyFrameN(nativePointer.address) }
    val pts: Duration by lazy { (ptsN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val bestEffortTimestamp: Duration by lazy { (bestEffortTimestampN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val duration: Duration by lazy { (durationN(nativePointer.address) * stream.timeBase).asDouble.seconds }
    val buf: Array<ByteBuffer> by lazy { dataN(nativePointer.address) }
    val rawData: Array<Long> by lazy { rawDataN(nativePointer.address) }
}

private external fun widthN(frame: Long): Int
private external fun heightN(frame: Long): Int
private external fun formatN(frame: Long): Int
private external fun keyFrameN(frame: Long): Boolean
private external fun ptsN(frame: Long): Long
private external fun bestEffortTimestampN(frame: Long): Long
private external fun durationN(frame: Long): Long
private external fun dataN(frame: Long): Array<ByteBuffer>
private external fun rawDataN(frame: Long): Array<Long>

private external fun releaseFrameN(frame: Long)
