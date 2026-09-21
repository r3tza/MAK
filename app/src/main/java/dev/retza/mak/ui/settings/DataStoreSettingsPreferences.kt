package dev.retza.mak.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val themeKey = stringPreferencesKey("theme_mode")

class DataStoreSettingsPreferences(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferences {
    override val theme: Flow<ThemeMode> = dataStore.data.map { preferences ->
        themeModeFromStored(preferences[themeKey])
    }

    override suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[themeKey] = mode.name }
    }
}
