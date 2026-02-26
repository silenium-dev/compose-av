import org.gradle.kotlin.dsl.mavenCentral
import org.gradle.kotlin.dsl.repositories

pluginManagement {
    repositories {
        maven("https://reposilite.silenium.dev/releases") {
            name = "silenium-releases"
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

val deployNative = if (extra.has("deploy.native")) {
    extra.get("deploy.native")?.toString()?.toBoolean() ?: true
} else true
if (deployNative) {
    include(":native")
}

rootProject.name = "compose-av"

includeBuild("build-logic")
