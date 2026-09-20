package dev.retza.mak.ui.occurrence

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.LocalDate
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

    private fun occurrenceViewModel(repository: FakeMakRepository) =
        OccurrenceViewModel(repository, feedbackSink = FeedbackController())

    private fun recordingViewModel(
        repository: FakeMakRepository,
        sink: RecordingFeedbackSink
    ) = OccurrenceViewModel(repository, feedbackSink = sink)

    @Test
    fun openValidOccurrenceBuildsDetails() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
    fun openInvalidArgsLeavesDetailsEmpty() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
    fun occurrenceNoteSaveDeletesOnEmpty() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
        val viewModel = occurrenceViewModel(repository)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "12:30") }
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
    fun deleteSelectedClassEmitsCloseExactlyOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "12:30") }
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
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(targetDateDraft = "2026-09-23") }
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertEquals("Przeniesiono termin", sink.published.single().message)
        assertEquals(OccurrenceStatusUi.Moved, viewModel.details.value.status)
    }

    @Test
    fun cancelAndRestorePublishMessages() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
    fun invalidTimeRangeIsRejectedWithoutSaveOrFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "10:30") }
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertNotNull(viewModel.details.value.draftErrors[OccurrenceEditField.EndTime])
        assertTrue(repository.occurrenceChanges.isEmpty())
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun outOfSemesterDateIsRejectedWithoutSaveOrFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(targetDateDraft = "2026-12-01") }
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()

        assertNotNull(viewModel.details.value.draftErrors[OccurrenceEditField.Date])
        assertTrue(repository.occurrenceChanges.isEmpty())
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun occurrenceChangeErrorKeepsDraftAndPublishesOneError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "12:30") }
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
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertEquals("Zapisano notatkę dla wszystkich terminów", sink.published.single().message)

        viewModel.updateSharedNoteDraft("")
        advanceUntilIdle()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertEquals("Usunięto notatkę dla wszystkich terminów", sink.published.last().message)
    }

    @Test
    fun occurrenceNotePublishesSaveAndDeleteMessages() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Zapisano notatkę dla tej daty", sink.published.single().message)

        viewModel.updateOccurrenceNoteDraft("")
        advanceUntilIdle()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Usunięto notatkę dla tej daty", sink.published.last().message)
    }

    @Test
    fun repositoryErrorPublishesSingleErrorAndKeepsDraft() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        val effects = mutableListOf<OccurrenceEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()
        repository.failSaves = true
        viewModel.saveSharedNote()
        advanceUntilIdle()

        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertEquals("Nie udało się zapisać notatki.", sink.published.single().message)
        assertEquals("Nowa", viewModel.details.value.sharedNoteDraft)
        assertNotNull(viewModel.details.value.sharedNoteError)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun repeatedSharedNoteSaveRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveSharedNote()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveClass" })
        assertEquals(1, sink.published.size)
    }

    @Test
    fun repeatedOccurrenceNoteSaveRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "12:30") }
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
        val repository = FakeMakRepository()
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
    fun repeatedRestoreRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()
        viewModel.cancelOccurrence()
        advanceUntilIdle()

        repository.events.clear()
        sink.published.clear()
        repository.saveGate = CompletableDeferred()
        viewModel.restoreOccurrence()
        viewModel.restoreOccurrence()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteOccurrenceChange" })
        assertEquals(1, sink.published.size)
        assertEquals("Przywrócono termin", sink.published.single().message)
    }

    @Test
    fun cancellationDoesNotPublishErrorAndClearsSavingFlags() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink(repository.events)
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1:2026-09-21")
        advanceUntilIdle()

        repository.cancelSaves = true

        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertFalse(viewModel.details.value.isSavingSharedNote)

        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertFalse(viewModel.details.value.isSavingOccurrenceNote)

        viewModel.updateDraft { it.copy(startTimeDraft = "11:00", endTimeDraft = "12:30") }
        advanceUntilIdle()
        viewModel.saveOccurrenceChange()
        advanceUntilIdle()
        assertFalse(viewModel.details.value.isSaving)

        assertTrue(sink.published.isEmpty())
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
