package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CollisionLabelsTest {
    private val date = LocalDate.of(2026, 9, 21)
    private val semester = Semester(id = "semester", name = "Semestr")
    private val calendar = AcademicCalendar("calendar", date, date.plusDays(7), WeekType.A)
    private val course = StudyProgram("course", "Informatyka", "#137B71")
    private val assignment = SemesterProgram("assignment", semester.id, course.id, calendar.id)

    @Test
    fun oneCollisionGetsTheSameExactRangeForBothClasses() {
        val collisions = collisions(
            classItem("first", "Programowanie", 9, 10),
            classItem("second", "Matematyka", 9, 10, startMinute = 30, endMinute = 30)
        )
        assertEquals("Kolizja 09:30-10:00", collisionLabels(collisions)["first:$date"])
        assertEquals("Kolizja 09:30-10:00", collisionLabels(collisions)["second:$date"])
    }

    @Test
    fun multipleRangesAreSortedAndDeduplicated() {
        val first = classItem("first", "Programowanie", 10, 12)
        val second = classItem("second", "Matematyka", 10, 11, startMinute = 30)
        val third = classItem("third", "Fizyka", 11, 12, startMinute = 30, endMinute = 30)
        val detected = collisions(first, second, third)
        val duplicate = detected + detected.first()
        assertEquals("Kolizje: 10:30-11:00, 11:30-12:00", collisionLabels(duplicate)["first:$date"])
        assertEquals("Matematyka, Fizyka", collisionPartnerNames(duplicate)["first:$date"])
    }

    @Test
    fun partnerNamesAreSymmetricAndDuplicateNamesAppearOnce() {
        val first = classItem("first", "Programowanie", 9, 11)
        val second = classItem("second", "Matematyka", 9, 10, startMinute = 30)
        val third = classItem("third", "Matematyka", 10, 10, startMinute = 15, endMinute = 45)
        val names = collisionPartnerNames(collisions(first, second, third))
        assertEquals("Matematyka", names["first:$date"])
        assertEquals("Programowanie", names["second:$date"])
        assertEquals("Programowanie", names["third:$date"])
    }

    @Test
    fun partnerNamesUseOverlapStartThenAlphabeticalOrder() {
        val first = classItem("first", "Główny", 9, 12)
        val later = classItem("later", "Wcześniej alfabetycznie", 10, 11, endMinute = 30)
        val tieB = classItem("tie-b", "Beta", 9, 10, startMinute = 30)
        val tieA = classItem("tie-a", "Alfa", 9, 10, startMinute = 30)
        val names = collisionPartnerNames(collisions(first, later, tieB, tieA))
        assertEquals("Alfa, Beta, Wcześniej alfabetycznie", names["first:$date"])
    }

    private fun collisions(vararg items: ClassItem): List<Collision> {
        val schedule = ScheduleResolver().resolve(
            date = date,
            semester = semester,
            classes = items.toList(),
            courses = listOf(course),
            semesterPrograms = listOf(assignment),
            calendars = listOf(calendar)
        ).occurrences
        return CollisionDetector().detect(schedule)
    }

    private fun classItem(
        id: String,
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
