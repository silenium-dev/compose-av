package dev.silenium.compose.av.build

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

// Never cache as read-only outputs break cache restore
// Caching will instead happen on nix level
abstract class NixBuildTask : Exec() {
    @get:Input
    abstract val libName: Property<String>

    @get:OutputDirectory
    abstract val resultDir: DirectoryProperty

    override fun exec() {
        commandLine("nix", "build", "-o", resultDir.get().asFile.absolutePath, ".#${libName.get()}")
        standardOutput = System.out
        errorOutput = System.out
        super.exec()
    }
}
