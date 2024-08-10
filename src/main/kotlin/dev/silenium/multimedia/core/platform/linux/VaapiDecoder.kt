package dev.silenium.multimedia.core.platform.linux

import dev.silenium.multimedia.core.data.*
import dev.silenium.multimedia.core.decode.Decoder
import dev.silenium.multimedia.core.demux.Stream
import dev.silenium.multimedia.core.hw.FramesContext
import dev.silenium.multimedia.core.render.SoftwareGLRenderInterop

class VaapiDecoder(val deviceContext: VaapiDeviceContext) : Decoder() {
    lateinit var framesContext: FramesContext

    override var nativePointer: NativePointer =
        createN(deviceContext.address).getOrThrow()
            .asNativePointer(::releaseDecoder)

    override fun createGLRenderInterop() = when (deviceContext) {
        is VaapiDeviceContext.DRM -> VAEGLRenderInterop(this)
        is VaapiDeviceContext.GLX -> SoftwareGLRenderInterop(this)
    }

    override fun configure(pad: UInt, metadata: Stream): Result<Unit> {
        if (::framesContext.isInitialized) error("already configured")
        framesContext = FramesContext(
            deviceContext,
            metadata.codecParameters.width,
            metadata.codecParameters.height,
            mapToNativeN(metadata.codecParameters.format.id).let(::fromId),
            initialPoolSize = 64,
        )
        return super.configure(pad, metadata)
    }

    override fun outputMetadata(inputMetadata: Stream): FramePadMetadata {
        return super.outputMetadata(inputMetadata).copy(
            isHW = true,
            format = AVPixelFormat.AV_PIX_FMT_VAAPI,
            swFormat = mapToNativeN(inputMetadata.codecParameters.format.id).let(::fromId),
            framesContext = framesContext,
        )
    }
}

private external fun createN(deviceContext: Long): Result<Long>

private external fun mapToNativeN(format: Int): Int
