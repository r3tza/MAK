package dev.retza.mak.sync

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class SyncIssue { AUTHORIZATION_REQUIRED, INVALID_REMOTE_PLAN, NEWER_REMOTE_PLAN, LOCAL_PLAN_TOO_LARGE, FAILED }

/** Both versions changed; the user chooses which one to keep. Bound to the versions it described. */
@Serializable
data class PendingSyncChoice(
    val localFingerprint: String,
    val remoteMd5: String,
    val local: PlanSummary,
    val remote: PlanSummary,
    val phoneChangedAtMillis: Long? = null,
    val driveChangedAtMillis: Long? = null,
    // Empty without a shared plan from the last run: then only a whole version can be kept.
    val differences: List<PlanDifference> = emptyList()
)

@Serializable
data class PlanSummary(val semesterCount: Int, val classCount: Int, val studyProgramCount: Int = 0)

/**
 * Lives in `noBackupFilesDir`: after restoring an Android backup the account must be connected again.
 * [localFingerprint] and [remoteMd5] describe the plan both sides had after the last successful run.
 */
@Serializable
data class SyncState(
    val account: SyncAccount? = null,
    val remoteMd5: String? = null,
    val localFingerprint: String? = null,
    val lastSyncedAtMillis: Long? = null,
    // The last transfer was an own upload. Another phone may overwrite it, so the local plan is
    // archived before the next download. Runs without a transfer keep the value.
    val lastRunUploaded: Boolean = false,
    val issue: SyncIssue? = null,
    val issueMessage: String? = null,
    val pendingChoice: PendingSyncChoice? = null,
    // The last edit on this phone since the last run; a run clears it.
    val localChangedAtMillis: Long? = null
)

interface SyncStateStore {
    fun load(): SyncState
    fun save(state: SyncState)
}

class FileSyncStateStore(private val file: File) : SyncStateStore {
    private val json = Json { ignoreUnknownKeys = true }

    /** A damaged file means an unknown state, which only asks for a new connection. */
    override fun load(): SyncState = runCatching {
        if (file.isFile) json.decodeFromString(SyncState.serializer(), file.readText()) else SyncState()
    }.getOrDefault(SyncState())

    override fun save(state: SyncState) = writeAtomically(file, json.encodeToString(SyncState.serializer(), state).toByteArray())
}

enum class ArchiveSource { PHONE, DRIVE }

data class ArchivedPlan(val id: String, val createdAtMillis: Long, val source: ArchiveSource)

/** The last [limit] plans replaced by synchronization, so a rejected version can still be exported. */
class SyncArchive(private val directory: File, private val limit: Int = 10) {
    fun add(bytes: ByteArray, source: ArchiveSource, nowMillis: Long) {
        directory.mkdirs()
        writeAtomically(File(directory, "$nowMillis-${source.name}.json"), bytes)
        list().drop(limit).forEach { File(directory, "${it.id}.json").delete() }
    }

    fun list(): List<ArchivedPlan> = directory.listFiles().orEmpty()
        .filter { it.extension == "json" }
        .mapNotNull { file ->
            val millis = file.nameWithoutExtension.substringBefore('-').toLongOrNull() ?: return@mapNotNull null
            val source = runCatching { ArchiveSource.valueOf(file.nameWithoutExtension.substringAfter('-')) }
                .getOrNull() ?: return@mapNotNull null
            ArchivedPlan(file.nameWithoutExtension, millis, source)
        }
        .sortedByDescending { it.createdAtMillis }

    fun read(id: String): ByteArray? = File(directory, "$id.json").takeIf { it.isFile && it.parentFile == directory }?.readBytes()
}

/**
 * The plan both sides had after the last successful run, in the Drive file format. It tells a row
 * changed on one side apart from two new rows that got the same number. Without [file] it lives in memory.
 */
class SyncBaseCopy(private val file: File? = null) {
    @Volatile
    private var memory: ByteArray? = null

    fun read(): ByteArray? {
        val target = file ?: return memory
        return runCatching { target.takeIf { it.isFile }?.readBytes() }.getOrNull()
    }

    fun write(bytes: ByteArray) {
        val target = file
        if (target == null) memory = bytes else writeAtomically(target, bytes)
    }

    fun delete() {
        val target = file
        if (target == null) memory = null else target.delete()
    }
}

private fun writeAtomically(target: File, bytes: ByteArray) {
    target.parentFile?.mkdirs()
    val temporary = File(target.parentFile, "${target.name}.tmp")
    temporary.outputStream().use { output ->
        output.write(bytes)
        output.fd.sync()
    }
    if (!temporary.renameTo(target)) {
        target.delete()
        check(temporary.renameTo(target)) { "Cannot write ${target.name}" }
    }
}
