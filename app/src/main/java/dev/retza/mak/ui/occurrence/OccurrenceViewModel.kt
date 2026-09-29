package dev.retza.mak.ui.occurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.OccurrenceEditDecision
import dev.retza.mak.domain.OccurrenceEditResult
import dev.retza.mak.domain.OccurrenceSlot
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.decideOccurrenceEdit
import dev.retza.mak.domain.noteContentChanged
import dev.retza.mak.domain.occurrenceId
import dev.retza.mak.domain.occurrenceRoomOverride
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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

sealed interface OccurrenceEffect {
    data object CloseDetails : OccurrenceEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class OccurrenceViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val activePlanProvider: ActivePlanProvider,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val state = MutableStateFlow(OccurrenceDetailsUiState())
    val details: StateFlow<OccurrenceDetailsUiState> = state.asStateFlow()

    private val selectedClassIdState = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = selectedClassIdState.asStateFlow()

    private val effectsChannel = Channel<OccurrenceEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private val activePlanData = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester ->
            if (semester == null) {
                flowOf(null)
            } else {
                scheduleRepository.observeActivePlanData(semester.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var originalDate: LocalDate? = null
    private var noteDate: LocalDate? = null
    private var openJob: Job? = null
    private var occurrenceStateOperationRunning = false

    fun open(routeId: String) {
        val args = OccurrenceArgs.parse(routeId) ?: return
        open(args)
    }

    fun open(args: OccurrenceArgs) {
        openJob?.cancel()
        selectedClassIdState.value = null
        originalDate = null
        noteDate = null
        state.value = derive(emptyOccurrenceDetails())
        openJob = viewModelScope.launch {
            val data = activePlanData.first { it != null } ?: return@launch
            val built = buildDetails(data, args)
            if (built == null) {
                state.value = derive(emptyOccurrenceDetails().copy(notFound = true))
                return@launch
            }
            originalDate = built.baseDate.toLocalDateOrNull()
            noteDate = built.baseDate.toLocalDateOrNull()
            selectedClassIdState.value = args.classId
            state.value = derive(built)
        }
    }

    fun update(transform: (OccurrenceDetailsUiState) -> OccurrenceDetailsUiState) {
        state.update { derive(transform(it)) }
    }

    fun updateDraft(transform: (OccurrenceDetailsUiState) -> OccurrenceDetailsUiState) {
        state.update {
            derive(transform(it).copy(draftErrors = emptyMap(), draftError = null))
        }
    }

    fun openEditDialog() = update {
        it.copy(
            showEditDialog = true,
            targetDateDraft = it.currentDate,
            startTimeDraft = it.startTime,
            endTimeDraft = it.endTime,
            roomDraft = it.room.orEmpty(),
            draftErrors = emptyMap(),
            draftError = null
        )
    }

    fun dismissEditDialog() {
        if (state.value.isSaving) return
        update { it.copy(showEditDialog = false, draftErrors = emptyMap(), draftError = null) }
    }

    fun requestClassDeletion() = update { it.copy(showDeleteConfirmation = true) }

    fun cancelClassDeletion() = update { it.copy(showDeleteConfirmation = false) }

    fun deleteSelectedClass() {
        val classId = selectedClassIdState.value ?: return
        viewModelScope.launch {
            try {
                scheduleRepository.deleteClass(classId)
                effectsChannel.trySend(OccurrenceEffect.CloseDetails)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                update { it.copy(showDeleteConfirmation = false) }
                feedbackSink.publish(UiFeedback("Nie udało się usunąć zajęć.", UiFeedbackKind.Error))
            }
        }
    }

    fun cancelOccurrence() {
        if (occurrenceStateOperationRunning) return
        val data = activePlanData.value ?: return
        val classId = selectedClassIdState.value ?: return
        val original = originalDate ?: return
        occurrenceStateOperationRunning = true
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się odwołać terminu.",
            onFinish = { occurrenceStateOperationRunning = false },
            onError = { update { it.copy(draftError = "Nie udało się odwołać terminu.") } }
        ) {
            val existing = freshPlanData()?.occurrenceChanges?.firstOrNull {
                it.classId == classId.toString() && it.originalDate == original
            }
            scheduleRepository.saveOccurrenceChange(
                OccurrenceChangeRecord(
                    id = existing?.id?.toLongOrNull() ?: 0,
                    semesterId = data.semester.id.toLong(),
                    classId = classId,
                    originalDate = original,
                    kind = OccurrenceChangeKind.CANCELLED,
                    targetDate = null,
                    startTime = null,
                    endTime = null,
                    room = null,
                    building = null,
                    teacherName = null,
                    note = null
                )
            )
            reload(data.semester.id.toLong(), classId, original)
            feedbackSink.publish(UiFeedback("Odwołano termin", UiFeedbackKind.Success))
        }
    }

    fun restoreOccurrence() {
        if (occurrenceStateOperationRunning) return
        val data = activePlanData.value ?: return
        val classId = selectedClassIdState.value ?: return
        val original = originalDate ?: return
        occurrenceStateOperationRunning = true
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się przywrócić terminu.",
            onFinish = { occurrenceStateOperationRunning = false }
        ) {
            val change = freshPlanData()?.occurrenceChanges?.firstOrNull {
                it.classId == classId.toString() && it.originalDate == original
            } ?: return@launchUiOperation
            scheduleRepository.deleteOccurrenceChange(change.id.toLong())
            reload(data.semester.id.toLong(), classId, original)
            feedbackSink.publish(UiFeedback("Przywrócono termin", UiFeedbackKind.Success))
        }
    }

