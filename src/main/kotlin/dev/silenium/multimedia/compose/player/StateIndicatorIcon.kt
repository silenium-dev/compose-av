package dev.silenium.multimedia.compose.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

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
