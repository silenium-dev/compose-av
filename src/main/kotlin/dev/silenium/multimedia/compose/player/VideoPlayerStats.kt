package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.GLSurfaceState
import dev.silenium.compose.gl.surface.Stats
import dev.silenium.multimedia.compose.format.format
import dev.silenium.multimedia.core.util.deferredFlowStateOf

@Composable
fun VideoPlayerStats(player: VideoPlayer, state: GLSurfaceState, textColor: Color = Color.White) {
    Column(modifier = Modifier.padding(6.dp)) {
        val position by deferredFlowStateOf(player::position)
        val duration by deferredFlowStateOf(player::duration)
        Text("Positon: ${position?.format() ?: "N/A"}", color = textColor)
        Text("Duration: ${duration?.format() ?: "N/A"}", color = textColor)

        val display by state.displayStatistics.collectAsState()
        Text("Display datapoints: ${display.frameTimes.values.size}", color = textColor)
        Text("Display frame time: ${display.frameTimes.median.inWholeMicroseconds / 1000.0} ms", color = textColor)
        Text(
            "Display frame time (99th): ${display.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms",
            color = textColor
        )
        Text("Display FPS: ${display.fps.median}", color = textColor)
        Text("Display FPS (99th): ${display.fps.percentile(0.99, Stats.Percentile.LOWEST)}", color = textColor)

        val render by state.renderStatistics.collectAsState()
        Text("Render datapoints: ${render.frameTimes.values.size}", color = textColor)
        Text("Render frame time: ${render.frameTimes.median.inWholeMicroseconds / 1000.0} ms", color = textColor)
        Text(
            "Render frame time (99th): ${render.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms",
            color = textColor
        )
        Text("Render FPS: ${render.fps.median}", color = textColor)
        Text("Render FPS (99th): ${render.fps.percentile(0.99, Stats.Percentile.LOWEST)}", color = textColor)
    }
}
