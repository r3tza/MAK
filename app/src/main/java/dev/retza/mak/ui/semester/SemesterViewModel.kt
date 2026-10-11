package dev.retza.mak.ui.semester

import dev.retza.mak.ui.components.DefaultCourseColor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import dev.retza.mak.domain.validateDateRange
import dev.retza.mak.domain.weekStart
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.ui.FormErrorTexts
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.shortDateWithYearFormatter
import dev.retza.mak.domain.toLocalDateOrNull
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.freedays.freeDaysSummary
import dev.retza.mak.ui.freedays.groupFreeDays
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SemesterEffect {
    data object CloseConfiguration : SemesterEffect
    data object CourseAdded : SemesterEffect
}

/**
 * Shared by the semester screens through the semester navigation graph. It reads the semester from the
 * configuration stream, so writes need no manual refresh and changes from other places reach the screens.
 */
@KoinViewModel
class SemesterViewModel(
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink,
    planEditTracker: PlanEditTracker,
    savedState: SavedStateHandle
) : ViewModel() {
    private val state = MutableStateFlow(SemesterScreenUiState())
    val semester: StateFlow<SemesterScreenUiState> = state.asStateFlow()

    // The route argument is text; a value that is not a number leaves the screen empty.
    private val semesterId: Long? = savedState.get<String>("semesterId")?.toLongOrNull()

    private val effectsChannel = Channel<SemesterEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var configuration: SemesterConfiguration? = null
    private var selectedCalendarId: Long? = null
    // True while a course form is open: the form is a draft that holds plan edits until it closes.
    private var courseFormOpen = false

    // Every edit action below runs through the session: it reloads the semester before the first draft.
    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { refreshForEdit() },
        hasDraft = { state.value.hasPlanDraft() || courseFormOpen }
    )

    init {
        val id = semesterId
        if (id != null) {
            viewModelScope.launch {
                semesterRepository.observeSemesterConfiguration(id).collect { loaded ->
                    if (loaded == null) clear() else apply(loaded)
                }
            }
        }
        viewModelScope.launch {
            state.map { it.hasPlanDraft() }.distinctUntilChanged().collect { hasDraft ->
                if (!hasDraft) editSession.releaseIfNoDraft()
            }
        }
    }

    override fun onCleared() {
        editSession.close()
    }

    /** Reads the configuration once before the first edit of an edit admission. Returns false when the semester is gone. */
    suspend fun refreshForEdit(): Boolean {
        val id = semesterId ?: return false
        val loaded = semesterRepository.observeSemesterConfiguration(id).first()
        if (loaded == null) {
            clear()
            return false
        }
        // The first keystroke in the semester form admits the screen before this refresh; keep what it typed.
        apply(loaded, keepSemesterForm = configuration != null)
        return true
    }

    /**
     * Admits the course form when it opens. A form opened by [openAddCourse] or [openCourseEdit] is already admitted.
     * Returns false when the semester is gone; the screen then leaves.
     */
    suspend fun openCourseFormOnce(): Boolean {
        if (courseFormOpen) return true
        courseFormOpen = editSession.admitForm()
        return courseFormOpen
    }

    /** Closes the course form and releases plan edits when no other draft is left. */
    fun endCourseForm() {
        courseFormOpen = false
        viewModelScope.launch { editSession.releaseIfNoDraft() }
    }

    fun cancelCourseForm() {
        resetCourseDraft()
        endCourseForm()
    }

    fun openAddCourse(navigate: () -> Unit) = editSession.run {
        resetCourseDraft()
        courseFormOpen = true
        navigate()
    }

    fun openCourseEdit(navigate: () -> Unit) = editSession.run {
        courseFormOpen = true
        navigate()
    }

    fun updateSemesterName(value: String) = editSession.edit {
        update { it.copy(semester = it.semester.copy(name = value)) }
    }

    fun updateSemesterStartDate(value: String) = editSession.edit {
        update { it.copy(semester = it.semester.copy(startDate = value)) }
    }

    fun updateSemesterEndDate(value: String) = editSession.edit {
        update { it.copy(semester = it.semester.copy(endDate = value)) }
    }

    fun updateSemesterFirstWeek(value: WeekType) = editSession.run {
        update { it.copy(semester = it.semester.copy(firstWeek = value)) }
    }

    fun updateOverrideWeekType(value: WeekType) = editSession.run {
        update { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
    }

    fun updateOverrideScope(value: WeekOverrideScope) = editSession.run {
        update { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
    }

    private fun clear() {
        configuration = null
        selectedCalendarId = null
        state.value = SemesterScreenUiState()
    }

    /** Rebuilds the screen from [loaded], keeping the calendar choice and unsaved drafts. */
    private fun apply(loaded: SemesterConfiguration, keepSemesterForm: Boolean = false) {
        configuration = loaded
        selectedCalendarId = loaded.calendars.firstOrNull { it.id == selectedCalendarId }?.id
            ?: loaded.sharedCalendar()?.id
        val current = state.value
        val courseCalendarId = current.courseCalendarId
            ?.takeIf { value -> loaded.calendars.any { it.id == value } }
            ?: selectedCalendarId
        val fresh = loaded.toSemesterScreenState(selectedCalendarId)
        state.value = fresh.copy(
            semester = if (keepSemesterForm) current.semester else fresh.semester,
            courseNameDraft = current.courseNameDraft,
            courseColorDraft = current.courseColorDraft,
            courseCalendarMode = current.courseCalendarMode,
            courseCalendarId = courseCalendarId,
            courseProgramMode = current.courseProgramMode,
            courseProgramId = current.courseProgramId,
            overrideForm = current.overrideForm
        )
    }

    fun update(transform: (SemesterScreenUiState) -> SemesterScreenUiState) {
        state.update { transform(it) }
    }

    fun saveSemester() {
        if (state.value.semester.isSaving) return
        val id = semesterId ?: return
        val currentCalendarId = selectedCalendarId ?: return
        val form = state.value.semester
        val start = form.startDate.toLocalDateOrNull()
        val end = form.endDate.toLocalDateOrNull()
        val nameValid = form.name.isNotBlank()
        val dateErrors = validateDateRange(start, end)
        if (!nameValid || dateErrors.isNotEmpty() || start == null || end == null) {
            val texts = FormErrorTexts.dateRange(dateErrors)
            update {
                it.copy(semester = form.copy(
                    nameError = if (nameValid) null else FormErrorTexts.SemesterNameRequired,
                    startDateError = texts.start,
                    endDateError = texts.end,
                    dateRangeError = texts.range
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
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać semestru.",
            onFinish = {
                update { it.copy(semester = it.semester.copy(isSaving = false)) }
            },
            onError = {
                update {
                    it.copy(semester = it.semester.copy(dateRangeError = "Nie udało się zapisać semestru."))
                }
            }
        ) {
            semesterRepository.updateSemesterWithCalendar(
                SemesterRecord(id = id, name = form.name.trim()),
                AcademicCalendarRecord(
                    id = currentCalendarId,
                    semesterId = id,
                    startDate = start,
                    endDate = end,
                    firstWeekType = form.firstWeek
                )
            )
            feedbackSink.publish(UiFeedback("Zapisano semestr", UiFeedbackKind.Success))
            effectsChannel.trySend(SemesterEffect.CloseConfiguration)
        }
    }

    fun saveCalendar() {
        if (state.value.semester.isSaving) return
        val semester = semesterId ?: return
        val calendarId = selectedCalendarId ?: return
        val form = state.value.semester
        val start = form.startDate.toLocalDateOrNull()
        val end = form.endDate.toLocalDateOrNull()
        val dateErrors = validateDateRange(start, end)
        if (dateErrors.isNotEmpty() || start == null || end == null) {
            val texts = FormErrorTexts.dateRange(dateErrors)
            update {
                it.copy(semester = form.copy(
                    startDateError = texts.start,
                    endDateError = texts.end,
                    dateRangeError = texts.range
                ))
            }
            return
        }
        update {
            it.copy(semester = form.copy(
                isSaving = true,
                startDateError = null,
                endDateError = null,
                dateRangeError = null
            ))
        }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać kalendarza.",
            onFinish = {
                update { it.copy(semester = it.semester.copy(isSaving = false)) }
            },
            onError = {
                update {
                    it.copy(semester = it.semester.copy(dateRangeError = "Nie udało się zapisać kalendarza."))
                }
            }
        ) {
            semesterRepository.saveCalendar(
                AcademicCalendarRecord(
                    id = calendarId,
                    semesterId = semester,
                    startDate = start,
                    endDate = end,
                    firstWeekType = form.firstWeek
                )
            )
            feedbackSink.publish(UiFeedback("Zapisano kalendarz", UiFeedbackKind.Success))
        }
    }

    fun updateCourseName(value: String) = update {
        it.copy(courseNameDraft = value, courseNameError = null)
    }

    fun updateCourseColor(value: String) = update {
        it.copy(courseColorDraft = value)
    }

    fun resetCourseDraft() = update { current ->
        current.copy(
            courseNameDraft = "",
            courseColorDraft = DefaultCourseColor,
            courseCalendarMode = CourseCalendarModeUi.SHARED,
            courseCalendarId = current.selectedCalendarId,
            courseProgramMode = CourseProgramModeUi.NEW,
            courseProgramId = null,
            courseNameError = null
        )
    }

    fun setCourseProgramMode(mode: CourseProgramModeUi) = update { current ->
        current.copy(
            courseProgramMode = mode,
            courseProgramId = if (mode == CourseProgramModeUi.NEW) null else current.courseProgramId,
            courseNameError = null
        )
    }

    fun selectCourseProgram(id: Long?) = update { current ->
        val option = id?.let { selected -> current.courseProgramOptions.firstOrNull { it.id == selected } }
        if (option == null) {
            current.copy(courseProgramId = null)
        } else {
            current.copy(
                courseProgramMode = CourseProgramModeUi.EXISTING,
                courseProgramId = option.id,
                courseNameDraft = option.name,
                courseColorDraft = option.color,
                courseNameError = null
            )
        }
    }

    fun setCourseCalendarMode(mode: CourseCalendarModeUi) = update { current ->
        current.copy(
            courseCalendarMode = mode,
            courseCalendarId = current.courseCalendarId ?: current.selectedCalendarId
        )
    }

    fun selectCourseCalendar(calendarId: Long) = update {
        it.copy(courseCalendarId = calendarId, courseCalendarMode = CourseCalendarModeUi.SHARED)
    }

    fun selectCalendar(calendarId: Long) {
        selectedCalendarId = calendarId
        apply(configuration ?: return)
    }

    fun addCourse() {
        if (state.value.isAddingCourse) return
        val id = semesterId ?: return
        val draft = state.value
        val wantsExisting = draft.courseProgramMode == CourseProgramModeUi.EXISTING
        val existingProgramId = if (wantsExisting) draft.courseProgramId else null
        if (wantsExisting && existingProgramId == null) {
            update { it.copy(courseNameError = FormErrorTexts.ProgramRequired) }
            return
        }
        val name = if (wantsExisting) {
            draft.courseProgramOptions.firstOrNull { it.id == draft.courseProgramId }?.name
                ?: draft.courseNameDraft.trim()
        } else {
            draft.courseNameDraft.trim()
        }
        if (name.isBlank()) {
            update { it.copy(courseNameError = FormErrorTexts.ProgramNameRequired) }
            return
        }
        if (existingProgramId != null &&
            draft.courseItems.any { it.programId == existingProgramId }
        ) {
            update { it.copy(courseNameError = "Ten kierunek jest już przypięty do semestru.") }
            return
        }
        val sourceCalendarId = draft.courseCalendarId ?: selectedCalendarId
        if (sourceCalendarId == null) {
            update { it.copy(courseNameError = "Brak kalendarza semestru.") }
            return
        }
        val program = StudyProgramRecord(
            id = existingProgramId ?: 0L,
            name = name,
            color = draft.courseColorDraft.ifBlank { DefaultCourseColor }
        )
        update { it.copy(isAddingCourse = true, courseNameError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się dodać kierunku.",
            onFinish = {
                update { it.copy(isAddingCourse = false) }
            }
        ) {
            if (draft.courseCalendarMode == CourseCalendarModeUi.SEPARATE) {
                semesterRepository.addSeparatedSemesterProgram(
                    semesterId = id,
                    studyProgram = program,
                    sourceCalendarId = sourceCalendarId
                )
            } else {
                semesterRepository.saveStudyProgramAssignment(
                    semesterId = id,
                    studyProgram = program,
                    academicCalendarId = sourceCalendarId
                )
            }
            update {
                it.copy(courseNameDraft = "", courseProgramId = null, isAddingCourse = false)
            }
            endCourseForm()
            feedbackSink.publish(UiFeedback("Dodano kierunek", UiFeedbackKind.Success))
            effectsChannel.trySend(SemesterEffect.CourseAdded)
        }
    }

    fun separateCourseCalendar(assignmentId: Long) = editSession.run {
        if (state.value.isSeparatingCalendar) return@run
        update { it.copy(isSeparatingCalendar = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się rozdzielić kalendarza.",
            onFinish = {
                update { it.copy(isSeparatingCalendar = false) }
            }
        ) {
            semesterRepository.separateSemesterProgramCalendar(assignmentId)
            feedbackSink.publish(UiFeedback("Rozdzielono kalendarz kierunku", UiFeedbackKind.Success))
        }
    }

    fun requestReconnect(assignmentId: Long, calendarId: Long) = editSession.run {
        val data = configuration ?: return@run
        val assignment = data.programs.firstOrNull { it.id == assignmentId } ?: return@run
        val sourceBecomesUnused = assignment.academicCalendarId != calendarId &&
            data.programs.none {
                it.academicCalendarId == assignment.academicCalendarId && it.id != assignmentId
            }
        val programName = data.studyPrograms
            .firstOrNull { it.id == assignment.studyProgramId }
            ?.name
            .orEmpty()
        update {
            it.copy(
                pendingReconnect = ReconnectCalendarUi(
                    assignmentId = assignmentId,
                    calendarId = calendarId,
                    programName = programName,
                    sourceBecomesUnused = sourceBecomesUnused
                ),
                reconnectError = null
            )
        }
    }

    fun cancelReconnect() = update { it.copy(pendingReconnect = null, reconnectError = null) }

    fun confirmReconnect() {
        val pending = state.value.pendingReconnect ?: return
        if (state.value.isReconnectingCalendar) return
        val assignmentId = pending.assignmentId
        val calendarId = pending.calendarId
        update { it.copy(isReconnectingCalendar = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się połączyć kalendarza.",
            onFinish = {
                update { it.copy(isReconnectingCalendar = false) }
            },
            onError = { update { it.copy(reconnectError = "Nie udało się połączyć kalendarza.") } }
        ) {
            semesterRepository.reconnectSemesterProgram(assignmentId, calendarId)
            update { it.copy(pendingReconnect = null) }
            feedbackSink.publish(UiFeedback("Połączono kierunek z kalendarzem", UiFeedbackKind.Success))
        }
    }

    fun requestCalendarDeletion(calendarId: Long) = editSession.run {
        val calendar = state.value.calendars.firstOrNull { it.id == calendarId } ?: return@run
        update { it.copy(pendingCalendarDeletion = calendar) }
    }

    fun cancelCalendarDeletion() {
        if (state.value.isDeletingCalendar) return
        update { it.copy(pendingCalendarDeletion = null) }
    }

    fun confirmCalendarDeletion() {
        if (state.value.isDeletingCalendar) return
        val id = state.value.pendingCalendarDeletion?.id ?: return
        update { it.copy(isDeletingCalendar = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć kalendarza.",
            onFinish = {
                update { it.copy(isDeletingCalendar = false) }
            }
        ) {
            semesterRepository.deleteCalendar(id)
            update { it.copy(pendingCalendarDeletion = null) }
            if (selectedCalendarId == id) selectedCalendarId = null
            feedbackSink.publish(UiFeedback("Usunięto kalendarz", UiFeedbackKind.Success))
        }
    }

    fun requestCourseDeletion(id: Long) = editSession.run {
        val course = state.value.courseItems.firstOrNull { it.assignmentId == id } ?: return@run
        if (state.value.isPreparingCourseDeletion) return@run
        update { it.copy(isPreparingCourseDeletion = true) }
        viewModelScope.launch {
            try {
                val classCount = semesterRepository.countClassesForAssignment(id)
                update {
                    it.copy(
                        isPreparingCourseDeletion = false,
                        pendingCourseDeletion = CourseDeletionUi(
                            assignmentId = id,
                            programName = course.name,
                            classCount = classCount
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                update { it.copy(isPreparingCourseDeletion = false) }
                feedbackSink.publish(UiFeedback("Nie udało się usunąć kierunku.", UiFeedbackKind.Error))
            }
        }
    }

    fun cancelCourseDeletion() {
        if (state.value.isDeletingCourse) return
        update { it.copy(pendingCourseDeletion = null) }
    }

    fun confirmCourseDeletion() {
        if (state.value.isDeletingCourse) return
        val pending = state.value.pendingCourseDeletion ?: return
        val assignmentId = pending.assignmentId
        update { it.copy(isDeletingCourse = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć kierunku.",
            onFinish = {
                update { it.copy(isDeletingCourse = false) }
            }
        ) {
            semesterRepository.deleteSemesterProgram(assignmentId)
            update { it.copy(pendingCourseDeletion = null) }
            feedbackSink.publish(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success))
        }
    }

    fun newWeekOverride() = editSession.run {
        update {
            it.copy(overrideForm = WeekOverrideFormUiState(isOpen = true))
        }
    }

    fun editWeekOverride(id: Long) = editSession.run {
        val item = state.value.overrides.firstOrNull { it.id == id } ?: return@run
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

    /** Any picked day stands for its whole week, so the form keeps the Monday of that week. */
    fun updateWeekStartDate(value: String) = editSession.edit {
        update {
            val monday = value.toLocalDateOrNull()?.weekStart()
            it.copy(
                overrideForm = it.overrideForm.copy(
                    weekStartDate = monday?.toString() ?: value,
                    weekStartDateError = null
                )
            )
        }
    }

    fun cancelWeekOverrideEdit() = update {
        it.copy(overrideForm = WeekOverrideFormUiState())
    }

    fun saveWeekOverride() {
        if (state.value.overrideForm.isSaving) return
        val semester = semesterId ?: return
        val currentCalendarId = selectedCalendarId ?: return
        val form = state.value.overrideForm
        val date = form.weekStartDate.toLocalDateOrNull()
            ?.weekStart()
        if (date == null) {
            update { it.copy(overrideForm = form.copy(weekStartDateError = "Wybierz tydzień.")) }
            return
        }
        // A new override has no id yet; the repository treats 0 as "insert".
        val overrideId = form.id ?: 0L
        update { it.copy(overrideForm = form.copy(isSaving = true, weekStartDateError = null)) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać korekty tygodnia.",
            onFinish = {
                update { it.copy(overrideForm = it.overrideForm.copy(isSaving = false)) }
            }
        ) {
            semesterRepository.saveWeekOverride(
                WeekOverrideRecord(
                    id = overrideId,
                    semesterId = semester,
                    academicCalendarId = currentCalendarId,
                    weekStartDate = date,
                    weekType = form.weekType,
                    scope = form.scope
                )
            )
            update { it.copy(overrideForm = WeekOverrideFormUiState()) }
            val message = if (form.id == null) "Dodano korektę tygodnia" else "Zapisano korektę tygodnia"
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
        }
    }

    fun requestWeekOverrideDeletion(id: Long) = editSession.run {
        val item = state.value.overrides.firstOrNull { it.id == id } ?: return@run
        update { it.copy(pendingOverrideDeletion = item) }
    }

    fun cancelWeekOverrideDeletion() {
        if (state.value.isDeletingOverride) return
        update { it.copy(pendingOverrideDeletion = null) }
    }

    fun confirmWeekOverrideDeletion() {
        if (state.value.isDeletingOverride) return
        val overrideId = state.value.pendingOverrideDeletion?.id ?: return
        update { it.copy(isDeletingOverride = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć korekty tygodnia.",
            onFinish = {
                update { it.copy(isDeletingOverride = false) }
            }
        ) {
            semesterRepository.deleteWeekOverride(overrideId)
            update { it.copy(pendingOverrideDeletion = null) }
            update { it.copy(overrideForm = WeekOverrideFormUiState()) }
            feedbackSink.publish(UiFeedback("Usunięto korektę tygodnia", UiFeedbackKind.Success))
        }
    }
}


private fun SemesterConfiguration.sharedCalendar(): AcademicCalendarRecord? =
    calendars.minByOrNull { it.id }

private fun SemesterConfiguration.toSemesterScreenState(selectedCalendarId: Long?): SemesterScreenUiState {
    val selected = calendars.firstOrNull { it.id == selectedCalendarId } ?: sharedCalendar()
    val courseItems = programs.mapNotNull { assignment ->
        val program = studyPrograms.firstOrNull { it.id == assignment.studyProgramId }
            ?: return@mapNotNull null
        val calendar = calendars.firstOrNull { it.id == assignment.academicCalendarId }
        SemesterCourseUi(
            assignmentId = assignment.id,
            programId = assignment.studyProgramId,
            name = program.name,
            color = program.color,
            calendarId = calendar?.id,
            calendarLabel = calendar?.let { calendarRangeLabel(it.startDate, it.endDate) }.orEmpty(),
            sharesCalendar = programs.count {
                it.academicCalendarId == assignment.academicCalendarId
            } > 1
        )
    }
    val form = SemesterFormUiState(
        name = semester.name,
        startDate = selected?.startDate?.toString().orEmpty(),
        endDate = selected?.endDate?.toString().orEmpty(),
        firstWeek = selected?.firstWeekType ?: WeekType.A
    )
    return SemesterScreenUiState(
        semester = form,
        loadedSemester = form,
        overrides = overrides
            .filter { it.academicCalendarId == selected?.id }
            .map { override ->
                WeekOverrideUi(
                    override.id,
                    override.weekStartDate.toString(),
                    override.weekType,
                    override.scope
                )
            },
        overrideCount = overrides.size,
        freeDaysSummary = freeDaysSummary(groupFreeDays(freeDays)),
        courseItems = courseItems,
        calendars = calendars.map { calendar ->
            SemesterCalendarUi(
                id = calendar.id,
                startDate = calendar.startDate.toString(),
                endDate = calendar.endDate.toString(),
                rangeLabel = calendarRangeLabel(calendar.startDate, calendar.endDate),
                firstWeek = calendar.firstWeekType,
                courseNames = programs
                    .filter { it.academicCalendarId == calendar.id }
                    .mapNotNull { assignment ->
                        studyPrograms.firstOrNull { it.id == assignment.studyProgramId }?.name
                    }
            )
        },
        selectedCalendarId = selected?.id,
        courseCalendarId = selected?.id,
        courseProgramOptions = studyPrograms.map {
            SemesterProgramOptionUi(it.id, it.name, it.color)
        }
    )
}

internal fun calendarRangeLabel(start: LocalDate, end: LocalDate): String =
    "${start.format(shortDateWithYearFormatter)} - ${end.format(shortDateWithYearFormatter)}"

