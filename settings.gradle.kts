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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val deployKotlin = if (extra.has("deploy.kotlin")) {
    extra.get("deploy.kotlin")?.toString()?.toBoolean() ?: true
} else true
if (deployKotlin) {
    include(":native")
}

rootProject.name = "compose-av"

includeBuild("build-logic")
