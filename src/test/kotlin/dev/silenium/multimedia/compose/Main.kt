package dev.silenium.multimedia.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.multimedia.compose.player.VideoSurfaceWithControls
import dev.silenium.multimedia.compose.player.rememberVideoPlayer
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.milliseconds

@OptIn(InternalMultimediaApi::class)
@Composable
fun App() {
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        val file = remember {
            val videoFile = Files.createTempFile("video", ".webm")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        val player = rememberVideoPlayer()
        var ready by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
        ) {
            VideoSurfaceWithControls(
                player, showStats = true, modifier = Modifier.fillMaxSize(),
                onInitialized = {
                    ready = true
                }
            )
        }
        LaunchedEffect(file) {
            withContext(Dispatchers.Default) {
                while (!ready && isActive) delay(10.milliseconds)
                player.command("loadfile", file.absolutePathString())
            }
        }
    }
}

suspend fun main(): Unit = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
