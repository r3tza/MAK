package dev.retza.mak.ui.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    System,
    Light,
    Dark
}

interface SettingsPreferences {
    val theme: Flow<ThemeMode>

    suspend fun setTheme(mode: ThemeMode)
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
