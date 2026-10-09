package dev.retza.mak.ui

import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.ui.settings.SettingsPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * The plan data of the active semester with the settings that change how it resolves, so every view,
 * the widget and notifications follow both (`ARCHITECTURE.md`, section 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@org.koin.core.annotation.Single
class ActivePlanSource(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val preferences: SettingsPreferences
) {
    /** Null without an active semester or its data. */
    fun observeActive(): Flow<ActivePlanInputs?> =
        semesterRepository.observeActiveSemester().flatMapLatest { semester ->
            if (semester == null) flowOf(null) else observe(semester.id)
        }

    fun observe(semesterId: Long): Flow<ActivePlanInputs?> =
        combine(scheduleRepository.observeActivePlanData(semesterId), preferences.planDisplay) { data, display ->
            data?.let { ActivePlanInputs(it, display) }
        }
}
