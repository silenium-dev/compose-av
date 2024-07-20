package dev.silenium.multimedia.demux

import dev.silenium.compose.gl.util.Natives
import dev.silenium.multimedia.BuildConstants
import dev.silenium.multimedia.data.NativeCleanable
import dev.silenium.multimedia.data.Packet
import dev.silenium.multimedia.data.asNativePointer
import java.io.IOException
import java.nio.file.Path
import kotlin.time.Duration

class FileDemuxer(path: Path) : Demuxer, NativeCleanable {
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

    override val nativePointer = initializeNativeContextN().also {
        if (it <= 0L) {
            error("Failed to initialize native context")
        }
    }.asNativePointer(::releaseNativeContextN)

    override fun nextPacket(): Result<Packet> {
        val packetPointer = nextPacketN(nativePointer.address)
        if (packetPointer <= 0L) {
            return Result.failure(IOException("Failed to get next packet: ${Errors.fromCode(packetPointer.toInt())}"))
        }
        return Result.success(Packet(packetPointer))
    }

    override fun seek(timestamp: Duration): Boolean {
        TODO("Not yet implemented")
    }

    override fun duration(): Duration? {
        TODO("Not yet implemented")
    }

    override fun position(): Duration? {
        TODO("Not yet implemented")
    }
}

private external fun initializeNativeContextN(): Long
private external fun releaseNativeContextN(nativeContext: Long)
private external fun nextPacketN(nativeContext: Long): Long
