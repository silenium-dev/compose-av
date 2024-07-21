import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.Stats
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.decode.VaapiDecoder
import dev.silenium.multimedia.demux.FileDemuxer
import dev.silenium.multimedia.demux.Stream
import dev.silenium.multimedia.vaapi.Surface
import dev.silenium.multimedia.vaapi.VA
import org.lwjgl.opengles.GLES30.*
import java.nio.file.Files
import kotlin.io.path.outputStream


@OptIn(ExperimentalStdlibApi::class)
@Composable
fun App() {
    val demuxer = remember {
        val videoFile = Files.createTempFile("video", ".webm")
        FileDemuxer::class.java.classLoader.getResourceAsStream("video-short.webm").use {
            videoFile.outputStream().use(it::copyTo)
        }
        videoFile.toFile().deleteOnExit()
        FileDemuxer(videoFile)
    }
    val decoder = remember {
        VaapiDecoder(demuxer.streams.first { it.type == Stream.Type.VIDEO }, "/dev/dri/renderD128")
    }
    val frame = remember {
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.receive().getOrThrow().also {
            println("VASurface: 0x${it.rawData[3].toHexString()}")
            println("Format: ${it.format}")
            println("Is HW: ${it.isHW}")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            frame.close()
            decoder.close()
            demuxer.close()
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.background(Color.Black).fillMaxSize()) {
            var color by remember { mutableStateOf(Color.Red) }
            var surface: Surface? by remember { mutableStateOf(null) }
            var shaderProgram by remember { mutableStateOf(0) }
            var vao by remember { mutableStateOf(0) }
            var vbo by remember { mutableStateOf(0) }
            var ibo by remember { mutableStateOf(0) }
            val state = rememberGLSurfaceState()

            val vertices = floatArrayOf(
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                1f, 1f, 1f, 1f,
                -1f, 1f, 0f, 1f
            )
            val indices = intArrayOf(0, 1, 3, 1, 2, 3)

            suspend fun initGL() {
//                val image = Path("va_surface.png").inputStream().use(ImageIO::read)
//                glTexImage2D(
//                    GL_TEXTURE_2D, 0, GL_RGBA, image.width, image.height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
//                    image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
//                )

                surface = VA.createTextureFromSurface(frame, decoder).getOrThrow()

                shaderProgram = glCreateProgram()
                val vertexShader = glCreateShader(GL_VERTEX_SHADER)
                val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
                //language=Glsl
                glShaderSource(
                    vertexShader, """
                    #version 300 es
                    in vec2 position;
                    in vec2 uv;
                    out vec2 fragUV;
                    void main() {
                        gl_Position = vec4(position, 0.0, 1.0);
                        fragUV = uv;
                    }
                """.trimIndent()
                )
                glCompileShader(vertexShader)
                glGetShaderInfoLog(vertexShader).let(::println)
                glAttachShader(shaderProgram, vertexShader)
                //language=Glsl
                glShaderSource(
                    fragmentShader, """
                    #version 300 es
                    precision mediump float;
                    in vec2 fragUV;
                    out vec4 color;
                    
                    uniform sampler2D tex;
                    
                    void main() {
                    float r = texture(tex, fragUV).r;
                        color = vec4(r, r, r, 1.0);
                    }
                """.trimIndent()
                )
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
                    println("Failed to link program: $log")
                    return
                }

                vao = glGenVertexArrays()
                glBindVertexArray(vao)
                vbo = glGenBuffers()
                glBindBuffer(GL_ARRAY_BUFFER, vbo)
                glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
                val positionLocation = glGetAttribLocation(shaderProgram, "position")
                glVertexAttribPointer(positionLocation, 2, GL_FLOAT, true, 4 * 4, 0)
                glEnableVertexAttribArray(positionLocation)
                val uvLocation = glGetAttribLocation(shaderProgram, "uv")
                glVertexAttribPointer(uvLocation, 2, GL_FLOAT, true, 4 * 4, 2 * 4)
                glEnableVertexAttribArray(uvLocation)

                ibo = glGenBuffers()
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
                var err = glGetError()
                while (err != GL_NO_ERROR) {
                    println("GL error: $err")
                    err = glGetError()
                }
            }

            GLSurfaceView(
                state = state,
                modifier = Modifier.align(Alignment.TopStart).fillMaxHeight().aspectRatio(3508f / 1930f)
                    .drawWithContent {
                        drawContent()
                        drawRect(color.copy(alpha = 0.2f))
                    },
                cleanup = {
                    println("Cleanup")
                    surface?.close()
                    surface = null
                }
            ) {
                if (surface == null) initGL()
                glClearColor(0f, 0f, 0f, 1f)
                glClear(GL_COLOR_BUFFER_BIT)
                glUseProgram(shaderProgram)
                glBindTexture(GL_TEXTURE_2D, surface!!.planeTextures[0])
                glBindVertexArray(vao)
                glBindBuffer(GL_ARRAY_BUFFER, vbo)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

                var err = glGetError()
                while (err != GL_NO_ERROR) {
                    println("GL error: $err")
                    err = glGetError()
                }

                glFinish()
//                val pixels = IntArray(size.width * size.height)
//                glReadPixels(0, 0, size.width, size.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
//
//                BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB).apply {
//                    for (y in 0 until size.height) {
//                        for (x in 0 until size.width) {
//                            val i = y * size.width + x
//                            val pixel = pixels[i]
//                            val a = (pixel shr 24) and 0xff
//                            val b = (pixel shr 16) and 0xff
//                            val g = (pixel shr 8) and 0xff
//                            val r = pixel and 0xff
//                            setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
//                        }
//                    }
//                }.let {
//                    ImageIO.write(it, "png", java.io.File("va_surface.png"))
//                    println("Saved image")
//                }
            }
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Column {
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
                    Text("Render FPS: ${render.fps.median} ms")
                    Text("Render FPS (99th): ${render.fps.percentile(0.99, Stats.Percentile.LOWEST)}")
                }
            }
            Button(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                onClick = {
                    color = Color(
                        red = Math.random().toFloat(),
                        green = Math.random().toFloat(),
                        blue = Math.random().toFloat(),
                        alpha = 1.0f
                    )
                },
            ) {
                Text("Randomize color")
            }
        }
    }
}

suspend fun main(): Unit = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
