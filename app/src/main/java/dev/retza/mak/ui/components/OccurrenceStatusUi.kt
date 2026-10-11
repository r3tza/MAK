package dev.retza.mak.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.retza.mak.domain.OccurrenceStatus

enum class OccurrenceStatusUi {
    Scheduled,
    Cancelled,
    Changed,
    Moved,
    OneOff
}

fun OccurrenceStatus.toUi(): OccurrenceStatusUi = when (this) {
    OccurrenceStatus.SCHEDULED -> OccurrenceStatusUi.Scheduled
    OccurrenceStatus.CANCELLED -> OccurrenceStatusUi.Cancelled
    OccurrenceStatus.MOVED -> OccurrenceStatusUi.Moved
    OccurrenceStatus.MODIFIED -> OccurrenceStatusUi.Changed
    OccurrenceStatus.ONE_OFF -> OccurrenceStatusUi.OneOff
}

/** The status word shown with its icon on the details screen. */
fun OccurrenceStatusUi.label(): String = when (this) {
    OccurrenceStatusUi.Scheduled -> "Zaplanowane"
    OccurrenceStatusUi.Cancelled -> "Odwołane"
    OccurrenceStatusUi.Changed -> "Zmienione"
    OccurrenceStatusUi.Moved -> "Przeniesione"
    OccurrenceStatusUi.OneOff -> "Jednorazowe"
}

/** The word on a class card and in its description; null for an ordinary term. */
fun OccurrenceStatusUi.cardBadge(): String? = when (this) {
    OccurrenceStatusUi.Cancelled -> "Odwołane"
    OccurrenceStatusUi.OneOff -> "Jednorazowe"
    OccurrenceStatusUi.Changed, OccurrenceStatusUi.Moved -> "Zmienione"
    OccurrenceStatusUi.Scheduled -> null
}

/** Icon and color for a term whose status changes the plan or marks a one-off date; null otherwise. */
@Composable
internal fun occurrenceStatusIcon(status: OccurrenceStatusUi): Pair<ImageVector, Color>? = when (status) {
    OccurrenceStatusUi.Cancelled -> Icons.Outlined.EventBusy to MaterialTheme.colorScheme.error
    OccurrenceStatusUi.Changed, OccurrenceStatusUi.Moved -> Icons.Outlined.EditCalendar to modifiedTint()
    OccurrenceStatusUi.OneOff -> Icons.Outlined.LooksOne to oneOffTint()
    OccurrenceStatusUi.Scheduled -> null
}
