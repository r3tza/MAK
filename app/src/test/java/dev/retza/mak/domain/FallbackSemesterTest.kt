package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FallbackSemesterTest {
    private val today = LocalDate.of(2026, 10, 10)

    private fun range(start: String, end: String) = LocalDate.parse(start)..LocalDate.parse(end)

    @Test
    fun coveringTodayWinsOverLaterStart() {
        val semesters = listOf(
            SemesterSpan(1, listOf(range("2026-09-01", "2026-12-31"))),
            SemesterSpan(2, listOf(range("2027-02-01", "2027-06-30")))
        )

        assertEquals(1L, fallbackActiveSemester(semesters, today))
    }

    @Test
    fun withoutCoveringTodayLatestStartWins() {
        val semesters = listOf(
            SemesterSpan(1, listOf(range("2026-02-01", "2026-06-30"))),
            SemesterSpan(2, listOf(range("2026-09-01", "2026-12-31")))
        )
        val beforeBoth = LocalDate.of(2025, 1, 1)

        assertEquals(2L, fallbackActiveSemester(semesters, beforeBoth))
    }

    @Test
    fun withoutCalendarsFirstSemesterWins() {
        val semesters = listOf(SemesterSpan(7, emptyList()), SemesterSpan(8, emptyList()))

        assertEquals(7L, fallbackActiveSemester(semesters, today))
    }

    @Test
    fun semesterWithCalendarsBeatsOneWithoutCalendars() {
        val semesters = listOf(
            SemesterSpan(7, emptyList()),
            SemesterSpan(8, listOf(range("2026-01-01", "2026-03-31")))
        )

        assertEquals(8L, fallbackActiveSemester(semesters, today))
    }

    @Test
    fun emptyListGivesNull() {
        assertNull(fallbackActiveSemester(emptyList(), today))
    }
}
