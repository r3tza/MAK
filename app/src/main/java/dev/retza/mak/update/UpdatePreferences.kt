package dev.retza.mak.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

data class UpdatePreferenceState(
    val automaticChecks: Boolean = false,
    val lastAutomaticCheckMillis: Long? = null,
    val dismissedVersionCode: Long? = null
)

interface UpdatePreferences {
    val state: Flow<UpdatePreferenceState>
    suspend fun setAutomaticChecks(enabled: Boolean)
    suspend fun recordAutomaticCheck(timeMillis: Long)
    suspend fun dismiss(versionCode: Long)
}

@Single(binds = [UpdatePreferences::class])
class DataStoreUpdatePreferences(private val dataStore: DataStore<Preferences>) : UpdatePreferences {
    override val state = dataStore.data.map { values ->
        UpdatePreferenceState(
            automaticChecks = values[AUTOMATIC] ?: false,
            lastAutomaticCheckMillis = values[LAST_CHECK],
            dismissedVersionCode = values[DISMISSED]
        )
    }

    override suspend fun setAutomaticChecks(enabled: Boolean) { dataStore.edit { it[AUTOMATIC] = enabled } }
    override suspend fun recordAutomaticCheck(timeMillis: Long) { dataStore.edit { it[LAST_CHECK] = timeMillis } }
    override suspend fun dismiss(versionCode: Long) { dataStore.edit { it[DISMISSED] = versionCode } }

    private companion object {
        val AUTOMATIC = booleanPreferencesKey("update_automatic_checks")
        val LAST_CHECK = longPreferencesKey("update_last_automatic_check")
        val DISMISSED = longPreferencesKey("update_dismissed_version")
    }
}

fun shouldCheckAutomatically(state: UpdatePreferenceState, nowMillis: Long): Boolean {
    if (!state.automaticChecks) return false
    val previous = state.lastAutomaticCheckMillis ?: return true
    return nowMillis < previous || nowMillis - previous >= 24L * 60L * 60L * 1000L
}
