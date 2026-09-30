package dev.retza.mak.sync

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

interface PlanSyncGateway {
    suspend fun snapshot(): BackupData

    /**
     * Replaces the plan only while it still has [expectedFingerprint], in one transaction. The local
     * active semester stays when [keepLocalActive] and its number exists in [data]; otherwise
     * [fallbackActiveId] becomes active. False means the plan changed meanwhile and nothing was written.
     */
    suspend fun replaceIfUnchanged(
        expectedFingerprint: String,
        data: BackupData,
        keepLocalActive: Boolean,
        fallbackActiveId: Long?
    ): Boolean
}

@Single(binds = [PlanSyncGateway::class])
class RoomPlanSyncGateway(
    private val database: AppDatabase,
    private val backup: PlanBackupGateway
) : PlanSyncGateway {
    override suspend fun snapshot(): BackupData = backup.snapshot()

    override suspend fun replaceIfUnchanged(
        expectedFingerprint: String,
        data: BackupData,
        keepLocalActive: Boolean,
        fallbackActiveId: Long?
    ): Boolean = database.withTransaction {
        val current = backup.snapshot()
        if (SyncPlanFile.fingerprint(current) != expectedFingerprint) return@withTransaction false
        val activeId = activeSemesterAfterReplace(current.activeSemesterId, data, keepLocalActive, fallbackActiveId)
        backup.replaceAll(data.withActiveSemester(activeId))
        true
    }
}

/** Local numbers mean the same semester only within one plan lineage, after the first synchronization. */
internal fun activeSemesterAfterReplace(
    localActiveId: Long?,
    data: BackupData,
    keepLocalActive: Boolean,
    fallbackActiveId: Long?
): Long? = localActiveId?.takeIf { id -> keepLocalActive && data.semesters.any { it.semester.id == id } }
    ?: fallbackActiveId

/**
 * Counts open plan editors. A downloaded plan waits until none is open, so a form never saves over
 * a plan it did not show.
 */
@Single
class PlanEditTracker {
    private val openEditors = MutableStateFlow(0)
    val editorCount: StateFlow<Int> = openEditors.asStateFlow()

    val isEditing: Boolean get() = openEditors.value > 0

    fun open() = openEditors.update { it + 1 }

    fun close() = openEditors.update { (it - 1).coerceAtLeast(0) }
}
