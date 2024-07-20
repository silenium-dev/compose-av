package dev.silenium.multimedia.vaapi

import dev.silenium.compose.gl.util.Natives
import org.lwjgl.egl.EGL15

object VA {
    init {
        Natives.load("libgl-demo.so")
    }

    fun createTextureFromSurface(
        texture: Int,
        vaSurface: Long,
        vaDisplay: Long,
        eglDisplay: Long = EGL15.eglGetCurrentDisplay(),
    ): Long {
        return createTextureFromSurfaceN(texture, vaSurface, vaDisplay, eglDisplay)
    }

    fun destroySurface(surface: Long) {
        destroySurfaceN(surface)
    }
}

private external fun getVADisplayN(hwDeviceCtx: Long): Long
private external fun createTextureFromSurfaceN(texture: Int, vaSurface: Long, vaDisplay: Long, eglDisplay: Long): Long
private external fun destroySurfaceN(surface: Long)
