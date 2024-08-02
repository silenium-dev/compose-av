package dev.silenium.multimedia.core.platform.linux

import dev.silenium.multimedia.core.data.*
import dev.silenium.multimedia.core.filter.SimpleFilter
import dev.silenium.multimedia.core.util.Natives

/**
 * Uses VAAPI pipeline to convert YUV frames to RGB.
 * Falls back to opengl if VAAPI does not support the format.
 */
class VaapiYuvToRgbConversion(val deviceContext: VaapiDeviceContext, configure: Frame) : SimpleFilter(),
    NativeCleanable {
    private val timeBase = configure.timeBase
    override val nativePointer: NativePointer =
        createN(deviceContext.nativePointer.address, configure.nativePointer.address, timeBase)
            .map { it.asNativePointer(::destroyN) }
            .getOrThrow()

    @Synchronized
    override fun submit(value: Frame): Result<Unit> = submitN(nativePointer.address, value.nativePointer.address)

    @Synchronized
    override fun next(): Result<Frame> = receiveN(nativePointer.address).map { Frame(it, timeBase) }

    override fun close() {
        println("VaapiYuvToRgbConversion.close")
        super<SimpleFilter>.close()
        super<NativeCleanable>.close()
    }

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
