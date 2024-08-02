package dev.silenium.multimedia.core.data

import kotlin.reflect.KClass

interface FFmpegEnum {
    val id: Int
}

private val ffmpegEnumCache by lazy { mutableMapOf<KClass<out FFmpegEnum>, Map<Int, FFmpegEnum>>() }

@Suppress("UNCHECKED_CAST")
fun <E> fromId(id: Int, klass: KClass<E>): E? where E : Enum<E>, E : FFmpegEnum {
    return ffmpegEnumCache.getOrPut(klass) {
        klass.java.enumConstants.associateBy(FFmpegEnum::id)
    }[id] as E?
}

inline fun <reified E> fromIdOrNull(id: Int) where E : Enum<E>, E : FFmpegEnum = fromId(id, E::class)
inline fun <reified E> fromId(id: Int) where E : Enum<E>, E : FFmpegEnum =
    fromId(id, E::class) ?: throw NoSuchElementException("Invalid ${E::class.simpleName} id ${id}")
