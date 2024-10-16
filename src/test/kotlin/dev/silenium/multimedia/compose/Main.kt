package dev.silenium.multimedia.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.multimedia.compose.player.VideoSurfaceWithControls
import dev.silenium.multimedia.compose.player.rememberVideoPlayer
import dev.silenium.multimedia.compose.util.LocalFullscreenProvider
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.*
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
        var ready by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val player = rememberVideoPlayer(
            onInitialized = {
                ready = true
            },
        )
        DisposableEffect(Unit) {
            onDispose {
                ready = false
            }
        }
        LaunchedEffect(file) {
            withContext(Dispatchers.Default) {
                while (!ready && isActive) delay(10.milliseconds)
                player.command("loadfile", file.absolutePathString())
            }
        }
        val fullscreen = LocalFullscreenProvider.current.isFullscreen
        val lazyState = rememberLazyListState()
        BoxWithConstraints(
            modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()
        ) {
            var visible by remember { mutableStateOf(true) }
            var wasPaused by remember { mutableStateOf("no") }
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(2.seconds)
                    visible = !visible
                }
            }
            val modifier = when {
                fullscreen -> Modifier.size(
                    this@BoxWithConstraints.maxWidth,
                    this@BoxWithConstraints.maxHeight
                )

                else -> Modifier.requiredSizeIn(
                    this@BoxWithConstraints.minWidth,
                    this@BoxWithConstraints.minHeight,
                    this@BoxWithConstraints.maxWidth,
                    this@BoxWithConstraints.maxHeight,
                )
            }
            LazyColumn(
                modifier = modifier,
                state = lazyState,
                userScrollEnabled = !fullscreen,
            ) {
                if (visible) {
                    item(key = "video", contentType = "video") {
                        VideoSurfaceWithControls(
                            player = player,
                            modifier = Modifier.fillParentMaxSize().animateItem(),
                            showStats = true,
                            controlFocusRequester = remember { FocusRequester() },
                        )
                        DisposableEffect(Unit) {
                            coroutineScope.launch {
                                println("Setting pause to $wasPaused")
                                player.setProperty("pause", wasPaused)
                            }

                            onDispose {
                                coroutineScope.launch {
                                    wasPaused = player.getProperty<String>("pause").getOrNull() ?: "no"
                                    player.setProperty("pause", "yes")
                                }
                            }
                        }
                    }
                } else {
                    item(key = "video", contentType = "empty") {
                        Box(
                            modifier = Modifier.fillParentMaxSize().animateItem(),
                        ) {
                            Text(
                                text = "Video player is not visible",
                                modifier = Modifier.padding(16.dp).align(Alignment.Center),
                                style = MaterialTheme.typography.h6,
                                color = MaterialTheme.colors.onBackground
                            )
                        }
                    }
                }
                item(key = "text", contentType = "text") {
                    Text(
                        text = "This is a test video player",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
        }
        var previousIndex by remember { mutableStateOf(0) }
        var previousOffset by remember { mutableStateOf(0) }
        LaunchedEffect(fullscreen) {
            if (fullscreen) {
                previousIndex = lazyState.firstVisibleItemIndex
                previousOffset = lazyState.firstVisibleItemScrollOffset
                lazyState.scrollToItem(0)
            } else {
                lazyState.scrollToItem(previousIndex, previousOffset)
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
