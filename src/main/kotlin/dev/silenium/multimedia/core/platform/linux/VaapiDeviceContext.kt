package dev.silenium.multimedia.core.platform.linux

import dev.silenium.compose.gl.context.EGLContext
import dev.silenium.compose.gl.context.GLXContext
import dev.silenium.multimedia.core.data.AVBufferRef
import dev.silenium.multimedia.core.data.AVHWDeviceType
import dev.silenium.multimedia.core.hw.DeviceContext
import dev.silenium.multimedia.core.util.Natives

sealed class VaapiDeviceContext(bufferRef: AVBufferRef) :
    DeviceContext(AVHWDeviceType.AV_HWDEVICE_TYPE_VAAPI, bufferRef) {
    constructor(nativePointer: Long) : this(AVBufferRef(nativePointer))

    val display: Long by lazy { getDisplayN(bufferRef.nativePointer.address) }

    data class GLX(
        val glxContext: GLXContext = GLXContext.fromCurrent()
            ?: error("No context current, please specify explicit context")
    ) : VaapiDeviceContext(createGlxN(glxContext.display).getOrThrow())

    data class DRM(val drmDevice: String) : VaapiDeviceContext(createDrmN(drmDevice).getOrThrow())

    companion object {
        init {
            Natives.ensureLoaded()
        }

        fun detect() = when {
            EGLContext.fromCurrent() != null -> DRM("/dev/dri/renderD128")
            GLXContext.fromCurrent() != null -> GLX()
            else -> error("No context current, please specify explicit context")
        }
    }
}

private external fun getDisplayN(device: Long): Long
private external fun createDrmN(device: String): Result<Long>
private external fun createGlxN(display: Long): Result<Long>
