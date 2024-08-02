package dev.silenium.multimedia.core.render

import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.decode.SoftwareDecoder

class SoftwareGLRenderInterop(override val decoder: SoftwareDecoder) : GLRenderInterop<SoftwareDecoder>() {
    override fun isSupported(frame: Frame): Boolean {
        return !frame.isHW
    }

    override fun mapImpl(frame: Frame): Result<GLInteropImage> =
        mapN(frame.nativePointer.address).map { GLInteropImage(frame, it) }
}

private external fun mapN(frame: Long): Result<Long>
