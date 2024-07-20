package dev.silenium.multimedia.util

import dev.silenium.compose.gl.util.Natives
import dev.silenium.multimedia.BuildConstants

object NativeLoader {
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            Natives.load(BuildConstants.NativeLibName)
            loaded = true
        }
    }
}
