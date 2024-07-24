package dev.silenium.compose.av.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.silenium.compose.av.data.AVMediaType
import dev.silenium.compose.av.data.AVPixelFormat.AV_PIX_FMT_P010BE
import dev.silenium.compose.av.data.AVPixelFormat.AV_PIX_FMT_P010LE
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.demux.FileDemuxer
import dev.silenium.compose.av.platform.linux.VAGLRenderInterop
import dev.silenium.compose.av.platform.linux.VaapiDecoder
import dev.silenium.compose.av.render.GLInteropImage
import dev.silenium.compose.av.render.GLRenderInterop
import dev.silenium.compose.av.util.Resources.loadTextFromClasspath
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.lwjgl.opengles.GLES30.*
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

class VideoPlayer(path: Path) : AutoCloseable {
    val demuxer = FileDemuxer(path)
    private val decoder =
        VaapiDecoder(demuxer.streams.first { it.type == AVMediaType.AVMEDIA_TYPE_VIDEO }, "/dev/dri/renderD128")
    private val frames = Channel<Frame>(4, onBufferOverflow = BufferOverflow.SUSPEND)

    init {
        println("Codec: ${decoder.stream.codec.description}")
    }

    private lateinit var interop: GLRenderInterop
    private var glInitialized = false
    private var shaderProgram: Int = 0
    private var vao: Int = 0
    private var vbo: Int = 0
    private var ibo: Int = 0

    private val decodeJob = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            val packet = demuxer.nextPacket().getOrNull() ?: break
            while (decoder.submit(packet).isFailure) delay(10.milliseconds)
            while (isActive) {
                val frame = decoder.receive().getOrNull() ?: break
                frames.send(frame)
            }
        }
    }

    private fun initShader() {
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

    private fun initBuffers() {
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

    private suspend fun initializeGL() {
        if (glInitialized) return
        initShader()
        initBuffers()
        interop = VAGLRenderInterop(decoder)
        glInitialized = true
    }

    private suspend fun renderImage(image: GLInteropImage) {
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        glUseProgram(shaderProgram)
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)

        image.planeSwizzles.map { intArrayOf(it.r.ordinal, it.g.ordinal, it.b.ordinal, it.a.ordinal) }
            .forEachIndexed { idx, it ->
                val swizzleLocation = glGetUniformLocation(shaderProgram, "swizzle_$idx")
                glUniform4iv(swizzleLocation, it)
            }

        val hdrLocation = glGetUniformLocation(shaderProgram, "enableHDR")
        if (image.frame.swFormat!! in setOf(AV_PIX_FMT_P010LE, AV_PIX_FMT_P010BE)) {
            glUniform1i(hdrLocation, 1)
        } else {
            glUniform1i(hdrLocation, 0)
        }

        image.planeTextures.forEachIndexed { idx, it ->
            glActiveTexture(GL_TEXTURE0 + idx)
            glBindTexture(GL_TEXTURE_2D, it)
        }

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
    }

    context(GLDrawScope)
    suspend fun onRender() {
        initializeGL()
        frames.receive().use { frame ->
            interop.map(frame).getOrElse {
                println("Failed to map frame: $it")
                return
            }.use {
                renderImage(it)
            }
        }
    }

    override fun close() {
        runBlocking { decodeJob.cancelAndJoin() }
        demuxer.close()
        decoder.close()
    }
}

@Composable
fun rememberVideoPlayer(path: Path) =
    remember(path) { VideoPlayer(path) }
        .also {
            DisposableEffect(path) {
                onDispose {
                    it.close()
                }
            }
        }

@Composable
fun VideoPlayer(
    path: Path,
    showStats: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val player = rememberVideoPlayer(path)
    val state = rememberGLSurfaceState()

    Box(modifier = modifier) {
        GLSurfaceView(
            state,
            modifier = Modifier.fillMaxSize(),
            presentMode = GLSurfaceView.PresentMode.MAILBOX,
            draw = player::onRender,
        )
        if (showStats) {
            Surface(modifier = Modifier.padding(6.dp).width(360.dp), shape = MaterialTheme.shapes.medium) {
                PlayerStats(player, state)
            }
        }
    }
}
