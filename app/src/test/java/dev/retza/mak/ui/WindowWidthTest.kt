package dev.retza.mak.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowWidthTest {

    @Test
    fun widthClassUsesMaterialBreakpoints() {
        assertEquals(MakWidthClass.Compact, makWidthClassFor(599.dp))
        assertEquals(MakWidthClass.Medium, makWidthClassFor(600.dp))
        assertEquals(MakWidthClass.Medium, makWidthClassFor(839.dp))
        assertEquals(MakWidthClass.Expanded, makWidthClassFor(840.dp))
    }

    @Test
    fun onlySmallDevicesStayInPortrait() {
        assertTrue(shouldLockPortrait(599))
        assertFalse(shouldLockPortrait(600))
    }
}
