package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.AVPixelFormat
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.render.GLInteropImage
import dev.silenium.compose.av.render.GLRenderInterop
import dev.silenium.compose.av.util.Natives
import org.slf4j.LoggerFactory

class VAGLXRenderInterop(override val decoder: VaapiDecoder) : GLRenderInterop<VaapiDecoder> {
    init {
        Natives.ensureLoaded()
        log.error("VAGLXRenderInterop is not working yet")
    }

    override fun isSupported(frame: Frame): Boolean {
        return frame.isHW && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI
    }

    override fun map(frame: Frame): Result<GLInteropImage> {
        val vaDisplay = getVADisplayN(frame.nativePointer.address)
        val vaSurface = frame.rawData[3]
        return mapN(
            vaSurface,
            vaDisplay,
            frame.nativePointer.address,
            decoder.nativePointer.address
        ).map { GLInteropImage(frame, it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VAGLXRenderInterop::class.java)
    }
}

private external fun getVADisplayN(frame: Long): Long
private external fun mapN(
    vaSurface: Long,
    vaDisplay: Long,
    frame: Long,
    codecContext: Long,
): Result<Long>
