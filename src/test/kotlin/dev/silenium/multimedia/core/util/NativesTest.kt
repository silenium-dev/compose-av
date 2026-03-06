package dev.silenium.multimedia.core.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec

class NativesTest : FunSpec({
    test("can load natives") {
        shouldNotThrow<UnsatisfiedLinkError> {
            Natives.ensureLoaded()
        }
    }
})
