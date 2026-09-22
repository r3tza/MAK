package dev.retza.mak.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ClassForm
import dev.retza.mak.domain.ClassValidationError
import dev.retza.mak.domain.ClassValidator
import dev.retza.mak.domain.Recurrence as DomainRecurrence
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ClassEditEffect {
    data object CloseEditor : ClassEditEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ClassEditViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(defaultClassEditState())
    val editor: StateFlow<ClassEditUiState> = state.asStateFlow()

    private val effectsChannel = Channel<ClassEditEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var editingClassId: Long? = null
    private var openJob: Job? = null

    private val activePlanData = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester ->
            if (semester == null) {
                flowOf(null)
            } else {
                scheduleRepository.observeActivePlanData(semester.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            activePlanData.collect { data ->
                state.update { current ->
                    val calendar = data?.calendarForAssignment(current.semesterProgramId)
                    current.copy(
                        courseOptions = data?.courseOptions().orEmpty(),
                        semesterStartDate = calendar?.startDate?.toString(),
                        semesterEndDate = calendar?.endDate?.toString()
                    )
                }
            }
        }
    }

    fun openNew(oneOffDate: LocalDate? = null) {
        openJob?.cancel()
        editingClassId = null
        val recurrenceId = if (oneOffDate == null) "every_week" else "once"
        state.value = withActiveOptions(
            defaultClassEditState().copy(
                recurrenceId = recurrenceId,
                recurrenceLabel = classEditRecurrenceLabel(recurrenceId),
                occurrenceDate = oneOffDate?.toString().orEmpty()
            )
        )
    }

    fun openEdit(occurrenceId: String) {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return
        openJob?.cancel()
        editingClassId = null
        state.value = withActiveOptions(defaultClassEditState())
        openJob = viewModelScope.launch {
            val data = activePlanData.first { it != null } ?: return@launch
            val item = data.classes.firstOrNull { it.id == classId.toString() } ?: return@launch
            val assignment = data.semesterPrograms.firstOrNull { it.id == item.semesterProgramId }
            val program = assignment?.let { link ->
                data.courses.firstOrNull { it.id == link.studyProgramId }
            }
            val recurrenceId = when (item.recurrence) {
                DomainRecurrence.EVERY_WEEK -> "every_week"
                DomainRecurrence.A_WEEK -> "a_week"
                DomainRecurrence.B_WEEK -> "b_week"
                DomainRecurrence.ONCE -> "once"
            }
            editingClassId = classId
            state.value = withActiveOptions(
                defaultClassEditState().copy(
                    title = "Edytuj zajęcia",
                    name = item.name,
                    courseName = program?.name.orEmpty(),
                    semesterProgramId = item.semesterProgramId,
                    type = item.type,
                    dayLabel = classEditDayNames[item.dayOfWeek].orEmpty(),
                    startTime = item.startTime.toString(),
                    endTime = item.endTime.toString(),
                    recurrenceId = recurrenceId,
                    recurrenceLabel = classEditRecurrenceLabel(recurrenceId),
                    occurrenceDate = item.date?.toString().orEmpty(),
                    room = item.room.orEmpty(),
                    building = item.building.orEmpty(),
                    group = item.group.orEmpty(),
                    teacher = item.teacherName.orEmpty(),
                    note = item.classNote.orEmpty()
                )
            )
        }
    }

    fun update(transform: (ClassEditUiState) -> ClassEditUiState) {
        state.update { transform(it).copy(errors = emptyMap()) }
    }

    fun selectCourse(optionId: String) {
        val calendar = activePlanData.value?.calendarForAssignment(optionId)
        update {
            it.copy(
                semesterProgramId = optionId,
                courseName = it.courseOptions.firstOrNull { option -> option.id == optionId }
                    ?.label
                    .orEmpty(),
                semesterStartDate = calendar?.startDate?.toString(),
                semesterEndDate = calendar?.endDate?.toString()
            )
        }
    }

    fun save() {
        if (state.value.isSaving) return
        val data = activePlanData.value ?: return
        val editor = state.value
        val start = editor.startTime.toLocalTimeOrNull()
        val end = editor.endTime.toLocalTimeOrNull()
        val recurrence = classEditRecurrenceFromId(editor.recurrenceId)
        val date = editor.occurrenceDate.toLocalDateOrNull()
        val assignmentId = editor.semesterProgramId
        val assignment = assignmentId?.let { id ->
            data.semesterPrograms.firstOrNull { it.id == id }
        }
        val validation = ClassValidator.validate(
            ClassForm(
                name = editor.name,
                semesterProgramId = assignmentId,
                startTime = start,
                endTime = end,
                recurrence = recurrence,
                date = date
            )
        )
        val errors = buildMap {
            if (ClassValidationError.NAME_REQUIRED in validation.errors) {
                put(ClassEditField.Name, FieldErrorUi("Podaj nazwę przedmiotu."))
            }
            if (ClassValidationError.PROGRAM_REQUIRED in validation.errors) {
                put(ClassEditField.Course, FieldErrorUi("Wybierz kierunek."))
            }
            if (ClassValidationError.START_TIME_REQUIRED in validation.errors) {
                put(ClassEditField.StartTime, FieldErrorUi("Podaj godzinę rozpoczęcia."))
            }
            if (ClassValidationError.END_TIME_REQUIRED in validation.errors) {
                put(ClassEditField.EndTime, FieldErrorUi("Podaj godzinę zakończenia."))
            }
            if (
                ClassValidationError.END_NOT_AFTER_START in validation.errors ||
                ClassValidationError.CROSSES_MIDNIGHT in validation.errors
            ) {
                put(ClassEditField.EndTime, FieldErrorUi("Koniec musi być późniejszy niż początek tego samego dnia."))
            }
            if (ClassValidationError.DATE_REQUIRED in validation.errors) {
                put(ClassEditField.Date, FieldErrorUi("Podaj datę zajęć jednorazowych."))
            }
            val calendar = data.calendarForAssignment(assignment?.id)
            if (recurrence == DomainRecurrence.ONCE && date != null && calendar != null &&
                (date.isBefore(calendar.startDate) || date.isAfter(calendar.endDate))
            ) {
                put(ClassEditField.Date, FieldErrorUi("Data musi należeć do kalendarza kierunku."))
            }
            if (editor.dayLabel !in classEditDayNames.values) {
                put(ClassEditField.Day, FieldErrorUi("Wybierz dzień tygodnia."))
            }
            if (editor.type.isBlank()) {
                put(ClassEditField.Type, FieldErrorUi("Wybierz typ zajęć."))
            }
        }
        if (errors.isNotEmpty() || assignment == null || start == null || end == null) {
            state.update { it.copy(errors = errors) }
            return
        }

        val classId = editingClassId
        val successMessage = if (classId == null) "Dodano zajęcia" else "Zapisano zmiany zajęć"
        state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                scheduleRepository.saveClass(
                    ClassRecord(
                        id = classId ?: 0,
                        semesterId = data.semester.id.toLong(),
                        semesterProgramId = assignment.id.toLong(),
                        name = editor.name.trim(),
                        type = editor.type,
                        teacherName = editor.teacher.trim().ifEmpty { null },
                        dayOfWeek = classEditDayNames.entries.first { it.value == editor.dayLabel }.key,
                        startTime = start,
                        endTime = end,
                        room = editor.room.trim().ifEmpty { null },
                        building = editor.building.trim().ifEmpty { null },
                        group = editor.group.trim().ifEmpty { null },
                        recurrence = DomainRecurrence.valueOf(recurrence.name),
                        date = if (recurrence == DomainRecurrence.ONCE) date else null,
                        classNote = editor.note.trim().ifEmpty { null }
                    )
                )
                editingClassId = null
                state.value = withActiveOptions(defaultClassEditState())
                feedbackSink.publish(UiFeedback(successMessage, UiFeedbackKind.Success))
                effectsChannel.trySend(ClassEditEffect.CloseEditor)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                feedbackSink.publish(UiFeedback("Nie udało się zapisać zajęć.", UiFeedbackKind.Error))
            } finally {
                state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun withActiveOptions(value: ClassEditUiState): ClassEditUiState {
        val data = activePlanData.value
        val calendar = data?.calendarForAssignment(value.semesterProgramId)
        return value.copy(
            courseOptions = data?.courseOptions().orEmpty(),
            semesterStartDate = calendar?.startDate?.toString(),
            semesterEndDate = calendar?.endDate?.toString()
        )
    }
}

private fun ActivePlanData.courseOptions(): List<ClassCourseOptionUi> =
    semesterPrograms.mapNotNull { assignment ->
        val program = courses.firstOrNull { it.id == assignment.studyProgramId }
            ?: return@mapNotNull null
        ClassCourseOptionUi(assignment.id, program.name)
    }

private fun defaultClassEditState() = ClassEditUiState(
    typeOptions = listOf("Wykład", "Ćwiczenia", "Laboratorium", "Projekt", "Seminarium", "Inne"),
    dayOptions = classEditDayNames.values.toList(),
    recurrenceOptions = listOf(
        RecurrenceOptionUi("every_week", "Co tydzień"),
        RecurrenceOptionUi("a_week", "Tydzień A"),
        RecurrenceOptionUi("b_week", "Tydzień B"),
        RecurrenceOptionUi("once", "Jednorazowo")
    )
)

private fun classEditRecurrenceFromId(id: String): DomainRecurrence = when (id) {
    "a_week" -> DomainRecurrence.A_WEEK
    "b_week" -> DomainRecurrence.B_WEEK
    "once" -> DomainRecurrence.ONCE
    else -> DomainRecurrence.EVERY_WEEK
}

private fun classEditRecurrenceLabel(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): java.time.LocalTime? =
    runCatching { java.time.LocalTime.parse(this) }.getOrNull()

private val classEditDayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Poniedziałek",
    DayOfWeek.TUESDAY to "Wtorek",
    DayOfWeek.WEDNESDAY to "Środa",
    DayOfWeek.THURSDAY to "Czwartek",
    DayOfWeek.FRIDAY to "Piątek",
    DayOfWeek.SATURDAY to "Sobota",
    DayOfWeek.SUNDAY to "Niedziela"
)
