plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    maven("https://reposilite.silenium.dev/releases") {
        name = "silenium-releases"
    }
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    api(libs.bundles.gradle.plugins)
    libs.bundles.kotlin.plugins.get().forEach {
        api(variantOf(provider { it }) {
            classifier("gradle813")
        })
    }
    api(libs.jni.utils)
}

gradlePlugin {
    plugins {
        register("natives") {
            id = "av-natives"
            implementationClass = "dev.silenium.compose.av.build.NativesPlugin"
        }
    }
}
