package dev.retza.mak.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import dev.retza.mak.ui.theme.DarkColorScheme
import dev.retza.mak.ui.theme.LightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteRoleColorsTest {
    @Test
    fun rolesShareTheNeutralSurfaceAndDifferOnlyByIcon() {
        listOf(LightColorScheme, DarkColorScheme).forEach { scheme ->
            MakNoteRole.entries.forEach { role ->
                val colors = noteRoleColors(role, scheme)
                assertEquals(scheme.surfaceContainerLow, colors.container)
                assertEquals(scheme.outlineVariant, colors.border)
                assertEquals(scheme.onSurface, colors.title)
                assertEquals(scheme.onSurfaceVariant, colors.body)
            }

            val neutral = noteRoleColors(MakNoteRole.Neutral, scheme)
            assertNull(neutral.icon)
            assertNull(neutral.iconTint)

            val warning = noteRoleColors(MakNoteRole.Warning, scheme)
            assertEquals(Icons.Outlined.WarningAmber, warning.icon)
            assertEquals(scheme.tertiary, warning.iconTint)

            val error = noteRoleColors(MakNoteRole.Error, scheme)
            assertEquals(Icons.Outlined.ErrorOutline, error.icon)
            assertEquals(scheme.error, error.iconTint)
        }
    }
}
