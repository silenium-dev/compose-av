package dev.silenium.multimedia.core.vaapi

import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.flow.process
import dev.silenium.multimedia.core.flow.processConstructed
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import dev.silenium.multimedia.core.platform.linux.VaapiYuvToRgbConversion
import dev.silenium.multimedia.core.util.Mode
import dev.silenium.multimedia.core.util.savePNG
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.outputStream

class VATest : FunSpec({
    val file = run {
        val videoFile = Files.createTempFile("video", ".webm")
        FileDemuxer::class.java.classLoader.getResourceAsStream("1080p.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }
    test("something") {
        val context = VaapiDeviceContext.DRM("/dev/dri/renderD128")
        println(context.display)
        val demuxer = FileDemuxer(file)
        println(demuxer.streams)
        val decoder = VaapiDecoder(demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }, context)
        demuxer.process(0, decoder).processConstructed { VaapiYuvToRgbConversion(context, it) }.drop(100)
            .take(1).collect {
                withContext(Dispatchers.IO) {
                    it.savePNG(0, Files.createTempFile("frame", ".png"), Mode.RGB0)
                }
            }

        demuxer.close()
        println("Demuxer closed")
        context.close()
        println("Context closed")
    }
})
