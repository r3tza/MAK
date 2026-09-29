package dev.retza.mak.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class MakPoppyLogoTest {
    @Test
    fun opacityStaysBetweenDimAndFull() {
        for (step in 0..100) {
            val alpha = waitingPetalAlpha(phase = step / 100f, index = step % POPPY_PETAL_COUNT)
            assertTrue("alpha $alpha out of range", alpha in 0.3f - 0.001f..1f + 0.001f)
        }
    }
}
