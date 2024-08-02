package dev.silenium.multimedia.core.flow

interface Sink<T> {
    fun submit(value: T): Result<Unit>
}
