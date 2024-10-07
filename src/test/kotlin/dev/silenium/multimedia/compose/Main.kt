package dev.silenium.multimedia.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.multimedia.compose.player.VideoPlayer
import java.nio.file.Files
import kotlin.io.path.outputStream

@Composable
fun App() {
    MaterialTheme {
        val file = remember {
            val videoFile = Files.createTempFile("video", ".webm")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        VideoPlayer(file, showStats = true, modifier = Modifier.fillMaxSize())
    }
}

suspend fun main(): Unit = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
