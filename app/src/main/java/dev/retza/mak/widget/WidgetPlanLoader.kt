package dev.retza.mak.widget

import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.settings.SettingsPreferences
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class WidgetPlanLoader(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val activePlanProvider: ActivePlanProvider,
    private val preferences: SettingsPreferences,
    private val clock: Clock,
    private val presenter: WidgetPresenter = WidgetPresenter()
) {
    suspend fun load(): WidgetUiState {
        val date = LocalDate.now(clock)
        val dateLabel = widgetDateLabel(date)
        return try {
            val semester = semesterRepository.observeActiveSemester().first()
                ?: return WidgetUiState.NoActiveSemester(dateLabel)
            val planData = scheduleRepository.observeActivePlanData(semester.id).first()
                ?: return WidgetUiState.Error(dateLabel)
            val plan = activePlanProvider.resolve(planData, date, preferences.planDisplay.first())
            presenter.present(date, planData.semester.name, plan, LocalTime.now(clock))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WidgetUiState.Error(dateLabel)
        }
    }
}
