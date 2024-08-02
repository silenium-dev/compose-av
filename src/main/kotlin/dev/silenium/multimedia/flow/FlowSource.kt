package dev.silenium.multimedia.core.flow

import dev.silenium.multimedia.core.util.AVException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

abstract class FlowSource<T : AutoCloseable> : Flow<T>, AutoCloseable {
    private val callbacks = CopyOnWriteArrayList<suspend (T) -> Unit>()
    private val finished = CompletableDeferred<Unit>()

    private val job = CoroutineScope(Dispatchers.Default).launch {
        while (isActive) {
            while (callbacks.isEmpty()) delay(1.milliseconds)

            val result = next()
            if (result.isSuccess) {
                val item = result.getOrThrow()
                callbacks.forEach {
                    if (!isActive) return@forEach
                    it(item)
                }
                item.close()
            } else {
                val e = result.exceptionOrNull() ?: error("Unknown error")
                if (e is AVException) {
                    when (e.error) {
                        -11, // EAGAIN
                        -12 -> {  // ENOMEM
                            delay(1.milliseconds)
                            continue
                        }

                        -541478725 -> break // AVERROR_EOF
                        else -> throw e
                    }
                }
            }
        }
    }.apply {
        invokeOnCompletion { finished.complete(Unit) }
    }

    protected abstract fun next(): Result<T>

    override suspend fun collect(collector: FlowCollector<T>) {
        if (finished.isCompleted) {
            return
        }
        callbacks.add(collector::emit)
        finished.await()
        println("Source finished: ${this@FlowSource}")
        callbacks.remove(collector::emit)
    }

    override fun close() = runBlocking {
        job.cancelAndJoin()
        println("Source closed: ${this@FlowSource}")
    }
}
