package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.domain.ClassForm
import dev.retza.mak.domain.ClassValidationError
import dev.retza.mak.domain.ClassValidator
import dev.retza.mak.domain.CollisionDetector
import dev.retza.mak.domain.Course
import dev.retza.mak.domain.OccurrenceChange
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.OccurrenceNote
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.ScheduleResolver
import dev.retza.mak.domain.Semester
import dev.retza.mak.domain.Teacher
import dev.retza.mak.domain.WeekOverride
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarMarkerColor
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.edit.ClassEditField
import dev.retza.mak.ui.edit.ClassEditUiState
import dev.retza.mak.ui.edit.RecurrenceOptionUi
import dev.retza.mak.ui.schedule.ScheduleDayUi
import dev.retza.mak.ui.schedule.ScheduleFilterUi
import dev.retza.mak.ui.schedule.ScheduleUiState
import dev.retza.mak.ui.schedule.ScheduleView
import dev.retza.mak.ui.settings.SettingsUiState
import dev.retza.mak.ui.settings.ThemeOptionUi
import dev.retza.mak.ui.setup.SetupField
import dev.retza.mak.ui.setup.SetupStep
import dev.retza.mak.ui.setup.SetupWizardUiState
import dev.retza.mak.ui.today.TodayUiState
import dev.retza.mak.ui.occurrence.OccurrenceDetailsUiState
import dev.retza.mak.ui.occurrence.OccurrenceStatusUi
import dev.retza.mak.ui.semester.SemesterFormUiState
import dev.retza.mak.ui.semester.SemesterScreenUiState
import dev.retza.mak.ui.semester.WeekOverrideFormUiState
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekOverrideUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.JsonExportCodec
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
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
    val today: TodayUiState = emptyTodayState(),
    val schedule: ScheduleUiState = emptyScheduleState(),
    val editor: ClassEditUiState = defaultEditorState(),
    val occurrence: OccurrenceDetailsUiState = OccurrenceDetailsUiState(),
    val semester: SemesterScreenUiState = SemesterScreenUiState(),
    val selectedClassId: Long? = null,
    val themeId: String = "system",
    val settings: SettingsUiState = SettingsUiState(),
    val setup: SetupWizardUiState = SetupWizardUiState(),
    val activeSemesterData: SemesterWithData? = null
)

private data class Controls(
    val destination: MakDestination = MakDestination.Today,
    val scheduleDate: LocalDate,
    val calendarMonth: YearMonth,
    val calendarDate: LocalDate,
    val todayDate: LocalDate,
    val scheduleView: ScheduleView = ScheduleView.List,
    val courseFilterId: String = "all",
    val showCancelled: Boolean = false,
    val setup: SetupWizardUiState = SetupWizardUiState(),
    val forceSetup: Boolean = false,
    val editor: ClassEditUiState = defaultEditorState(),
    val editorClassId: Long? = null,
    val selectedClassId: Long? = null,
    val selectedOccurrenceDate: LocalDate? = null,
    val selectedNoteDate: LocalDate? = null,
    val occurrenceDraft: OccurrenceDetailsUiState = OccurrenceDetailsUiState(),
    val semesterDraft: SemesterScreenUiState = SemesterScreenUiState(),
    val semesterEditId: Long? = null,
    val semesterToDeleteId: String? = null,
    val themeId: String = "system"
)

