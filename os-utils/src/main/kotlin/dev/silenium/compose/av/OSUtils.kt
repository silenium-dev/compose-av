package dev.silenium.compose.av

import dev.silenium.multimedia.BuildConstants

object OSUtils {
    private val os = System.getProperty("os.name").lowercase()
    private val arch = System.getProperty("os.arch").lowercase()

    fun isWindows(): Boolean = os.contains("win")
    fun isLinux(): Boolean = os.contains("nix") || os.contains("nux")

    fun libIdentifier(): String { // TODO: Improve
        val name = when {
            os.contains("win") -> "windows"
            os.contains("nix") || os.contains("nux") -> "linux"
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
        val bits = if (arch.contains("64")) "64" else "32"
        val cpuArch = when {
            arch.contains("arm") -> "arm${bits}"
            arch.contains("x86") || arch.contains("amd") -> when(bits) {
                "64" -> "x64"
                "32" -> "x86"
                else -> throw UnsupportedOperationException("Unsupported bits: $bits")
            }
            else -> throw UnsupportedOperationException("Unsupported architecture: $arch")
        }
        return "${name}-${cpuArch}"
    }

    fun libFileName(): String {
        val extension = when {
            isWindows() -> "dll"
            isLinux() -> "so"
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
        return "${BuildConstants.LibBaseName}-${libIdentifier()}.${extension}"
    }
}
