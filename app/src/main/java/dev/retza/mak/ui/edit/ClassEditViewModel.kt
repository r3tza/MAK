package dev.retza.mak.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ClassEditImpact
import dev.retza.mak.domain.hiddenByClassEdit
import dev.retza.mak.domain.ClassForm
import dev.retza.mak.domain.ClassValidationError
import dev.retza.mak.domain.ClassValidator
import dev.retza.mak.domain.Recurrence as DomainRecurrence
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import java.time.DayOfWeek
import java.time.LocalDate
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
import org.koin.core.annotation.KoinViewModel

sealed interface ClassEditEffect {
    data object CloseEditor : ClassEditEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ClassEditViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val feedbackSink: FeedbackSink,
    private val savedState: SavedStateHandle
) : ViewModel() {
    // The draft survives process death: Android may stop the app while the user copies
    // the timetable from another app, and returning must not clear the form.
    private val state = MutableStateFlow(savedState.restoreClassEditDraft() ?: defaultClassEditState())
    val editor: StateFlow<ClassEditUiState> = state.asStateFlow()

    private val effectsChannel = Channel<ClassEditEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var editingClassId: Long? = savedState[KEY_EDITING_CLASS_ID]
        set(value) {
            field = value
            savedState[KEY_EDITING_CLASS_ID] = value
        }
    private var openJob: Job? = null
    private var editPlanData: ActivePlanData? = null

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
            state.collect { savedState.storeClassEditDraft(it) }
        }
        viewModelScope.launch {
            activePlanData.collect { data ->
                val optionsData = editPlanData ?: data
                state.update { current ->
                    val calendar = optionsData?.calendarForAssignment(current.semesterProgramId)
                    current.copy(
                        courseOptions = optionsData?.courseOptions().orEmpty(),
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
        editPlanData = null
        val recurrenceId = if (oneOffDate == null) "every_week" else "once"
        openJob = viewModelScope.launch {
            val data = freshActivePlanData()
            editPlanData = data
            state.value = withActiveOptions(
                defaultClassEditState().copy(
                    recurrenceId = recurrenceId,
                    recurrenceLabel = classEditRecurrenceLabel(recurrenceId),
                    occurrenceDate = oneOffDate?.toString().orEmpty()
                ),
                data
            )
        }
    }

    suspend fun openNewFromFreshPlan(oneOffDate: LocalDate? = null) {
        openJob?.cancel()
        editingClassId = null
        editPlanData = null
        val recurrenceId = if (oneOffDate == null) "every_week" else "once"
        state.value = withActiveOptions(
            defaultClassEditState().copy(
                recurrenceId = recurrenceId,
                recurrenceLabel = classEditRecurrenceLabel(recurrenceId),
                occurrenceDate = oneOffDate?.toString().orEmpty()
            ),
            freshActivePlanData().also { editPlanData = it }
        )
    }

    /**
     * Opens the editor from its route. Keeps the current draft when it already belongs to this
     * class, so recreating the screen (rotation, process death) does not reload stored data.
     */
    fun openEditIfNeeded(occurrenceId: String) {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return
        if (editingClassId == classId) return
        openEdit(occurrenceId)
    }

    fun openEdit(occurrenceId: String) {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return
        openJob?.cancel()
        editingClassId = null
        editPlanData = null
        state.value = withActiveOptions(defaultClassEditState())
        openJob = viewModelScope.launch {
            val data = freshActivePlanData() ?: return@launch
            val item = data.classes.firstOrNull { it.id == classId.toString() } ?: return@launch
            editPlanData = data
            editingClassId = classId
            state.value = classEditState(item, data)
        }
    }

    suspend fun openEditFromFreshPlan(occurrenceId: String): Boolean {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return false
        openJob?.cancel()
        editingClassId = null
        editPlanData = null
        state.value = withActiveOptions(defaultClassEditState())
        val data = freshActivePlanData() ?: return false
        val item = data.classes.firstOrNull { it.id == classId.toString() } ?: return false
        editPlanData = data
        editingClassId = classId
        state.value = classEditState(item, data)
        return true
    }

    private fun classEditState(item: dev.retza.mak.domain.ClassItem, data: ActivePlanData): ClassEditUiState {
        val assignment = data.semesterPrograms.firstOrNull { it.id == item.semesterProgramId }
        val program = assignment?.let { link -> data.courses.firstOrNull { it.id == link.studyProgramId } }
        val recurrenceId = when (item.recurrence) {
            DomainRecurrence.EVERY_WEEK -> "every_week"
            DomainRecurrence.A_WEEK -> "a_week"
            DomainRecurrence.B_WEEK -> "b_week"
            DomainRecurrence.ONCE -> "once"
        }
        return withActiveOptions(
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
            ), data
        )
    }

    fun update(transform: (ClassEditUiState) -> ClassEditUiState) {
        state.update { transform(it).copy(errors = emptyMap()) }
    }

    fun selectCourse(optionId: String) {
        val data = editPlanData ?: activePlanData.value
        val calendar = data?.calendarForAssignment(optionId)
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

    fun save() = save(confirmedHiddenData = false)

    fun confirmSaveWithHiddenData() {
        if (state.value.isSaving) return
        state.update { it.copy(pendingHiddenData = null) }
        save(confirmedHiddenData = true)
    }

    fun dismissHiddenData() {
        if (state.value.isSaving) return
        state.update { it.copy(pendingHiddenData = null) }
    }

    private fun save(confirmedHiddenData: Boolean) {
        if (state.value.isSaving) return
        val data = editPlanData ?: activePlanData.value ?: return
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
        val record = ClassRecord(
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
        if (classId != null && !confirmedHiddenData) {
            val impact = data.classEditImpact(classId.toString(), record)
            if (impact != null && !impact.isEmpty) {
                state.update {
                    it.copy(pendingHiddenData = HiddenDataWarningUi(impact.changeCount, impact.noteCount))
                }
                return
            }
        }
        val successMessage = if (classId == null) "Dodano zajęcia" else "Zapisano zmiany zajęć"
        state.update { it.copy(isSaving = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać zajęć.",
            onFinish = { state.update { it.copy(isSaving = false) } }
        ) {
            scheduleRepository.saveClass(record)
            val savedPlanData = freshActivePlanData()
            editingClassId = null
            state.value = withActiveOptions(defaultClassEditState(), savedPlanData)
            feedbackSink.publish(UiFeedback(successMessage, UiFeedbackKind.Success))
            effectsChannel.trySend(ClassEditEffect.CloseEditor)
        }
    }

    private suspend fun freshActivePlanData(): ActivePlanData? =
        semesterRepository.observeActiveSemester().first()?.let { semester ->
            scheduleRepository.observeActivePlanData(semester.id).first()
        }.also { editPlanData = it }

    private fun withActiveOptions(
        value: ClassEditUiState,
        data: ActivePlanData? = activePlanData.value
    ): ClassEditUiState {
        val calendar = data?.calendarForAssignment(value.semesterProgramId)
        return value.copy(
            courseOptions = data?.courseOptions().orEmpty(),
            semesterStartDate = calendar?.startDate?.toString(),
            semesterEndDate = calendar?.endDate?.toString()
        )
    }
}

private fun ActivePlanData.classEditImpact(classId: String, record: ClassRecord): ClassEditImpact? {
    val before = classes.firstOrNull { it.id == classId } ?: return null
    val after = before.copy(
        semesterProgramId = record.semesterProgramId.toString(),
        dayOfWeek = record.dayOfWeek,
        startTime = record.startTime,
        endTime = record.endTime,
        recurrence = record.recurrence,
        date = record.date
    )
    return hiddenByClassEdit(
        before = before,
        after = after,
        beforeCalendar = calendarForAssignment(before.semesterProgramId),
        afterCalendar = calendarForAssignment(after.semesterProgramId),
        overrides = weekOverrides,
        changes = occurrenceChanges,
        notes = occurrenceNotes
    )
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

private const val KEY_EDITING_CLASS_ID = "class_edit_editing_class_id"
private const val KEY_DRAFT = "class_edit_draft"

// Only the values typed by the user are stored; options and calendar ranges come from Room.
private val draftFields: List<Pair<String, (ClassEditUiState) -> String?>> = listOf(
    "title" to { it.title },
    "name" to { it.name },
    "courseName" to { it.courseName },
    "semesterProgramId" to { it.semesterProgramId },
    "type" to { it.type },
    "dayLabel" to { it.dayLabel },
    "startTime" to { it.startTime },
    "endTime" to { it.endTime },
    "recurrenceId" to { it.recurrenceId },
    "occurrenceDate" to { it.occurrenceDate },
    "room" to { it.room },
    "building" to { it.building },
    "group" to { it.group },
    "teacher" to { it.teacher },
    "note" to { it.note }
)

private fun SavedStateHandle.storeClassEditDraft(value: ClassEditUiState) {
    set(KEY_DRAFT, true)
    draftFields.forEach { (key, read) -> set("$KEY_DRAFT.$key", read(value)) }
}

private fun SavedStateHandle.restoreClassEditDraft(): ClassEditUiState? {
    if (get<Boolean>(KEY_DRAFT) != true) return null
    fun field(key: String): String? = get<String>("$KEY_DRAFT.$key")
    val recurrenceId = field("recurrenceId") ?: "every_week"
    return defaultClassEditState().copy(
        title = field("title") ?: "Dodaj zajęcia",
        name = field("name").orEmpty(),
        courseName = field("courseName").orEmpty(),
        semesterProgramId = field("semesterProgramId"),
        type = field("type").orEmpty(),
        dayLabel = field("dayLabel").orEmpty(),
        startTime = field("startTime").orEmpty(),
        endTime = field("endTime").orEmpty(),
        recurrenceId = recurrenceId,
        recurrenceLabel = classEditRecurrenceLabel(recurrenceId),
        occurrenceDate = field("occurrenceDate").orEmpty(),
        room = field("room").orEmpty(),
        building = field("building").orEmpty(),
        group = field("group").orEmpty(),
        teacher = field("teacher").orEmpty(),
        note = field("note").orEmpty()
    )
}
