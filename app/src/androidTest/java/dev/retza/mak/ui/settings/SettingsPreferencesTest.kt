package dev.retza.mak.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

        scope?.cancel()
        val second = preferences(openDataStore())
        assertEquals(ThemeMode.Dark, second.theme.first())
    }

    @Test
    fun unknownStoredThemeReadsAsSystem() = runBlocking {
        val dataStore = openDataStore()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "neon" }

        assertEquals(ThemeMode.System, DataStoreSettingsPreferences(dataStore).theme.first())
    }

    private fun preferences(dataStore: DataStore<Preferences>): SettingsPreferences =
        DataStoreSettingsPreferences(dataStore)

    private fun openDataStore(): DataStore<Preferences> {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
        scope = dataStoreScope
        return PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
    }
}
