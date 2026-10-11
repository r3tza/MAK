package dev.retza.mak.ui.occurrence

import dev.retza.mak.domain.toLocalDateOrNull
import java.time.LocalDate

data class OccurrenceArgs(
    val classId: Long,
    val originalDate: LocalDate
) {
    companion object {
        fun from(classId: String?, date: String?): OccurrenceArgs? {
            val id = classId?.toLongOrNull() ?: return null
            val day = date?.toLocalDateOrNull() ?: return null
            return OccurrenceArgs(id, day)
        }
    }
}
