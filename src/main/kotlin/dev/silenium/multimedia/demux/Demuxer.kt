package dev.silenium.multimedia.demux

import dev.silenium.multimedia.data.Packet
import kotlin.time.Duration

interface Demuxer {
    fun nextPacket(): Result<Packet>
    fun seek(timestamp: Duration): Boolean
    fun duration(): Duration?
    fun position(): Duration?
}
