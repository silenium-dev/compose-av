import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform

plugins {
    id("av-natives")
}

val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true

natives {
    libName = rootProject.name
    platform = providers.gradleProperty("deploy.platform")
        .map(Platform.Companion::invoke)
        .orElse(NativePlatform.platform())
}

publishing {
    publications {
        if (deployNative) {
            val platform = natives.platform.get()
            val libName = rootProject.name
            create<MavenPublication>("natives${platform.capitalized}") {
                from(components["java"])
                artifactId = "$libName-natives-$platform"
            }
        }
    }
}
