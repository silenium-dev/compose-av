package dev.silenium.multimedia.core.hw

import dev.silenium.libs.flows.api.Reference
import dev.silenium.multimedia.core.data.AVBufferRef
import dev.silenium.multimedia.core.data.AVHWDeviceType
import dev.silenium.multimedia.core.data.AVPixelFormat
import dev.silenium.multimedia.core.data.fromId

open class FramesContext(
    val type: AVHWDeviceType,
    val bufferRef: AVBufferRef,
) : Reference<FramesContext> {
    constructor(
        deviceContext: DeviceContext,
        width: Int,
        height: Int,
        swFormat: AVPixelFormat,
        initialPoolSize: Int = 1
    ) : this(
        deviceContext.type,
        AVBufferRef(
            createN(
                deviceContext = deviceContext.address,
                width = width,
                height = height,
                format = deviceContext.type.hwFormat.id,
                swFormat = swFormat.id,
                initialPoolSize = initialPoolSize
            ).getOrThrow()
        ),
    )

    val address: Long get() = bufferRef.nativePointer.address

    val width: Int by lazy { widthN(address) }
    val height: Int by lazy { heightN(address) }
    val format: AVPixelFormat by lazy { fromId(formatN(address)) }
    val swFormat: AVPixelFormat by lazy { fromId(swFormatN(address)) }
    val initialPoolSize: Int by lazy { initialPoolSizeN(address) }
    val deviceContext: DeviceContext by lazy {
        DeviceContext(type, AVBufferRef(deviceContextN(address)))
    }

    override fun clone(): Result<FramesContext> {
        return bufferRef.clone().map { FramesContext(type, it) }
    }

    override fun close() {
        bufferRef.close()
    }
}

private external fun createN(
    deviceContext: Long,
    width: Int,
    height: Int,
    format: Int,
    swFormat: Int,
    initialPoolSize: Int
): Result<Long>

private external fun widthN(pointer: Long): Int
private external fun heightN(pointer: Long): Int
private external fun formatN(pointer: Long): Int
private external fun swFormatN(pointer: Long): Int
private external fun initialPoolSizeN(pointer: Long): Int
private external fun deviceContextN(pointer: Long): Long
