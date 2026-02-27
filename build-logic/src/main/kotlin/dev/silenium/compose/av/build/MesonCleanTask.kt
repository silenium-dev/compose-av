package dev.silenium.compose.av.build

import dev.silenium.libs.jni.Platform
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable

abstract class MesonCleanTask : Exec() {
    @get:Input
    abstract val targetDir: Property<File>

    @get:Input
    abstract val platform: Property<Platform>

    @TaskAction
    override fun exec() {
        val platformFile = targetDir.get().resolve("platform.txt")
        if (!platformFile.exists() || platformFile.readText() != platform.get().full) {
            targetDir.get().deleteRecursively() // Completely delete target dir on platform mismatch
            return
        }

        val mesonCommand = arrayOf<Serializable>(
            "nix", "develop", "--ignore-env", "--keep-env-var", "JAVA_HOME", "--command",
            "ninja",
            "-C", targetDir.get().absolutePath,
            "clean",
        )
        commandLine(*mesonCommand)

        super.exec()
    }
}