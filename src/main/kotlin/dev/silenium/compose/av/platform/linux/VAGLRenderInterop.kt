package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.AVPixelFormat
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.render.GLInteropImage
import dev.silenium.compose.av.render.GLRenderInterop
import dev.silenium.compose.av.util.Natives
import org.lwjgl.egl.EGL15

class VAGLRenderInterop(
    override val decoder: VaapiDecoder,
    private val eglDisplay: Long = EGL15.eglGetCurrentDisplay(),
) : GLRenderInterop<VaapiDecoder> {
    init {
        Natives.ensureLoaded()
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
