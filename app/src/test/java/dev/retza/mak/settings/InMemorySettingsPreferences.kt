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
        notifications.value = notifications.value.withKinds(
            evening = enabled,
            beforeClass = enabled,
            gapEvening = enabled,
            gapBeforeClass = enabled
        )
    }

    override suspend fun setEveningNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.readKindsThroughMainSwitch().withKinds(evening = enabled)
    }

    override suspend fun setBeforeClassNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.readKindsThroughMainSwitch().withKinds(beforeClass = enabled)
    }

    override suspend fun setGapEveningNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.readKindsThroughMainSwitch().withKinds(gapEvening = enabled)
    }

    override suspend fun setGapBeforeClassNotificationsEnabled(enabled: Boolean) {
        awaitWrite()
        notifications.value = notifications.value.readKindsThroughMainSwitch().withKinds(gapBeforeClass = enabled)
    }

    override suspend fun setEveningHour(hour: LocalTime) {
        awaitWrite()
        notifications.value = notifications.value.copy(eveningHour = hour)
    }

    override suspend fun setBeforeClassLeadMinutes(minutes: Long) {
        awaitWrite()
        notifications.value = notifications.value.copy(leadMinutes = minutes)
    }

    override suspend fun setGapEveningHour(hour: LocalTime) {
        awaitWrite()
        notifications.value = notifications.value.copy(gapEveningHour = hour)
    }

    override suspend fun setGapLeadMinutes(minutes: Long) {
        awaitWrite()
        notifications.value = notifications.value.copy(gapLeadMinutes = minutes)
    }

    private suspend fun awaitWrite() {
        if (failNextWrite) {
            failNextWrite = false
            throw IllegalStateException("settings write failed")
        }
    }
}

/** Same reading rule as DataStoreSettingsPreferences: a kind is on only while the main switch is on. */
private fun CollisionNotificationPreferences.readKindsThroughMainSwitch(): CollisionNotificationPreferences =
    withKinds(
        evening = enabled && eveningEnabled,
        beforeClass = enabled && beforeClassEnabled,
        gapEvening = enabled && gapEveningEnabled,
        gapBeforeClass = enabled && gapBeforeClassEnabled
    )

/** Sets the kinds and the main switch, which is on exactly when some kind is on. */
private fun CollisionNotificationPreferences.withKinds(
    evening: Boolean = eveningEnabled,
    beforeClass: Boolean = beforeClassEnabled,
    gapEvening: Boolean = gapEveningEnabled,
    gapBeforeClass: Boolean = gapBeforeClassEnabled
): CollisionNotificationPreferences = copy(
    enabled = evening || beforeClass || gapEvening || gapBeforeClass,
    eveningEnabled = evening,
    beforeClassEnabled = beforeClass,
    gapEveningEnabled = gapEvening,
    gapBeforeClassEnabled = gapBeforeClass
)
