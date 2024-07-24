# compose-av

A kotlin library for av processing

## Features

### Core

- [ ] Demuxing (🚧)
- [ ] Muxing
- [ ] Video decoding (🚧)
- [ ] Video playback (🚧)
- [ ] Video processing
- [ ] Video encoding
- [ ] Audio decoding
- [ ] Audio playback
- [ ] Audio processing
- [ ] Audio encoding

### Compose

- [ ] Compose video player (🚧)
- [ ] Compose audio player

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
    implementation("dev.silenium.compose:compose-av:0.1.0")
}
```

### Video player

Demuxer is currently very experimental, so it might not work with files that can be played with other players.

```kotlin
@Composable
fun App() {
    val file = remember { Path("path/to/video.mp4") }
    VideoPlayer(file, modifier = Modifier.aspectRatio(16f / 9))
}
```
