package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    onInitialized: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        VideoSurface(
            player, showStats,
            onInitialized = onInitialized,
            modifier = Modifier.matchParentSize(),
        )
        VideoSurfaceControls(player, Modifier.matchParentSize())
    }
}
