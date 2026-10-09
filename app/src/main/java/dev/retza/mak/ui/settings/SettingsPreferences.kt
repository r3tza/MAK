package dev.retza.mak.ui.settings

import dev.retza.mak.domain.PlanDisplaySettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    System,
    Light,
    Dark
}

data class CollisionNotificationPreferences(
    val enabled: Boolean = false,
    val eveningEnabled: Boolean = true,
    val beforeClassEnabled: Boolean = true,
    val eveningHour: LocalTime = LocalTime.of(20, 0),
    val leadMinutes: Long = 30
)

interface SettingsPreferences {
    val theme: Flow<ThemeMode>

    val collisionNotifications: Flow<CollisionNotificationPreferences>

    val gapThresholdMinutes: Flow<Int>

    /** Settings every view of the plan, the widget and notifications resolve the plan with. */
    val planDisplay: Flow<PlanDisplaySettings>

    suspend fun setTheme(mode: ThemeMode)

    suspend fun setGapThresholdMinutes(minutes: Int)

    suspend fun setMinimumBreakMinutes(minutes: Int)

    suspend fun setStudyProgramHidden(id: String, hidden: Boolean)

    /** After a plan from elsewhere replaced this one, the stored numbers name other programs. */
    suspend fun clearHiddenStudyPrograms()

    /** Forgets hidden programs that no longer exist, so a reused number is not hidden by accident. */
    suspend fun retainHiddenStudyPrograms(existingIds: Set<String>)

    suspend fun setCollisionNotificationsEnabled(enabled: Boolean)

    suspend fun setEveningNotificationsEnabled(enabled: Boolean)

    suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean)

    suspend fun setEveningHour(hour: LocalTime)

    suspend fun setBeforeClassLeadMinutes(minutes: Long)
}

fun themeModeFromStored(value: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.System

fun themeModeFromId(id: String): ThemeMode = when (id) {
    "light" -> ThemeMode.Light
    "dark" -> ThemeMode.Dark
    else -> ThemeMode.System
}

fun ThemeMode.toId(): String = when (this) {
    ThemeMode.System -> "system"
    ThemeMode.Light -> "light"
    ThemeMode.Dark -> "dark"
}
