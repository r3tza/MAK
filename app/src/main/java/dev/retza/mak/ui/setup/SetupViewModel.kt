package dev.retza.mak.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    val canSkipClasses: Boolean = true,
    val isSaving: Boolean = false
)

sealed interface SetupEffect {
    data object FinishToToday : SetupEffect
    data object ReturnToSettings : SetupEffect
    data object OpenNewClassEditor : SetupEffect
}

data class SetupSemesterResume(
    val semesterId: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val firstWeekLabel: String
)

@KoinViewModel
class SetupViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(SetupWizardUiState())
    val setup: StateFlow<SetupWizardUiState> = state.asStateFlow()

    private val effectsChannel = Channel<SetupEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var semesterId: Long? = null
    private var courseId: Long? = null
    private var saveJob: Job? = null
    private var sessionToken = 0L

    fun start(resume: SetupSemesterResume? = null) {
        sessionToken += 1
        saveJob?.cancel()
        if (resume == null) {
            semesterId = null
            courseId = null
            state.value = SetupWizardUiState()
        } else {
            semesterId = resume.semesterId
            courseId = null
            state.value = SetupWizardUiState(
                step = SetupStep.Course,
                semesterName = resume.name,
                startDate = resume.startDate,
                endDate = resume.endDate,
                firstWeekLabel = resume.firstWeekLabel
            )
        }
    }

    fun update(transform: (SetupWizardUiState) -> SetupWizardUiState) {
        state.update { transform(it).copy(errors = emptyMap()) }
    }

    fun next() {
        when (state.value.step) {
            SetupStep.Semester -> advanceFromSemester()
            SetupStep.Course -> saveConfiguration()
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

    fun addClass() {
        effectsChannel.trySend(SetupEffect.OpenNewClassEditor)
    }

    fun finish() {
        start()
        effectsChannel.trySend(SetupEffect.FinishToToday)
    }

    fun returnToSettings() {
        start()
        effectsChannel.trySend(SetupEffect.ReturnToSettings)
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
        state.update { it.copy(step = SetupStep.Course, errors = emptyMap()) }
    }

    private fun saveConfiguration() {
        if (state.value.isSaving) return
        val current = state.value
        val start = current.startDate.toLocalDateOrNull() ?: return
        val end = current.endDate.toLocalDateOrNull() ?: return
        val name = current.courseName.trim()
        if (name.isBlank()) {
            state.update {
                it.copy(errors = mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku.")))
            }
            return
        }
        val existingSemester = semesterId
        val existingCourse = courseId
        val isUpdate = existingSemester != null
        state.update { it.copy(isSaving = true, errors = emptyMap()) }
        val token = sessionToken
        saveJob = viewModelScope.launch {
            try {
                val ids = repository.saveSetupConfiguration(
                    SemesterEntity(
                        id = existingSemester ?: 0L,
                        name = current.semesterName.trim(),
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(current.firstWeekLabel),
                        isActive = true
                    ),
                    CourseEntity(
                        id = existingCourse ?: 0L,
                        semesterId = existingSemester ?: 0L,
                        name = name,
                        color = current.courseColor.ifBlank { "#137b71" }
                    )
                )
                if (token != sessionToken) return@launch
                semesterId = ids.semesterId
                courseId = ids.courseId
                state.update { it.copy(step = SetupStep.Classes, errors = emptyMap()) }
                val message = if (isUpdate) {
                    "Zaktualizowano konfigurację"
                } else {
                    "Utworzono semestr i kierunek"
                }
                feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (token == sessionToken) {
                    feedbackSink.publish(
                        UiFeedback("Nie udało się zapisać konfiguracji.", UiFeedbackKind.Error)
                    )
                }
            } finally {
                if (token == sessionToken) {
                    state.update { it.copy(isSaving = false) }
                }
            }
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
