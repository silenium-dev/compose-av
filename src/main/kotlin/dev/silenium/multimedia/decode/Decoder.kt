package dev.silenium.multimedia.decode

import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.data.NativeCleanable
import dev.silenium.multimedia.data.Packet
import dev.silenium.multimedia.demux.Stream
import dev.silenium.multimedia.util.asFrameResult
import dev.silenium.multimedia.util.asUnitResult

abstract class Decoder(val stream: Stream) : NativeCleanable {
    open fun releaseDecoder(pointer: Long) = releaseDecoderN(pointer)

    open fun submit(packet: Packet): Result<Unit> {
        return submitN(nativePointer.address, packet.nativePointer.address).asUnitResult()
    }

    open fun receive(): Result<Frame> {
        return receiveN(nativePointer.address).mapCatching { it.asFrameResult(stream).getOrThrow() }
    }
}

private external fun releaseDecoderN(decoder: Long)
private external fun submitN(decoder: Long, packet: Long): Int
private external fun receiveN(decoder: Long): Result<Long>
