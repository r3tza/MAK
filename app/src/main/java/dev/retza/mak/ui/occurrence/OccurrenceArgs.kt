package dev.retza.mak.ui.occurrence

import java.time.LocalDate

data class OccurrenceArgs(
    val classId: Long,
    val date: LocalDate
) {
    fun toRouteId(): String = "$classId:$date"

    companion object {
        fun parse(routeId: String): OccurrenceArgs? {
            val classId = routeId.substringBefore(':').toLongOrNull() ?: return null
            val date = runCatching { LocalDate.parse(routeId.substringAfter(':', "")) }.getOrNull()
                ?: return null
            return OccurrenceArgs(classId, date)
        }
    }
}
