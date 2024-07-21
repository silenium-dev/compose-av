package dev.silenium.multimedia.util

fun Int.asAVErrorString(): String = avErrorStringN(this)

private external fun avErrorStringN(error: Int): String
