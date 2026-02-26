package dev.silenium.compose.av.build

import dev.silenium.libs.jni.Platform
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface NativesExtension {
    val platform: Property<Platform>
    val libName: Property<String>
}
