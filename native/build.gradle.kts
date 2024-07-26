import dev.silenium.compose.av.BuildConstants
import dev.silenium.compose.av.OSUtils
import dev.silenium.compose.av.OSUtilsImpl
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    id("dev.silenium.compose.av.os-utils")
    `maven-publish`
    base
}

configurations.create("main")

val platform = OSUtils.osArchIdentifier()
val libName = BuildConstants.LibBaseName
val useFfmpegGpl = findProperty("ffmpeg.gpl")?.toString()?.toBoolean() ?: true
val platformExtension = when {
    useFfmpegGpl -> "-gpl"
    else -> ""
}

val cmakeExe = findProperty("cmake.executable") as? String ?: "cmake"
val generateMakefile = tasks.register<Exec>("generateMakefile") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
    val additionalFlags = mutableListOf(
        "-DJAVA_HOME=${System.getProperty("java.home")}",
        "-DPROJECT_NAME=${libName}",
        "-DFFMPEG_PLATFORM=${platform}${platformExtension}", // TODO: Detect platform
        "-DFFMPEG_VERSION=${libs.ffmpeg.natives.get().version}"
    )
    if (OSUtils.isWindows()) {
        additionalFlags += "-DFFMPEG_PREFIX=\"${findProperty("ffmpeg.prefix") ?: error("ffmpeg.prefix is not set")}\""
    } else if (OSUtils.isLinux()) {
//        additionalFlags += "-GNinja"
    }
    commandLine(
        cmakeExe,
        *additionalFlags.toTypedArray(),
        layout.projectDirectory.asFile.absolutePath,
    )

    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    outputs.dir(workingDir)
    standardOutput = System.out
}

val compileNative = tasks.register<Exec>("compileNative") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile
    commandLine(cmakeExe, "--build", ".")
    dependsOn(generateMakefile)

    standardOutput = System.out
    if (OSUtils.isWindows()) {
        inputs.files(layout.buildDirectory.files("cmake/*.sln"))
        inputs.files(layout.buildDirectory.files("cmake/*.vcxproj"))
        inputs.files(layout.buildDirectory.files("cmake/*.vcxproj.filters"))
        outputs.files(layout.buildDirectory.file("cmake/Debug/${libName}.dll"))
    } else if (OSUtils.isLinux()) {
        inputs.file(layout.buildDirectory.file("cmake/CMakeCache.txt"))
        outputs.files(layout.buildDirectory.file("cmake/lib${libName}.so"))
    }
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
}

val jar = tasks.register<Jar>("jar") {
    group = "build"
    dependsOn(compileNative)
    // Required for configuration cache
    val osUtils = OSUtilsImpl(providers.systemProperty("os.name").get(), providers.systemProperty("os.arch").get())

    from(compileNative.get().outputs.files) {
        into("natives")
        rename {
            osUtils.libFileName()
        }
    }
    archiveBaseName.set("${libName}-natives-${platform}${platformExtension}")
}

tasks.build {
    dependsOn(compileNative, jar)
}

artifacts {
    add("main", jar) {
        name = "${libName}-natives-${platform}${platformExtension}"
    }
}

publishing {
    publications {
        create<MavenPublication>(
            "natives${
                (platform + platformExtension).split("-").joinToString("") { it.capitalized() }
            }"
        ) {
            artifact(jar)
            artifactId = "${libName}-natives-${platform}${platformExtension}"
        }
    }
}
