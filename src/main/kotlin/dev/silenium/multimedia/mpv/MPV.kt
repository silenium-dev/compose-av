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
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.time.Duration.Companion.milliseconds

interface MPVAsyncListener {
    suspend fun onPropertyChanged(name: String, value: Any?)
    suspend fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    suspend fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
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
    private val propertySubscriptions = ConcurrentHashMap<String, Pair<Long, KClass<*>>>()
    private val propertyGetCallbacks = ConcurrentHashMap<Long, (Result<Any?>) -> Unit>()
    private val propertyGetCallbackId = AtomicLong(0)
    private val propertySetCallbacks = ConcurrentHashMap<Long, (Result<Unit>) -> Unit>()
    private val propertySetCallbackId = AtomicLong(0)

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

    private suspend fun <T> setProperty(
        name: String,
        value: T,
        fn: (Long, String, T, Long) -> Result<Unit>,
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val subscriptionId = propertySetCallbackId.getAndIncrement()
        propertySetCallbacks[subscriptionId] = { result ->
            continuation.resume(result)
        }
        fn(nativePointer.address, name, value, subscriptionId).onFailure {
            propertySetCallbacks.remove(subscriptionId)
            logger.error("Failed to set property $name", it)
            continuation.resume(Result.failure(it))
        }
    }

    suspend fun setPropertyAsync(name: String, value: String) = setProperty(name, value, ::setPropertyStringAsyncN)
    suspend fun setPropertyAsync(name: String, value: Long) = setProperty(name, value, ::setPropertyLongAsyncN)
    suspend fun setPropertyAsync(name: String, value: Double) = setProperty(name, value, ::setPropertyDoubleAsyncN)
    suspend fun setPropertyAsync(name: String, value: Boolean) = setProperty(name, value, ::setPropertyFlagAsyncN)

