package dev.silenium.multimedia.core.util

import dev.silenium.multimedia.core.data.Frame
import org.jetbrains.annotations.Blocking
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO

enum class Mode {
    GREY, RG, RGB, RGB0
}

@Blocking
fun Frame.savePNG(plane: Int, path: Path, mode: Mode) {
    if (isHW) {
        transferToSW().getOrThrow().savePNG(plane, path, mode)
    } else {
        val buffer = buf[plane] ?: return
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        BufferedImage(
            width, height, when (mode) {
                Mode.GREY -> BufferedImage.TYPE_BYTE_GRAY
                Mode.RG -> BufferedImage.TYPE_3BYTE_BGR
                Mode.RGB -> BufferedImage.TYPE_3BYTE_BGR
                Mode.RGB0 -> BufferedImage.TYPE_4BYTE_ABGR
            }
        ).apply {
            repeat(height) { y ->
                repeat(width) { x ->
                    val index = y * pitch[plane] + x * when (mode) {
                        Mode.GREY -> 1
                        Mode.RG -> 2
                        Mode.RGB -> 3
                        Mode.RGB0 -> 4
                    }
                    val value = when (mode) {
                        Mode.GREY -> data[index].toInt() and 0xFF
                        Mode.RG -> {
                            val r = data[index].toInt() and 0xFF
                            val g = data[index + 1].toInt() and 0xFF
                            (r shl 16) or (g shl 8)
                        }

                        Mode.RGB -> {
                            val r = data[index].toInt() and 0xFF
                            val g = data[index + 1].toInt() and 0xFF
                            val b = data[index + 2].toInt() and 0xFF
                            (r shl 16) or (g shl 8) or b
                        }

                        Mode.RGB0 -> {
                            val r = data[index].toInt() and 0xFF
                            val g = data[index + 1].toInt() and 0xFF
                            val b = data[index + 2].toInt() and 0xFF
                            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    setRGB(x, y, value)
                }
            }
            ImageIO.write(this, "png", path.toFile())
        }
    }
}
