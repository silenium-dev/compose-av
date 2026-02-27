import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform

plugins {
    id("av-natives")
}

natives {
    libName = "compose-av"
    platform = providers.gradleProperty("native.platform")
        .map(Platform.Companion::invoke)
        .orElse(NativePlatform.platform())
}