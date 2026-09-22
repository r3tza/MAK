package dev.retza.mak.ui.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
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

@KoinViewModel
class SemesterViewModel(
    private val semesterRepository: SemesterRepository,
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
    private var selectedCalendarId: Long? = null

    fun open(id: String) {
        sessionToken += 1
        val token = sessionToken
        openJob?.cancel()
        semesterIdState.value = null
        selectedCalendarId = null
        state.value = SemesterScreenUiState()
        val parsed = id.toLongOrNull() ?: return
        openJob = viewModelScope.launch {
            try {
                semesterRepository.setActiveSemester(parsed)
                val snapshot = loadSnapshot(parsed) ?: return@launch
                if (token != sessionToken) return@launch
                semesterIdState.value = parsed
                selectedCalendarId = snapshot.sharedCalendar()?.id
                state.value = snapshot.toSemesterScreenState(selectedCalendarId)
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
        val currentCalendarId = selectedCalendarId ?: return
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
                semesterRepository.updateSemesterWithCalendar(
                    SemesterRecord(id = id, name = form.name.trim()),
                    AcademicCalendarRecord(
                        id = currentCalendarId,
                        semesterId = id,
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(form.firstWeek.name)
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

    fun saveCalendar() {
        if (state.value.semester.isSaving) return
        val semester = semesterIdState.value ?: return
        val calendarId = selectedCalendarId ?: return
        val form = state.value.semester
        val start = form.startDate.toLocalDateOrNull()
        val end = form.endDate.toLocalDateOrNull()
        if (start == null || end == null || end.isBefore(start)) {
            update {
                it.copy(semester = form.copy(
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
            it.copy(semester = form.copy(
                isSaving = true,
                startDateError = null,
                endDateError = null,
                dateRangeError = null
            ))
        }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.saveCalendar(
                    AcademicCalendarRecord(
                        id = calendarId,
                        semesterId = semester,
                        startDate = start,
                        endDate = end,
                        firstWeekType = WeekType.valueOf(form.firstWeek.name)
                    )
                )
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Zapisano kalendarz", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                update {
                    it.copy(semester = it.semester.copy(dateRangeError = "Nie udało się zapisać kalendarza."))
                }
                feedbackSink.publish(UiFeedback("Nie udało się zapisać kalendarza.", UiFeedbackKind.Error))
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

    fun setCourseProgramMode(mode: CourseProgramModeUi) = update { current ->
        current.copy(
            courseProgramMode = mode,
            courseProgramId = if (mode == CourseProgramModeUi.NEW) null else current.courseProgramId,
            courseNameError = null
        )
    }

    fun selectCourseProgram(id: String?) = update { current ->
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

    fun selectCourseCalendar(calendarId: String) = update {
        it.copy(courseCalendarId = calendarId, courseCalendarMode = CourseCalendarModeUi.SHARED)
    }

    fun selectCalendar(calendarId: String) {
        selectedCalendarId = calendarId.toLongOrNull()
        val token = sessionToken
        viewModelScope.launch { refresh(token) }
    }

    fun addCourse() {
        if (state.value.isAddingCourse) return
        val id = semesterIdState.value ?: return
        val draft = state.value
        val wantsExisting = draft.courseProgramMode == CourseProgramModeUi.EXISTING
        val existingProgramId = if (wantsExisting) draft.courseProgramId?.toLongOrNull() else null
        if (wantsExisting && existingProgramId == null) {
            update { it.copy(courseNameError = "Wybierz kierunek.") }
            return
        }
        val name = if (wantsExisting) {
            draft.courseProgramOptions.firstOrNull { it.id == draft.courseProgramId }?.name
                ?: draft.courseNameDraft.trim()
        } else {
            draft.courseNameDraft.trim()
        }
        if (name.isBlank()) {
            update { it.copy(courseNameError = "Podaj nazwę kierunku.") }
            return
        }
        if (existingProgramId != null &&
            draft.courseItems.any { it.programId == existingProgramId.toString() }
        ) {
            update { it.copy(courseNameError = "Ten kierunek jest już przypięty do semestru.") }
            return
        }
        val sourceCalendarId = draft.courseCalendarId?.toLongOrNull() ?: selectedCalendarId
        if (sourceCalendarId == null) {
            update { it.copy(courseNameError = "Brak kalendarza semestru.") }
            return
        }
        val program = StudyProgramRecord(
            id = existingProgramId ?: 0L,
            name = name,
            color = draft.courseColorDraft.ifBlank { "#137b71" }
        )
        update { it.copy(isAddingCourse = true, courseNameError = null) }
        val token = sessionToken
        viewModelScope.launch {
            try {
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
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                update {
                    it.copy(courseNameDraft = "", courseProgramId = null, isAddingCourse = false)
                }
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

    fun separateCourseCalendar(assignmentId: String) {
        if (state.value.isSeparatingCalendar) return
        val id = assignmentId.toLongOrNull() ?: return
        update { it.copy(isSeparatingCalendar = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.separateSemesterProgramCalendar(id)
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Rozdzielono kalendarz kierunku", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się rozdzielić kalendarza.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isSeparatingCalendar = false) }
                }
            }
        }
    }

    fun requestReconnect(assignmentId: String, calendarId: String) {
        val assignmentValue = assignmentId.toLongOrNull() ?: return
        val calendarValue = calendarId.toLongOrNull() ?: return
        val semesterId = semesterIdState.value ?: return
        val token = sessionToken
        viewModelScope.launch {
            val data = loadSnapshot(semesterId) ?: return@launch
            if (!isCurrentSession(token)) return@launch
            val assignment = data.programs.firstOrNull { it.id == assignmentValue } ?: return@launch
            val sourceBecomesUnused = assignment.academicCalendarId != calendarValue &&
                data.programs.none {
                    it.academicCalendarId == assignment.academicCalendarId && it.id != assignmentValue
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
    }

    fun cancelReconnect() = update { it.copy(pendingReconnect = null, reconnectError = null) }

    fun confirmReconnect() {
        val pending = state.value.pendingReconnect ?: return
        if (state.value.isReconnectingCalendar) return
        val assignmentId = pending.assignmentId.toLongOrNull() ?: return
        val calendarId = pending.calendarId.toLongOrNull() ?: return
        update { it.copy(isReconnectingCalendar = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.reconnectSemesterProgram(assignmentId, calendarId)
                if (!isCurrentSession(token)) return@launch
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                update { it.copy(pendingReconnect = null) }
                feedbackSink.publish(UiFeedback("Połączono kierunek z kalendarzem", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                update { it.copy(reconnectError = "Nie udało się połączyć kalendarza.") }
                feedbackSink.publish(UiFeedback("Nie udało się połączyć kalendarza.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isReconnectingCalendar = false) }
                }
            }
        }
    }

    fun deleteUnusedCalendar(calendarId: String) {
        if (state.value.isDeletingCalendar) return
        val id = calendarId.toLongOrNull() ?: return
        update { it.copy(isDeletingCalendar = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.deleteCalendar(id)
                if (!isCurrentSession(token)) return@launch
                if (selectedCalendarId == id) selectedCalendarId = null
                refresh(token)
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Usunięto kalendarz", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isCurrentSession(token)) return@launch
                feedbackSink.publish(UiFeedback("Nie udało się usunąć kalendarza.", UiFeedbackKind.Error))
            } finally {
                if (isCurrentSession(token)) {
                    update { it.copy(isDeletingCalendar = false) }
                }
            }
        }
    }

    fun deleteCourse(id: String) {
        if (state.value.isDeletingCourse) return
        val assignmentId = id.toLongOrNull() ?: return
        update { it.copy(isDeletingCourse = true) }
        val token = sessionToken
        viewModelScope.launch {
            try {
                semesterRepository.deleteSemesterProgram(assignmentId)
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
        val data = loadSnapshot(id) ?: return
        if (!isCurrentSession(token)) return
        val current = state.value
        selectedCalendarId = data.calendars.firstOrNull { it.id == selectedCalendarId }?.id
            ?: data.sharedCalendar()?.id
        val requestedCourseCalendarId = current.courseCalendarId?.toLongOrNull()
        val courseCalendarId = requestedCourseCalendarId
            ?.takeIf { value -> data.calendars.any { it.id == value } }
            ?: selectedCalendarId
        state.value = data.toSemesterScreenState(selectedCalendarId).copy(
            courseNameDraft = current.courseNameDraft,
            courseColorDraft = current.courseColorDraft,
            courseCalendarMode = current.courseCalendarMode,
            courseCalendarId = courseCalendarId?.toString(),
            courseProgramMode = current.courseProgramMode,
            courseProgramId = current.courseProgramId,
            overrideForm = current.overrideForm
        )
    }

    private suspend fun loadSnapshot(id: Long): SemesterSnapshot? {
        val semester = semesterRepository.observeSemester(id).first() ?: return null
        return SemesterSnapshot(
            semester = semester,
            programs = semesterRepository.observeSemesterPrograms(id).first(),
            studyPrograms = semesterRepository.observeStudyPrograms().first(),
            calendars = semesterRepository.observeCalendars(id).first(),
            overrides = semesterRepository.observeWeekOverrides(id).first()
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
        val currentCalendarId = selectedCalendarId ?: return
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
                semesterRepository.saveWeekOverride(
                    WeekOverrideRecord(
                        id = form.id?.toLongOrNull() ?: 0,
                        semesterId = semester,
                        academicCalendarId = currentCalendarId,
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
                semesterRepository.deleteWeekOverride(overrideId)
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
}

private fun String.toLocalDateOrNull(): java.time.LocalDate? =
    runCatching { java.time.LocalDate.parse(this) }.getOrNull()

private data class SemesterSnapshot(
    val semester: SemesterRecord,
    val programs: List<SemesterProgramRecord>,
    val studyPrograms: List<StudyProgramRecord>,
    val calendars: List<AcademicCalendarRecord>,
    val overrides: List<WeekOverrideRecord>
)

private fun SemesterSnapshot.sharedCalendar(): AcademicCalendarRecord? =
    calendars.minByOrNull { it.id }

private fun SemesterSnapshot.toSemesterScreenState(selectedCalendarId: Long?): SemesterScreenUiState {
    val selected = calendars.firstOrNull { it.id == selectedCalendarId } ?: sharedCalendar()
    val courseItems = programs.mapNotNull { assignment ->
        val program = studyPrograms.firstOrNull { it.id == assignment.studyProgramId }
            ?: return@mapNotNull null
        val calendar = calendars.firstOrNull { it.id == assignment.academicCalendarId }
        SemesterCourseUi(
            assignmentId = assignment.id.toString(),
            programId = assignment.studyProgramId.toString(),
            name = program.name,
            color = program.color,
            calendarId = calendar?.id?.toString().orEmpty(),
            calendarLabel = calendarLabel(calendar),
            sharesCalendar = programs.count {
                it.academicCalendarId == assignment.academicCalendarId
            } > 1
        )
    }
    return SemesterScreenUiState(
        semester = SemesterFormUiState(
            name = semester.name,
            startDate = selected?.startDate?.toString().orEmpty(),
            endDate = selected?.endDate?.toString().orEmpty(),
            firstWeek = selected?.let { WeekTypeUi.valueOf(it.firstWeekType.name) } ?: WeekTypeUi.A
        ),
        overrides = overrides
            .filter { it.academicCalendarId == selected?.id }
            .map { override ->
                WeekOverrideUi(
                    override.id.toString(),
                    override.weekStartDate.toString(),
                    WeekTypeUi.valueOf(override.weekType.name),
                    WeekOverrideScopeUi.valueOf(override.scope.name)
                )
            },
        overrideCount = overrides.size,
        courseItems = courseItems,
        calendars = calendars.map { calendar ->
            SemesterCalendarUi(
                id = calendar.id.toString(),
                startDate = calendar.startDate.toString(),
                endDate = calendar.endDate.toString(),
                firstWeek = WeekTypeUi.valueOf(calendar.firstWeekType.name),
                courseNames = programs
                    .filter { it.academicCalendarId == calendar.id }
                    .mapNotNull { assignment ->
                        studyPrograms.firstOrNull { it.id == assignment.studyProgramId }?.name
                    }
            )
        },
        selectedCalendarId = selected?.id?.toString(),
        courseCalendarId = selected?.id?.toString(),
        courseProgramOptions = studyPrograms.map {
            SemesterProgramOptionUi(it.id.toString(), it.name, it.color)
        }
    )
}

private fun calendarLabel(calendar: AcademicCalendarRecord?): String =
    calendar?.let { "${it.startDate} - ${it.endDate}" }.orEmpty()
