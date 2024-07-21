package dev.silenium.multimedia.decode

import dev.silenium.multimedia.demux.FileDemuxer
import dev.silenium.multimedia.demux.Stream
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

@OptIn(ExperimentalStdlibApi::class)
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
        val decoder = VaapiDecoder(demuxer.streams.first { it.type == Stream.Type.VIDEO }, "/dev/dri/renderD128")
        decoder.submit(demuxer.nextPacket().getOrThrow())
        val frame = decoder.receive().getOrThrow()
        frame.stream shouldBe demuxer.streams.first { it.type == Stream.Type.VIDEO }
        println("VASurface: 0x${frame.rawData[3].toHexString()}")
        frame.close()
        decoder.close()
        demuxer.close()
    }
})
