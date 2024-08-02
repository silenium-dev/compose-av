package dev.silenium.multimedia.core.flow

import dev.silenium.multimedia.core.data.Packet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel

suspend fun <T, I> ReceiveChannel<Packet>.process(
    stream: Int,
    sink: T
): ReceiveChannel<I> where T : Sink<Packet>, T : ChannelSource<I>, I : AutoCloseable = coroutineScope {
    launch {
        for (packet in this@process) {
            if (packet.stream.index == stream) {
                while (isActive) {
                    val result = sink.submit(packet)
                    if (result.isSuccess) break
                    if (result.isFailure) {
                        val e = result.exceptionOrNull() ?: error("Unknown error")
                        if (!e.shouldIgnore) throw e
                    }
                }
            }
            packet.close()
        }
        sink.close()
    }
    sink
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T, I, S> ReceiveChannel<S>.processConstructed(
    sinkConstructor: (S) -> T
): ReceiveChannel<I> where T : Sink<S>, T : ChannelSource<I>, I : AutoCloseable, S : AutoCloseable = coroutineScope {
    val sink = CompletableDeferred<T>()
    launch {
        for (item in this@processConstructed) {
            if (!sink.isCompleted) sink.complete(sinkConstructor(item))
            while (true) {
                val result = sink.getCompleted().submit(item)
                if (result.isSuccess) break
                if (result.isFailure) {
                    val e = result.exceptionOrNull() ?: error("Unknown error")
                    if (!e.shouldIgnore) throw e
                }
            }
            item.close()
        }
    }
    sink.await()
}
