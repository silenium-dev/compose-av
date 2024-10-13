package dev.silenium.multimedia.compose.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import dev.silenium.multimedia.core.util.deferredFlowStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun VideoControls(
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

@Composable
fun BoxScope.StateIndicatorIcon(icon: ImageVector? = null, description: String? = null) {
    if (icon == null) return
    val duration = 640
    val easing = EaseOut
    val alphaAnimator = remember { AnimationState(1f) }
    val scaleAnimator = remember { AnimationState(1f) }
    val alpha by alphaAnimator.asFloatState()
    val scale by scaleAnimator.asFloatState()

    LaunchedEffect(icon) {
        alphaAnimator.animateTo(1f, snap(0))
        scaleAnimator.animateTo(1f, snap(0))
        listOf(
            launch { alphaAnimator.animateTo(0f, tween(duration, easing = easing)) },
            launch { scaleAnimator.animateTo(1.5f, tween(duration, easing = easing)) },
        ).joinAll()
    }

    Surface(
        shape = CircleShape,
        modifier = Modifier
            .align(Alignment.Center)
            .size(48.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape),
        color = Color.Black.copy(alpha = 0.25f),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.fillMaxSize().padding(10.dp),
        )
    }
}

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
        VideoControls(player, Modifier.align(Alignment.BottomCenter).fillMaxWidth().wrapContentHeight())
        LaunchedEffect(focus) {
            focus.requestFocus()
        }
    }
}