@OptIn(ExperimentalCoroutinesApi::class)
class MakViewModel(
    private val repository: MakRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val resolver: ScheduleResolver = ScheduleResolver(),
    private val collisionDetector: CollisionDetector = CollisionDetector()
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val controls = MutableStateFlow(
        Controls(
            scheduleDate = today,
            calendarMonth = YearMonth.from(today),
            calendarDate = today,
            todayDate = today
        )
    )

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

    fun navigate(destination: MakDestination) {
        controls.update { it.copy(destination = destination) }
    }

    fun refreshToday() {
        controls.update { it.copy(todayDate = LocalDate.now(clock)) }
    }

    fun updateSetup(transform: (SetupWizardUiState) -> SetupWizardUiState) {
        controls.update { it.copy(setup = transform(it.setup)) }
    }

    fun setupNext() {
        val setup = uiState.value.setup
        when (setup.step) {
            SetupStep.Semester -> saveSetupSemester(setup)
            SetupStep.Course -> saveSetupCourse(setup)
            SetupStep.Classes -> finishSetup()
        }
    }

    fun setupBack() {
        controls.update { control ->
            val data = uiState.value.activeSemesterData
            val previous = when {
                data != null && data.courses.isNotEmpty() -> SetupStep.Classes
                data != null -> SetupStep.Course
                else -> SetupStep.Semester
            }
            control.copy(setup = control.setup.copy(step = previous, errors = emptyMap()))
        }
    }

    fun finishSetup() {
        controls.update { it.copy(forceSetup = false, destination = MakDestination.Today) }
    }

    fun cancelSetup() {
        controls.update { it.copy(forceSetup = false, destination = MakDestination.Settings) }
    }

    fun startSemesterSetup() {
        controls.update {
            it.copy(
                forceSetup = true,
                destination = MakDestination.Setup,
                setup = SetupWizardUiState()
            )
        }
    }

    fun openNewClass(oneOffDate: LocalDate? = null) {
        val recurrenceId = if (oneOffDate == null) "every_week" else "once"
        controls.update {
            it.copy(
                destination = MakDestination.EditClass,
                editor = defaultEditorState().copy(
                    recurrenceId = recurrenceId,
                    recurrenceLabel = recurrenceLabel(recurrenceId),
                    occurrenceDate = oneOffDate?.toString().orEmpty()
                ),
                editorClassId = null
            )
        }
    }

    fun openEditClass(occurrenceId: String) {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return
        val data = uiState.value.activeSemesterData ?: return
        val item = data.classes.firstOrNull { it.id == classId } ?: return
        val course = data.courses.firstOrNull { it.id == item.courseId }
        val teacher = data.teachers.firstOrNull { it.id == item.teacherId }
        val recurrenceId = when (item.recurrence) {
            dev.retza.mak.data.entity.Recurrence.EVERY_WEEK -> "every_week"
            dev.retza.mak.data.entity.Recurrence.A_WEEK -> "a_week"
            dev.retza.mak.data.entity.Recurrence.B_WEEK -> "b_week"
            dev.retza.mak.data.entity.Recurrence.ONCE -> "once"
        }
        controls.update {
            it.copy(
                destination = MakDestination.EditClass,
                editorClassId = classId,
                editor = defaultEditorState().copy(
                    title = "Edytuj zajęcia",
                    name = item.name,
                    courseName = course?.name.orEmpty(),
                    type = item.type,
                    dayLabel = dayNames[item.dayOfWeek].orEmpty(),
                    startTime = item.startTime.toString(),
                    endTime = item.endTime.toString(),
                    recurrenceId = recurrenceId,
                    recurrenceLabel = recurrenceLabel(recurrenceId),
                    occurrenceDate = item.date?.toString().orEmpty(),
                    room = item.room.orEmpty(),
                    building = item.building.orEmpty(),
                    group = item.group.orEmpty(),
                    teacher = teacher?.name.orEmpty(),
                    note = item.classNote.orEmpty()
                )
            )
        }
    }

    fun openOccurrence(occurrenceId: String) {
        val classId = occurrenceId.substringBefore(':').toLongOrNull() ?: return
        val date = occurrenceId.substringAfter(':', "").toLocalDateOrNull() ?: return
        val data = uiState.value.activeSemesterData ?: return
        val occurrence = resolve(data, date).occurrences.firstOrNull { it.classId == classId.toString() }
        val base = data.classes.firstOrNull { it.id == classId } ?: return
        val existingChange = data.occurrenceChanges.firstOrNull {
            it.classId == classId && (it.originalDate == date || it.targetDate == date)
        }
        val existingNote = data.occurrenceNotes.firstOrNull {
            it.classId == classId && it.occurrenceDate == date
        }
        val status = when {
            existingChange?.kind == dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED -> OccurrenceStatusUi.Cancelled
            base.recurrence == dev.retza.mak.data.entity.Recurrence.ONCE -> OccurrenceStatusUi.OneOff
            existingChange?.targetDate != null && existingChange.targetDate != existingChange.originalDate -> OccurrenceStatusUi.Moved
            existingChange != null -> OccurrenceStatusUi.Changed
            else -> OccurrenceStatusUi.Scheduled
        }
        val details = OccurrenceDetailsUiState(
            subjectName = occurrence?.name ?: base.name,
            courseName = occurrence?.course?.name ?: data.courses.firstOrNull { it.id == base.courseId }?.name.orEmpty(),
            typeLabel = base.type,
            dateLabel = date.format(fullDateFormatter),
            startTime = (occurrence?.startTime ?: base.startTime).toString(),
            endTime = (occurrence?.endTime ?: base.endTime).toString(),
            room = occurrence?.room ?: base.room,
            building = occurrence?.building ?: base.building,
            teacherName = occurrence?.teacher?.name ?: data.teachers.firstOrNull { it.id == base.teacherId }?.name,
            groupName = base.group,
            weekLabel = resolve(data, date).weekType?.let { "Tydzień ${it.name}" },
            originalDateLabel = existingChange?.originalDate?.toString(),
            targetDateLabel = existingChange?.targetDate?.toString(),
            status = status,
            sharedNote = base.classNote,
            occurrenceNote = existingNote?.body,
            occurrenceNoteDraft = existingNote?.body.orEmpty(),
            targetDateDraft = date.toString(),
            startTimeDraft = (occurrence?.startTime ?: base.startTime).toString(),
            endTimeDraft = (occurrence?.endTime ?: base.endTime).toString(),
            roomDraft = (occurrence?.room ?: base.room).orEmpty(),
            canCancelOccurrence = base.recurrence != dev.retza.mak.data.entity.Recurrence.ONCE && existingChange == null,
            canChangeOccurrence = base.recurrence != dev.retza.mak.data.entity.Recurrence.ONCE && status != OccurrenceStatusUi.Cancelled,
            canMoveOccurrence = base.recurrence != dev.retza.mak.data.entity.Recurrence.ONCE && status != OccurrenceStatusUi.Cancelled,
            canRestoreOccurrence = existingChange != null
        )
        controls.update {
            it.copy(
                destination = MakDestination.OccurrenceDetails,
                selectedClassId = classId,
                selectedOccurrenceDate = existingChange?.originalDate ?: date,
                selectedNoteDate = date,
                occurrenceDraft = details
            )
        }
    }

    fun updateOccurrence(transform: (OccurrenceDetailsUiState) -> OccurrenceDetailsUiState) {
        controls.update { it.copy(occurrenceDraft = transform(it.occurrenceDraft)) }
    }

    fun requestClassDeletion() = updateOccurrence { it.copy(showDeleteConfirmation = true) }

    fun cancelClassDeletion() = updateOccurrence { it.copy(showDeleteConfirmation = false) }

    fun deleteSelectedClass() {
        val classId = controls.value.selectedClassId ?: return
        viewModelScope.launch {
            repository.deleteClass(classId)
            controls.update { it.copy(destination = MakDestination.Schedule) }
        }
    }

    fun cancelSelectedOccurrence() = saveOccurrenceChange(cancelled = true, move = false)

    fun changeSelectedOccurrence() = saveOccurrenceChange(cancelled = false, move = false)

    fun moveSelectedOccurrence() = saveOccurrenceChange(cancelled = false, move = true)

    fun restoreSelectedOccurrence() {
        val data = uiState.value.activeSemesterData ?: return
        val classId = controls.value.selectedClassId ?: return
        val date = controls.value.selectedOccurrenceDate ?: return
        val change = data.occurrenceChanges.firstOrNull { it.classId == classId && it.originalDate == date } ?: return
        viewModelScope.launch {
            repository.deleteOccurrenceChange(change.id)
            controls.update { it.copy(destination = MakDestination.Schedule) }
        }
    }

    fun saveOccurrenceNote() {
        val data = uiState.value.activeSemesterData ?: return
        val classId = controls.value.selectedClassId ?: return
        val date = controls.value.selectedNoteDate ?: return
        val body = controls.value.occurrenceDraft.occurrenceNoteDraft.trim()
        if (body.isEmpty()) return
        val existing = data.occurrenceNotes.firstOrNull { it.classId == classId && it.occurrenceDate == date }
        viewModelScope.launch {
            repository.saveOccurrenceNote(
                OccurrenceNoteEntity(existing?.id ?: 0, data.semester.id, classId, date, body)
            )
            controls.update { it.copy(destination = MakDestination.Schedule) }
        }
    }

    fun deleteOccurrenceNote() {
        val data = uiState.value.activeSemesterData ?: return
        val classId = controls.value.selectedClassId ?: return
        val date = controls.value.selectedNoteDate ?: return
        val note = data.occurrenceNotes.firstOrNull { it.classId == classId && it.occurrenceDate == date } ?: return
        viewModelScope.launch {
            repository.deleteOccurrenceNote(note.id)
            controls.update { it.copy(destination = MakDestination.Schedule) }
        }
    }

    private fun saveOccurrenceChange(cancelled: Boolean, move: Boolean) {
        val data = uiState.value.activeSemesterData ?: return
        val classId = controls.value.selectedClassId ?: return
        val date = controls.value.selectedOccurrenceDate ?: return
        val draft = controls.value.occurrenceDraft
        val start = draft.startTimeDraft.toLocalTimeOrNull() ?: return
        val end = draft.endTimeDraft.toLocalTimeOrNull() ?: return
        if (!end.isAfter(start)) return
        val existing = data.occurrenceChanges.firstOrNull { it.classId == classId && it.originalDate == date }
        val target = if (move) draft.targetDateDraft.toLocalDateOrNull() ?: return else existing?.targetDate ?: date
        viewModelScope.launch {
            repository.saveOccurrenceChange(
                OccurrenceChangeEntity(
                    id = existing?.id ?: 0,
                    semesterId = data.semester.id,
                    classId = classId,
                    originalDate = date,
                    kind = if (cancelled) dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED else dev.retza.mak.data.entity.OccurrenceChangeKind.MODIFIED,
                    targetDate = if (cancelled) null else target,
                    newStartTime = if (cancelled) null else start,
                    newEndTime = if (cancelled) null else end,
                    newRoom = if (cancelled) null else draft.roomDraft.trim().ifEmpty { null },
                    newBuilding = null,
                    newTeacherId = null,
                    newNote = null
                )
            )
            controls.update { it.copy(destination = MakDestination.Schedule) }
        }
    }

    fun openNewClassForSelectedCalendarDay() {
        openNewClass(controls.value.calendarDate)
    }

    fun updateEditor(transform: (ClassEditUiState) -> ClassEditUiState) {
        controls.update { it.copy(editor = transform(it.editor).copy(errors = emptyMap())) }
    }

    fun saveClass() {
        val data = uiState.value.activeSemesterData ?: return
        val editor = controls.value.editor
        val start = editor.startTime.toLocalTimeOrNull()
        val end = editor.endTime.toLocalTimeOrNull()
        val recurrence = recurrenceFromId(editor.recurrenceId)
        val date = editor.occurrenceDate.toLocalDateOrNull()
        val course = data.courses.firstOrNull { it.name == editor.courseName }
        val validation = ClassValidator.validate(
            ClassForm(
                name = editor.name,
                courseId = course?.id?.toString(),
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
            if (ClassValidationError.COURSE_REQUIRED in validation.errors) {
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
            if (recurrence == Recurrence.ONCE && date != null &&
                (date.isBefore(data.semester.startDate) || date.isAfter(data.semester.endDate))
            ) {
                put(ClassEditField.Date, FieldErrorUi("Data musi należeć do aktywnego semestru."))
            }
            if (editor.dayLabel !in dayNames.values) {
                put(ClassEditField.Day, FieldErrorUi("Wybierz dzień tygodnia."))
            }
            if (editor.type.isBlank()) {
                put(ClassEditField.Type, FieldErrorUi("Wybierz typ zajęć."))
            }
        }
        if (errors.isNotEmpty() || course == null || start == null || end == null) {
            controls.update { it.copy(editor = editor.copy(errors = errors)) }
            return
        }

        viewModelScope.launch {
            val teacherId = editor.teacher.trim().takeIf(String::isNotEmpty)?.let { name ->
                data.teachers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
                    ?: repository.saveTeacher(
                        TeacherEntity(semesterId = data.semester.id, name = name)
                    )
            }
            repository.saveClass(
                ClassEntity(
                    id = controls.value.editorClassId ?: 0,
                    semesterId = data.semester.id,
                    name = editor.name.trim(),
                    type = editor.type,
                    courseId = course.id,
                    teacherId = teacherId,
                    dayOfWeek = dayNames.entries.first { it.value == editor.dayLabel }.key,
                    startTime = start,
                    endTime = end,
                    room = editor.room.trim().ifEmpty { null },
                    building = editor.building.trim().ifEmpty { null },
                    group = editor.group.trim().ifEmpty { null },
                    recurrence = dev.retza.mak.data.entity.Recurrence.valueOf(recurrence.name),
                    date = if (recurrence == Recurrence.ONCE) date else null,
                    classNote = editor.note.trim().ifEmpty { null }
                )
            )
            controls.update {
                it.copy(destination = MakDestination.Today, editor = defaultEditorState(), editorClassId = null)
            }
        }
    }

    fun selectScheduleView(view: ScheduleView) {
        controls.update { it.copy(scheduleView = view) }
    }

    fun changeWeek(amount: Long) {
        controls.update { it.copy(scheduleDate = it.scheduleDate.plusWeeks(amount)) }
    }

    fun selectScheduleDay(id: String) {
        id.toLocalDateOrNull()?.let { date -> controls.update { it.copy(scheduleDate = date) } }
    }

    fun selectCourseFilter(id: String) {
        controls.update { it.copy(courseFilterId = id) }
    }

    fun changeMonth(amount: Long) {
        controls.update { control ->
            val month = control.calendarMonth.plusMonths(amount)
            control.copy(calendarMonth = month, calendarDate = month.atDay(1))
        }
    }

    fun selectCalendarDay(id: String) {
        id.toLocalDateOrNull()?.let { date ->
            controls.update { it.copy(calendarDate = date, calendarMonth = YearMonth.from(date)) }
        }
    }

    fun setShowCancelled(value: Boolean) {
        controls.update { it.copy(showCancelled = value) }
    }

    fun saveVisibleWeekOverride(weekType: WeekTypeUi, scope: WeekOverrideScopeUi) {
        val data = uiState.value.activeSemesterData ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.weekStartDate == monday && it.scope == entityScope
        }
        viewModelScope.launch {
            repository.saveWeekOverride(
                WeekOverrideEntity(
                    id = existing?.id ?: 0,
                    semesterId = data.semester.id,
                    weekStartDate = monday,
                    weekType = dev.retza.mak.data.entity.WeekType.valueOf(weekType.name),
                    scope = entityScope
                )
            )
        }
    }

    fun clearVisibleWeekOverride(scope: WeekOverrideScopeUi) {
        val data = uiState.value.activeSemesterData ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.weekStartDate == monday && it.scope == entityScope
        } ?: return
        viewModelScope.launch { repository.deleteWeekOverride(existing.id) }
    }

    fun selectSemester(id: String) {
        id.toLongOrNull()?.let { semesterId ->
            controls.update { it.copy(courseFilterId = "all") }
            viewModelScope.launch { repository.setActiveSemester(semesterId) }
        }
    }

    fun requestSemesterDeletion(id: String) {
        controls.update { it.copy(semesterToDeleteId = id) }
    }

    fun cancelSemesterDeletion() {
        controls.update { it.copy(semesterToDeleteId = null) }
    }

    fun confirmSemesterDeletion() {
        val id = controls.value.semesterToDeleteId?.toLongOrNull() ?: return
        val remaining = uiState.value.settings.semesters.firstOrNull { it.id != id.toString() }
        viewModelScope.launch {
            repository.deleteSemester(id)
            if (remaining != null) repository.setActiveSemester(remaining.id.toLong())
            else repository.clearActiveSemester()
            controls.update {
                it.copy(
                    semesterToDeleteId = null,
                    destination = if (remaining == null) MakDestination.Setup else MakDestination.Settings,
                    forceSetup = remaining == null,
                    setup = if (remaining == null) SetupWizardUiState() else it.setup
                )
            }
        }
    }

    fun selectTheme(id: String) {
        controls.update { it.copy(themeId = id) }
    }

    fun openSemesterConfiguration(id: String) {
        val semesterId = id.toLongOrNull() ?: return
        viewModelScope.launch {
            repository.setActiveSemester(semesterId)
            val data = repository.observeSemesterData(semesterId).first() ?: return@launch
            controls.update {
                it.copy(
                    destination = MakDestination.Semester,
                    semesterEditId = semesterId,
                    semesterDraft = SemesterScreenUiState(
                        semester = SemesterFormUiState(
                            name = data.semester.name,
                            startDate = data.semester.startDate.toString(),
                            endDate = data.semester.endDate.toString(),
                            firstWeek = WeekTypeUi.valueOf(data.semester.firstWeekType.name)
                        ),
                        overrides = data.weekOverrides.map { override ->
                            WeekOverrideUi(
                                override.id.toString(),
                                override.weekStartDate.toString(),
                                WeekTypeUi.valueOf(override.weekType.name),
                                WeekOverrideScopeUi.valueOf(override.scope.name)
                            )
                        },
                        courses = data.courses.map { course -> course.id.toString() to course.name }
                    )
                )
            }
        }
    }

    fun updateSemester(transform: (SemesterScreenUiState) -> SemesterScreenUiState) {
        controls.update { it.copy(semesterDraft = transform(it.semesterDraft)) }
    }

    fun saveSemesterConfiguration() {
        val id = controls.value.semesterEditId ?: return
        val draft = controls.value.semesterDraft.semester
        val start = draft.startDate.toLocalDateOrNull()
        val end = draft.endDate.toLocalDateOrNull()
        if (draft.name.isBlank() || start == null || end == null || end.isBefore(start)) {
            updateSemester {
                it.copy(semester = draft.copy(
                    nameError = if (draft.name.isBlank()) "Podaj nazwę semestru." else null,
                    startDateError = if (start == null) "Podaj poprawną datę." else null,
                    endDateError = if (end == null) "Podaj poprawną datę." else null,
                    dateRangeError = if (start != null && end != null && end.isBefore(start)) "Koniec nie może być wcześniejszy od początku." else null
                ))
            }
            return
        }
        viewModelScope.launch {
            repository.saveSemester(
                SemesterEntity(id, draft.name.trim(), start, end, dev.retza.mak.data.entity.WeekType.valueOf(draft.firstWeek.name), true)
            )
            controls.update { it.copy(destination = MakDestination.Settings) }
        }
    }

    fun newWeekOverride() = updateSemester { it.copy(overrideForm = WeekOverrideFormUiState()) }

    fun editWeekOverride(id: String) {
        val item = controls.value.semesterDraft.overrides.firstOrNull { it.id == id } ?: return
        updateSemester {
            it.copy(overrideForm = WeekOverrideFormUiState(item.id, item.weekStartDate, item.weekType, item.scope))
        }
    }

    fun cancelWeekOverrideEdit() = newWeekOverride()

    fun saveWeekOverride() {
        val semesterId = controls.value.semesterEditId ?: return
        val form = controls.value.semesterDraft.overrideForm
        val date = form.weekStartDate.toLocalDateOrNull()
        if (date == null || date.dayOfWeek != DayOfWeek.MONDAY) {
            updateSemester { it.copy(overrideForm = form.copy(weekStartDateError = "Wybierz poniedziałek w formacie RRRR-MM-DD.")) }
            return
        }
        viewModelScope.launch {
            repository.saveWeekOverride(
                WeekOverrideEntity(
                    id = form.id?.toLongOrNull() ?: 0,
                    semesterId = semesterId,
                    weekStartDate = date,
                    weekType = dev.retza.mak.data.entity.WeekType.valueOf(form.weekType.name),
                    scope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(form.scope.name)
                )
            )
            controls.update { it.copy(destination = MakDestination.Settings) }
        }
    }

    fun deleteWeekOverride(id: String) {
        id.toLongOrNull()?.let { overrideId -> viewModelScope.launch { repository.deleteWeekOverride(overrideId) } }
        controls.update { it.copy(destination = MakDestination.Settings) }
    }

    fun addCourse() {
        val data = uiState.value.activeSemesterData ?: return
        val draft = controls.value.semesterDraft
        if (draft.courseNameDraft.isBlank()) return
        viewModelScope.launch {
            repository.saveCourse(
                CourseEntity(
                    semesterId = data.semester.id,
                    name = draft.courseNameDraft.trim(),
                    color = draft.courseColorDraft.ifBlank { "#137b71" }
                )
            )
            controls.update { it.copy(semesterDraft = it.semesterDraft.copy(courseNameDraft = "")) }
        }
    }

    fun deleteCourse(id: String) {
        id.toLongOrNull()?.let { courseId -> viewModelScope.launch { repository.deleteCourse(courseId) } }
    }

    fun exportJson(onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            onReady(JsonExportCodec.encode(ExportSnapshot.from(repository.getAllSemesterData())))
        }
    }

    private fun saveSetupSemester(setup: SetupWizardUiState) {
        val start = setup.startDate.toLocalDateOrNull()
        val end = setup.endDate.toLocalDateOrNull()
        val errors = buildMap {
            if (setup.semesterName.isBlank()) put(SetupField.SemesterName, FieldErrorUi("Podaj nazwę semestru."))
            if (start == null) put(SetupField.StartDate, FieldErrorUi("Podaj datę w formacie RRRR-MM-DD."))
            if (end == null || start != null && end.isBefore(start)) {
                put(SetupField.EndDate, FieldErrorUi("Data końca nie może być wcześniejsza od początku."))
            }
        }
        if (errors.isNotEmpty() || start == null || end == null) {
            controls.update { it.copy(setup = setup.copy(errors = errors)) }
            return
        }
        viewModelScope.launch {
            repository.saveSemester(
                SemesterEntity(
                    name = setup.semesterName.trim(),
                    startDate = start,
                    endDate = end,
                    firstWeekType = dev.retza.mak.data.entity.WeekType.valueOf(setup.firstWeekLabel),
                    isActive = true
                )
            )
            controls.update {
                it.copy(setup = setup.copy(step = SetupStep.Course, errors = emptyMap()))
            }
        }
    }

    private fun saveSetupCourse(setup: SetupWizardUiState) {
        val semesterId = uiState.value.activeSemesterData?.semester?.id
        val errors = if (setup.courseName.isBlank()) {
            mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku."))
        } else emptyMap()
        if (semesterId == null || errors.isNotEmpty()) {
            controls.update { it.copy(setup = setup.copy(errors = errors)) }
            return
        }
        viewModelScope.launch {
            repository.saveCourse(
                CourseEntity(
                    semesterId = semesterId,
                    name = setup.courseName.trim(),
                    color = setup.courseColor.ifBlank { "#137b71" }
                )
            )
            controls.update {
                it.copy(setup = setup.copy(step = SetupStep.Classes, errors = emptyMap()))
            }
        }
    }

    private fun buildState(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): MakUiState {
        val requiresSetup = control.forceSetup || semesterList.isEmpty() ||
            activeData != null && activeData.courses.isEmpty()
        val destination = if (requiresSetup) MakDestination.Setup else control.destination
        val editor = control.editor.copy(courseOptions = activeData?.courses?.map { it.name }.orEmpty())
        return MakUiState(
            destination = destination,
            requiresSetup = requiresSetup,
            today = buildToday(activeData, control.todayDate),
            schedule = buildSchedule(activeData, control),
            editor = editor,
            occurrence = control.occurrenceDraft,
            selectedClassId = control.selectedClassId,
            themeId = control.themeId,
            semester = control.semesterDraft.copy(
                overrides = activeData?.weekOverrides?.map { override ->
                    WeekOverrideUi(
                        override.id.toString(),
                        override.weekStartDate.toString(),
                        WeekTypeUi.valueOf(override.weekType.name),
                        WeekOverrideScopeUi.valueOf(override.scope.name)
                    )
                }.orEmpty(),
                courses = activeData?.courses?.map { it.id.toString() to it.name }.orEmpty()
            ),
            settings = buildSettings(semesterList, activeData, control),
            setup = if (activeData != null && activeData.courses.isEmpty()) {
                control.setup.copy(step = SetupStep.Course)
            } else control.setup,
            activeSemesterData = activeData
        )
    }

    private fun buildToday(data: SemesterWithData?, date: LocalDate): TodayUiState {
        if (data == null) return emptyTodayState()
        val schedule = resolve(data, date)
        val collisions = collisionDetector.detect(schedule).flatMap { listOf(it.first.id, it.second.id) }.toSet()
        return TodayUiState(
            dateLabel = date.format(todayTitleFormatter).replaceFirstChar { it.titlecase(polishLocale) },
            semesterLabel = data.semester.name,
            weekLabel = schedule.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem",
            summaryLabel = classCountLabel(schedule.occurrences.size),
            items = schedule.occurrences.map { it.toUi(it.id in collisions) }
        )
    }

    private fun buildSchedule(data: SemesterWithData?, control: Controls): ScheduleUiState {
        if (data == null) return emptyScheduleState()
        val selected = resolve(data, control.scheduleDate)
        val filtered = selected.occurrences.filter {
            control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
        }
        val cancelled = if (control.showCancelled) cancelledItems(data, control.scheduleDate) else emptyList()
        val collisions = collisionDetector.detect(selected).flatMap { listOf(it.first.id, it.second.id) }.toSet()
        val monday = control.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val calendarDays = calendarDates(control.calendarMonth).map { date ->
            val occurrences = resolve(data, date).occurrences.filter {
                control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
            }
            CalendarDayUi(
                id = date.toString(),
                dayLabel = date.dayOfMonth.toString(),
                accessibilityLabel = "${date.format(fullDateFormatter)}, ${classCountLabel(occurrences.size)}",
                isInCurrentMonth = YearMonth.from(date) == control.calendarMonth,
                isToday = date == today,
                isSelected = date == control.calendarDate,
                markers = occurrences.take(3).map {
                    CalendarMarkerUi(
                        id = it.id,
                        contentDescription = it.name,
                        colorToken = markerColor(data, it)
                    )
                }
            )
        }
        val calendarSchedule = resolve(data, control.calendarDate)
        val calendarFiltered = calendarSchedule.occurrences.filter {
            control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
        }
        return ScheduleUiState(
            view = control.scheduleView,
            weekRangeLabel = "${monday.format(shortDateFormatter)} - ${monday.plusDays(6).format(shortDateFormatter)}",
            weekSubtitle = if (monday == currentWeekMonday) "Bieżący tydzień" else data.semester.name,
            weekTypeLabel = selected.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem",
            weekSourceLabel = if (selected.correction == null) "Wyliczony automatycznie" else "Korekta ręczna",
            days = (0L..6L).map { offset ->
                val date = monday.plusDays(offset)
                ScheduleDayUi(
                    id = date.toString(),
                    shortLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, polishLocale),
                    dateLabel = date.dayOfMonth.toString(),
                    accessibilityLabel = date.format(fullDateFormatter),
                    isSelected = date == control.scheduleDate,
                    isEnabled = !date.isBefore(data.semester.startDate) && !date.isAfter(data.semester.endDate)
                )
            },
            filters = listOf(ScheduleFilterUi("all", "Wszystkie", control.courseFilterId == "all")) +
                data.courses.map {
                    ScheduleFilterUi(it.id.toString(), it.name, control.courseFilterId == it.id.toString())
                },
            selectedDayLabel = dayNames[control.scheduleDate.dayOfWeek].orEmpty(),
            selectedDayCountLabel = classCountLabel(filtered.size + cancelled.size),
            items = filtered.map { it.toUi(it.id in collisions) } + cancelled,
            calendarMonthLabel = control.calendarMonth.format(monthFormatter),
            calendarDays = calendarDays,
            calendarSelectedDayLabel = control.calendarDate.format(fullDateFormatter),
            calendarSelectedDayCountLabel = classCountLabel(calendarFiltered.size),
            calendarItems = calendarFiltered.map { it.toUi(false) } +
                if (control.showCancelled) cancelledItems(data, control.calendarDate) else emptyList(),
            showCancelled = control.showCancelled,
            hasOneWeekCorrection = data.weekOverrides.any {
                it.weekStartDate == monday && it.scope == dev.retza.mak.data.entity.WeekOverrideScope.ONE_WEEK
            },
            hasFromWeekCorrection = data.weekOverrides.any {
                it.weekStartDate == monday && it.scope == dev.retza.mak.data.entity.WeekOverrideScope.FROM_WEEK
            }
        )
    }

    private fun buildSettings(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): SettingsUiState = SettingsUiState(
        semesters = semesterList.map { semester ->
            val isActive = semester.id == activeData?.semester?.id
            SemesterUi(
                id = semester.id.toString(),
                name = semester.name,
                dateRangeLabel = "${semester.startDate.format(shortDateFormatter)} - ${semester.endDate.format(shortDateFormatter)}",
                firstWeekLabel = "Pierwszy tydzień ${semester.firstWeekType.name}",
                courseCountLabel = if (isActive) "${activeData.courses.size} kierunków" else "Dane odizolowane",
                classCountLabel = if (isActive) classCountLabel(activeData.classes.size) else "Osobny plan",
                isActive = isActive
            )
        },
        activeSemesterId = activeData?.semester?.id?.toString(),
        themeOptions = listOf(
            ThemeOptionUi("system", "Systemowy", control.themeId == "system"),
            ThemeOptionUi("light", "Jasny", control.themeId == "light"),
            ThemeOptionUi("dark", "Ciemny", control.themeId == "dark")
        ),
        semesterToDeleteId = control.semesterToDeleteId
    )

    private fun cancelledItems(data: SemesterWithData, date: LocalDate): List<ClassItemUi> =
        data.occurrenceChanges.filter {
            it.originalDate == date && it.kind == dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED
        }.mapNotNull { change ->
            val item = data.classes.firstOrNull { it.id == change.classId } ?: return@mapNotNull null
            val course = data.courses.firstOrNull { it.id == item.courseId }
            val teacher = data.teachers.firstOrNull { it.id == item.teacherId }
            ClassItemUi(
                id = "${item.id}:$date",
                name = item.name,
                type = item.type,
                courseName = course?.name.orEmpty(),
                courseColor = course?.color,
                startTime = item.startTime.toString(),
                endTime = item.endTime.toString(),
                room = item.room,
                teacherName = teacher?.name,
                note = item.classNote,
                statusBadge = "Odwołane",
                isCancelled = true
            )
        }

    private fun resolve(data: SemesterWithData, date: LocalDate) = resolver.resolve(
        date = date,
        semester = data.semester.toDomain(),
        classes = data.classes.map { it.toDomain() },
        courses = data.courses.map { it.toDomain() },
        teachers = data.teachers.map { it.toDomain() },
        weekOverrides = data.weekOverrides.map { it.toDomain() },
        occurrenceChanges = data.occurrenceChanges.map { it.toDomain() },
        occurrenceNotes = data.occurrenceNotes.map { it.toDomain() }
    )

    class Factory(private val repository: MakRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MakViewModel::class.java))
            return MakViewModel(repository) as T
        }
    }
}

