package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TomorrowPlanTest {
    private val today = LocalDate.of(2026, 9, 21)
    private val tomorrow = LocalDate.of(2026, 9, 22)

    @Test
    fun dayWithoutClassesSwitchesAtSix() {
        assertEquals(LocalTime.of(18, 0), tomorrowSwitchTime(emptyList()))
    }

    @Test
    fun switchesTwoHoursAfterLastClassEnds() {
        val occurrences = listOf(
            occurrence(1L, LocalTime.of(9, 0), LocalTime.of(12, 0)),
            occurrence(2L, LocalTime.of(13, 0), LocalTime.of(15, 30))
        )

        assertEquals(LocalTime.of(17, 30), tomorrowSwitchTime(occurrences))
    }

    @Test
    fun lastClassEndingBeforeTenStillSwitches() {
        val occurrences = listOf(occurrence(1L, LocalTime.of(20, 0), LocalTime.of(21, 59)))

        assertEquals(LocalTime.of(23, 59), tomorrowSwitchTime(occurrences))
    }

    @Test
    fun lastClassEndingAtTenHasNoSwitch() {
        val occurrences = listOf(occurrence(1L, LocalTime.of(20, 0), LocalTime.of(22, 0)))

        assertNull(tomorrowSwitchTime(occurrences))
    }

    @Test
    fun showsTodayBeforeSwitchTimeAndTomorrowFromIt() {
        val switchTime = LocalTime.of(17, 30)

        assertEquals(today, shownPlanDate(today, LocalTime.of(17, 29), switchTime, enabled = true))
        assertEquals(tomorrow, shownPlanDate(today, LocalTime.of(17, 30), switchTime, enabled = true))
        assertEquals(tomorrow, shownPlanDate(today, LocalTime.of(23, 0), switchTime, enabled = true))
    }

    @Test
    fun showsTodayWhenDisabledOrWithoutSwitchTime() {
        assertEquals(today, shownPlanDate(today, LocalTime.of(23, 0), LocalTime.of(17, 30), enabled = false))
        assertEquals(today, shownPlanDate(today, LocalTime.of(23, 0), null, enabled = true))
    }

    private fun occurrence(id: Long, start: LocalTime, end: LocalTime) = PlannedOccurrence(
        classItem = ClassItem(
            id = id,
            semesterId = 1L,
            semesterProgramId = 1L,
            name = "Zajęcia $id",
            type = "Wykład",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = start,
            endTime = end
        ),
        date = today,
        originalDate = today,
        startTime = start,
        endTime = end,
        room = null,
        building = null,
        teacherName = null,
        studyProgram = null,
        classNote = null,
        occurrenceNote = null
    )
}
