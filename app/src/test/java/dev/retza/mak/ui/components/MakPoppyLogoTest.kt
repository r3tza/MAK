package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MakPoppyLogoTest {
    @Test
    fun waveStartsAtFullOpacityAndDimsHalfAPeriodLater() {
        assertEquals(1f, waitingPetalAlpha(phase = 0f, index = 0), 0.001f)
        assertEquals(0.3f, waitingPetalAlpha(phase = 0.5f, index = 0), 0.001f)
        assertEquals(1f, waitingPetalAlpha(phase = 1f, index = 0), 0.001f)
    }

    @Test
    fun eachPetalLagsTheOneBeforeByAFifthOfThePeriod() {
        val step = 1f / POPPY_PETAL_COUNT
        for (index in 1 until POPPY_PETAL_COUNT) {
            assertEquals(
                waitingPetalAlpha(phase = 0.3f, index = 0),
                waitingPetalAlpha(phase = 0.3f + step * index, index = index),
                0.001f
            )
        }
    }

    @Test
    fun opacityStaysBetweenDimAndFull() {
        for (step in 0..100) {
            val alpha = waitingPetalAlpha(phase = step / 100f, index = step % POPPY_PETAL_COUNT)
            assert(alpha in 0.3f - 0.001f..1f + 0.001f) { "alpha $alpha out of range" }
        }
    }
}
