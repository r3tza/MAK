package dev.retza.mak.settings

import dev.retza.mak.domain.PlanDisplaySettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    System,
    Light,
    Dark
}

/**
 * `enabled` is true exactly when at least one of the four kinds is enabled. Turning the main switch on or off sets
 * all kinds; turning one kind on turns the main switch on; turning the last kind off turns the main switch off.
 * A stored main switch that is false or missing reads as all kinds off. The gap kinds are new, so a missing stored
 * gap kind reads as off, while the two collision kinds read as on.
 */
data class CollisionNotificationPreferences(
    val enabled: Boolean = false,
    val eveningEnabled: Boolean = true,
    val beforeClassEnabled: Boolean = true,
    val eveningHour: LocalTime = LocalTime.of(20, 0),
    val leadMinutes: Long = 30,
    val gapEveningEnabled: Boolean = false,
    val gapBeforeClassEnabled: Boolean = false,
    val gapEveningHour: LocalTime = LocalTime.of(20, 0),
    val gapLeadMinutes: Long = 30
)

interface SettingsPreferences {
    val theme: Flow<ThemeMode>

    val collisionNotifications: Flow<CollisionNotificationPreferences>

    /** Settings every view of the plan, the widget and notifications resolve the plan with. */
    val planDisplay: Flow<PlanDisplaySettings>

    suspend fun setTheme(mode: ThemeMode)

    suspend fun setGapThresholdMinutes(minutes: Int)

    suspend fun setMinimumBreakMinutes(minutes: Int)

    suspend fun setStudyProgramHidden(id: Long, hidden: Boolean)

    /** After a plan from elsewhere replaced this one, the stored numbers name other programs. */
    suspend fun clearHiddenStudyPrograms()

    /** Forgets hidden programs that no longer exist, so a reused number is not hidden by accident. */
    suspend fun retainHiddenStudyPrograms(existingIds: Set<Long>)

    suspend fun setCollisionNotificationsEnabled(enabled: Boolean)

    suspend fun setEveningNotificationsEnabled(enabled: Boolean)

    suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean)

    suspend fun setGapEveningNotificationsEnabled(enabled: Boolean)

    suspend fun setGapBeforeClassNotificationsEnabled(enabled: Boolean)

    suspend fun setEveningHour(hour: LocalTime)

    suspend fun setBeforeClassLeadMinutes(minutes: Long)

    suspend fun setGapEveningHour(hour: LocalTime)

    suspend fun setGapLeadMinutes(minutes: Long)
}

fun themeModeFromStored(value: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.System
