package dev.silenium.multimedia.core.decode

import dev.silenium.libs.flows.buffer.BufferSink
import dev.silenium.libs.flows.impl.FlowGraph
import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.data.FramePadMetadata
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

@OptIn(ExperimentalStdlibApi::class)
@RequiresTag("vaapi")
class VaapiDecoderTest : FunSpec({
    val videoFile = Files.createTempFile("video", ".webm")
    this::class.java.classLoader.getResourceAsStream("1080p.webm").use {
        videoFile.outputStream().use(it::copyTo)
    }
    afterSpec {
        videoFile.deleteIfExists()
    }
    test("test VaapiDecoder") {
        val demuxer = FileDemuxer(videoFile)
        val decoder = VaapiDecoder(VaapiDeviceContext.DRM("/dev/dri/renderD128"))
        val bufferSink = BufferSink<Frame, FramePadMetadata>()
        val graph = FlowGraph {
            val demuxerSource = source(demuxer, "demuxer")
            val decoderTransformer = transformer(decoder, "decoder")
            connect(demuxerSource to decoderTransformer)
            val sink = sink(bufferSink, "sink")
            connect(decoderTransformer to sink)
        }

        val started = CompletableDeferred<Unit>()
        val frameDeferred = async {
            println("started")
            started.complete(Unit)
            val value = bufferSink.flow.first {
                println("FlowState: ${it[0u]?.size}")
                it[0u]?.size?.let { it >= 4 } == true
            }[0u]!!.last().value
            graph.close()
            println("Frame: $value")
            value
        }
        started.await()
        demuxer.start()
        val frame = frameDeferred.await()

        frame.timeBase shouldBe demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }.timeBase
        println("VASurface: 0x${frame.data[3].toHexString()}")
        println("PTS: ${frame.pts}")
        println("Format: ${frame.format}")
        println("Is HW: ${frame.isHW}")
        val swFrame = frame.transferToSW().getOrThrow()
        swFrame.buf.forEach {
            println("Plane: ${it?.size}")
        }
        val yPlane = swFrame.buf[0]
        println("Y Plane: ${yPlane?.size}")
        val uvPlane = swFrame.buf[1]
        println("UV Plane: ${uvPlane?.size}")
        println("Extent: ${swFrame.width}x${swFrame.height}")
        println("Format: ${swFrame.format}")
        println("Is HW: ${swFrame.isHW}")
        swFrame.close()
        println("Ref counts: ${frame.buf.joinToString { it?.refCount.toString() }}")
        frame.close()
    }
})
