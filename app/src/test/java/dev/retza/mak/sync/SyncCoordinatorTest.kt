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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncCoordinatorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val account = SyncAccount("sub-1", "ala@example.com", "ala@example.com")
    private val phone = FakeGateway()
    private val drive = FakeDrive()
    private val editors = PlanEditTracker()
    private val archive by lazy { SyncArchive(folder.newFolder("archive")) }
    private val coordinator by lazy {
        SyncCoordinator(phone, drive, InMemoryStore(SyncState(account = account)), archive, editors,
            Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC))
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

        assertEquals(SyncOutcome.InvalidRemotePlan, coordinator.synchronize())
        assertEquals(listOf("Telefon"), phone.names())
        assertEquals(0, drive.uploads)
        assertEquals(SyncIssue.INVALID_REMOTE_PLAN, coordinator.state.value.issue)
    }

    @Test
    fun downloadWaitsWhileAnEditorIsOpen() = runTest {
        phone.data = plan("Wspólny")
        coordinator.synchronize()
        drive.put(plan("Wspólny", "Z Dysku"))
        editors.open()

        assertEquals(SyncOutcome.WaitingForEditor, coordinator.synchronize())
        assertEquals(listOf("Wspólny"), phone.names())

        editors.close()
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
    fun appliedPlanKeepsLocalActiveSemesterOrFallsBackToFirst() {
        val data = plan("A", "B")

        assertEquals(2L, data.withActiveSemester(2L).activeSemesterId)
        assertEquals(1L, data.withActiveSemester(7L).activeSemesterId)
    }
}

private fun BackupData.names() = semesters.map { it.semester.name }

private fun plan(vararg names: String, active: Long? = null): BackupData = BackupData(
    studyPrograms = listOf(StudyProgramEntity(id = 1, name = "Informatyka", color = "#137B71")),
    semesters = names.mapIndexed { index, name ->
        val id = index + 1L
        SemesterBackup(
            semester = SemesterEntity(id = id, name = name, isActive = id == (active ?: 1L)),
            calendars = listOf(
                AcademicCalendarEntity(id, id, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31), WeekType.A)
            ),
            programs = listOf(SemesterProgramEntity(id, id, 1, id)),
            classes = emptyList(),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList()
        )
    }
)

private class FakeGateway : PlanSyncGateway {
    var data = BackupData(emptyList(), emptyList())
    var beforeReplace: () -> Unit = {}

    fun names() = data.names()

    override suspend fun snapshot() = data

    override suspend fun replaceIfUnchanged(expectedFingerprint: String, data: BackupData): Boolean {
        beforeReplace()
        beforeReplace = {}
        if (SyncPlanFile.fingerprint(this.data) != expectedFingerprint) return false
        this.data = data.withActiveSemester(this.data.activeSemesterId)
        return true
    }
}

private class FakeDrive : PlanFileTransport {
    var file: ByteArray? = null
    var uploads = 0
    var finds = 0
    var beforeFind: () -> Unit = {}

    fun put(data: BackupData) = putRaw(SyncPlanFile.encode(data))

    fun putRaw(bytes: ByteArray) {
        file = bytes
    }

    fun plan() = SyncPlanFile.decode(file!!)

    override suspend fun find(account: SyncAccount): RemotePlanFile? {
        finds += 1
        beforeFind()
        return file?.let { RemotePlanFile("file-1", md5(it)) }
    }

    override suspend fun download(account: SyncAccount, file: RemotePlanFile) = this.file!!

    override suspend fun upload(account: SyncAccount, existing: RemotePlanFile?, bytes: ByteArray): RemotePlanFile {
        uploads += 1
        file = bytes
        return RemotePlanFile("file-1", md5(bytes))
    }

    override suspend fun delete(account: SyncAccount, file: RemotePlanFile) {
        this.file = null
    }
}

private class InMemoryStore(private var state: SyncState) : SyncStateStore {
    override fun load() = state

    override fun save(state: SyncState) {
        this.state = state
    }
}
