package dev.silenium.multimedia.core.vaapi

import dev.silenium.libs.flows.buffer.BufferSink
import dev.silenium.libs.flows.impl.FlowGraph
import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.data.FramePadMetadata
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import dev.silenium.multimedia.core.platform.linux.VaapiYuvToRgbConversion
import dev.silenium.multimedia.core.util.Mode
import dev.silenium.multimedia.core.util.savePNG
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.outputStream

//@RequiresTag("vaapi")
class VATest : FunSpec({
    val file = run {
        val videoFile = Files.createTempFile("video", ".webm")
        FileDemuxer::class.java.classLoader.getResourceAsStream("1080p.cut.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }
    test("something") {
        val demuxer = FileDemuxer(file)
        val context = VaapiDeviceContext.DRM("/dev/dri/renderD128")
        val decoder = VaapiDecoder(context)
        val filter: VaapiYuvToRgbConversion
        val bufferSink = BufferSink<Frame, FramePadMetadata>()
        println(context.display)
        println(demuxer.streams)

        val graph = FlowGraph {
            val demuxerSource = source(demuxer, "demuxer")
            val decoderTransformer = transformer(decoder, "decoder")
            connect(demuxerSource to decoderTransformer) { _, _, pad, metadata ->
                if (metadata.type == AVMediaType.AVMEDIA_TYPE_VIDEO) pad else null
            }
            filter = VaapiYuvToRgbConversion(decoder.outputMetadata.values.first(), decoder.framesContext)
            val sink = sink(bufferSink, "sink")
            connect(decoderTransformer to sink)
        }
        val started = CompletableDeferred<Unit>()
        val frameDeferred = async {
            started.complete(Unit)
            bufferSink.flow.first { it[0u]?.size?.let { it >= 6 } == true }[0u]!!.last().value
        }
        started.await()
        demuxer.start()
        val frame = frameDeferred.await()
        graph.close()

        filter.submit(frame).getOrThrow()
        val converted = filter.receive().getOrThrow()

        withContext(Dispatchers.IO) {
            converted.savePNG(0, Files.createTempFile("frame", ".png"), Mode.RGB0)
        }

        context.close()
        frame.close()
    }
})
