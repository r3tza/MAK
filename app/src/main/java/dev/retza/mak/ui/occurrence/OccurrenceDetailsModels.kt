package dev.retza.mak.ui.occurrence

import androidx.compose.runtime.Immutable

enum class OccurrenceStatusUi {
    Scheduled,
    Cancelled,
    Changed,
    Moved,
    OneOff
}

@Immutable
data class OccurrenceDetailsUiState(
    val title: String = "Szczegóły terminu",
    val subjectName: String = "",
    val courseName: String = "",
    val typeLabel: String = "",
    val dateLabel: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val room: String? = null,
    val building: String? = null,
    val teacherName: String? = null,
    val groupName: String? = null,
    val weekLabel: String? = null,
    val originalDateLabel: String? = null,
    val targetDateLabel: String? = null,
    val status: OccurrenceStatusUi = OccurrenceStatusUi.Scheduled,
    val statusLabel: String? = null,
    val sharedNote: String? = null,
    val occurrenceNote: String? = null,
    val occurrenceNoteDraft: String = occurrenceNote.orEmpty(),
    val targetDateDraft: String = "",
    val startTimeDraft: String = "",
    val endTimeDraft: String = "",
    val roomDraft: String = "",
    val showDeleteConfirmation: Boolean = false,
    val canEditBaseClass: Boolean = true,
    val canDeleteBaseClass: Boolean = true,
    val canCancelOccurrence: Boolean = true,
    val canChangeOccurrence: Boolean = true,
    val canMoveOccurrence: Boolean = true,
    val canRestoreOccurrence: Boolean = false,
    val canEditOccurrenceNote: Boolean = true
)
