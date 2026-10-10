package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CollisionLabelsTest {
    private val date = LocalDate.of(2026, 9, 21)
    private val semester = Semester(id = 1L, name = "Semestr")
    private val calendar = AcademicCalendar(1L, date, date.plusDays(7), WeekType.A)
    private val course = StudyProgram(1L, "Informatyka", "#137B71")
    private val assignment = SemesterProgram(1L, semester.id, course.id, calendar.id)

    @Test
    fun oneCollisionGetsTheSameExactRangeForBothClasses() {
        val collisions = collisions(
            classItem(1L, "Programowanie", 9, 10),
            classItem(2L, "Matematyka", 9, 10, startMinute = 30, endMinute = 30)
        )
        assertEquals("Kolizja 09:30-10:00", collisionLabels(collisions)["1:$date"])
        assertEquals("Kolizja 09:30-10:00", collisionLabels(collisions)["2:$date"])
    }

    @Test
    fun multipleRangesAreSortedAndDeduplicated() {
        val first = classItem(1L, "Programowanie", 10, 12)
        val second = classItem(2L, "Matematyka", 10, 11, startMinute = 30)
        val third = classItem(3L, "Fizyka", 11, 12, startMinute = 30, endMinute = 30)
        val detected = collisions(first, second, third)
        val duplicate = detected + detected.first()
        assertEquals("Kolizje: 10:30-11:00, 11:30-12:00", collisionLabels(duplicate)["1:$date"])
        assertEquals("Matematyka, Fizyka", collisionPartnerNames(duplicate)["1:$date"])
    }

    @Test
    fun partnerNamesAreSymmetricAndDuplicateNamesAppearOnce() {
        val first = classItem(1L, "Programowanie", 9, 11)
        val second = classItem(2L, "Matematyka", 9, 10, startMinute = 30)
        val third = classItem(3L, "Matematyka", 10, 10, startMinute = 15, endMinute = 45)
        val names = collisionPartnerNames(collisions(first, second, third))
        assertEquals("Matematyka", names["1:$date"])
        assertEquals("Programowanie", names["2:$date"])
        assertEquals("Programowanie", names["3:$date"])
    }

    @Test
    fun partnerNamesUseOverlapStartThenAlphabeticalOrder() {
        val first = classItem(1L, "Główny", 9, 12)
        val later = classItem(4L, "Wcześniej alfabetycznie", 10, 11, endMinute = 30)
        val tieB = classItem(5L, "Beta", 9, 10, startMinute = 30)
        val tieA = classItem(6L, "Alfa", 9, 10, startMinute = 30)
        val names = collisionPartnerNames(collisions(first, later, tieB, tieA))
        assertEquals("Alfa, Beta, Wcześniej alfabetycznie", names["1:$date"])
    }

    @Test
    fun touchingClassesAreDescribedWithoutBreak() {
        val collisions = collisions(
            classItem(1L, "Programowanie", 8, 9, startMinute = 15, endMinute = 45),
            classItem(2L, "Statystyka", 9, 11, startMinute = 45, endMinute = 15)
        )
        assertEquals("Bez przerwy o 09:45", collisionLabels(collisions)["1:$date"])
        assertEquals("Bez przerwy o 09:45", collisionLabels(collisions)["2:$date"])
        assertEquals("Programowanie", collisionPartnerNames(collisions)["2:$date"])
    }

    @Test
    fun shortBreakNamesItsLength() {
        val collisions = collisions(
            classItem(1L, "Programowanie", 8, 9, startMinute = 15, endMinute = 45),
            classItem(2L, "Statystyka", 9, 11, startMinute = 50, endMinute = 15),
            minimumBreakMinutes = 10
        )
        assertEquals("Przerwa 5 min o 09:45", collisionLabels(collisions)["2:$date"])
    }

    @Test
    fun overlapComesBeforeBreaksForOneClass() {
        val collisions = collisions(
            classItem(7L, "Statystyka", 10, 11, endMinute = 15),
            classItem(8L, "Analiza", 9, 10, startMinute = 30, endMinute = 30),
            classItem(9L, "Programowanie", 11, 12, startMinute = 15)
        )
        assertEquals("Kolizja 10:00-10:30, bez przerwy o 11:15", collisionLabels(collisions)["7:$date"])
    }

    private fun collisions(vararg items: ClassItem, minimumBreakMinutes: Int = 0): List<Collision> {
        val schedule = ScheduleResolver().resolve(
            date = date,
            semester = semester,
            classes = items.toList(),
            courses = listOf(course),
            semesterPrograms = listOf(assignment),
            calendars = listOf(calendar)
        ).occurrences
        return CollisionDetector().detect(schedule, minimumBreakMinutes)
    }

    private fun classItem(
        id: Long,
        name: String,
        startHour: Int,
        endHour: Int,
        startMinute: Int = 0,
        endMinute: Int = 0
) = ClassItem(
        id = id,
        semesterId = semester.id,
        name = name,
        type = "Wykład",
        semesterProgramId = assignment.id,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(startHour, startMinute),
        endTime = LocalTime.of(endHour, endMinute)
    )
}
