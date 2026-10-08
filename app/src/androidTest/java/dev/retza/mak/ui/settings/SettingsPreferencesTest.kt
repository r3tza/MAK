package dev.retza.mak.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPreferencesTest {
    private lateinit var context: Context
    private lateinit var file: File
    private var scope: CoroutineScope? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        file = File(context.filesDir, "settings-${System.nanoTime()}.preferences_pb")
    }

    @After
    fun tearDown() {
        scope?.cancel()
        file.delete()
    }

    @Test
    fun themeRoundTripsAcrossInstances() = runBlocking {
        val first = preferences(openDataStore())
        first.setTheme(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, first.theme.first())

        // DataStore releases the file only after its scope completes, so wait for it.
        scope?.coroutineContext?.job?.cancelAndJoin()
        val second = preferences(openDataStore())
        assertEquals(ThemeMode.Dark, second.theme.first())
    }

    @Test
    fun minimumBreakDefaultsToZeroAndClampsToFifteen() = runBlocking {
        val preferences = preferences(openDataStore())
        assertEquals(0, preferences.planDisplay.first().minimumBreakMinutes)

        preferences.setMinimumBreakMinutes(10)
        assertEquals(10, preferences.planDisplay.first().minimumBreakMinutes)

        preferences.setMinimumBreakMinutes(40)
        assertEquals(15, preferences.planDisplay.first().minimumBreakMinutes)
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

    private fun openDataStore(): DataStore<Preferences> {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
        scope = dataStoreScope
        return PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
    }
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
