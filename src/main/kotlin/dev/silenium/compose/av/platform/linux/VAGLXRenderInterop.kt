package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.AVPixelFormat
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.render.GLInteropImage
import dev.silenium.compose.av.render.GLRenderInterop
import dev.silenium.compose.av.util.Natives

/**
 * Maps a VAAPI frame to a GLInteropImage in a GLX context.
 *
 * **Note:** Involves two copies: (VAAPI -> CPU -> GL) and is not as efficient as [VAEGLRenderInterop],
 * but the only supported option on default Compose for Desktop.
 */
class VAGLXRenderInterop(override val decoder: VaapiDecoder) : GLRenderInterop<VaapiDecoder>() {
    override fun isSupported(frame: Frame): Boolean {
        return frame.isHW && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI
    }

    override fun mapImpl(frame: Frame): Result<GLInteropImage> {
        return mapN(frame.nativePointer.address).map { GLInteropImage(frame, it) }
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun mapN(frame: Long): Result<Long>
