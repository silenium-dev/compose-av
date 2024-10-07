package dev.silenium.multimedia.mpv

import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.multimedia.core.util.NativeCleanable
import dev.silenium.multimedia.core.util.NativePointer
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.asNativePointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lwjgl.opengl.GL
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface MPVAsyncListener {
    suspend fun onPropertyChanged(name: String, value: Any)
}

class MPV : NativeCleanable, MPVAsyncListener {
    private var callback: Long? = null
    private val initialized = AtomicBoolean(false)
    private val listener = MPVListenerWrapper(this)
    private val subscriptionId = AtomicLong(0)
    private val _duration = MutableStateFlow(Duration.ZERO)
    val duration get() = _duration.asStateFlow()
    private val _position = MutableStateFlow(Duration.ZERO)
    val position get() = _position.asStateFlow()

    override val nativePointer = NativePointer(createN().getOrThrow()) {
        callback?.let(::unsetCallbackN)
        listener.close()
        destroyN(it)
    }

    fun setOption(name: String, value: String) {
        if (initialized.get()) {
            logger.warn("Cannot set option after initialization, ignoring")
            return
        }
        logger.debug("Setting option $name=$value")
        setOptionStringN(nativePointer.address, name, value)
    }

    fun initialize(): Result<Unit> {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing MPV")
            return initializeN(nativePointer.address).mapCatching {
                callback = setCallbackN(nativePointer.address, listener).getOrThrow()
            }
        }

        logger.warn("MPV is already initialized")
        return Result.success(Unit)
    }

    fun setProperty(name: String, value: String) = setPropertyStringN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Long) = setPropertyLongN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Double) = setPropertyDoubleN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Boolean) = setPropertyFlagN(nativePointer.address, name, value)

    fun getPropertyString(name: String) = getPropertyStringN(nativePointer.address, name)
    fun getPropertyLong(name: String) = getPropertyLongN(nativePointer.address, name)
    fun getPropertyDouble(name: String) = getPropertyDoubleN(nativePointer.address, name)
    fun getPropertyFlag(name: String) = getPropertyFlagN(nativePointer.address, name)

    fun observePropertyString(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        return observePropertyStringN(nativePointer.address, subscriptionId, name).map { subscriptionId }
    }

    fun observePropertyLong(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        return observePropertyLongN(nativePointer.address, subscriptionId, name).map { subscriptionId }
    }

    fun observePropertyDouble(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        return observePropertyDoubleN(nativePointer.address, subscriptionId, name).map { subscriptionId }
    }

    fun observePropertyFlag(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        return observePropertyFlagN(nativePointer.address, subscriptionId, name).map { subscriptionId }
    }

    fun unobserveProperty(subscriptionId: Long) = unobservePropertyN(nativePointer.address, subscriptionId)

    fun command(command: Collection<String>) = commandN(nativePointer.address, command.toTypedArray())
    fun command(command: String) = commandStringN(nativePointer.address, command)

    override suspend fun onPropertyChanged(name: String, value: Any) {
        if (name == "duration") {
            _duration.value = (value as Double).seconds
        } else if (name == "time-pos") {
            _position.value = (value as Double).seconds
        }
    }

    fun createRender(updateCallback: () -> Unit) = Render(this, updateCallback)

    class Render(mpv: MPV, private val updateCallback: () -> Unit) : NativeCleanable {
        override val nativePointer: NativePointer =
            createRenderN(mpv.nativePointer.address, this).getOrThrow()
                .asNativePointer(::destroyRenderN)

        fun render(fbo: FBO): Result<Unit> {
            return renderN(
                nativePointer.address,
                fbo.id,
                fbo.size.width,
                fbo.size.height,
                fbo.colorAttachment.internalFormat
            )
        }

        fun requestUpdate() = updateCallback()

        fun getGlProc(name: String): Long {
            return GL.getFunctionProvider()?.getFunctionAddress(name) ?: 0
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MPV::class.java)

        init {
            Natives.ensureLoaded()
        }
    }
}

interface MPVListener {
    fun onPropertyChanged(name: String, value: Any)
}

private class MPVListenerWrapper(private val wrapped: MPVAsyncListener) : MPVListener, AutoCloseable {
    private sealed interface Event {
        suspend fun call(listener: MPVAsyncListener)
    }

    private data class PropertyChanged(val name: String, val value: Any) : Event {
        override suspend fun call(listener: MPVAsyncListener) {
            listener.onPropertyChanged(name, value)
        }
    }

    private val events = ConcurrentLinkedQueue<Event>()
    private val executor = CoroutineScope(Dispatchers.Default).launch {
        while (isActive) {
            val event = events.poll()
            if (event == null) {
                delay(1.milliseconds)
                continue
            }
            event.call(wrapped)
        }
    }

    override fun onPropertyChanged(name: String, value: Any) {
        events.add(PropertyChanged(name, value))
    }

    override fun close() {
        executor.cancel()
    }
}

private external fun createN(): Result<Long>
private external fun destroyN(handle: Long)
private external fun setOptionStringN(handle: Long, name: String, value: String): Result<Unit>
private external fun initializeN(handle: Long): Result<Unit>

private external fun setCallbackN(handle: Long, listener: MPVListener): Result<Long>
private external fun unsetCallbackN(callbackHandle: Long): Result<Unit>

private external fun commandN(handle: Long, command: Array<String>): Result<Unit>
private external fun commandStringN(handle: Long, command: String): Result<Unit>

private external fun getPropertyStringN(handle: Long, name: String): Result<String>
private external fun getPropertyLongN(handle: Long, name: String): Result<Long>
private external fun getPropertyDoubleN(handle: Long, name: String): Result<Double>
private external fun getPropertyFlagN(handle: Long, name: String): Result<Boolean>

private external fun setPropertyStringN(handle: Long, name: String, value: String): Result<Unit>
private external fun setPropertyLongN(handle: Long, name: String, value: Long): Result<Unit>
private external fun setPropertyDoubleN(handle: Long, name: String, value: Double): Result<Unit>
private external fun setPropertyFlagN(handle: Long, name: String, value: Boolean): Result<Unit>

private external fun observePropertyStringN(handle: Long, subscriptionId: Long, name: String): Result<Unit>
private external fun observePropertyLongN(handle: Long, subscriptionId: Long, name: String): Result<Unit>
private external fun observePropertyDoubleN(handle: Long, subscriptionId: Long, name: String): Result<Unit>
private external fun observePropertyFlagN(handle: Long, subscriptionId: Long, name: String): Result<Unit>
private external fun unobservePropertyN(handle: Long, subscriptionId: Long): Result<Unit>

private external fun createRenderN(handle: Long, self: MPV.Render): Result<Long>
private external fun destroyRenderN(handle: Long)
private external fun renderN(handle: Long, fbo: Int, width: Int, height: Int, glInternalFormat: Int): Result<Unit>
