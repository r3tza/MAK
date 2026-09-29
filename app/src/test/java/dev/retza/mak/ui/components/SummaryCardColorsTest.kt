package dev.retza.mak.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.retza.mak.ui.theme.MakSummaryEnd
import dev.retza.mak.ui.theme.MakSummaryStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryCardColorsTest {

    @Test
    fun lightThemeKeepsCurrentGradient() {
        assertEquals(listOf(MakSummaryStart, MakSummaryEnd), summaryCardGradient(false))
    }

    @Test
    fun darkGradientKeepsNumbersReadable() {
        val darkGradient = summaryCardGradient(true)
        for (color in darkGradient) {
            val bg = color.toArgb()
            val whiteContrast = contrastRatio(Color.White.toArgb(), bg)
            val alertContrast = contrastRatio(SummaryAlert.toArgb(), bg)
            val okContrast = contrastRatio(SummaryOk.toArgb(), bg)

            assertTrue("White text contrast $whiteContrast against $color should be >= 9.0", whiteContrast >= 9.0)
            assertTrue("SummaryAlert contrast $alertContrast against $color should be >= 4.5", alertContrast >= 4.5)
            assertTrue("SummaryOk contrast $okContrast against $color should be >= 4.5", okContrast >= 4.5)
        }
    }
}
