import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.gl.fbo.EGLContext
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.Stats
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import dev.silenium.multimedia.vaapi.VA
import org.lwjgl.egl.EGL15.*
import org.lwjgl.opengles.GLES30.*
import org.lwjgl.system.MemoryUtil
import java.io.File
import javax.imageio.ImageIO


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val windowSize = LocalWindowInfo.current.containerSize
    Surface(modifier = Modifier.size(0.dp).padding(0.dp).drawWithContent {
        val currentDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
        val currentReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)

        val eglContext = EGLContext.fromCurrent() ?: return@drawWithContent drawContent()

        val texture = glGenTextures()
        val attr = intArrayOf(EGL_GL_TEXTURE_LEVEL, 0, EGL_IMAGE_PRESERVED, EGL_TRUE, EGL_NONE)
        val buf = MemoryUtil.memAllocPointer(attr.size)
        attr.forEachIndexed { idx, it ->
            buf.put(idx, it.toLong())
        }
        val eglImage = eglCreateImage(eglContext.display, eglContext.context, EGL_TEXTURE_2D, texture.toLong(), buf)
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            windowSize.width,
            windowSize.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null as IntArray?
        )
        val fbo = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            println("Failed to create framebuffer: $status")
            return@drawWithContent drawContent()
        }

        glBindFramebuffer(GL_READ_FRAMEBUFFER, currentDrawFbo)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo)
        glBlitFramebuffer(
            0,
            0,
            windowSize.width,
            windowSize.height,
            0,
            0,
            windowSize.width,
            windowSize.height,
            GL_COLOR_BUFFER_BIT,
            GL_LINEAR
        )
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentDrawFbo)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, currentReadFbo)
        glDeleteFramebuffers(fbo)
    }) {}
    MaterialTheme {
        Box(modifier = Modifier.background(Color.Black).fillMaxSize()) {
            var color by remember { mutableStateOf(Color.Red) }
            var texture by remember { mutableStateOf(0) }
            var surface by remember { mutableStateOf(0L) }
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
                texture = glGenTextures()
                glBindTexture(GL_TEXTURE_2D, texture)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
//                val image = Path("va_surface.png").inputStream().use(ImageIO::read)
//                glTexImage2D(
//                    GL_TEXTURE_2D, 0, GL_RGBA, image.width, image.height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
//                    image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
//                )

                val image = ImageIO.read(File("image.png"))

//                surface = VA.createTextureFromSurface(texture, vaSurface, vaDisplay)
                if (surface <= 0) {
                    println("Failed to create surface")
                    return
                }

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
                        color = vec4(texture(tex, fragUV).rgb, 1.0);
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
                state = state, modifier = Modifier.align(Alignment.TopStart).fillMaxHeight().aspectRatio(3508f / 1930f),
                cleanup = {
                    println("Cleanup")
                    if (surface > 0) {
                        VA.destroySurface(surface)
                        surface = 0
                    }
                    if (texture > 0) {
                        glDeleteTextures(texture)
                        texture = 0
                    }
                }
            ) {
                if (texture == 0) initGL()
                glClearColor(0f, 0f, 0f, 1f)
                glClear(GL_COLOR_BUFFER_BIT)
                glUseProgram(shaderProgram)
                glBindTexture(GL_TEXTURE_2D, texture)
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
            Column(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
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
            Button(onClick = {
                color = Color(
                    red = Math.random().toFloat(),
                    green = Math.random().toFloat(),
                    blue = Math.random().toFloat(),
                    alpha = 1.0f
                )
            }) {
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
