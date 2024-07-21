package dev.silenium.multimedia.demux

import dev.silenium.compose.gl.util.Natives
import dev.silenium.multimedia.BuildConstants
import dev.silenium.multimedia.data.NativeCleanable
import dev.silenium.multimedia.data.Packet
import dev.silenium.multimedia.data.asNativePointer
import dev.silenium.multimedia.util.asAVErrorString
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

class FileDemuxer(url: URL) : Demuxer, NativeCleanable {
    constructor(path: Path) : this(path.toUri().toURL())

    init {
        Natives.load(BuildConstants.NativeLibName)
    }

    private enum class Errors(val code: Int) {
        EndOfFile(-1),
        EAgain(-2),
        ReadPacketFailed(-3);

        companion object {
            fun fromCode(code: Int): Errors {
                return entries.firstOrNull { it.code == code } ?: error("Unknown error code: $code")
            }
        }
    }

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

    override fun duration() = durationN(nativePointer.address).takeIf { it > 0 }?.microseconds
    override fun position() = positionN(nativePointer.address)
}

private external fun initializeNativeContextN(url: String): Long
private external fun releaseNativeContextN(nativeContext: Long)
private external fun nextPacketN(nativeContext: Long): Long
private external fun positionN(nativeContext: Long): Long
private external fun durationN(nativeContext: Long): Long
private external fun seekN(nativeContext: Long, timestampUs: Long): Int
