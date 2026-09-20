package dev.retza.mak.ui.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.feedback.FeedbackSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemesterViewModel(
    private val repository: MakRepository,
    @Suppress("unused") private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(SemesterScreenUiState())
    val semester: StateFlow<SemesterScreenUiState> = state.asStateFlow()

    private val semesterIdState = MutableStateFlow<Long?>(null)
    val semesterId: StateFlow<Long?> = semesterIdState.asStateFlow()

    private var openJob: Job? = null

    fun open(id: String) {
        val parsed = id.toLongOrNull() ?: return
        openJob?.cancel()
        semesterIdState.value = null
        state.value = SemesterScreenUiState()
        openJob = viewModelScope.launch {
            try {
                repository.setActiveSemester(parsed)
                val data = repository.observeSemesterData(parsed).first() ?: return@launch
                semesterIdState.value = parsed
                state.value = data.toSemesterScreenState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Invalid id or storage error: keep the cleared state.
            }
        }
    }

    fun update(transform: (SemesterScreenUiState) -> SemesterScreenUiState) {
        state.update { transform(it) }
    }

    class Factory(
        private val repository: MakRepository,
        private val feedbackSink: FeedbackSink
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SemesterViewModel::class.java))
            return SemesterViewModel(repository, feedbackSink) as T
        }
    }
}

private fun SemesterWithData.toSemesterScreenState(): SemesterScreenUiState = SemesterScreenUiState(
    semester = SemesterFormUiState(
        name = semester.name,
        startDate = semester.startDate.toString(),
        endDate = semester.endDate.toString(),
        firstWeek = WeekTypeUi.valueOf(semester.firstWeekType.name)
    ),
    overrides = weekOverrides.map { override ->
        WeekOverrideUi(
            override.id.toString(),
            override.weekStartDate.toString(),
            WeekTypeUi.valueOf(override.weekType.name),
            WeekOverrideScopeUi.valueOf(override.scope.name)
        )
    },
    courses = courses.map { course -> course.id.toString() to course.name }
)
