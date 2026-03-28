package dev.silenium.multimedia.core.util

import androidx.compose.runtime.remember
import dev.silenium.multimedia.core.mpv.MPV
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream

class NativesTest : FunSpec({
    test("can load natives") {
        shouldNotThrow<UnsatisfiedLinkError> {
            Natives.ensureLoaded()
        }
        val file = run {
            val videoFile = Files.createTempFile("video", ".webm")
            Thread.currentThread().contextClassLoader.getResourceAsStream("1080p.webm").use {
                videoFile.outputStream().use(it::copyTo)
            }
            videoFile.apply { toFile().deleteOnExit() }
        }
        MPV().use {
            it.setOption("terminal", "yes").getOrThrow()
            it.setOption("hwdec", "no").getOrThrow()
            it.setOption("msg-level", "all=info").getOrThrow()
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
