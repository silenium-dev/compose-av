package dev.silenium.compose.av.decode

import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.data.NativeCleanable
import dev.silenium.compose.av.data.Packet
import dev.silenium.compose.av.demux.Stream
import dev.silenium.compose.av.util.asFrameResult
import dev.silenium.compose.av.util.asUnitResult

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
