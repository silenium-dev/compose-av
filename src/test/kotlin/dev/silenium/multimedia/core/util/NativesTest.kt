package dev.silenium.multimedia.core.util

import dev.silenium.multimedia.core.mpv.MPV
import dev.silenium.multimedia.core.mpv.Node
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.delay
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.seconds

class NativesTest : FunSpec({
    test("can load natives") {
        shouldNotThrow<UnsatisfiedLinkError> {
            Natives.ensureLoaded()
        }
        val file = run {
            val videoFile = Files.createTempFile("video", ".webm")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").shouldNotBeNull().use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        MPV().use {
            it.setProperty("terminal", Node.String("yes")).getOrThrow()
            it.setPropertyAsync("hwdec", Node.String("auto")).getOrThrow()
            it.setPropertyAsync("msg-level", Node.String("all=info")).getOrThrow()
            it.initialize().getOrThrow()
            it.command("loadfile", file.absolutePathString())
                .getOrThrow()
                .let(::println)
            it.commandAsync("loadfile", file.absolutePathString())
                .getOrThrow()
                .let(::println)
            it.getPropertyLongAsync("playlist-count")
                .getOrThrow()
                .let { println("Playlist count: $it") }
            it.getPropertyNodeAsync("playlist")
                .getOrThrow()
                .let { println("Playlist: $it") }
        }
    }
})
