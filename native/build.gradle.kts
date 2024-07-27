import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.incremental.createDirectory

buildscript {
    repositories {
        maven("https://reposilite.silenium.dev/releases") {
            name = "silenium-releases"
        }
    }
    dependencies {
        classpath(libs.jni.utils)
    }
}

plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

val libName = rootProject.name
val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true

val withGPL: Boolean = findProperty("ffmpeg.gpl").toString().toBoolean()
val platformExtension = "-gpl".takeIf { withGPL }.orEmpty()
val platformString = findProperty("ffmpeg.platform")?.toString()
val platform = platformString?.let { Platform(it, platformExtension) } ?: NativePlatform.platform(platformExtension)

val cmakeExe = findProperty("cmake.executable") as? String ?: "cmake"
val generateMakefile = tasks.register<Exec>("generateMakefile") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
    val additionalFlags = mutableListOf(
        "-DJAVA_HOME=${System.getProperty("java.home")}",
        "-DPROJECT_NAME=${libName}",
        "-DFFMPEG_PLATFORM=${platform}",
        "-DFFMPEG_PLATFORM_EXTENSION=${platform.extension}",
        "-DFFMPEG_VERSION=${libs.ffmpeg.natives.get().version}",
    )
    commandLine(
        cmakeExe,
        *additionalFlags.toTypedArray(),
        layout.projectDirectory.asFile.absolutePath,
    )

    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.properties(
        "JAVA_HOME" to System.getProperty("java.home"),
        "PROJECT_NAME" to libName,
        "FFMPEG_PLATFORM" to platform,
        "FFMPEG_PLATFORM_EXTENSION" to platform.extension,
        "FFMPEG_VERSION" to libs.ffmpeg.natives.get().version,
    )
    outputs.dir(workingDir)
    standardOutput = System.out
}

val compileNative = tasks.register<Exec>("compileNative") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile
    commandLine(cmakeExe, "--build", ".")
    dependsOn(generateMakefile)

    standardOutput = System.out
    val fileNameTemplate = NativeLoader.fileNameTemplate(platform)
    when (platform.os) {
        Platform.OS.WINDOWS -> {
            outputs.files(layout.buildDirectory.file("cmake/Debug/${fileNameTemplate.format(libName)}"))
        }

        Platform.OS.LINUX, Platform.OS.DARWIN -> {
            outputs.files(layout.buildDirectory.file("cmake/${fileNameTemplate.format(libName)}"))
        }
    }
    inputs.file(layout.buildDirectory.file("cmake/CMakeCache.txt"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    outputs.cacheIf { true }
}

tasks.processResources {
    dependsOn(compileNative)
    // Required for configuration cache
    val platformString = findProperty("ffmpeg.platform")?.toString()
    val withGPL: Boolean = findProperty("ffmpeg.gpl").toString().toBoolean()
    val platformExtension = "-gpl".takeIf { withGPL }.orEmpty()
    val platform = platformString?.let { Platform(it, platformExtension) } ?: NativePlatform.platform(platformExtension)

    from(compileNative.get().outputs.files) {
        rename {
            NativeLoader.libPath(libName, platform = platform)
        }
    }
}

kotlin {
    jvmToolchain(8)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        languageVersion = KotlinVersion.KOTLIN_1_7
    }
}

publishing {
    publications {
        create<MavenPublication>("natives${platform.capitalized}") {
            from(components["java"])
            artifactId = "$libName-natives-$platform"
        }
    }
}
