package dev.silenium.compose.av.build

import dev.silenium.libs.jni.Platform
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.org.apache.commons.codec.digest.DigestUtils
import java.net.URI
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.relativeTo

abstract class PrepareSubprojectsTask : DefaultTask() {
    @get:Inject
    abstract val execOps: ExecOperations

    @get:Inject
    abstract val fsOps: FileSystemOperations

    @get:Inject
    abstract val progressLoggerFactory: ProgressLoggerFactory

    @get:Input
    abstract val platform: Property<Platform>

    @get:Internal
    abstract val downloadDir: DirectoryProperty

    @get:Internal
    abstract val mpvDlFile: RegularFileProperty

    @get:OutputDirectory
    abstract val subprojectDir: DirectoryProperty

    @get:Internal
    abstract val packageCacheDir: DirectoryProperty

    @get:InputDirectory
    abstract val subprojectTplDir: DirectoryProperty

    init {
        mpvDlFile.convention(downloadDir.zip(platform) { dir, p -> dir.file("mpv-win-${p.arch}.7z") })
        subprojectDir.convention(project.layout.projectDirectory.dir("subprojects"))
        subprojectTplDir.convention(project.layout.projectDirectory.dir("subprojects.tpl"))
        packageCacheDir.convention(project.rootProject.layout.projectDirectory.dir(".gradle/meson-cache/packagecache"))

        outputs.dir(subprojectDir)
        inputs.dir(subprojectTplDir)
    }

    @TaskAction
    fun prepare() {
        val cacheLink = subprojectDir.get().asFile.toPath().resolve("packagecache")
        cacheLink.deleteIfExists()
        downloadDir.get().asFile.mkdirs()
        subprojectDir.get().asFile.deleteRecursively()
        subprojectDir.get().asFile.mkdirs()
        when (platform.get().os) {
            Platform.OS.WINDOWS -> {
                downloadMpvWindows()
                extractMpvSubproject()
                configureMpvSubproject()
            }

            Platform.OS.LINUX -> {
                copySubprojects(platform.get())
            }

            else -> Unit
        }
        packageCacheDir.get().asFile.mkdirs()
        Files.createSymbolicLink(cacheLink, packageCacheDir.get().asFile.toPath().relativeTo(cacheLink.parent))
    }

    private fun copySubprojects(platform: Platform) {
        // TODO: Template url and hash for linux
        fsOps.copy {
            from(subprojectTplDir.dir(platform.os.toString()))
            into(subprojectDir)
        }
    }

    private fun downloadMpvWindows() {
        val meta = mpvWindows[platform.get().arch] ?: error("Unsupported platform: ${platform.get().arch}")
        val file = mpvDlFile.get().asFile
        if (file.exists() && file.inputStream().use(DigestUtils::sha256Hex) == meta.fileHash) {
            return logger.lifecycle("MPV Windows binaries are up-to-date")
        }

        val progressLogger = progressLoggerFactory.newOperation(PrepareSubprojectsTask::class.java)
        progressLogger.start("Downloading MPV Windows binaries", "")

        file.toPath().createParentDirectories()
        file.outputStream().use { output ->
            downloadFile(progressLogger, logger, meta.uri, output)
        }
    }

    private fun extractMpvSubproject() {
        execOps.exec {
            commandLine(
                "nix-shell", "-p", "p7zip", "--command",
                "7z x -o${subprojectDir.dir("mpv").get().asFile.absolutePath} ${mpvDlFile.get().asFile.absolutePath}",
            )
        }.assertNormalExitValue()
    }

    private fun configureMpvSubproject() {
        fsOps.copy {
            from(subprojectTplDir.dir("${platform.get().os}/packagefiles/mpv"))
            into(subprojectDir.dir("mpv"))
        }
    }
}

internal data class MpvWindowsMetadata(val arch: String, val fileHash: String) {
    val uri: URI by lazy { URI.create("https://repoflow.silenium.dev/api/universal/personal/github-releases/shinchiro/mpv-winbuild-cmake/20260225/mpv-dev-${arch}-20260225-git-92ed2d2.7z") }
}

internal val mpvWindows = mapOf(
    Platform.Arch.ARM64 to MpvWindowsMetadata(
        "aarch64",
        "783b60c8ca94eda76596f2b355f860f35dffe6f56aa2e3762f2fc7fd3f7304d2"
    ),
    Platform.Arch.X86_64 to MpvWindowsMetadata(
        "x86_64",
        "5d266a6899b8bb175a6857c93c53679f040953167a2bed65b83edb03e0b48b65"
    ),
)

internal data class MpvLinuxMetadata(val arch: String, val fileHash: String) {
    val uri: URI by lazy { URI.create("https://repoflow.silenium.dev/api/universal/personal/github-releases/BtbN/FFmpeg-Builds/autobuild-2026-02-25-13-05/ffmpeg-n8.0.1-64-g15504610b0-${arch}-gpl-shared-8.0.tar.xz") }
}

internal val mpvLinux = mapOf(
    Platform.Arch.X86_64 to MpvLinuxMetadata(
        "linux64",
        "bb09d1cad8016c92e3ce323481144f18bef41ffd5de81efa41a1d071dbcbb52f"
    ),
    Platform.Arch.ARM64 to MpvLinuxMetadata(
        "linuxarm64",
        "b4ad73f547a77da7b9d94661962a6d5d312a945f4bcb3f82c790b2730ce2c52f"
    ),
)