private fun markerColor(
    data: SemesterWithData,
    occurrence: PlannedOccurrence
): CalendarMarkerColor {
    if (occurrence.occurrenceChange != null) return CalendarMarkerColor.Error
    val index = data.courses.indexOfFirst { it.id.toString() == occurrence.course?.id }
    return if (index > 0) CalendarMarkerColor.Secondary else CalendarMarkerColor.Primary
}

private fun SemesterEntity.toDomain() = Semester(
    id = id.toString(),
    name = name,
    startDate = startDate,
    endDate = endDate,
    firstWeekType = WeekType.valueOf(firstWeekType.name)
)

private fun CourseEntity.toDomain() = Course(id.toString(), semesterId.toString(), name, color)

private fun TeacherEntity.toDomain() = Teacher(id.toString(), semesterId.toString(), name)

private fun ClassEntity.toDomain() = dev.retza.mak.domain.ClassItem(
    id = id.toString(),
    semesterId = semesterId.toString(),
    name = name,
    type = type,
    courseId = courseId.toString(),
    teacherId = teacherId?.toString(),
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    room = room,
    building = building,
    group = group,
    recurrence = Recurrence.valueOf(recurrence.name),
    date = date,
    classNote = classNote
)

private fun dev.retza.mak.data.entity.WeekOverrideEntity.toDomain() = WeekOverride(
    id = id.toString(),
    semesterId = semesterId.toString(),
    weekStartDate = weekStartDate,
    weekType = WeekType.valueOf(weekType.name),
    scope = WeekOverrideScope.valueOf(scope.name)
)

