package dev.retza.mak.data

import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.settings.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

/**
 * The plan data of the active semester with the settings that change how it resolves, so every view,
 * the widget and notifications follow both (`ARCHITECTURE.md`, section 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivePlanSource(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val preferences: SettingsPreferences,
    scope: CoroutineScope
) {
    // One Room query for every collector. The cache is dropped when nobody listens, so a later first()
    // from a receiver reads the database again instead of an old plan.
    private val active: Flow<ActivePlanInputs?> = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester -> if (semester == null) flowOf(null) else observe(semester.id) }
        .distinctUntilChanged()
        .shareIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
            replay = 1
        )

    /** Null without an active semester or its data. */
    fun observeActive(): Flow<ActivePlanInputs?> = active

    fun observe(semesterId: Long): Flow<ActivePlanInputs?> =
        combine(scheduleRepository.observeActivePlanData(semesterId), preferences.planDisplay) { data, display ->
            data?.let { ActivePlanInputs(it, display) }
        }
}
