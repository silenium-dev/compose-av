package dev.silenium.multimedia.compose.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.core.util.deferredFlowStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun VideoSurfaceControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var positionSlider: Float? by remember { mutableStateOf(null) }
    val duration by deferredFlowStateOf(player::duration)
    val position by deferredFlowStateOf(player::position)
    val paused by deferredFlowStateOf(player::paused)
    val muted by deferredFlowStateOf(player::muted)
    val volume by deferredFlowStateOf(player::volume)
    val backgroundColor = MaterialTheme.colors.surface
    Box(modifier = modifier.drawBehind {
        drawRect(
            Brush.linearGradient(
                0f to backgroundColor.copy(alpha = 0.0f),
                0.2f to backgroundColor.copy(alpha = 0.15f),
                1f to backgroundColor.copy(alpha = 0.6f),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
            )
        )
    }) {
        Slider(
            positionSlider ?: position?.inWholeMilliseconds?.div(1000.0f) ?: 0f,
            valueRange = 0f..(duration?.inWholeMilliseconds?.div(1000.0f) ?: 0f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp).align(Alignment.TopCenter)
                .offset(y = (-24).dp),
            onValueChange = { positionSlider = it },
            onValueChangeFinished = {
                positionSlider?.let {
                    coroutineScope.launch {
                        player.seekAbsolute(it.toDouble().seconds)
                    }
                }
                positionSlider = null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().align(Alignment.Center)
                .padding(vertical = 0.dp, horizontal = 0.dp)
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        player.seekRelative((-10).seconds)
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s", tint = MaterialTheme.colors.onSurface)
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
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colors.onSurface)
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
            var volumeVisible by remember { mutableStateOf(false) }
            var slideActive by remember { mutableStateOf(false) }
            var volumeSlider: Float? by remember { mutableStateOf(null) }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .onPointerEvent(PointerEventType.Enter) {
                        volumeVisible = true
                    }.onPointerEvent(PointerEventType.Exit) {
                        volumeVisible = false
                    }
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            player.toggleMute().onFailure {
                                println("Failed to toggle mute: $it")
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        if (muted != false) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (muted != false) "Unmute" else "Mute",
                        tint = MaterialTheme.colors.onSurface,
                    )
                }
                var setJob: Job? by remember { mutableStateOf(null) }
                if (volumeVisible || slideActive) {
                    val sliderVolume = if (muted == true) 0f else volumeSlider ?: volume?.toFloat()
                    if (sliderVolume != null) {
                        Slider(
                            sliderVolume,
                            valueRange = 0f..100f,
                            modifier = Modifier.width(100.dp).padding(horizontal = 4.dp, vertical = 0.dp),
                            onValueChange = {
                                volumeSlider = it
                                slideActive = true
                                setJob?.cancel()
                                setJob = coroutineScope.launch {
                                    if (muted == true) {
                                        player.toggleMute()
                                    }
                                    player.setVolume(it.roundToLong())
                                }
                            },
                            onValueChangeFinished = {
                                volumeSlider?.let {
                                    volumeSlider = null
                                    slideActive = false
                                    setJob?.cancel()
                                    coroutineScope.launch {
                                        if (muted == true) {
                                            player.toggleMute()
                                        }
                                        player.setVolume(it.roundToLong())
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
