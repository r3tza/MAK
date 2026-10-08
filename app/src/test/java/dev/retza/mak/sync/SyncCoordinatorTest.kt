package dev.retza.mak.sync

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncCoordinatorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val account = SyncAccount("sub-1", "ala@example.com", "ala@example.com")
    private val phone = FakeGateway()
    private val drive = FakeDrive()
    private val editors = PlanEditTracker(
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), graceMillis = 0
    )
    private val archive by lazy { SyncArchive(folder.newFolder("archive")) }
    private val coordinator by lazy {
        SyncCoordinator(phone, drive, InMemoryStore(SyncState(account = account)), archive, editors,
            Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC), kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @Test
    fun firstConnectionOfEmptyPhoneDownloadsPlanWithoutAsking() = runTest {
        drive.put(plan("Zimowy"))

        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
        assertEquals(listOf("Zimowy"), phone.names())
        assertEquals(emptyList<ArchivedPlan>(), archive.list())
    }

    @Test
    fun firstConnectionWithTwoDifferentPlansAsksAndChangesNothing() = runTest {
        phone.data = plan("Telefon")
        drive.put(plan("Dysk"))

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        assertEquals(listOf("Telefon"), phone.names())
        assertEquals(0, drive.uploads)
    }

    @Test
    fun phoneWithOnlyStudyProgramsAsksAndCountsThem() = runTest {
        phone.data = plan()
        drive.put(plan("Dysk"))

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        assertEquals(PlanSummary(0, 0, 1), coordinator.state.value.pendingChoice?.local)
    }

    @Test
    fun keepingDriveArchivesThePhonePlanBeforeReplacingIt() = runTest {
        phone.data = plan("Telefon")
        drive.put(plan("Dysk"))
        coordinator.synchronize()

        assertEquals(SyncOutcome.Downloaded, coordinator.resolveChoice(SyncChoice.KEEP_DRIVE))
        assertEquals(listOf("Dysk"), phone.names())
        val archived = archive.list().single()
        assertEquals(ArchiveSource.PHONE, archived.source)
        assertEquals(listOf("Telefon"), SyncPlanFile.decode(archive.read(archived.id)!!).names())
    }

    @Test
    fun keepingPhoneArchivesTheDrivePlanBeforeOverwritingIt() = runTest {
        phone.data = plan("Telefon")
        drive.put(plan("Dysk"))
        coordinator.synchronize()

        assertEquals(SyncOutcome.Uploaded, coordinator.resolveChoice(SyncChoice.KEEP_PHONE))
        assertEquals(listOf("Telefon"), drive.plan().names())
        assertEquals(ArchiveSource.DRIVE, archive.list().single().source)
    }

    @Test
    fun choiceIsAskedAgainWhenDriveChangedAfterTheQuestion() = runTest {
        phone.data = plan("Telefon")
        drive.put(plan("Dysk"))
        coordinator.synchronize()
        drive.put(plan("Dysk nowszy"))

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.resolveChoice(SyncChoice.KEEP_PHONE))
        assertEquals(listOf("Dysk nowszy"), drive.plan().names())
    }

    @Test
    fun changeOnOneSideOnlyIsTakenWithoutAsking() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()

        phone.data = plan("Wspólny", "Letni")
        assertEquals(SyncOutcome.Uploaded, coordinator.synchronize())
        assertEquals(listOf("Wspólny", "Letni"), drive.plan().names())

        drive.put(plan("Wspólny", "Letni", "Kolejny"))
        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
        assertEquals(listOf("Wspólny", "Letni", "Kolejny"), phone.names())
    }

    @Test
    fun changesOnBothSidesAfterSynchronizationAsk() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()

        phone.data = plan("Wspólny", "Z telefonu")
        drive.put(plan("Wspólny", "Z Dysku"))

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        assertEquals(1, drive.uploads)
    }

    @Test
    fun activeSemesterChangeIsNotAPlanChange() = runTest {
        phone.data = plan("A", "B", active = 1)
        coordinator.synchronize()

        phone.data = plan("A", "B", active = 2)
        assertEquals(SyncOutcome.UpToDate, coordinator.synchronize())
        assertEquals(1, drive.uploads)
    }

    @Test
    fun driveChangedRightBeforeUploadIsNotOverwritten() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()
        phone.data = plan("Wspólny", "Z telefonu")
        drive.finds = 0
        drive.beforeFind = { if (drive.finds == 2) drive.put(plan("Wspólny", "Z Dysku")) }

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        assertEquals(listOf("Wspólny", "Z Dysku"), drive.plan().names())
    }

    @Test
    fun localEditDuringDownloadIsKeptAndAskedAbout() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()
        drive.put(plan("Wspólny", "Z Dysku"))
        phone.beforeReplace = { phone.data = plan("Wspólny", "Edycja w trakcie") }

        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        assertEquals(listOf("Wspólny", "Edycja w trakcie"), phone.names())
    }

    @Test
    fun invalidDriveFileLeavesBothSidesUnchanged() = runTest {
        phone.data = plan("Telefon")
        drive.putRaw("""{"schemaVersion": 99}""".toByteArray())

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())
        assertEquals(listOf("Telefon"), phone.names())
        assertEquals(0, drive.uploads)
        assertEquals(SyncIssue.INVALID_REMOTE_PLAN, coordinator.state.value.issue)
    }

    @Test
    fun incompleteRemoteFileAfterFollowingAnotherPhoneIsRecordedAndKeptUnchanged() = runTest {
        drive.put(plan("Plan przyjęty od drugiego telefonu"))
        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
        val acceptedPlan = phone.data
        val acceptedRemote = "{}".toByteArray()
        drive.putRaw(acceptedRemote)

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())

        assertEquals(acceptedPlan, phone.data)
        assertEquals(acceptedRemote.toList(), drive.file!!.toList())
        assertEquals(SyncIssue.INVALID_REMOTE_PLAN, coordinator.state.value.issue)
        assertEquals(0, drive.uploads)
    }

    @Test
    fun oversizedLocalPlanIsNotUploadedAndGetsItsOwnPersistentIssue() = runTest {
        phone.data = planWithEncodedSize(SyncPlanFile.MAX_BYTES + 1)
        val fingerprint = SyncPlanFile.fingerprint(phone.data)

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())

        assertEquals(SyncIssue.LOCAL_PLAN_TOO_LARGE, coordinator.state.value.issue)
        assertTrue(coordinator.state.value.issueMessage.orEmpty().lowercase().contains("lokal"))
        assertNull(drive.file)
        assertEquals(0, drive.uploads)
        assertEquals(fingerprint.length, 64)
    }

    @Test
    fun localPlanAtTheSizeLimitCanBeUploaded() = runTest {
        phone.data = planWithEncodedSize(SyncPlanFile.MAX_BYTES)

        assertEquals(SyncOutcome.Uploaded, coordinator.synchronize())
        assertEquals(SyncPlanFile.MAX_BYTES, drive.file!!.size)
        assertEquals(1, drive.uploads)
    }

    @Test
    fun oversizedPhonePlanIsNotArchivedOrUploadedWhenKeepingPhone() = runTest {
        phone.data = planWithEncodedSize(SyncPlanFile.MAX_BYTES + 1)
        drive.put(plan("Dysk"))
        assertEquals(SyncOutcome.ChoiceRequired, coordinator.synchronize())
        val remoteBeforeChoice = drive.file!!.toList()

        assertEquals(SyncOutcome.NeedsAttention, coordinator.resolveChoice(SyncChoice.KEEP_PHONE))

        assertEquals(remoteBeforeChoice, drive.file!!.toList())
        assertEquals(emptyList<ArchivedPlan>(), archive.list())
        assertEquals(SyncIssue.LOCAL_PLAN_TOO_LARGE, coordinator.state.value.issue)
    }

    @Test
    fun downloadWaitsWhileAnEditorIsOpen() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()
        drive.put(plan("Wspólny", "Z Dysku"))
        editors.beginEdit("form")

        assertEquals(SyncOutcome.WaitingForEditor, coordinator.synchronize())
        assertEquals(listOf("Wspólny"), phone.names())

        editors.release("form").join()
        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())
    }

    @Test
    fun disconnectKeepsThePhonePlan() = runTest {
        phone.data = plan("Telefon")
        coordinator.synchronize()

        coordinator.disconnect(deleteRemote = true)

        assertEquals(listOf("Telefon"), phone.names())
        assertNull(drive.file)
        assertNull(coordinator.state.value.account)
    }

    @Test
    fun followingTheOtherPhoneDoesNotFillTheArchive() = runTest {
        phone.data = plan("Wspólny")
        drive.put(plan("Wspólny"))
        coordinator.synchronize()

        drive.put(plan("Wspólny", "Z Dysku"))
        coordinator.synchronize()
        drive.put(plan("Wspólny", "Z Dysku", "Kolejny"))
        coordinator.synchronize()

        assertEquals(emptyList<ArchivedPlan>(), archive.list())
    }

    @Test
    fun downloadAfterOwnUploadArchivesThePhonePlan() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()
        phone.data = plan("Wspólny", "Z telefonu")
        coordinator.synchronize()
        coordinator.synchronize()

        drive.put(plan("Wspólny", "Z Dysku"))
        assertEquals(SyncOutcome.Downloaded, coordinator.synchronize())

        val archived = archive.list().single()
        assertEquals(listOf("Wspólny", "Z telefonu"), SyncPlanFile.decode(archive.read(archived.id)!!).names())
    }

    @Test
    fun firstDownloadActivatesTheSemesterOfToday() = runTest {
        drive.put(
            plan("Stary", "Bieżący", "Przyszły", starts = listOf(
                LocalDate.of(2025, 10, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2027, 2, 20)
            ))
        )

        coordinator.synchronize()

        assertEquals(2L, phone.data.activeSemesterId)
    }

    @Test
    fun laterDownloadKeepsTheLocalActiveSemester() = runTest {
        phone.data = plan("A", "B", active = 2)
        coordinator.synchronize()
        drive.put(plan("A", "B", "C"))

        coordinator.synchronize()

        assertEquals(2L, phone.data.activeSemesterId)
    }

    @Test
    fun semesterOfTodayFallsBackToTheLatestStart() {
        val data = plan("Stary", "Nowszy", starts = listOf(LocalDate.of(2024, 10, 1), LocalDate.of(2025, 10, 1)))

        assertEquals(2L, data.semesterCovering(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun deniedDriveAccessIsRecordedForTheUser() = runTest {
        phone.data = plan("Telefon")
        drive.failure = DriveHttpException(403, "quota")

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())
        assertEquals(SyncIssue.FAILED, coordinator.state.value.issue)
    }

    @Test
    fun unreadableDriveResponseDoesNotAskToReconnect() = runTest {
        phone.data = plan("Telefon")
        drive.failure = DriveHttpException(0, "unreadable")

        runCatching { coordinator.synchronize() }

        assertNull(coordinator.state.value.issue)
    }

    @Test
    fun otherGoogleAccountTellsToConnectAgain() = runTest {
        phone.data = plan("Telefon")
        drive.failure = AccountMismatchException()

        assertEquals(SyncOutcome.NeedsAttention, coordinator.synchronize())
        assertEquals(SyncIssue.FAILED, coordinator.state.value.issue)
        assertEquals(true, coordinator.state.value.issueMessage.orEmpty().contains("Wyłącz synchronizację"))
    }

    @Test
    fun networkErrorIsLeftForRetryWithoutAnIssue() = runTest {
        phone.data = plan("Telefon")
        drive.failure = java.io.IOException("offline")

        val thrown = runCatching { coordinator.synchronize() }.exceptionOrNull()

        assertEquals(java.io.IOException::class, thrown?.let { it::class })
        assertNull(coordinator.state.value.issue)
    }

    @Test
    fun stateIsWrittenOnTheInjectedDispatcherNotTheCaller() = runTest {
        val writes = mutableListOf<String>()
        val store = object : SyncStateStore {
            override fun load() = SyncState(account = account)
            override fun save(state: SyncState) {
                writes += Thread.currentThread().name
            }
        }
        val io = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "sync-io") }
        val dispatcher = io.asCoroutineDispatcher()
        val onIo = SyncCoordinator(phone, drive, store, archive, editors,
            Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC), dispatcher)
        phone.data = plan("Telefon")

        onIo.synchronize()
        io.shutdown()

        assertEquals(listOf("sync-io"), writes.distinct())
    }

    @Test
    fun archiveKeepsTheTenNewestPlans() {
        repeat(11) { index -> archive.add(byteArrayOf(index.toByte()), ArchiveSource.PHONE, 1_000L + index) }

        val kept = archive.list()
        assertEquals(10, kept.size)
        assertEquals(1_010L, kept.first().createdAtMillis)
        assertEquals(1_001L, kept.last().createdAtMillis)
    }
}

