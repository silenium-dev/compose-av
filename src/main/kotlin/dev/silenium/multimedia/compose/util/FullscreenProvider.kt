package dev.silenium.multimedia.compose.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

class FullscreenProvider {
    var isFullscreen by mutableStateOf(false)
        private set
    val windowState = WindowState()

    @Synchronized
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        windowState.placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }
}

val LocalFullscreenProvider = staticCompositionLocalOf { FullscreenProvider() }
