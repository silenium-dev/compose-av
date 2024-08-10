package dev.silenium.multimedia.core.demux

import dev.silenium.libs.flows.api.FlowItem
import dev.silenium.libs.flows.base.SourceBase
import dev.silenium.multimedia.core.data.NativeCleanable
import dev.silenium.multimedia.core.data.NativePointer
import dev.silenium.multimedia.core.data.Packet
import dev.silenium.multimedia.core.data.times
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.shouldIgnore
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

abstract class Demuxer(override val nativePointer: NativePointer) :
    SourceBase<Packet, Stream>(),
    NativeCleanable {
    override val outputMetadata: Map<UInt, Stream> by ::streams
    private var job: Job? = null

    fun duration(stream: Stream): Duration? =
        stream.duration.takeIf { it > Long.MIN_VALUE }?.let { (it * stream.timeBase).seconds } ?: duration

    protected open suspend fun CoroutineScope.run() {
        while (isActive) {
            val result = nextPacketN(nativePointer.address)
            if (result.isFailure) {
                val e = result.exceptionOrNull()!!
                if (e.shouldIgnore) continue
                throw e
            }
            Packet(result.getOrThrow(), streams[streamIndexN(result.getOrThrow()).toUInt()]!!).use {
                publish(FlowItem(it.stream.index, it.stream, it))
            }
        }
    }

    fun start(coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        job = coroutineScope.launch { run() }
    }

    override fun close() {
        println("Demuxer.close")
        runBlocking { job?.cancelAndJoin() }
        super<SourceBase>.close()
        super<NativeCleanable>.close()
    }

    fun seek(timestamp: Duration) =
        seekN(nativePointer.address, timestamp.inWholeMicroseconds)

    /**
     * @return byte position
     */
    val position by lazy { positionN(nativePointer.address) }
    val duration by lazy { durationN(nativePointer.address).takeIf { it > 0 }?.microseconds }
    val isSeekable by lazy { isSeekableN(nativePointer.address) }
    val streams: Map<UInt, Stream> by lazy {
        (0u until streamCountN(nativePointer.address).toUInt())
            .associateWith { streamN(nativePointer.address, it.toLong()) }
            .mapValues { Stream(it.value.getOrThrow()) }
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun nextPacketN(nativeContext: Long): Result<Long>
private external fun positionN(nativeContext: Long): Long
private external fun durationN(nativeContext: Long): Long
private external fun seekN(nativeContext: Long, timestampUs: Long): Result<Int>
private external fun isSeekableN(nativeContext: Long): Boolean

private external fun streamCountN(nativeContext: Long): Long
private external fun streamN(nativeContext: Long, index: Long): Result<Long>
private external fun streamIndexN(packet: Long): Long
