package dev.silenium.multimedia.core.data

import dev.silenium.libs.flows.api.Reference
import dev.silenium.multimedia.core.demux.Stream
import java.nio.ByteBuffer

data class Packet(override val nativePointer: NativePointer, val stream: Stream) : NativeCleanable, Reference<Packet> {
    constructor(pointer: Long, stream: Stream) : this(pointer.asNativePointer(::releasePacketN), stream)

    val size by lazy { sizeN(nativePointer.address) }
    val data: ByteBuffer by lazy { dataN(nativePointer.address) }

    override fun clone(): Result<Packet> {
        return clonePacketN(nativePointer.address).map { Packet(it, stream) }
    }

    override fun close() = super.close()
}

private external fun clonePacketN(packet: Long): Result<Long>
private external fun releasePacketN(packet: Long)
private external fun dataN(packet: Long): ByteBuffer
private external fun sizeN(packet: Long): Int
