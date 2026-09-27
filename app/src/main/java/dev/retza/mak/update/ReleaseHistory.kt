package dev.retza.mak.update

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

data class ReleaseHistoryEntry(
    val versionName: String,
    val date: String? = null,
    val changes: List<String> = emptyList()
)

/**
 * User-facing release notes from `assets/release_notes.json`, newest release first. The release
 * workflow reads the same file for `update.json` and the GitHub release description.
 */
@Serializable
private data class ReleaseNotesFile(
    val minorChanges: String,
    val releases: List<ReleaseNote>
)

@Serializable
private data class ReleaseNote(
    val version: String,
    val date: String,
    val changes: List<String> = emptyList()
)

private val releaseNotesJson = Json { ignoreUnknownKeys = true }

/** A release without listed changes shows the file's `minorChanges` text instead. */
fun parseReleaseNotes(text: String): List<ReleaseHistoryEntry> {
    val file = releaseNotesJson.decodeFromString<ReleaseNotesFile>(text)
    return file.releases.map { note ->
        ReleaseHistoryEntry(
            versionName = note.version,
            date = note.date,
            changes = note.changes.ifEmpty { listOf(file.minorChanges) }
        )
    }
}

fun interface ReleaseNotesProvider {
    fun load(): List<ReleaseHistoryEntry>
}

@Single(binds = [ReleaseNotesProvider::class])
class AssetReleaseNotesProvider(
    private val context: Context
) : ReleaseNotesProvider {
    // A damaged file only hides the history; it must not break the settings screen.
    override fun load(): List<ReleaseHistoryEntry> = runCatching {
        context.assets.open(RELEASE_NOTES_ASSET).bufferedReader().use { parseReleaseNotes(it.readText()) }
    }.getOrDefault(emptyList())
}

/** The installed release and the ones before it, at most three; unknown versions start at the newest. */
fun releaseHistoryFor(
    installedVersionName: String,
    releases: List<ReleaseHistoryEntry>
): List<ReleaseHistoryEntry> {
    val releaseName = installedVersionName.removeSuffix(DEBUG_VERSION_SUFFIX)
    val installedIndex = releases.indexOfFirst { it.versionName == releaseName }
    return releases
        .drop(installedIndex.coerceAtLeast(0))
        .take(MAX_RELEASE_HISTORY_ENTRIES)
}

const val RELEASE_NOTES_ASSET = "release_notes.json"
private const val DEBUG_VERSION_SUFFIX = "-debug"
private const val MAX_RELEASE_HISTORY_ENTRIES = 3
