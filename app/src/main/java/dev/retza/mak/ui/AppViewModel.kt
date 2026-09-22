package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.ui.setup.SetupSemesterResume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.annotation.KoinViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val hasLoadedData: Boolean = false,
    val requiresSetup: Boolean = false,
    val setupResume: SetupSemesterResume? = null
)

private data class SetupCheck(
    val semesterId: Long,
    val plan: ActivePlanData
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    private val setupCheck = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester ->
            if (semester == null) {
                flowOf(null)
            } else {
                scheduleRepository.observeActivePlanData(semester.id)
                    .map { plan -> plan?.let { SetupCheck(semester.id, it) } }
            }
        }

    val uiState = setupCheck
        .map { check ->
            AppUiState(
                hasLoadedData = true,
                requiresSetup = check == null || check.plan.semesterPrograms.isEmpty(),
                setupResume = check
                    ?.takeIf { it.plan.semesterPrograms.isEmpty() }
                    ?.let { ready ->
                        val calendar = ready.plan.calendars
                            .minByOrNull { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                        SetupSemesterResume(
                            semesterId = ready.semesterId,
                            calendarId = calendar?.id?.toLongOrNull() ?: 0L,
                            name = ready.plan.semester.name,
                            startDate = calendar?.startDate?.toString().orEmpty(),
                            endDate = calendar?.endDate?.toString().orEmpty(),
                            firstWeekLabel = calendar?.firstWeekType?.name ?: "A"
                        )
                    }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState()
        )

    init {
        viewModelScope.launch {
            combine(
                semesterRepository.observeSemesters(),
                semesterRepository.observeActiveSemester()
            ) { list, active -> list to active }
                .collect { (list, active) ->
                    if (active == null && list.isNotEmpty()) {
                        semesterRepository.setActiveSemester(list.first().id)
                    }
                }
        }
    }
}
