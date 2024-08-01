package dev.silenium.compose.av.render

import dev.silenium.compose.av.data.Frame
import dev.silenium.compose.av.decode.Decoder
import dev.silenium.compose.gl.surface.RollingWindowStatistics
import kotlinx.datetime.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

abstract class GLRenderInterop<D : Decoder<D>> {
    protected val log: Logger by lazy { LoggerFactory.getLogger(javaClass) }

    private var stats = RollingWindowStatistics()
    private var lastStatsPrint = Clock.System.now()

    abstract val decoder: D
    abstract fun isSupported(frame: Frame): Boolean
    protected abstract fun mapImpl(frame: Frame): Result<GLInteropImage>

    fun map(frame: Frame): Result<GLInteropImage> = runCatching {
        if (!isSupported(frame)) error("Unsupported frame: $frame")
        val start = System.nanoTime()
        mapImpl(frame).getOrThrow().also {
            val end = System.nanoTime()
            stats = stats.add(end, (end - start).nanoseconds)
            val now = Clock.System.now()
            if (now - lastStatsPrint > 1.seconds) {
                log.trace("map time min=${stats.frameTimes.min.floatMs}ms, max=${stats.frameTimes.max.floatMs}ms, avg=${stats.frameTimes.average.floatMs}ms, median=${stats.frameTimes.median.floatMs}ms")
                lastStatsPrint = now
            }
        }
    }
}

private val Duration.floatMs: Float get() = inWholeMicroseconds.toFloat() / 1000.0f
