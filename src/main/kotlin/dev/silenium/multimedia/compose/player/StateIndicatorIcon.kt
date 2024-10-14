package dev.silenium.multimedia.compose.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import dev.silenium.multimedia.core.util.deferredFlowStateOf
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@OptIn(InternalMultimediaApi::class)
@Composable
fun StateIndicatorIcon(player: VideoPlayer, modifier: Modifier = Modifier) {
    val paused by deferredFlowStateOf(player::paused)
    val loading by player.property<Boolean>("seeking")
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

    icon?.let {
        val duration = 640
        val easing = EaseOut
        val alphaAnimator = remember { AnimationState(1f) }
        val scaleAnimator = remember { AnimationState(1f) }
        val alpha by alphaAnimator.asFloatState()
        val scale by scaleAnimator.asFloatState()

        LaunchedEffect(icon) {
            alphaAnimator.animateTo(1f, snap(0), sequentialAnimation = true)
            scaleAnimator.animateTo(1f, snap(0), sequentialAnimation = true)
            listOf(
                launch { alphaAnimator.animateTo(0f, tween(duration, easing = easing)) },
                launch { scaleAnimator.animateTo(1.5f, tween(duration, easing = easing)) },
            ).joinAll()
        }

        Surface(
            shape = CircleShape,
            modifier = modifier
                .size(48.dp)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape),
            color = Color.Black.copy(alpha = 0.25f),
        ) {
            Icon(
                it,
                contentDescription = if (paused == true) "Paused" else "Resumed",
                tint = Color.White,
                modifier = Modifier.fillMaxSize().padding(10.dp),
            )
        }
    }
}
