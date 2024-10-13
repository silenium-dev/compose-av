package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi

@OptIn(InternalMultimediaApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    onInitialized: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        var ready by remember { mutableStateOf(false) }
        val loading by player.property<Boolean>("seeking")
        val paused by player.property<Boolean>("pause")
        val focus = remember { FocusRequester() }
        VideoSurface(
            player, showStats,
            onInitialized = {
                onInitialized()
                ready = true
            },
            modifier = Modifier.matchParentSize()
                .handleInputs(player)
                .focusRequester(focus)
                .focusable(enabled = true, interactionSource = MutableInteractionSource()),
        )
        var icon: ImageVector? by remember { mutableStateOf(null) }
        LaunchedEffect(loading, paused) {
            icon = if (loading == true) {
                null
            } else if (paused == true) {
                Icons.Default.Pause
            } else if (icon != null) {
                Icons.Default.PlayArrow
            } else {
                null
            }
        }
        StateIndicatorIcon(icon)
        if (loading != false) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(56.dp),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round,
            )
        }
        VideoSurfaceControls(player, Modifier.align(Alignment.BottomCenter).fillMaxWidth().wrapContentHeight())
        LaunchedEffect(focus) {
            focus.requestFocus()
        }
    }
}
