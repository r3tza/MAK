package dev.retza.mak.sync

import dev.retza.mak.data.repository.BackupData
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SyncOutcome {
    data object NoAccount : SyncOutcome
    data object UpToDate : SyncOutcome
    data object Uploaded : SyncOutcome
    data object Downloaded : SyncOutcome
    data object ChoiceRequired : SyncOutcome
    data object WaitingForEditor : SyncOutcome

    /** A lasting problem recorded in [SyncState.issue]; repeating the run will not help. */
    data object NeedsAttention : SyncOutcome

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
    private val clock: Clock,
    // Encoding the plan and writing files with fsync must not block the main thread of a screen.
    private val ioDispatcher: CoroutineDispatcher,
    private val baseCopy: SyncBaseCopy = SyncBaseCopy()
) {
    private val lock = Mutex()

    // Room reports the plan replaced by a download as a change; edits this soon after it are ignored.
    @Volatile
    private var lastReplacementMillis = Long.MIN_VALUE / 2

    // Read on first use, so creating the coordinator during app start does no file access.
    private val current by lazy { MutableStateFlow(store.load()) }
    val state: StateFlow<SyncState> get() = current

    suspend fun synchronize(): SyncOutcome = serialized { run(choice = null) }

    /** Applies [choice] only to the versions the question described; otherwise the question is asked again. */
    suspend fun resolveChoice(choice: SyncChoice): SyncOutcome = serialized { run(choice) }

    suspend fun connect(account: SyncAccount) = serialized {
        baseCopy.delete()
        save(SyncState(account = account))
    }

    /** Keeps the local plan. Deleting the Drive file first means a failure leaves the account connected. */
    suspend fun disconnect(deleteRemote: Boolean) = serialized {
        val account = current.value.account
        if (deleteRemote && account != null) transport.deleteAll(account)
        baseCopy.delete()
        save(SyncState())
    }

    /** Remembers when the plan on this phone last changed, for the version choice. */
    suspend fun recordLocalChange() = serialized {
        val now = clock.millis()
        if (current.value.account == null || now - lastReplacementMillis < REPLACEMENT_ECHO_MILLIS) return@serialized
        save(current.value.copy(localChangedAtMillis = now))
    }

    private suspend fun <T> serialized(block: suspend () -> T): T =
        withContext(ioDispatcher) { lock.withLock { block() } }

    fun archivedPlans(): List<ArchivedPlan> = archive.list()

    fun archivedPlan(id: String): ByteArray? = archive.read(id)

    private suspend fun run(choice: SyncChoice?): SyncOutcome {
        val account = current.value.account ?: return SyncOutcome.NoAccount
        return try {
            repeat(MAX_PASSES) { pass(account, choice)?.let { return it } }
            SyncOutcome.RetryLater
        } catch (error: InvalidRemotePlanException) {
            issue(SyncIssue.INVALID_REMOTE_PLAN, error.message)
        } catch (_: UserActionRequiredException) {
            authorizationRequired()
        } catch (_: AccountMismatchException) {
            failed(ACCOUNT_MISMATCH)
        } catch (error: DriveHttpException) {
            when {
                error.isTransient() -> throw error
                error.statusCode == 401 -> authorizationRequired()
                error.statusCode == 403 -> failed("Dysk Google odmówił dostępu. Sprawdź, czy na koncie jest wolne miejsce.")
                else -> failed(GENERIC_FAILURE)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw error
        } catch (_: Exception) {
            failed(GENERIC_FAILURE)
        }
    }

    private fun authorizationRequired() = issue(SyncIssue.AUTHORIZATION_REQUIRED, message = null)

    private fun failed(message: String) = issue(SyncIssue.FAILED, message)

    private fun issue(issue: SyncIssue, message: String?): SyncOutcome {
        save(current.value.copy(issue = issue, issueMessage = message))
        return SyncOutcome.NeedsAttention
    }

    /** One attempt; null means one side changed during the attempt and it has to start over. */
    private suspend fun pass(account: SyncAccount, choice: SyncChoice?): SyncOutcome? {
        val state = current.value
        val local = gateway.snapshot()
        val localBytes = SyncPlanFile.encode(local)
        val localFingerprint = SyncPlanFile.fingerprint(localBytes)
        val remote = transport.find(account)
            ?: return upload(account, localFingerprint, localBytes, expected = null)
        val firstRun = state.localFingerprint == null
        val localChanged = firstRun || localFingerprint != state.localFingerprint
        val remoteChanged = firstRun || remote.md5 != state.remoteMd5
        if (!localChanged && !remoteChanged) {
            // Both sides still hold the shared plan; it is written only when an older version left none.
            return synced(remote, localFingerprint, SyncOutcome.UpToDate, localBytes.takeIf { baseCopy.read() == null })
        }
        if (!remoteChanged) return upload(account, localFingerprint, localBytes, expected = remote)

        val remoteBytes = transport.download(account, remote)
        val remoteData = SyncPlanFile.decode(remoteBytes)
        if (SyncPlanFile.fingerprint(remoteData) == localFingerprint) {
            return synced(remote, localFingerprint, SyncOutcome.UpToDate, localBytes)
        }
        if (!localChanged || firstRun && SyncPlanFile.isEmpty(local)) {
            return download(local, localFingerprint, localBytes, remote, remoteBytes, remoteData, firstRun, rejectsLocal = false)
        }
        if (firstRun && SyncPlanFile.isEmpty(remoteData)) {
            return upload(account, localFingerprint, localBytes, remote)
        }

        val asked = state.pendingChoice
        if (choice != null && asked?.localFingerprint == localFingerprint && asked.remoteMd5 == remote.md5) {
            return when (choice) {
                SyncChoice.KEEP_PHONE -> {
                    localSizeIssue(localBytes)?.let { return it }
                    archive.add(remoteBytes, ArchiveSource.DRIVE, clock.millis())
                    upload(account, localFingerprint, localBytes, remote)
                }
                SyncChoice.KEEP_DRIVE -> download(
                    local, localFingerprint, localBytes, remote, remoteBytes, remoteData, firstRun, rejectsLocal = true
                )
            }
        }
        save(
            state.copy(
                pendingChoice = PendingSyncChoice(
                    localFingerprint = localFingerprint,
                    remoteMd5 = remote.md5,
                    local = local.summary(),
                    remote = remoteData.summary(),
                    phoneChangedAtMillis = state.localChangedAtMillis,
                    driveChangedAtMillis = remote.modifiedAtMillis,
                    differences = sharedPlan()?.let { planDifferences(local, remoteData, it) }.orEmpty()
                ),
                issue = null,
                issueMessage = null
            )
        )
        return SyncOutcome.ChoiceRequired
    }

    /** A damaged or missing copy means the differences cannot be told apart reliably. */
    private fun sharedPlan(): BackupData? = baseCopy.read()?.let { runCatching { SyncPlanFile.decode(it) }.getOrNull() }

    private suspend fun upload(
        account: SyncAccount,
        localFingerprint: String,
        localBytes: ByteArray,
        expected: RemotePlanFile?
    ): SyncOutcome? {
        localSizeIssue(localBytes)?.let { return it }
        // Drive has no conditional write, so the file is checked again right before replacing it.
        val latest = transport.find(account)
        if (latest?.md5 != expected?.md5) return null
        val uploaded = transport.upload(account, latest, localBytes)
        return synced(uploaded, localFingerprint, SyncOutcome.Uploaded, localBytes)
    }

    private suspend fun download(
        local: BackupData,
        localFingerprint: String,
        localBytes: ByteArray,
        remote: RemotePlanFile,
        remoteBytes: ByteArray,
        remoteData: BackupData,
        firstRun: Boolean,
        rejectsLocal: Boolean
    ): SyncOutcome? {
        // A plan this phone only followed was built on by the other phone. Only a rejected plan or one
        // that another upload may have overwritten could be lost, so only those go to the archive.
        val mayBeLost = rejectsLocal || current.value.lastRunUploaded
        val fallbackActive = remoteData.semesterCovering(clock.instant().atZone(clock.zone).toLocalDate())
        val replacement = editTracker.withReplacement {
            if (mayBeLost && !SyncPlanFile.isEmpty(local)) {
                archive.add(localBytes, ArchiveSource.PHONE, clock.millis())
            }
            gateway.replaceIfUnchanged(localFingerprint, remoteData, keepLocalActive = !firstRun, fallbackActive)
                .also { lastReplacementMillis = clock.millis() }
        }
        val replaced = when (replacement) {
            PlanReplacementResult.Editing -> return SyncOutcome.WaitingForEditor
            is PlanReplacementResult.Applied -> replacement.value
        }
        if (!replaced) return null
        return synced(remote, SyncPlanFile.fingerprint(remoteData), SyncOutcome.Downloaded, remoteBytes)
    }

    /** [sharedBytes] is the plan both sides now hold, kept as the base of the next comparison. */
    private fun synced(
        remote: RemotePlanFile,
        localFingerprint: String,
        outcome: SyncOutcome,
        sharedBytes: ByteArray?
    ): SyncOutcome {
        sharedBytes?.let(baseCopy::write)
        save(
            current.value.copy(
                remoteMd5 = remote.md5,
                localFingerprint = localFingerprint,
                lastSyncedAtMillis = clock.millis(),
                lastRunUploaded = when (outcome) {
                    SyncOutcome.Uploaded -> true
                    SyncOutcome.Downloaded -> false
                    else -> current.value.lastRunUploaded
                },
                issue = null,
                issueMessage = null,
                pendingChoice = null,
                localChangedAtMillis = null
            )
        )
        return outcome
    }

    private fun save(state: SyncState) {
        store.save(state)
        current.value = state
    }

    private fun localSizeIssue(bytes: ByteArray): SyncOutcome? =
        if (bytes.size > SyncPlanFile.MAX_BYTES) {
            issue(SyncIssue.LOCAL_PLAN_TOO_LARGE, LOCAL_PLAN_TOO_LARGE_MESSAGE)
        } else {
            null
        }

    private companion object {
        const val MAX_PASSES = 3
        const val REPLACEMENT_ECHO_MILLIS = 2_000L
        const val GENERIC_FAILURE = "Nie udało się zsynchronizować planu."
        const val LOCAL_PLAN_TOO_LARGE_MESSAGE = "Lokalny plan przekracza limit 8 MiB i nie może zostać wysłany."
        const val ACCOUNT_MISMATCH =
            "Telefon ma inne konto Google niż połączone. Wyłącz synchronizację i połącz konto ponownie."
    }
}

private fun BackupData.summary() = PlanSummary(semesters.size, semesters.sumOf { it.classes.size }, studyPrograms.size)
