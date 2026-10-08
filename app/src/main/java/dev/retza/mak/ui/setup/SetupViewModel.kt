package dev.retza.mak.ui.setup

import dev.retza.mak.ui.components.DefaultCourseColor
import dev.retza.mak.ui.components.suggestedCourseColor
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
import dev.retza.mak.ui.feedback.launchUiOperation
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

/** Week A/B source for a further program added in the wizard. */
enum class SetupCalendarMode {
    Shared,
    Separate
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
    val courseColor: String = DefaultCourseColor,
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
    val isSaving: Boolean = false,
    // True while the course step adds a further program to the already saved semester.
    val isAddingAnotherProgram: Boolean = false,
    val calendarMode: SetupCalendarMode = SetupCalendarMode.Shared,
    // Programs saved in this wizard session, in the order they were added.
    val semesterProgramNames: List<String> = emptyList(),
    val semesterProgramIds: List<Long> = emptyList()
) {
    /** Existing programs the course step may offer; one already in the semester cannot be added twice. */
    val availableProgramOptions: List<SetupProgramOptionUi>
        get() = if (isAddingAnotherProgram) {
            programOptions.filterNot { it.id in semesterProgramIds }
        } else {
            programOptions
        }
}

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

    // The first program's draft, restored when the further program form closes.
    private var firstProgramDraft: SetupWizardUiState? = null

    // Colors of the programs saved in this session, so a further program starts with a distinct one.
    private var semesterProgramColors: List<String> = emptyList()

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

    fun selectCalendarMode(mode: SetupCalendarMode) = update { it.copy(calendarMode = mode) }

    fun selectProgramMode(mode: SetupProgramMode) = update {
        if (it.isProgramChoiceLocked) it else it.copy(programMode = mode)
    }

    fun selectProgram(id: Long) = update {
        if (it.isProgramChoiceLocked) it else it.copy(selectedProgramId = id)
    }

    fun start(resume: SetupSemesterResume? = null) {
        start(resume, state.value.programOptions)
    }

    private fun start(resume: SetupSemesterResume?, programOptions: List<SetupProgramOptionUi>) {
        sessionToken += 1
        saveJob?.cancel()
        firstProgramDraft = null
        semesterProgramColors = emptyList()
        if (resume == null) {
            semesterId = null
            courseId = null
            calendarId = null
            state.value = SetupWizardUiState().withProgramOptions(programOptions)
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
            ).withProgramOptions(programOptions)
        }
    }

    suspend fun startFromRoom(resumeExisting: Boolean) {
        val programOptions = semesterRepository.observeStudyPrograms().first().map {
            SetupProgramOptionUi(it.id, it.name, it.color)
        }
        if (!resumeExisting) {
            start(resume = null, programOptions = programOptions)
            return
        }
        val active = semesterRepository.observeActiveSemester().first()
        val resume = active?.let { semester ->
            val assignments = semesterRepository.observeSemesterPrograms(semester.id).first()
            if (assignments.isNotEmpty()) return@let null
            val calendar = semesterRepository.observeCalendars(semester.id).first()
                .minByOrNull { it.id }
            SetupSemesterResume(
                semesterId = semester.id,
                calendarId = calendar?.id ?: 0L,
                name = semester.name,
                startDate = calendar?.startDate?.toString().orEmpty(),
                endDate = calendar?.endDate?.toString().orEmpty(),
                firstWeekLabel = calendar?.firstWeekType?.name ?: "A"
            )
        }
        start(resume, programOptions)
    }

    fun update(transform: (SetupWizardUiState) -> SetupWizardUiState) {
        state.update { transform(it).copy(errors = emptyMap()) }
    }

    fun next() {
        when (state.value.step) {
            SetupStep.Semester -> advanceFromSemester()
            SetupStep.Course -> if (state.value.isAddingAnotherProgram) saveAnotherProgram() else saveConfiguration()
            SetupStep.Classes -> Unit
        }
    }

    fun back() {
        if (state.value.isAddingAnotherProgram) {
            if (!state.value.isSaving) closeAnotherProgram()
            return
        }
        state.update {
            when (it.step) {
                SetupStep.Classes -> it.copy(step = SetupStep.Course, errors = emptyMap())
                SetupStep.Course -> it.copy(step = SetupStep.Semester, errors = emptyMap())
                SetupStep.Semester -> it
            }
        }
    }

    /** Opens the course form for a further program in the semester saved by this wizard. */
    fun startAnotherProgram() {
        val current = state.value
        if (current.step != SetupStep.Classes || semesterId == null || calendarId == null) return
        firstProgramDraft = current
        state.value = current.copy(
            step = SetupStep.Course,
            isAddingAnotherProgram = true,
            courseName = "",
            courseColor = suggestedCourseColor(semesterProgramColors),
            programMode = SetupProgramMode.New,
            selectedProgramId = null,
            isProgramChoiceLocked = false,
            calendarMode = SetupCalendarMode.Shared,
            errors = emptyMap()
        )
    }

    private fun closeAnotherProgram(savedName: String? = null, savedId: Long? = null) {
        val draft = firstProgramDraft ?: return
        firstProgramDraft = null
        val current = state.value
        state.value = draft.copy(
            step = SetupStep.Classes,
            isAddingAnotherProgram = false,
            programOptions = current.programOptions,
            isSemesterActive = current.isSemesterActive,
            semesterProgramNames = current.semesterProgramNames + listOfNotNull(savedName),
            semesterProgramIds = current.semesterProgramIds + listOfNotNull(savedId),
            errors = emptyMap()
        )
    }

    private fun saveAnotherProgram() {
        val current = state.value
        if (current.isSaving) return
        val semester = semesterId ?: return
        val calendar = calendarId ?: return
        val program = programRecordFor(current, id = 0L) ?: return
        state.update { it.copy(isSaving = true, errors = emptyMap()) }
        val token = sessionToken
        saveJob = viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się dodać kierunku.",
            onFinish = {
                if (token == sessionToken) {
                    state.update { it.copy(isSaving = false) }
                }
            },
            isCurrent = { token == sessionToken }
        ) {
            val ids = when (current.calendarMode) {
                SetupCalendarMode.Shared -> semesterRepository.saveStudyProgramAssignment(
                    semesterId = semester,
                    studyProgram = program,
                    academicCalendarId = calendar
                )
                SetupCalendarMode.Separate -> semesterRepository.addSeparatedSemesterProgram(
                    semesterId = semester,
                    studyProgram = program,
                    sourceCalendarId = calendar
                )
            }
            if (token != sessionToken) return@launchUiOperation
            semesterProgramColors = semesterProgramColors + program.color
            state.update { it.copy(isSaving = false) }
            closeAnotherProgram(savedName = program.name, savedId = ids.studyProgramId)
            feedbackSink.publish(UiFeedback("Dodano kierunek", UiFeedbackKind.Success))
        }
    }

    /** Validates the course form; shows the field error and returns null when it is incomplete. */
    private fun programRecordFor(current: SetupWizardUiState, id: Long): StudyProgramRecord? =
        when (current.programMode) {
            SetupProgramMode.New -> {
                val name = current.courseName.trim()
                if (name.isBlank()) {
                    state.update {
                        it.copy(errors = mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku.")))
                    }
                    null
                } else {
                    StudyProgramRecord(
                        id = id,
                        name = name,
                        color = current.courseColor.ifBlank { DefaultCourseColor }
                    )
                }
            }

            SetupProgramMode.Existing -> {
                val selected = current.availableProgramOptions.firstOrNull { it.id == current.selectedProgramId }
                if (selected == null) {
                    state.update {
                        it.copy(errors = mapOf(SetupField.CourseProgram to FieldErrorUi("Wybierz kierunek.")))
                    }
                    null
                } else {
                    // A shared program keeps its current name and color.
                    StudyProgramRecord(id = selected.id, name = selected.name, color = selected.color)
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
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się ustawić aktywnego semestru.",
            onFinish = {
                if (token == sessionToken) {
                    state.update { it.copy(isActivating = false) }
                }
            },
            isCurrent = { token == sessionToken }
        ) {
            semesterRepository.setActiveSemester(id)
            if (token != sessionToken) return@launchUiOperation
            state.update { it.copy(isSemesterActive = true) }
            effectsChannel.trySend(SetupEffect.OpenNewClassEditor)
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
        val program = programRecordFor(current, id = courseId ?: 0L) ?: return
        val existingSemester = semesterId
        val existingCalendar = calendarId
        val isUpdate = existingSemester != null
        state.update { it.copy(isSaving = true, errors = emptyMap()) }
        val token = sessionToken
        saveJob = viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać konfiguracji.",
            onFinish = {
                if (token == sessionToken) {
                    state.update { it.copy(isSaving = false) }
                }
            },
            isCurrent = { token == sessionToken }
        ) {
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
            if (token != sessionToken) return@launchUiOperation
            semesterId = ids.semesterId
            courseId = ids.studyProgramId
            semesterProgramColors = listOf(program.color) + semesterProgramColors.drop(1)
            calendarId = ids.academicCalendarId
            state.update {
                it.copy(
                    step = SetupStep.Classes,
                    errors = emptyMap(),
                    isProgramChoiceLocked = true,
                    isSemesterActive = ids.semesterIsActive,
                    semesterProgramNames = listOf(program.name) + it.semesterProgramNames.drop(1),
                    semesterProgramIds = listOf(ids.studyProgramId) + it.semesterProgramIds.drop(1)
                )
            }
            val message = when {
                isUpdate -> "Zaktualizowano konfigurację"
                ids.semesterIsActive -> "Utworzono semestr i kierunek"
                else -> "Utworzono semestr i kierunek. Aktywny semestr się nie zmienił."
            }
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
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
