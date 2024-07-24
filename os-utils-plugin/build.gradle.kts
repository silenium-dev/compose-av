plugins {
    `kotlin-dsl`
}

group = "dev.silenium.compose"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":os-utils"))

    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("os-utils") {
            id = "dev.silenium.compose.av.os-utils"
            implementationClass = "dev.silenium.compose.av.OSUtilsPlugin"
        }
    }
}
