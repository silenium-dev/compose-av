package dev.silenium.compose.av.decode

import dev.silenium.compose.av.data.AVMediaType
import dev.silenium.compose.av.demux.FileDemuxer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class SoftwareDecoderTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    this::class.java.classLoader.getResourceAsStream("video.webm").use {
        videoFile.outputStream().use(it::copyTo)
    }
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test SoftwareDecoder") {
        val demuxer = FileDemuxer(videoFile)
        val decoder = SoftwareDecoder(demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO })
        decoder.submit(demuxer.nextPacket().getOrThrow())
        val frame = decoder.receive().getOrThrow()
        frame.stream shouldBe demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }
        println(frame.buf.first()?.limit())
        frame.close()
        decoder.close()
        demuxer.close()
    }
})
