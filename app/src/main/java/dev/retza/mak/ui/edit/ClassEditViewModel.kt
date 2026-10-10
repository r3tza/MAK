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
import dev.retza.mak.domain.dateInCalendar
import dev.retza.mak.domain.Recurrence as DomainRecurrence
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.data.ActivePlanSource
import dev.retza.mak.ui.FormErrorTexts
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.programForAssignment
import dev.retza.mak.ui.toLocalDateOrNull
import dev.retza.mak.ui.toLocalTimeOrNull
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.occurrence.OccurrenceArgs
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed interface ClassEditEffect {
    data object CloseEditor : ClassEditEffect
}

enum class ClassEditOpenState { Opening, Open, Missing }

@KoinViewModel
class ClassEditViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val feedbackSink: FeedbackSink,
    private val savedState: SavedStateHandle,
    activePlanSource: ActivePlanSource,
    planEditTracker: PlanEditTracker
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
    private var editPlanData: ActivePlanData? = null

    // Kept in the saved state so a rotation or process death does not load the form again over a restored draft.
    private val openState = MutableStateFlow(
        savedState.get<String>(KEY_OPEN_STATE)?.let(ClassEditOpenState::valueOf) ?: ClassEditOpenState.Opening
    )
    val opened: StateFlow<ClassEditOpenState> = openState.asStateFlow()

    // Unfiltered: the form offers every study program and only marks the hidden ones (`FEATURES.md`, I-79).
    private val activePlan = activePlanSource.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The form is a draft from its start, so it holds the edit key until the view model is cleared.
    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { true },
        hasDraft = { true }
    )

    init {
        viewModelScope.launch {
            state.collect { savedState.storeClassEditDraft(it) }
        }
        viewModelScope.launch {
            activePlan.collect { inputs ->
                state.update { withActiveOptions(it, editPlanData ?: inputs?.data) }
            }
        }
    }

    override fun onCleared() {
        editSession.close()
    }

    /** Loads the form for this destination once; a restored draft is kept (`ARCHITECTURE.md`, section 5). */
    suspend fun openOnce() {
        // Admission comes first: after process death the open state is restored and the form must still hold the key.
        val admitted = editSession.admitForm()
        if (openState.value != ClassEditOpenState.Opening) return
        val classId = savedState.get<String>("classId")?.toLongOrNull()
        val date = savedState.get<String>("date")?.toLocalDateOrNull()
        val result = if (!admitted) {
            ClassEditOpenState.Missing
        } else if (classId != null && date != null) {
            if (openEditFromFreshPlan(OccurrenceArgs(classId, date).toRouteId())) {
                ClassEditOpenState.Open
            } else {
                ClassEditOpenState.Missing
            }
        } else {
            openNewFromFreshPlan(date)
            ClassEditOpenState.Open
        }
        openState.value = result
        savedState[KEY_OPEN_STATE] = result.name
    }

    suspend fun openNewFromFreshPlan(oneOffDate: LocalDate? = null) {
        editingClassId = null
        editPlanData = null
        state.value = withActiveOptions(
            defaultClassEditState().copy(
                recurrence = if (oneOffDate == null) DomainRecurrence.EVERY_WEEK else DomainRecurrence.ONCE,
                occurrenceDate = oneOffDate?.toString().orEmpty()
            ),
            freshActivePlanData().also { editPlanData = it }
        )
    }

    suspend fun openEditFromFreshPlan(occurrenceId: String): Boolean {
        val classId = OccurrenceArgs.parse(occurrenceId)?.classId ?: return false
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
        val program = data.programForAssignment(item.semesterProgramId)
        return withActiveOptions(
            defaultClassEditState().copy(
                title = "Edytuj zajęcia",
                name = item.name,
                courseName = program?.name.orEmpty(),
                semesterProgramId = item.semesterProgramId,
                type = item.type,
                dayLabel = dayNames[item.dayOfWeek].orEmpty(),
                startTime = item.startTime.toString(),
                endTime = item.endTime.toString(),
                recurrence = item.recurrence,
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
        val data = editPlanData ?: activePlan.value?.data
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
        val data = editPlanData ?: activePlan.value?.data ?: return
        val editor = state.value
        val start = editor.startTime.toLocalTimeOrNull()
        val end = editor.endTime.toLocalTimeOrNull()
        val recurrence = editor.recurrence
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
                put(ClassEditField.Course, FieldErrorUi(FormErrorTexts.ProgramRequired))
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
                put(ClassEditField.EndTime, FieldErrorUi(FormErrorTexts.EndNotAfterStart))
            }
            if (ClassValidationError.DATE_REQUIRED in validation.errors) {
                put(ClassEditField.Date, FieldErrorUi("Podaj datę zajęć jednorazowych."))
            }
            val calendar = data.calendarForAssignment(assignment?.id)
            if (recurrence == DomainRecurrence.ONCE && date != null && !dateInCalendar(date, calendar)) {
                put(ClassEditField.Date, FieldErrorUi(FormErrorTexts.DateOutsideCalendar))
            }
            if (editor.dayLabel !in dayNames.values) {
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
            dayOfWeek = dayNames.entries.first { it.value == editor.dayLabel }.key,
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
        data: ActivePlanData? = activePlan.value?.data
    ): ClassEditUiState {
        val calendar = data?.calendarForAssignment(value.semesterProgramId)
        return value.copy(
            courseOptions = data?.courseOptions(activePlan.value?.display?.hiddenProgramIds.orEmpty()).orEmpty(),
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

private fun ActivePlanData.courseOptions(hiddenProgramIds: Set<String>): List<ClassCourseOptionUi> =
    semesterPrograms.mapNotNull { assignment ->
        val program = courses.firstOrNull { it.id == assignment.studyProgramId }
            ?: return@mapNotNull null
        ClassCourseOptionUi(assignment.id, program.name, isHidden = program.id in hiddenProgramIds)
    }

private fun defaultClassEditState() = ClassEditUiState()

// Drafts saved before the form kept `Recurrence` stored the old option ids under "recurrenceId".
private fun legacyRecurrenceFromId(id: String?): DomainRecurrence = when (id) {
    "a_week" -> DomainRecurrence.A_WEEK
    "b_week" -> DomainRecurrence.B_WEEK
    "once" -> DomainRecurrence.ONCE
    else -> DomainRecurrence.EVERY_WEEK
}

private const val KEY_EDITING_CLASS_ID = "class_edit_editing_class_id"
private const val KEY_OPEN_STATE = "class_edit_open_state"
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
    "recurrence" to { it.recurrence.name },
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
    val recurrence = field("recurrence")?.let { DomainRecurrence.valueOf(it) }
        ?: legacyRecurrenceFromId(field("recurrenceId"))
    return defaultClassEditState().copy(
        title = field("title") ?: "Dodaj zajęcia",
        name = field("name").orEmpty(),
        courseName = field("courseName").orEmpty(),
        semesterProgramId = field("semesterProgramId"),
        type = field("type").orEmpty(),
        dayLabel = field("dayLabel").orEmpty(),
        startTime = field("startTime").orEmpty(),
        endTime = field("endTime").orEmpty(),
        recurrence = recurrence,
        occurrenceDate = field("occurrenceDate").orEmpty(),
        room = field("room").orEmpty(),
        building = field("building").orEmpty(),
        group = field("group").orEmpty(),
        teacher = field("teacher").orEmpty(),
        note = field("note").orEmpty()
    )
}
