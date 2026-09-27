package dev.retza.mak.update

data class ReleaseHistoryEntry(
    val versionName: String,
    val date: String? = null,
    val changes: List<String> = emptyList()
)

// Newest release first.
private val knownReleaseHistory = listOf(
    ReleaseHistoryEntry(
        versionName = "0.2.0",
        date = "2026-09-27",
        changes = listOf(
            "Dodano aktualizacje z poziomu aplikacji: ręczne lub automatyczne sprawdzanie, pobieranie i instalację.",
            "Dodano widget z planem na dziś oraz powiadomienia o kolizjach dzień wcześniej i przed zajęciami.",
            "Dodano osobne kalendarze tygodni dla kierunków, liczenie okienek i import kopii zapasowej z podglądem.",
            "Dodano edycję nazw i kolorów kierunków.",
            "Notatka do terminu zostaje przy terminie po jego przeniesieniu.",
            "Nowe logo, ekran startowy i uporządkowane ustawienia; lepsza czytelność przy dużej czcionce i obsługa klawiatury."
        )
    ),
    ReleaseHistoryEntry(
        versionName = "0.1.0",
        date = "2026-09-19",
        changes = listOf(
            "Dodano semestry, kierunki oraz widoki Dzisiaj, Plan i kalendarz.",
            "Dodano tygodnie A/B, korekty oraz zmiany pojedynczych terminów.",
            "Dodano notatki do zajęć i terminów oraz eksport planu do JSON."
        )
    )
)

fun releaseHistoryFor(installedVersionName: String): List<ReleaseHistoryEntry> {
    val releaseName = installedVersionName.removeSuffix(DEBUG_VERSION_SUFFIX)
    val installedIndex = knownReleaseHistory.indexOfFirst { it.versionName == releaseName }
    return knownReleaseHistory
        .drop(installedIndex.coerceAtLeast(0))
        .take(MAX_RELEASE_HISTORY_ENTRIES)
}

private const val DEBUG_VERSION_SUFFIX = "-debug"
private const val MAX_RELEASE_HISTORY_ENTRIES = 3
