package dev.silenium.multimedia.decode

data class Codec(val id: Int, val name: String) {
    override fun toString(): String = name
}
