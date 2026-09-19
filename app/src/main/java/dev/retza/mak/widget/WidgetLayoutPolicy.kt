package dev.retza.mak.widget

internal fun widgetItemLimit(heightDp: Float, compact: Boolean): Int {
    val estimatedHeaderAndPadding = if (compact) 54f else 58f
    val estimatedRowHeight = if (compact) 38f else 42f
    val limit = ((heightDp - estimatedHeaderAndPadding) / estimatedRowHeight)
        .toInt()
        .coerceAtLeast(1)
    return if (compact) limit.coerceAtMost(2) else limit
}

internal fun widgetOverflowLabel(total: Int, visible: Int): String? =
    if (visible < total) "Jeszcze ${total - visible}" else null

internal fun widgetCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}
