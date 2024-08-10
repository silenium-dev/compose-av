package dev.silenium.multimedia.core.demux

import dev.silenium.multimedia.core.data.NativeCleanable
import dev.silenium.multimedia.core.data.asNativePointer
import java.net.URL
import java.nio.file.Path

class FileDemuxer(url: URL) :
    Demuxer(initializeNativeContextN(url.toString()).getOrThrow().asNativePointer(::releaseNativeContextN)),
    NativeCleanable {
    constructor(path: Path) : this(path.toUri().toURL())
}

private external fun initializeNativeContextN(url: String): Result<Long>
private external fun releaseNativeContextN(nativeContext: Long)
