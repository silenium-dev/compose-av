package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.Packet
import dev.silenium.multimedia.core.data.times
import dev.silenium.multimedia.core.flow.FlowSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class Demuxer : FlowSource<Packet>(), AutoCloseable {
    abstract fun seek(timestamp: Duration)

    /**
     * @return byte position
     */
    abstract val position: Long
    abstract val duration: Duration?
    abstract val isSeekable: Boolean
    abstract val streams: Map<UInt, Stream>

    fun duration(stream: Stream): Duration? =
        stream.duration.takeIf { it > Long.MIN_VALUE }?.let { (it * stream.timeBase).seconds } ?: duration
}
