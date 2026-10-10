package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterRepository
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
    // An active semester without study programs: the wizard continues with it instead of starting over.
    val canResumeSetup: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppViewModel(
    private val semesterRepository: SemesterRepository
) : ViewModel() {
    val uiState = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester ->
            if (semester == null) {
                flowOf(AppUiState(hasLoadedData = true, requiresSetup = true))
            } else {
                semesterRepository.observeSemesterPrograms(semester.id).map { programs ->
                    AppUiState(
                        hasLoadedData = true,
                        requiresSetup = programs.isEmpty(),
                        canResumeSetup = programs.isEmpty()
                    )
                }
            }
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
