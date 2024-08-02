package dev.silenium.multimedia.core.filter

import dev.silenium.multimedia.core.data.Frame
import dev.silenium.multimedia.core.flow.FlowSource
import dev.silenium.multimedia.core.flow.Sink

abstract class SimpleFilter : Sink<Frame>, FlowSource<Frame>() {
    abstract override fun submit(value: Frame): Result<Unit>
    abstract override fun next(): Result<Frame>
}
