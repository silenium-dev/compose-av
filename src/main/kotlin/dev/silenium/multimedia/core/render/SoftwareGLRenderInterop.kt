package dev.silenium.multimedia.core.render

import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.decode.Decoder

class SoftwareGLRenderInterop(override val decoder: Decoder) : GLRenderInterop<Decoder>() {
    override fun isSupported(frame: Frame): Boolean {
        return true // Supports all formats, hw frames are first copied to sw
    }

    override fun mapImpl(frame: Frame): Result<GLInteropImage> =
        mapN(frame.nativePointer.address).map { GLInteropImage(frame, it) }
}

private external fun mapN(frame: Long): Result<Long>
