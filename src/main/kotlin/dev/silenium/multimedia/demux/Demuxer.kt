package dev.silenium.multimedia.demux

import dev.silenium.multimedia.data.Packet
import kotlin.time.Duration

interface Demuxer {
    fun nextPacket(): Result<Packet>
    fun seek(timestamp: Duration)
    fun duration(): Duration?

    /**
     * @return byte position
     */
    fun position(): Long
}
