package dev.silenium.multimedia.core.util

interface NativeCleanable : AutoCloseable {
    val nativePointer: NativePointer

    fun release() = nativePointer.close()
    override fun close() = release()
}