    fun updateSharedNoteDraft(value: String) = update {
        it.copy(sharedNoteDraft = value, sharedNoteError = null)
    }

    fun updateOccurrenceNoteDraft(value: String) = update {
        it.copy(occurrenceNoteDraft = value, occurrenceNoteError = null)
    }

    fun saveSharedNote() {
        if (state.value.isSavingSharedNote) return
        val classId = selectedClassIdState.value ?: return
        val draft = state.value
        val note = draft.sharedNoteDraft.trim().ifEmpty { null }
        if (!noteContentChanged(draft.sharedNoteDraft, draft.sharedNote)) return
        update { it.copy(isSavingSharedNote = true, sharedNoteError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać notatki.",
            onFinish = { update { it.copy(isSavingSharedNote = false) } },
            onError = {
                update {
                    it.copy(isSavingSharedNote = false, sharedNoteError = "Nie udało się zapisać notatki.")
                }
            }
        ) {
            val base = freshPlanData()?.classes?.firstOrNull { it.id == classId.toString() }
                ?: return@launchUiOperation
            scheduleRepository.saveClass(base.copy(classNote = note).toRecord())
            update { current ->
                val draftUnchanged = !noteContentChanged(current.sharedNoteDraft, note)
                current.copy(
                    sharedNote = note,
                    sharedNoteDraft = if (draftUnchanged) note.orEmpty() else current.sharedNoteDraft,
                    isSavingSharedNote = false
                )
            }
            val message = if (note == null) {
                "Usunięto notatkę do zajęć"
            } else {
                "Zapisano notatkę do zajęć"
            }
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
        }
    }

    fun saveOccurrenceNote() {
        if (state.value.isSavingOccurrenceNote) return
        val data = activePlanData.value ?: return
        val classId = selectedClassIdState.value ?: return
        val date = noteDate ?: return
        val draft = state.value
        val note = draft.occurrenceNoteDraft.trim().ifEmpty { null }
        if (!noteContentChanged(draft.occurrenceNoteDraft, draft.occurrenceNote)) return
        update { it.copy(isSavingOccurrenceNote = true, occurrenceNoteError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać notatki.",
            onFinish = { update { it.copy(isSavingOccurrenceNote = false) } },
            onError = {
                update {
                    it.copy(isSavingOccurrenceNote = false, occurrenceNoteError = "Nie udało się zapisać notatki.")
                }
            }
        ) {
            val existing = freshPlanData()?.occurrenceNotes?.firstOrNull {
                it.classId == classId.toString() && it.occurrenceDate == date
            }
            if (note == null) {
                existing?.let { scheduleRepository.deleteOccurrenceNote(it.id.toLong()) }
            } else {
                scheduleRepository.saveOccurrenceNote(
                    OccurrenceNoteRecord(
                        id = existing?.id?.toLongOrNull() ?: 0,
                        semesterId = data.semester.id.toLong(),
                        classId = classId,
                        occurrenceDate = date,
                        body = note
                    )
                )
            }
            update { current ->
                val draftUnchanged = !noteContentChanged(current.occurrenceNoteDraft, note)
                current.copy(
                    occurrenceNote = note,
                    occurrenceNoteDraft = if (draftUnchanged) note.orEmpty() else current.occurrenceNoteDraft,
                    isSavingOccurrenceNote = false
                )
            }
            val message = if (note == null) {
                "Usunięto notatkę do terminu"
            } else {
                "Zapisano notatkę do terminu"
            }
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
        }
    }

    fun saveOccurrenceChange() {
        if (state.value.isSaving) return
        val data = activePlanData.value ?: return
        val classId = selectedClassIdState.value ?: return
        val original = originalDate ?: return
        val draft = state.value
        val base = draft.toBaseSlot()
        val current = draft.toCurrentSlot()
        if (base == null || current == null) {
            update { it.copy(draftErrors = mapOf(OccurrenceEditField.Date to FieldErrorUi("Podaj poprawną datę."))) }
            return
        }
        val decision = decideOccurrenceEdit(
            base = base,
            current = current,
            hasChange = draft.canRestoreOccurrence,
            draftDate = draft.targetDateDraft,
            draftStartTime = draft.startTimeDraft,
            draftEndTime = draft.endTimeDraft,
            draftRoom = draft.roomDraft
        )
        when (decision) {
            is OccurrenceEditDecision.InvalidDateTime -> update {
                it.copy(draftErrors = mapOf(OccurrenceEditField.Date to FieldErrorUi("Podaj poprawną datę.")))
            }

            is OccurrenceEditDecision.Ready -> {
                if (decision.result == OccurrenceEditResult.NoChange) return
                val errors = occurrenceDraftErrors(decision.slot, draft)
                if (errors.isNotEmpty()) {
                    update { it.copy(draftErrors = errors) }
                    return
                }
                update { it.copy(isSaving = true, draftErrors = emptyMap(), draftError = null) }
                viewModelScope.launchUiOperation(
                    feedbackSink = feedbackSink,
                    errorMessage = "Nie udało się zapisać zmian.",
                    onFinish = { update { it.copy(isSaving = false) } },
                    onError = { update { it.copy(isSaving = false, draftError = "Nie udało się zapisać zmian.") } }
                ) {
                    val existing = freshPlanData()?.occurrenceChanges?.firstOrNull {
                        it.classId == classId.toString() && it.originalDate == original
                    }
                    when (decision.result) {
                        OccurrenceEditResult.NoChange -> Unit
                        OccurrenceEditResult.Restored ->
                            existing?.let { scheduleRepository.deleteOccurrenceChange(it.id.toLong()) }

                        OccurrenceEditResult.Modified,
                        OccurrenceEditResult.Moved -> scheduleRepository.saveOccurrenceChange(
                            OccurrenceChangeRecord(
                                id = existing?.id?.toLongOrNull() ?: 0,
                                semesterId = data.semester.id.toLong(),
                                classId = classId,
                                originalDate = original,
                                kind = OccurrenceChangeKind.MODIFIED,
                                targetDate = decision.slot.date,
                                startTime = decision.slot.startTime,
                                endTime = decision.slot.endTime,
                                room = occurrenceRoomOverride(base.room, decision.slot.room),
                                building = null,
                                teacherName = null,
                                note = null
                            )
                        )
                    }
                    reload(data.semester.id.toLong(), classId, original)
                    val message = when (decision.result) {
                        OccurrenceEditResult.Restored -> "Przywrócono termin"
                        OccurrenceEditResult.Moved -> "Przeniesiono termin"
                        else -> "Zmieniono termin"
                    }
                    feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
                }
            }
        }
    }

    private suspend fun freshPlanData(): ActivePlanData? =
        activePlanData.value?.semester?.id?.toLong()?.let { semesterId ->
            scheduleRepository.observeActivePlanData(semesterId).first()
        }

    private suspend fun reload(semesterId: Long, classId: Long, occurrenceDate: LocalDate) {
        val fresh = scheduleRepository.observeActivePlanData(semesterId).first() ?: return
        val built = buildDetails(fresh, OccurrenceArgs(classId, occurrenceDate)) ?: return
        originalDate = built.baseDate.toLocalDateOrNull()
        noteDate = built.baseDate.toLocalDateOrNull()
        selectedClassIdState.value = classId
        state.value = derive(built)
    }

    private fun buildDetails(
        data: ActivePlanData,
        args: OccurrenceArgs
    ): OccurrenceDetailsUiState? {
        val classId = args.classId
        val originalDate = args.originalDate
        val base = data.classes.firstOrNull { it.id == classId.toString() } ?: return null
        val change = data.occurrenceChanges.firstOrNull {
            it.classId == classId.toString() && it.originalDate == originalDate
        }
        val effectiveDate = change?.targetDate ?: originalDate
        val plan = activePlan(data, effectiveDate)
        val id = occurrenceId(classId.toString(), originalDate)
        val effectiveStart = change?.startTime ?: base.startTime
        val effectiveEnd = change?.endTime ?: base.endTime
        val effectiveRoom = (change?.room ?: base.room)?.trim()?.ifEmpty { null }
        val existingNote = data.occurrenceNotes.firstOrNull {
            it.classId == classId.toString() && it.occurrenceDate == originalDate
        }
        val status = when {
            change?.kind == OccurrenceChangeKind.CANCELLED -> OccurrenceStatusUi.Cancelled
            base.recurrence == Recurrence.ONCE -> OccurrenceStatusUi.OneOff
            change?.targetDate != null && change.targetDate != change.originalDate -> OccurrenceStatusUi.Moved
            change != null -> OccurrenceStatusUi.Changed
            else -> OccurrenceStatusUi.Scheduled
        }
        val canEdit = base.recurrence != Recurrence.ONCE &&
            status != OccurrenceStatusUi.Cancelled
        return OccurrenceDetailsUiState(
            subjectName = base.name,
            courseName = data.studyProgramName(base.semesterProgramId),
            typeLabel = base.type,
            dateLabel = effectiveDate.format(occurrenceDateFormatter),
            currentDate = effectiveDate.toString(),
            startTime = effectiveStart.toString(),
            endTime = effectiveEnd.toString(),
            room = effectiveRoom,
            building = change?.building ?: base.building,
            teacherName = change?.teacherName ?: base.teacherName,
            groupName = base.group,
            weekLabel = plan.schedule.weekType?.let { "Tydzień ${it.name}" },
            conflictLabel = collisionLabels(plan.collisions)[id],
            conflictWith = collisionPartnerNames(plan.collisions)[id],
            originalDateLabel = change?.originalDate?.toString(),
            targetDateLabel = change?.targetDate?.toString(),
            status = status,
            sharedNote = base.classNote,
            occurrenceNote = existingNote?.body,
            occurrenceNoteDraft = existingNote?.body.orEmpty(),
            targetDateDraft = effectiveDate.toString(),
            startTimeDraft = effectiveStart.toString(),
            endTimeDraft = effectiveEnd.toString(),
            roomDraft = effectiveRoom.orEmpty(),
            baseDate = originalDate.toString(),
            baseStartTime = base.startTime.toString(),
            baseEndTime = base.endTime.toString(),
            baseRoom = base.room?.trim()?.ifEmpty { null },
            semesterStartDate = data.calendarForAssignment(base.semesterProgramId)
                ?.startDate?.toString().orEmpty(),
            semesterEndDate = data.calendarForAssignment(base.semesterProgramId)
                ?.endDate?.toString().orEmpty(),
            canCancelOccurrence = canEdit && change == null,
            canChangeOccurrence = canEdit,
            canMoveOccurrence = canEdit,
            canRestoreOccurrence = change != null
        )
    }

    private fun activePlan(data: ActivePlanData, date: LocalDate) =
        activePlanProvider.resolve(data, date)

    private fun ActivePlanData.studyProgramName(assignmentId: String): String {
        val assignment = semesterPrograms.firstOrNull { it.id == assignmentId } ?: return ""
        return courses.firstOrNull { it.id == assignment.studyProgramId }?.name.orEmpty()
    }

    private fun derive(value: OccurrenceDetailsUiState): OccurrenceDetailsUiState = value.copy(
        canSaveOccurrenceEdit = canSaveOccurrenceEdit(value),
        canSaveSharedNote = noteContentChanged(value.sharedNoteDraft, value.sharedNote),
        canSaveOccurrenceNote = noteContentChanged(value.occurrenceNoteDraft, value.occurrenceNote)
    )
}

private fun emptyOccurrenceDetails(): OccurrenceDetailsUiState = OccurrenceDetailsUiState(
    canCancelOccurrence = false,
    canChangeOccurrence = false,
    canMoveOccurrence = false,
    canEditBaseClass = false,
    canDeleteBaseClass = false
)

private fun canSaveOccurrenceEdit(state: OccurrenceDetailsUiState): Boolean {
    val base = state.toBaseSlot() ?: return false
    val current = state.toCurrentSlot() ?: return false
    val decision = decideOccurrenceEdit(
        base = base,
        current = current,
        hasChange = state.canRestoreOccurrence,
        draftDate = state.targetDateDraft,
        draftStartTime = state.startTimeDraft,
        draftEndTime = state.endTimeDraft,
        draftRoom = state.roomDraft
    )
    return decision is OccurrenceEditDecision.Ready &&
        decision.result != OccurrenceEditResult.NoChange
}

private fun occurrenceDraftErrors(
    slot: OccurrenceSlot,
    state: OccurrenceDetailsUiState
): Map<OccurrenceEditField, FieldErrorUi> = buildMap {
    val semesterStart = state.semesterStartDate?.toLocalDateOrNull()
    val semesterEnd = state.semesterEndDate?.toLocalDateOrNull()
    if ((semesterStart != null && slot.date.isBefore(semesterStart)) ||
        (semesterEnd != null && slot.date.isAfter(semesterEnd))
    ) {
        put(OccurrenceEditField.Date, FieldErrorUi("Data musi należeć do aktywnego semestru."))
    }
    if (!slot.endTime.isAfter(slot.startTime)) {
        put(OccurrenceEditField.EndTime, FieldErrorUi("Koniec musi być późniejszy niż początek tego samego dnia."))
    }
}

private fun OccurrenceDetailsUiState.toBaseSlot(): OccurrenceSlot? {
    val date = baseDate.toLocalDateOrNull() ?: return null
    val start = baseStartTime.toLocalTimeOrNull() ?: return null
    val end = baseEndTime.toLocalTimeOrNull() ?: return null
    return OccurrenceSlot(date, start, end, baseRoom)
}

private fun OccurrenceDetailsUiState.toCurrentSlot(): OccurrenceSlot? {
    val date = currentDate.toLocalDateOrNull() ?: return null
    val start = startTime.toLocalTimeOrNull() ?: return null
    val end = endTime.toLocalTimeOrNull() ?: return null
    return OccurrenceSlot(date, start, end, room)
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): java.time.LocalTime? =
    runCatching { java.time.LocalTime.parse(this) }.getOrNull()

private val occurrenceDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("pl-PL"))
