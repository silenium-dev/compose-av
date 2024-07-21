package dev.silenium.multimedia.util

class AVException(val operation: String, val error: Int) :
    Exception("FFmpeg error during $operation: ${error.asAVErrorString()}")

fun Int.asAVError(operation: String = "ffmpeg call"): Exception = AVException(operation, this)
fun Int.asAVErrorString(): String = avErrorStringN(this)

private external fun avErrorStringN(error: Int): String
