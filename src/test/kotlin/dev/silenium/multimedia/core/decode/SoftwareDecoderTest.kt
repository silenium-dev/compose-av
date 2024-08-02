package dev.silenium.multimedia.core.decode

import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.flow.process
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class SoftwareDecoderTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    this::class.java.classLoader.getResourceAsStream("1080p.webm").use {
        videoFile.outputStream().use(it::copyTo)
    }
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test SoftwareDecoder") {
        val demuxer = FileDemuxer(videoFile)
        val decoder = SoftwareDecoder(demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO })
        demuxer.process(0, decoder).take(1).collect { frame ->
            frame.timeBase shouldBe demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }.timeBase
            println(frame.buf.first()?.limit())
        }
        decoder.close()
        demuxer.close()
    }
})
