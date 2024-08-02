package dev.silenium.multimedia.core.util

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.multimedia.build.BuildConstants


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
