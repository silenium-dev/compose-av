package dev.silenium.multimedia.data

enum class Swizzle {
    USE_RED,
    USE_GREEN,
    USE_BLUE,
    USE_ALPHA,
}

data class Swizzles(val r: Swizzle, val g: Swizzle, val b: Swizzle, val a: Swizzle)
