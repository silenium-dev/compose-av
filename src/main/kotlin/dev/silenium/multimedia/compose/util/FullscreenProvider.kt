package dev.silenium.multimedia.compose.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

interface FullscreenProvider {
    val isFullscreen: Boolean
    val windowState: WindowState
    fun toggleFullscreen()
}

class DesktopFullscreenProvider : FullscreenProvider {
    override var isFullscreen by mutableStateOf(false)
        private set
    override val windowState = WindowState()

    @Synchronized
    override fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        windowState.placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
    }
}

val LocalFullscreenProvider = staticCompositionLocalOf { DesktopFullscreenProvider() }
