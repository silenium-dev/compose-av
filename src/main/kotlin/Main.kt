import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.gl.fbo.FBOPool
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lwjgl.opengles.GLES30.*
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


@OptIn(ExperimentalStdlibApi::class)
@Composable
fun App() {
    val textureDir = remember { Path("textures").createDirectories() }
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

    val channel = remember { Channel<Frame>(4) }
    var frame by remember {
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.submit(demuxer.nextPacket().getOrThrow())
        decoder.receive().getOrThrow().also {
            println("VASurface: 0x${it.rawData[3].toHexString()}")
            println("Format: ${it.format}")
            println("SW-Format: ${it.swFormat}")
            println("Is HW: ${it.isHW}")
        }.let { mutableStateOf(it) }
    }
    DisposableEffect(Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val packet = demuxer.nextPacket().getOrNull() ?: break
                decoder.submit(packet)
                while (isActive) {
                    channel.send(decoder.receive().getOrNull() ?: break)
                }
            }
        }
        onDispose {
            job.cancel()
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
                surface = VA.createTextureFromSurface(frame, decoder).getOrThrow()
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
                glUseProgram(shaderProgram)

                ibo = glGenBuffers()
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
                var err = glGetError()
                while (err != GL_NO_ERROR) {
                    println("GL error: $err")
                    err = glGetError()
                }
            }

            var idx by remember { mutableStateOf(0L) }
            val fboField = remember {
                val prop =
                    Class.forName("dev.silenium.compose.gl.surface.GLDrawScopeImpl").kotlin.memberProperties.first { it.name == "fbo" } as KProperty1<Any, FBOPool.FBO>
                prop.isAccessible = true
                prop
            }

            GLSurfaceView(
                state = state,
                modifier = Modifier.align(Alignment.TopStart).fillMaxHeight().aspectRatio(16f / 9f)
//                    .drawWithContent {
//                        drawContent()
//                        drawRect(color.copy(alpha = 0.2f))
//                    }
                ,
                cleanup = {
                    println("Cleanup")
                    surface?.close()
                    surface = null
                }
            ) {
                val fbo = fboField.get(this)
                println("${idx}: FBO ${fbo.id}")
                if (surface == null) initGL()
                channel.receive().let {
                    frame.close()
                    surface?.close()
                    frame = it
                    surface = VA.createTextureFromSurface(frame, decoder).getOrThrow()
                    println("${idx}: va surface: 0x${frame.rawData[3].toHexString()}")
                    println("${idx}: y texture: 0x${surface!!.planeTextures[0].toHexString()}")
                    println("${idx}: u texture: 0x${surface!!.planeTextures[1].toHexString()}")
                }
                glClearColor(0f, 0f, 0f, 1f)
                glClear(GL_COLOR_BUFFER_BIT)
                glUseProgram(shaderProgram)

                val formatLocation = glGetUniformLocation(shaderProgram, "tex_format")
                glUniform1i(formatLocation, 2)
                val limitedLocation = glGetUniformLocation(shaderProgram, "limitedRange")
                glUniform1i(limitedLocation, 1)
                val hdrLocation = glGetUniformLocation(shaderProgram, "enableHDR")
                glUniform1i(hdrLocation, 1)
                val texYLocation = glGetUniformLocation(shaderProgram, "tex_y")
                glUniform1i(texYLocation, 0)
                val texULocation = glGetUniformLocation(shaderProgram, "tex_u")
                glUniform1i(texULocation, 1)
                val texVLocation = glGetUniformLocation(shaderProgram, "tex_v")
                glUniform1i(texVLocation, 2)
                surface!!.planeTextures.forEachIndexed { idx, it ->
                    glActiveTexture(GL_TEXTURE0 + idx)
                    glBindTexture(GL_TEXTURE_2D, it)
                }
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
                val pixels = IntArray(size.width * size.height)
                glReadPixels(0, 0, size.width, size.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

                BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB).apply {
                    for (y in 0 until size.height) {
                        for (x in 0 until size.width) {
                            val i = y * size.width + x
                            val pixel = pixels[i]
                            val a = (pixel shr 24) and 0xff
                            val b = (pixel shr 16) and 0xff
                            val g = (pixel shr 8) and 0xff
                            val r = pixel and 0xff
                            setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                        }
                    }
                }.let { image ->
                    println("${idx}: Saving image")
                    textureDir.resolve("va_surface-%03d.png".format(idx++)).outputStream().use {
                        ImageIO.write(image, "png", it)
                    }
                }
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
