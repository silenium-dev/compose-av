package dev.silenium.compose.av.build

import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLogger
import java.io.OutputStream
import java.net.URI
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun downloadFile(progressLogger: ProgressLogger, logger: Logger, uri: URI, outputStream: OutputStream) {
    try {
        val conn = uri.toURL().openConnection()
        conn.connect()
        val size = conn.contentLengthLong
        progressLogger.progress("${size.toMB()} MB")

        var downloaded = 0L
        var lastLog = Clock.System.now()
        val buf = ByteArray(1024)
        conn.inputStream.use { input ->
            while (true) {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException("Download interrupted")
                }
                val read = input.read(buf)
                if (read == -1) break
                outputStream.write(buf, 0, read)

                downloaded += read
                if (downloaded > 0) {
                    progressLogger.progress("${downloaded.toMB()} MB / ${size.toMB()} MB (${downloaded partOf size}%)")
                } else {
                    progressLogger.progress("${size.toMB()} MB")
                }
                val now = Clock.System.now()
                if (now - lastLog > 1.seconds) {
                    lastLog = now
                    logger.lifecycle("${progressLogger.description}: ${downloaded.toMB()} MB / ${size.toMB()} MB (${downloaded partOf size}%)")
                }
            }
        }
        progressLogger.completed()
        logger.lifecycle("${progressLogger.description}: ${downloaded.toMB()} MB / ${size.toMB()} MB (${downloaded partOf size}%)")
    } catch (e: Exception) {
        progressLogger.completed(e.message ?: "Unknown error", true)
    }
}

private fun Long.toMB() = this / 1024 / 1024
private infix fun Long.partOf(full: Long) = (this * 100) / full
