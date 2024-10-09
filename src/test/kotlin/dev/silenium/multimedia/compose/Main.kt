package dev.silenium.multimedia.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.multimedia.compose.player.VideoSurface
import dev.silenium.multimedia.compose.player.rememberVideoPlayer
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalMultimediaApi::class)
@Composable
fun App() {
    MaterialTheme {
        val file1 = remember {
            val videoFile = Files.createTempFile("video", ".webm")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        val file2 = remember {
            val videoFile = Files.createTempFile("video", ".mp4")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.mp4").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        val player = rememberVideoPlayer()
        var file by remember { mutableStateOf(file1) }
        LaunchedEffect(Unit) {
            delay(5.seconds)
            while (isActive) {
                file = if (file == file1) file2 else file1
                delay(5.seconds)
            }
        }
        LaunchedEffect(file) {
            withContext(Dispatchers.Default) {
                player.command(arrayOf("loadfile", file.absolutePathString()))
            }
        }
        VideoSurface(player, showStats = true, modifier = Modifier.fillMaxSize())
    }
}

suspend fun main(): Unit = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
