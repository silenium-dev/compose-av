package dev.silenium.compose.av.util

import dev.silenium.compose.av.OSUtils
import dev.silenium.compose.gl.util.Natives

object NativeLoader {
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            Natives.load(OSUtils.libFileName())
            Natives.load("linux-x86_64/libavutil.so")
            Natives.load("linux-x86_64/libavcodec.so")
            Natives.load("linux-x86_64/libavformat.so")
            Natives.load("linux-x86_64/libswscale.so")
            loaded = true
        }
    }
}
