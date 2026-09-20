package dev.retza.mak.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object MakWidgetSizes {
    val small = DpSize(180.dp, 110.dp)
    val medium = DpSize(280.dp, 180.dp)
    val large = DpSize(360.dp, 260.dp)

    val responsiveSizes = setOf(small, medium, large)
}

internal enum class WidgetLayoutMode {
    Compact,
    ExpandedMedium,
    ExpandedLarge
}

internal fun widgetLayoutMode(width: Dp, height: Dp): WidgetLayoutMode =
    when {
        width < 260.dp || height < 160.dp -> WidgetLayoutMode.Compact
        width < 320.dp || height < 220.dp -> WidgetLayoutMode.ExpandedMedium
        else -> WidgetLayoutMode.ExpandedLarge
    }
