package dev.retza.mak.ui

// One place for form error texts, so the same rule always shows the same message.
internal object FormErrorTexts {
    const val SemesterNameRequired = "Podaj nazwę semestru."
    const val ProgramNameRequired = "Podaj nazwę kierunku."
    const val ProgramRequired = "Wybierz kierunek."
    const val DateRequired = "Podaj poprawną datę."
    const val EndBeforeStart = "Koniec nie może być wcześniejszy od początku."
    const val DateOutsideCalendar = "Data musi należeć do kalendarza kierunku."
    const val EndNotAfterStart = "Koniec musi być późniejszy niż początek tego samego dnia."
}
