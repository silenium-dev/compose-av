package dev.silenium.multimedia.compose.player

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.*
import dev.silenium.multimedia.core.annotation.InternalMultimediaApi
import dev.silenium.multimedia.core.util.deferredFlowStateOf
import dev.silenium.multimedia.core.util.mapState
import dev.silenium.multimedia.mpv.MPV
import org.lwjgl.opengl.GL30.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VideoPlayer(hwdec: Boolean = false) : AutoCloseable {
    private var initialized = false

    @PublishedApi
    internal val mpv = createMPV(hwdec)
    private var render: MPV.Render? = null
    suspend fun duration() = mpv.propertyFlow<Double>("duration").mapState { it?.seconds }
    suspend fun position() = mpv.propertyFlow<Double>("time-pos").mapState { it?.seconds }

    @InternalMultimediaApi
    suspend inline fun <reified T : Any> getProperty(name: String): Result<T?> = mpv.getPropertyAsync<T>(name)

    @InternalMultimediaApi
    suspend fun <T : Any> setProperty(name: String, value: T) = mpv.setPropertyAsync(name, value)

    @Composable
    @InternalMultimediaApi
    inline fun <reified T : Any> property(name: String): State<T?> = deferredFlowStateOf { mpv.propertyFlow(name) }

    @InternalMultimediaApi
    suspend fun command(vararg command: String) = mpv.commandAsync(command.toList().toTypedArray())

    suspend fun togglePause() = mpv.commandAsync("cycle", "pause")

    suspend fun seekAbsolute(position: Duration) =
        mpv.commandAsync(arrayOf("seek", position.inWholeMilliseconds.div(1000.0).toString(), "absolute"))

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

    private fun initialize(state: GLSurfaceState) {
        if (initialized) return
        render = mpv.createRender(advancedControl = true, state::requestUpdate)
        initialized = true
    }

    fun onRender(scope: GLDrawScope, state: GLSurfaceState) {
        initialize(state)

        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)
        render?.render(scope.fbo)?.getOrThrow()
        scope.redrawAfter(null)
    }

    override fun close() {
        mpv.command("stop")
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
fun rememberVideoPlayer(): VideoPlayer {
    val player = remember { VideoPlayer() }
    DisposableEffect(player) {
        onDispose {
            player.close()
        }
    }
    return player
}

@Composable
fun VideoSurface(
    player: VideoPlayer,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
    onInitialized: () -> Unit = {},
) {
    val surfaceState = rememberGLSurfaceState()

    @OptIn(InternalMultimediaApi::class)
    val dwidth by player.property<Long>("width")

    @OptIn(InternalMultimediaApi::class)
    val dheight by player.property<Long>("height")
    val fboSizeOverride = remember(dwidth, dheight) {
        dwidth?.let { w ->
            dheight?.let { h ->
                FBOSizeOverride(w.toInt(), h.toInt())
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        var initialized by remember { mutableStateOf(false) }
        GLSurfaceView(
            surfaceState,
            modifier = Modifier.fillMaxSize(),
            presentMode = GLSurfaceView.PresentMode.MAILBOX,
            swapChainSize = 3,
            draw = {
                player.onRender(this, surfaceState)
                if (!initialized) {
                    initialized = true
                    onInitialized()
                }
            },
            fboSizeOverride = fboSizeOverride,
        )
        if (showStats) {
            Surface(
                modifier = Modifier.padding(6.dp).width(360.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                VideoPlayerStats(player, surfaceState)
            }
        }
    }
}
