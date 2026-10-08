package dev.retza.mak.sync

import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.repository.BackupData
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanMergeTest {
    private fun merge(phone: BackupData, drive: BackupData, base: BackupData, pick: (PlanDifference) -> PlanSide): PlanMergeResult {
        val differences = planDifferences(phone, drive, base)
        return mergePlans(phone, drive, differences, differences.associateWith(pick), TODAY)
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
    fun keepingBothCollidingClassesRenumbersTheDriveOneAndItsNote() {
        val base = basePlan()
        val shared = base.semesters[0].classes
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
        val phone = basePlan(classes = base.semesters[0].classes + classRow(2, "Bazy danych", DayOfWeek.MONDAY, 12))

        val merged = merge(phone, base, base) { PlanSide.DRIVE }.data()

        assertEquals(listOf(1L), merged.semesters.single().classes.map { it.id })
    }

    @Test
    fun driveClassNeedingAProgramLeftOutIsAProblem() {
        val base = basePlan()
        val drive = basePlan(
            programs = base.studyPrograms + StudyProgramEntity(id = 2, name = "Ekonometria", color = "#A65724"),
            assignments = base.semesters[0].programs + SemesterProgramEntity(id = 2, semesterId = 1, studyProgramId = 2, academicCalendarId = 1),
            classes = base.semesters[0].classes + classRow(2, "Statystyka", DayOfWeek.TUESDAY, 10, semesterProgramId = 2)
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

    @Test(expected = IllegalArgumentException::class)
    fun aMissingPickIsAProgrammingError() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Telefon")))
        val differences = planDifferences(phone, base, base)

        mergePlans(phone, base, differences, emptyMap(), TODAY)
    }
}
