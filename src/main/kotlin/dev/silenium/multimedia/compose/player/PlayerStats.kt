package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.GLSurfaceState
import dev.silenium.compose.gl.surface.Stats

@Composable
fun PlayerStats(player: VideoPlayer, state: GLSurfaceState) {
    Column(modifier = Modifier.padding(6.dp)) {
//        Text("Duration: ${player.demuxer.duration(player.demuxer.streams.values.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO })}")

        val display by state.displayStatistics.collectAsState()
        Text("Display datapoints: ${display.frameTimes.values.size}")
        Text("Display frame time: ${display.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
        Text("Display frame time (99th): ${display.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
        Text("Display FPS: ${display.fps.median}")
        Text("Display FPS (99th): ${display.fps.percentile(0.99, Stats.Percentile.LOWEST)}")

        val render by state.renderStatistics.collectAsState()
        Text("Render datapoints: ${render.frameTimes.values.size}")
        Text("Render frame time: ${render.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
        Text("Render frame time (99th): ${render.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
        Text("Render FPS: ${render.fps.median}")
        Text("Render FPS (99th): ${render.fps.percentile(0.99, Stats.Percentile.LOWEST)}")
    }
}
