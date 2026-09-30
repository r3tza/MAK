package dev.retza.mak.sync

import dev.retza.mak.data.repository.BackupData
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncOutcome {
    data object NoAccount : SyncOutcome
    data object UpToDate : SyncOutcome
    data object Uploaded : SyncOutcome
    data object Downloaded : SyncOutcome
    data object ChoiceRequired : SyncOutcome
    data object WaitingForEditor : SyncOutcome
    data object InvalidRemotePlan : SyncOutcome
    data object AuthorizationRequired : SyncOutcome

    /** The plan kept changing on one side during the run; the next run starts over. */
    data object RetryLater : SyncOutcome
}

enum class SyncChoice { KEEP_PHONE, KEEP_DRIVE }

/**
 * Keeps one plan file on Drive in step with the phone (`SYNC_PROPOSAL.md`, „Przebieg synchronizacji”).
 * All runs and account changes are serialized by one lock.
 */
class SyncCoordinator(
    private val gateway: PlanSyncGateway,
    private val transport: PlanFileTransport,
    private val store: SyncStateStore,
    private val archive: SyncArchive,
    private val editTracker: PlanEditTracker,
    private val clock: Clock
) {
    private val lock = Mutex()
    private val current = MutableStateFlow(store.load())
    val state: StateFlow<SyncState> = current.asStateFlow()

    suspend fun synchronize(): SyncOutcome = lock.withLock { run(choice = null) }

    /** Applies [choice] only to the versions the question described; otherwise the question is asked again. */
    suspend fun resolveChoice(choice: SyncChoice): SyncOutcome = lock.withLock { run(choice) }

    suspend fun connect(account: SyncAccount) = lock.withLock { save(SyncState(account = account)) }

    /** Keeps the local plan. Deleting the Drive file first means a failure leaves the account connected. */
    suspend fun disconnect(deleteRemote: Boolean) = lock.withLock {
        val account = current.value.account
        if (deleteRemote && account != null) transport.find(account)?.let { transport.delete(account, it) }
        save(SyncState())
    }

    fun archivedPlans(): List<ArchivedPlan> = archive.list()

    fun archivedPlan(id: String): ByteArray? = archive.read(id)

    private suspend fun run(choice: SyncChoice?): SyncOutcome {
        val account = current.value.account ?: return SyncOutcome.NoAccount
        return try {
            repeat(MAX_PASSES) { pass(account, choice)?.let { return it } }
            SyncOutcome.RetryLater
        } catch (error: InvalidRemotePlanException) {
            save(current.value.copy(issue = SyncIssue.INVALID_REMOTE_PLAN, issueMessage = error.message))
            SyncOutcome.InvalidRemotePlan
        } catch (_: UserActionRequiredException) {
            save(current.value.copy(issue = SyncIssue.AUTHORIZATION_REQUIRED, issueMessage = null))
            SyncOutcome.AuthorizationRequired
        }
    }

    /** One attempt; null means one side changed during the attempt and it has to start over. */
    private suspend fun pass(account: SyncAccount, choice: SyncChoice?): SyncOutcome? {
        val state = current.value
        val local = gateway.snapshot()
        val localFingerprint = SyncPlanFile.fingerprint(local)
        val remote = transport.find(account) ?: return upload(account, local, localFingerprint, expected = null)
        val firstRun = state.localFingerprint == null
        val localChanged = firstRun || localFingerprint != state.localFingerprint
        val remoteChanged = firstRun || remote.md5 != state.remoteMd5
        if (!localChanged && !remoteChanged) return synced(remote, localFingerprint, SyncOutcome.UpToDate)
        if (!remoteChanged) return upload(account, local, localFingerprint, expected = remote)

        val remoteBytes = transport.download(account, remote)
        val remoteData = SyncPlanFile.decode(remoteBytes)
        if (SyncPlanFile.fingerprint(remoteData) == localFingerprint) {
            return synced(remote, localFingerprint, SyncOutcome.UpToDate)
        }
        if (!localChanged || firstRun && SyncPlanFile.isEmpty(local)) {
            return download(local, localFingerprint, remote, remoteData)
        }
        if (firstRun && SyncPlanFile.isEmpty(remoteData)) return upload(account, local, localFingerprint, remote)

        val asked = state.pendingChoice
        if (choice != null && asked?.localFingerprint == localFingerprint && asked.remoteMd5 == remote.md5) {
            return when (choice) {
                SyncChoice.KEEP_PHONE -> {
                    archive.add(remoteBytes, ArchiveSource.DRIVE, clock.millis())
                    upload(account, local, localFingerprint, remote)
                }
                SyncChoice.KEEP_DRIVE -> download(local, localFingerprint, remote, remoteData)
            }
        }
        save(
            state.copy(
                pendingChoice = PendingSyncChoice(localFingerprint, remote.md5, local.summary(), remoteData.summary()),
                issue = null,
                issueMessage = null
            )
        )
        return SyncOutcome.ChoiceRequired
    }

    private suspend fun upload(
        account: SyncAccount,
        local: BackupData,
        localFingerprint: String,
        expected: RemotePlanFile?
    ): SyncOutcome? {
        // Drive has no conditional write, so the file is checked again right before replacing it.
        val latest = transport.find(account)
        if (latest?.md5 != expected?.md5) return null
        val uploaded = transport.upload(account, latest, SyncPlanFile.encode(local))
        return synced(uploaded, localFingerprint, SyncOutcome.Uploaded)
    }

    private suspend fun download(
        local: BackupData,
        localFingerprint: String,
        remote: RemotePlanFile,
        remoteData: BackupData
    ): SyncOutcome? {
        if (editTracker.isEditing) return SyncOutcome.WaitingForEditor
        if (!SyncPlanFile.isEmpty(local)) archive.add(SyncPlanFile.encode(local), ArchiveSource.PHONE, clock.millis())
        if (!gateway.replaceIfUnchanged(localFingerprint, remoteData)) return null
        return synced(remote, SyncPlanFile.fingerprint(remoteData), SyncOutcome.Downloaded)
    }

    private fun synced(remote: RemotePlanFile, localFingerprint: String, outcome: SyncOutcome): SyncOutcome {
        save(
            current.value.copy(
                remoteFileId = remote.id,
                remoteMd5 = remote.md5,
                localFingerprint = localFingerprint,
                lastSyncedAtMillis = clock.millis(),
                issue = null,
                issueMessage = null,
                pendingChoice = null
            )
        )
        return outcome
    }

    private fun save(state: SyncState) {
        store.save(state)
        current.value = state
    }

    private companion object {
        const val MAX_PASSES = 3
    }
}

private fun BackupData.summary() = PlanSummary(semesters.size, semesters.sumOf { it.classes.size })
