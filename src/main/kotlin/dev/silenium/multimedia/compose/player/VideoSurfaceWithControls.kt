package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    controlFocusRequester: FocusRequester? = null,
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
        )
        VideoSurfaceControls(player, Modifier.matchParentSize(), controlFocusRequester)
    }
}
