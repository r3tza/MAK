package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DistinctLabelsTest {
    @Test
    fun uniqueLabelsStayUnchanged() {
        assertEquals(listOf("Zima", "Lato"), distinctLabels(listOf("Zima", "Lato")))
    }

    @Test
    fun repeatedLabelsGetOrderNumbers() {
        assertEquals(
            listOf("Zima (1)", "Lato", "Zima (2)"),
            distinctLabels(listOf("Zima", "Lato", "Zima"))
        )
    }
}
