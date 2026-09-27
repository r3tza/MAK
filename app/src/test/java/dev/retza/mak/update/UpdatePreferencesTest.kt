package dev.retza.mak.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePreferencesTest {
    @Test
    fun automaticCheckRequiresOptInAndTwentyFourHours() {
        val day = 24L * 60L * 60L * 1000L
        assertFalse(shouldCheckAutomatically(UpdatePreferenceState(), day))
        assertTrue(shouldCheckAutomatically(UpdatePreferenceState(automaticChecks = true), day))
        assertFalse(shouldCheckAutomatically(UpdatePreferenceState(true, day), day + day - 1))
        assertTrue(shouldCheckAutomatically(UpdatePreferenceState(true, day), day + day))
    }

    @Test
    fun clockMovingBackAllowsOneRecoveryCheck() {
        assertTrue(shouldCheckAutomatically(UpdatePreferenceState(true, 2_000), 1_000))
        assertFalse(shouldCheckAutomatically(UpdatePreferenceState(true, 1_000), 1_000))
    }
}
