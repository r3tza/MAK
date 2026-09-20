package dev.retza.mak.widget

internal fun widgetItemLimit(mode: WidgetLayoutMode): Int = when (mode) {
    WidgetLayoutMode.Compact -> 1
    WidgetLayoutMode.ExpandedMedium -> 2
    WidgetLayoutMode.ExpandedLarge -> 3
}

internal fun widgetNameMaxLines(mode: WidgetLayoutMode): Int =
    if (mode == WidgetLayoutMode.ExpandedLarge) 2 else 1

internal fun widgetNameCharacterLimit(mode: WidgetLayoutMode): Int = when (mode) {
    WidgetLayoutMode.Compact -> 28
    WidgetLayoutMode.ExpandedMedium -> 42
    WidgetLayoutMode.ExpandedLarge -> 56
}

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
