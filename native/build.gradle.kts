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

val javac = tasks.withType<JavaCompile>().first().javaCompiler.map(JavaCompiler::getExecutablePath)
val javaHome = javac.map { it.asFile.parentFile.parentFile.absolutePath }
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
        "JAVA_HOME" to javaHome.get(),
    )
    logger.lifecycle("JAVA_HOME: ${javaHome.get()}")

    commandLine(
        mesonExe, "setup",
        targetDir.absolutePath,
        "-Dwrap_mode=forcefallback",
        "-Ddefault_library=static",
        "-Dprefer_static=true",
        "-Dmpv:drm=enabled",
        "-Dmpv:openal=enabled",
        "-Dmpv:vulkan=disabled",
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
        "JAVA_HOME" to javaHome.get(),
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
        jvmTarget = JvmTarget.JVM_11
    }
}

java {
    sourceCompatibility = kotlin.compilerOptions.jvmTarget.map { JavaVersion.toVersion(it.target) }.get()
    targetCompatibility = sourceCompatibility

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
