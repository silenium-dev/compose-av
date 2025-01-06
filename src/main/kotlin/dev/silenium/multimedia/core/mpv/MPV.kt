package dev.silenium.multimedia.core.mpv

import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.multimedia.compose.util.mapState
import dev.silenium.multimedia.core.util.NativeCleanable
import dev.silenium.multimedia.core.util.NativePointer
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.asNativePointer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.lwjgl.opengl.GL
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.time.Duration.Companion.milliseconds

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
    private val commandReplyCallbacks = ConcurrentHashMap<Long, (Result<Unit>) -> Unit>()
    private val commandReplyCallbackId = AtomicLong(0)

    private val propertyUpdates = MutableSharedFlow<Property<*>>()

    @OptIn(ExperimentalContracts::class)
    private inline fun <R : Any> guard(other: R? = null, block: () -> Result<R>): Result<R> {
        contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
        if (!initialized.get()) {
            if (other != null) {
                return Result.success(other)
            }
            return Result.failure(IllegalStateException("MPV is not initialized"))
        }
        return block()
    }

    @OptIn(ExperimentalContracts::class)
    @JvmName("guardNonNull")
    private inline fun <R> guardNonNull(other: R? = null, block: () -> Result<R?>): Result<R?> {
        contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
        if (!initialized.get()) {
            if (other != null) {
                return Result.success(other)
            }
            return Result.failure(IllegalStateException("MPV is not initialized"))
        }
        return block()
    }

    override val nativePointer = NativePointer(createN().getOrThrow()) {
        destroyN(it)
    }

    fun setOption(name: String, value: String) {
        if (nativePointer.closed) {
            error("MPV is closed")
        }
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
    ): Result<Unit> = guard(Unit) {
        suspendCancellableCoroutine { continuation ->
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
    ): Result<T?> = guardNonNull<T>(null) {
        suspendCancellableCoroutine { continuation ->
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
    }

    suspend fun getPropertyStringAsync(name: String) = getProperty(name, String::class, ::getPropertyStringAsyncN)
    suspend fun getPropertyLongAsync(name: String) = getProperty(name, Long::class, ::getPropertyLongAsyncN)
    suspend fun getPropertyDoubleAsync(name: String) = getProperty(name, Double::class, ::getPropertyDoubleAsyncN)
    suspend fun getPropertyFlagAsync(name: String) = getProperty(name, Boolean::class, ::getPropertyFlagAsyncN)

    fun getPropertyString(name: String) = getPropertyStringN(nativePointer.address, name)
    fun getPropertyLong(name: String) = getPropertyLongN(nativePointer.address, name)
    fun getPropertyDouble(name: String) = getPropertyDoubleN(nativePointer.address, name)
    fun getPropertyFlag(name: String) = getPropertyFlagN(nativePointer.address, name)

    private fun subscribe(name: String, type: KClass<*>, fn: (Long, String, Long) -> Result<Unit>): Result<Unit> =
        guard(Unit) {
            runCatching {
                propertySubscriptions.computeIfAbsent(name) {
                    logger.debug("Observing property $name")
                    val subscriptionId = subscriptionId.getAndIncrement()
                    fn(nativePointer.address, name, subscriptionId)
                        .map { subscriptionId to type }.getOrThrow()
                }
            }.map {}
        }

    fun observePropertyString(name: String) = subscribe(name, String::class, ::observePropertyStringN)
    fun observePropertyLong(name: String) = subscribe(name, Long::class, ::observePropertyLongN)
    fun observePropertyDouble(name: String) = subscribe(name, Double::class, ::observePropertyDoubleN)
    fun observePropertyFlag(name: String) = subscribe(name, Boolean::class, ::observePropertyFlagN)

    fun unobserveProperty(name: String): Result<Unit> = guard(Unit) {
        propertySubscriptions.remove(name)?.let { (id, _) ->
            logger.debug("Unobserving property $name")
            unobservePropertyN(nativePointer.address, id)
        } ?: run {
            logger.debug("Property $name is not being observed")
            Result.success(Unit)
        }
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
                    SharingCommand.START -> subscribe(name).getOrElse {
                        logger.error("Failed to subscribe to property $name", it)
                        return@onEach
                    }

                    SharingCommand.STOP,
                    SharingCommand.STOP_AND_RESET_REPLAY_CACHE,
                        -> unsubscribe(name).getOrElse {
                        logger.error("Failed to unsubscribe from property $name", it)
                        return@onEach
                    }
                }
            }
        }
    }

    suspend fun propertyFlowString(name: String): StateFlow<String?> {
        val initialValue = getPropertyStringAsync(name).getOrElse {
            logger.error("Failed to get initial value for property $name", it)
            null
        }
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<StringProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyString, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowLong(name: String): StateFlow<Long?> {
        val initialValue = getPropertyLongAsync(name).getOrElse {
            logger.error("Failed to get initial value for property $name", it)
            null
        }
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<LongProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyLong, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowDouble(name: String): StateFlow<Double?> {
        val initialValue = getPropertyDoubleAsync(name).getOrElse {
            logger.error("Failed to get initial value for property $name", it)
            null
        }
        val flow = propertyUpdates.filter { it.name == name }.filterIsInstance<DoubleProperty>().map { it.value }
        return flow.stateIn(
            CoroutineScope(EmptyCoroutineContext),
            Sharing(name, ::observePropertyDouble, ::unobserveProperty, SharingStarted.WhileSubscribed()),
            initialValue,
        )
    }

    suspend fun propertyFlowFlag(name: String): StateFlow<Boolean?> {
        val initialValue = getPropertyFlagAsync(name).getOrElse {
            logger.error("Failed to get initial value for property $name", it)
            null
        }
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

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Any> getPropertyAsync(name: String): Result<T?> = when (T::class) {
        String::class -> getPropertyStringAsync(name)
        Long::class -> getPropertyLongAsync(name)
        Double::class -> getPropertyDoubleAsync(name)
        Boolean::class -> getPropertyFlagAsync(name)
        else -> error("Unsupported property type: ${T::class}")
    } as Result<T?>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getProperty(name: String): Result<T?> = when (T::class) {
        String::class -> getPropertyString(name)
        Long::class -> getPropertyLong(name)
        Double::class -> getPropertyDouble(name)
        Boolean::class -> getPropertyFlag(name)
        else -> error("Unsupported property type: ${T::class}")
    } as Result<T?>

    suspend fun <T : Any> setPropertyAsync(name: String, value: T) = when (value) {
        is String -> setPropertyAsync(name, value)
        is Long -> setPropertyAsync(name, value)
        is Double -> setPropertyAsync(name, value)
        is Boolean -> setPropertyAsync(name, value)
        else -> error("Unsupported property type: ${value::class}")
    }

    fun <T : Any> setProperty(name: String, value: T) = when (value) {
        is String -> setProperty(name, value)
        is Long -> setProperty(name, value)
        is Double -> setProperty(name, value)
        is Boolean -> setProperty(name, value)
        else -> error("Unsupported property type: ${value::class}")
    }

    fun command(command: Array<String>) = guard(Unit) {
        commandN(nativePointer.address, command)
    }

    fun command(command: String) = guard(Unit) {
        commandStringN(nativePointer.address, command)
    }

    @JvmName("commandAsyncVararg")
    suspend fun commandAsync(vararg command: String) = commandAsync(command.toList().toTypedArray())
    suspend fun commandAsync(command: Array<String>): Result<Unit> = guard(Unit) {
        suspendCancellableCoroutine { continuation ->
            val subscriptionId = commandReplyCallbackId.getAndIncrement()
            commandReplyCallbacks[subscriptionId] = { result ->
                continuation.resume(result)
            }
            commandAsyncN(nativePointer.address, command, subscriptionId).onFailure {
                commandReplyCallbacks.remove(subscriptionId)
                logger.error("Failed to execute command ${command.joinToString()}", it)
                continuation.resume(Result.failure(it))
            }
        }
    }

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

    override suspend fun onCommandReply(subscriptionId: Long, result: Result<Unit>) {
        commandReplyCallbacks.remove(subscriptionId)?.invoke(result)
    }

    fun createRender(advancedControl: Boolean = false, updateCallback: () -> Unit) =
        Render(this, advancedControl, updateCallback)

    class Render internal constructor(mpv: MPV, advancedControl: Boolean, private val updateCallback: () -> Unit) :
        NativeCleanable {
        override val nativePointer: NativePointer =
            createRenderN(mpv.nativePointer.address, this, advancedControl).getOrThrow()
                .asNativePointer(::destroyRenderN)

        @OptIn(ExperimentalContracts::class)
        private fun <R> guard(other: R? = null, block: () -> Result<R>): Result<R> {
            contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
            if (nativePointer.closed) {
                if (other != null) {
                    return Result.success(other)
                }
                return Result.failure(IllegalStateException("Render is closed"))
            }
            return block()
        }

        fun render(fbo: FBO): Result<Unit> = guard(Unit) {
            renderN(
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

    override fun close() {
        initialized.set(false)
        super.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MPV::class.java)

        init {
            Natives.ensureLoaded()
        }
    }
}

interface MPVAsyncListener {
    suspend fun onPropertyChanged(name: String, value: Any?)
    suspend fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    suspend fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
    suspend fun onCommandReply(subscriptionId: Long, result: Result<Unit>)
}

private interface MPVListener {
    fun onPropertyChanged(name: String, value: Any?)
    fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
    fun onCommandReply(subscriptionId: Long, result: Result<Unit>)
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

    private data class CommandReply(val subscriptionId: Long, val result: Result<Unit>) : Event {
        override suspend fun call(listener: MPVAsyncListener) {
            listener.onCommandReply(subscriptionId, result)
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

    override fun onCommandReply(subscriptionId: Long, result: Result<Unit>) {
        events.add(CommandReply(subscriptionId, result))
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
private external fun commandAsyncN(handle: Long, command: Array<String>, subscriptionId: Long): Result<Unit>

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

private external fun createRenderN(handle: Long, self: MPV.Render, advancedControl: Boolean): Result<Long>
private external fun destroyRenderN(handle: Long)
private external fun renderN(handle: Long, fbo: Int, width: Int, height: Int, glInternalFormat: Int): Result<Unit>
