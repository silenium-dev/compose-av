package dev.silenium.multimedia.rewrite

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.silenium.compose.gl.surface.GLSurface
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.core.mpv.MPV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.lwjgl.opengl.GL30.*
import java.nio.file.Path

@Composable
fun VideoPlayer(file: Path, suspend: Boolean = false, modifier: Modifier = Modifier) {
    val mpv = remember {
        MPV().apply {
            setOption("terminal", "yes")
            setOption("msg-level", "all=info")
            setOption("vo", "libmpv")
            setOption("hwdec", "auto")
            initialize().getOrThrow()
            setProperty("loop", "inf").getOrThrow()
            setProperty("keep-open", "yes").getOrThrow()
            setProperty("ao-volume", "100").getOrNull()
        }
    }
    var render: MPV.Render? by remember { mutableStateOf(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(ready, file) {
        withContext(Dispatchers.Default) {
            while (!ready) {
                delay(10)
            }
            println("Loading file")
            mpv.commandAsync("loadfile", file.toAbsolutePath().toString()).getOrThrow()
        }
    }
    LaunchedEffect(ready, suspend) {
        withContext(Dispatchers.Default) {
            mpv.commandAsync("set", "pause", if (suspend) "yes" else "no").getOrThrow()
        }
    }
    val state = rememberGLSurfaceState()
    GLSurfaceView(state, modifier = modifier, presentMode = GLSurface.PresentMode.MAILBOX, swapChainSize = 3) {
        if (!ready) {
            render = mpv.createRender(advancedControl = true, state::requestUpdate)
            ready = true
        }
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)
        render?.render(fbo)?.getOrThrow()
    }
    DisposableEffect(Unit) {
        onDispose {
            println("Disposing")
            render?.close()
            mpv.close()
        }
    }
}
