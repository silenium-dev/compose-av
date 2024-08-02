package dev.silenium.multimedia.core.util

import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.data.Rational

fun Long.asFrameResult(timeBase: Rational) = if (this <= 0L) {
    Result.failure(this.toInt().asAVError())
} else {
    Result.success(Frame(this, timeBase))
}

fun Int.asUnitResult() = if (this != 0) {
    Result.failure(this.asAVError())
} else {
    Result.success(Unit)
}
