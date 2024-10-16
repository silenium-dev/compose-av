package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.*
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi

@Composable
private fun createGLSurface(
    player: VideoPlayer,
    surfaceState: GLSurfaceState = rememberGLSurfaceState(),
    onInitialized: () -> Unit = {},
): GLSurface {
    var initialized by remember { mutableStateOf(false) }

    @OptIn(InternalMultimediaApi::class)
    val dwidth by player.property<Long>("dwidth")

    @OptIn(InternalMultimediaApi::class)
    val dheight by player.property<Long>("dheight")
    val fboSizeOverride = remember(dwidth, dheight) {
        dwidth?.let { w ->
            dheight?.let { h ->
                FBOSizeOverride(w.toInt(), h.toInt())
            }
        }
    }
    return rememberGLSurface(
        surfaceState,
        presentMode = GLSurface.PresentMode.MAILBOX,
        swapChainSize = 3,
        fboSizeOverride = fboSizeOverride,
        draw = {
            player.onRender(this, surfaceState)
            if (!initialized) {
                initialized = true
                onInitialized()
            }
        }
    )
}

@Composable
fun VideoSurface(
    player: VideoPlayer,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val surfaceState = rememberGLSurfaceState()
    BoxWithConstraints(modifier = modifier) {
        GLSurfaceView(
            surface = player.surface ?: createGLSurface(player, surfaceState),
            modifier = Modifier.matchParentSize(),
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
fun rememberVideoPlayer(
    surfaceState: GLSurfaceState = rememberGLSurfaceState(),
    onInitialized: () -> Unit = {},
): VideoPlayer {
    val player = remember { VideoPlayer() }
    val surface = createGLSurface(player, surfaceState, onInitialized)

    LaunchedEffect(player, surface) {
        player.surface = surface
    }
    DisposableEffect(player) {
        onDispose {
            player.close()
        }
    }
    return player
}
