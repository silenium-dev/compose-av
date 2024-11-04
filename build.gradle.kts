import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.idea.ext)
    `maven-publish`
}

val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true
val deployKotlin = (findProperty("deploy.kotlin") as String?)?.toBoolean() ?: true
val skikoEGL = (findProperty("skiko.egl") as String?)?.toBoolean() ?: false

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("androidx.annotation:annotation-jvm:1.9.1")
    implementation(libs.compose.gl)
    implementation(libs.compose.gl.natives)
    implementation(libs.jni.utils)
    implementation(libs.jna)
    implementation(libs.bundles.kotlinx)
    implementation(libs.slf4j.api)  // for logging
    if (deployNative) {
        implementation(project(":native"))
    }
    implementation(kotlin("reflect"))
    if (skikoEGL) {
        implementation(libs.bundles.skiko) {
            version {
                strictly(libs.skiko.awt.runtime.linux.x64.get().version!!)
            }
        }
    }

    testImplementation(compose.materialIconsExtended)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.logback.classic)
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gl-demo"
            packageVersion = "1.0.0"
        }
    }
}

val templateSrc = layout.projectDirectory.dir("src/main/templates")
val templateDst = layout.buildDirectory.dir("generated/templates")
val templateProps = mapOf(
    "LIBRARY_NAME" to rootProject.name,
)
tasks {
    test {
        useJUnitPlatform()
    }

    val generateTemplates = register<Copy>("generateTemplates") {
        from(templateSrc)
        into(templateDst)
        expand(templateProps)

        inputs.dir(templateSrc)
        inputs.properties(templateProps)
        outputs.dir(templateDst)
    }

    withType<Jar> {
        dependsOn(generateTemplates)
    }

    compileKotlin {
        dependsOn("generateTemplates")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
    jvmToolchain(11)
}

sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    group = "dev.silenium.compose.av"
    version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

    repositories {
        maven("https://reposilite.silenium.dev/releases")
        mavenCentral()
        google()
    }

    publishing {
        repositories {
            val url = System.getenv("MAVEN_REPO_URL") ?: return@repositories
            maven(url) {
                name = "reposilite"
                credentials {
                    username = System.getenv("MAVEN_REPO_USERNAME") ?: ""
                    password = System.getenv("MAVEN_REPO_PASSWORD") ?: ""
                }
            }
        }
    }
}

publishing {
    publications {
        if (deployKotlin) {
            create<MavenPublication>("main") {
                from(components["java"])
            }
        }
    }
}

rootProject.idea.project {
    this as ExtensionAware
    configure<ProjectSettings> {
        this as ExtensionAware
        configure<TaskTriggersConfig> {
            afterSync("generateTemplates")
        }
    }
}
