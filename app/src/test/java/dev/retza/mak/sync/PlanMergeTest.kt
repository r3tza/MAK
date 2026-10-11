package dev.retza.mak.sync

import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.toEntity
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanMergeTest {
    private fun merge(phone: BackupData, drive: BackupData, base: BackupData, pick: (PlanDifference) -> PlanSide): PlanMergeResult {
        val differences = planDifferences(phone, drive, base)
        return mergePlans(phone, drive, differences, differences.associateWith(pick))
    }

    private fun PlanMergeResult.data(): BackupData = when (this) {
        is PlanMergeResult.Ready -> data
        else -> throw AssertionError("Expected a ready plan, got $this")
    }

    @Test
    fun picksTakeTheRoomFromDriveAndTheNoteFromThePhone() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Zadania z listy 3")))
        val drive = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16, room = "B204")))

        val merged = merge(phone, drive, base) { if (it.key.kind == PlanRowKind.CLASS) PlanSide.DRIVE else PlanSide.PHONE }.data()

        val semester = merged.semesters.single()
        assertEquals("B204", semester.classes.single().room)
        assertEquals("Zadania z listy 3", semester.occurrenceNotes.single().body)
        assertTrue(semester.semester.isActive)
    }

    @Test
    fun theRoomFromDriveAndTheClassNoteFromThePhoneBothStay() {
        val base = basePlan()
        val phone = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16).copy(classNote = "Telefon")))
        val drive = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16, room = "B204")))

        val merged = merge(phone, drive, base) { if (it.field == ClassField.ROOM) PlanSide.DRIVE else PlanSide.PHONE }.data()

        val saved = merged.semesters.single().classes.single()
        assertEquals("B204", saved.room)
        assertEquals("Telefon", saved.classNote)
    }

    @Test
    fun keepingBothCollidingClassesRenumbersTheDriveOneAndItsNote() {
        val base = basePlan()
        val shared = base.semesters[0].classes.map { it.toEntity() }
        val phone = basePlan(classes = shared + classRow(2, "Bazy danych", DayOfWeek.MONDAY, 12))
        val drive = basePlan(
            classes = shared + classRow(2, "Statystyka", DayOfWeek.TUESDAY, 10),
            notes = listOf(noteRow(5, classId = 2, body = "Kolokwium", date = TODAY.minusDays(2)))
        )

        val merged = merge(phone, drive, base) { if (it.phoneKey != null) PlanSide.PHONE else PlanSide.DRIVE }.data()

        val classes = merged.semesters.single().classes.associateBy { it.name }
        assertEquals(2L, classes.getValue("Bazy danych").id)
        assertEquals(3L, classes.getValue("Statystyka").id)
        assertEquals(3L, merged.semesters.single().occurrenceNotes.single().classId)
    }

    @Test
    fun takingDriveRemovesAClassThatDriveDoesNotHave() {
        val base = basePlan()
        val phone = basePlan(classes = base.semesters[0].classes.map { it.toEntity() } + classRow(2, "Bazy danych", DayOfWeek.MONDAY, 12))

        val merged = merge(phone, base, base) { PlanSide.DRIVE }.data()

        assertEquals(listOf(1L), merged.semesters.single().classes.map { it.id })
    }

    @Test
    fun driveClassNeedingAProgramLeftOutIsAProblem() {
        val base = basePlan()
        val drive = basePlan(
            programs = base.studyPrograms.map { it.toEntity() } + StudyProgramEntity(id = 2, name = "Ekonometria", color = "#A65724"),
            assignments = base.semesters[0].programs.map { it.toEntity() } + SemesterProgramEntity(id = 2, semesterId = 1, studyProgramId = 2, academicCalendarId = 1),
            classes = base.semesters[0].classes.map { it.toEntity() } + classRow(2, "Statystyka", DayOfWeek.TUESDAY, 10, semesterProgramId = 2)
        )

        val result = merge(base, drive, base) { if (it.key.kind == PlanRowKind.CLASS) PlanSide.DRIVE else PlanSide.PHONE }

        val problems = (result as PlanMergeResult.Problems).problems
        assertEquals(PlanRowKey(PlanRowKind.CLASS, 2), problems.single().child)
        assertEquals(PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, 2), problems.single().parent)
        assertEquals(PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, 2), problems.single().parentDifference?.key)
    }

    @Test
    fun twoNotesForOneOccurrenceAreInvalid() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Telefon")))
        val drive = basePlan(notes = listOf(noteRow(2, classId = 1, body = "Dysk")))

        val result = merge(phone, drive, base) { if (it.phoneKey != null) PlanSide.PHONE else PlanSide.DRIVE }

        assertTrue(result is PlanMergeResult.Invalid)
    }

    @Test
    fun referencesFollowWithReferences() {
        val calendar = AcademicCalendarRecord(id = 1, semesterId = 1, startDate = TODAY, endDate = TODAY, firstWeekType = WeekType.A)
        val semesterProgram = SemesterProgramRecord(id = 2, semesterId = 1, studyProgramId = 3, academicCalendarId = 4)
        val classRecord = ClassRecord(
            id = 5, semesterId = 1, semesterProgramId = 2, name = "Konsultacje", type = "Wykład", teacherName = null,
            dayOfWeek = DayOfWeek.THURSDAY, startTime = LocalTime.of(16, 0), endTime = LocalTime.of(17, 0), room = null,
            building = null, group = null, recurrence = Recurrence.EVERY_WEEK, date = null, classNote = null
        )
        val override = WeekOverrideRecord(
            id = 6, semesterId = 1, academicCalendarId = 4, weekStartDate = TODAY, weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val note = OccurrenceNoteRecord(id = 7, semesterId = 1, classId = 5, occurrenceDate = TODAY, body = "Zadania")
        val change = OccurrenceChangeRecord(
            id = 8, semesterId = 1, classId = 5, originalDate = TODAY, kind = OccurrenceChangeKind.CANCELLED,
            targetDate = null, startTime = null, endTime = null, room = null, building = null, teacherName = null, note = null
        )

        assertEquals(listOf(PlanRowKey(PlanRowKind.SEMESTER, 1)), calendar.references())
        assertEquals(
            listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.STUDY_PROGRAM, 3), PlanRowKey(PlanRowKind.CALENDAR, 4)),
            semesterProgram.references()
        )
        assertEquals(
            listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, 2)),
            classRecord.references()
        )
        assertEquals(
            listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.CALENDAR, 4)),
            override.references()
        )
        assertEquals(
            listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.CLASS, 5)),
            note.references()
        )
        assertEquals(
            listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.CLASS, 5)),
            change.references()
        )
        assertEquals(emptyList<PlanRowKey>(), StudyProgramRecord(id = 9, name = "Informatyka", color = "#137B71").references())
        assertEquals(emptyList<PlanRowKey>(), SemesterRecord(id = 1, name = "Zimowy").references())
    }

    @Test(expected = IllegalArgumentException::class)
    fun aMissingPickIsAProgrammingError() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Telefon")))
        val differences = planDifferences(phone, base, base)

        mergePlans(phone, base, differences, emptyMap())
    }
}
