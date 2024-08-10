package dev.silenium.multimedia.core.decode

import dev.silenium.libs.flows.api.FlowItem
import dev.silenium.libs.flows.base.JobTransformerBase
import dev.silenium.multimedia.core.data.*
import dev.silenium.multimedia.core.demux.Stream
import dev.silenium.multimedia.core.render.GLRenderInterop
import dev.silenium.multimedia.core.render.SoftwareGLRenderInterop
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.shouldIgnore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalUnsignedTypes::class)
open class Decoder :
    JobTransformerBase<Packet, Stream, Frame, FramePadMetadata>(CoroutineScope(Dispatchers.IO), 0u),
    NativeCleanable {
    override val nativePointer: NativePointer = createN().getOrThrow()
        .asNativePointer(::releaseDecoder)

    /**
     * Must be called with an OpenGL context bound.
     */
    open fun createGLRenderInterop(): GLRenderInterop<out Decoder> = SoftwareGLRenderInterop(this)

    @Synchronized
    open fun releaseDecoder(pointer: Long) = releaseN(pointer)

    open fun flush() = flushN(nativePointer.address)

    override suspend fun receive(item: FlowItem<Packet, Stream>): Result<Unit> {
        check(item.pad == 0u) { "Invalid pad" }
        check(inputMetadata.containsKey(0u)) { "Decoder not configured" }
        check(inputMetadata[0u] == item.metadata) { "Invalid stream" }
        while (coroutineContext.isActive) {
            val result = synchronized(this@Decoder) { submitN(nativePointer.address, item.value.nativePointer.address) }
            if (result.isFailure) {
                val e = result.exceptionOrNull()!!
                if (e.shouldIgnore) {
                    delay(1)
                    continue
                }
            }
            item.close()
            return result
        }
        throw CancellationException()
    }

    override suspend fun CoroutineScope.run() {
        while (isActive) {
            val result = synchronized(this@Decoder) { receiveN(nativePointer.address) }
                .map { Frame(it, inputMetadata[0u]!!.timeBase) }
            if (result.isFailure) {
                val e = result.exceptionOrNull()!!
                if (e.shouldIgnore) {
                    delay(1)
                    continue
                }
                throw e
            }
            result.getOrThrow().use {
                publish(FlowItem(0u, outputMetadata[0u]!!, it))
            }
        }
    }

    override fun configure(pad: UInt, metadata: Stream): Result<Unit> {
        val result = configureN(this, nativePointer.address, metadata.codecParameters.nativePointer.address)
        if (result.isFailure) return result
        return super.configure(pad, metadata)
    }

    override fun outputMetadata(inputMetadata: Stream): FramePadMetadata {
        return FramePadMetadata(
            inputMetadata.codecParameters.width,
            inputMetadata.codecParameters.height,
            inputMetadata.codecParameters.format,
            swFormat = null,
            isHW = false,
            framesContext = null,
            inputMetadata.codecParameters.colorSpace,
            inputMetadata.codecParameters.colorPrimaries,
            inputMetadata.codecParameters.colorRange,
            inputMetadata.codecParameters.colorTrc,
            inputMetadata.sampleAspectRatio,
            inputMetadata.timeBase,
        )
    }

    override fun close() {
        job?.cancel()
        super<JobTransformerBase>.close()
        super<NativeCleanable>.close()
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun createN(): Result<Long>
private external fun configureN(self: Decoder, decoder: Long, codecParameters: Long): Result<Unit>
private external fun releaseN(decoder: Long)

private external fun submitN(decoder: Long, packet: Long): Result<Unit>
private external fun receiveN(decoder: Long): Result<Long>
private external fun flushN(deviceContext: Long)
