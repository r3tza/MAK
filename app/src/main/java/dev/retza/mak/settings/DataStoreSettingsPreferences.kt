package dev.retza.mak.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.retza.mak.di.readWithRetry
import dev.retza.mak.domain.PlanDisplayLimits
import dev.retza.mak.domain.PlanDisplaySettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val themeKey = stringPreferencesKey("theme_mode")
private val notificationsEnabledKey = booleanPreferencesKey("collision_notifications_enabled")
private val eveningEnabledKey = booleanPreferencesKey("evening_notifications_enabled")
private val beforeClassEnabledKey = booleanPreferencesKey("before_class_notifications_enabled")
private val eveningHourKey = intPreferencesKey("evening_hour_minutes")
private val leadMinutesKey = intPreferencesKey("before_class_lead_minutes")
private val gapEveningEnabledKey = booleanPreferencesKey("gap_evening_notifications_enabled")
private val gapBeforeClassEnabledKey = booleanPreferencesKey("gap_before_class_notifications_enabled")
private val gapEveningHourKey = intPreferencesKey("gap_evening_hour_minutes")
private val gapLeadMinutesKey = intPreferencesKey("gap_lead_minutes")
private val gapThresholdMinutesKey = intPreferencesKey("gap_threshold_minutes")
private val minimumBreakMinutesKey = intPreferencesKey("minimum_break_minutes")
private val hiddenStudyProgramIdsKey = stringSetPreferencesKey("hidden_study_program_ids")

private const val defaultEveningHourMinutes = 20 * 60
private const val defaultLeadMinutes = 30

@org.koin.core.annotation.Single(binds = [SettingsPreferences::class])
class DataStoreSettingsPreferences(
    private val dataStore: DataStore<Preferences>
) : SettingsPreferences {
    private val preferences: Flow<Preferences> = dataStore.readWithRetry()

    override val theme: Flow<ThemeMode> = preferences.map { stored ->
        themeModeFromStored(stored[themeKey])
    }.distinctUntilChanged()

    override val collisionNotifications: Flow<CollisionNotificationPreferences> =
        preferences.map { stored -> stored.toNotificationPreferences() }.distinctUntilChanged()

    override val planDisplay: Flow<PlanDisplaySettings> = preferences.map { stored ->
        PlanDisplaySettings(
            minimumBreakMinutes = (stored[minimumBreakMinutesKey] ?: 0)
                .coerceIn(0, PlanDisplayLimits.maxMinimumBreakMinutes),
            gapThresholdMinutes = (stored[gapThresholdMinutesKey] ?: PlanDisplayLimits.DEFAULT_GAP_THRESHOLD_MINUTES)
                .coerceIn(PlanDisplayLimits.minGapThresholdMinutes, PlanDisplayLimits.MAX_GAP_THRESHOLD_MINUTES),
            hiddenProgramIds = storedHiddenProgramIds(stored[hiddenStudyProgramIdsKey])
        )
    }.distinctUntilChanged()

    override suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[themeKey] = mode.name }
    }

    override suspend fun setCollisionNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences.writeNotificationKinds(NotificationKinds(enabled, enabled, enabled, enabled))
        }
    }

    override suspend fun setEveningNotificationsEnabled(enabled: Boolean) {
        updateNotificationKinds { it.copy(evening = enabled) }
    }

    override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) {
        updateNotificationKinds { it.copy(beforeClass = enabled) }
    }

    override suspend fun setGapEveningNotificationsEnabled(enabled: Boolean) {
        updateNotificationKinds { it.copy(gapEvening = enabled) }
    }

    override suspend fun setGapBeforeClassNotificationsEnabled(enabled: Boolean) {
        updateNotificationKinds { it.copy(gapBeforeClass = enabled) }
    }

    override suspend fun setEveningHour(hour: LocalTime) {
        dataStore.edit { preferences -> preferences[eveningHourKey] = hour.toSecondOfDay() / 60 }
    }

    override suspend fun setBeforeClassLeadMinutes(minutes: Long) {
        dataStore.edit { preferences -> preferences[leadMinutesKey] = minutes.toInt() }
    }

    override suspend fun setGapEveningHour(hour: LocalTime) {
        dataStore.edit { preferences -> preferences[gapEveningHourKey] = hour.toSecondOfDay() / 60 }
    }

    override suspend fun setGapLeadMinutes(minutes: Long) {
        dataStore.edit { preferences -> preferences[gapLeadMinutesKey] = minutes.toInt() }
    }

    /** Reads the kinds and writes the changed set back in one edit, so a kind and the main switch never diverge. */
    private suspend fun updateNotificationKinds(change: (NotificationKinds) -> NotificationKinds) {
        dataStore.edit { preferences ->
            preferences.writeNotificationKinds(change(preferences.notificationKinds()))
        }
    }

    override suspend fun setGapThresholdMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[gapThresholdMinutesKey] = minutes.coerceIn(
                PlanDisplayLimits.minGapThresholdMinutes,
                PlanDisplayLimits.MAX_GAP_THRESHOLD_MINUTES
            )
        }
    }

    override suspend fun setMinimumBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[minimumBreakMinutesKey] = minutes.coerceIn(0, PlanDisplayLimits.maxMinimumBreakMinutes)
        }
    }

    override suspend fun setStudyProgramHidden(id: Long, hidden: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenStudyProgramIdsKey].orEmpty()
            val stored = id.toString()
            preferences[hiddenStudyProgramIdsKey] = if (hidden) current + stored else current - stored
        }
    }

    override suspend fun clearHiddenStudyPrograms() {
        dataStore.edit { preferences -> preferences.remove(hiddenStudyProgramIdsKey) }
    }

    override suspend fun retainHiddenStudyPrograms(existingIds: Set<Long>) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenStudyProgramIdsKey] ?: return@edit
            preferences[hiddenStudyProgramIdsKey] = current
                .filterTo(mutableSetOf()) { stored -> stored.toLongOrNull()?.let { it in existingIds } == true }
        }
    }
}

