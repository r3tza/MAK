package dev.retza.mak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedCalendarTest {

    @Test
    fun sharedCalendarIsTheLowestId() {
        assertEquals(1L, sharedCalendarOf(listOf(3L, 1L, 2L)) { it })
        assertNull(sharedCalendarOf(emptyList<Long>()) { it })
    }
}
