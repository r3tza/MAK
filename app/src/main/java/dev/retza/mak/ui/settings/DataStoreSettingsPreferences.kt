package dev.retza.mak.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.retza.mak.domain.PlanDisplaySettings
import java.io.IOException
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

private val themeKey = stringPreferencesKey("theme_mode")
private val notificationsEnabledKey = booleanPreferencesKey("collision_notifications_enabled")
private val eveningEnabledKey = booleanPreferencesKey("evening_notifications_enabled")
private val beforeClassEnabledKey = booleanPreferencesKey("before_class_notifications_enabled")
private val eveningHourKey = intPreferencesKey("evening_hour_minutes")
private val leadMinutesKey = intPreferencesKey("before_class_lead_minutes")
private val gapThresholdMinutesKey = intPreferencesKey("gap_threshold_minutes")
private val minimumBreakMinutesKey = intPreferencesKey("minimum_break_minutes")

private const val readRetryDelayMillis = 100L
private const val defaultEveningHourMinutes = 20 * 60
private const val defaultLeadMinutes = 30
private const val defaultGapThresholdMinutes = 30
// A gap is longer than the threshold and a collision break is not longer than the minimum break, so
// keeping the smallest threshold at least the largest minimum break means no break is both
// (decision of 2026-10-09). A stored threshold of 15 minutes is read as 20.
private const val minGapThresholdMinutes = 20
private const val maxGapThresholdMinutes = 180
private const val maxMinimumBreakMinutes = 20

@org.koin.core.annotation.Single(binds = [SettingsPreferences::class])
class DataStoreSettingsPreferences(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferences {
    private val preferences: Flow<Preferences> = dataStore.data
        .retryWhen { cause, _ ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(readRetryDelayMillis)
            true
        }

    override val theme: Flow<ThemeMode> = preferences.map { stored ->
        themeModeFromStored(stored[themeKey])
    }

    override val collisionNotifications: Flow<CollisionNotificationPreferences> =
        preferences.map { stored -> stored.toNotificationPreferences() }

    override val gapThresholdMinutes: Flow<Int> = preferences.map { stored ->
        (stored[gapThresholdMinutesKey] ?: defaultGapThresholdMinutes)
            .coerceIn(minGapThresholdMinutes, maxGapThresholdMinutes)
    }

    override val planDisplay: Flow<PlanDisplaySettings> = preferences.map { stored ->
        PlanDisplaySettings(
            minimumBreakMinutes = (stored[minimumBreakMinutesKey] ?: 0).coerceIn(0, maxMinimumBreakMinutes)
        )
    }.distinctUntilChanged()

    override suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[themeKey] = mode.name }
    }

    override suspend fun setCollisionNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[notificationsEnabledKey] = enabled }
    }

    override suspend fun setEveningNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[eveningEnabledKey] = enabled }
    }

    override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[beforeClassEnabledKey] = enabled }
    }

    override suspend fun setEveningHour(hour: LocalTime) {
        dataStore.edit { preferences -> preferences[eveningHourKey] = hour.toSecondOfDay() / 60 }
    }

    override suspend fun setBeforeClassLeadMinutes(minutes: Long) {
        dataStore.edit { preferences -> preferences[leadMinutesKey] = minutes.toInt() }
    }

    override suspend fun setGapThresholdMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[gapThresholdMinutesKey] =
                minutes.coerceIn(minGapThresholdMinutes, maxGapThresholdMinutes)
        }
    }

    override suspend fun setMinimumBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[minimumBreakMinutesKey] = minutes.coerceIn(0, maxMinimumBreakMinutes)
        }
    }
}

private fun Preferences.toNotificationPreferences(): CollisionNotificationPreferences {
    val eveningMinutes = (this[eveningHourKey] ?: defaultEveningHourMinutes)
        .coerceIn(0, 23 * 60 + 59)
    return CollisionNotificationPreferences(
        enabled = this[notificationsEnabledKey] ?: false,
        eveningEnabled = this[eveningEnabledKey] ?: true,
        beforeClassEnabled = this[beforeClassEnabledKey] ?: true,
        eveningHour = LocalTime.ofSecondOfDay(eveningMinutes * 60L),
        leadMinutes = (this[leadMinutesKey] ?: defaultLeadMinutes).coerceIn(5, 120).toLong()
    )
}
