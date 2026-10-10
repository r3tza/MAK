package dev.retza.mak.ui.occurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.OccurrenceEditDecision
import dev.retza.mak.domain.OccurrenceEditResult
import dev.retza.mak.domain.OccurrenceSlot
import dev.retza.mak.domain.WeekStatus
import dev.retza.mak.domain.decideOccurrenceEdit
import dev.retza.mak.domain.noteContentChanged
import dev.retza.mak.domain.occurrenceId
import dev.retza.mak.domain.occurrenceRoomOverride
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.ui.ActivePlanSource
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.OccurrenceStatusUi
import dev.retza.mak.ui.components.toUi
import dev.retza.mak.ui.label
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OccurrenceEffect {
    data object CloseDetails : OccurrenceEffect
}

@KoinViewModel
class OccurrenceViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val activePlanProvider: ActivePlanProvider,
    private val feedbackSink: FeedbackSink,
    private val activePlanSource: ActivePlanSource,
    planEditTracker: PlanEditTracker,
    savedState: SavedStateHandle
) : ViewModel() {
    private val state = MutableStateFlow(OccurrenceDetailsUiState())
    val details: StateFlow<OccurrenceDetailsUiState> = state.asStateFlow()

    private val selectedClassIdState = MutableStateFlow<Long?>(null)
    val selectedClassId: StateFlow<Long?> = selectedClassIdState.asStateFlow()

    private val effectsChannel = Channel<OccurrenceEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private val activePlan = activePlanSource.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Every edit action below runs through the session: it reloads the shown occurrence before the first draft.
    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { shownRouteId()?.let { refreshForEdit(it) } ?: false },
        hasDraft = { state.value.hasPlanDraft() }
    )

    private var originalDate: LocalDate? = null
    private var openJob: Job? = null
    private var occurrenceStateOperationRunning = false
    private var editPlanData: ActivePlanData? = null
    // The occurrence shown now; a move in the details points it at the new date.
    private var currentArgs: OccurrenceArgs? = null

    // The destination's arguments are read once; the view model lives only as long as its destination.
    init {
        val args = OccurrenceArgs.parse("${savedState.get<String>("classId")}:${savedState.get<String>("date")}")
        if (args != null) {
            open(args)
        } else {
            state.value = derive(emptyOccurrenceDetails().copy(notFound = true))
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

    /** The occurrence the details show now, as the route id the view model reloads before an edit. */
    private fun shownRouteId(): String? {
        val classId = selectedClassIdState.value ?: return null
        val originalDate = runCatching { LocalDate.parse(details.value.baseDate) }.getOrNull() ?: return null
        return OccurrenceArgs(classId, originalDate).toRouteId()
    }

    internal fun open(args: OccurrenceArgs) {
        openJob?.cancel()
        selectedClassIdState.value = null
        originalDate = null
        state.value = derive(emptyOccurrenceDetails())
        currentArgs = args
        openJob = viewModelScope.launch {
            var first = true
            // Later emissions come from a saved class form, a plan downloaded by sync or a changed
            // minimum break.
            activePlan.filterNotNull().collect { inputs ->
                editPlanData = inputs.data
                val shown = currentArgs ?: return@collect
                val built = buildDetails(inputs, shown)
                if (built == null) {
                    if (first) {
                        state.value = derive(emptyOccurrenceDetails().copy(notFound = true))
                    } else {
                        clearMissingOccurrence()
                    }
                } else if (first) {
                    originalDate = built.baseDate.toLocalDateOrNull()
                    selectedClassIdState.value = shown.classId
                    state.value = derive(built)
                } else {
                    val current = state.value
                    if (!current.showEditDialog) {
                        originalDate = built.baseDate.toLocalDateOrNull()
                    }
                    selectedClassIdState.value = shown.classId
                    state.value = derive(current.withFreshData(built))
                }
                first = false
            }
        }
    }

    /** Reloads the source data after edit admission, before a preview starts a draft or confirmation. */
    suspend fun refreshForEdit(routeId: String): Boolean {
        val args = OccurrenceArgs.parse(routeId) ?: return false
        val semester = semesterRepository.observeActiveSemester().first()
        val inputs = semester?.let { activePlanSource.observe(it.id).first() }
        val built = inputs?.let { buildDetails(it, args) }
        if (inputs == null || built == null) {
            clearMissingOccurrence()
            return false
        }
        editPlanData = inputs.data
        currentArgs = args
        originalDate = built.baseDate.toLocalDateOrNull()
        selectedClassIdState.value = args.classId
        // Keeps what the user already typed: the first keystroke admits the screen before this refresh.
        state.value = derive(state.value.withFreshData(built))
        return true
    }

    private fun clearMissingOccurrence() {
        selectedClassIdState.value = null
        originalDate = null
        state.value = derive(emptyOccurrenceDetails().copy(notFound = true))
    }

    fun update(transform: (OccurrenceDetailsUiState) -> OccurrenceDetailsUiState) {
        state.update { derive(transform(it)) }
    }

    fun updateDraft(transform: (OccurrenceDetailsUiState) -> OccurrenceDetailsUiState) {
        state.update {
            derive(transform(it).copy(draftErrors = emptyMap(), draftError = null))
        }
    }

    fun openEditDialog() = editSession.run {
        update {
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
    }

    fun dismissEditDialog() = editSession.run {
        if (state.value.isSaving) return@run
        update { it.copy(showEditDialog = false, draftErrors = emptyMap(), draftError = null) }
    }

    fun requestClassDeletion() = editSession.run { update { it.copy(showDeleteConfirmation = true) } }

    fun cancelClassDeletion() = editSession.run { update { it.copy(showDeleteConfirmation = false) } }

    fun deleteSelectedClass() = editSession.run {
        if (state.value.isDeletingClass) return@run
        val classId = selectedClassIdState.value ?: return@run
        update { it.copy(isDeletingClass = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć zajęć.",
            onFinish = { update { it.copy(isDeletingClass = false) } },
            onError = { update { it.copy(showDeleteConfirmation = false) } }
        ) {
            scheduleRepository.deleteClass(classId)
            effectsChannel.trySend(OccurrenceEffect.CloseDetails)
        }
    }

    fun cancelOccurrence() = editSession.run {
        if (occurrenceStateOperationRunning) return@run
        val data = editPlanData ?: activePlan.value?.data ?: return@run
        val classId = selectedClassIdState.value ?: return@run
        val original = originalDate ?: return@run
        occurrenceStateOperationRunning = true
        update { it.copy(isSaving = true, draftError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się odwołać terminu.",
            onFinish = {
                occurrenceStateOperationRunning = false
                update { it.copy(isSaving = false) }
            },
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

    fun restoreOccurrence() = editSession.run {
        if (occurrenceStateOperationRunning) return@run
        val data = editPlanData ?: activePlan.value?.data ?: return@run
        val classId = selectedClassIdState.value ?: return@run
        val original = originalDate ?: return@run
        occurrenceStateOperationRunning = true
        update { it.copy(isSaving = true, draftError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się przywrócić terminu.",
            onFinish = {
                occurrenceStateOperationRunning = false
                update { it.copy(isSaving = false) }
            }
        ) {
            val change = freshPlanData()?.occurrenceChanges?.firstOrNull {
                it.classId == classId.toString() && it.originalDate == original
            } ?: return@launchUiOperation
            scheduleRepository.deleteOccurrenceChange(change.id.toLong())
            reload(data.semester.id.toLong(), classId, original)
            feedbackSink.publish(UiFeedback("Przywrócono termin", UiFeedbackKind.Success))
        }
    }

    fun updateSharedNoteDraft(value: String) = editSession.edit {
        update { it.copy(sharedNoteDraft = value, sharedNoteError = null) }
    }

    fun updateOccurrenceNoteDraft(value: String) = editSession.edit {
        update { it.copy(occurrenceNoteDraft = value, occurrenceNoteError = null) }
    }

    fun updateTargetDateDraft(value: String) = editSession.run {
        updateDraft { it.copy(targetDateDraft = value) }
    }

    fun updateStartTimeDraft(value: String) = editSession.run {
        updateDraft { it.copy(startTimeDraft = value) }
    }

    fun updateEndTimeDraft(value: String) = editSession.run {
        updateDraft { it.copy(endTimeDraft = value) }
    }

    fun updateRoomDraft(value: String) = editSession.run {
        updateDraft { it.copy(roomDraft = value) }
    }

    fun saveSharedNote() = editSession.run {
        if (state.value.isSavingSharedNote) return@run
        val classId = selectedClassIdState.value ?: return@run
        val draft = state.value
        val note = draft.sharedNoteDraft.trim().ifEmpty { null }
        if (!noteContentChanged(draft.sharedNoteDraft, draft.sharedNote)) return@run
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

    fun saveOccurrenceNote() = editSession.run {
        if (state.value.isSavingOccurrenceNote) return@run
        val data = editPlanData ?: activePlan.value?.data ?: return@run
        val classId = selectedClassIdState.value ?: return@run
        val date = originalDate ?: return@run
        val draft = state.value
        val note = draft.occurrenceNoteDraft.trim().ifEmpty { null }
        if (!noteContentChanged(draft.occurrenceNoteDraft, draft.occurrenceNote)) return@run
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

    fun saveOccurrenceChange() = editSession.run {
        if (state.value.isSaving) return@run
        val data = editPlanData ?: activePlan.value?.data ?: return@run
        val classId = selectedClassIdState.value ?: return@run
        val original = originalDate ?: return@run
        val draft = state.value
        val base = draft.toBaseSlot()
        val current = draft.toCurrentSlot()
        if (base == null || current == null) {
            update { it.copy(draftErrors = mapOf(OccurrenceEditField.Date to FieldErrorUi("Podaj poprawną datę."))) }
            return@run
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
                if (decision.result == OccurrenceEditResult.NoChange) return@run
                val errors = occurrenceDraftErrors(decision.slot, draft)
                if (errors.isNotEmpty()) {
                    update { it.copy(draftErrors = errors) }
                    return@run
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
        semesterRepository.observeActiveSemester().first()?.let { semester ->
            scheduleRepository.observeActivePlanData(semester.id).first()
        }.also { editPlanData = it }

    private suspend fun reload(semesterId: Long, classId: Long, occurrenceDate: LocalDate) {
        val fresh = activePlanSource.observe(semesterId).first() ?: return
        editPlanData = fresh.data
        val args = OccurrenceArgs(classId, occurrenceDate)
        val built = buildDetails(fresh, args) ?: return
        currentArgs = args
        originalDate = built.baseDate.toLocalDateOrNull()
        selectedClassIdState.value = classId
        state.value = derive(built)
    }

    private fun buildDetails(
        inputs: ActivePlanInputs,
        args: OccurrenceArgs
    ): OccurrenceDetailsUiState? {
        val data = inputs.data
        val classId = args.classId
        val originalDate = args.originalDate
        val id = occurrenceId(classId.toString(), originalDate)
        val change = data.occurrenceChanges.firstOrNull {
            it.classId == classId.toString() && it.originalDate == originalDate
        }
        val effectiveDate = change?.targetDate ?: originalDate
        val plan = activePlanProvider.resolve(inputs, effectiveDate)
        val occurrence = (plan.schedule.occurrences + plan.schedule.cancelled).firstOrNull { it.id == id }
            ?: return null
        val base = occurrence.classItem
        val status = occurrence.status.toUi()
        val canEdit = status != OccurrenceStatusUi.OneOff &&
            status != OccurrenceStatusUi.Cancelled
        return OccurrenceDetailsUiState(
            subjectName = base.name,
            courseName = data.studyProgramName(base.semesterProgramId),
            typeLabel = base.type,
            dateLabel = occurrence.date.format(occurrenceDateFormatter),
            currentDate = occurrence.date.toString(),
            startTime = occurrence.startTime.toString(),
            endTime = occurrence.endTime.toString(),
            room = occurrence.room,
            building = occurrence.building,
            teacherName = occurrence.teacherName,
            groupName = base.group,
            weekLabel = data.calendarForAssignment(base.semesterProgramId)
                ?.let { plan.schedule.weeksByCalendarId[it.id] }
                ?.let { WeekStatus.Single(it.weekType).label() },
            conflictLabel = collisionLabels(plan.collisions)[id],
            conflictWith = collisionPartnerNames(plan.collisions)[id],
            originalDateLabel = occurrence.occurrenceChange?.originalDate?.toString(),
            targetDateLabel = occurrence.occurrenceChange?.targetDate?.toString(),
            status = status,
            sharedNote = occurrence.classNote,
            occurrenceNote = occurrence.occurrenceNoteBody,
            occurrenceNoteDraft = occurrence.occurrenceNoteBody.orEmpty(),
            targetDateDraft = occurrence.date.toString(),
            startTimeDraft = occurrence.startTime.toString(),
            endTimeDraft = occurrence.endTime.toString(),
            roomDraft = occurrence.room.orEmpty(),
            baseDate = originalDate.toString(),
            baseStartTime = base.startTime.toString(),
            baseEndTime = base.endTime.toString(),
            baseRoom = base.room?.trim()?.ifEmpty { null },
            semesterStartDate = data.calendarForAssignment(base.semesterProgramId)
                ?.startDate?.toString().orEmpty(),
            semesterEndDate = data.calendarForAssignment(base.semesterProgramId)
                ?.endDate?.toString().orEmpty(),
            canCancelOccurrence = canEdit && occurrence.occurrenceChange == null,
            canChangeOccurrence = canEdit,
            canRestoreOccurrence = occurrence.occurrenceChange != null
        )
    }

    private fun ActivePlanData.studyProgramName(assignmentId: String): String {
        val assignment = semesterPrograms.firstOrNull { it.id == assignmentId } ?: return ""
        return courses.firstOrNull { it.id == assignment.studyProgramId }?.name.orEmpty()
    }

    /**
     * Takes the stored values from [fresh] and keeps what the user is doing: a note draft that
     * differs from its saved note, the drafts of an open edit dialog, and the dialog and saving flags.
     */
    private fun OccurrenceDetailsUiState.withFreshData(fresh: OccurrenceDetailsUiState): OccurrenceDetailsUiState {
        val keepSharedDraft = noteContentChanged(sharedNoteDraft, sharedNote)
        val keepOccurrenceDraft = noteContentChanged(occurrenceNoteDraft, occurrenceNote)
        return fresh.copy(
            sharedNoteDraft = if (keepSharedDraft) sharedNoteDraft else fresh.sharedNote.orEmpty(),
            sharedNoteError = sharedNoteError,
            occurrenceNoteDraft = if (keepOccurrenceDraft) occurrenceNoteDraft else fresh.occurrenceNote.orEmpty(),
            occurrenceNoteError = occurrenceNoteError,
            targetDateDraft = if (showEditDialog) targetDateDraft else fresh.targetDateDraft,
            startTimeDraft = if (showEditDialog) startTimeDraft else fresh.startTimeDraft,
            endTimeDraft = if (showEditDialog) endTimeDraft else fresh.endTimeDraft,
            roomDraft = if (showEditDialog) roomDraft else fresh.roomDraft,
            draftErrors = draftErrors,
            draftError = draftError,
            showEditDialog = showEditDialog,
            isSaving = isSaving,
            isSavingSharedNote = isSavingSharedNote,
            isSavingOccurrenceNote = isSavingOccurrenceNote,
            showDeleteConfirmation = showDeleteConfirmation,
            isDeletingClass = isDeletingClass
        )
    }

    private fun derive(value: OccurrenceDetailsUiState): OccurrenceDetailsUiState = value.copy(
        canSaveOccurrenceEdit = canSaveOccurrenceEdit(value)
    )
}

private fun emptyOccurrenceDetails(): OccurrenceDetailsUiState = OccurrenceDetailsUiState(
    canCancelOccurrence = false,
    canChangeOccurrence = false,
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