private fun BackupData.names() = semesters.map { it.semester.name }

private fun plan(vararg names: String, active: Long? = null, starts: List<LocalDate> = emptyList()): BackupData = BackupData(
    studyPrograms = listOf(StudyProgramEntity(id = 1, name = "Informatyka", color = "#137B71")),
    semesters = names.mapIndexed { index, name ->
        val id = index + 1L
        SemesterBackup(
            semester = SemesterEntity(id = id, name = name, isActive = id == (active ?: 1L)),
            calendars = listOf(
                starts.getOrNull(index).let { start ->
                    val from = start ?: LocalDate.of(2026, 10, 1)
                    AcademicCalendarEntity(id, id, from, from.plusMonths(4), WeekType.A)
                }
            ),
            programs = listOf(SemesterProgramEntity(id, id, 1, id)),
            classes = emptyList(),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList()
        )
    }
)

private fun planWithEncodedSize(encodedSize: Int): BackupData {
    val base = plan("Plan")
    val existingName = base.studyPrograms.single().name
    val baseSize = SyncPlanFile.encode(base).size
    return base.copy(
        studyPrograms = listOf(
            base.studyPrograms.single().copy(name = "x".repeat(existingName.length + encodedSize - baseSize))
        )
    )
}

private class FakeGateway : PlanSyncGateway {
    var data = BackupData(emptyList(), emptyList())
    var beforeReplace: () -> Unit = {}

