package dev.silenium.multimedia.render

import dev.silenium.multimedia.data.Frame

interface GLRenderInterop {
    fun isSupported(frame: Frame): Boolean
    fun map(frame: Frame): Result<GLInteropImage>
}
