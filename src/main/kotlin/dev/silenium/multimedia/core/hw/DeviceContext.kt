package dev.silenium.multimedia.core.hw

import dev.silenium.libs.flows.api.Reference
import dev.silenium.multimedia.core.data.AVBufferRef
import dev.silenium.multimedia.core.data.AVHWDeviceType

open class DeviceContext(val type: AVHWDeviceType, val bufferRef: AVBufferRef) : Reference<DeviceContext> {
    constructor(type: AVHWDeviceType, device: String? = null) : this(
        type,
        AVBufferRef(createN(type.id, device).getOrThrow())
    )

    val address: Long by lazy { bufferRef.nativePointer.address }

    override fun clone(): Result<DeviceContext> {
        return bufferRef.clone().map { DeviceContext(type, it) }
    }

    override fun close() {
        bufferRef.close()
    }
}

private external fun createN(type: Int, device: String?): Result<Long>
