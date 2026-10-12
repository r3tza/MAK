package dev.retza.mak.domain

/** The shared calendar of a semester is its calendar with the lowest id. */
fun <T> sharedCalendarOf(calendars: Collection<T>, id: (T) -> Long): T? = calendars.minByOrNull(id)
