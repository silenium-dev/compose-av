package dev.silenium.multimedia.vaapi

import dev.silenium.compose.gl.util.Natives
import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.data.NativeCleanable
import dev.silenium.multimedia.data.NativePointer
import dev.silenium.multimedia.data.asNativePointer
import dev.silenium.multimedia.decode.VaapiDecoder
import org.lwjgl.egl.EGL15

data class Surface(override val nativePointer: NativePointer) : NativeCleanable {
    val planeTextures: Array<Int> by lazy { planeTexturesN(nativePointer.address) }
}

object VA {
    init {
        Natives.load("libgl-demo.so")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun createTextureFromSurface(
        frame: Frame,
        decoder: VaapiDecoder,
        eglDisplay: Long = EGL15.eglGetCurrentDisplay(),
    ): Result<Surface> = runCatching {
        val vaDisplay = decoder.vaDisplay
        val vaSurface = frame.rawData[3]
//        println("VA Surface: 0x${vaSurface.toHexString()}")
//        println("VA Display: 0x${vaDisplay.toHexString()}")
        return createTextureFromSurfaceN(frame.swFormat!!.id, vaSurface, vaDisplay, eglDisplay)
            .map { Surface(it.asNativePointer(::destroySurfaceN)) }
    }
}

private external fun getVADisplayN(frame: Long): Long
private external fun createTextureFromSurfaceN(
    format: Int,
    vaSurface: Long,
    vaDisplay: Long,
    eglDisplay: Long
): Result<Long>
private external fun destroySurfaceN(surface: Long)
private external fun planeTexturesN(surface: Long): Array<Int>
