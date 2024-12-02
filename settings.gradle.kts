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
