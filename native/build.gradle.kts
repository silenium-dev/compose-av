import dev.silenium.libs.jni.NativePlatform

plugins {
    id("av-natives")
}

natives {
    libName = "compose-av"
    platform = NativePlatform.platform()
}