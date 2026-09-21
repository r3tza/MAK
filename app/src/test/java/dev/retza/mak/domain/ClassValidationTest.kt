package dev.retza.mak.domain

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassValidationTest {
    private fun form(
        name: String = "Programming",
        semesterProgramId: String? = "assignment",
        start: LocalTime? = LocalTime.of(10, 0),
        end: LocalTime? = LocalTime.of(11, 0),
        recurrence: Recurrence = Recurrence.EVERY_WEEK
    ) = ClassForm(name, semesterProgramId, start, end, recurrence)

    @Test
    fun nameAndCourseAreRequired() {
        val result = validateClassForm(form(name = " ", semesterProgramId = ""))

        assertTrue(result.has(ClassValidationError.NAME_REQUIRED))
        assertTrue(result.has(ClassValidationError.PROGRAM_REQUIRED))
        assertFalse(result.isValid)
    }

    @Test
    fun startAndEndTimesAreRequired() {
        val result = validateClassForm(form(start = null, end = null))

        assertTrue(result.has(ClassValidationError.START_TIME_REQUIRED))
        assertTrue(result.has(ClassValidationError.END_TIME_REQUIRED))
    }

    @Test
    fun endMustBeLaterThanStart() {
        val result = validateClassForm(form(start = LocalTime.of(10, 0), end = LocalTime.of(10, 0)))

        assertTrue(result.has(ClassValidationError.END_NOT_AFTER_START))
        assertFalse(result.isValid)
    }

    @Test
    fun classesCrossingMidnightAreRejected() {
        val result = validateClassForm(form(start = LocalTime.of(23, 0), end = LocalTime.MIDNIGHT))

        assertTrue(result.has(ClassValidationError.CROSSES_MIDNIGHT))
        assertFalse(result.isValid)
    }

    @Test
    fun oneTimeClassRequiresDate() {
        val result = validateClassForm(form(recurrence = Recurrence.ONCE))

        assertTrue(result.has(ClassValidationError.DATE_REQUIRED))
    }
}