private fun dev.retza.mak.data.entity.OccurrenceNoteEntity.toDomain() = OccurrenceNote(
    id = id.toString(),
    classId = classId.toString(),
    occurrenceDate = occurrenceDate,
    body = body
)

private fun dev.retza.mak.data.entity.OccurrenceChangeEntity.toDomain() = OccurrenceChange(
    id = id.toString(),
    classId = classId.toString(),
    originalDate = originalDate,
    kind = OccurrenceChangeKind.valueOf(kind.name),
    targetDate = targetDate,
    startTime = newStartTime,
    endTime = newEndTime,
    room = newRoom,
    building = newBuilding,
    teacherId = newTeacherId?.toString(),
    note = newNote
)

private fun PlannedOccurrence.toUi(hasCollision: Boolean): ClassItemUi {
    val cancelled = occurrenceChange?.kind == OccurrenceChangeKind.CANCELLED
    val modified = occurrenceChange?.kind == OccurrenceChangeKind.MODIFIED
    val oneOff = classItem.recurrence == Recurrence.ONCE
    return ClassItemUi(
        id = id,
        name = name,
        type = classItem.type,
        courseName = course?.name.orEmpty(),
        courseColor = course?.color,
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        room = room,
        teacherName = teacher?.name,
        weekLabel = when (classItem.recurrence) {
            Recurrence.A_WEEK -> "Tydzień A"
            Recurrence.B_WEEK -> "Tydzień B"
            Recurrence.ONCE -> "Jednorazowe"
            Recurrence.EVERY_WEEK -> null
        },
        note = occurrenceNoteBody ?: classNote,
        statusBadge = when {
            cancelled -> "Odwołane"
            oneOff -> "Jednorazowe"
            modified -> "Zmienione"
            else -> null
        },
        isCancelled = cancelled,
        isModified = modified,
        isOneOff = oneOff,
        hasConflict = hasCollision
    )
}

