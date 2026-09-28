package dev.retza.mak.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Stored course color for a bar, dot or marker drawn on [background], adapted to a 3:1 contrast.
 * Without a valid color it falls back to the neutral variant color.
 */
@Composable
internal fun courseShapeColor(hex: String?, background: Color): Color {
    val parsed = hex?.let(::parseHexColor) ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return courseShapeColor(parsed, background)
}

internal fun courseShapeColor(color: Color, background: Color): Color =
    Color(courseShapeColor(color.toArgb(), background.toArgb()))
