package dev.silenium.multimedia.core.decode

import dev.silenium.multimedia.core.data.NativePointer
import dev.silenium.multimedia.core.data.asNativePointer
import dev.silenium.multimedia.core.demux.Stream
import dev.silenium.multimedia.core.render.SoftwareGLRenderInterop

class SoftwareDecoder(stream: Stream) : Decoder<SoftwareDecoder>(stream) {
    override fun createGLRenderInterop() = SoftwareGLRenderInterop(this)

    override val nativePointer: NativePointer =
        createDecoderN(stream.nativePointer.address).getOrThrow()
            .asNativePointer(::releaseDecoder)
}

private external fun createDecoderN(stream: Long): Result<Long>
