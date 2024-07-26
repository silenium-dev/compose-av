package dev.silenium.compose.av.decode

import dev.silenium.compose.av.data.NativePointer
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.demux.Stream
import dev.silenium.compose.av.render.SoftwareGLRenderInterop

class SoftwareDecoder(stream: Stream) : Decoder<SoftwareDecoder>(stream) {
    override fun createGLRenderInterop() = SoftwareGLRenderInterop(this)

    override val nativePointer: NativePointer =
        createDecoderN(stream.nativePointer.address).getOrThrow()
            .asNativePointer(::releaseDecoder)
}

private external fun createDecoderN(stream: Long): Result<Long>
