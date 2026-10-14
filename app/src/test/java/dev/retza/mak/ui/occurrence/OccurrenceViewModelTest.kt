package dev.retza.mak.ui.occurrence

import androidx.lifecycle.SavedStateHandle
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.components.OccurrenceStatusUi
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.activePlanSource
import dev.retza.mak.settings.InMemorySettingsPreferences
import dev.retza.mak.settings.SettingsPreferences
import java.time.LocalDate
import java.time.LocalTime
import dev.retza.mak.sync.PlanEditTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OccurrenceViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun occurrenceViewModel(
        repository: FakeRepository,
        preferences: SettingsPreferences = InMemorySettingsPreferences(),
        savedState: SavedStateHandle = SavedStateHandle(),
        planEditTracker: PlanEditTracker = PlanEditTracker(CoroutineScope(UnconfinedTestDispatcher()))
    ) = OccurrenceViewModel(FakeSemesterRepository(repository), repository, ActivePlanProvider(), feedbackSink = FeedbackController(), activePlanSource = activePlanSource(repository, preferences), planEditTracker = planEditTracker, savedState = savedState)

    private fun recordingViewModel(
        repository: FakeRepository,
        sink: RecordingFeedbackSink
    ) = OccurrenceViewModel(FakeSemesterRepository(repository), repository, ActivePlanProvider(), feedbackSink = sink, activePlanSource = activePlanSource(repository), planEditTracker = PlanEditTracker(CoroutineScope(UnconfinedTestDispatcher())), savedState = SavedStateHandle())

    // Invalid route ids are ignored, as the removed public overload did.
    private fun OccurrenceViewModel.open(routeId: String) {
        OccurrenceArgs.from(routeId.substringBefore(':'), routeId.substringAfter(':', ""))?.let { open(it) }
    }

    @Test
    fun detailsOpenFromSavedStateArguments() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(
            repository,
            savedState = SavedStateHandle(mapOf("classId" to "1", "date" to "2026-09-21"))
        )
        advanceUntilIdle()

        assertFalse(viewModel.details.value.notFound)
        assertEquals("Programowanie", viewModel.details.value.subjectName)
    }

    @Test
    fun missingArgumentsShowNotFound() {
        val viewModel = occurrenceViewModel(FakeRepository())

        assertTrue(viewModel.details.value.notFound)
    }

    @Test
    fun openValidOccurrenceBuildsDetails() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals("Programowanie", state.subjectName)
        assertEquals("L204", state.room)
        assertEquals("2026-09-21", state.currentDate)
        assertEquals(OccurrenceStatusUi.Scheduled, state.status)
        assertEquals(1L, viewModel.selectedClassId.value)
    }

    @Test
    fun classChangedAfterOpeningIsShownInDetails() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        repository.classes[0] = repository.classes[0].copy(room = "B204")
        repository.notifyDataChanged()
        advanceUntilIdle()

        assertEquals("B204", viewModel.details.value.room)
    }

    @Test
    fun unsavedNoteDraftSurvivesAChangeOfTheClass() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Niezapisana notatka")

        repository.classes[0] = repository.classes[0].copy(room = "B204")
        repository.notifyDataChanged()
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals("B204", state.room)
        assertEquals("Niezapisana notatka", state.sharedNoteDraft)
        assertTrue(state.canSaveSharedNote)
    }

    @Test
    fun noteDraftHoldsTheKeyUntilSaved() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val tracker = PlanEditTracker(CoroutineScope(mainDispatcher))
        val viewModel = occurrenceViewModel(repository, planEditTracker = tracker)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateOccurrenceNoteDraft("Notatka do terminu")
        advanceUntilIdle()
        assertTrue(tracker.isEditing)

        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Notatka do terminu", viewModel.details.value.occurrenceNote)
        assertFalse(tracker.isEditing)
    }

    @Test
    fun firstTypedCharacterSurvivesTheAdmissionRefresh() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateOccurrenceNoteDraft("K")
        assertEquals("K", viewModel.details.value.occurrenceNoteDraft)
        viewModel.updateOccurrenceNoteDraft("Ko")
        advanceUntilIdle()

        assertEquals("Ko", viewModel.details.value.occurrenceNoteDraft)
    }

    @Test
    fun classDeletedAfterOpeningShowsNotFound() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        repository.classes.clear()
        repository.notifyDataChanged()
        advanceUntilIdle()

        assertTrue(viewModel.details.value.notFound)
        assertEquals(null, viewModel.selectedClassId.value)
    }

    @Test
    fun noteWriteRemainsProtectedEvenWhenDraftIsNoLongerDirty() {
        assertTrue(OccurrenceDetailsUiState(isSavingSharedNote = true).hasPlanDraft())
        assertTrue(OccurrenceDetailsUiState(isSavingOccurrenceNote = true).hasPlanDraft())
    }

    @Test
    fun ongoingSharedNoteWriteKeepsAdmissionAfterDraftReturnsToBaseline() = runTest(mainDispatcher) {
        val repository = FakeRepository().apply { saveGate = CompletableDeferred() }
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        val baseline = viewModel.details.value.sharedNote.orEmpty()
        viewModel.updateSharedNoteDraft("Nowa notatka")

        viewModel.saveSharedNote()
        assertTrue(viewModel.details.value.isSavingSharedNote)
        viewModel.updateSharedNoteDraft(baseline)

        assertFalse(viewModel.details.value.canSaveSharedNote)
        assertTrue(viewModel.details.value.hasPlanDraft())
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.details.value.isSavingSharedNote)
    }

    @Test
    fun detailsShowCollisionRangeAndPartnerOnEffectiveDate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.classes += repository.classes.single().copy(
            id = 2L,
            name = "Matematyka",
            startTime = java.time.LocalTime.of(10, 0),
            endTime = java.time.LocalTime.of(11, 0)
        )
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        assertEquals("Kolizja 10:00-10:30", viewModel.details.value.conflictLabel)
        assertEquals("Matematyka", viewModel.details.value.conflictWith)
    }

    @Test
    fun openingDeletedClassShowsNotFound() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("99:2026-09-21")
        advanceUntilIdle()

        val state = viewModel.details.value
        assertTrue(state.notFound)
        assertFalse(state.canCancelOccurrence)
        assertFalse(state.canEditBaseClass)
        assertFalse(state.canDeleteBaseClass)
        assertEquals(null, viewModel.selectedClassId.value)
    }

    @Test
    fun refreshAfterReplacementRejectsActionWhenOccurrenceWasDeleted() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        repository.classes.clear()
        val refreshed = viewModel.refreshForEdit(OccurrenceArgs(1L, LocalDate.of(2026, 9, 21)))

        assertFalse(refreshed)
        assertTrue(viewModel.details.value.notFound)
        assertEquals(null, viewModel.selectedClassId.value)
    }

    @Test
    fun routesOfRegularAndMovedOccurrenceOnSameDayOpenDifferentDetails() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.saveOccurrenceChange(
            dev.retza.mak.data.repository.OccurrenceChangeRecord(
                id = 0,
                semesterId = 1L,
                classId = 1L,
                originalDate = LocalDate.of(2026, 9, 21),
                kind = dev.retza.mak.domain.OccurrenceChangeKind.MODIFIED,
                targetDate = LocalDate.of(2026, 9, 28),
                startTime = java.time.LocalTime.of(16, 0),
                endTime = java.time.LocalTime.of(17, 0),
                room = null,
                building = null,
                teacherName = null,
                note = null
            )
        )
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("1:2026-09-28")
        advanceUntilIdle()
        val regular = viewModel.details.value
        assertEquals(OccurrenceStatusUi.Scheduled, regular.status)
        assertEquals("2026-09-28", regular.currentDate)

        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        val moved = viewModel.details.value
        assertEquals(OccurrenceStatusUi.Moved, moved.status)
        assertEquals("2026-09-28", moved.currentDate)
        assertEquals("16:00", moved.startTime)
    }

    @Test
    fun detailsUseAssignmentCalendarRange() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val result = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(
                name = "Fizyka",
                color = "#000000"
            ),
            sourceCalendarId = 1L
        )
        repository.saveCalendar(
            repository.calendars.first { it.id == result.academicCalendarId }
                .copy(startDate = LocalDate.of(2026, 11, 1), endDate = LocalDate.of(2026, 12, 20))
        )
        repository.classes += repository.classes.single().copy(
            id = 2L,
            semesterProgramId = result.semesterProgramId,
            name = "Fizyka zajęcia"
        )
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("2:2026-11-02")
        advanceUntilIdle()

        assertEquals("2026-11-01", viewModel.details.value.semesterStartDate)
        assertEquals("2026-12-20", viewModel.details.value.semesterEndDate)
    }

    @Test
    fun openInvalidArgsLeavesDetailsEmpty() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("nope")
        advanceUntilIdle()
        assertEquals("", viewModel.details.value.subjectName)
        assertEquals(null, viewModel.selectedClassId.value)

        viewModel.open("42:not-a-date")
        advanceUntilIdle()
        assertEquals(null, viewModel.selectedClassId.value)

        viewModel.open(OccurrenceArgs(99L, LocalDate.of(2026, 9, 21)))
        advanceUntilIdle()
        assertEquals(null, viewModel.selectedClassId.value)
    }

    @Test
    fun openWaitsForFirstSemesterDataEmission() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        assertEquals("", viewModel.details.value.subjectName)
        assertEquals(null, viewModel.selectedClassId.value)
        assertFalse(viewModel.details.value.canCancelOccurrence)
        assertFalse(viewModel.details.value.canDeleteBaseClass)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Programowanie", viewModel.details.value.subjectName)
        assertEquals(1L, viewModel.selectedClassId.value)
    }

    @Test
    fun lastOpenWinsWhenDataArrivesLate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.classes += repository.classes.single().copy(id = 2L, name = "Matematyka")
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        viewModel.open(OccurrenceArgs(1L, LocalDate.of(2026, 9, 21)))
        viewModel.open(OccurrenceArgs(2L, LocalDate.of(2026, 9, 21)))
        advanceUntilIdle()
        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Matematyka", viewModel.details.value.subjectName)
        assertEquals(2L, viewModel.selectedClassId.value)
    }

    @Test
    fun sharedNoteSaveKeepsEditTypedDuringSave() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Pierwsza")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertTrue(viewModel.details.value.isSavingSharedNote)

        viewModel.updateSharedNoteDraft("Druga")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals("Pierwsza", state.sharedNote)
        assertEquals("Druga", state.sharedNoteDraft)
        assertFalse(state.isSavingSharedNote)
        assertEquals("Pierwsza", repository.classes.single().classNote)
    }

    @Test
    fun sharedNoteSaveErrorKeepsDraftAndClearsSaving() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.saveSharedNote()
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals("Nowa", state.sharedNoteDraft)
        assertEquals("Wspólna", state.sharedNote)
        assertFalse(state.isSavingSharedNote)
        assertNotNull(state.sharedNoteError)
    }

    @Test
    fun noteOfMovedOccurrenceIsSavedUnderOriginalDate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.saveOccurrenceChange(
            dev.retza.mak.data.repository.OccurrenceChangeRecord(
                id = 0,
                semesterId = 1L,
                classId = 1L,
                originalDate = LocalDate.of(2026, 9, 21),
                kind = dev.retza.mak.domain.OccurrenceChangeKind.MODIFIED,
                targetDate = LocalDate.of(2026, 9, 23),
                startTime = null,
                endTime = null,
                room = null,
                building = null,
                teacherName = null,
                note = null
            )
        )
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateOccurrenceNoteDraft("Oddać projekt")
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 9, 21), repository.occurrenceNotes.single().occurrenceDate)
        assertEquals("Oddać projekt", viewModel.details.value.occurrenceNote)
    }

    @Test
    fun occurrenceNoteSaveDeletesOnEmpty() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        assertFalse(viewModel.details.value.canSaveOccurrenceNote)

        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()
        assertTrue(viewModel.details.value.canSaveOccurrenceNote)
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Notatka daty", viewModel.details.value.occurrenceNote)
        assertEquals(1, repository.occurrenceNotes.size)

        viewModel.updateOccurrenceNoteDraft("")
        advanceUntilIdle()
        assertTrue(viewModel.details.value.canSaveOccurrenceNote)
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals(null, state.occurrenceNote)
        assertEquals("", state.occurrenceNoteDraft)
        assertTrue(repository.occurrenceNotes.isEmpty())
    }

    @Test
    fun occurrenceNoteSaveKeepsEditTypedDuringSave() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateOccurrenceNoteDraft("Pierwsza")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertTrue(viewModel.details.value.isSavingOccurrenceNote)

        viewModel.updateOccurrenceNoteDraft("Druga")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals("Pierwsza", state.occurrenceNote)
        assertEquals("Druga", state.occurrenceNoteDraft)
        assertFalse(state.isSavingOccurrenceNote)
    }

    @Test
    fun occurrenceChangeSaveRefreshesStatusAfterEditedDraft() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateStartTimeDraft("11:00")
        viewModel.updateEndTimeDraft("12:30")
        advanceUntilIdle()
        assertTrue(viewModel.details.value.canSaveOccurrenceEdit)

        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals(OccurrenceStatusUi.Changed, state.status)
        assertEquals("11:00", state.startTime)
        assertEquals(1, repository.occurrenceChanges.size)
    }

    @Test
    fun deleteSelectedClassFailureKeepsDetailsAndReportsError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(mutableListOf())
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        val effects = mutableListOf<OccurrenceEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        viewModel.requestClassDeletion()
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.deleteSelectedClass()
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        assertFalse(viewModel.details.value.showDeleteConfirmation)
        assertFalse(viewModel.details.value.isDeletingClass)
        assertEquals(1, repository.classes.size)
        assertEquals(
            UiFeedback("Nie udało się usunąć zajęć.", UiFeedbackKind.Error),
            sink.published.single()
        )
    }

    @Test
    fun deleteSelectedClassRunsOnceWhileTheFirstIsPending() = runTest(mainDispatcher) {
        val repository = FakeRepository().apply { saveGate = CompletableDeferred() }
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        val effects = mutableListOf<OccurrenceEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.deleteSelectedClass()
        viewModel.deleteSelectedClass()
        advanceUntilIdle()
        assertTrue(viewModel.details.value.isDeletingClass)

        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteClass" })
        assertEquals(listOf(OccurrenceEffect.CloseDetails), effects)
    }

    @Test
    fun deleteSelectedClassEmitsCloseExactlyOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        val effects = mutableListOf<OccurrenceEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.deleteSelectedClass()
        advanceUntilIdle()

        assertEquals(listOf(OccurrenceEffect.CloseDetails), effects)
        assertTrue(repository.classes.isEmpty())
    }

    @Test
    fun noChangeDoesNotSaveOrPublish() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
        assertTrue(repository.occurrenceChanges.isEmpty())
        assertTrue(repository.events.isEmpty())
    }

    @Test
    fun modifiedChangePublishesSuccessAfterSave() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateStartTimeDraft("11:00")
        viewModel.updateEndTimeDraft("12:30")
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback("Zmieniono termin", UiFeedbackKind.Success)),
            sink.published
        )
        assertEquals(listOf("saveOccurrenceChange", "feedback:Zmieniono termin"), repository.events)
    }

    @Test
    fun movedChangePublishesMovedMessage() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateTargetDateDraft("2026-09-23")
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertEquals("Przeniesiono termin", sink.published.single().message)
        assertEquals(OccurrenceStatusUi.Moved, viewModel.details.value.status)
    }

    @Test
    fun cancelAndRestorePublishMessages() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.cancelOccurrence()
        advanceUntilIdle()
        assertEquals("Odwołano termin", sink.published.single().message)
        assertEquals(OccurrenceStatusUi.Cancelled, viewModel.details.value.status)
        assertTrue(viewModel.details.value.canRestoreOccurrence)
        assertFalse(viewModel.details.value.canCancelOccurrence)

        viewModel.restoreOccurrence()
        advanceUntilIdle()
        assertEquals(2, sink.published.size)
        assertEquals("Przywrócono termin", sink.published.last().message)
        assertEquals(OccurrenceStatusUi.Scheduled, viewModel.details.value.status)
        assertFalse(viewModel.details.value.canRestoreOccurrence)
        assertTrue(viewModel.details.value.canCancelOccurrence)
    }

    @Test
    fun movedOccurrenceCanBeCancelledAfterConfirmationAndRestoredToPlanDate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateOccurrenceNoteDraft("Notatka terminu")
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        viewModel.openEditDialog()
        viewModel.updateTargetDateDraft("2026-09-23")
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()
        assertEquals(OccurrenceStatusUi.Moved, viewModel.details.value.status)
        assertTrue(viewModel.details.value.canCancelOccurrence)

        viewModel.requestCancelOccurrence()
        advanceUntilIdle()
        assertTrue(viewModel.details.value.showCancelChangedConfirmation)
        assertEquals(OccurrenceStatusUi.Moved, viewModel.details.value.status)

        viewModel.cancelOccurrence()
        advanceUntilIdle()
        val cancelled = viewModel.details.value
        assertFalse(cancelled.showCancelChangedConfirmation)
        assertEquals(OccurrenceStatusUi.Cancelled, cancelled.status)
        assertEquals("2026-09-21", cancelled.currentDate)
        assertTrue(cancelled.canRestoreOccurrence)
        assertEquals("Notatka terminu", cancelled.occurrenceNote)

        viewModel.requestRestoreOccurrence()
        advanceUntilIdle()
        assertTrue(viewModel.details.value.showRestoreConfirmation)
        assertTrue(viewModel.details.value.restoreTargetLabel.endsWith("${cancelled.baseStartTime} - ${cancelled.baseEndTime}"))

        viewModel.restoreOccurrence()
        advanceUntilIdle()
        val restored = viewModel.details.value
        assertFalse(restored.showRestoreConfirmation)
        assertEquals(OccurrenceStatusUi.Scheduled, restored.status)
        assertEquals("2026-09-21", restored.currentDate)
    }

    @Test
    fun requestCancelOnRegularOccurrenceCancelsWithoutConfirmation() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.requestCancelOccurrence()
        advanceUntilIdle()

        assertFalse(viewModel.details.value.showCancelChangedConfirmation)
        assertEquals(OccurrenceStatusUi.Cancelled, viewModel.details.value.status)
    }

    @Test
    fun requestRestoreWithoutChangeSetsNoFlag() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.requestRestoreOccurrence()
        advanceUntilIdle()

        assertFalse(viewModel.details.value.showRestoreConfirmation)
    }

    @Test
    fun restoreWithoutChangeDoesNothing() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.restoreOccurrence()
        advanceUntilIdle()

        assertTrue(sink.published.none { it.kind == UiFeedbackKind.Error })
        assertEquals(OccurrenceStatusUi.Scheduled, viewModel.details.value.status)
    }

    @Test
    fun invalidTimeRangeIsRejectedWithoutSaveOrFeedback() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateStartTimeDraft("11:00")
        viewModel.updateEndTimeDraft("10:30")
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertNotNull(viewModel.details.value.draftErrors[OccurrenceEditField.EndTime])
        assertTrue(repository.occurrenceChanges.isEmpty())
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun dateOutsideProgramCalendarIsRejectedWithoutSaveOrFeedback() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateTargetDateDraft("2026-12-01")
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertNotNull(viewModel.details.value.draftErrors[OccurrenceEditField.Date])
        assertEquals(
            "Data musi należeć do kalendarza kierunku.",
            viewModel.details.value.draftErrors[OccurrenceEditField.Date]?.message
        )
        assertTrue(repository.occurrenceChanges.isEmpty())
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun occurrenceChangeErrorKeepsDraftAndPublishesOneError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.openEditDialog()
        viewModel.updateStartTimeDraft("11:00")
        viewModel.updateEndTimeDraft("12:30")
        advanceUntilIdle()
        repository.failSaves = true
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        val state = viewModel.details.value
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertEquals("11:00", state.startTimeDraft)
        assertEquals("Nie udało się zapisać zmian.", state.draftError)
        assertFalse(state.isSaving)
    }

    @Test
    fun sharedNotePublishesSaveAndDeleteMessages() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertEquals("Zapisano notatkę do zajęć", sink.published.single().message)

        viewModel.updateSharedNoteDraft("")
        advanceUntilIdle()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertEquals("Usunięto notatkę do zajęć", sink.published.last().message)
    }

    @Test
    fun occurrenceNotePublishesSaveAndDeleteMessages() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Zapisano notatkę do terminu", sink.published.single().message)

        viewModel.updateOccurrenceNoteDraft("")
        advanceUntilIdle()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Usunięto notatkę do terminu", sink.published.last().message)
    }

    @Test
    fun repeatedOccurrenceNoteSaveRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveOccurrenceNote()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveOccurrenceNote" })
        assertEquals(1, sink.published.size)
    }

    @Test
    fun repeatedOccurrenceChangeSaveRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.openEditDialog()
        viewModel.updateStartTimeDraft("11:00")
        viewModel.updateEndTimeDraft("12:30")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveOccurrenceChange()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveOccurrenceChange" })
        assertEquals(1, sink.published.size)
    }

    @Test
    fun repeatedCancelRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.cancelOccurrence()
        viewModel.cancelOccurrence()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveOccurrenceChange" })
        assertEquals(1, sink.published.size)
        assertEquals("Odwołano termin", sink.published.single().message)
    }
    @Test
    fun detailsOfHiddenProgramAreNotFound() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val preferences = InMemorySettingsPreferences()
        preferences.setStudyProgramHidden(1L, hidden = true)
        val viewModel = occurrenceViewModel(repository, preferences)
        advanceUntilIdle()

        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        assertTrue(viewModel.details.value.notFound)
    }

    @Test
    fun detailsWeekComesFromTheClassCalendar() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val calendarB = repository.saveCalendar(
            AcademicCalendarEntity(
                id = 0L,
                semesterId = 1L,
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2026, 10, 31),
                firstWeekType = WeekType.B
            )
        )
        val programB = repository.saveSemesterProgram(
            SemesterProgramEntity(id = 0L, semesterId = 1L, studyProgramId = 1L, academicCalendarId = calendarB)
        )
        repository.classes += repository.classes.single().copy(
            id = 2L,
            name = "Matematyka",
            semesterProgramId = programB,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 0),
            room = null
        )
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()

        // On 2026-09-14 the first calendar has week A and this class's calendar has week B.
        viewModel.open("2:2026-09-14")
        advanceUntilIdle()

        assertEquals("Tydzień B", viewModel.details.value.weekLabel)
    }
}

private class RecordingFeedbackSink(
    private val eventLog: MutableList<String>
) : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
        eventLog += "feedback:${feedback.message}"
    }
}
