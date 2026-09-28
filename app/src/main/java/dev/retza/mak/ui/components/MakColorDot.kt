package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Course color shown next to the course name. It is decorative: the name next to it carries
 * the information, so the dot has no semantics of its own.
 */
@Composable
fun MakColorDot(
    color: String,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            // The surface is lighter than the screen in the dark theme, so it is the stricter background.
            .background(courseShapeColor(color, MaterialTheme.colorScheme.surface), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}
