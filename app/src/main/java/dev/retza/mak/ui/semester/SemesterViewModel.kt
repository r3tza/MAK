package dev.retza.mak.ui.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SemesterEffect {
    data object CloseConfiguration : SemesterEffect
}

class SemesterViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(SemesterScreenUiState())
    val semester: StateFlow<SemesterScreenUiState> = state.asStateFlow()

    private val semesterIdState = MutableStateFlow<Long?>(null)
    val semesterId: StateFlow<Long?> = semesterIdState.asStateFlow()

    private val effectsChannel = Channel<SemesterEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

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

    fun saveSemester() {
        if (state.value.semester.isSaving) return
        val id = semesterIdState.value ?: return
        val form = state.value.semester
        val start = form.startDate.toLocalDateOrNull()
        val end = form.endDate.toLocalDateOrNull()
        if (form.name.isBlank() || start == null || end == null || end.isBefore(start)) {
            update {
                it.copy(semester = form.copy(
                    nameError = if (form.name.isBlank()) "Podaj nazwę semestru." else null,
                    startDateError = if (start == null) "Podaj poprawną datę." else null,
                    endDateError = if (end == null) "Podaj poprawną datę." else null,
                    dateRangeError = if (start != null && end != null && end.isBefore(start)) {
                        "Koniec nie może być wcześniejszy od początku."
                    } else {
                        null
                    }
                ))
            }
            return
        }
        update {
            it.copy(
                semester = form.copy(
                    isSaving = true,
                    nameError = null,
                    startDateError = null,
                    endDateError = null,
                    dateRangeError = null
                )
            )
        }
        viewModelScope.launch {
            try {
                repository.saveSemester(
                    SemesterEntity(
                        id = id,
                        name = form.name.trim(),
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(form.firstWeek.name),
                        isActive = true
                    )
                )
                feedbackSink.publish(UiFeedback("Zapisano semestr", UiFeedbackKind.Success))
                effectsChannel.trySend(SemesterEffect.CloseConfiguration)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                update {
                    it.copy(semester = it.semester.copy(dateRangeError = "Nie udało się zapisać semestru."))
                }
                feedbackSink.publish(UiFeedback("Nie udało się zapisać semestru.", UiFeedbackKind.Error))
            } finally {
                update { it.copy(semester = it.semester.copy(isSaving = false)) }
            }
        }
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

private fun String.toLocalDateOrNull(): java.time.LocalDate? =
    runCatching { java.time.LocalDate.parse(this) }.getOrNull()

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
