package dev.retza.mak.ui.occurrence

import java.time.LocalDate

data class OccurrenceArgs(
    val classId: Long,
    val originalDate: LocalDate
) {
    fun toRouteId(): String = "$classId:$originalDate"

    companion object {
        fun parse(routeId: String): OccurrenceArgs? {
            val classId = routeId.substringBefore(':').toLongOrNull() ?: return null
            val date = runCatching { LocalDate.parse(routeId.substringAfter(':', "")) }.getOrNull()
                ?: return null
            return OccurrenceArgs(classId, date)
        }
    }
}
