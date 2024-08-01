package dev.silenium.compose.av.decode

import dev.silenium.compose.av.data.AVMediaType
import dev.silenium.compose.av.demux.FileDemuxer
import dev.silenium.compose.av.platform.linux.VaapiDecoder
import dev.silenium.compose.av.platform.linux.VaapiDeviceContext
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

@OptIn(ExperimentalStdlibApi::class)
@RequiresTag("disabled")
class VaapiDecoderTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    this::class.java.classLoader.getResourceAsStream("video.webm").use {
        videoFile.outputStream().use(it::copyTo)
    }
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test VaapiDecoder") {
        val demuxer = FileDemuxer(videoFile)
        val decoder =
            VaapiDecoder(
                demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO },
                VaapiDeviceContext.DRM("/dev/dri/renderD128")
            )
        decoder.submit(demuxer.nextPacket().getOrThrow())
        val frame = decoder.receive().getOrThrow()
        frame.timeBase shouldBe demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }.timeBase
        println("VASurface: 0x${frame.rawData[3].toHexString()}")
        println("Format: ${frame.format}")
        println("Is HW: ${frame.isHW}")
        val swFrame = frame.transferToSW().getOrThrow()
        swFrame.buf.forEach {
            println("Plane: ${it?.limit()}")
        }
        val yPlane = swFrame.buf[0]
        println("Y Plane: ${yPlane?.limit()}")
        val uvPlane = swFrame.buf[1]
        println("UV Plane: ${uvPlane?.limit()}")
        println("Extent: ${swFrame.width}x${swFrame.height}")
        println("Format: ${swFrame.format}")
        println("Is HW: ${swFrame.isHW}")
        swFrame.close()
        frame.close()
        decoder.close()
        demuxer.close()
    }
})
