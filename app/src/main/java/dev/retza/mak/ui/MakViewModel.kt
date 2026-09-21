package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class MakDestination {
    Today,
    Schedule,
    EditClass,
    OccurrenceDetails,
    Semester,
    Settings,
    Setup
}

data class MakUiState(
    val destination: MakDestination = MakDestination.Today,
    val requiresSetup: Boolean = true,
    val hasLoadedData: Boolean = false,
    val activeSemesterData: SemesterWithData? = null
)

private data class Controls(
    val destination: MakDestination = MakDestination.Today
)

@OptIn(ExperimentalCoroutinesApi::class)
class MakViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink,
    private val classEditViewModel: ClassEditViewModel
) : ViewModel() {
    private val controls = MutableStateFlow(Controls())

    private val semesters = repository.observeSemesters()
    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val uiState = combine(semesters, activeSemesterData, controls) { semesterList, activeData, control ->
        buildState(semesterList, activeData, control)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MakUiState()
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

    fun publishFeedback(kind: UiFeedbackKind, message: String) {
        feedbackSink.publish(UiFeedback(message, kind))
    }

    fun navigate(destination: MakDestination) {
        controls.update { it.copy(destination = destination) }
    }

    private fun buildState(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): MakUiState {
        val requiresSetup = semesterList.isEmpty() ||
            activeData != null && activeData.courses.isEmpty()
        val destination = if (requiresSetup) MakDestination.Setup else control.destination
        return MakUiState(
            destination = destination,
            requiresSetup = requiresSetup,
            hasLoadedData = true,
            activeSemesterData = activeData
        )
    }

    class Factory(
        private val repository: MakRepository,
        private val feedbackSink: FeedbackSink,
        private val classEditViewModel: ClassEditViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MakViewModel::class.java))
            return MakViewModel(repository, feedbackSink, classEditViewModel) as T
        }
    }
}
