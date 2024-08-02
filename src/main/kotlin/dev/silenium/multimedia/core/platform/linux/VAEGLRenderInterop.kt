package dev.silenium.multimedia.core.platform.linux

import dev.silenium.multimedia.core.data.AVPixelFormat
import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.render.GLInteropImage
import dev.silenium.multimedia.core.render.GLRenderInterop
import dev.silenium.multimedia.core.util.Natives
import org.lwjgl.egl.EGL15

/**
 * Maps a VAAPI frame to a GLInteropImage in an EGL context.
 *
 * **Note:** zero-copy implementation: (VAAPI -> GL),
 * but only supported when using patched Skiko in Compose for Desktop.
 */
class VAEGLRenderInterop(
    override val decoder: VaapiDecoder,
    private val eglDisplay: Long = EGL15.eglGetCurrentDisplay(),
) : GLRenderInterop<VaapiDecoder>() {
    override fun mapImpl(frame: Frame): Result<GLInteropImage> = runCatching {
        return mapN(frame.nativePointer.address, eglDisplay)
            .map { GLInteropImage(frame, it) }
    }

    override fun isSupported(frame: Frame): Boolean {
        return frame.isHW && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI && frame.swFormat == AVPixelFormat.AV_PIX_FMT_RGB0
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun mapN(frame: Long, eglDisplay: Long): Result<Long>
