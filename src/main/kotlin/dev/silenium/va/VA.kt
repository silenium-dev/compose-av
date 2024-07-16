package dev.silenium.va

import dev.silenium.compose.gl.util.Natives

object VA {
    init {
        Natives.load("libgl-demo.so")
    }

    external fun createTextureFromSurface(texture: Int): Long
    external fun destroySurface(surface: Long)
}
