pluginManagement {
    repositories {
        maven("https://nexus.silenium.dev/repository/maven-releases") {
            name = "nexus"
        }
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.3.2"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { false }
    }
}

rootProject.name = "compose-av"

include(":natives")
