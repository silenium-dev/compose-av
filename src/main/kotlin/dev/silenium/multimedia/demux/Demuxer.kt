package dev.silenium.multimedia.demux

import dev.silenium.multimedia.data.Packet
import kotlin.time.Duration

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
}
