package dev.retza.mak.ui.settings

import dev.retza.mak.export.JsonExportCodec
import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(
        repository: FakeMakRepository,
        preferences: SettingsPreferences = InMemorySettingsPreferences(),
        sink: FeedbackSink = RecordingFeedbackSink()
    ) = SettingsViewModel(repository, preferences, sink)

    @Test
    fun settingsStateMapsSemestersActiveAndTheme() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        val state = viewModel.settings.value
        assertEquals(2, state.semesters.size)
        assertEquals("1", state.activeSemesterId)
        assertEquals("Semestr", state.semesters.first { it.id == "1" }.name)
        assertEquals("system", state.themeOptions.first { it.isSelected }.id)
    }

    @Test
    fun selectThemePersistsAndSelectsOption() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val preferences = InMemorySettingsPreferences()
        val viewModel = viewModel(repository, preferences)
        backgroundScope.launch { viewModel.settings.collect {} }

        viewModel.selectTheme("dark")
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
        assertEquals("dark", viewModel.settings.value.themeOptions.first { it.isSelected }.id)
    }

    @Test
    fun selectingUnknownThemeIdKeepsSystem() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)

        viewModel.selectTheme("neon")
        advanceUntilIdle()

        assertEquals(ThemeMode.System, viewModel.themeMode.value)
    }

    @Test
    fun storedUnknownThemeFallsBackToSystem() {
        assertEquals(ThemeMode.System, themeModeFromStored(null))
        assertEquals(ThemeMode.System, themeModeFromStored("neon"))
        assertEquals(ThemeMode.Dark, themeModeFromStored("Dark"))
    }

    @Test
    fun themeWriteFailurePublishesSingleErrorAndClearsFlag() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val preferences = InMemorySettingsPreferences()
        preferences.failNextWrite = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, preferences, sink)

        viewModel.selectTheme("dark")
        advanceUntilIdle()

        assertEquals(listOf("Nie udało się zapisać motywu."), sink.published.map { it.message })

        viewModel.selectTheme("dark")
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
        assertEquals(1, sink.published.size)
    }

    @Test
    fun doubleThemeSelectionRunsOneWrite() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val preferences = InMemorySettingsPreferences()
        preferences.writeGate = CompletableDeferred()
        val viewModel = viewModel(repository, preferences)

        viewModel.selectTheme("dark")
        viewModel.selectTheme("light")
        advanceUntilIdle()
        assertEquals(0, preferences.writeCount)

        preferences.writeGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, preferences.writeCount)
        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
    }

    @Test
    fun selectSemesterPublishesSuccessAndSetsActive() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.selectSemester("2")
        advanceUntilIdle()

        assertEquals(2L, repository.activeSemesterId)
        assertEquals(listOf("Zmieniono aktywny semestr"), sink.published.map { it.message })
    }

    @Test
    fun selectSemesterFailureKeepsActiveAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.failSetActiveSemester = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        advanceUntilIdle()

        viewModel.selectSemester("2")
        advanceUntilIdle()

        assertEquals(1L, repository.activeSemesterId)
        assertEquals(
            listOf("Nie udało się zmienić aktywnego semestru."),
            sink.published.map { it.message }
        )
    }

    @Test
    fun doubleSemesterSelectionRunsOneOperation() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.activeSemesterGate = CompletableDeferred()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)

        viewModel.selectSemester("2")
        viewModel.selectSemester("1")
        advanceUntilIdle()
        assertEquals(0, repository.setActiveCount)

        repository.activeSemesterGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.setActiveCount)
        assertEquals(listOf("Zmieniono aktywny semestr"), sink.published.map { it.message })
        assertEquals(2L, repository.activeSemesterId)
    }

    @Test
    fun deletingInactiveSemesterKeepsActive() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("2")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        assertEquals(1L, repository.activeSemesterId)
        assertEquals(listOf("1"), viewModel.settings.value.semesters.map { it.id })
        assertEquals(listOf("Usunięto semestr"), sink.published.map { it.message })
    }

    @Test
    fun deletingActiveSemesterSelectsFallback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("1")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        assertEquals(2L, repository.activeSemesterId)
        assertEquals(listOf("2"), viewModel.settings.value.semesters.map { it.id })
    }

    @Test
    fun deletingLastSemesterClearsActive() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("1")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()
        viewModel.requestSemesterDeletion("2")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        assertEquals(0L, repository.activeSemesterId)
        assertTrue(viewModel.settings.value.semesters.isEmpty())
    }

    @Test
    fun deleteFailureKeepsDialogAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.failSaves = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("2")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        assertEquals(
            listOf("Nie udało się usunąć semestru."),
            sink.published.map { it.message }
        )
        assertEquals("2", viewModel.settings.value.semesterToDeleteId)
        assertEquals(listOf("1", "2"), viewModel.settings.value.semesters.map { it.id })
        assertFalse(viewModel.settings.value.isDeletingSemester)
    }

    @Test
    fun doubleConfirmRunsOneDeletion() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.saveGate = CompletableDeferred()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("1")
        viewModel.confirmSemesterDeletion()
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteSemesterAndSelectFallback" })
    }

    @Test
    fun cancelledDeletionPublishesNoErrorAndClearsFlag() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.cancelSaves = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.requestSemesterDeletion("2")
        viewModel.confirmSemesterDeletion()
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
        assertFalse(viewModel.settings.value.isDeletingSemester)
        assertEquals(listOf("1", "2"), viewModel.settings.value.semesters.map { it.id })
    }

    @Test
    fun exportKeepsSchemaVersion() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        var bytes: ByteArray? = null

        viewModel.exportJson { bytes = it }
        advanceUntilIdle()

        val snapshot = JsonExportCodec.decode(bytes!!)
        assertEquals(1, snapshot.schemaVersion)
    }

    @Test
    fun preferencesRestoreThemeForNewInstance() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val preferences = InMemorySettingsPreferences()
        val first = viewModel(repository, preferences)
        first.selectTheme("light")
        advanceUntilIdle()

        val second = viewModel(repository, preferences)

        assertEquals(ThemeMode.Light, second.themeMode.value)
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
