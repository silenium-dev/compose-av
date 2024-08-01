package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.AVPixelFormat
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.render.GLInteropImage
import dev.silenium.compose.av.render.GLRenderInterop
import dev.silenium.compose.av.util.Natives
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
        return frame.isHW && frame.format == AVPixelFormat.AV_PIX_FMT_VAAPI
    }

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun mapN(frame: Long, eglDisplay: Long): Result<Long>
