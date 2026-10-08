package dev.retza.mak.ui.occurrence

import androidx.compose.runtime.Immutable
import dev.retza.mak.ui.components.FieldErrorUi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val conflictLabel: String? = null,
    val conflictWith: String? = null,
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
    // The route points to a class that no longer exists in the active semester.
    val notFound: Boolean = false,
    val canCancelOccurrence: Boolean = true,
    val canChangeOccurrence: Boolean = true,
    val canMoveOccurrence: Boolean = true,
    val canRestoreOccurrence: Boolean = false
)

/** Details block a downloaded plan only while the user has typed or opened something to confirm. */
fun OccurrenceDetailsUiState.hasPlanDraft(): Boolean =
    canSaveSharedNote || canSaveOccurrenceNote || isSaving || isSavingSharedNote ||
        isSavingOccurrenceNote || showEditDialog || showDeleteConfirmation

/** The status word shown with its icon on the details screen. */
fun OccurrenceStatusUi.label(): String = when (this) {
    OccurrenceStatusUi.Scheduled -> "Zaplanowane"
    OccurrenceStatusUi.Cancelled -> "Odwołane"
    OccurrenceStatusUi.Changed -> "Zmienione"
    OccurrenceStatusUi.Moved -> "Przeniesione"
    OccurrenceStatusUi.OneOff -> "Jednorazowe"
}

/**
 * What differs from the base occurrence, one line per change; the class card shows only an icon,
 * so this is where the user reads the details.
 */
fun OccurrenceDetailsUiState.changeLines(): List<String> {
    if (status != OccurrenceStatusUi.Changed && status != OccurrenceStatusUi.Moved) return emptyList()
    return buildList {
        val from = originalDateLabel?.let(::changeDate)
        val to = targetDateLabel?.let(::changeDate)
        if (from != null && to != null && from != to) add("Przeniesione z $from na $to")
        if (baseStartTime.isNotBlank() && (startTime != baseStartTime || endTime != baseEndTime)) {
            add("Godziny zmienione z $baseStartTime - $baseEndTime na $startTime - $endTime")
        }
        if (room != baseRoom) {
            add(
                when {
                    baseRoom == null -> "Dodano salę $room"
                    room == null -> "Usunięto salę $baseRoom"
                    else -> "Sala zmieniona z $baseRoom na $room"
                }
            )
        }
    }
}

private val changeDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl"))

private fun changeDate(iso: String): String =
    runCatching { LocalDate.parse(iso).format(changeDateFormatter) }.getOrDefault(iso)
