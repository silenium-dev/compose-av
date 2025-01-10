package dev.silenium.multimedia.compose.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.compose.util.LocalFullscreenProvider
import dev.silenium.multimedia.compose.util.deferredFlowStateOf
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalMultimediaApi::class)
@Preview
@Composable
fun VideoSurfaceControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val paused by deferredFlowStateOf(player::paused)
    val loading by player.property<Boolean>("seeking")
    val backgroundColor = MaterialTheme.colors.surface
    val focus = remember(focusRequester) { focusRequester ?: FocusRequester() }
    var menuOpen by remember { mutableStateOf(false) }
    val fullscreenProvider = LocalFullscreenProvider.current

    Box(modifier = modifier) {
        StateIndicatorIcon(player, Modifier.align(Alignment.Center))
        if (loading != false) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(56.dp),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round,
            )
        }
        Box(
            modifier = Modifier.matchParentSize()
                .handleInputs(player, focus.takeIf { focusOnClick })
                .focusRequester(focus)
                .focusable(enabled = true, interactionSource = remember { MutableInteractionSource() })
        )

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(enabled = false) {} // consume clicks to prevent them from reaching the video surface
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            0f to backgroundColor.copy(alpha = 0.0f),
                            0.2f to backgroundColor.copy(alpha = 0.15f),
                            1f to backgroundColor.copy(alpha = 0.6f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                        )
                    )
                }
        ) {
            PositionSlider(
                player, coroutineScope,
                Modifier.fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 0.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-24).dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight().align(Alignment.Center)
                    .padding(vertical = 0.dp, horizontal = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            player.seekRelative((-10).seconds)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            player.togglePause()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    AnimatedContent(paused, transitionSpec = {
                        ContentTransform(fadeIn(), fadeOut())
                    }) { it ->
                        if (it != false) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colors.onSurface
                            )
                        } else {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            player.seekRelative(10.seconds)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
                VolumeSlider(player, coroutineScope)
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    IconButton(
                        onClick = { menuOpen = !menuOpen },
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colors.onSurface,
                        )
                    }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(onClick = { player.config.pixelPerfect = !player.config.pixelPerfect }) {
                            Checkbox(
                                player.config.pixelPerfect,
                                onCheckedChange = {
                                    player.config.pixelPerfect = it
                                },
                            )
                            Text("Pixel-Perfect Rendering")
                        }
                    }
                }
                IconButton(
                    onClick = {
                        fullscreenProvider.toggleFullscreen()
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    AnimatedContent(fullscreenProvider.isFullscreen, transitionSpec = {
                        ContentTransform(fadeIn(), fadeOut())
                    }) { it ->
                        if (it) {
                            Icon(
                                Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = MaterialTheme.colors.onSurface
                            )
                        } else {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Enter Fullscreen",
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(focus) {
        if (focusRequester == null) { // only focus if we created the focus requester
            focus.requestFocus()
        }
    }
}
