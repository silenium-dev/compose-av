package dev.silenium.multimedia.vaapi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VATest : FunSpec({
    test("something") {
        VA.destroySurface(0)
    }
})
