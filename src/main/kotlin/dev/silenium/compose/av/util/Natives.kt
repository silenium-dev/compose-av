package dev.silenium.compose.av.util

import dev.silenium.compose.av.BuildConstants
import dev.silenium.libs.jni.NativeLoader


object Natives {
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            NativeLoader.loadLibraryFromClasspath(BuildConstants.LIBRARY_NAME).getOrThrow()
            loaded = true
        }
    }
}
