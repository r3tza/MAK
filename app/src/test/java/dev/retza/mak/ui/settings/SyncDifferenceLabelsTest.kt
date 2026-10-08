package dev.retza.mak.ui.settings

import dev.retza.mak.sync.PlanMergeProblem
import dev.retza.mak.sync.PlanRowKey
import dev.retza.mak.sync.PlanRowKind
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.sync.TODAY
import dev.retza.mak.sync.basePlan
import dev.retza.mak.sync.classRow
import dev.retza.mak.sync.noteRow
import dev.retza.mak.sync.planDifferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDifferenceLabelsTest {
    @Test
    fun aRoomChangeNamesTheClassTheFieldAndBothValues() {
        val base = basePlan()
        val drive = basePlan(classes = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16, room = "B204")))
        val difference = planDifferences(base, drive, base).single()

        val label = SyncDifferenceLabels(base, drive).describe(difference)

        assertEquals(SyncDifferenceUi("Konsultacje", "Sala, Czwartek, 16:00-17:00", "A12", "B204"), label)
    }

    @Test
    fun aNoteOnOneSideNamesTheDateAndShowsNoneOnTheOther() {
        val base = basePlan()
        val phone = basePlan(notes = listOf(noteRow(1, classId = 1, body = "Zadania z listy 3")))
        val difference = planDifferences(phone, base, base).single()

        val label = SyncDifferenceLabels(phone, base).describe(difference)

        assertEquals(SyncDifferenceUi("Konsultacje", "Notatka do terminu, 8 października", "Zadania z listy 3", "Brak"), label)
    }

    @Test
    fun aProblemNamesBothEntries() {
        val base = basePlan()
        val problem = PlanMergeProblem(
            child = PlanRowKey(PlanRowKind.CLASS, 1),
            childSide = PlanSide.PHONE,
            parent = PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, 1),
            childDifference = null,
            parentDifference = null
        )

        val text = SyncDifferenceLabels(base, base).describe(problem)

        assertEquals("„Konsultacje” wymaga: Informatyka. Wybierz tę samą wersję przy obu pozycjach.", text)
    }

    @Test
    fun changeLabelsSayTodayYesterdayOrTheDate() {
        val zone = ZoneOffset.UTC
        fun at(date: LocalDate) = date.atTime(12, 20).toInstant(zone).toEpochMilli()

        assertEquals("Zmieniony dziś o 12:20", changeLabel(at(TODAY), zone, TODAY))
        assertEquals("Zmieniony wczoraj o 12:20", changeLabel(at(TODAY.minusDays(1)), zone, TODAY))
        assertEquals("Zmieniony 1 października o 12:20", changeLabel(at(LocalDate.of(2026, 10, 1)), zone, TODAY))
        assertEquals(UNKNOWN_CHANGE, changeLabel(null, zone, TODAY))
    }
}
