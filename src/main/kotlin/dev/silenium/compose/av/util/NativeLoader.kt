package dev.silenium.compose.av.util

import dev.silenium.compose.av.OSUtils
import dev.silenium.compose.gl.util.Natives

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
