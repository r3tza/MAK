package dev.retza.mak.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.retza.mak.text.polishPlural

internal enum class WidgetStatusMode {
    ConflictOnly,
    Primary,
    All
}

internal data class WidgetLayoutPolicy(
    val accentHeight: Dp,
    // The teacher fits only next to the room in a wide widget.
    val includeTeacher: Boolean,
    val nameMaxLines: Int,
    val nameCharacterLimit: Int,
    val rowSpacing: Dp,
    val statusMode: WidgetStatusMode,
    val metadataCharacterLimit: Int,
    val noteCharacterLimit: Int
)

internal val WidgetHeaderSpacing = 6.dp

internal fun widgetLayoutPolicy(mode: WidgetLayoutMode): WidgetLayoutPolicy {
    val wide = mode.width == WidgetWidthMode.Wide
    val policy = when (mode.height) {
        WidgetHeightMode.Compact -> WidgetLayoutPolicy(
            accentHeight = 28.dp,
            includeTeacher = wide,
            nameMaxLines = 1,
            nameCharacterLimit = if (wide) 36 else 28,
            rowSpacing = 0.dp,
            statusMode = WidgetStatusMode.ConflictOnly,
            metadataCharacterLimit = if (wide) 36 else 28,
            noteCharacterLimit = if (wide) 44 else 34
        )
        WidgetHeightMode.Medium -> WidgetLayoutPolicy(
            accentHeight = 42.dp,
            includeTeacher = wide,
            nameMaxLines = 1,
            nameCharacterLimit = if (wide) 50 else 42,
            rowSpacing = 4.dp,
            statusMode = WidgetStatusMode.Primary,
            metadataCharacterLimit = if (wide) 42 else 34,
            noteCharacterLimit = if (wide) 52 else 40
        )
        WidgetHeightMode.Large, WidgetHeightMode.ExtraLarge -> WidgetLayoutPolicy(
            accentHeight = 52.dp,
            includeTeacher = wide,
            nameMaxLines = if (wide) 2 else 1,
            nameCharacterLimit = if (wide) 56 else 42,
            rowSpacing = 6.dp,
            statusMode = WidgetStatusMode.All,
            metadataCharacterLimit = if (wide) 52 else 42,
            noteCharacterLimit = if (wide) 64 else 48
        )
    }
    return policy
}

internal fun widgetNameMaxLines(mode: WidgetLayoutMode): Int =
    widgetLayoutPolicy(mode).nameMaxLines

internal fun truncateWidgetText(value: String, maxCharacters: Int): String =
    if (value.length <= maxCharacters) value
    else value.take((maxCharacters - 1).coerceAtLeast(1)) + "…"

internal fun widgetShouldShowConflict(item: WidgetOccurrenceUi): Boolean = item.conflictLabel != null

internal fun widgetShouldShowNotes(
    item: WidgetOccurrenceUi,
    policy: WidgetLayoutPolicy
): Boolean = (item.classNote != null || item.occurrenceNote != null) && when (policy.statusMode) {
    WidgetStatusMode.ConflictOnly -> false
    WidgetStatusMode.Primary -> item.conflictLabel == null
    WidgetStatusMode.All -> true
}

internal fun widgetConflictCountLabel(count: Int): String =
    "$count ${polishPlural(count, "kolizja", "kolizje", "kolizji")}"
