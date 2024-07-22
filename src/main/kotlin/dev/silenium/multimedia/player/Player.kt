package dev.silenium.multimedia.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.Stats
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.decode.VaapiDecoder
import dev.silenium.multimedia.demux.FileDemuxer
import dev.silenium.multimedia.demux.Stream
import dev.silenium.multimedia.util.Resources.loadTextFromClasspath
import dev.silenium.multimedia.vaapi.Surface
import dev.silenium.multimedia.vaapi.VA
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.lwjgl.opengles.GLES30.*
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VideoPlayer(
    path: Path,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val frameChannel = remember { Channel<Frame>(4, onBufferOverflow = BufferOverflow.SUSPEND) }
    val demuxer = remember { FileDemuxer(path) }
    val decoder =
        remember { VaapiDecoder(demuxer.streams.first { it.type == Stream.Type.VIDEO }, "/dev/dri/renderD128") }
    DisposableEffect(path) {
        val decodeJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val packet = demuxer.nextPacket().getOrNull() ?: break
                while (decoder.submit(packet).isFailure) delay(10.milliseconds)
                while (isActive) {
                    val frame = decoder.receive().getOrNull() ?: break
                    frameChannel.send(frame)
                }
            }
        }
        onDispose {
            decodeJob.cancel()
            decoder.close()
            demuxer.close()
        }
    }

    var shaderProgram: Int by remember { mutableStateOf(0) }
    var vao: Int by remember { mutableStateOf(0) }
    var vbo: Int by remember { mutableStateOf(0) }
    var ibo: Int by remember { mutableStateOf(0) }
    val state = rememberGLSurfaceState()

    fun initShader() {
        shaderProgram = glCreateProgram()
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(vertexShader, loadTextFromClasspath("shaders/video.vert"))
        glCompileShader(vertexShader)
        glGetShaderInfoLog(vertexShader).let(::println)
        glAttachShader(shaderProgram, vertexShader)
        glShaderSource(fragmentShader, loadTextFromClasspath("shaders/video.frag"))
        glCompileShader(fragmentShader)
        glGetShaderInfoLog(fragmentShader).let(::println)
        glAttachShader(shaderProgram, fragmentShader)
        glLinkProgram(shaderProgram)
        glGetProgramInfoLog(shaderProgram).let(::println)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        // Check status
        val status = IntArray(1)
        glGetProgramiv(shaderProgram, GL_LINK_STATUS, status)
        if (status[0] == GL_FALSE) {
            val log = glGetProgramInfoLog(shaderProgram)
            error("Failed to link program: $log")
        }

        // TODO: Get parameters from decoder
        glUseProgram(shaderProgram)
        val formatLocation = glGetUniformLocation(shaderProgram, "tex_format")
        glUniform1i(formatLocation, 2)
        val limitedLocation = glGetUniformLocation(shaderProgram, "limitedRange")
        glUniform1i(limitedLocation, 1)
        val texYLocation = glGetUniformLocation(shaderProgram, "tex_y")
        glUniform1i(texYLocation, 0)
        val texULocation = glGetUniformLocation(shaderProgram, "tex_u")
        glUniform1i(texULocation, 1)
        val texVLocation = glGetUniformLocation(shaderProgram, "tex_v")
        glUniform1i(texVLocation, 2)
    }

    fun initBuffers() {
        val vertices = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            1f, 1f, 1f, 1f,
            -1f, 1f, 0f, 1f
        )
        val indices = intArrayOf(0, 1, 3, 1, 2, 3)

        vao = glGenVertexArrays()
        glBindVertexArray(vao)

        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        ibo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

        val positionLocation = glGetAttribLocation(shaderProgram, "position")
        glVertexAttribPointer(positionLocation, 2, GL_FLOAT, true, 4 * 4, 0)
        glEnableVertexAttribArray(positionLocation)
        val uvLocation = glGetAttribLocation(shaderProgram, "uv")
        glVertexAttribPointer(uvLocation, 2, GL_FLOAT, true, 4 * 4, 2 * 4)
        glEnableVertexAttribArray(uvLocation)

        var err = glGetError()
        while (err != GL_NO_ERROR) {
            println("GL error: $err")
            err = glGetError()
        }
    }

    fun initGL() {
        initShader()
        initBuffers()
    }

    fun GLDrawScope.renderGL(surface: Surface) {
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        glUseProgram(shaderProgram)
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)

        surface.planeSwizzles.map { intArrayOf(it.r.ordinal, it.g.ordinal, it.b.ordinal, it.a.ordinal) }
            .forEachIndexed { idx, it ->
                val swizzleLocation = glGetUniformLocation(shaderProgram, "swizzle_$idx")
                glUniform4iv(swizzleLocation, it)
            }

        val hdrLocation = glGetUniformLocation(shaderProgram, "enableHDR")
        if (surface.frame.swFormat?.name in setOf("p010le", "p010be")) {
            glUniform1i(hdrLocation, 1)
        } else {
            glUniform1i(hdrLocation, 0)
        }

        surface.planeTextures.forEachIndexed { idx, it ->
            glActiveTexture(GL_TEXTURE0 + idx)
            glBindTexture(GL_TEXTURE_2D, it)
        }

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
    }

    var glInitialized by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GLSurfaceView(state, modifier = Modifier.fillMaxSize(), presentMode = GLSurfaceView.PresentMode.MAILBOX) {
            val frame = frameChannel.receive()
            if (!glInitialized) {
                initGL()
                glInitialized = true
            }
            val surface = VA.createTextureFromSurface(frame, decoder).getOrThrow()
            renderGL(surface)
            redrawAfter(frame.duration)
            surface.close()
            frame.close()
        }
        if (showStats) {
            Surface(modifier = Modifier.padding(6.dp).width(360.dp), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(6.dp)) {
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
        }
    }
}
