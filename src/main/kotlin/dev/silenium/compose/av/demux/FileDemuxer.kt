package dev.silenium.compose.av.demux

import dev.silenium.compose.av.data.NativeCleanable
import dev.silenium.compose.av.data.Packet
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.util.NativeLoader
import dev.silenium.compose.av.util.asAVErrorString
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class FileDemuxer(url: URL) : Demuxer, NativeCleanable {
    constructor(path: Path) : this(path.toUri().toURL())

    override val nativePointer = initializeNativeContextN(url.toString()).also {
        if (it <= 0L) {
            error("Failed to initialize native context")
        }
    }.asNativePointer(::releaseNativeContextN)

    override fun nextPacket(): Result<Packet> {
        val packetPointer = nextPacketN(nativePointer.address)
        if (packetPointer < 0L) {
            return Result.failure(IOException("Failed to get next packet: ${packetPointer.toInt().asAVErrorString()}"))
        } else if (packetPointer == 0L) {
            return Result.failure(IOException("Could not allocate packet"))
        }
        return Result.success(Packet(packetPointer))
    }

    override fun seek(timestamp: Duration) {
        val result = seekN(nativePointer.address, timestamp.inWholeMicroseconds)
        check(result >= 0) {
            "Failed to seek to $timestamp: ${result.asAVErrorString()}"
        }
    }

    override val duration by lazy { durationN(nativePointer.address).takeIf { it > 0 }?.microseconds }
    override val position by lazy { positionN(nativePointer.address) }
    override val isSeekable by lazy { isSeekableN(nativePointer.address) }
    override val streams: List<Stream> by lazy {
        (0 until streamCountN(nativePointer.address))
            .map { streamN(nativePointer.address, it) }
            .map(::Stream)
    }

    companion object {
        init {
            NativeLoader.ensureLoaded()
        }
    }
}

private external fun initializeNativeContextN(url: String): Long
private external fun releaseNativeContextN(nativeContext: Long)
private external fun nextPacketN(nativeContext: Long): Long
private external fun positionN(nativeContext: Long): Long
private external fun durationN(nativeContext: Long): Long
private external fun seekN(nativeContext: Long, timestampUs: Long): Int
private external fun isSeekableN(nativeContext: Long): Boolean

private external fun streamCountN(nativeContext: Long): Long
private external fun streamN(nativeContext: Long, index: Long): Long
