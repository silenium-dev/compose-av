package dev.silenium.multimedia.core.data

import dev.silenium.multimedia.core.hw.FramesContext
import kotlin.time.Duration

data class FramePadMetadata(
    val width: Int,
    val height: Int,
    val format: AVPixelFormat,
    val swFormat: AVPixelFormat?,
    val isHW: Boolean,
    val framesContext: FramesContext?,
    val colorSpace: AVColorSpace,
    val colorPrimaries: AVColorPrimaries,
    val colorRange: AVColorRange,
    val colorTrc: AVColorTransferCharacteristic,
    val sampleAspectRatio: Rational,
    val timeBase: Rational,
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(!isHW || swFormat != null) { "HW frames must have a sw format" }
    }
}

data class FrameMetadata(
    val width: Int,
    val height: Int,
    val format: AVPixelFormat,
    val swFormat: AVPixelFormat?,
    val isHW: Boolean,
    val framesContext: FramesContext?,
    val colorSpace: AVColorSpace,
    val colorPrimaries: AVColorPrimaries,
    val colorRange: AVColorRange,
    val colorTrc: AVColorTransferCharacteristic,
    val sampleAspectRatio: Rational,
    val timeBase: Rational,
    val pts: Duration,
    val duration: Duration,
    val bestEffortTimestamp: Duration,
    val keyFrame: Boolean,
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(!isHW || swFormat != null) { "HW frames must have a sw format" }
    }
}
