package dev.retza.mak.ui.occurrence

import androidx.compose.runtime.Immutable
import dev.retza.mak.ui.components.FieldErrorUi

enum class OccurrenceStatusUi {
    Scheduled,
    Cancelled,
    Changed,
    Moved,
    OneOff
}

enum class OccurrenceEditField {
    Date,
    StartTime,
    EndTime
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
    val sharedNoteDraft: String = sharedNote.orEmpty(),
    val sharedNoteError: String? = null,
    val occurrenceNote: String? = null,
    val occurrenceNoteDraft: String = occurrenceNote.orEmpty(),
    val occurrenceNoteError: String? = null,
    val currentDate: String = "",
    val targetDateDraft: String = "",
    val startTimeDraft: String = "",
    val endTimeDraft: String = "",
    val roomDraft: String = "",
    val baseDate: String = "",
    val baseStartTime: String = "",
    val baseEndTime: String = "",
    val baseRoom: String? = null,
    val semesterStartDate: String? = null,
    val semesterEndDate: String? = null,
    val draftErrors: Map<OccurrenceEditField, FieldErrorUi> = emptyMap(),
    val draftError: String? = null,
    val showEditDialog: Boolean = false,
    val isSaving: Boolean = false,
    val canSaveOccurrenceEdit: Boolean = false,
    val isSavingSharedNote: Boolean = false,
    val isSavingOccurrenceNote: Boolean = false,
    val canSaveSharedNote: Boolean = false,
    val canSaveOccurrenceNote: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val canEditBaseClass: Boolean = true,
    val canDeleteBaseClass: Boolean = true,
    val canCancelOccurrence: Boolean = true,
    val canChangeOccurrence: Boolean = true,
    val canMoveOccurrence: Boolean = true,
    val canRestoreOccurrence: Boolean = false
)
