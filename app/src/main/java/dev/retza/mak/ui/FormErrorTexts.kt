package dev.retza.mak.ui

import dev.retza.mak.domain.DateRangeError

internal data class DateRangeErrorTexts(val start: String?, val end: String?, val range: String?)

// One place for form error texts, so the same rule always shows the same message.
internal object FormErrorTexts {
    const val SemesterNameRequired = "Podaj nazwę semestru."
    const val ProgramNameRequired = "Podaj nazwę kierunku."
    const val ProgramRequired = "Wybierz kierunek."
    const val DateRequired = "Podaj poprawną datę."
    const val EndBeforeStart = "Koniec nie może być wcześniejszy od początku."
    const val DateOutsideCalendar = "Data musi należeć do kalendarza kierunku."
    const val EndNotAfterStart = "Koniec musi być późniejszy niż początek tego samego dnia."

    fun dateRange(errors: Set<DateRangeError>): DateRangeErrorTexts = DateRangeErrorTexts(
        start = if (DateRangeError.START_REQUIRED in errors) DateRequired else null,
        end = if (DateRangeError.END_REQUIRED in errors) DateRequired else null,
        range = if (DateRangeError.END_BEFORE_START in errors) EndBeforeStart else null
    )
}
