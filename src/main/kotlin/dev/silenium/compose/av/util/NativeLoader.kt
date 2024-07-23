package dev.silenium.compose.av.util

import dev.silenium.compose.gl.util.Natives
import dev.silenium.compose.av.OSUtils

object NativeLoader {
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            Natives.load(OSUtils.libFileName())
            loaded = true
        }
    }
}
