package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.*
import dev.silenium.compose.av.filter.SimpleFilter
import dev.silenium.compose.av.util.Natives

/**
 * Uses VAAPI pipeline to convert YUV frames to RGB.
 * Falls back to opengl if VAAPI does not support the format.
 */
class VaapiYuvToRgbConversion(val deviceContext: VaapiDeviceContext, configure: Frame) : SimpleFilter, NativeCleanable {
    private val timeBase = configure.timeBase
    override val nativePointer: NativePointer =
        createN(deviceContext.nativePointer.address, configure.nativePointer.address, timeBase)
            .map { it.asNativePointer(::destroyN) }
            .getOrThrow()

    override fun submit(frame: Frame): Result<Unit> = submitN(nativePointer.address, frame.nativePointer.address)
    override fun receive(): Result<Frame> = receiveN(nativePointer.address).map { Frame(it, timeBase) }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun createN(deviceContext: Long, inputFrame: Long, timeBase: Rational): Result<Long>
private external fun destroyN(context: Long)

private external fun submitN(context: Long, frame: Long): Result<Unit>
private external fun receiveN(context: Long): Result<Long>
