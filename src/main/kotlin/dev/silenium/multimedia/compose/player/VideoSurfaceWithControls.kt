package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import org.jetbrains.skia.Paint

@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    controlFocusRequester: FocusRequester? = null,
    paint: Paint = Paint(),
) {
    BoxWithConstraints(modifier) {
        VideoSurface(
            player, showStats,
            modifier = Modifier.align(Alignment.Center).requiredSizeIn(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            ),
            paint = paint,
        )
        VideoSurfaceControls(player, Modifier.matchParentSize(), controlFocusRequester)
    }
}
