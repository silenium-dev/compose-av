package dev.silenium.compose.av.demux

import dev.silenium.compose.av.data.AVMediaType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.seconds

class FileDemuxerTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    this::class.java.classLoader.getResourceAsStream("1080p.webm").use {
        videoFile.outputStream().use(it::copyTo)
    }
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test FileDemuxer") {
        videoFile.shouldExist()
        val demuxer = FileDemuxer(videoFile)
        demuxer.duration.shouldNotBeNull() shouldBeGreaterThan 44.seconds
        shouldNotThrowAny {
            demuxer.seek(4.seconds)
        }
        demuxer.streams.map(Stream::type) shouldContainExactlyInAnyOrder listOf(
            AVMediaType.AVMEDIA_TYPE_VIDEO,
        )
        demuxer.isSeekable shouldBe true
        val packet = demuxer.nextPacket().shouldBeSuccess()
        packet.size shouldBeGreaterThan 0
        packet.data.limit() shouldBe packet.size
        packet.close()
        demuxer.close()
    }
})
