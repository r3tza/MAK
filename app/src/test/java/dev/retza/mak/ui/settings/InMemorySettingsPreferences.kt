package dev.retza.mak.ui.settings

import dev.retza.mak.domain.PlanDisplaySettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySettingsPreferences(
    initialTheme: ThemeMode = ThemeMode.System,
    initialNotifications: CollisionNotificationPreferences = CollisionNotificationPreferences(),
    initialGapThresholdMinutes: Int = 30,
    initialPlanDisplay: PlanDisplaySettings = PlanDisplaySettings.DEFAULT
) : SettingsPreferences {
    private val state = MutableStateFlow(initialTheme)
    private val notifications = MutableStateFlow(initialNotifications)
    private val gapThreshold = MutableStateFlow(initialGapThresholdMinutes)
    private val display = MutableStateFlow(initialPlanDisplay)

    var failNextWrite = false

    override val theme: Flow<ThemeMode> = state

    override val collisionNotifications: Flow<CollisionNotificationPreferences> = notifications

    override val gapThresholdMinutes: Flow<Int> = gapThreshold

    override val planDisplay: Flow<PlanDisplaySettings> = display

    override suspend fun setTheme(mode: ThemeMode) {
        awaitWrite()
        state.value = mode
    }

    override suspend fun setGapThresholdMinutes(minutes: Int) {
        awaitWrite()
        gapThreshold.value = minutes
    }

    override suspend fun setMinimumBreakMinutes(minutes: Int) {
        awaitWrite()
        display.value = display.value.copy(minimumBreakMinutes = minutes)
    }

    override suspend fun setStudyProgramHidden(id: String, hidden: Boolean) {
        awaitWrite()
        val current = display.value.hiddenProgramIds
        display.value = display.value.copy(hiddenProgramIds = if (hidden) current + id else current - id)
    }

    override suspend fun clearHiddenStudyPrograms() {
        awaitWrite()
        display.value = display.value.copy(hiddenProgramIds = emptySet())
    }

    override suspend fun retainHiddenStudyPrograms(existingIds: Set<String>) {
        awaitWrite()
        display.value = display.value.copy(hiddenProgramIds = display.value.hiddenProgramIds intersect existingIds)
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
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("settings write failed")
        }
    }
}
