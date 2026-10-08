package dev.retza.mak.sync

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDifferencesTest {
    @Test
    fun equalPlansHaveNoDifferences() {
        assertEquals(emptyList<PlanDifference>(), planDifferences(basePlan(), basePlan(), basePlan()))
    }

    @Test
    fun activeSemesterIsNotADifference() {
        assertEquals(emptyList<PlanDifference>(), planDifferences(basePlan(active = true), basePlan(active = false), basePlan()))
    }

    @Test
    fun roomOnOneSideAndNoteOnTheOtherAreTwoDifferences() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Zadania z listy 3")))
        val drive = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16, room = "B204")))

        val differences = planDifferences(phone, drive, base)

        assertEquals(
            listOf(PlanRowKey(PlanRowKind.CLASS, 1), PlanRowKey(PlanRowKind.OCCURRENCE_NOTE, 1)),
            differences.map { it.key }
        )
        assertEquals(PlanRowKey(PlanRowKind.OCCURRENCE_NOTE, 1), differences[1].phoneKey)
        assertEquals(null, differences[1].driveKey)
    }

    @Test
    fun aClassChangedOnBothSidesDiffersFieldByField() {
        val base = basePlan()
        val phone = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16).copy(classNote = "Telefon")))
        val drive = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16, room = "B204")))

        val differences = planDifferences(phone, drive, base)

        assertEquals(listOf(ClassField.ROOM, ClassField.NOTE), differences.map { it.field })
        assertTrue(differences.all { it.phoneKey != null && it.driveKey != null })
    }

    @Test
    fun classesAddedOnBothSidesUnderOneNumberAreTwoCollisions() {
        val base = basePlan()
        val phone = basePlan(classes = basePlan().semesters[0].classes + classRow(2, "Bazy danych", DayOfWeek.MONDAY, 12))
        val drive = basePlan(classes = basePlan().semesters[0].classes + classRow(2, "Statystyka", DayOfWeek.TUESDAY, 10))

        val differences = planDifferences(phone, drive, base)

        assertEquals(2, differences.size)
        assertTrue(differences.all { it.collision })
        assertEquals(1, differences.count { it.phoneKey != null })
        assertEquals(1, differences.count { it.driveKey != null })
    }

    @Test
    fun classDifferencesComeInWeekOrder() {
        val base = basePlan(classes = emptyList())
        val phone = basePlan(classes = listOf(classRow(1, "Czwartek", DayOfWeek.THURSDAY, 16), classRow(2, "Poniedziałek", DayOfWeek.MONDAY, 8)))

        val differences = planDifferences(phone, base, base)

        assertEquals(listOf(2L, 1L), differences.map { it.key.id })
    }
}
