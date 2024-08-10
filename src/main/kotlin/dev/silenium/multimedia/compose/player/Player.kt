package dev.silenium.multimedia.compose.player

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
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.libs.flows.api.FlowGraph
import dev.silenium.libs.flows.api.FlowItem
import dev.silenium.libs.flows.api.Sink
import dev.silenium.libs.flows.impl.FlowGraph
import dev.silenium.multimedia.core.data.AVMediaType
import dev.silenium.multimedia.core.data.AVPixelFormat.*
import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.data.FramePadMetadata
import dev.silenium.multimedia.core.decode.Decoder
import dev.silenium.multimedia.core.demux.FileDemuxer
import dev.silenium.multimedia.core.platform.linux.VaapiDecoder
import dev.silenium.multimedia.core.platform.linux.VaapiDeviceContext
import dev.silenium.multimedia.core.platform.linux.VaapiYuvToRgbConversion
import dev.silenium.multimedia.core.render.GLInteropImage
import dev.silenium.multimedia.core.render.GLRenderInterop
import dev.silenium.multimedia.core.util.Resources.loadTextFromClasspath
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.lwjgl.opengl.GL30.*
import java.nio.file.Path
import java.util.*

class VideoPlayer(path: Path) : Sink<Frame, FramePadMetadata> {
    val demuxer = FileDemuxer(path)
    private var decoder: Decoder? = null
    private var conversion: VaapiYuvToRgbConversion? = null
    private var deviceContext: VaapiDeviceContext? = null

    private val frames = Channel<Frame>(4, onBufferOverflow = BufferOverflow.SUSPEND)
    private var flowGraph: FlowGraph? = null

    private lateinit var interop: GLRenderInterop<*>
    private var glInitialized = false
    private var shaderProgram: Int = 0
    private var vao: Int = 0
    private var vbo: Int = 0
    private var ibo: Int = 0

    private val _inputMetadata = mutableMapOf<UInt, FramePadMetadata?>()
    override val inputMetadata: Map<UInt, FramePadMetadata?> = Collections.unmodifiableMap(_inputMetadata)

    override fun configure(pad: UInt, metadata: FramePadMetadata): Result<Unit> {
        check(!_inputMetadata.containsKey(pad)) { "Already configured" }
        _inputMetadata[pad] = metadata
        return Result.success(Unit)
    }

//    private var lastFrame: Frame? = null

    override suspend fun receive(item: FlowItem<Frame, FramePadMetadata>): Result<Unit> {
        println("PTS: ${item.value.pts}, VASurface: 0x${item.value.data[3].toString(16)}")
//        println("PTS: ${item.value.pts}")
//        lastFrame?.let {
//            if (it.pts >= item.value.pts) {
//                println("Dropping frame: ${item.value.pts}")
//                item.close()
//                return Result.success(Unit)
//            }
//        }
//        lastFrame?.close()
//        lastFrame = item.value.clone().getOrThrow()
        frames.send(item.value)
        return Result.success(Unit)
    }

    private fun initShader() {
        shaderProgram = glCreateProgram()
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(vertexShader, loadTextFromClasspath("shaders/video.vert"))
        glCompileShader(vertexShader)
        glGetShaderInfoLog(vertexShader).takeIf(String::isNotBlank)?.let(::println)
        glAttachShader(shaderProgram, vertexShader)
        glShaderSource(fragmentShader, loadTextFromClasspath("shaders/video.frag"))
        glCompileShader(fragmentShader)
        glGetShaderInfoLog(fragmentShader).takeIf(String::isNotBlank)?.let(::println)
        glAttachShader(shaderProgram, fragmentShader)
        glLinkProgram(shaderProgram)
        glGetProgramInfoLog(shaderProgram).takeIf(String::isNotBlank)?.let(::println)
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
        deviceContext = VaapiDeviceContext.detect()
        println("Device context: $deviceContext")
        val decoder = VaapiDecoder(deviceContext!!).also { this.decoder = it }
//        val decoder = Decoder().also { this.decoder = it }
        flowGraph = FlowGraph {
            val demuxerSource = source(demuxer, "demuxer")
            val decoderTransformer = transformer(decoder, "decoder")
            connect(demuxerSource to decoderTransformer) { selected, _, pad, metadata ->
                if (metadata.type == AVMediaType.AVMEDIA_TYPE_VIDEO && selected.isEmpty()) pad else null
            }
//            conversion = VaapiYuvToRgbConversion(decoder.framesContext)
//            conversion = VaapiYuvToRgbConversion()
//            val filterTransformer = transformer(conversion!!, "filter")
//            connect(decoderTransformer to filterTransformer)
            val sink = sink(this@VideoPlayer, "sink")
//            connect(filterTransformer to sink)
            connect(decoderTransformer to sink)
        }
        demuxer.start()

        initShader()
        initBuffers()
        interop = decoder.createGLRenderInterop()
        glInitialized = true
    }

    private suspend fun renderImage(image: GLInteropImage, hdr: Boolean = false) {
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
        if (hdr) {
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
                val format = decoder!!.outputMetadata[0u]!!.swFormat
                val hdr = when (format) {
                    AV_PIX_FMT_P010LE,
                    AV_PIX_FMT_P010BE,
                    AV_PIX_FMT_YUV420P10LE,
                    AV_PIX_FMT_YUV420P10BE -> true

                    else -> false
                }
                renderImage(it, hdr)
            }
        }
    }

    fun stop() {
        flowGraph?.close()
    }

    override fun close() = Unit
}

@Composable
fun rememberVideoPlayer(path: Path) =
    remember(path) { VideoPlayer(path) }
        .also {
            DisposableEffect(path) {
                onDispose {
                    it.stop()
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
