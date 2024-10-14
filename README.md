# compose-av

A kotlin library for audio and video playback in Jetpack Compose.
It is based on [libmpv](https://github.com/mpv-player/mpv) and
[compose-gl](https://github.com/silenium-dev/compose-gl).
**Disclaimer**: This library is in a very early stage of development, so it might not work as expected.

## Features

- [ ] Compose video player (🚧)
- [ ] Compose audio player

## Platforms

- [x] Linux
- [ ] Windows
- [ ] Android

*Note: macOS/iOS support won't be implemented because of lack of development hardware.
If you want to contribute, feel free to do so.*

## Usage

Gradle:

```kotlin
repositories {
    maven {
        name = "silenium-dev releases"
        url = uri("https://reposilite.silenium.dev/releases")
    }
}

dependencies {
    implementation("dev.silenium.compose.av:compose-av:0.1.0")
}
```

The library currently requires the `libmpv` shared library to be installed on the system.
On Linux, you can install it using your package manager, e.g. on Ubuntu:

```shell
sudo apt install libmpv2
```

### Example

See the [Main.kt](src/test/kotlin/dev/silenium/multimedia/compose/Main.kt) in the test sources
on how to use the library.
