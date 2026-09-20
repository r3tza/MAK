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
    val nameMaxLines: Int,
    val nameCharacterLimit: Int,
    val rowSpacing: Dp,
    val statusMode: WidgetStatusMode,
    val includeTeacher: Boolean,
    val metadataCharacterLimit: Int
)

internal fun widgetLayoutPolicy(mode: WidgetLayoutMode): WidgetLayoutPolicy = when (mode) {
    WidgetLayoutMode.Compact -> WidgetLayoutPolicy(
        itemLimit = 1,
        accentHeight = 30.dp,
        nameMaxLines = 1,
        nameCharacterLimit = 28,
        rowSpacing = 0.dp,
        statusMode = WidgetStatusMode.Hidden,
        includeTeacher = false,
        metadataCharacterLimit = 28
    )
    WidgetLayoutMode.ExpandedMedium -> WidgetLayoutPolicy(
        itemLimit = 2,
        accentHeight = 42.dp,
        nameMaxLines = 1,
        nameCharacterLimit = 42,
        rowSpacing = 4.dp,
        statusMode = WidgetStatusMode.Primary,
        includeTeacher = true,
        metadataCharacterLimit = 34
    )
    WidgetLayoutMode.ExpandedLarge -> WidgetLayoutPolicy(
        itemLimit = 3,
        accentHeight = 52.dp,
        nameMaxLines = 2,
        nameCharacterLimit = 56,
        rowSpacing = 6.dp,
        statusMode = WidgetStatusMode.All,
        includeTeacher = true,
        metadataCharacterLimit = 42
    )
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
        WidgetStatusMode.Primary -> conflict ?: note
        WidgetStatusMode.All -> listOfNotNull(conflict, note)
            .joinToString(", ")
            .takeIf(String::isNotBlank)
    }
}
