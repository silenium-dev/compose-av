package dev.silenium.compose.av.build

import dev.silenium.libs.jni.Platform
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.operations.BuildOperationDescriptor
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.org.apache.commons.codec.digest.DigestUtils
import java.net.URI
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

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
        packageCacheDir.convention(project.layout.buildDirectory.dir("packagecache"))

//        outputs.file(platform.map { p -> mpvDlFile.takeIf { p.os == Platform.OS.WINDOWS } })
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
        Files.createSymbolicLink(cacheLink, packageCacheDir.get().asFile.toPath())
    }

    private fun copySubprojects(platform: Platform) {
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
    val uri: URI by lazy { URI.create("https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/20260225/mpv-dev-${arch}-20260225-git-92ed2d2.7z") }
}

internal val mpvWindows = mapOf(
    Platform.Arch.X86 to MpvWindowsMetadata("i686", "c3f25283f7c5ec3eb718fb9a1b686f714744c04559e8aaa2e691bc8e49552c71"),
    Platform.Arch.ARM64 to MpvWindowsMetadata(
        "aarch64",
        "783b60c8ca94eda76596f2b355f860f35dffe6f56aa2e3762f2fc7fd3f7304d2"
    ),
    Platform.Arch.X86_64 to MpvWindowsMetadata(
        "x86_64",
        "5d266a6899b8bb175a6857c93c53679f040953167a2bed65b83edb03e0b48b65"
    ),
)
