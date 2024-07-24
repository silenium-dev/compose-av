package dev.silenium.compose.av.util

import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.demux.Stream

fun Long.asFrameResult(stream: Stream) = if (this <= 0L) {
    Result.failure(this.toInt().asAVError())
} else {
    Result.success(Frame(this, stream))
}

fun Int.asUnitResult() = if (this != 0) {
    Result.failure(this.asAVError())
} else {
    Result.success(Unit)
}