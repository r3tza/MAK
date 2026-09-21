package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.setup.SetupSemesterResume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MakDestination {
    Today,
    Schedule,
    EditClass,
    OccurrenceDetails,
    Semester,
    Settings,
    Setup
}

data class AppUiState(
    val destination: MakDestination = MakDestination.Today,
    val hasLoadedData: Boolean = false,
    val requiresSetup: Boolean = false,
    val setupResume: SetupSemesterResume? = null
)

private data class Controls(
    val destination: MakDestination = MakDestination.Today
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppViewModel(
    private val repository: MakRepository
) : ViewModel() {
    private val controls = MutableStateFlow(Controls())

    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val uiState = combine(activeSemesterData, controls) { activeData, control ->
        val requiresSetup = activeData == null || activeData.courses.isEmpty()
        AppUiState(
            destination = control.destination,
            hasLoadedData = true,
            requiresSetup = requiresSetup,
            setupResume = activeData
                ?.takeIf { it.courses.isEmpty() }
                ?.semester
                ?.let { semester ->
                    SetupSemesterResume(
                        semesterId = semester.id,
                        name = semester.name,
                        startDate = semester.startDate.toString(),
                        endDate = semester.endDate.toString(),
                        firstWeekLabel = semester.firstWeekType.name
                    )
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState()
    )

    init {
        viewModelScope.launch {
            combine(repository.observeSemesters(), repository.observeActiveSemester()) { list, active ->
                list to active
            }.collect { (list, active) ->
                if (active == null && list.isNotEmpty()) {
                    repository.setActiveSemester(list.first().id)
                }
            }
        }
    }

    fun navigate(destination: MakDestination) {
        controls.update { it.copy(destination = destination) }
    }
}
