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

class InMemorySettingsPreferences(
    initialTheme: ThemeMode = ThemeMode.System
) : SettingsPreferences {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initialTheme)

    override val theme: Flow<ThemeMode> = state

    override suspend fun setTheme(mode: ThemeMode) {
        state.value = mode
    }
}

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
