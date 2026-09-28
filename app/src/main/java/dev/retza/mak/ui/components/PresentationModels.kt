package dev.retza.mak.ui.components

import androidx.compose.runtime.Immutable

@Immutable
data class ClassItemUi(
    val id: String,
    val name: String,
    val type: String,
    val courseName: String,
    val courseColor: String? = null,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val building: String? = null,
    val teacherName: String? = null,
    val weekLabel: String? = null,
    val classNote: String? = null,
    val occurrenceNote: String? = null,
    val statusBadge: String? = null,
    val isCancelled: Boolean = false,
    val isModified: Boolean = false,
    val isOneOff: Boolean = false,
    val conflictLabel: String? = null,
    val conflictWith: String? = null
)

@Immutable
data class SemesterUi(
    val id: String,
    val name: String,
    val dateRangeLabel: String,
    val firstWeekLabel: String,
    val courseCountLabel: String,
    val classCountLabel: String,
    val isActive: Boolean = false
)

@Immutable
data class CalendarDayUi(
    val id: String,
    val dayLabel: String,
    val accessibilityLabel: String,
    val isInCurrentMonth: Boolean = true,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val markers: List<CalendarMarkerUi> = emptyList(),
    val hasMoreMarkers: Boolean = false
)

@Immutable
data class CalendarMarkerUi(
    val id: String,
    val contentDescription: String,
    val isChanged: Boolean = false,
    val colorHex: String? = null
)

/** One entry of the calendar legend: a course colour or the changed-occurrence marker. */
@Immutable
data class CalendarLegendUi(
    val label: String,
    val colorHex: String? = null,
    val isChange: Boolean = false
)

@Immutable
data class FieldErrorUi(
    val message: String
)

sealed interface ScreenStatus {
    data object Ready : ScreenStatus
    data object Loading : ScreenStatus
    data class Error(val message: String) : ScreenStatus
}
