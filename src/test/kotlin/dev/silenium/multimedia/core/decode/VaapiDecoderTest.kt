package dev.silenium.multimedia.core.decode

import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.flow.process
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
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
                demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO },
                VaapiDeviceContext.DRM("/dev/dri/renderD128")
            )
        demuxer.process(0, decoder).take(1).collect { frame ->
            frame.timeBase shouldBe demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }.timeBase
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
        }
        decoder.close()
        demuxer.close()
    }
})
