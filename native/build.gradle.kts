import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

val mesonExe = findProperty("meson.executable") as? String ?: "meson"
val targetDir = layout.buildDirectory.dir("meson").get().asFile.apply { mkdirs() }
val generateMakefile = tasks.register<Exec>("generateMakefile") {
    workingDir = layout.projectDirectory.asFile

    doFirst {
        delete(targetDir)
    }

    environment(
        "CFLAGS" to "-fPIC",
        "CXXFLAGS" to "-fPIC",
    )

    commandLine(
        mesonExe, "setup",
        targetDir.absolutePath,
        "--force-fallback-for=ffmpeg,libjpeg,openal,libass,harfbuzz,expat,libplacebo,libpng,zlib",
    )

    inputs.file(layout.projectDirectory.file("meson.build"))
    inputs.file(layout.projectDirectory.file("src/meson.build"))
    outputs.dir(targetDir)
    standardOutput = System.out
}

val compileNative = tasks.register<Exec>("compileNative") {
    commandLine(mesonExe, "compile", "-C", targetDir)
    dependsOn(generateMakefile)

    environment(
        "CFLAGS" to "-fPIC",
        "CXXFLAGS" to "-fPIC",
    )

    standardOutput = System.out
    val fileNameTemplate = NativeLoader.fileNameTemplate(platform)
    outputs.files(layout.buildDirectory.file("meson/src/${fileNameTemplate.format(libName)}"))
    inputs.file(layout.buildDirectory.file("meson/build.ninja"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("meson.build"))
    outputs.cacheIf { true }
}

tasks.processResources {
    dependsOn(compileNative)
    // Required for configuration cache
    val libName = rootProject.name
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
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        jvmTarget = JvmTarget.JVM_17
    }
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        if (deployNative) {
            create<MavenPublication>("natives${platform.capitalized}") {
                from(components["java"])
                artifactId = "$libName-natives-$platform"
            }
        }
    }
}
