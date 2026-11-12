package dev.retza.mak.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

/** Course [color] as text on [background], adapted to a 4.5:1 contrast. */
internal fun courseTextColor(color: Color, background: Color): Color =
    Color(courseTextColor(color.toArgb(), background.toArgb()))

/**
 * Look of a running class on cards and in the widget (I-112): the fitted course color fades from
 * [START] at the start, through [MIDDLE] at [MIDDLE_STOP] of the width, to the plain background.
 */
internal object CurrentClassTint {
    const val START = 0.23f
    const val MIDDLE = 0.09f
    const val MIDDLE_STOP = 0.45f

    /** Background under the start of the row, where the times and "Teraz" sit. */
    fun startBackground(fittedColor: Color, background: Color): Color = lerp(background, fittedColor, START)
}
