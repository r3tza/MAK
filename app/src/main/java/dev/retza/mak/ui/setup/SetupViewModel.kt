package dev.retza.mak.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.feedback.FeedbackSink
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupStep {
    Semester,
    Course,
    Classes
}

enum class SetupField {
    SemesterName,
    StartDate,
    EndDate,
    CourseName
}

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Semester,
    val semesterName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val firstWeekLabel: String = "A",
    val courseName: String = "",
    val courseColor: String = "#137B71",
    val errors: Map<SetupField, FieldErrorUi> = emptyMap(),
    val status: ScreenStatus = ScreenStatus.Ready,
    val canSkipClasses: Boolean = true
)

class SetupViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(SetupWizardUiState())
    val setup: StateFlow<SetupWizardUiState> = state.asStateFlow()

    private var semesterId: Long? = null
    private var courseId: Long? = null
    private var saveJob: Job? = null
    private var sessionToken = 0L

    fun start() {
        sessionToken += 1
        saveJob?.cancel()
        semesterId = null
        courseId = null
        state.value = SetupWizardUiState()
    }

    fun update(transform: (SetupWizardUiState) -> SetupWizardUiState) {
        state.update { transform(it).copy(errors = emptyMap()) }
    }

    fun next() {
        when (state.value.step) {
            SetupStep.Semester -> advanceFromSemester()
            SetupStep.Course -> saveCourse()
            SetupStep.Classes -> Unit
        }
    }

    fun back() {
        state.update {
            when (it.step) {
                SetupStep.Classes -> it.copy(step = SetupStep.Course, errors = emptyMap())
                SetupStep.Course -> it.copy(step = SetupStep.Semester, errors = emptyMap())
                SetupStep.Semester -> it
            }
        }
    }

    private fun advanceFromSemester() {
        val current = state.value
        val start = current.startDate.toLocalDateOrNull()
        val end = current.endDate.toLocalDateOrNull()
        val errors = buildMap {
            if (current.semesterName.isBlank()) {
                put(SetupField.SemesterName, FieldErrorUi("Podaj nazwę semestru."))
            }
            if (start == null) put(SetupField.StartDate, FieldErrorUi("Wybierz poprawną datę."))
            if (end == null || start != null && end.isBefore(start)) {
                put(SetupField.EndDate, FieldErrorUi("Data końca nie może być wcześniejsza od początku."))
            }
        }
        if (errors.isNotEmpty() || start == null || end == null) {
            state.update { it.copy(errors = errors) }
            return
        }
        val token = sessionToken
        saveJob = viewModelScope.launch {
            try {
                val id = repository.saveSemester(
                    SemesterEntity(
                        id = semesterId ?: 0L,
                        name = current.semesterName.trim(),
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(current.firstWeekLabel),
                        isActive = true
                    )
                )
                if (token != sessionToken) return@launch
                semesterId = id
                state.update { it.copy(step = SetupStep.Course, errors = emptyMap()) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (token != sessionToken) return@launch
                state.update {
                    it.copy(
                        errors = mapOf(
                            SetupField.SemesterName to FieldErrorUi("Nie udało się zapisać semestru.")
                        )
                    )
                }
            }
        }
    }

    private fun saveCourse() {
        val current = state.value
        val name = current.courseName.trim()
        if (name.isBlank()) {
            state.update {
                it.copy(errors = mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku.")))
            }
            return
        }
        val token = sessionToken
        saveJob = viewModelScope.launch {
            try {
                val targetSemester = semesterId ?: return@launch
                val id = repository.saveCourse(
                    CourseEntity(
                        id = courseId ?: 0L,
                        semesterId = targetSemester,
                        name = name,
                        color = current.courseColor.ifBlank { "#137b71" }
                    )
                )
                if (token != sessionToken) return@launch
                courseId = id
                state.update { it.copy(step = SetupStep.Classes, errors = emptyMap()) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (token != sessionToken) return@launch
                state.update {
                    it.copy(
                        errors = mapOf(
                            SetupField.CourseName to FieldErrorUi("Nie udało się zapisać kierunku.")
                        )
                    )
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
            require(modelClass.isAssignableFrom(SetupViewModel::class.java))
            return SetupViewModel(repository, feedbackSink) as T
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
