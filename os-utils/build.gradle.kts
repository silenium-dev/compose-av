plugins {
    kotlin("jvm")
}

group = "dev.silenium.compose"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.commons.lang)
}

tasks.test {
    useJUnitPlatform()
}

val templateSrc = layout.projectDirectory.dir("src/main/templates")
val templateDst = layout.buildDirectory.dir("generated/templates")
val templateProps = mapOf(
    "libBaseName" to "compose-av",
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

    compileKotlin {
        dependsOn(generateTemplates)
    }
}

sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}

kotlin {
    jvmToolchain(11)
}
