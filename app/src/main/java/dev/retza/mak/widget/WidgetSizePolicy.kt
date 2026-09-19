package dev.retza.mak.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object MakWidgetSizes {
    val small = DpSize(180.dp, 110.dp)
    val large = DpSize(280.dp, 180.dp)
    val compactWidth = 240.dp
    val compactHeight = 160.dp

    fun isCompact(width: Dp, height: Dp): Boolean =
        width < compactWidth || height < compactHeight
}
