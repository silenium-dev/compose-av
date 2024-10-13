package dev.silenium.multimedia.compose.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLSurfaceState
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
    suspend fun paused() = mpv.propertyFlow<Boolean>("pause")
    suspend fun volume() = mpv.propertyFlow<Double>("volume")
    suspend fun muted() = mpv.propertyFlow<Boolean>("mute")

    @InternalMultimediaApi
    suspend inline fun <reified T : Any> getProperty(name: String): Result<T?> = mpv.getPropertyAsync<T>(name)

    @InternalMultimediaApi
    suspend inline fun <reified T : Any> setProperty(name: String, value: T) = mpv.setPropertyAsync(name, value)

    @Composable
    @InternalMultimediaApi
    inline fun <reified T : Any> property(name: String): State<T?> = deferredFlowStateOf { mpv.propertyFlow(name) }

    @InternalMultimediaApi
    suspend fun command(vararg command: String) = mpv.commandAsync(command.toList().toTypedArray())

    suspend fun togglePause() = mpv.commandAsync("cycle", "pause")
    suspend fun toggleMute() = mpv.commandAsync("cycle", "mute")
    suspend fun setVolume(volume: Long) = mpv.commandAsync("set", "volume", volume.toString())

    suspend fun seekAbsolute(position: Duration) =
        mpv.commandAsync(arrayOf("seek", position.inWholeMilliseconds.div(1000.0).toString(), "absolute"))
    suspend fun seekRelative(by: Duration) =
        mpv.commandAsync(arrayOf("seek", by.inWholeMilliseconds.div(1000.0).toString(), "relative"))

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
        mpv.setProperty("ao-volume", 100).getOrNull() // ignore errors
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
