package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.gl.util.Natives
import dev.silenium.multimedia.data.AVPixelFormat
import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.render.GLInteropImage
import dev.silenium.multimedia.render.GLRenderInterop
import org.lwjgl.egl.EGL15

class VAGLRenderInterop(
    private val decoder: VaapiDecoder,
    private val eglDisplay: Long = EGL15.eglGetCurrentDisplay(),
) : GLRenderInterop {
    init {
        Natives.load("libgl-demo.so")
    }

    override fun map(frame: Frame): Result<GLInteropImage> = runCatching {
//        val vaDisplay = decoder.vaDisplay
        val vaDisplay = getVADisplayN(frame.nativePointer.address)
        val vaSurface = frame.rawData[3]
//        println("VA Surface: 0x${vaSurface.toHexString()}")
//        println("VA Display: 0x${vaDisplay.toHexString()}")
        return mapN(frame.swFormat!!.id, vaSurface, vaDisplay, eglDisplay)
            .map { GLInteropImage(frame, it) }
    }

    override fun isSupported(frame: Frame): Boolean {
        return frame.isHW && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI
    }
}

private external fun getVADisplayN(frame: Long): Long
private external fun mapN(
    format: Int,
    vaSurface: Long,
    vaDisplay: Long,
    eglDisplay: Long
): Result<Long>
