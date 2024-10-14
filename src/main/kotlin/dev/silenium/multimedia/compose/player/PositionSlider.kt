package dev.silenium.multimedia.compose.player

import androidx.compose.material.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.silenium.multimedia.compose.util.deferredFlowStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun PositionSlider(player: VideoPlayer, coroutineScope: CoroutineScope, modifier: Modifier = Modifier) {
    var positionSlider: Float? by remember { mutableStateOf(null) }
    val duration by deferredFlowStateOf(player::duration)
    val position by deferredFlowStateOf(player::position)

    Slider(
        positionSlider ?: position?.inWholeMilliseconds?.div(1000.0f) ?: 0f,
        valueRange = 0f..(duration?.inWholeMilliseconds?.div(1000.0f) ?: 0f),
        modifier = modifier,
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
}
