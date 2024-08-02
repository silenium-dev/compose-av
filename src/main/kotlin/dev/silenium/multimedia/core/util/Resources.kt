package dev.silenium.multimedia.core.util

import java.nio.charset.Charset

object Resources {
    fun loadBytesFromClasspath(path: String) = javaClass.classLoader.getResourceAsStream(path)!!.readBytes()
    fun loadTextFromClasspath(path: String, charset: Charset = Charsets.UTF_8) =
        loadBytesFromClasspath(path).toString(charset)
}
