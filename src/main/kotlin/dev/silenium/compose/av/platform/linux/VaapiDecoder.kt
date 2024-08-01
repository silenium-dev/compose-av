package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.NativePointer
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.decode.Decoder
import dev.silenium.compose.av.demux.Stream
import dev.silenium.compose.av.util.Natives

class VaapiDecoder(stream: Stream, val context: VaapiDeviceContext) : Decoder<VaapiDecoder>(stream) {
    override val nativePointer: NativePointer =
        createN(stream.nativePointer.address, context.nativePointer.address).getOrThrow()
            .asNativePointer(::releaseDecoder)

    override fun createGLRenderInterop() = when (context) {
        is VaapiDeviceContext.DRM -> VAEGLRenderInterop(this)
        is VaapiDeviceContext.GLX -> VAGLXRenderInterop(this)
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun createN(stream: Long, deviceContext: Long): Result<Long>
