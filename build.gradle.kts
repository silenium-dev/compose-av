import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.idea.ext)
    `maven-publish`
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false

dependencies {
    implementation(libs.compose.desktop.common)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.annotation)
    implementation(libs.compose.gl)
    implementation(libs.compose.gl.natives)
    implementation(libs.jni.utils)
    implementation(libs.jna)
    implementation(libs.bundles.kotlinx)
    implementation(libs.slf4j.api)  // for logging
    implementation(project(":natives"))
    implementation(kotlin("reflect"))

    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.compose.material.icons.extended)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.logback.classic)
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
        jvmTarget = JvmTarget.JVM_17
    }
}

sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}

java {
    sourceCompatibility = kotlin.compilerOptions.jvmTarget.map { JavaVersion.toVersion(it.target) }.get()
    targetCompatibility = sourceCompatibility

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
            if (deployEnabled) {
                val url = findProperty("deploy.repo-url") as? String ?: error("No deploy.repo-url specified")
                maven(url) {
                    name = "reposilite"
                    credentials {
                        username = findProperty("deploy.username") as? String ?: ""
                        password = findProperty("deploy.password") as? String ?: ""
                    }
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
