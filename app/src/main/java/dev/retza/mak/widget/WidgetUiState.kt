package dev.retza.mak.widget

sealed interface WidgetUiState {
    val dateLabel: String

    data class NoActiveSemester(
        override val dateLabel: String
    ) : WidgetUiState

    data class OutsideSemester(
        override val dateLabel: String,
        val semesterName: String
    ) : WidgetUiState

    data class EmptyDay(
        override val dateLabel: String,
        val weekLabel: String
    ) : WidgetUiState

    data class Ready(
        override val dateLabel: String,
        val weekLabel: String,
        val collisionCount: Int,
        val items: List<WidgetOccurrenceUi>
    ) : WidgetUiState

    data class Error(
        override val dateLabel: String,
        val message: String = "Nie udało się wczytać planu"
    ) : WidgetUiState
}

enum class WidgetOccurrencePhase {
    Past,
    Current,
    Next,
    Scheduled
}

data class WidgetOccurrenceUi(
    val id: String,
    val startTime: String,
    val endTime: String,
    val name: String,
    val courseName: String,
    val courseColor: String?,
    val roomLabel: String,
    val teacherName: String?,
    val conflicts: List<WidgetConflictUi>,
    val hasNote: Boolean,
    val phase: WidgetOccurrencePhase = WidgetOccurrencePhase.Scheduled
)

data class WidgetConflictUi(
    val timeRange: String,
    val otherOccurrenceName: String
)
