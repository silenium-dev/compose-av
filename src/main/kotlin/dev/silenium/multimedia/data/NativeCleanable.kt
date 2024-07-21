package dev.silenium.multimedia.data

interface NativeCleanable : AutoCloseable {
    val nativePointer: NativePointer

    fun release() = nativePointer.close()
    override fun close() = release()
}
