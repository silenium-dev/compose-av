package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi

@OptIn(InternalMultimediaApi::class)
@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    onInitialized: () -> Unit = {},
) {
    BoxWithConstraints(modifier) {
        BoxWithConstraints(
            modifier = Modifier.requiredSizeIn(
                this@BoxWithConstraints.minWidth,
                this@BoxWithConstraints.minHeight,
                this@BoxWithConstraints.maxWidth,
                this@BoxWithConstraints.maxHeight,
            )
        ) {
            VideoSurface(
                player, showStats,
                onInitialized = onInitialized,
                modifier = Modifier.align(Alignment.Center).requiredSizeIn(
                    minWidth = minWidth,
                    minHeight = minHeight,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                ),
            )
            VideoSurfaceControls(player, Modifier.matchParentSize())
        }
    }
}
