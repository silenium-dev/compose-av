package dev.silenium.compose.av.build

import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.kotlin.dsl.environment
import java.io.File
import java.io.Serializable

abstract class MesonSetupTask : Exec() {
    @get:Input
    abstract val targetDir: Property<File>

    @get:Input
    abstract val javaHome: Property<File>

    @get:Input
    abstract val platform: Property<Platform>

    init {
        inputs.file("meson.build")
        val outputDirs = listOf(
            "meson-info",
        )
        val outputFiles = listOf(
            "build.ninja",
            "platform.txt",
        )
        outputFiles.forEach { file ->
            outputs.file(targetDir.map { it.resolve(file) })
        }
        outputDirs.forEach { dir ->
            outputs.dir(targetDir.map { it.resolve(dir) })
        }
    }

    override fun exec() {
        logger.lifecycle("JAVA_HOME: ${javaHome.get()}")
        environment(
            "JAVA_HOME" to javaHome.get().absolutePath,
        )

        val platformFile = targetDir.get().resolve("platform.txt")
        if (!platformFile.exists() || platformFile.readText() != platform.get().full) {
            targetDir.get().deleteRecursively()
        }

        val crossArgs = arrayOf("--cross-file", "cross/${platform.get().osArch}.txt")

        val mesonCommand = arrayOf<Serializable>(
            "nix", "develop", "--ignore-env", "--keep-env-var", "JAVA_HOME", "--command",
            "meson", "setup",
            "--wipe",
            targetDir.get().absolutePath,
            *crossArgs,
        )
        commandLine(*mesonCommand)

        super.exec()

        platformFile.writeText(platform.get().full)
    }
}
