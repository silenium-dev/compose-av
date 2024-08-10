package dev.silenium.multimedia.core.data

import dev.silenium.libs.flows.api.Reference
import java.nio.ByteBuffer

data class AVBufferRef(override val nativePointer: NativePointer) : NativeCleanable, Reference<AVBufferRef> {
    constructor(nativePointer: Long) : this(nativePointer.asNativePointer(::destroyAVBufferN))

    val dataPtr: Long by lazy { bufferDataPtrN(nativePointer.address) }
    val size: Long by lazy { bufferSizeN(nativePointer.address) }
    val data: ByteBuffer by lazy { byteBufferN(nativePointer.address) }
    val refCount get() = refCountN(nativePointer.address)

    override fun clone(): Result<AVBufferRef> {
        val cloned = cloneAVBufferN(nativePointer.address)
        return if (cloned != 0L) {
            Result.success(AVBufferRef(cloned.asNativePointer(::destroyAVBufferN)))
        } else {
            Result.failure(RuntimeException("Failed to clone AVBuffer"))
        }
    }

    override fun close() {
        destroyAVBufferN(nativePointer.address)
    }
}

private external fun destroyAVBufferN(buffer: Long)
private external fun cloneAVBufferN(buffer: Long): Long
private external fun byteBufferN(buffer: Long): ByteBuffer
private external fun bufferSizeN(buffer: Long): Long
private external fun bufferDataPtrN(buffer: Long): Long
private external fun refCountN(buffer: Long): Int
