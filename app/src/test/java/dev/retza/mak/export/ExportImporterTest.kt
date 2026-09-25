package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportImporterTest {
    @Test
    fun prepareAcceptsSupportedSnapshot() {
        val result = ExportImporter.prepare(validSnapshot())

        assertTrue(result is ImportSnapshotResult.Ready)
        val data = (result as ImportSnapshotResult.Ready).data
        assertEquals(1, data.studyPrograms.size)
        assertEquals(1L, data.activeSemesterId)
        assertEquals(1, data.semesters.single().classes.size)
        assertEquals(1, data.semesters.single().calendars.size)
    }

    @Test
    fun prepareRejectsUnsupportedVersion() {
        val result = ExportImporter.prepare(validSnapshot().copy(schemaVersion = 1))

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.single()
                .contains("Nieobsługiwana wersja pliku")
        )
    }

    @Test
    fun versionTwoFileMovesNoteToOriginalDateOfMovedOccurrence() {
        val result = ExportImporter.prepare(snapshotWithMovedOccurrenceNote().copy(schemaVersion = 2))

        val note = (result as ImportSnapshotResult.Ready).data.semesters.single().occurrenceNotes.single()
        assertEquals(LocalDate.of(2026, 10, 6), note.occurrenceDate)
        assertEquals("Oddać projekt", note.body)
    }

    @Test
    fun currentVersionFileKeepsNoteDate() {
        val snapshot = snapshotWithMovedOccurrenceNote()
        assertEquals(3, snapshot.schemaVersion)

        val result = ExportImporter.prepare(snapshot)

        val note = (result as ImportSnapshotResult.Ready).data.semesters.single().occurrenceNotes.single()
        assertEquals(LocalDate.of(2026, 10, 8), note.occurrenceDate)
    }

    @Test
    fun prepareRejectsOrphanClass() {
        val snapshot = validSnapshot()
        val semester = snapshot.semesters.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(
                    classes = semester.classes.map { it.copy(semesterProgramId = 999) }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("nieistniejące przypisanie kierunku")
            }
        )
    }

    @Test
    fun prepareRejectsCrossSemesterCalendar() {
        val snapshot = validSnapshot()
        val semester = snapshot.semesters.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(
                    calendars = semester.calendars.map { it.copy(semesterId = 999) }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("innego semestru")
            }
        )
    }

    @Test
    fun prepareRejectsTwoActiveSemesters() {
        val snapshot = ExportSnapshot.from(
            semesters = listOf(
                semesterData(1, active = true),
                semesterData(2, active = true)
            ),
            studyPrograms = listOf(program())
        )

        val result = ExportImporter.prepare(snapshot)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("więcej niż jeden aktywny semestr")
            }
        )
    }

    @Test
    fun prepareRejectsOneOffWithoutDate() {
        val snapshot = validSnapshot()
        val semester = snapshot.semesters.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(
                    classes = semester.classes.map {
                        it.copy(recurrence = Recurrence.ONCE.name, date = null)
                    }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("nie mają daty")
            }
        )
    }

    @Test
    fun prepareRejectsDuplicateStudyProgramIds() {
        val snapshot = validSnapshot().copy(
            studyPrograms = listOf(
                StudyProgramSnapshot(3, "Informatyka", "#111111"),
                StudyProgramSnapshot(3, "Fizyka", "#222222")
            )
        )

        val result = ExportImporter.prepare(snapshot)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("powtórzonym identyfikatorze")
            }
        )
    }

    @Test
    fun prepareRejectsInvalidChangeTimeInsteadOfDroppingIt() {
        val snapshot = snapshotWithChange(
            OccurrenceChangeEntity(
                id = 20,
                semesterId = 1,
                classId = 1000,
                originalDate = LocalDate.of(2026, 10, 13),
                kind = OccurrenceChangeKind.MODIFIED,
                targetDate = null,
                newStartTime = LocalTime.of(9, 0),
                newEndTime = LocalTime.of(10, 0),
                newRoom = null,
                newBuilding = null,
                newTeacherName = null,
                newNote = null
            )
        )
        val semester = snapshot.semesters.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(
                    occurrenceChanges = semester.occurrenceChanges.map {
                        it.copy(startTime = "niepoprawna")
                    }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("niepoprawną godzinę początku")
            }
        )
    }

    @Test
    fun prepareRejectsChangeEndBeforeStart() {
        val snapshot = snapshotWithChange(
            OccurrenceChangeEntity(
                id = 20,
                semesterId = 1,
                classId = 1000,
                originalDate = LocalDate.of(2026, 10, 13),
                kind = OccurrenceChangeKind.MODIFIED,
                targetDate = null,
                newStartTime = LocalTime.of(11, 0),
                newEndTime = LocalTime.of(9, 0),
                newRoom = null,
                newBuilding = null,
                newTeacherName = null,
                newNote = null
            )
        )

        val result = ExportImporter.prepare(snapshot)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("koniec przed początkiem")
            }
        )
    }

    @Test
    fun prepareRejectsDuplicateCalendarIdsAcrossSemesters() {
        val snapshot = ExportSnapshot.from(
            semesters = listOf(
                semesterData(1, active = true),
                semesterData(2, active = false)
            ),
            studyPrograms = listOf(program())
        )
        val second = snapshot.semesters[1]
        val broken = snapshot.copy(
            semesters = listOf(
                snapshot.semesters[0],
                second.copy(
                    calendars = second.calendars.map {
                        it.copy(id = snapshot.semesters[0].calendars.single().id)
                    }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("kalendarze o powtórzonym identyfikatorze")
            }
        )
    }

    @Test
    fun prepareRejectsDuplicateAssignmentInSemester() {
        val snapshot = validSnapshot()
        val semester = snapshot.semesters.single()
        val assignment = semester.programs.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(programs = listOf(assignment, assignment.copy(id = 999)))
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("ten sam kierunek do semestru")
            }
        )
    }

    @Test
    fun prepareRejectsClassEndNotAfterStart() {
        val snapshot = validSnapshot()
        val semester = snapshot.semesters.single()
        val broken = snapshot.copy(
            semesters = listOf(
                semester.copy(
                    classes = semester.classes.map {
                        it.copy(startTime = "12:00", endTime = "11:00")
                    }
                )
            )
        )

        val result = ExportImporter.prepare(broken)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("kończą się przed początkiem")
            }
        )
    }

    @Test
    fun prepareRejectsWeekOverrideNotOnMonday() {
        val semester = semesterData(1, active = true).copy(
            weekOverrides = listOf(
                dev.retza.mak.data.entity.WeekOverrideEntity(
                    id = 40,
                    semesterId = 1,
                    academicCalendarId = 10,
                    weekStartDate = LocalDate.of(2026, 10, 6),
                    weekType = WeekType.A,
                    scope = dev.retza.mak.data.entity.WeekOverrideScope.ONE_WEEK
                )
            )
        )
        val snapshot = ExportSnapshot.from(listOf(semester), listOf(program()))

        val result = ExportImporter.prepare(snapshot)

        assertTrue(result is ImportSnapshotResult.Invalid)
        assertTrue(
            (result as ImportSnapshotResult.Invalid).errors.any {
                it.contains("musi zaczynać się w poniedziałek")
            }
        )
    }

    private fun snapshotWithChange(change: OccurrenceChangeEntity): ExportSnapshot {
        val base = semesterData(1, active = true).copy(occurrenceChanges = listOf(change))
        return ExportSnapshot.from(listOf(base), listOf(program()))
    }

    private fun snapshotWithMovedOccurrenceNote(): ExportSnapshot {
        val moved = OccurrenceChangeEntity(
            id = 20,
            semesterId = 1,
            classId = 1000,
            originalDate = LocalDate.of(2026, 10, 6),
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = LocalDate.of(2026, 10, 8),
            newStartTime = null,
            newEndTime = null,
            newRoom = null,
            newBuilding = null,
            newTeacherName = null,
            newNote = null
        )
        val note = OccurrenceNoteEntity(
            id = 30,
            semesterId = 1,
            classId = 1000,
            occurrenceDate = LocalDate.of(2026, 10, 8),
            body = "Oddać projekt"
        )
        val base = semesterData(1, active = true).copy(
            occurrenceChanges = listOf(moved),
            occurrenceNotes = listOf(note)
        )
        return ExportSnapshot.from(listOf(base), listOf(program()))
    }

    private fun program() = StudyProgramEntity(id = 3, name = "Informatyka", color = "#3366FF")

    private fun semesterData(id: Long, active: Boolean): SemesterWithData {
        val calendar = AcademicCalendarEntity(
            id = id * 10,
            semesterId = id,
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2027, 2, 15),
            firstWeekType = WeekType.B
        )
        val assignment = SemesterProgramEntity(
            id = id * 100,
            semesterId = id,
            studyProgramId = 3,
            academicCalendarId = calendar.id
        )
        val classEntity = ClassEntity(
            id = id * 1000,
            semesterId = id,
            semesterProgramId = assignment.id,
            name = "Programowanie",
            type = "Wykład",
            teacherName = "Jan Kowalski",
            dayOfWeek = DayOfWeek.TUESDAY,
            startTime = LocalTime.of(10, 15),
            endTime = LocalTime.of(12, 0),
            room = null,
            building = null,
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
        return SemesterWithData(
            semester = SemesterEntity(id = id, name = "Semestr $id", isActive = active),
            semesterPrograms = listOf(assignment),
            academicCalendars = listOf(calendar),
            studyPrograms = listOf(program()),
            classes = listOf(classEntity),
            weekOverrides = emptyList(),
            occurrenceNotes = emptyList(),
            occurrenceChanges = emptyList()
        )
    }

    private fun validSnapshot(): ExportSnapshot =
        ExportSnapshot.from(
            semesters = listOf(semesterData(1, active = true)),
            studyPrograms = listOf(program())
        )
}
