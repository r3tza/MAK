package dev.retza.mak.ui.settings

import dev.retza.mak.settings.DataStoreSettingsPreferences
import dev.retza.mak.settings.ThemeMode
import dev.retza.mak.settings.SettingsPreferences
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPreferencesTest {
    private val preferencesFile = TestPreferencesFile(ApplicationProvider.getApplicationContext<Context>())

    @After
    fun tearDown() = preferencesFile.delete()

    @Test
    fun themeRoundTripsAcrossInstances() = runBlocking {
        val first = preferences(openDataStore())
        first.setTheme(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, first.theme.first())

        preferencesFile.close()
        val second = preferences(openDataStore())
        assertEquals(ThemeMode.Dark, second.theme.first())
    }

    @Test
    fun otherKeyDoesNotReemitTheme() = runBlocking {
        val preferences = preferences(openDataStore())
        val themes = mutableListOf<ThemeMode>()
        val collector = launch(Dispatchers.Default) { preferences.theme.collect { themes.add(it) } }
        try {
            preferences.setGapThresholdMinutes(45)
            preferences.planDisplay.first { it.gapThresholdMinutes == 45 }
            delay(200)
        } finally {
            collector.cancelAndJoin()
        }
        assertEquals(1, themes.size)
    }

    @Test
    fun minimumBreakDefaultsToZeroAndClampsToTwenty() = runBlocking {
        val preferences = preferences(openDataStore())
        assertEquals(0, preferences.planDisplay.first().minimumBreakMinutes)

        preferences.setMinimumBreakMinutes(10)
        assertEquals(10, preferences.planDisplay.first().minimumBreakMinutes)

        preferences.setMinimumBreakMinutes(40)
        assertEquals(20, preferences.planDisplay.first().minimumBreakMinutes)
    }

    @Test
    fun hiddenStudyProgramsAreSavedClearedAndPrunedToExistingOnes() = runBlocking {
        val preferences = preferences(openDataStore())
        assertEquals(emptySet<Long>(), preferences.planDisplay.first().hiddenProgramIds)

        preferences.setStudyProgramHidden(1L, hidden = true)
        preferences.setStudyProgramHidden(2L, hidden = true)
        preferences.setStudyProgramHidden(2L, hidden = false)
        preferences.setStudyProgramHidden(3L, hidden = true)
        assertEquals(setOf(1L, 3L), preferences.planDisplay.first().hiddenProgramIds)

        preferences.retainHiddenStudyPrograms(setOf(1L, 2L))
        assertEquals(setOf(1L), preferences.planDisplay.first().hiddenProgramIds)

        preferences.clearHiddenStudyPrograms()
        assertEquals(emptySet<Long>(), preferences.planDisplay.first().hiddenProgramIds)
    }

    @Test
    fun storedGapThresholdIsClampedToTwentyAndKeptAboveIt() = runBlocking {
        val preferences = preferences(openDataStore())
        preferences.setGapThresholdMinutes(15)
        assertEquals(20, preferences.planDisplay.first().gapThresholdMinutes)

        preferences.setGapThresholdMinutes(45)
        assertEquals(45, preferences.planDisplay.first().gapThresholdMinutes)
    }

    @Test
    fun turningMainOnEnablesAllKinds() = runBlocking {
        val preferences = preferences(openDataStore())

        preferences.setCollisionNotificationsEnabled(true)

        assertNotifications(preferences, enabled = true, evening = true, beforeClass = true)
    }

    @Test
    fun turningMainOffDisablesAllKinds() = runBlocking {
        val preferences = preferences(openDataStore())
        preferences.setCollisionNotificationsEnabled(true)

        preferences.setCollisionNotificationsEnabled(false)

        assertNotifications(preferences, enabled = false, evening = false, beforeClass = false)
    }

    @Test
    fun turningOffLastKindTurnsMainOff() = runBlocking {
        val preferences = preferences(openDataStore())
        preferences.setCollisionNotificationsEnabled(true)
        preferences.setGapEveningNotificationsEnabled(false)
        preferences.setGapBeforeClassNotificationsEnabled(false)

        preferences.setEveningNotificationsEnabled(false)
        assertNotifications(preferences, enabled = true, evening = false, beforeClass = true)

        preferences.setBeforeClassNotificationsEnabled(false)
        assertNotifications(preferences, enabled = false, evening = false, beforeClass = false)
    }

    @Test
    fun turningOnOneKindTurnsMainOnAndLeavesOtherOff() = runBlocking {
        val preferences = preferences(openDataStore())
        assertNotifications(preferences, enabled = false, evening = false, beforeClass = false)

        preferences.setEveningNotificationsEnabled(true)

        assertNotifications(preferences, enabled = true, evening = true, beforeClass = false)
    }

    @Test
    fun legacyDisabledFunctionReadsAsAllKindsOff() = runBlocking {
        // Written straight into the store: the old version stored only the main switch as false.
        // The kind keys are missing, so their defaults (true) must not switch them on.
        val dataStore = openDataStore()
        dataStore.edit { it[booleanPreferencesKey("collision_notifications_enabled")] = false }

        assertNotifications(preferences(dataStore), enabled = false, evening = false, beforeClass = false)
    }

    @Test
    fun mainSwitchCoversGapKinds() = runBlocking {
        val preferences = preferences(openDataStore())

        preferences.setCollisionNotificationsEnabled(true)
        assertGapNotifications(preferences, gapEvening = true, gapBeforeClass = true)

        preferences.setEveningNotificationsEnabled(false)
        preferences.setBeforeClassNotificationsEnabled(false)
        preferences.setGapEveningNotificationsEnabled(false)
        assertNotifications(preferences, enabled = true, evening = false, beforeClass = false)
        assertGapNotifications(preferences, gapEvening = false, gapBeforeClass = true)

        preferences.setGapBeforeClassNotificationsEnabled(false)
        assertNotifications(preferences, enabled = false, evening = false, beforeClass = false)
        assertGapNotifications(preferences, gapEvening = false, gapBeforeClass = false)
    }

    @Test
    fun gapKindsAreOffAfterUpdate() = runBlocking {
        // Written straight into the store: the update stored only the main switch, so the gap kinds are missing.
        val dataStore = openDataStore()
        dataStore.edit { it[booleanPreferencesKey("collision_notifications_enabled")] = true }

        val preferences = preferences(dataStore)
        assertNotifications(preferences, enabled = true, evening = true, beforeClass = true)
        assertGapNotifications(preferences, gapEvening = false, gapBeforeClass = false)
    }

    private suspend fun assertNotifications(
        preferences: SettingsPreferences,
        enabled: Boolean,
        evening: Boolean,
        beforeClass: Boolean
    ) {
        val stored = preferences.collisionNotifications.first()
        assertEquals(enabled, stored.enabled)
        assertEquals(evening, stored.eveningEnabled)
        assertEquals(beforeClass, stored.beforeClassEnabled)
    }

    private suspend fun assertGapNotifications(
        preferences: SettingsPreferences,
        gapEvening: Boolean,
        gapBeforeClass: Boolean
    ) {
        val stored = preferences.collisionNotifications.first()
        assertEquals(gapEvening, stored.gapEveningEnabled)
        assertEquals(gapBeforeClass, stored.gapBeforeClassEnabled)
    }

    @Test
    fun readErrorFallsBackToEmptyPreferences() = runBlocking {
        val preferences = DataStoreSettingsPreferences(ThrowingDataStore(IOException("read failed")))

        assertEquals(ThemeMode.System, preferences.theme.first())
    }

    @Test
    fun nonIoReadErrorIsPropagated() = runBlocking {
        val preferences =
            DataStoreSettingsPreferences(ThrowingDataStore(IllegalStateException("read failed")))

        try {
            preferences.theme.first()
            fail("Expected the read error to be propagated")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun readRecoversAfterTransientIOException() = runBlocking {
        val preferences = DataStoreSettingsPreferences(RecoveringDataStore())
        val last = MutableStateFlow<ThemeMode?>(null)
        val job = launch(Dispatchers.Default) {
            preferences.theme.collect { last.value = it }
        }

        withTimeout(5_000) { while (last.value != ThemeMode.System) delay(10) }
        preferences.setTheme(ThemeMode.Dark)
        withTimeout(5_000) { while (last.value != ThemeMode.Dark) delay(10) }

        job.cancelAndJoin()
        assertEquals(ThemeMode.Dark, last.value)
    }

    private fun preferences(dataStore: DataStore<Preferences>): SettingsPreferences =
        DataStoreSettingsPreferences(dataStore)

    private fun openDataStore(): DataStore<Preferences> = preferencesFile.open()
}

private class ThrowingDataStore(private val error: Throwable) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw error }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw error
}

private class RecoveringDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val firstReadFailed = AtomicBoolean(false)

    override val data: Flow<Preferences> = flow {
        if (firstReadFailed.compareAndSet(false, true)) {
            throw IOException("transient read failure")
        }
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
