package dev.silenium.multimedia.core.flow

import dev.silenium.multimedia.core.data.Packet
import dev.silenium.multimedia.core.util.AVException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

val Throwable.shouldIgnore
    get() = when (this) {
        is AVException -> when (error) {
            -11 -> true // EAGAIN
            -12 -> true // ENOMEM
            -541478725 -> true // AVERROR_EOF
            else -> false
        }

        else -> false
    }

fun <T, I> Flow<Packet>.process(
    stream: Int,
    sink: T
): Flow<I> where T : Sink<Packet>, T : FlowSource<I>, I : AutoCloseable =
    filter { it.stream.index == stream }.process(sink)

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T, I, S> Flow<S>.processConstructed(
    sinkConstructor: (S) -> T
): Flow<I> where T : Sink<S>, T : FlowSource<I>, I : AutoCloseable, S : AutoCloseable {
    val sink = CompletableDeferred<T>()
    onEach {
        if (!sink.isCompleted) sink.complete(sinkConstructor(it))
        while (true) {
            val result = sink.getCompleted().submit(it)
            if (result.isSuccess) break
            if (result.isFailure) {
                val e = result.exceptionOrNull() ?: error("Unknown error")
                if (!e.shouldIgnore) throw e
            }
        }
    }
        .onCompletion { if (sink.isCompleted) sink.getCompleted().close() }
        .launchIn(CoroutineScope(Dispatchers.IO))
    return sink.await()
}

fun <T, I, S> Flow<S>.process(
    sink: T
): Flow<I> where T : Sink<S>, T : FlowSource<I>, I : AutoCloseable, S : AutoCloseable {
    onEach {
        while (true) {
            val result = sink.submit(it)
            if (result.isSuccess) break
            if (result.isFailure) {
                val e = result.exceptionOrNull() ?: error("Unknown error")
                if (!e.shouldIgnore) throw e
            }
        }
    }
        .onCompletion { sink.close() }
        .launchIn(CoroutineScope(Dispatchers.IO))
    return sink
}
