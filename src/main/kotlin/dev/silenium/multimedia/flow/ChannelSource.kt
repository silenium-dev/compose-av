package dev.silenium.multimedia.core.flow

import dev.silenium.multimedia.core.util.AVException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.time.Duration.Companion.milliseconds

abstract class ChannelSource<E : AutoCloseable>(val channel: Channel<E> = Channel(Channel.RENDEZVOUS)) :
    ReceiveChannel<E> by channel, AutoCloseable {
    private val job = CoroutineScope(Dispatchers.Default).launch {
        var item: E? = null
        try {
            while (isActive) {
                val result = runCatching { next() }.getOrNull() ?: continue
                if (result.isSuccess) {
                    item = result.getOrThrow()
                    channel.send(item)
                    item = null
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
        } catch (e: CancellationException) {
            println("ChannelSource cancelled: $e")
        } catch (e: ClosedSendChannelException) {
            println("Channel closed: $e")
        } catch (e: Throwable) {
            println("ChannelSource error: $e")
            throw e
        } finally {
            item?.close()
            channel.close()
        }
    }

    protected abstract fun next(): Result<E>

    override fun close() = runBlocking {
        job.cancelAndJoin()
        println("Source closed: ${this@ChannelSource}")
    }
}
