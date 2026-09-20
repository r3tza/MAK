package dev.retza.mak.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class WidgetStatusMode {
    Hidden,
    Primary,
    All
}

internal data class WidgetLayoutPolicy(
    val itemLimit: Int,
    val accentHeight: Dp,
    val headerSpacing: Dp,
    val nameMaxLines: Int,
    val nameCharacterLimit: Int,
    val rowSpacing: Dp,
    val statusMode: WidgetStatusMode,
    val includeTeacher: Boolean,
    val metadataCharacterLimit: Int,
    val overflowSpacing: Dp,
    val footerHeight: Dp
)

internal fun widgetLayoutPolicy(mode: WidgetLayoutMode): WidgetLayoutPolicy {
    val wide = mode.width == WidgetWidthMode.Wide
    return when (mode.height) {
        WidgetHeightMode.Compact -> WidgetLayoutPolicy(
            itemLimit = 1,
            accentHeight = 28.dp,
            headerSpacing = 6.dp,
            nameMaxLines = 1,
            nameCharacterLimit = if (wide) 36 else 28,
            rowSpacing = 0.dp,
            statusMode = WidgetStatusMode.Hidden,
            includeTeacher = wide,
            metadataCharacterLimit = if (wide) 36 else 28,
            overflowSpacing = 2.dp,
            footerHeight = 14.dp
        )
        WidgetHeightMode.Medium -> WidgetLayoutPolicy(
            itemLimit = 2,
            accentHeight = 42.dp,
            headerSpacing = 6.dp,
            nameMaxLines = 1,
            nameCharacterLimit = if (wide) 50 else 42,
            rowSpacing = 4.dp,
            statusMode = WidgetStatusMode.Primary,
            includeTeacher = wide,
            metadataCharacterLimit = if (wide) 42 else 34,
            overflowSpacing = 2.dp,
            footerHeight = 14.dp
        )
        WidgetHeightMode.Large -> WidgetLayoutPolicy(
            itemLimit = 3,
            accentHeight = 52.dp,
            headerSpacing = 6.dp,
            nameMaxLines = if (wide) 2 else 1,
            nameCharacterLimit = if (wide) 56 else 42,
            rowSpacing = 6.dp,
            statusMode = WidgetStatusMode.All,
            includeTeacher = wide,
            metadataCharacterLimit = if (wide) 52 else 42,
            overflowSpacing = 2.dp,
            footerHeight = 14.dp
        )
    }
}

internal fun widgetItemLimit(mode: WidgetLayoutMode): Int =
    widgetLayoutPolicy(mode).itemLimit

internal fun widgetNameMaxLines(mode: WidgetLayoutMode): Int =
    widgetLayoutPolicy(mode).nameMaxLines

internal fun widgetNameCharacterLimit(mode: WidgetLayoutMode): Int =
    widgetLayoutPolicy(mode).nameCharacterLimit

internal fun widgetOverflowLabel(total: Int, visible: Int): String? =
    if (visible < total) "Jeszcze ${total - visible}" else null

internal fun widgetCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}

internal fun truncateWidgetText(value: String, maxCharacters: Int): String =
    if (value.length <= maxCharacters) value
    else value.take((maxCharacters - 1).coerceAtLeast(1)) + "…"

internal fun widgetStatusLabel(
    item: WidgetOccurrenceUi,
    statusMode: WidgetStatusMode
): String? {
    val conflict = item.conflictLabel?.takeIf(String::isNotBlank)
    val note = item.hasNote.takeIf { it }?.let { "Notatka" }
    return when (statusMode) {
        WidgetStatusMode.Hidden -> null
        WidgetStatusMode.Primary -> conflict?.let { truncateWidgetText(it, STATUS_CHARACTER_LIMIT) }
            ?: note
        WidgetStatusMode.All -> when {
            conflict != null && note != null -> {
                val suffix = ", $note"
                truncateWidgetText(conflict, STATUS_CHARACTER_LIMIT - suffix.length) + suffix
            }
            conflict != null -> truncateWidgetText(conflict, STATUS_CHARACTER_LIMIT)
            else -> note
        }
    }
}

private const val STATUS_CHARACTER_LIMIT = 30
