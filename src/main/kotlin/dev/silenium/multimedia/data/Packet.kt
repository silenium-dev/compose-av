package dev.silenium.multimedia.data

import java.nio.ByteBuffer

data class Packet(override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long) : this(pointer.asNativePointer(::releasePacketN))

    val size by lazy { sizeN(nativePointer.address) }
    val data: ByteBuffer by lazy { dataN(nativePointer.address, ByteBuffer.allocateDirect(size)) }
}

private external fun releasePacketN(packet: Long)
private external fun dataN(packet: Long, buf: ByteBuffer): ByteBuffer
private external fun sizeN(packet: Long): Int
