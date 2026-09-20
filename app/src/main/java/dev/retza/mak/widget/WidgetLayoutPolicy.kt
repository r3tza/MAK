package dev.retza.mak.widget

internal fun widgetItemLimit(heightDp: Float, compact: Boolean): Int {
    if (compact) return 1
    val estimatedHeaderAndPadding = 54f
    val estimatedRowHeight = 48f
    val limit = ((heightDp - estimatedHeaderAndPadding) / estimatedRowHeight)
        .toInt()
        .coerceAtLeast(1)
    return limit.coerceAtMost(3)
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
