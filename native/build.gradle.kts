import dev.silenium.compose.av.OSUtils
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    id("dev.silenium.compose.av.os-utils")
    `maven-publish`
    base
}

configurations.create("main")

val artifactSuffix = OSUtils.libIdentifier()

tasks {
    val cmakeExe = findProperty("cmake.executable") as? String ?: "cmake"
    val generateMakefile = register<Exec>("generateMakefile") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
        val additionalFlags = mutableListOf(
            "-DJAVA_HOME=${System.getProperty("java.home")}",
            "-DPROJECT_NAME=${rootProject.name}",
        )
        if (OSUtils.isWindows()) {
            additionalFlags += "-DFFMPEG_PREFIX=\"${findProperty("ffmpeg.prefix") ?: error("ffmpeg.prefix is not set")}\""
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
    val compileNative = register<Exec>("compileNative") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile
        commandLine(cmakeExe, "--build", ".")
        dependsOn(generateMakefile)

        standardOutput = System.out
        if (OSUtils.isWindows()) {
            inputs.files(layout.buildDirectory.files("cmake/*.sln"))
            inputs.files(layout.buildDirectory.files("cmake/*.vcxproj"))
            inputs.files(layout.buildDirectory.files("cmake/*.vcxproj.filters"))
            outputs.files(layout.buildDirectory.file("cmake/Debug/${rootProject.name}.dll"))
        } else if (OSUtils.isLinux()) {
            inputs.file(layout.buildDirectory.file("cmake/build.ninja"))
            outputs.files(layout.buildDirectory.file("cmake/lib${rootProject.name}.so"))
        }
        inputs.dir(layout.projectDirectory.dir("src"))
        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    }

    val jar = register<Jar>("jar") {
        group = "build"
        dependsOn(compileNative)

        from(compileNative.get().outputs.files) {
            into("natives")
            rename {
                val base = it.substringBeforeLast(".")
                val ext = it.substringAfterLast(".")
                "$base-$artifactSuffix.$ext"
            }
        }
        archiveBaseName.set("${rootProject.name}-native-$artifactSuffix")
    }

    build {
        dependsOn(compileNative, jar)
    }
}

artifacts {
    add("main", tasks["compileNative"].outputs.files.first()) {
        name = "${rootProject.name}-native-$artifactSuffix"
    }
}

publishing {
    publications {
        create<MavenPublication>("native${artifactSuffix.split("-").joinToString("") { it.capitalized() }}") {
//            from()
        }
    }
}
