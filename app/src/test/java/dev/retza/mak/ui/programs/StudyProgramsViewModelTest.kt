package dev.retza.mak.ui.programs

import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyProgramsViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private class RecordingFeedbackSink : FeedbackSink {
        val published = mutableListOf<UiFeedback>()
        override fun publish(feedback: UiFeedback) {
            published += feedback
        }
    }

    private fun viewModel(repository: FakeRepository, sink: FeedbackSink = RecordingFeedbackSink()) =
        StudyProgramsViewModel(FakeSemesterRepository(repository), sink)

    @Test
    fun listsGlobalPrograms() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertEquals(repository.studyPrograms.map { it.id }, viewModel.programs.value.programs.map { it.id })
    }

    @Test
    fun savingEditedProgramUpdatesNameAndColorAndClosesEditor() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        val effects = mutableListOf<StudyProgramsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        val program = repository.studyPrograms.first()
        advanceUntilIdle()

        viewModel.openEditIfNeeded(program.id)
        advanceUntilIdle()
        viewModel.updateName("  Informatyka stosowana ")
        viewModel.updateColor("#334FCE")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.studyPrograms.first { it.id == program.id }
        assertEquals("Informatyka stosowana", saved.name)
        assertEquals("#334FCE", saved.color)
        assertEquals(listOf(UiFeedback("Zapisano kierunek", UiFeedbackKind.Success)), sink.published)
        assertEquals(listOf(StudyProgramsEffect.CloseEditor), effects)
        assertNull(viewModel.programs.value.editor.id)
    }

    @Test
    fun blankNameShowsErrorWithoutSaving() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openEditIfNeeded(repository.studyPrograms.first().id)
        advanceUntilIdle()

        viewModel.updateName("   ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Podaj nazwę kierunku.", viewModel.programs.value.editor.nameError)
        assertFalse("saveStudyProgram" in repository.events)
    }

    @Test
    fun failedSaveKeepsDraftAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        advanceUntilIdle()
        viewModel.openEditIfNeeded(repository.studyPrograms.first().id)
        advanceUntilIdle()
        viewModel.updateName("Nowa nazwa")

        repository.failSaves = true
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Nowa nazwa", viewModel.programs.value.editor.name)
        assertFalse(viewModel.programs.value.editor.isSaving)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
    }

    @Test
    fun doubleSaveWritesOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openEditIfNeeded(repository.studyPrograms.first().id)
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.save()
        viewModel.save()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveStudyProgram" })
    }

    @Test
    fun reopeningSameProgramKeepsDraft() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val id = repository.studyPrograms.first().id
        advanceUntilIdle()
        viewModel.openEditIfNeeded(id)
        advanceUntilIdle()
        viewModel.updateName("Szkic")

        viewModel.openEditIfNeeded(id)
        advanceUntilIdle()

        assertEquals("Szkic", viewModel.programs.value.editor.name)
    }
}
