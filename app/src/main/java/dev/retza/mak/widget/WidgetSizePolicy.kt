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
    val extraLargeNarrow = DpSize(180.dp, 340.dp)
    val extraLargeWide = DpSize(240.dp, 340.dp)

    val responsiveSizes = setOf(
        compactNarrow,
        compactWide,
        mediumNarrow,
        mediumWide,
        largeNarrow,
        largeWide,
        extraLargeNarrow,
        extraLargeWide
    )
}

internal enum class WidgetHeightMode {
    Compact,
    Medium,
    Large,
    ExtraLarge
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
            height < 300.dp -> WidgetHeightMode.Large
            else -> WidgetHeightMode.ExtraLarge
        },
        width = if (width < 240.dp) WidgetWidthMode.Narrow else WidgetWidthMode.Wide
    )

internal fun widgetOccurrenceItemId(id: String): Long =
    (id.fold(1125899906842597L) { hash, character ->
        hash * 31 + character.code
    } and 0x3FFF_FFFF_FFFF_FFFFL) + 1
