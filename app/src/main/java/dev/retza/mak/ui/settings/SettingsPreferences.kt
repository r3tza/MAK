package dev.retza.mak.ui.settings

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

    suspend fun setTheme(mode: ThemeMode)

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