/** Effective on/off state of the four notification kinds. */
private data class NotificationKinds(
    val evening: Boolean,
    val beforeClass: Boolean,
    val gapEvening: Boolean,
    val gapBeforeClass: Boolean
) {
    val any: Boolean get() = evening || beforeClass || gapEvening || gapBeforeClass
}

/**
 * A kind is on only while the main switch is on. Older versions kept the kind defaults `true` while the feature
 * was off, so a missing or false main switch turns every kind off. The gap kinds are new and default to off.
 * No migration is needed.
 */
private fun Preferences.notificationKinds(): NotificationKinds {
    val main = this[notificationsEnabledKey] ?: false
    return NotificationKinds(
        evening = main && (this[eveningEnabledKey] ?: true),
        beforeClass = main && (this[beforeClassEnabledKey] ?: true),
        gapEvening = main && (this[gapEveningEnabledKey] ?: false),
        gapBeforeClass = main && (this[gapBeforeClassEnabledKey] ?: false)
    )
}

/** Writes the four kinds and the main switch, which is on exactly when some kind is on. */
private fun MutablePreferences.writeNotificationKinds(kinds: NotificationKinds) {
    this[notificationsEnabledKey] = kinds.any
    this[eveningEnabledKey] = kinds.evening
    this[beforeClassEnabledKey] = kinds.beforeClass
    this[gapEveningEnabledKey] = kinds.gapEvening
    this[gapBeforeClassEnabledKey] = kinds.gapBeforeClass
}

private fun Preferences.storedEveningHour(key: Preferences.Key<Int>): LocalTime {
    val minutes = (this[key] ?: defaultEveningHourMinutes).coerceIn(0, 23 * 60 + 59)
    return LocalTime.ofSecondOfDay(minutes * 60L)
}

private fun Preferences.storedLeadMinutes(key: Preferences.Key<Int>): Long =
    (this[key] ?: defaultLeadMinutes).coerceIn(5, 120).toLong()

private fun Preferences.toNotificationPreferences(): CollisionNotificationPreferences {
    val kinds = notificationKinds()
    return CollisionNotificationPreferences(
        enabled = kinds.any,
        eveningEnabled = kinds.evening,
        beforeClassEnabled = kinds.beforeClass,
        eveningHour = storedEveningHour(eveningHourKey),
        leadMinutes = storedLeadMinutes(leadMinutesKey),
        gapEveningEnabled = kinds.gapEvening,
        gapBeforeClassEnabled = kinds.gapBeforeClass,
        gapEveningHour = storedEveningHour(gapEveningHourKey),
        gapLeadMinutes = storedLeadMinutes(gapLeadMinutesKey)
    )
}

/** Hidden program ids are stored as text; a stored value that is not a number is ignored, never read as 0. */
private fun storedHiddenProgramIds(stored: Set<String>?): Set<Long> =
    stored.orEmpty().mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }
