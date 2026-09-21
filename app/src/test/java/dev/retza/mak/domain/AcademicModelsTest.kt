package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AcademicModelsTest {
    private val informatics = StudyProgram("program-1", "Informatyka", "#137B71")
    private val autumn = AcademicCalendar(
        id = "calendar-1",
        startDate = LocalDate.of(2026, 10, 1),
        endDate = LocalDate.of(2027, 2, 28),
        firstWeekType = WeekType.A
    )
    private val spring = AcademicCalendar(
        id = "calendar-2",
        startDate = LocalDate.of(2027, 3, 1),
        endDate = LocalDate.of(2027, 6, 30),
        firstWeekType = WeekType.B
    )

    @Test
    fun oneStudyProgramCanBeAssignedToManySemesters() {
        val winter = SemesterProgram("assignment-1", "semester-winter", informatics.id, autumn.id)
        val summer = SemesterProgram("assignment-2", "semester-summer", informatics.id, spring.id)

        assertEquals(informatics.id, winter.studyProgramId)
        assertEquals(informatics.id, summer.studyProgramId)
        assertNotEquals(winter.semesterId, summer.semesterId)
    }

    @Test
    fun severalProgramsCanShareOneAcademicCalendar() {
        val informaticsProgram = SemesterProgram("assignment-1", "semester-winter", informatics.id, autumn.id)
        val mathematics = StudyProgram("program-2", "Matematyka", "#2244AA")
        val mathematicsProgram = SemesterProgram("assignment-2", "semester-winter", mathematics.id, autumn.id)

        assertEquals(informaticsProgram.academicCalendarId, mathematicsProgram.academicCalendarId)
        assertNotEquals(informaticsProgram.studyProgramId, mathematicsProgram.studyProgramId)
    }

    @Test
    fun programFromAnotherUniversityKeepsItsOwnCalendar() {
        val informaticsProgram = SemesterProgram("assignment-1", "semester-winter", informatics.id, autumn.id)
        val exchange = StudyProgram("program-3", "Informatyka za granicą", "#AA2244")
        val exchangeProgram = SemesterProgram("assignment-2", "semester-winter", exchange.id, spring.id)

        assertNotEquals(informaticsProgram.academicCalendarId, exchangeProgram.academicCalendarId)
        assertNotEquals(autumn.startDate, spring.startDate)
        assertNotEquals(autumn.firstWeekType, spring.firstWeekType)
    }

    @Test
    fun academicCalendarRejectsReversedDates() {
        assertThrows(IllegalArgumentException::class.java) {
            AcademicCalendar(
                id = "calendar-bad",
                startDate = LocalDate.of(2027, 2, 1),
                endDate = LocalDate.of(2027, 1, 1),
                firstWeekType = WeekType.A
            )
        }
    }
}
