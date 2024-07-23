package dev.silenium.compose.av.decode

import dev.silenium.multimedia.data.NativePointer
import dev.silenium.multimedia.data.asNativePointer
import dev.silenium.multimedia.demux.Stream

class SoftwareDecoder(stream: Stream) : Decoder(stream) {
    override val nativePointer: NativePointer =
        createDecoderN(stream.nativePointer.address).getOrThrow()
            .asNativePointer(::releaseDecoder)
}

private external fun createDecoderN(stream: Long): Result<Long>
