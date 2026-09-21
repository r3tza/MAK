package dev.retza.mak.ui.settings

import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsPreferences(
    initialTheme: ThemeMode = ThemeMode.System,
    initialNotifications: CollisionNotificationPreferences = CollisionNotificationPreferences()
) : SettingsPreferences {
    private val state = MutableStateFlow(initialTheme)
    private val notifications = MutableStateFlow(initialNotifications)

    var failNextWrite = false
    var writeGate: CompletableDeferred<Unit>? = null
    var writeCount = 0

    override val theme: Flow<ThemeMode> = state

    override val collisionNotifications: Flow<CollisionNotificationPreferences> = notifications

    override suspend fun setTheme(mode: ThemeMode) {
        awaitWrite()
        state.value = mode
    }

    override suspend fun setCollisionNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.copy(enabled = enabled)
    }

    override suspend fun setEveningNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.copy(eveningEnabled = enabled)
    }

    override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.copy(beforeClassEnabled = enabled)
    }

    override suspend fun setEveningHour(hour: LocalTime) {
        awaitWrite()
        notifications.value = notifications.value.copy(eveningHour = hour)
    }

    override suspend fun setBeforeClassLeadMinutes(minutes: Long) {
        awaitWrite()
        notifications.value = notifications.value.copy(leadMinutes = minutes)
    }

    private suspend fun awaitWrite() {
        writeGate?.await()
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("settings write failed")
        }
        writeCount += 1
    }
}
