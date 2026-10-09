package dev.retza.mak.widget

import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.ActivePlanSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class WidgetPlanLoader(
    private val semesterRepository: SemesterRepository,
    private val activePlanSource: ActivePlanSource,
    private val activePlanProvider: ActivePlanProvider,
    private val clock: Clock,
    private val presenter: WidgetPresenter = WidgetPresenter()
) {
    suspend fun load(): WidgetUiState {
        val date = LocalDate.now(clock)
        val dateLabel = widgetDateLabel(date)
        return try {
            val semester = semesterRepository.observeActiveSemester().first()
                ?: return WidgetUiState.NoActiveSemester(dateLabel)
            val inputs = activePlanSource.observe(semester.id).first()
                ?: return WidgetUiState.Error(dateLabel)
            val plan = activePlanProvider.resolve(inputs, date)
            presenter.present(date, inputs.data.semester.name, plan, LocalTime.now(clock))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WidgetUiState.Error(dateLabel)
        }
    }
}
