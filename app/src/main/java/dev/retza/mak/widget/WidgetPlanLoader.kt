package dev.retza.mak.widget

import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class WidgetPlanLoader(
    private val repository: MakRepository,
    private val activePlanProvider: ActivePlanProvider = ActivePlanProvider(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val presenter: WidgetPresenter = WidgetPresenter()
) {
    suspend fun load(): WidgetUiState {
        val date = LocalDate.now(clock)
        val dateLabel = widgetDateLabel(date)
        return runCatching {
            val semester = repository.observeActiveSemester().first()
                ?: return@runCatching WidgetUiState.NoActiveSemester(dateLabel)
            val data = repository.observeSemesterData(semester.id).first()
                ?: return@runCatching WidgetUiState.Error(dateLabel)
            val planData = data.toActivePlanData()
            val plan = activePlanProvider.resolve(planData, date)
            presenter.present(date, data.semester.name, plan)
        }.getOrElse {
            WidgetUiState.Error(dateLabel)
        }
    }
}
