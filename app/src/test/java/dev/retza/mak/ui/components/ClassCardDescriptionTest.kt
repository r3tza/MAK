package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClassCardDescriptionTest {
    private val base = ClassItemUi(
        id = "1:2026-10-05",
        name = "Analiza",
        type = "Wykład",
        courseName = "Informatyka",
        startTime = "08:00",
        endTime = "09:30",
        room = "A1"
    )

    @Test
    fun cancelledClassIsReadAsCancelled() {
        val item = base.copy(statusBadge = "Odwołane", isCancelled = true)

        assertEquals(
            "08:00-09:30, Analiza, Odwołane, Informatyka, Wykład, A1",
            classCardDescription(item)
        )
    }

    @Test
    fun weekOfAlternatingClassIsRead() {
        assertEquals(
            "08:00-09:30, Analiza, Informatyka, Wykład, Tydzień A, A1",
            classCardDescription(base.copy(weekLabel = "Tydzień A"))
        )
    }

    @Test
    fun oneOffLabelIsReadOnce() {
        val item = base.copy(weekLabel = "Jednorazowe", statusBadge = "Jednorazowe", isOneOff = true)

        assertEquals("08:00-09:30, Analiza, Jednorazowe, Informatyka, Wykład, A1", classCardDescription(item))
    }

    @Test
    fun emptyTypeAndCourseDoNotLeaveEmptyParts() {
        val description = classCardDescription(base.copy(type = "", courseName = ""))

        assertEquals("08:00-09:30, Analiza, A1", description)
        assertFalse(description.contains(", ,"))
    }

    @Test
    fun partnerNameIsNotReadWithoutConflictLabel() {
        val description = classCardDescription(base.copy(conflictWith = "Matematyka"))

        assertFalse(description.contains("Matematyka"))
        assertFalse(description.contains("z:"))
    }
}
