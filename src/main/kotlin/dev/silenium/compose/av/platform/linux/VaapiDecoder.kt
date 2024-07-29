package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.data.NativePointer
import dev.silenium.compose.av.data.Packet
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.decode.Decoder
import dev.silenium.compose.av.demux.Stream
import dev.silenium.compose.av.util.Natives
import dev.silenium.compose.av.util.asFrameResult
import dev.silenium.compose.av.util.asUnitResult
import dev.silenium.compose.gl.context.GLXContext

class VaapiDecoder(stream: Stream, val type: Device) : Decoder<VaapiDecoder>(stream) {
    sealed class Device(val create: (stream: Long) -> Result<Long>) {
        data class GLX(
            val glxContext: GLXContext = GLXContext.fromCurrent()
                ?: error("No context current, please specify explicit context")
        ) : Device({ createGLXDecoderN(it, glxContext.display) })

        data class DRM(val drmDevice: String) : Device({ createDRMDecoderN(it, drmDevice) })
    }

    override val nativePointer: NativePointer =
        type.create(stream.nativePointer.address).getOrThrow()
            .asNativePointer(::releaseDecoder)

    val vaDisplay by lazy { getVADisplayN(nativePointer.address) }

    override fun createGLRenderInterop() = VAGLXRenderInterop(this)
    override fun releaseDecoder(pointer: Long) = releaseDecoderN(pointer)

    override fun submit(packet: Packet): Result<Unit> {
        return submitN(nativePointer.address, packet.nativePointer.address).asUnitResult()
    }

    override fun receive(): Result<Frame> {
        return receiveN(nativePointer.address).mapCatching { it.asFrameResult(stream).getOrThrow() }
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun releaseDecoderN(decoder: Long)
private external fun submitN(decoder: Long, packet: Long): Int
private external fun receiveN(decoder: Long): Result<Long>

private external fun createDRMDecoderN(stream: Long, vaDevice: String): Result<Long>
private external fun createGLXDecoderN(stream: Long, glxDisplay: Long): Result<Long>
private external fun getVADisplayN(decoder: Long): Long
