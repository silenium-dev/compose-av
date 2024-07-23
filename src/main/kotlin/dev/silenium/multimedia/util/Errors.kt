package dev.silenium.multimedia.util

class AVException(val operation: String, val error: Int) :
    Exception("FFmpeg error during $operation: ${error.asAVErrorString()}")

class EGLException(val operation: String, val error: Long) :
    Exception("EGL error during $operation: ${error.asEGLErrorString()}")

class GLException(val operation: String, val error: Int) :
    Exception("GL error during $operation: ${error.asGLErrorString()}")

class VAException(val operation: String, val error: Int) :
    Exception("VA error during $operation: ${error.asVAErrorString()}")

fun Int.asAVError(operation: String = "ffmpeg call"): Exception = AVException(operation, this)
fun Int.asAVErrorString(): String = avErrorStringN(this)
fun Long.asEGLError(operation: String = "EGL call"): Exception = EGLException(operation, this)
fun Long.asEGLErrorString(): String = eglErrorStringN(this)
fun Int.asGLError(operation: String = "GL call"): Exception = GLException(operation, this)
fun Int.asGLErrorString(): String = glErrorStringN(this)
fun Int.asVAError(operation: String = "VA call"): Exception = VAException(operation, this)
fun Int.asVAErrorString(): String = vaErrorStringN(this)

private external fun avErrorStringN(error: Int): String
private external fun eglErrorStringN(error: Long): String
private external fun glErrorStringN(error: Int): String
private external fun vaErrorStringN(error: Int): String
