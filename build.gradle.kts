import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "dev.silenium.compose"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://reposilite.silenium.dev/snapshots")
    maven("https://reposilite.silenium.dev/releases")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val natives by configurations.creating

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("dev.silenium.compose:compose-gl:0.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    natives(project(":native", configuration = "main"))
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

tasks {
    processResources {
        dependsOn(":native:build")
        from(natives) {
            into("natives")
        }
    }

    compileKotlin {
        dependsOn(":native:build")
    }
}
