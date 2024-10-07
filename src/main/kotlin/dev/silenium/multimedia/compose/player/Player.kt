package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLSurfaceState
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.core.util.mapState
import dev.silenium.multimedia.mpv.MPV
import org.lwjgl.opengl.GL30.*
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.seconds

class VideoPlayer(private val path: Path) : AutoCloseable {
    private var glInitialized = false
    private val mpv = createMPV()
    private var render: MPV.Render? = null
    suspend fun duration() = mpv.propertyFlow<Double>("duration").mapState { it?.seconds }
    suspend fun position() = mpv.propertyFlow<Double>("time-pos").mapState { it?.seconds }

    private fun createMPV(hwdec: Boolean = true): MPV {
        val mpv = MPV()
        val options = defaultOptions.toMutableMap()
        options["hwdec"] = if (hwdec) "auto" else "no"
        options.forEach {
            mpv.setOption(it.key, it.value)
        }
        mpv.initialize().getOrThrow()
        mpv.setProperty("loop", false).getOrThrow()
        mpv.setProperty("keep-open", "yes").getOrThrow()
        return mpv
    }

    context(GLDrawScope)
    fun initializeGL(state: GLSurfaceState) {
        if (glInitialized) return
        render = mpv.createRender(state::requestUpdate)
        mpv.command(listOf("loadfile", path.absolutePathString())).getOrThrow()
        glInitialized = true
    }

    context(GLDrawScope)
    fun onRender() {
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)
        render?.render(fbo)?.getOrThrow()
    }

    fun stop() {
        mpv.command("stop").getOrThrow()
    }

    override fun close() {
        stop()
        render?.close()
        mpv.close()
    }

    companion object {
        private val defaultOptions = mapOf(
            "terminal" to "yes",
            "msg-level" to "all=info",
            "vo" to "libmpv",
            "hwdec" to "auto",
        )
        private val initProperties = mapOf(
            "loop" to "inf",
            "keep-open" to "yes",
        )
    }
}

@Composable
fun rememberVideoPlayer(path: Path) = remember(path) { VideoPlayer(path) }

@Composable
fun VideoPlayer(
    path: Path,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val player = rememberVideoPlayer(path)
    val state = rememberGLSurfaceState()
    var initialized by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GLSurfaceView(
            state,
            modifier = Modifier.fillMaxSize(),
            presentMode = GLSurfaceView.PresentMode.MAILBOX,
            draw = {
                if (!initialized) {
                    player.initializeGL(state)
                    initialized = true
                }
                player.onRender()
            },
            cleanup = player::close,
        )
        if (showStats) {
            Surface(modifier = Modifier.padding(6.dp).width(360.dp), shape = MaterialTheme.shapes.medium) {
                PlayerStats(player, state)
            }
        }
    }
}
