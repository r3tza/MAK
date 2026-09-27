package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupAnimationTest {
    @Test
    fun petalsStartSmallAndTurnedAndSettleByTheEnd() {
        assertEquals(PetalPose(scale = 0.2f, rotation = -25f), bloomPetalPose(elapsedMillis = 0f, index = 0))
        for (index in 0 until 5) {
            assertEquals(SettledPetal, bloomPetalPose(elapsedMillis = INTRO_BLOOM_MILLIS.toFloat(), index = index))
        }
    }

    @Test
    fun laterPetalsStartLater() {
        val first = bloomPetalPose(elapsedMillis = 400f, index = 0)
        val last = bloomPetalPose(elapsedMillis = 400f, index = 4)
        assertTrue(first.scale > last.scale)
        assertEquals(PetalPose(scale = 0.2f, rotation = -25f), last)
    }

    @Test
    fun petalOvershootsBeforeSettling() {
        // Petal 0 reaches its peak at 70% of its 900 ms growth, which starts at 100 ms.
        val peak = bloomPetalPose(elapsedMillis = 100f + 0.7f * 900f, index = 0)
        assertEquals(1.04f, peak.scale, 0.001f)
        assertEquals(2f, peak.rotation, 0.001f)
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
