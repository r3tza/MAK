package dev.retza.mak.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.RoomPlanBackupGateway
import dev.retza.mak.data.repository.PlanBackupGateway
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.ui.settings.DataStoreSettingsPreferences
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.ui.settings.TestPreferencesFile
import java.time.LocalDate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The only synchronization code that replaces the whole database, checked on real Room. */
@RunWith(AndroidJUnit4::class)
class RoomPlanSyncGatewayTest {
    private lateinit var database: AppDatabase
    private lateinit var backup: RoomPlanBackupGateway
    private lateinit var gateway: RoomPlanSyncGateway
    private lateinit var preferences: SettingsPreferences
    private val preferencesFile = TestPreferencesFile(ApplicationProvider.getApplicationContext<Context>())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        backup = RoomPlanBackupGateway(database)
        preferences = DataStoreSettingsPreferences(preferencesFile.open())
        gateway = RoomPlanSyncGateway(database, backup, preferences)
    }

    @After
    fun tearDown() {
        database.close()
        preferencesFile.delete()
    }

    @Test
    fun firstDownloadShowsTheHiddenProgramsAgain() = runBlocking {
        backup.replaceAll(plan("A", active = 1))
        preferences.setStudyProgramHidden("1", hidden = true)

        gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("X"), keepLocalActive = false, fallbackActiveId = 1
        )

        assertTrue(preferences.planDisplay.first().hiddenProgramIds.isEmpty())
    }

    @Test
    fun laterDownloadKeepsTheHiddenPrograms() = runBlocking {
        backup.replaceAll(plan("A", active = 1))
        preferences.setStudyProgramHidden("1", hidden = true)

        gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("A", "B"), keepLocalActive = true, fallbackActiveId = 1
        )

        assertEquals(setOf("1"), preferences.planDisplay.first().hiddenProgramIds)
    }

    @Test
    fun planChangedSinceTheReadIsNotReplaced() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 1))
        val seen = SyncPlanFile.fingerprint(gateway.snapshot())
        backup.replaceAll(plan("A", "B", "Edytowany", active = 1))

        val replaced = gateway.replaceIfUnchanged(seen, plan("Z Dysku"), keepLocalActive = true, fallbackActiveId = 1)

        assertFalse(replaced)
        assertEquals(listOf("A", "B", "Edytowany"), gateway.snapshot().semesters.map { it.semester.name })
    }

    @Test
    fun editorStartedDuringRoomReplacementReadsTheCommittedPlan() = runBlocking {
        val original = plan("Lokalny", active = 1)
        val downloaded = plan("Pobrany", active = 1)
        backup.replaceAll(original)
        val expected = SyncPlanFile.fingerprint(gateway.snapshot())
        val replacementEntered = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val blockingBackup = object : PlanBackupGateway {
            override suspend fun snapshot() = backup.snapshot()

            override suspend fun replaceAll(data: BackupData): Long? {
                replacementEntered.complete(Unit)
                finishReplacement.await()
                return backup.replaceAll(data)
            }
        }
        val blockingGateway = RoomPlanSyncGateway(database, blockingBackup, preferences)
        val tracker = PlanEditTracker(CoroutineScope(Dispatchers.Default))

        val replacement = async(Dispatchers.IO) {
            tracker.withReplacement {
                blockingGateway.replaceIfUnchanged(
                    expected,
                    downloaded,
                    keepLocalActive = true,
                    fallbackActiveId = 1
                )
            }
        }
        replacementEntered.await()

        var editorData: BackupData? = null
        val editor = async(start = CoroutineStart.UNDISPATCHED) {
            tracker.beginEdit("semester-form")
            editorData = backup.snapshot()
        }
        assertFalse(editor.isCompleted)
        assertEquals(null, editorData)

        finishReplacement.complete(Unit)
        val result = replacement.await()
        editor.await()

        assertTrue(result is PlanReplacementResult.Applied && result.value)
        assertEquals(listOf("Pobrany"), editorData?.semesters?.map { it.semester.name })
    }

    @Test
    fun editorAdmittedAfterReplacementDoesNotSeeADeletedRoomClass() = runBlocking {
        val oldPlan = plan("Lokalny", active = 1).withOneClass()
        val replacementPlan = plan("Pobrany", active = 1)
        backup.replaceAll(oldPlan)
        val expected = SyncPlanFile.fingerprint(gateway.snapshot())
        val replacementEntered = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val blockingBackup = object : PlanBackupGateway {
            override suspend fun snapshot() = backup.snapshot()
            override suspend fun replaceAll(data: BackupData): Long? {
                replacementEntered.complete(Unit)
                finishReplacement.await()
                return backup.replaceAll(data)
            }
        }
        val tracker = PlanEditTracker(CoroutineScope(Dispatchers.Default))
        val replacement = async(Dispatchers.IO) {
            tracker.withReplacement {
                RoomPlanSyncGateway(database, blockingBackup, preferences).replaceIfUnchanged(
                    expected,
                    replacementPlan,
                    keepLocalActive = true,
                    fallbackActiveId = 1
                )
            }
        }
        replacementEntered.await()

        var classesBeforeCommit: List<ClassEntity>? = null
        val editor = async(start = CoroutineStart.UNDISPATCHED) {
            tracker.beginEdit("class-form")
            classesBeforeCommit = backup.snapshot().semesters.flatMap { it.classes }
        }
        assertFalse(editor.isCompleted)
        assertEquals(null, classesBeforeCommit)

        finishReplacement.complete(Unit)
        replacement.await()
        editor.await()

        assertEquals(emptyList<ClassEntity>(), classesBeforeCommit)
        assertTrue(backup.snapshot().semesters.flatMap { it.classes }.isEmpty())
    }

    @Test
    fun editorAlreadyAdmittedMakesRoomDownloadWaitWithoutArchivingOrReplacing() = runBlocking {
        val local = plan("Lokalny")
        val remote = plan("Z Dysku")
        backup.replaceAll(local)
        val localSnapshot = gateway.snapshot()
        val localBytes = SyncPlanFile.encode(localSnapshot)
        val oldRemoteMd5 = md5(SyncPlanFile.encode(plan("Poprzedni Dysk")))
        val remoteBytes = SyncPlanFile.encode(remote)
        val account = SyncAccount("sub", "test@example.com", "test@example.com")
        val transport = MutablePlanTransport(remoteBytes)
        val store = TestStore(
            SyncState(
                account = account,
                remoteMd5 = oldRemoteMd5,
                localFingerprint = SyncPlanFile.fingerprint(localBytes),
                lastRunUploaded = true
            )
        )
        val archiveDirectory = File(
            ApplicationProvider.getApplicationContext<Context>().cacheDir,
            "room-edit-first-${System.nanoTime()}"
        )
        val archive = SyncArchive(archiveDirectory)
        val tracker = PlanEditTracker(CoroutineScope(Dispatchers.Unconfined))
        val coordinator = SyncCoordinator(
            gateway = gateway,
            transport = transport,
            store = store,
            archive = archive,
            editTracker = tracker,
            clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC),
            ioDispatcher = Dispatchers.Unconfined
        )
        tracker.beginEdit("class-form")

        assertEquals(SyncOutcome.WaitingForEditor, coordinator.synchronize())
        assertEquals(listOf("Lokalny"), gateway.snapshot().semesters.map { it.semester.name })
        assertTrue(archive.list().isEmpty())

        tracker.endEdit("class-form")
        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
        assertEquals(listOf("Z Dysku"), gateway.snapshot().semesters.map { it.semester.name })
        assertEquals(1, archive.list().size)
        archiveDirectory.deleteRecursively()
        Unit
    }

    @Test
    fun replacementKeepsLocalActiveSemesterOfTheSameLineage() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 2))

        val replaced = gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("A", "B", "C"), keepLocalActive = true, fallbackActiveId = 3
        )

        assertTrue(replaced)
        assertEquals(2L, gateway.snapshot().activeSemesterId)
    }

    @Test
    fun firstReplacementUsesTheFallbackSemester() = runBlocking {
        backup.replaceAll(plan("A", "B", active = 1))

        gateway.replaceIfUnchanged(
            SyncPlanFile.fingerprint(gateway.snapshot()), plan("X", "Y", "Z"), keepLocalActive = false, fallbackActiveId = 3
        )

        assertEquals(3L, gateway.snapshot().activeSemesterId)
    }

    @Test
    fun coordinatorDoesNotReplaceRoomPlanWithIncompleteRemoteJson() = runBlocking {
        val account = SyncAccount("sub", "test@example.com", "test@example.com")
        val transport = MutablePlanTransport(SyncPlanFile.encode(plan("Przyjęty plan")))
        val coordinator = SyncCoordinator(
            gateway = gateway,
            transport = transport,
            store = TestStore(SyncState(account = account)),
            archive = SyncArchive(ApplicationProvider.getApplicationContext<Context>().cacheDir.resolve("room-sync-archive")),
            editTracker = PlanEditTracker(CoroutineScope(Dispatchers.Unconfined)),
            clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC),
            ioDispatcher = Dispatchers.Unconfined
        )
        val acceptedNames = listOf("Przyjęty plan")

        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
        assertEquals(acceptedNames, gateway.snapshot().semesters.map { it.semester.name })
        transport.bytes = "{}".toByteArray()

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())
        assertEquals(acceptedNames, gateway.snapshot().semesters.map { it.semester.name })
        assertEquals(SyncIssue.INVALID_REMOTE_PLAN, coordinator.state.value.issue)
        assertEquals("{}", transport.bytes.decodeToString())
    }

    private class TestStore(private var state: SyncState) : SyncStateStore {
        override fun load() = state
        override fun save(state: SyncState) { this.state = state }
    }

    private class MutablePlanTransport(var bytes: ByteArray) : PlanFileTransport {
        override suspend fun find(account: SyncAccount) = RemotePlanFile("file", md5(bytes))
        override suspend fun download(account: SyncAccount, file: RemotePlanFile) = bytes
        override suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile =
            error("Invalid remote content must not be overwritten")
        override suspend fun deleteAll(account: SyncAccount) = Unit
    }

    private fun plan(vararg names: String, active: Long? = null) = BackupData(
        studyPrograms = listOf(StudyProgramEntity(id = 1, name = "Informatyka", color = "#137B71")),
        semesters = names.mapIndexed { index, name ->
            val id = index + 1L
            SemesterBackup(
                semester = SemesterEntity(id = id, name = name, isActive = id == active),
                calendars = listOf(AcademicCalendarEntity(id, id, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31), WeekType.A)),
                programs = listOf(SemesterProgramEntity(id, id, 1, id)),
                classes = emptyList(),
                weekOverrides = emptyList(),
                occurrenceNotes = emptyList(),
                occurrenceChanges = emptyList()
            )
        }
    )

    private fun BackupData.withOneClass(): BackupData = copy(
        semesters = semesters.mapIndexed { index, semester ->
            if (index != 0) semester else semester.copy(
                classes = listOf(
                    ClassEntity(
                        id = 1,
                        semesterId = 1,
                        semesterProgramId = 1,
                        name = "Lokalne zajęcia",
                        type = "Wykład",
                        teacherName = null,
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        room = null,
                        building = null,
                        group = null,
                        recurrence = Recurrence.EVERY_WEEK,
                        date = null,
                        classNote = null
                    )
                )
            )
        }
    )
}
