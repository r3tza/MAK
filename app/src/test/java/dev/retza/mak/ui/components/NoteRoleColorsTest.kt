package dev.retza.mak.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import dev.retza.mak.ui.theme.DarkColorScheme
import dev.retza.mak.ui.theme.LightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteRoleColorsTest {
    @Test
    fun noteRolesUseSemanticColorsInLightAndDarkSchemes() {
        listOf(LightColorScheme, DarkColorScheme).forEach { scheme ->
            MakNoteRole.entries.forEach { role ->
                assertNotEquals(scheme.primaryContainer, noteRoleColors(role, scheme).container)
            }

            val neutral = noteRoleColors(MakNoteRole.Neutral, scheme)
            assertEquals(scheme.surfaceContainerLow, neutral.container)
            assertEquals(scheme.outlineVariant, neutral.border)
            assertEquals(scheme.onSurface, neutral.title)
            assertEquals(scheme.onSurfaceVariant, neutral.body)
            assertNull(neutral.icon)

            val warning = noteRoleColors(MakNoteRole.Warning, scheme)
            assertEquals(scheme.tertiaryContainer, warning.container)
            assertEquals(scheme.onTertiaryContainer, warning.title)
            assertEquals(scheme.onTertiaryContainer, warning.body)
            assertEquals(Icons.Outlined.WarningAmber, warning.icon)

            val error = noteRoleColors(MakNoteRole.Error, scheme)
            assertEquals(scheme.errorContainer, error.container)
            assertEquals(scheme.onErrorContainer, error.title)
            assertEquals(scheme.onErrorContainer, error.body)
            assertEquals(Icons.Outlined.ErrorOutline, error.icon)
        }
    }
}
