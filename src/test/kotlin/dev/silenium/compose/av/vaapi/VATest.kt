package dev.silenium.compose.av.vaapi

import dev.silenium.compose.av.data.AVMediaType
import dev.silenium.compose.av.demux.FileDemuxer
import dev.silenium.compose.av.platform.linux.VaapiDecoder
import dev.silenium.compose.av.platform.linux.VaapiDeviceContext
import dev.silenium.compose.av.platform.linux.VaapiYuvToRgbConversion
import dev.silenium.compose.av.util.Mode
import dev.silenium.compose.av.util.savePNG
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import kotlin.io.path.outputStream

class VATest : FunSpec({
    val file = run {
        val videoFile = Files.createTempFile("video", ".webm")
        FileDemuxer::class.java.classLoader.getResourceAsStream("1080p.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }
    test("something") {
        val context = VaapiDeviceContext.DRM("/dev/dri/renderD128")
        println(context.display)
        val demuxer = FileDemuxer(file)
        println(demuxer.streams)
        val decoder = VaapiDecoder(demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }, context)
        repeat(100) {
            decoder.submit(demuxer.nextPacket().getOrThrow())
            var frame = decoder.receive()
            while (frame.isSuccess) {
                frame.getOrThrow().close()
                frame = decoder.receive()
            }
        }

        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        val frame = decoder.receive().getOrThrow()
        val conversion = VaapiYuvToRgbConversion(context, frame)
        conversion.submit(frame)
        val convertedFrame = conversion.receive().getOrThrow()

        convertedFrame.savePNG(0, Files.createTempFile("frame", ".png"), Mode.RGB0)

        println(convertedFrame)
        frame.close()
        println("Frame closed")
        convertedFrame.close()
        println("Converted frame closed")
        conversion.close()
        println("Conversion closed")
        decoder.close()
        println("Decoder closed")
        demuxer.close()
        println("Demuxer closed")
        context.close()
        println("Context closed")
    }
})
