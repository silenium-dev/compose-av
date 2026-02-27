package dev.silenium.compose.av.build

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.NativePlatform
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

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
        ext.platform.convention(target.provider(NativePlatform::platform))
        val tmpDir = target.layout.buildDirectory.dir("tmp/natives")
        val targetDir = target.layout.buildDirectory.dir("natives")

        val mesonClean = target.tasks.register<MesonCleanTask>("mesonClean") {
            this.targetDir.set(targetDir.map(Directory::getAsFile))
            this.platform.set(ext.platform)
        }
        target.tasks.named("clean") {
            dependsOn(mesonClean)
        }

        val prepareSubprojects = target.tasks.register<PrepareSubprojectsTask>("prepareSubprojects") {
            mustRunAfter(mesonClean)

            doFirst {
                tmpDir.get().asFile.deleteRecursively()
                tmpDir.get().asFile.mkdirs()
            }
            downloadDir.set(tmpDir)
            platform.set(ext.platform)
        }

        val javac = target.tasks.withType<JavaCompile>().first().javaCompiler.map(JavaCompiler::getExecutablePath)
        val javaHome = javac.map { it.asFile.parentFile.parentFile }
        val mesonSetup = target.tasks.register<MesonSetupTask>("mesonSetup") {
            dependsOn(prepareSubprojects)
            mustRunAfter(mesonClean)

            this.targetDir.set(targetDir.map(Directory::getAsFile))
            this.platform.set(ext.platform)
            this.javaHome.set(javaHome)

            inputs.dir(target.layout.projectDirectory.dir("subprojects.tpl"))
        }

        val mesonCompile = target.tasks.register<MesonCompileTask>("mesonCompile") {
            dependsOn(mesonSetup)
            mustRunAfter(mesonClean)

            this.targetDir.set(targetDir.map(Directory::getAsFile))
            this.platform.set(ext.platform)
            this.libName.set(ext.libName)
            this.javaHome.set(javaHome)
            this.sourceDir.set(target.layout.projectDirectory.dir("src"))
        }

        target.afterEvaluate {
            tasks.named<ProcessResources>("processResources") {
                from(mesonCompile.map { it.outputs.files }) {
                    rename {
                        NativeLoader.libPath(ext.libName.get(), platform = ext.platform.get())
                    }
                }
            }
        }
    }
}
