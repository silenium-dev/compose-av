package dev.silenium.multimedia.core.decode

import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.data.NativeCleanable
import dev.silenium.multimedia.core.data.Packet
import dev.silenium.multimedia.core.demux.Stream
import dev.silenium.multimedia.core.flow.FlowSource
import dev.silenium.multimedia.core.flow.Sink
import dev.silenium.multimedia.core.render.GLRenderInterop
import dev.silenium.multimedia.core.util.asFrameResult
import dev.silenium.multimedia.core.util.asUnitResult

abstract class Decoder<T : Decoder<T>>(val stream: Stream) : Sink<Packet>, FlowSource<Frame>(), NativeCleanable {
    /**
     * Must be called with an OpenGL context bound.
     */
    abstract fun createGLRenderInterop(): GLRenderInterop<T>

    @Synchronized
    open fun releaseDecoder(pointer: Long) = releaseDecoderN(pointer)

    @Synchronized
    override fun submit(value: Packet): Result<Unit> {
        return submitN(nativePointer.address, value.nativePointer.address).asUnitResult()
    }

    @Synchronized
    override fun next(): Result<Frame> {
        println("Decoder.receive")
        return receiveN(nativePointer.address).mapCatching { it.asFrameResult(stream.timeBase).getOrThrow() }
    }

    override fun close() {
        super<FlowSource>.close()
        super<NativeCleanable>.close()
    }
}

private external fun releaseDecoderN(decoder: Long)
private external fun submitN(decoder: Long, packet: Long): Int
private external fun receiveN(decoder: Long): Result<Long>
