package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime

/** ISO text from a form or a route; surrounding spaces are ignored, any other text gives null. */
fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(trim()) }.getOrNull()

/** ISO text from a form or a route; surrounding spaces are ignored, any other text gives null. */
fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(trim()) }.getOrNull()
