package dev.silenium.multimedia.core.mpv

import dev.silenium.multimedia.core.util.Natives

sealed interface Node {
    data class Long(val value: kotlin.Long) : Node
    data class String(val value: kotlin.String) : Node
    data class Flag(val value: kotlin.Boolean) : Node
    data class Double(val value: kotlin.Double) : Node
    data class List(val nodes: kotlin.collections.List<Node>) : Node {
        constructor(elements: Array<Node>) : this(elements.toList())
    }

    data class Map(val entries: kotlin.collections.Map<kotlin.String, Node>) : Node {
        data class Entry(val key: kotlin.String, val value: Node)

        @Suppress("unused")
        constructor(entries: Array<Entry>) : this(entries.associate { it.key to it.value })
    }

    data class ByteArray(val value: kotlin.ByteArray) : Node {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArray

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    data object None : Node
}
