package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.NativeCleanable
import dev.silenium.multimedia.core.data.Packet
import dev.silenium.multimedia.core.data.asNativePointer
import dev.silenium.multimedia.core.util.AVException
import dev.silenium.multimedia.core.util.Natives
import dev.silenium.multimedia.core.util.asAVErrorString
import java.net.URL
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class FileDemuxer(url: URL) : Demuxer(), NativeCleanable {
    constructor(path: Path) : this(path.toUri().toURL())

    override val nativePointer = initializeNativeContextN(url.toString()).also {
        if (it <= 0L) {
            error("Failed to initialize native context")
        }
    }.asNativePointer(::releaseNativeContextN)

    override fun next(): Result<Packet> {
        val packetPointer = nextPacketN(nativePointer.address)
        if (packetPointer < 0L) {
            return Result.failure(AVException("get next packet", packetPointer.toInt()))
        } else if (packetPointer == 0L) {
            return Result.failure(AVException("get next packet", -12 /* ENOMEM */))
        }
        return Result.success(Packet(packetPointer, streams[streamIndexN(packetPointer).toUInt()]!!))
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
    override val streams: Map<UInt, Stream> by lazy {
        (0u until streamCountN(nativePointer.address).toUInt())
            .associateWith { streamN(nativePointer.address, it.toLong()) }
            .mapValues { Stream(it.value) }
    }

    override fun close() {
        super<Demuxer>.close()
        super<NativeCleanable>.close()
        println("FileDemuxer closed")
    }

    companion object {
        init {
            Natives.ensureLoaded()
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
private external fun streamIndexN(packet: Long): Long
