package dev.silenium.multimedia.core.util

class EGLException(val operation: String, val error: Long) :
    Exception("EGL error during $operation: ${error.asEGLErrorString()}")

class GLException(val operation: String, val error: Int) :
    Exception("GL error during $operation: ${error.asGLErrorString()}")

class MPVException(val operation: String, val error: Int) :
    Exception("MPV error during $operation: ${error.asMPVErrorString()}")

fun Long.asEGLError(operation: String = "EGL call"): Exception = EGLException(operation, this)
fun Long.asEGLErrorString(): String = eglErrorStringN(this)

fun Int.asGLError(operation: String = "GL call"): Exception = GLException(operation, this)
fun Int.asGLErrorString(): String = glErrorStringN(this)

fun Int.asMPVError(operation: String = "MPV call"): Exception = MPVException(operation, this)
fun Int.asMPVErrorString(): String = mpvErrorStringN(this)

private external fun mpvErrorStringN(error: Int): String
private external fun glErrorStringN(error: Int): String
private external fun eglErrorStringN(error: Long): String
