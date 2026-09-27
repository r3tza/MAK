package dev.retza.mak.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimePickerLayoutTest {
    @Test
    fun lowWindowUsesHorizontalClock() {
        assertTrue(useHorizontalTimePicker(559.dp))
        assertFalse(useHorizontalTimePicker(560.dp))
        assertFalse(useHorizontalTimePicker(800.dp))
    }
}
