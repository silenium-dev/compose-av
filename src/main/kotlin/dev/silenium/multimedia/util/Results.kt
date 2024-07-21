package dev.silenium.multimedia.util

import dev.silenium.multimedia.data.Frame
import dev.silenium.multimedia.demux.Stream

fun Long.asFrameResult(stream: Stream) = if (this <= 0L) {
    Result.failure(this.toInt().asAVError())
} else {
    Result.success(Frame(this, stream))
}

fun Int.asUnitResult() = if (this < 0) {
    Result.failure(this.asAVError())
} else {
    Result.success(Unit)
}
