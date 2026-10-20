package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.FreeDayEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway
import dev.retza.mak.settings.InMemorySettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanBackupServiceTest {
    private val preferences = InMemorySettingsPreferences()
    private val service = PlanBackupService(
        object : PlanBackupGateway {
            override suspend fun snapshot() = BackupData(emptyList(), emptyList())

            override suspend fun replaceAll(data: BackupData): Long? = null

            override suspend fun replaceIfCurrent(prepare: (current: BackupData) -> BackupData?): Boolean =
                prepare(snapshot())?.let { replaceAll(it); true } ?: false
        },
        preferences
    )

    @Test
    fun importShowsTheProgramsHiddenOnThisPhoneAgain() = runTest {
        preferences.setStudyProgramHidden(1L, hidden = true)
        val ready = service.prepareImport("""{"schemaVersion":3,"studyPrograms":[],"semesters":[]}""".toByteArray())

        service.confirmImport((ready as ImportPreparation.Ready).handle)

        assertTrue(preferences.planDisplay.first().hiddenProgramIds.isEmpty())
    }

    @Test
    fun fileFromANewerVersionAsksToUpdateTheApp() {
        val newer = """{"schemaVersion":5,"studyPrograms":[],"semesters":[],"daysOff":[]}"""

        val result = service.prepareImport(newer.toByteArray())

        assertEquals(
            ImportPreparation.Invalid(listOf("Plik pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.")),
            result
        )
    }

    @Test
    fun damagedFileStillCannotBeRead() {
        val result = service.prepareImport("{".toByteArray())

        assertEquals(ImportPreparation.Invalid(listOf("Nie udało się odczytać pliku.")), result)
    }

    @Test
    fun summaryCountsTheSameFreeDayRangeInSeveralCalendarsOnce() {
        fun calendar(id: Long) = AcademicCalendarEntity(
            id = id,
            semesterId = 1,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 15),
            firstWeekType = WeekType.A
        )
        fun freeDay(id: Long, calendarId: Long, day: LocalDate) = FreeDayEntity(
            id = id,
            semesterId = 1,
            academicCalendarId = calendarId,
            startDate = day,
            endDate = day,
            name = "Dzień rektorski"
        )
        val semester = SemesterWithData(
            semester = SemesterEntity(id = 1, name = "Semestr 1", isActive = true),
            semesterPrograms = emptyList(),
            academicCalendars = listOf(calendar(10), calendar(20)),
            studyPrograms = emptyList(),
            classes = emptyList(),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList(),
            freeDays = listOf(
                freeDay(1, 10, LocalDate.of(2026, 11, 11)),
                freeDay(2, 20, LocalDate.of(2026, 11, 11)),
                freeDay(3, 10, LocalDate.of(2026, 12, 24))
            )
        )
        val bytes = JsonExportCodec.encode(exportSnapshotOf(listOf(semester), emptyList()))

        val ready = service.prepareImport(bytes) as ImportPreparation.Ready

        assertEquals(2, ready.summary.freeDayCount)
    }
}
