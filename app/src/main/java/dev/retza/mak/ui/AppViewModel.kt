package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.MakRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppViewModel(
    private val repository: MakRepository
) : ViewModel() {
    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val uiState = activeSemesterData
        .map { activeData ->
            val calendar = activeData?.academicCalendars?.minByOrNull { it.id }
            AppUiState(
                hasLoadedData = true,
                requiresSetup = activeData == null || activeData.semesterPrograms.isEmpty(),
                setupResume = activeData
                    ?.takeIf { it.semesterPrograms.isEmpty() }
                    ?.let { data ->
                        SetupSemesterResume(
                            semesterId = data.semester.id,
                            calendarId = calendar?.id ?: 0L,
                            name = data.semester.name,
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
            combine(repository.observeSemesters(), repository.observeActiveSemester()) { list, active ->
                list to active
            }.collect { (list, active) ->
                if (active == null && list.isNotEmpty()) {
                    repository.setActiveSemester(list.first().id)
                }
            }
        }
    }

    class Factory(
        private val repository: MakRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(repository) as T
        }
    }
}
