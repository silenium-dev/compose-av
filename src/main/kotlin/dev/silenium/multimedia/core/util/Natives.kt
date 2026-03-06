package dev.silenium.multimedia.core.util

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import dev.silenium.multimedia.build.BuildConstants


object Natives {
    private var loaded = false

    private val platformDeps = mapOf(
        Platform.OS.LINUX to listOf(
            "avutil",
            "swresample",
            "swscale",
            "avcodec",
            "avformat",
            "avfilter",
            "avdevice",
        ),
        Platform.OS.WINDOWS to listOf(
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
