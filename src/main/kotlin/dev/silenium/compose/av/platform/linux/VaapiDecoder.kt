package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.NativePointer
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.decode.Decoder
import dev.silenium.compose.av.demux.Stream
import dev.silenium.compose.av.util.NativeLoader

class VaapiDecoder(stream: Stream, val vaDevice: String) : Decoder(stream) {
    override val nativePointer: NativePointer =
        createDecoderN(stream.nativePointer.address, vaDevice).getOrThrow()
            .asNativePointer(::releaseDecoder)
    val vaDisplay by lazy { getVADisplayN(nativePointer.address) }

    companion object {
        init {
            NativeLoader.ensureLoaded()
        }
    }
}

private external fun createDecoderN(stream: Long, vaDevice: String): Result<Long>
private external fun getVADisplayN(decoder: Long): Long
