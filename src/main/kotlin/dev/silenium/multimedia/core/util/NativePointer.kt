package dev.silenium.multimedia.core.util

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

data class NativePointer(val address: Long, val clean: (Long) -> Unit) : AutoCloseable {
    private val _closed = AtomicBoolean(false)
    val closed get() = _closed.get()
    override fun close() {
        if (address == 0L) {
            logger.warn("Attempt to close NULL NativePointer")
            return
        }
        if (_closed.compareAndSet(false, true)) {
            clean(address)
        } else {
            logger.warn("Attempt to close already closed NativePointer: $this")
        }
    }

    companion object {
        val NULL = NativePointer(0) {}
        private val logger = LoggerFactory.getLogger(NativePointer::class.java)
    }
}

fun Long.asNativePointer(clean: (Long) -> Unit) = NativePointer(this, clean)
