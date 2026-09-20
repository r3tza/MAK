package dev.retza.mak.ui.occurrence

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
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
}
