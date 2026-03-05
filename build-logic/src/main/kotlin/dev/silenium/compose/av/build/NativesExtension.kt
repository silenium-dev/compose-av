package dev.silenium.compose.av.build

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface NativesExtension {
    val sourceFiles: ConfigurableFileCollection
    val libName: Property<String>
    val libVersion: Property<String>

    val nixFlake: RegularFileProperty
    val nixFlakeLock: RegularFileProperty
}
