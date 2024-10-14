package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.compose.util.deferredFlowStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeSlider(player: VideoPlayer, coroutineScope: CoroutineScope, modifier: Modifier = Modifier) {
    val muted by deferredFlowStateOf(player::muted)
    val volume by deferredFlowStateOf(player::volume)

    var volumeVisible by remember { mutableStateOf(false) }
    var slideActive by remember { mutableStateOf(false) }
    var volumeSlider: Float? by remember { mutableStateOf(null) }
    Row(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) {
                volumeVisible = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                volumeVisible = false
            }
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    player.toggleMute()
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
}
