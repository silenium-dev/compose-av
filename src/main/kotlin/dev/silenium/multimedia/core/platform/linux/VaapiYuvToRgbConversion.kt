package dev.silenium.multimedia.core.platform.linux

import dev.silenium.libs.flows.api.FlowItem
import dev.silenium.libs.flows.base.JobTransformerBase
import dev.silenium.multimedia.core.data.*
import dev.silenium.multimedia.core.hw.DeviceContext
import dev.silenium.multimedia.core.hw.FramesContext
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.shouldIgnore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * Uses VAAPI pipeline to convert YUV frames to RGB.
 * Falls back to opengl if VAAPI does not support the format.
 */
class VaapiYuvToRgbConversion :
    JobTransformerBase<Frame, FramePadMetadata, Frame, FramePadMetadata>(CoroutineScope(Dispatchers.IO), 0u),
    NativeCleanable {
    override lateinit var nativePointer: NativePointer
    private lateinit var outputFramesContext: FramesContext
    private var inputFramesContext: FramesContext? = null
    private lateinit var deviceContext: DeviceContext

    override fun configure(pad: UInt, metadata: FramePadMetadata): Result<Unit> {
        if (::nativePointer.isInitialized) {
            return Result.failure(IllegalStateException("Already configured"))
        }
        deviceContext = metadata.framesContext?.deviceContext?.clone()?.getOrThrow()
            ?: DeviceContext(AVHWDeviceType.AV_HWDEVICE_TYPE_VAAPI)
        inputFramesContext = when {
            metadata.framesContext != null -> metadata.framesContext.clone().getOrThrow()
            else -> FramesContext(
                deviceContext,
                metadata.width,
                metadata.height,
                metadata.format,
                initialPoolSize = 8,
            )
        }
        outputFramesContext = FramesContext(
            inputFramesContext!!.deviceContext,
            inputFramesContext!!.width,
            inputFramesContext!!.height,
            AVPixelFormat.AV_PIX_FMT_RGB0,
            initialPoolSize = 8,
        )
        nativePointer = createN(
            metadata,
            deviceContext.address,
            inputFramesContext!!.address,
            outputFramesContext.address,
        ).getOrThrow().asNativePointer(::destroyN)

        return super.configure(pad, metadata)
    }

    override fun outputMetadata(inputMetadata: FramePadMetadata): FramePadMetadata {
        return inputMetadata.copy(
            format = AVPixelFormat.AV_PIX_FMT_VAAPI,
            swFormat = AVPixelFormat.AV_PIX_FMT_RGB0,
        )
    }

    override suspend fun receive(item: FlowItem<Frame, FramePadMetadata>): Result<Unit> {
        println("PTS: ${item.value.pts}, VASurface: 0x${item.value.data[3].toString(16)}")
        while (coroutineContext.isActive) {
            val result = /*synchronized(this@VaapiYuvToRgbConversion) {*/
                submitN(
                    nativePointer.address,
                    item.value.nativePointer.address
                )
            /*}*/
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
            val result = /*synchronized(this@VaapiYuvToRgbConversion) { */receiveN(nativePointer.address)/* }*/
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

    override fun close() {
        job?.cancel()
        super<JobTransformerBase>.close()
        inputFramesContext?.close()
        outputFramesContext.close()
        deviceContext.close()
        super<NativeCleanable>.close()
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun createN(
    inputMetadata: FramePadMetadata,
    deviceContext: Long,
    inputFramesContext: Long,
    outputFramesContext: Long,
): Result<Long>

private external fun destroyN(context: Long)

private external fun submitN(context: Long, frame: Long): Result<Unit>
private external fun receiveN(context: Long): Result<Long>
