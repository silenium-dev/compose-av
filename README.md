# compose-av

A kotlin library for audio and video playback in Jetpack Compose.
It is based on [libmpv](https://github.com/mpv-player/mpv) and
[compose-gl](https://github.com/silenium-dev/compose-gl).
**Disclaimer**: This library is in a very early stage of development, so it might not work as expected.

## Features

- [ ] Compose video player (🚧)
- [ ] Compose audio player

## Platforms

- [x] Linux (x86-64, arm64)
- [x] Windows (x86-64)
- [ ] Android

*Note: macOS/iOS support won't be implemented because of lack of development hardware.
If you want to contribute, feel free to do so.*

## Usage

Gradle:

```kotlin
repositories {
    maven("https://nexus.silenium.dev/repository/maven-releases") {
        name = "silenium-dev-nexus"
    }
}

dependencies {
    implementation("dev.silenium.compose.av:compose-av:0.1.0")
}
```

### Example

See the [Main.kt](src/test/kotlin/dev/silenium/multimedia/compose/Main.kt) in the test sources
on how to use the library.
