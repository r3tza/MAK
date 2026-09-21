package dev.retza.mak.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

private val themeKey = stringPreferencesKey("theme_mode")

private const val readRetryDelayMillis = 100L

class DataStoreSettingsPreferences(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferences {
    override val theme: Flow<ThemeMode> = dataStore.data
        .retryWhen { cause, _ ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(readRetryDelayMillis)
            true
        }
        .map { preferences ->
            themeModeFromStored(preferences[themeKey])
        }

    override suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[themeKey] = mode.name }
    }
}
