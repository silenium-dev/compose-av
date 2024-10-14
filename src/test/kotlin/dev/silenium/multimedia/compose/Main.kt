package dev.silenium.multimedia.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.multimedia.compose.player.VideoSurfaceWithControls
import dev.silenium.multimedia.compose.player.rememberVideoPlayer
import dev.silenium.multimedia.compose.util.LocalFullscreenProvider
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
        ) {
            val fullscreen = LocalFullscreenProvider.current.isFullscreen
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scroll, !LocalFullscreenProvider.current.isFullscreen).fillMaxSize()
            ) {
                VideoSurfaceWithControls(
                    player = player,
                    modifier = Modifier.let {
                        when {
                            fullscreen -> it.size(this@BoxWithConstraints.maxWidth, this@BoxWithConstraints.maxHeight)
                            else -> it.requiredSizeIn(
                                this@BoxWithConstraints.minWidth,
                                this@BoxWithConstraints.minHeight,
                                this@BoxWithConstraints.maxWidth,
                                this@BoxWithConstraints.maxHeight,
                            )
                        }
                    },
                    showStats = true,
                    onInitialized = {
                        ready = true
                    }
                )
                Text(
                    text = "This is a test video player",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground
                )
            }
            var previousPosition by remember { mutableStateOf(0) }
            LaunchedEffect(fullscreen) {
                if (fullscreen) {
                    previousPosition = scroll.value
                    scroll.scrollTo(0)
                } else {
                    scroll.scrollTo(previousPosition)
                }
            }
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
    val state = LocalFullscreenProvider.current.windowState
    Window(state = state, onCloseRequest = ::exitApplication) {
        App()
    }
}