private fun defaultEditorState() = ClassEditUiState(
    typeOptions = listOf("Wykład", "Ćwiczenia", "Laboratorium", "Projekt", "Seminarium", "Inne"),
    dayOptions = dayNames.values.toList(),
    recurrenceOptions = listOf(
        RecurrenceOptionUi("every_week", "Co tydzień"),
        RecurrenceOptionUi("a_week", "Tydzień A"),
        RecurrenceOptionUi("b_week", "Tydzień B"),
        RecurrenceOptionUi("once", "Jednorazowo")
    )
)

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    summaryLabel = "0 zajęć"
)

private fun emptyScheduleState() = ScheduleUiState(
    weekRangeLabel = "",
    weekSubtitle = "",
    weekTypeLabel = "",
    weekSourceLabel = "",
    status = ScreenStatus.Ready
)

private fun recurrenceFromId(id: String): Recurrence = when (id) {
    "a_week" -> Recurrence.A_WEEK
    "b_week" -> Recurrence.B_WEEK
    "once" -> Recurrence.ONCE
    else -> Recurrence.EVERY_WEEK
}

private fun recurrenceLabel(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

private fun calendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L until 42L).map(first::plusDays)
}

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}

private val polishLocale = Locale.forLanguageTag("pl-PL")
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", polishLocale)
private val todayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polishLocale)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", polishLocale)
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", polishLocale)
private val dayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Poniedziałek",
    DayOfWeek.TUESDAY to "Wtorek",
    DayOfWeek.WEDNESDAY to "Środa",
    DayOfWeek.THURSDAY to "Czwartek",
    DayOfWeek.FRIDAY to "Piątek",
    DayOfWeek.SATURDAY to "Sobota",
    DayOfWeek.SUNDAY to "Niedziela"
)
