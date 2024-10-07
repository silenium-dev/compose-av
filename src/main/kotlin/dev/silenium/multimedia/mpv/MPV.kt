package dev.silenium.multimedia.mpv

import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.multimedia.core.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.lwjgl.opengl.GL
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

interface MPVAsyncListener {
    suspend fun onPropertyChanged(name: String, value: Any?)
}

sealed class Property<T> {
    abstract val name: String
    abstract val value: T
}

data class StringProperty(override val name: String, override val value: String?) : Property<String?>()
data class LongProperty(override val name: String, override val value: Long?) : Property<Long?>()
data class DoubleProperty(override val name: String, override val value: Double?) : Property<Double?>()
data class FlagProperty(override val name: String, override val value: Boolean?) : Property<Boolean?>()

class MPV : NativeCleanable, MPVAsyncListener {
    private var callback: Long? = null
    private val initialized = AtomicBoolean(false)
    private val listener = MPVListenerWrapper(this)
    private val subscriptionId = AtomicLong(0)
    private val observingProperties = ConcurrentHashMap<String, KClass<*>>()
    private val subscriptionIds = ConcurrentHashMap<Long, String>()

    private val propertyUpdates = MutableSharedFlow<Property<*>>()

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
        observingProperties[name] = String::class
        return observePropertyStringN(nativePointer.address, subscriptionId, name)
            .map { subscriptionIds[subscriptionId] = name; subscriptionId }
    }

    fun observePropertyLong(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        observingProperties[name] = Long::class
        return observePropertyLongN(nativePointer.address, subscriptionId, name)
            .map { subscriptionIds[subscriptionId] = name; subscriptionId }
    }

    fun observePropertyDouble(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        observingProperties[name] = Double::class
        return observePropertyDoubleN(nativePointer.address, subscriptionId, name)
            .map { subscriptionIds[subscriptionId] = name; subscriptionId }
    }

    fun observePropertyFlag(name: String): Result<Long> {
        val subscriptionId = subscriptionId.getAndIncrement()
        observingProperties[name] = Boolean::class
        return observePropertyFlagN(nativePointer.address, subscriptionId, name)
            .map { subscriptionIds[subscriptionId] = name; subscriptionId }
    }

    fun unobserveProperty(subscriptionId: Long) = unobservePropertyN(nativePointer.address, subscriptionId).map {
        subscriptionIds.remove(subscriptionId)
            ?.let(observingProperties::remove)
        Unit
    }

    private class Sharing(
        private val name: String,
        private val subscribe: (name: String) -> Result<Long>,
        private val unsubscribe: (subscriptionId: Long) -> Result<Unit>,
        private val wrapped: SharingStarted,
    ) : SharingStarted {
        private val subscriptionId = AtomicReference<Long?>(null)
        override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> {
            return wrapped.command(subscriptionCount).onEach { command ->
                when (command) {
                    SharingCommand.START -> subscriptionId.updateAndGet {
                        it ?: subscribe(name)
                            .onFailure { t -> logger.error("Failed to subscribe to property $name", t) }
                            .getOrNull()
                    }

                    SharingCommand.STOP,
                    SharingCommand.STOP_AND_RESET_REPLAY_CACHE,
                        -> subscriptionId.getAndSet(null)?.let(unsubscribe)
                }
            }
        }

        companion object {
            private val logger = LoggerFactory.getLogger(Sharing::class.java)
        }
    }

    fun propertyFlowString(name: String): StateFlow<String?> {
        val initialValue = getPropertyString(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<StringProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyString, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    fun propertyFlowLong(name: String): StateFlow<Long?> {
        val initialValue = getPropertyLong(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<LongProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyLong, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    fun propertyFlowDouble(name: String): StateFlow<Double?> {
        val initialValue = getPropertyDouble(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<DoubleProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyDouble, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    fun propertyFlowFlag(name: String): StateFlow<Boolean?> {
        val initialValue = getPropertyFlag(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<FlagProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyFlag, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    inline fun <reified E : Any> propertyFlow(name: String) = when (E::class) {
        String::class -> propertyFlowString(name).mapState { it as E? }
        Long::class -> propertyFlowLong(name).mapState { it as E? }
        Double::class -> propertyFlowDouble(name).mapState { it as E? }
        Boolean::class -> propertyFlowFlag(name).mapState { it as E? }
        else -> error("Unsupported property type: ${E::class}")
    }

    fun command(command: Collection<String>) = commandN(nativePointer.address, command.toTypedArray())
    fun command(command: String) = commandStringN(nativePointer.address, command)

    override suspend fun onPropertyChanged(name: String, value: Any?) {
        val type = value?.javaClass?.kotlin ?: observingProperties[name]
        val property = when (type) {
            String::class -> StringProperty(name, value as String?)
            Long::class -> LongProperty(name, value as Long?)
            Double::class -> DoubleProperty(name, value as Double?)
            Boolean::class -> FlagProperty(name, value as Boolean?)
            else -> {
                logger.warn("Unknown property type for $name: $value")
                return
            }
        }
        propertyUpdates.emit(property)
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

        // Used by native code
        @Suppress("unused")
        private fun requestUpdate() = updateCallback()

        // Used by native code
        @Suppress("unused")
        private fun getGlProc(name: String): Long {
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

private interface MPVListener {
    fun onPropertyChanged(name: String, value: Any?)
}

private class MPVListenerWrapper(private val wrapped: MPVAsyncListener) : MPVListener, AutoCloseable {
    private sealed interface Event {
        suspend fun call(listener: MPVAsyncListener)
    }

    private data class PropertyChanged(val name: String, val value: Any?) : Event {
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

    override fun onPropertyChanged(name: String, value: Any?) {
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

private external fun getPropertyStringN(handle: Long, name: String): Result<String?>
private external fun getPropertyLongN(handle: Long, name: String): Result<Long?>
private external fun getPropertyDoubleN(handle: Long, name: String): Result<Double?>
private external fun getPropertyFlagN(handle: Long, name: String): Result<Boolean?>

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
