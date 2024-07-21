package dev.silenium.multimedia.decode

import dev.silenium.multimedia.data.NativePointer
import dev.silenium.multimedia.data.asNativePointer
import dev.silenium.multimedia.demux.Stream

class VaapiDecoder(stream: Stream, val vaDevice: String) : Decoder(stream) {
    override val nativePointer: NativePointer =
        createDecoderN(stream.nativePointer.address, vaDevice).getOrThrow()
            .asNativePointer(::releaseDecoder)
    val vaDisplay by lazy { getVADisplayN(nativePointer.address) }
}

private external fun createDecoderN(stream: Long, vaDevice: String): Result<Long>
private external fun getVADisplayN(decoder: Long): Long