    fun names() = data.names()

    override suspend fun snapshot() = data

    override suspend fun replaceIfUnchanged(
        expectedFingerprint: String,
        data: BackupData,
        keepLocalActive: Boolean,
        fallbackActiveId: Long?
    ): Boolean {
        beforeReplace()
        beforeReplace = {}
        if (SyncPlanFile.fingerprint(this.data) != expectedFingerprint) return false
        this.data = data.withActiveSemester(
            activeSemesterAfterReplace(this.data.activeSemesterId, data, keepLocalActive, fallbackActiveId)
        )
        return true
    }
}

private class FakeDrive : PlanFileTransport {
    var file: ByteArray? = null
    var uploads = 0
    var finds = 0
    var beforeFind: () -> Unit = {}
    var failure: Exception? = null

    fun put(data: BackupData) = putRaw(SyncPlanFile.encode(data))

    fun putRaw(bytes: ByteArray) {
        file = bytes
    }

    fun plan() = SyncPlanFile.decode(file!!)

    override suspend fun find(account: SyncAccount): RemotePlanFile? {
        finds += 1
        failure?.let { throw it }
        beforeFind()
        return file?.let { RemotePlanFile("file-1", md5(it)) }
    }

    override suspend fun download(account: SyncAccount, file: RemotePlanFile) = this.file!!

    override suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile {
        uploads += 1
        file = bytes
        return RemotePlanFile("file-1", md5(bytes))
    }

    override suspend fun deleteAll(account: SyncAccount) {
        file = null
    }
}

private class InMemoryStore(private var state: SyncState) : SyncStateStore {
    override fun load() = state

    override fun save(state: SyncState) {
        this.state = state
    }
}
