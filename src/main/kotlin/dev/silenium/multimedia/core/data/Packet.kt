package dev.silenium.multimedia.core.data

import dev.silenium.multimedia.core.demux.Stream
import java.nio.ByteBuffer

data class Packet(override val nativePointer: NativePointer, val stream: Stream) : NativeCleanable {
    constructor(pointer: Long, stream: Stream) : this(pointer.asNativePointer(::releasePacketN), stream)

    val size by lazy { sizeN(nativePointer.address) }
    val data: ByteBuffer by lazy { dataN(nativePointer.address, ByteBuffer.allocateDirect(size)) }
}

private external fun releasePacketN(packet: Long)
private external fun dataN(packet: Long, buf: ByteBuffer): ByteBuffer
private external fun sizeN(packet: Long): Int
