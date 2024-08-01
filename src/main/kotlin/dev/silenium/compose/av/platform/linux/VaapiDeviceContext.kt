package dev.silenium.compose.av.platform.linux

import dev.silenium.compose.av.data.NativeCleanable
import dev.silenium.compose.av.data.NativePointer
import dev.silenium.compose.av.data.asNativePointer
import dev.silenium.compose.av.util.Natives
import dev.silenium.compose.av.util.destroyAVBufferN
import dev.silenium.compose.gl.context.GLXContext

sealed class VaapiDeviceContext(override val nativePointer: NativePointer) : NativeCleanable {
    constructor(nativePointer: Long) : this(nativePointer.asNativePointer(::destroyAVBufferN))

    val display: Long by lazy { getDisplayN(nativePointer.address) }

    data class GLX(
        val glxContext: GLXContext = GLXContext.fromCurrent()
            ?: error("No context current, please specify explicit context")
    ) : VaapiDeviceContext(createGlxN(glxContext.display).getOrThrow())

    data class DRM(val drmDevice: String) : VaapiDeviceContext(createDrmN(drmDevice).getOrThrow())

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private external fun getDisplayN(device: Long): Long
private external fun createDrmN(device: String): Result<Long>
private external fun createGlxN(display: Long): Result<Long>
