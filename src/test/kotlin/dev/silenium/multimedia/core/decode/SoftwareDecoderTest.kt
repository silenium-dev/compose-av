package dev.silenium.multimedia.core.decode

import dev.silenium.libs.flows.impl.FlowGraph
import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.demux.FileDemuxer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        val decoder = Decoder()
        val graph = FlowGraph {
            val demuxerSource = source(demuxer, "demuxer")
            val decoderTransformer = sink(decoder, "decoder")
            connect(demuxerSource to decoderTransformer)
        }

        val started = CompletableDeferred<Unit>()
        val frameDeferred = async {
            started.complete(Unit)
            decoder.flow.first().value
        }
        started.await()
        demuxer.start()
        val frame = frameDeferred.await()
        graph.close()

        frame.timeBase shouldBe demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }.timeBase
        println(frame.buf.first()?.size)
        frame.close()
    }
})
