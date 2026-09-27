package dev.retza.mak.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupAnimationTest {
    @Test
    fun coldStartWaitsForTheWholeBloomFromTheProcessStart() {
        assertEquals(10_000L + SPLASH_BLOOM_MILLIS, splashBloomEndsAt(processStartUptimeMillis = 10_000L, animationsOn = true))
    }

    @Test
    fun noWaitWhenSystemAnimationsAreOff() {
        assertEquals(10_000L, splashBloomEndsAt(processStartUptimeMillis = 10_000L, animationsOn = false))
    }
}
