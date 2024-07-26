package dev.silenium.compose.av.render

import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.decode.Decoder

interface GLRenderInterop<D : Decoder<D>> {
    val decoder: D
    fun isSupported(frame: Frame): Boolean
    fun map(frame: Frame): Result<GLInteropImage>
}