    fun setProperty(name: String, value: String) = setPropertyStringN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Long) = setPropertyLongN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Double) = setPropertyDoubleN(nativePointer.address, name, value)
    fun setProperty(name: String, value: Boolean) = setPropertyFlagN(nativePointer.address, name, value)

    private suspend fun <T : Any> getProperty(
        name: String,
        type: KClass<T>,
        fn: (Long, String, Long) -> Result<Unit>,
    ): Result<T?> = suspendCancellableCoroutine { continuation ->
        val subscriptionId = propertyGetCallbackId.getAndIncrement()
        propertyGetCallbacks[subscriptionId] = { result ->
            continuation.resume(result.map {
                logger.debug("Got property {}: {}", name, it)
                it?.let(type::cast)
            })
        }
        fn(nativePointer.address, name, subscriptionId).onFailure {
            propertyGetCallbacks.remove(subscriptionId)
            logger.error("Failed to get property $name", it)
            continuation.resume(Result.failure(it))
        }
    }

    suspend fun getPropertyStringAsync(name: String) = getProperty(name, String::class, ::getPropertyStringAsyncN)
    suspend fun getPropertyLongAsync(name: String) = getProperty(name, Long::class, ::getPropertyLongAsyncN)
    suspend fun getPropertyDoubleAsync(name: String) = getProperty(name, Double::class, ::getPropertyDoubleAsyncN)
    suspend fun getPropertyFlagAsync(name: String) = getProperty(name, Boolean::class, ::getPropertyFlagAsyncN)

    fun getPropertyString(name: String) = getPropertyStringN(nativePointer.address, name)
    fun getPropertyLong(name: String) = getPropertyLongN(nativePointer.address, name)
    fun getPropertyDouble(name: String) = getPropertyDoubleN(nativePointer.address, name)
    fun getPropertyFlag(name: String) = getPropertyFlagN(nativePointer.address, name)

    private fun subscribe(name: String, type: KClass<*>, fn: (Long, String, Long) -> Result<Unit>): Result<Unit> {
        if (propertySubscriptions.containsKey(name)) {
            logger.debug("Property $name is already being observed")
            return Result.success(Unit)
        }
        logger.debug("Observing property $name")
        val subscriptionId = subscriptionId.getAndIncrement()
        return fn(nativePointer.address, name, subscriptionId)
            .map { propertySubscriptions[name] = subscriptionId to type }
    }

    fun observePropertyString(name: String) = subscribe(name, String::class, ::observePropertyStringN)
    fun observePropertyLong(name: String) = subscribe(name, Long::class, ::observePropertyLongN)
    fun observePropertyDouble(name: String) = subscribe(name, Double::class, ::observePropertyDoubleN)
    fun observePropertyFlag(name: String) = subscribe(name, Boolean::class, ::observePropertyFlagN)

    fun unobserveProperty(name: String): Result<Unit> {
        val (id, _) = propertySubscriptions[name] ?: run {
            logger.debug("Property $name is not being observed")
            return Result.success(Unit)
        }
        return unobservePropertyN(nativePointer.address, id)
    }

    private class Sharing(
        private val name: String,
        private val subscribe: (name: String) -> Result<Unit>,
        private val unsubscribe: (name: String) -> Result<Unit>,
        private val wrapped: SharingStarted,
    ) : SharingStarted {
        override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> {
            return wrapped.command(subscriptionCount).onEach { command ->
                when (command) {
                    SharingCommand.START -> subscribe(name).getOrThrow()
                    SharingCommand.STOP,
                    SharingCommand.STOP_AND_RESET_REPLAY_CACHE,
                        -> unsubscribe(name).getOrThrow()
                }
            }
        }
    }

    suspend fun propertyFlowString(name: String): StateFlow<String?> {
        val initialValue = getPropertyStringAsync(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<StringProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyString, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowLong(name: String): StateFlow<Long?> {
        val initialValue = getPropertyLong(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<LongProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyLong, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowDouble(name: String): StateFlow<Double?> {
        val initialValue = getPropertyDouble(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<DoubleProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyDouble, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowFlag(name: String): StateFlow<Boolean?> {
        val initialValue = getPropertyFlag(name).getOrThrow()
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<FlagProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyFlag, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend inline fun <reified E : Any> propertyFlow(name: String) = when (E::class) {
        String::class -> propertyFlowString(name).mapState { it as E? }
        Long::class -> propertyFlowLong(name).mapState { it as E? }
        Double::class -> propertyFlowDouble(name).mapState { it as E? }
        Boolean::class -> propertyFlowFlag(name).mapState { it as E? }
        else -> error("Unsupported property type: ${E::class}")
    }

    fun command(command: Collection<String>) = commandN(nativePointer.address, command.toTypedArray())
    fun command(command: String) = commandStringN(nativePointer.address, command)

    override suspend fun onPropertyChanged(name: String, value: Any?) {
        val type = value?.javaClass?.kotlin ?: propertySubscriptions[name]?.second
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

    override suspend fun onPropertyGet(subscriptionId: Long, result: Result<Any?>) {
        propertyGetCallbacks.remove(subscriptionId)?.invoke(result)
    }

    override suspend fun onPropertySet(subscriptionId: Long, result: Result<Unit>) {
        propertySetCallbacks.remove(subscriptionId)?.invoke(result)
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
    fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
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

    private data class PropertyGet(val subscriptionId: Long, val result: Result<Any?>) : Event {
        override suspend fun call(listener: MPVAsyncListener) {
            listener.onPropertyGet(subscriptionId, result)
        }
    }

    private data class PropertySet(val subscriptionId: Long, val result: Result<Unit>) : Event {
        override suspend fun call(listener: MPVAsyncListener) {
            listener.onPropertySet(subscriptionId, result)
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

    override fun onPropertyGet(subscriptionId: Long, result: Result<Any?>) {
        events.add(PropertyGet(subscriptionId, result))
    }

    override fun onPropertySet(subscriptionId: Long, result: Result<Unit>) {
        events.add(PropertySet(subscriptionId, result))
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

private external fun getPropertyStringAsyncN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun getPropertyLongAsyncN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun getPropertyDoubleAsyncN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun getPropertyFlagAsyncN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun getPropertyStringN(handle: Long, name: String): Result<String?>
private external fun getPropertyLongN(handle: Long, name: String): Result<Long?>
private external fun getPropertyDoubleN(handle: Long, name: String): Result<Double?>
private external fun getPropertyFlagN(handle: Long, name: String): Result<Boolean?>

private external fun setPropertyStringAsyncN(
    handle: Long,
    name: String,
    value: String,
    subscriptionId: Long,
): Result<Unit>

private external fun setPropertyLongAsyncN(handle: Long, name: String, value: Long, subscriptionId: Long): Result<Unit>
private external fun setPropertyDoubleAsyncN(
    handle: Long,
    name: String,
    value: Double,
    subscriptionId: Long,
): Result<Unit>

private external fun setPropertyFlagAsyncN(
    handle: Long,
    name: String,
    value: Boolean,
    subscriptionId: Long,
): Result<Unit>

private external fun setPropertyStringN(handle: Long, name: String, value: String): Result<Unit>
private external fun setPropertyLongN(handle: Long, name: String, value: Long): Result<Unit>
private external fun setPropertyDoubleN(handle: Long, name: String, value: Double): Result<Unit>
private external fun setPropertyFlagN(handle: Long, name: String, value: Boolean): Result<Unit>

private external fun observePropertyStringN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun observePropertyLongN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun observePropertyDoubleN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun observePropertyFlagN(handle: Long, name: String, subscriptionId: Long): Result<Unit>
private external fun unobservePropertyN(handle: Long, subscriptionId: Long): Result<Unit>

private external fun createRenderN(handle: Long, self: MPV.Render): Result<Long>
private external fun destroyRenderN(handle: Long)
private external fun renderN(handle: Long, fbo: Int, width: Int, height: Int, glInternalFormat: Int): Result<Unit>
