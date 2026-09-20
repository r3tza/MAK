package dev.retza.mak.ui.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.DayOfWeek
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
    private var sessionToken = 0L

    fun open(id: String) {
        sessionToken += 1
        val token = sessionToken
        openJob?.cancel()
        semesterIdState.value = null
        state.value = SemesterScreenUiState()
        val parsed = id.toLongOrNull() ?: return
        openJob = viewModelScope.launch {
            try {
                repository.setActiveSemester(parsed)
                val data = repository.observeSemesterData(parsed).first() ?: return@launch
                if (token != sessionToken) return@launch
                semesterIdState.value = parsed
                state.value = data.toSemesterScreenState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Invalid id or storage error: keep the cleared state.
            }
        }
    }

    private fun isCurrentSession(token: Long): Boolean = token == sessionToken

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
        val token = sessionToken
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
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Zapisano semestr", UiFeedbackKind.Success))
                effectsChannel.trySend(SemesterEffect.CloseConfiguration)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                update {
                    it.copy(semester = it.semester.copy(dateRangeError = "Nie udało się zapisać semestru."))
                }
                feedbackSink.publish(UiFeedback("Nie udało się zapisać semestru.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(semester = it.semester.copy(isSaving = false)) }
                }
            }
        }
    }

    fun updateCourseName(value: String) = update {
        it.copy(courseNameDraft = value, courseNameError = null)
    }

    fun updateCourseColor(value: String) = update {
        it.copy(courseColorDraft = value)
    }

    fun addCourse() {
        if (state.value.isAddingCourse) return
        val id = semesterIdState.value ?: return
        val draft = state.value
        val name = draft.courseNameDraft.trim()
        if (name.isBlank()) {
            update { it.copy(courseNameError = "Podaj nazwę kierunku.") }
            return
        }
        update { it.copy(isAddingCourse = true, courseNameError = null) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                repository.saveCourse(
                    CourseEntity(
                        semesterId = id,
                        name = name,
                        color = draft.courseColorDraft.ifBlank { "#137b71" }
                    )
                )
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                update { it.copy(courseNameDraft = "", isAddingCourse = false) }
                feedbackSink.publish(UiFeedback("Dodano kierunek", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się dodać kierunku.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isAddingCourse = false) }
                }
            }
        }
    }

    fun deleteCourse(id: String) {
        if (state.value.isDeletingCourse) return
        val courseId = id.toLongOrNull() ?: return
        update { it.copy(isDeletingCourse = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                repository.deleteCourse(courseId)
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się usunąć kierunku.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isDeletingCourse = false) }
                }
            }
        }
    }

    private suspend fun refresh(token: Long) {
        if (!isCurrentSession(token)) return
        val id = semesterIdState.value ?: return
        val data = repository.observeSemesterData(id).first() ?: return
        if (!isCurrentSession(token)) return
        val current = state.value
        state.value = data.toSemesterScreenState().copy(
            courseNameDraft = current.courseNameDraft,
            courseColorDraft = current.courseColorDraft,
            overrideForm = current.overrideForm
        )
    }

    fun newWeekOverride() = update {
        it.copy(overrideForm = WeekOverrideFormUiState(isOpen = true))
    }

    fun editWeekOverride(id: String) {
        val item = state.value.overrides.firstOrNull { it.id == id } ?: return
        update {
            it.copy(
                overrideForm = WeekOverrideFormUiState(
                    id = item.id,
                    weekStartDate = item.weekStartDate,
                    weekType = item.weekType,
                    scope = item.scope,
                    isOpen = true
                )
            )
        }
    }

    fun cancelWeekOverrideEdit() = update {
        it.copy(overrideForm = WeekOverrideFormUiState())
    }

    fun saveWeekOverride() {
        if (state.value.overrideForm.isSaving) return
        val semester = semesterIdState.value ?: return
        val form = state.value.overrideForm
        val date = form.weekStartDate.toLocalDateOrNull()
        if (date == null || date.dayOfWeek != DayOfWeek.MONDAY) {
            update { it.copy(overrideForm = form.copy(weekStartDateError = "Wybierz poniedziałek.")) }
            return
        }
        update { it.copy(overrideForm = form.copy(isSaving = true, weekStartDateError = null)) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                repository.saveWeekOverride(
                    WeekOverrideEntity(
                        id = form.id?.toLongOrNull() ?: 0,
                        semesterId = semester,
                        weekStartDate = date,
                        weekType = WeekType.valueOf(form.weekType.name),
                        scope = WeekOverrideScope.valueOf(form.scope.name)
                    )
                )
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                update { it.copy(overrideForm = WeekOverrideFormUiState()) }
                val message = if (form.id == null) "Dodano korektę tygodnia" else "Zapisano korektę tygodnia"
                feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się zapisać korekty tygodnia.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(overrideForm = it.overrideForm.copy(isSaving = false)) }
                }
            }
        }
    }

    fun deleteWeekOverride(id: String) {
        if (state.value.isDeletingOverride) return
        val overrideId = id.toLongOrNull() ?: return
        update { it.copy(isDeletingOverride = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                repository.deleteWeekOverride(overrideId)
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                update { it.copy(overrideForm = WeekOverrideFormUiState()) }
                feedbackSink.publish(UiFeedback("Usunięto korektę tygodnia", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się usunąć korekty tygodnia.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isDeletingOverride = false) }
                }
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
