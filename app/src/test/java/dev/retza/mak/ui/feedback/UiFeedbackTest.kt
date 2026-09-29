package dev.retza.mak.ui.feedback

import androidx.compose.material3.SnackbarDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class UiFeedbackTest {
    @Test
    fun successAndInfoUseShortDuration() {
        assertEquals(
            SnackbarDuration.Short,
            UiFeedback("Zapisano", UiFeedbackKind.Success).toVisuals().duration
        )
        assertEquals(
            SnackbarDuration.Short,
            UiFeedback("Gotowe", UiFeedbackKind.Info).toVisuals().duration
        )
    }

    @Test
    fun errorAndWarningUseLongDuration() {
        assertEquals(
            SnackbarDuration.Long,
            UiFeedback("Błąd", UiFeedbackKind.Error).toVisuals().duration
        )
        assertEquals(
            SnackbarDuration.Long,
            UiFeedback("Uwaga", UiFeedbackKind.Warning).toVisuals().duration
        )
    }
}
