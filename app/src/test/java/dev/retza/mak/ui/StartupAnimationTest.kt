package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupAnimationTest {
    @Test
    fun petalsAreHiddenUntilTheyStartAndSettleByTheEnd() {
        assertEquals(0f, bloomPetalPose(elapsedMillis = 0f, index = 0).scale, 0.001f)
        assertEquals(PetalPose(scale = 0.2f, rotation = -25f), bloomPetalPose(elapsedMillis = 250f, index = 0))
        for (index in 0 until 5) {
            assertEquals(SettledPetal, bloomPetalPose(elapsedMillis = INTRO_BLOOM_MILLIS.toFloat(), index = index))
        }
    }

    @Test
    fun nameUnfoldsFromNothingToFullBeforeTheBloomEnds() {
        assertEquals(0f, nameRevealFraction(0f), 0.001f)
        assertEquals(1f, nameRevealFraction(1_300f), 0.001f)
        assertEquals(1f, nameRevealFraction(INTRO_BLOOM_MILLIS.toFloat()), 0.001f)
        assertTrue(nameRevealFraction(650f) in 0.01f..0.99f)
    }

    @Test
    fun bloomIsClaimedOncePerProcess() {
        // Other tests in this process may have claimed it already, so only the second call is certain.
        StartupBloom.claim()
        assertFalse(StartupBloom.claim())
    }
}
