package dev.silenium.multimedia.data

data class NativePointer(val address: Long, val clean: (Long) -> Unit): AutoCloseable {
    override fun close() = clean(address)
}

fun Long.asNativePointer(clean: (Long) -> Unit = {}) = NativePointer(this, clean)

interface NativeCleanable : AutoCloseable {
    val nativePointer: NativePointer

    fun release() = nativePointer.close()
    override fun close() = release()
}
