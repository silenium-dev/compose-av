package dev.silenium.multimedia.core.util

class AVException(val operation: String, val error: Int) :
    Exception("FFmpeg error during $operation: ${error.asAVErrorString()}")

class EGLException(val operation: String, val error: Long) :
    Exception("EGL error during $operation: ${error.asEGLErrorString()}")

class GLException(val operation: String, val error: Int) :
    Exception("GL error during $operation: ${error.asGLErrorString()}")

class VAException(val operation: String, val error: Int) :
    Exception("VA error during $operation: ${error.asVAErrorString()}")

class MPVException(val operation: String, val error: Int) :
    Exception("MPV error during $operation: ${error.asMPVErrorString()}")

fun Int.asAVError(operation: String = "ffmpeg call"): Exception = AVException(operation, this)
fun Int.asAVErrorString(): String = avErrorStringN(this)
fun Long.asEGLError(operation: String = "EGL call"): Exception = EGLException(operation, this)
fun Long.asEGLErrorString(): String = eglErrorStringN(this)
fun Int.asGLError(operation: String = "GL call"): Exception = GLException(operation, this)
fun Int.asGLErrorString(): String = glErrorStringN(this)
fun Int.asVAError(operation: String = "VA call"): Exception = VAException(operation, this)
fun Int.asVAErrorString(): String = vaErrorStringN(this)
fun Int.asMPVError(operation: String = "MPV call"): Exception = MPVException(operation, this)
fun Int.asMPVErrorString(): String = mpvErrorStringN(this)

private external fun avErrorStringN(error: Int): String
private external fun eglErrorStringN(error: Long): String
private external fun glErrorStringN(error: Int): String
private external fun vaErrorStringN(error: Int): String
private external fun mpvErrorStringN(error: Int): String

val Throwable.shouldIgnore
    get() = when (this) {
        is AVException -> when (error) {
            -11 -> true // EAGAIN
            -12 -> true // ENOMEM
            -541478725 -> true // AVERROR_EOF
            else -> false
        }

        else -> false
    }
