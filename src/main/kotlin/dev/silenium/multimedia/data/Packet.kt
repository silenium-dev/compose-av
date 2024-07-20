package dev.silenium.multimedia.data

data class Packet(override val nativePointer: NativePointer) : NativeCleanable {
    constructor(pointer: Long) : this(pointer.asNativePointer(::releasePacketN))
}

private external fun releasePacketN(packet: Long)
