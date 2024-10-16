package dev.silenium.multimedia.compose.player

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import dev.silenium.multimedia.compose.util.LocalFullscreenProvider
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class, InternalMultimediaApi::class)
@Composable
fun Modifier.handleInputs(player: VideoPlayer, focusRequester: FocusRequester? = null): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var setSpeedJob: Job? by remember { mutableStateOf(null) }
    var spedUp by remember { mutableStateOf(false) }
    var lastRelease by remember { mutableStateOf(Instant.DISTANT_PAST) }
    val fullscreenProvider = LocalFullscreenProvider.current
    return this.pointerInput(player) {
        awaitPointerEventScope {
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis.milliseconds
            while (true) {
                val event = awaitPointerEvent()
                if (event.button == PointerButton.Primary) {
                    focusRequester?.requestFocus()
                    if (event.type == PointerEventType.Press) {
                        setSpeedJob = coroutineScope.launch {
                            delay(longPressTimeout)
                            spedUp = true
                            player.setProperty("speed", 2.0)
                        }
                    } else if (event.type == PointerEventType.Release) {
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
                        val now = Clock.System.now()
                        if (lastRelease + longPressTimeout > now) {
                            fullscreenProvider.toggleFullscreen()
                        }
                        lastRelease = Clock.System.now()
                    }
                }
            }
        }
    }.onPreviewKeyEvent {
        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (it.key) {
            Key.Spacebar -> {
                coroutineScope.launch { player.togglePause() }
                true
            }

            else -> false
        }
    }
}
