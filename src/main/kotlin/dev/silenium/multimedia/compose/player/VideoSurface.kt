package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import org.jetbrains.skia.Paint

//@Composable
//private fun createGLSurface(
//    player: VideoPlayer,
//    surfaceState: GLSurfaceState = rememberGLSurfaceState(),
//    onInitialized: () -> Unit = {},
//): GLSurface {
//    var initialized by remember { mutableStateOf(false) }
//
//    @OptIn(InternalMultimediaApi::class)
//    val dwidth by player.property<Long>("dwidth")
//
//    @OptIn(InternalMultimediaApi::class)
//    val dheight by player.property<Long>("dheight")
//    val fboSizeOverride = remember(player.config.pixelPerfect, dwidth, dheight) {
//        if (!player.config.pixelPerfect) return@remember null
//        dwidth?.let { w ->
//            dheight?.let { h ->
//                FBOSizeOverride(w.toInt(), h.toInt())
//            }
//        }
//    }
//    return rememberGLSurface(
//        surfaceState,
//        presentMode = GLSurface.PresentMode.MAILBOX,
//        swapChainSize = 3,
//        fboSizeOverride = fboSizeOverride,
//        draw = {
//            player.onRender(this, surfaceState)
//            if (!initialized) {
//                initialized = true
//                onInitialized()
//            }
//        }
//    )
//}

@Composable
fun VideoSurface(
    player: VideoPlayer,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
    paint: Paint = Paint(),
    onInitialized: () -> Unit = {},
) {
    val surfaceState = rememberGLCanvasState()
    BoxWithConstraints(modifier = modifier) {
        GLCanvas(
            modifier = Modifier.matchParentSize(),
            state = surfaceState,
//            onDispose = player::onCanvasDispose,
        ) {
            player.onRender(this, surfaceState, onInitialized)
        }
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
    hwdec: Boolean = true,
): VideoPlayer {
    val player = remember { VideoPlayer(hwdec) }
//    val surface = createGLSurface(player, surfaceState, onInitialized)

//    DisposableEffect(player, surface) {
//        player.surface = surface
//        onDispose {
//            player.surface = null
//            player.close()
//        }
//    }
    return player
}
