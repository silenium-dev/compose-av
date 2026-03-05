package dev.silenium.multimedia.core.util

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import dev.silenium.multimedia.build.BuildConstants


object Natives {
    private var loaded = false

    private val platformDeps = mapOf(
        Platform.OS.LINUX to setOf(
            "avcodec",
            "avdevice",
            "avfilter",
            "avformat",
            "avutil",
            "compose-av",
            "swresample",
            "swscale",
        ),
        Platform.OS.WINDOWS to setOf(
            "compose-av",
            "libmpv-2",
        ),
    )

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            val deps = platformDeps[NativeLoader.nativePlatform.os]
                ?: error("Unsupported platform: ${NativeLoader.nativePlatform}")
            deps.forEach {
                NativeLoader.loadLibraryFromClasspath(it).getOrThrow()
            }
            NativeLoader.loadLibraryFromClasspath(BuildConstants.LIBRARY_NAME).getOrThrow()
            loaded = true
        }
    }
}
