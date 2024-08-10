package dev.silenium.multimedia.core.util

fun Int.asUnitResult() = if (this != 0) {
    Result.failure(this.asAVError())
} else {
    Result.success(Unit)
}
