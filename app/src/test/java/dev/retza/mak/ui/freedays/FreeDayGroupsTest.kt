package dev.retza.mak.ui.freedays

import dev.retza.mak.data.repository.FreeDayRecord
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FreeDayGroupsTest {
    private fun record(
        id: Long,
        calendarId: Long,
        start: LocalDate,
        end: LocalDate,
        name: String? = null
    ) = FreeDayRecord(
        id = id,
        semesterId = 1L,
        academicCalendarId = calendarId,
        startDate = start,
        endDate = end,
        name = name
    )

    private fun group(start: LocalDate, end: LocalDate) = FreeDayGroup(
        ids = listOf(1L),
        calendarIds = listOf(1L),
        startDate = start,
        endDate = end,
        name = null
    )

    @Test
    fun sameNameAndDatesInTwoCalendarsFormOneGroup() {
        val start = LocalDate.of(2026, 12, 23)
        val end = LocalDate.of(2027, 1, 1)
        val groups = groupFreeDays(
            listOf(
                record(id = 2L, calendarId = 2L, start = start, end = end, name = "Przerwa"),
                record(id = 1L, calendarId = 1L, start = start, end = end, name = "Przerwa")
            )
        )

        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().ids)
        assertEquals(listOf(1L, 2L), groups.single().calendarIds)
        assertEquals(10, groups.single().dayCount)
    }

    @Test
    fun differentNamesStaySeparate() {
        val day = LocalDate.of(2026, 11, 12)
        val groups = groupFreeDays(
            listOf(
                record(id = 1L, calendarId = 1L, start = day, end = day, name = "Święto"),
                record(id = 2L, calendarId = 1L, start = day, end = day, name = "Wyjazd")
            )
        )

        assertEquals(2, groups.size)
    }

    @Test
    fun summaryIsBrakWithoutGroups() {
        assertEquals("Brak", freeDaysSummary(emptyList()))
    }

    @Test
    fun summaryCountsOneRangeOfOneDay() {
        val day = LocalDate.of(2026, 11, 12)
        assertEquals("1 zakres, 1 dzień", freeDaysSummary(listOf(group(day, day))))
    }

    @Test
    fun summaryCountsDistinctDaysOfOverlappingRanges() {
        val groups = listOf(
            group(LocalDate.of(2026, 12, 23), LocalDate.of(2027, 1, 1)),
            group(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1))
        )

        assertEquals("2 zakresy, 10 dni", freeDaysSummary(groups))
    }

    @Test
    fun summaryUsesGenitivePluralFromFiveRanges() {
        val groups = (0L..4L).map { offset ->
            val day = LocalDate.of(2026, 10, 1).plusDays(offset * 7)
            group(day, day)
        }

        assertEquals("5 zakresów, 5 dni", freeDaysSummary(groups))
    }
}
