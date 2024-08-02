package dev.silenium.multimedia.core.platform.linux

import dev.silenium.multimedia.core.data.AVPixelFormat
import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.render.GLInteropImage
import dev.silenium.multimedia.core.render.GLRenderInterop
import dev.silenium.multimedia.core.util.Natives

/**
 * Maps a VAAPI frame to a GLInteropImage in a GLX context.
 *
 * **Note:** Involves two copies: (VAAPI -> CPU -> GL) and is not as efficient as [VAEGLRenderInterop],
 * but the only supported option on default Compose for Desktop.
 */
class VAGLXRenderInterop(override val decoder: VaapiDecoder) : GLRenderInterop<VaapiDecoder>() {
    override fun isSupported(frame: Frame): Boolean {
        return frame.isHW
                && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI
                && frame.swFormat == AVPixelFormat.AV_PIX_FMT_RGB0
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
