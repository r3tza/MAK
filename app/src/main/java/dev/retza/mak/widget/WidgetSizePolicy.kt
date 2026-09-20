package dev.retza.mak.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object MakWidgetSizes {
    val compactNarrow = DpSize(180.dp, 110.dp)
    val compactWide = DpSize(240.dp, 110.dp)
    val mediumNarrow = DpSize(180.dp, 175.dp)
    val mediumWide = DpSize(240.dp, 175.dp)
    val largeNarrow = DpSize(180.dp, 240.dp)
    val largeWide = DpSize(240.dp, 240.dp)

    val responsiveSizes = setOf(
        compactNarrow,
        compactWide,
        mediumNarrow,
        mediumWide,
        largeNarrow,
        largeWide
    )
}

internal enum class WidgetHeightMode {
    Compact,
    Medium,
    Large
}

internal enum class WidgetWidthMode {
    Narrow,
    Wide
}

internal data class WidgetLayoutMode(
    val height: WidgetHeightMode,
    val width: WidgetWidthMode
)

internal fun widgetLayoutMode(width: Dp, height: Dp): WidgetLayoutMode =
    WidgetLayoutMode(
        height = when {
            height < 160.dp -> WidgetHeightMode.Compact
            height < 220.dp -> WidgetHeightMode.Medium
            else -> WidgetHeightMode.Large
        },
        width = if (width < 240.dp) WidgetWidthMode.Narrow else WidgetWidthMode.Wide
    )
