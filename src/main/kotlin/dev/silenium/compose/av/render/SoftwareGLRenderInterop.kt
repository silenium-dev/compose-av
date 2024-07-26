package dev.silenium.compose.av.render

import dev.silenium.compose.av.data.AVPixelFormat
import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.decode.SoftwareDecoder

class SoftwareGLRenderInterop(override val decoder: SoftwareDecoder) : GLRenderInterop<SoftwareDecoder> {
    override fun isSupported(frame: Frame): Boolean {
        return !frame.isHW
    }

    private fun planeFractions(pixelFormat: AVPixelFormat): Map<Int, Pair<Int, Int>> = when (pixelFormat) {
        AVPixelFormat.AV_PIX_FMT_YUV420P10LE,
        AVPixelFormat.AV_PIX_FMT_YUV420P10BE,
        AVPixelFormat.AV_PIX_FMT_YUV420P -> mapOf(0 to (1 to 1), 1 to (2 to 2), 2 to (2 to 2))

        else -> error("Unsupported pixel format: $pixelFormat")
    }

    override fun map(frame: Frame): Result<GLInteropImage> =
        mapN(frame.nativePointer.address).map { GLInteropImage(frame, it) }
}

private external fun mapN(frame: Long): Result<Long>
