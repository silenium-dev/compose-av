package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import dev.silenium.multimedia.core.util.deferredFlowStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(InternalMultimediaApi::class, ExperimentalComposeUiApi::class)
@Composable
fun VideoSurfaceWithControls(
    player: VideoPlayer,
    modifier: Modifier = Modifier,
    showStats: Boolean = false,
    onInitialized: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier) {
        var ready by remember { mutableStateOf(false) }
        val loading by player.property<Boolean>("seeking")
        val paused by player.property<Boolean>("pause")
        val duration by deferredFlowStateOf(player::duration)
        val position by deferredFlowStateOf(player::position)
        var positionSlider: Float? by remember { mutableStateOf(null) }
        val focus = remember { FocusRequester() }
        var setSpeedJob: Job? by remember { mutableStateOf(null) }
        var spedUp by remember { mutableStateOf(false) }
        VideoSurface(
            player, showStats,
            onInitialized = {
                onInitialized()
                ready = true
            },
            modifier = Modifier.matchParentSize()
//                .clickable(enabled = true, interactionSource = MutableInteractionSource(), indication = null) {
//                    coroutineScope.launch {
//                        player.togglePause()
//                    }
//                }
                .onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Primary) {
                        setSpeedJob = coroutineScope.launch {
                            delay(500)
                            spedUp = true
                            player.setProperty("speed", 2.0)
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Release) {
                    if (it.button == PointerButton.Primary) {
                        setSpeedJob?.cancel()
                        if (spedUp) {
                            spedUp = false
                            setSpeedJob = null
                            coroutineScope.launch {
                                player.setProperty("speed", 1.0)
                            }
                        } else {
                            coroutineScope.launch {
                                player.togglePause()
                            }
                        }
                    }
                }
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.Spacebar -> {
                            println("${it.type}: ${it.key}")
                            coroutineScope.launch { player.togglePause() }
                            true
                        }

                        else -> false
                    }
                }
                .focusRequester(focus)
                .focusable(enabled = true, interactionSource = MutableInteractionSource()),
        )
        if (paused == true && loading == false) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = Color.Black.copy(alpha = 0.25f),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            }
        }
        if (loading != false) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(56.dp),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round,
            )
        }
        Slider(
            positionSlider ?: position?.inWholeMilliseconds?.div(1000.0f) ?: 0f,
            valueRange = 0f..(duration?.inWholeMilliseconds?.div(1000.0f) ?: 0f),
            modifier = Modifier.align(Alignment.BottomCenter),
            onValueChange = { positionSlider = it },
            onValueChangeFinished = {
                positionSlider?.let {
                    coroutineScope.launch {
                        player.seekAbsolute(it.toDouble().seconds)
                    }
                }
                positionSlider = null
            }
        )
    }
}
