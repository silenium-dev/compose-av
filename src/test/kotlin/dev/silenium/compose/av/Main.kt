package dev.silenium.compose.av

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.av.demux.FileDemuxer
import dev.silenium.compose.av.player.VideoPlayer
import java.nio.file.Files
import kotlin.io.path.outputStream

@Composable
fun App() {
    MaterialTheme {
        val file = remember {
            val videoFile = Files.createTempFile("video", ".webm")
            FileDemuxer::class.java.classLoader.getResourceAsStream("video.webm").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        VideoPlayer(file, showStats = true, modifier = Modifier.aspectRatio(16f / 9))
    }
}

suspend fun main(): Unit = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
