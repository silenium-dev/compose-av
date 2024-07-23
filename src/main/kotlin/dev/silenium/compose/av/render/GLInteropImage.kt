package dev.silenium.compose.av.render

import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.data.NativeCleanable
import dev.silenium.multimedia.data.NativePointer
import dev.silenium.multimedia.data.asNativePointer

data class GLInteropImage(val frame: Frame, override val nativePointer: NativePointer) : NativeCleanable {
    constructor(frame: Frame, pointer: Long) : this(frame, pointer.asNativePointer(::destroyN))

    val planeTextures: Array<Int> by lazy { planeTexturesN(nativePointer.address) }
    val planeSwizzles: Array<Swizzles> by lazy { planeSwizzlesN(nativePointer.address) }
}

private external fun destroyN(surface: Long)
private external fun planeTexturesN(surface: Long): Array<Int>
private external fun planeSwizzlesN(surface: Long): Array<Swizzles>
