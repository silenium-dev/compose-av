package dev.silenium.multimedia.simple

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import java.nio.file.Files
import kotlin.io.path.outputStream

suspend fun main() = awaitApplication {
    val file = remember {
        val videoFile = Files.createTempFile("video", ".webm")
        Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.apply { toFile().deleteOnExit() }
    }

    Window(onCloseRequest = this::exitApplication) {
        val state = rememberPagerState { 1000 }
        VerticalPager(state = state, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 2) {
            VideoPlayer(file = file, suspend = state.currentPage != it, modifier = Modifier.fillMaxSize())
        }
    }
}
