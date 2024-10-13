package dev.silenium.multimedia.compose.player

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, InternalMultimediaApi::class)
@Composable
fun Modifier.handleInputs(player: VideoPlayer): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var setSpeedJob: Job? by remember { mutableStateOf(null) }
    var spedUp by remember { mutableStateOf(false) }
    return this
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
}
