package dev.silenium.compose.av.render

import dev.silenium.compose.av.data.Frame

interface GLRenderInterop {
    fun isSupported(frame: Frame): Boolean
    fun map(frame: Frame): Result<GLInteropImage>
}
