package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupAnimationTest {
    @Test
    fun seedHeadAndIconCirclePopInFirst() {
        assertEquals(0f, bloomSeedHeadScale(0f), 0.001f)
        assertEquals(0f, bloomIconCircleScale(0f), 0.001f)
        // The seed head peaks at 70% of its 350 ms, before the first petal starts at 250 ms.
        assertEquals(1.12f, bloomSeedHeadScale(0.7f * 350f), 0.001f)
        assertEquals(1f, bloomSeedHeadScale(350f), 0.001f)
        assertEquals(1f, bloomIconCircleScale(450f), 0.001f)
    }

    @Test
    fun petalsAreHiddenUntilTheyStartAndSettleByTheEnd() {
        assertEquals(0f, bloomPetalPose(elapsedMillis = 0f, index = 0).scale, 0.001f)
        assertEquals(PetalPose(scale = 0.2f, rotation = -25f), bloomPetalPose(elapsedMillis = 250f, index = 0))
        for (index in 0 until 5) {
            assertEquals(SettledPetal, bloomPetalPose(elapsedMillis = INTRO_BLOOM_MILLIS.toFloat(), index = index))
        }
    }

    @Test
    fun laterPetalsStartLater() {
        val first = bloomPetalPose(elapsedMillis = 600f, index = 0)
        val last = bloomPetalPose(elapsedMillis = 600f, index = 4)
        assertTrue(first.scale > 0.2f)
        assertEquals(0f, last.scale, 0.001f)
    }

    @Test
    fun petalOvershootsBeforeSettling() {
        // Petal 0 reaches its peak at 70% of its 800 ms growth, which starts at 250 ms.
        val peak = bloomPetalPose(elapsedMillis = 250f + 0.7f * 800f, index = 0)
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
