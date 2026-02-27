package dev.silenium.compose.av.build

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileFactory
import org.gradle.api.internal.file.FilePropertyFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.environment
import java.io.File
import javax.inject.Inject

abstract class MesonCompileTask : Exec() {
    @get:Inject
    abstract val fileFactory: FileFactory

    @get:Input
    abstract val targetDir: Property<File>

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val javaHome: Property<File>

    @get:Input
    abstract val platform: Property<Platform>

    @get:Input
    abstract val libName: Property<String>

    @get:OutputFile
    abstract val libPath: RegularFileProperty

    init {
        libPath.convention(
            targetDir.zip(
                platform.zip(libName) { platform, libName ->
                    val fileNameTemplate = NativeLoader.fileNameTemplate(platform)
                    "src/${fileNameTemplate.format(libName)}"
                }
            ) { targetDir, path -> targetDir.resolve(path) }
                .map(fileFactory::file))
        inputs.file(targetDir.map { it.resolve("build.ninja") })
        outputs.file(libPath)
    }

    override fun exec() {
        environment(
            "JAVA_HOME" to javaHome.get().absolutePath,
        )
        commandLine(
            "nix", "develop", ".#${platform.get().osArch}", "--ignore-env", "--keep-env-var", "JAVA_HOME", "--command",
            "meson", "compile", "-C", targetDir.get().absolutePath, libName.get(),
        )
        super.exec()
    }
}
