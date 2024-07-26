package dev.silenium.compose.av

import org.apache.commons.lang3.ArchUtils

object OSUtils : OSUtilsImpl(System.getProperty("os.name"), System.getProperty("os.arch"))

open class OSUtilsImpl(os: String, arch: String) {
    private val os = os.lowercase()
    private val arch = arch.lowercase()

    fun isWindows(): Boolean = os.contains("win")
    fun isLinux(): Boolean = os.contains("nix") || os.contains("nux")

    fun osArchIdentifier(): String { // TODO: Improve
        val name = when {
            os.contains("win") -> "windows"
            os.contains("nix") || os.contains("nux") -> "linux"
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
        val cpu = ArchUtils.getProcessor(arch)
        val cpuArch = when {
            cpu.isAarch64 -> "aarch64"
            cpu.isX86 && cpu.is32Bit -> "x86"
            cpu.isX86 && cpu.is64Bit -> "x86_64"
            arch.contains("arm") -> arch // TODO: Adapt arm from ffmpeg-natives

            else -> throw UnsupportedOperationException("Unsupported architecture: ${cpu.arch}")
        }
        return "${name}-${cpuArch}"
    }

    fun libFileName(): String {
        val template = when {
            isWindows() -> "%s.dll"
            isLinux() -> "lib%s.so"
            else -> throw UnsupportedOperationException("Unsupported OS: $os")
        }
        return "${osArchIdentifier()}/${template.format(BuildConstants.LibBaseName)}"
    }
}
