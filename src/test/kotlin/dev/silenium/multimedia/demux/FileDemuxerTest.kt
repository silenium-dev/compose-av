package dev.silenium.multimedia.demux

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class FileDemuxerTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test FileDemuxer") {
        FileDemuxerTest::class.java.classLoader.getResourceAsStream("video.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        val demuxer = FileDemuxer(videoFile)
        val packet = demuxer.nextPacket().shouldBeSuccess()
        packet.size shouldBeGreaterThan 0
        packet.data.limit() shouldBe packet.size
        packet.close()
        demuxer.close()
    }
})
