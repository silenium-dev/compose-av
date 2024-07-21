package dev.silenium.multimedia.data

data class PixelFormat(val id: Int) {
    val name: String by lazy { nameN(id) }
    override fun toString() = name
}

private external fun nameN(id: Int): String
