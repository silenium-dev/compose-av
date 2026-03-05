package dev.silenium.compose.av.build

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

@Suppress("unused") // used as plugin entrypoint
class NativesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<KotlinPluginWrapper>()
        target.configure<KotlinJvmExtension> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        }
        target.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        val ext = target.extensions.create("natives", NativesExtension::class.java)
        ext.libVersion.convention(target.version.toString())
        ext.libName.convention(target.name)
        ext.nixFlakeLock.convention(target.layout.file(ext.nixFlake.map { it.asFile.resolveSibling("flake.lock") }))
        val nixResultDir = target.layout.buildDirectory.dir("nix-result")

        val nixClean = target.tasks.register<Delete>("nixClean") {
            delete(nixResultDir)
        }
        target.tasks.named("clean").configure {
            dependsOn(nixClean)
        }

        val nixBuild = target.tasks.register<NixBuildTask>("nixBuild") {
            doFirst {
                nixResultDir.get().asFile.deleteRecursively()
            }
            group = "build"
            inputs.files(ext.sourceFiles)
            inputs.files(ext.nixFlake, ext.nixFlakeLock)
            libName.set(ext.libName)
            resultDir.set(nixResultDir)
        }

        target.afterEvaluate {
            tasks.named<ProcessResources>("processResources") {
                val out = nixBuild.flatMap { it.resultDir.asFile }
                val targets = mapOf(
                    Platform(Platform.OS.LINUX, Platform.Arch.X86_64) to "x86_64-linux",
                    Platform(Platform.OS.LINUX, Platform.Arch.ARM64) to "aarch64-linux",
                    Platform(Platform.OS.WINDOWS, Platform.Arch.X86_64) to "x86_64-windows",
                )
                targets.forEach { (platform, target) ->
                    val src = out.map { out ->
                        out.resolve("${ext.libName.get()}-${target}-${ext.libVersion.get()}").resolve("lib")
                    }
                    val platformFormat = NativeLoader.fileNameTemplate(platform)
                    from(src) {
                        include("*.so")
                        include("*.dll")
                        include("*.dylib")
                        rename {
                            val matches = platformFormat
                                .replace(".", "\\.")
                                .replace("%s", "(.*)")
                                .toRegex()
                                .find(it) ?: error("library name does not match platform pattern: $it")

                            NativeLoader.libPath(matches.groupValues[1], platform = platform)
                        }
                    }
                }
            }
        }
    }
}
