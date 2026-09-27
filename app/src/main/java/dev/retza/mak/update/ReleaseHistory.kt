package dev.retza.mak.update

data class ReleaseHistoryEntry(
    val versionName: String,
    val date: String? = null,
    val changes: List<String> = emptyList()
)

private val knownReleaseHistory = listOf(
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
    val installedEntry = knownReleaseHistory.firstOrNull { it.versionName == installedVersionName }
        ?: ReleaseHistoryEntry(versionName = installedVersionName)
    return (listOf(installedEntry) + knownReleaseHistory.filterNot { it.versionName == installedVersionName })
        .take(MAX_RELEASE_HISTORY_ENTRIES)
}

private const val MAX_RELEASE_HISTORY_ENTRIES = 3
