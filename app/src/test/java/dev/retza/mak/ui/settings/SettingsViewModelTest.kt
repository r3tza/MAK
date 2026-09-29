package dev.retza.mak.ui.settings

import dev.retza.mak.export.AcademicCalendarSnapshot
import dev.retza.mak.export.ClassSnapshot
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.JsonExportCodec
import dev.retza.mak.export.PlanBackupService
import dev.retza.mak.export.SemesterProgramSnapshot
import dev.retza.mak.export.SemesterSnapshot
import dev.retza.mak.export.StudyProgramSnapshot
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
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
        repository: FakeRepository,
        preferences: SettingsPreferences = InMemorySettingsPreferences(),
        sink: FeedbackSink = RecordingFeedbackSink()
    ) = SettingsViewModel(
        FakeSemesterRepository(repository),
        repository,
        PlanBackupService(repository),
        preferences,
        sink,
        mainDispatcher
    )

    @Test
    fun settingsStateMapsSemestersActiveAndTheme() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        val state = viewModel.settings.value
        assertEquals(2, state.semesters.size)
        assertEquals("1", state.activeSemesterId)
        assertEquals("Semestr", state.semesters.first { it.id == "1" }.name)
        val activeSemester = state.semesters.first { it.id == "1" }
        assertTrue(activeSemester.firstWeekLabel.startsWith("pierwszy tydzień "))
        val inactiveSemester = state.semesters.first { it.id != "1" }
        assertEquals("", inactiveSemester.dateRangeLabel)
        assertEquals("", inactiveSemester.firstWeekLabel)
        assertEquals("", inactiveSemester.courseCountLabel)
        assertEquals("", inactiveSemester.classCountLabel)
        assertEquals("system", state.themeOptions.first { it.isSelected }.id)
    }

    @Test
    fun loadedThemeModeReportsStoredTheme() = runTest(mainDispatcher) {
        val viewModel = viewModel(FakeRepository(), InMemorySettingsPreferences(initialTheme = ThemeMode.Dark))
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, viewModel.loadedThemeMode.value)
    }

    @Test
    fun selectThemePersistsAndSelectsOption() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
    fun gapThresholdWriteFailurePublishesItsOwnError() = runTest(mainDispatcher) {
        val preferences = InMemorySettingsPreferences()
        preferences.failNextWrite = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(FakeRepository(), preferences, sink)

        viewModel.setGapThresholdMinutes("45")
        advanceUntilIdle()

        assertEquals(listOf("Nie udało się zapisać progu okienka."), sink.published.map { it.message })

        viewModel.setGapThresholdMinutes("45")
        advanceUntilIdle()
        assertEquals(1, sink.published.size)
    }

    @Test
    fun doubleThemeSelectionRunsOneWrite() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
    fun deleteFailureKeepsDialogAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
    fun exportSuccessPublishesSingleMessage() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)

        viewModel.exportJson { }
        advanceUntilIdle()
        viewModel.reportExportFinished(true)
        advanceUntilIdle()

        assertEquals(listOf("Wyeksportowano plan"), sink.published.map { it.message })
    }

    @Test
    fun exportReadFailurePublishesSingleError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.failGetAllSemesterData = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)

        viewModel.exportJson { }
        advanceUntilIdle()

        assertEquals(
            listOf("Nie udało się wyeksportować planu."),
            sink.published.map { it.message }
        )
    }

    @Test
    fun exportWriteFailurePublishesSingleError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)

        viewModel.exportJson { }
        advanceUntilIdle()
        viewModel.reportExportFinished(false)
        advanceUntilIdle()

        assertEquals(
            listOf("Nie udało się wyeksportować planu."),
            sink.published.map { it.message }
        )
    }

    @Test
    fun notificationsTogglePersistsAndMapsToState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val preferences = InMemorySettingsPreferences()
        val viewModel = viewModel(repository, preferences)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.setNotificationsEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.settings.value.notifications.enabled)
        assertEquals(true, preferences.collisionNotifications.first().enabled)
    }

    @Test
    fun notificationHourAndLeadPersist() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val preferences = InMemorySettingsPreferences()
        val viewModel = viewModel(repository, preferences)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.setEveningHour("21:00")
        viewModel.setBeforeClassLeadMinutes("45")
        advanceUntilIdle()

        val stored = preferences.collisionNotifications.first()
        assertEquals(LocalTime.of(21, 0), stored.eveningHour)
        assertEquals(45L, stored.leadMinutes)
        assertTrue(
            viewModel.settings.value.notifications.eveningHourOptions
                .first { it.id == "21:00" }
                .isSelected
        )
    }

    @Test
    fun notificationWriteFailurePublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val preferences = InMemorySettingsPreferences()
        preferences.failNextWrite = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, preferences, sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.setNotificationsEnabled(true)
        advanceUntilIdle()

        assertEquals(
            listOf("Nie udało się zapisać ustawień powiadomień."),
            sink.published.map { it.message }
        )
        assertFalse(viewModel.settings.value.notifications.enabled)
    }

    @Test
    fun gapThresholdDefaultsToThirtyAndPersists() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val preferences = InMemorySettingsPreferences()
        val viewModel = viewModel(repository, preferences)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.settings.value.gapThresholdOptions.first { it.id == "30" }.isSelected)

        viewModel.setGapThresholdMinutes("45")
        advanceUntilIdle()

        assertEquals(45, preferences.gapThresholdMinutes.first())
        assertTrue(viewModel.settings.value.gapThresholdOptions.first { it.id == "45" }.isSelected)
    }

    @Test
    fun prepareImportAcceptsValidSnapshotAndEmitsOpenEffect() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.prepareImport(validImportBytes())
        advanceUntilIdle()

        val preview = viewModel.settings.value.importPreview
        assertEquals(1, preview?.semesterCount)
        assertEquals(1, preview?.programCount)
        assertEquals(1, preview?.classCount)
        assertEquals("Semestr", preview?.activeSemesterName)
        assertEquals(listOf(SettingsEffect.OpenImportPreview), effects)
    }

    @Test
    fun prepareImportRejectsUnsupportedVersion() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        val bytes = JsonExportCodec.encode(validImportSnapshot().copy(schemaVersion = 1))
        viewModel.prepareImport(bytes)
        advanceUntilIdle()

        assertTrue(
            viewModel.settings.value.importErrorMessage?.contains("Nieobsługiwana wersja") == true
        )
        assertEquals(null, viewModel.settings.value.importPreview)
    }

    @Test
    fun prepareImportRejectsMalformedBytes() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.prepareImport(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals("Nie udało się odczytać pliku.", viewModel.settings.value.importErrorMessage)
    }

    @Test
    fun confirmImportReplacesDataAndEmitsCloseEffect() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink = sink)
        backgroundScope.launch { viewModel.settings.collect {} }
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.prepareImport(validImportBytes())
        advanceUntilIdle()
        viewModel.confirmImport()
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "replaceAllData" })
        assertEquals(null, viewModel.settings.value.importPreview)
        assertFalse(viewModel.settings.value.isReplacingData)
        assertTrue(sink.published.any { it.message == "Zaimportowano plan" })
        assertEquals(
            listOf(SettingsEffect.OpenImportPreview, SettingsEffect.CloseImportPreview),
            effects
        )
    }

    @Test
    fun cancelImportClosesPreviewWithoutReplacing() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.prepareImport(validImportBytes())
        advanceUntilIdle()
        viewModel.cancelImport()
        advanceUntilIdle()

        assertEquals(null, viewModel.settings.value.importPreview)
        assertTrue(repository.events.none { it == "replaceAllData" })
        assertEquals(
            listOf(SettingsEffect.OpenImportPreview, SettingsEffect.CloseImportPreview),
            effects
        )
    }

    @Test
    fun importReadErrorSetsMessage() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.reportImportReadError()
        advanceUntilIdle()

        assertEquals("Nie udało się odczytać pliku.", viewModel.settings.value.importErrorMessage)
    }

    @Test
    fun importTooLargeShowsSizeMessage() = runTest(mainDispatcher) {
        val viewModel = viewModel(FakeRepository())
        backgroundScope.launch { viewModel.settings.collect {} }
        advanceUntilIdle()

        viewModel.reportImportTooLarge()
        advanceUntilIdle()

        assertEquals("Plik jest za duży, aby był kopią MAK.", viewModel.settings.value.importErrorMessage)
    }
    private fun validImportSnapshot(): ExportSnapshot = ExportSnapshot(
        schemaVersion = 2,
        studyPrograms = listOf(StudyProgramSnapshot(1, "Informatyka", "#111111")),
        semesters = listOf(
            SemesterSnapshot(
                id = 1,
                name = "Semestr",
                isActive = true,
                calendars = listOf(
                    AcademicCalendarSnapshot(1, 1, "2026-10-01", "2027-02-28", "A")
                ),
                programs = listOf(SemesterProgramSnapshot(1, 1, 1, 1)),
                classes = listOf(
                    ClassSnapshot(
                        id = 1,
                        semesterId = 1,
                        semesterProgramId = 1,
                        name = "Programowanie",
                        type = "Wykład",
                        teacherName = null,
                        dayOfWeek = "MONDAY",
                        startTime = "08:00",
                        endTime = "09:30",
                        room = null,
                        building = null,
                        group = null,
                        recurrence = "EVERY_WEEK",
                        date = null,
                        classNote = null
                    )
                ),
                weekOverrides = emptyList(),
                occurrenceNotes = emptyList(),
                occurrenceChanges = emptyList()
            )
        )
    )

    private fun validImportBytes(): ByteArray = JsonExportCodec.encode(validImportSnapshot())
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
