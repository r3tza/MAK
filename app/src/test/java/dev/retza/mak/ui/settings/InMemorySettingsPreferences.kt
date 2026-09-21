package dev.retza.mak.ui.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsPreferences(
    initialTheme: ThemeMode = ThemeMode.System
) : SettingsPreferences {
    private val state = MutableStateFlow(initialTheme)

    var failNextWrite = false
    var writeGate: CompletableDeferred<Unit>? = null
    var writeCount = 0

    override val theme: Flow<ThemeMode> = state

    override suspend fun setTheme(mode: ThemeMode) {
        writeGate?.await()
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("theme write failed")
        }
        writeCount += 1
        state.value = mode
    }
}
