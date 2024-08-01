package dev.silenium.compose.av.filter

import dev.silenium.compose.av.data.Frame

interface SimpleFilter {
    fun submit(frame: Frame): Result<Unit>
    fun receive(): Result<Frame>
}
