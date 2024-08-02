package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.AVMediaType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
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
        demuxer.streams.map { it.value.type } shouldContainExactlyInAnyOrder listOf(
            AVMediaType.AVMEDIA_TYPE_VIDEO,
        )
        demuxer.isSeekable shouldBe true
        demuxer.take(1).collect { packet ->
            packet.size shouldBeGreaterThan 0
            packet.data.limit() shouldBe packet.size
        }
        demuxer.close()
    }
})
