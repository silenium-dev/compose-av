package dev.silenium.multimedia.demux

import dev.silenium.multimedia.data.Packet
import dev.silenium.multimedia.data.times
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface Demuxer {
    fun nextPacket(): Result<Packet>
    fun seek(timestamp: Duration)

    /**
     * @return byte position
     */
    val position: Long
    val duration: Duration?
    val isSeekable: Boolean
    val streams: List<Stream>

    fun duration(stream: Stream): Duration? =
        stream.duration.takeIf { it > Long.MIN_VALUE }?.let { (it * stream.timeBase).seconds } ?: duration
}
