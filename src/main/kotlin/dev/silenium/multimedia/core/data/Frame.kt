package dev.silenium.multimedia.core.data

import dev.silenium.libs.flows.api.Reference
import dev.silenium.multimedia.core.hw.FramesContext
import dev.silenium.multimedia.core.util.Natives
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Frame(override val nativePointer: NativePointer, val timeBase: Rational) :
    NativeCleanable, Reference<Frame> {
    constructor(pointer: Long, timeBase: Rational) : this(pointer.asNativePointer(::releaseFrameN), timeBase)

    val width: Int by lazy { widthN(nativePointer.address) }
    val height: Int by lazy { heightN(nativePointer.address) }
    val format: AVPixelFormat by lazy { fromId<AVPixelFormat>(formatN(nativePointer.address)) }
    val swFormat: AVPixelFormat? by lazy { swFormatN(nativePointer.address).takeIf { it >= 0 }?.let(::fromId) }
    val isHW: Boolean by lazy { isHWN(nativePointer.address) }
    val colorSpace: AVColorSpace by lazy { fromId(colorSpaceN(nativePointer.address)) }
    val colorPrimaries: AVColorPrimaries by lazy { fromId(colorPrimariesN(nativePointer.address)) }
    val colorRange: AVColorRange by lazy { fromId(colorRangeN(nativePointer.address)) }
    val colorTrc: AVColorTransferCharacteristic by lazy { fromId(colorTrcN(nativePointer.address)) }
    val sampleAspectRatio: Rational by lazy { sampleAspectRatioN(nativePointer.address) }
    val pts: Duration by lazy { (ptsN(nativePointer.address) * timeBase.asDouble).seconds }
    val duration: Duration by lazy { (durationN(nativePointer.address) * timeBase.asDouble).seconds }
    val bestEffortTimestamp: Duration by lazy { (bestEffortTimestampN(nativePointer.address) * timeBase.asDouble).seconds }
    val keyFrame: Boolean by lazy { keyFrameN(nativePointer.address) }
    val hwFramesContext: FramesContext? by lazy {
        hwFramesContextN(nativePointer.address).getOrNull()
            ?.let { FramesContext(AVHWDeviceType.fromFormat(format), AVBufferRef(it)) }
    }

    val buf: Array<AVBufferRef?> by lazy { bufN(nativePointer.address) }
    val data: Array<Long> by lazy { dataN(nativePointer.address) }
    val pitch: Array<Int> by lazy { pitchN(nativePointer.address) }

    val metadata by lazy {
        padMetadata.frame(pts, duration, bestEffortTimestamp, keyFrame)
    }
    val padMetadata by lazy {
        FramePadMetadata(
            width, height, format,
            swFormat, isHW, hwFramesContext,
            colorSpace, colorPrimaries, colorRange, colorTrc,
            sampleAspectRatio, timeBase,
        )
    }

    fun transferToSW(): Result<Frame> {
        return transferToSWN(nativePointer.address).map { Frame(it, timeBase) }
    }

    override fun clone(): Result<Frame> {
        return cloneN(nativePointer.address).map { Frame(it, timeBase) }
    }

    override fun close() = super.close()

    companion object {
        init {
            Natives.ensureLoaded()
        }
    }
}

private fun FramePadMetadata.frame(
    pts: Duration,
    duration: Duration,
    bestEffortTimestamp: Duration,
    keyFrame: Boolean,
) = FrameMetadata(
    width, height, format,
    swFormat, isHW, framesContext,
    colorSpace, colorPrimaries, colorRange, colorTrc,
    sampleAspectRatio, timeBase,
    pts, duration, bestEffortTimestamp, keyFrame,
)

private external fun timeBaseN(frame: Long): Rational
private external fun widthN(frame: Long): Int
private external fun heightN(frame: Long): Int
private external fun formatN(frame: Long): Int
private external fun colorSpaceN(frame: Long): Int
private external fun colorPrimariesN(frame: Long): Int
private external fun colorRangeN(frame: Long): Int
private external fun colorTrcN(frame: Long): Int
private external fun swFormatN(frame: Long): Int
private external fun keyFrameN(frame: Long): Boolean
private external fun sampleAspectRatioN(frame: Long): Rational
private external fun ptsN(frame: Long): Long
private external fun bestEffortTimestampN(frame: Long): Long
private external fun durationN(frame: Long): Long
private external fun isHWN(frame: Long): Boolean
private external fun bufN(frame: Long): Array<AVBufferRef?>
private external fun dataN(frame: Long): Array<Long>
private external fun pitchN(frame: Long): Array<Int>

private external fun transferToSWN(frame: Long): Result<Long>
private external fun cloneN(frame: Long): Result<Long>
private external fun hwFramesContextN(frame: Long): Result<Long>

private external fun releaseFrameN(frame: Long)
