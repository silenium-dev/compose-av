package dev.silenium.multimedia.simple

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.file.Files
import kotlin.io.path.outputStream

fun main() = application {
    val file = remember {
        val videoFile = Files.createTempFile("video", ".webm")
        Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }

    Window(onCloseRequest = this::exitApplication) {
        VideoPlayer(file = file, modifier = Modifier.fillMaxSize())
    }
}
