package dev.silenium.multimedia.core.flow

import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.seconds

class FlowsKtTest : StringSpec({
    val file = run {
        val videoFile = Files.createTempFile("video", ".webm")
        FileDemuxer::class.java.classLoader.getResourceAsStream("1080p.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }

    "as flow" {
        val demuxer = FileDemuxer(file)
        println(demuxer.streams)
        val decoder = VaapiDecoder(
            demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO },
            VaapiDeviceContext.DRM("/dev/dri/renderD128"),
        )
        val job = CoroutineScope(Dispatchers.Default).launch {
            demuxer.process(0, decoder).collect {
                println(it)
            }
        }
        job.invokeOnCompletion {
            println("Job completed")
        }

        delay(1.seconds)
        demuxer.close()
        job.join()
    }
})
