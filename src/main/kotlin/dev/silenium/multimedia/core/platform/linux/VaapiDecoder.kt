package dev.silenium.multimedia.core.platform.linux

import dev.silenium.multimedia.core.data.NativePointer
import dev.silenium.multimedia.core.data.asNativePointer
import dev.silenium.multimedia.core.decode.Decoder
import dev.silenium.multimedia.core.demux.Stream
import dev.silenium.multimedia.core.util.Natives

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
