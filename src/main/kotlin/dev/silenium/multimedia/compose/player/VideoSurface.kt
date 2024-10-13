package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.FBOSizeOverride
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi

@Composable
fun VideoSurface(
    player: VideoPlayer,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
    onInitialized: () -> Unit = {},
) {
    val surfaceState = rememberGLSurfaceState()

    @OptIn(InternalMultimediaApi::class)
    val dwidth by player.property<Long>("width")

    @OptIn(InternalMultimediaApi::class)
    val dheight by player.property<Long>("height")
    val fboSizeOverride = remember(dwidth, dheight) {
        dwidth?.let { w ->
            dheight?.let { h ->
                FBOSizeOverride(w.toInt(), h.toInt())
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        var initialized by remember { mutableStateOf(false) }
        GLSurfaceView(
            surfaceState,
            modifier = Modifier.fillMaxSize(),
            presentMode = GLSurfaceView.PresentMode.MAILBOX,
            swapChainSize = 3,
            draw = {
                player.onRender(this, surfaceState)
                if (!initialized) {
                    initialized = true
                    onInitialized()
                }
            },
            fboSizeOverride = fboSizeOverride,
        )
        if (showStats) {
            Surface(
                modifier = Modifier.padding(6.dp).width(360.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                VideoSurfaceStats(player, surfaceState)
            }
        }
    }
}

@Composable
fun rememberVideoPlayer(): VideoPlayer {
    val player = remember { VideoPlayer() }
    DisposableEffect(player) {
        onDispose {
            player.close()
        }
    }
    return player
}
