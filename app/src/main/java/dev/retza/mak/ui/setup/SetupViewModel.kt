package dev.retza.mak.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.domain.WeekType
import dev.retza.mak.domain.shouldActivateNewSemester
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.Clock
import java.time.LocalDate
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

enum class SetupStep {
    Semester,
    Course,
    Classes
}

enum class SetupField {
    SemesterName,
    StartDate,
    EndDate,
    CourseName,
    CourseProgram
}

enum class SetupProgramMode {
    New,
    Existing
}

data class SetupProgramOptionUi(
    val id: Long,
    val name: String,
    val color: String
)

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Semester,
    val semesterName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val firstWeekLabel: String = "A",
    val courseName: String = "",
    val courseColor: String = "#137B71",
    val programOptions: List<SetupProgramOptionUi> = emptyList(),
    val programMode: SetupProgramMode = SetupProgramMode.New,
    val selectedProgramId: Long? = null,
    // After the course step is saved, the program choice is fixed for this wizard session:
    // switching it would either rename a shared program or leave a second assignment behind.
    val isProgramChoiceLocked: Boolean = false,
    // False when the saved semester was not activated, see shouldActivateNewSemester.
    val isSemesterActive: Boolean = true,
    val isActivating: Boolean = false,
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
    val calendarId: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val firstWeekLabel: String
)

@KoinViewModel
class SetupViewModel(
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink,
    private val clock: Clock
) : ViewModel() {
    private val state = MutableStateFlow(SetupWizardUiState())
    val setup: StateFlow<SetupWizardUiState> = state.asStateFlow()

    private val effectsChannel = Channel<SetupEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var semesterId: Long? = null
    private var courseId: Long? = null
    private var calendarId: Long? = null
    private var saveJob: Job? = null
    private var sessionToken = 0L

    init {
        viewModelScope.launch {
            try {
                semesterRepository.observeStudyPrograms().collect { programs ->
                    val options = programs.map { SetupProgramOptionUi(it.id, it.name, it.color) }
                    state.update { it.withProgramOptions(options) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Without the list the wizard still creates a new program.
                state.update { it.withProgramOptions(emptyList()) }
            }
        }
    }

    fun selectProgramMode(mode: SetupProgramMode) = update {
        if (it.isProgramChoiceLocked) it else it.copy(programMode = mode)
    }

    fun selectProgram(id: Long) = update {
        if (it.isProgramChoiceLocked) it else it.copy(selectedProgramId = id)
    }

    fun start(resume: SetupSemesterResume? = null) {
        sessionToken += 1
        saveJob?.cancel()
        if (resume == null) {
            semesterId = null
            courseId = null
            calendarId = null
            state.value = SetupWizardUiState().withProgramOptions(state.value.programOptions)
        } else {
            semesterId = resume.semesterId
            courseId = null
            calendarId = resume.calendarId.takeIf { it != 0L }
            state.value = SetupWizardUiState(
                step = SetupStep.Course,
                semesterName = resume.name,
                startDate = resume.startDate,
                endDate = resume.endDate,
                firstWeekLabel = resume.firstWeekLabel
            ).withProgramOptions(state.value.programOptions)
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

    // The class form always saves into the active semester, so an inactive one is activated first.
    fun activateAndAddClass() {
        val id = semesterId ?: return
        if (state.value.isActivating) return
        state.update { it.copy(isActivating = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.setActiveSemester(id)
                if (token != sessionToken) return@launch
                state.update { it.copy(isSemesterActive = true) }
                effectsChannel.trySend(SetupEffect.OpenNewClassEditor)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (token == sessionToken) {
                    feedbackSink.publish(
                        UiFeedback("Nie udało się ustawić aktywnego semestru.", UiFeedbackKind.Error)
                    )
                }
            } finally {
                if (token == sessionToken) {
                    state.update { it.copy(isActivating = false) }
                }
            }
        }
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
        val program = when (current.programMode) {
            SetupProgramMode.New -> {
                val name = current.courseName.trim()
                if (name.isBlank()) {
                    state.update {
                        it.copy(errors = mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku.")))
                    }
                    return
                }
                StudyProgramRecord(
                    id = courseId ?: 0L,
                    name = name,
                    color = current.courseColor.ifBlank { "#137b71" }
                )
            }

            SetupProgramMode.Existing -> {
                val selected = current.programOptions.firstOrNull { it.id == current.selectedProgramId }
                if (selected == null) {
                    state.update {
                        it.copy(errors = mapOf(SetupField.CourseProgram to FieldErrorUi("Wybierz kierunek.")))
                    }
                    return
                }
                // A shared program keeps its current name and color.
                StudyProgramRecord(id = selected.id, name = selected.name, color = selected.color)
            }
        }
        val existingSemester = semesterId
        val existingCalendar = calendarId
        val isUpdate = existingSemester != null
        state.update { it.copy(isSaving = true, errors = emptyMap()) }
        val token = sessionToken
        saveJob = viewModelScope.launch {
            try {
                val activate = existingSemester == null && shouldActivateNewSemester(
                    today = LocalDate.now(clock),
                    calendarStart = start,
                    calendarEnd = end,
                    hasActiveSemester = semesterRepository.observeActiveSemester().first() != null
                )
                val ids = semesterRepository.saveSetupConfiguration(
                    SemesterRecord(
                        id = existingSemester ?: 0L,
                        name = current.semesterName.trim(),
                        isActive = activate
                    ),
                    program,
                    AcademicCalendarRecord(
                        id = existingCalendar ?: 0L,
                        semesterId = existingSemester ?: 0L,
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(current.firstWeekLabel)
                    ),
                    activate = activate
                )
                if (token != sessionToken) return@launch
                semesterId = ids.semesterId
                courseId = ids.studyProgramId
                calendarId = ids.academicCalendarId
                state.update {
                    it.copy(
                        step = SetupStep.Classes,
                        errors = emptyMap(),
                        isProgramChoiceLocked = true,
                        isSemesterActive = ids.semesterIsActive
                    )
                }
                val message = when {
                    isUpdate -> "Zaktualizowano konfigurację"
                    ids.semesterIsActive -> "Utworzono semestr i kierunek"
                    else -> "Utworzono semestr i kierunek. Aktywny semestr się nie zmienił."
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

private fun SetupWizardUiState.withProgramOptions(options: List<SetupProgramOptionUi>): SetupWizardUiState {
    if (isProgramChoiceLocked) return copy(programOptions = options)
    val selected = selectedProgramId?.takeIf { id -> options.any { it.id == id } }
    return copy(
        programOptions = options,
        programMode = if (options.isEmpty()) SetupProgramMode.New else programMode,
        selectedProgramId = selected
    )
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
