package dev.retza.mak.settings

import dev.retza.mak.domain.PlanDisplaySettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsPreferences(
    initialTheme: ThemeMode = ThemeMode.System,
    initialNotifications: CollisionNotificationPreferences = CollisionNotificationPreferences(),
    initialPlanDisplay: PlanDisplaySettings = PlanDisplaySettings.DEFAULT
) : SettingsPreferences {
    private val state = MutableStateFlow(initialTheme)
    private val notifications = MutableStateFlow(initialNotifications.readKindsThroughMainSwitch())
    private val display = MutableStateFlow(initialPlanDisplay)

    var failNextWrite = false

    override val theme: Flow<ThemeMode> = state

    override val collisionNotifications: Flow<CollisionNotificationPreferences> = notifications

    override val planDisplay: Flow<PlanDisplaySettings> = display

    override suspend fun setTheme(mode: ThemeMode) {
        awaitWrite()
        state.value = mode
    }

    override suspend fun setGapThresholdMinutes(minutes: Int) {
        awaitWrite()
        display.value = display.value.copy(gapThresholdMinutes = minutes)
    }

    override suspend fun setMinimumBreakMinutes(minutes: Int) {
        awaitWrite()
        display.value = display.value.copy(minimumBreakMinutes = minutes)
    }

    override suspend fun setStudyProgramHidden(id: Long, hidden: Boolean) {
        awaitWrite()
        val current = display.value.hiddenProgramIds
        display.value = display.value.copy(hiddenProgramIds = if (hidden) current + id else current - id)
    }

    override suspend fun clearHiddenStudyPrograms() {
        awaitWrite()
        display.value = display.value.copy(hiddenProgramIds = emptySet())
    }

    override suspend fun retainHiddenStudyPrograms(existingIds: Set<Long>) {
        awaitWrite()
        display.value = display.value.copy(hiddenProgramIds = display.value.hiddenProgramIds intersect existingIds)
    }

    override suspend fun setCollisionNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.copy(
            enabled = enabled,
            eveningEnabled = enabled,
            beforeClassEnabled = enabled
        )
    }

    override suspend fun setEveningNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        val stored = notifications.value.readKindsThroughMainSwitch()
        notifications.value = stored.copy(
            enabled = enabled || stored.beforeClassEnabled,
            eveningEnabled = enabled
        )
    }

    override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        val stored = notifications.value.readKindsThroughMainSwitch()
        notifications.value = stored.copy(
            enabled = stored.eveningEnabled || enabled,
            beforeClassEnabled = enabled
        )
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
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("settings write failed")
        }
    }
}

/** Same reading rule as DataStoreSettingsPreferences: a kind is on only while the main switch is on. */
private fun CollisionNotificationPreferences.readKindsThroughMainSwitch(): CollisionNotificationPreferences {
    val evening = enabled && eveningEnabled
    val beforeClass = enabled && beforeClassEnabled
    return copy(enabled = evening || beforeClass, eveningEnabled = evening, beforeClassEnabled = beforeClass)
}
