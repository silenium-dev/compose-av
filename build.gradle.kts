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

repositories {
    maven("https://reposilite.silenium.dev/snapshots")
    maven("https://reposilite.silenium.dev/releases")
    mavenCentral()
    google()
}

val natives: Configuration by configurations.creating

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    natives(project(":native", configuration = "main"))
    implementation(libs.compose.gl)
    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.logging)
    implementation(kotlin("reflect"))
    implementation(project(":os-utils"))

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
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
    "nativeLibName" to "gl-demo.dll",
)
tasks {
    test {
        useJUnitPlatform()
    }

    register<Copy>("generateTemplates") {
        from(templateSrc)
        into(templateDst)
        expand(templateProps)

        inputs.dir(templateSrc)
        inputs.properties(templateProps)
        outputs.dir(templateDst)
    }

    processResources {
        dependsOn(":native:build")
        from(natives) {
            into("natives")
        }
    }

    compileKotlin {
        dependsOn(":native:build", "generateTemplates")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    group = "dev.silenium.compose"
    version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

    publishing {
        repositories {
            maven(System.getenv("REPOSILITE_URL") ?: "https://reposilite.silenium.dev/private") {
                name = "reposilite"
                credentials {
                    username = System.getenv("REPOSILITE_USERNAME") ?: project.findProperty("reposiliteUser") as String? ?: ""
                    password = System.getenv("REPOSILITE_PASSWORD") ?: project.findProperty("reposilitePassword") as String? ?: ""
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
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
