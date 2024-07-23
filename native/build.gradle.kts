import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.org.jline.utils.OSUtils

plugins {
    base
}

configurations.create("main")

// TODO: publish system specific artifacts (windows x64, linux x64, arm64)

tasks {
    val cmakeExe = findProperty("cmake.executable") as? String ?: "cmake"
    register<Exec>("generateMakefile") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
        val additionalFlags = mutableListOf<String>()
        if (OSUtils.IS_WINDOWS) {
            additionalFlags += "-DFFMPEG_PREFIX=\"${findProperty("ffmpeg.prefix") ?: error("ffmpeg.prefix is not set")}\""
        }
        commandLine(
            cmakeExe,
            "-DJAVA_HOME=${System.getProperty("java.home")}",
            *additionalFlags.toTypedArray(),
            layout.projectDirectory.asFile.absolutePath,
        )

        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
        outputs.dir(workingDir)
        standardOutput = System.out
    }
    register<Exec>("compileNative") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile
        commandLine(cmakeExe, "--build", ".")
        dependsOn("generateMakefile")

        standardOutput = System.out
        if (OSUtils.IS_WINDOWS) {
            inputs.files(layout.buildDirectory.files("cmake/*.sln"))
            inputs.files(layout.buildDirectory.files("cmake/*.vcxproj"))
            inputs.files(layout.buildDirectory.files("cmake/*.vcxproj.filters"))
            outputs.files(layout.buildDirectory.file("cmake/Debug/gl-demo.dll"))
        } else {
            inputs.file(layout.buildDirectory.file("cmake/build.ninja"))
            outputs.files(layout.buildDirectory.file("cmake/libgl-demo.so"))
        }
        inputs.dir(layout.projectDirectory.dir("src"))
        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    }

    build {
        dependsOn("compileNative")
    }
}

artifacts {
    tasks["compileNative"].outputs.files.onEach {
        add("main", it)
    }
}
