package dev.retza.mak.ui.settings

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.sync.AccountMismatchException
import dev.retza.mak.sync.ArchiveSource
import dev.retza.mak.sync.AuthorizationAttempt
import dev.retza.mak.sync.PendingSyncChoice
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.sync.SyncAuthorization
import dev.retza.mak.sync.PlanSummary
import dev.retza.mak.sync.SyncAccount
import dev.retza.mak.sync.SyncChoice
import dev.retza.mak.sync.SyncCoordinator
import dev.retza.mak.sync.SyncIssue
import dev.retza.mak.sync.SyncOutcome
import dev.retza.mak.sync.SyncState
import dev.retza.mak.sync.SyncWorkScheduler
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import org.koin.core.annotation.KoinViewModel

data class SyncArchiveItemUi(val id: String, val label: String, val fileName: String)

data class SyncUiState(
    val accountEmail: String? = null,
    val lastSyncLabel: String? = null,
    val issue: String? = null,
    val needsReconnect: Boolean = false,
    // The Drive plan comes from a newer MAK; only an update of the app helps.
    val needsUpdate: Boolean = false,
    // Banner on Today; set only while an account is connected and something needs the user.
    val attention: SyncAttentionUi? = null,
    val choice: SyncChoiceUi? = null,
    val archive: List<SyncArchiveItemUi> = emptyList(),
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val showChoiceDialog: Boolean = false,
    val showDisconnectDialog: Boolean = false
)

/**
 * The open version choice. [phone] and [drive] are plan counts, shown when there are no
 * [differences] to list (no shared plan from the last run).
 */
data class SyncChoiceUi(
    val phone: List<String>,
    val drive: List<String>,
    val phoneChanged: String = UNKNOWN_CHANGE,
    val driveChanged: String = UNKNOWN_CHANGE,
    val differences: List<SyncDifferenceUi> = emptyList()
)

/** The screen „Wybierz zmiany”: one pick per difference, all required before saving. */
data class SyncChangesUi(
    val differences: List<SyncDifferenceUi> = emptyList(),
    val picks: Map<Int, PlanSide> = emptyMap(),
    val problem: String? = null,
    val flagged: Set<Int> = emptySet(),
    val isSaving: Boolean = false
) {
    val chosenCount: Int get() = picks.size
    val canSave: Boolean get() = differences.isNotEmpty() && picks.size == differences.size && !isSaving
}

/** [opensUpdate]: the action leads to the update screen instead of the sync screen. */
data class SyncAttentionUi(val text: String, val action: String, val opensUpdate: Boolean = false)

private data class SyncLocalState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val showChoiceDialog: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val differences: DescribedDifferences? = null,
    val picks: Map<Int, PlanSide> = emptyMap(),
    val problem: String? = null,
    val flagged: Set<Int> = emptySet()
)

/** Descriptions of the differences of one question, kept until the question changes. */
private class DescribedDifferences(
    val question: PendingSyncChoice,
    val items: List<SyncDifferenceUi>,
    val labels: SyncDifferenceLabels?
)

internal const val UNKNOWN_CHANGE = "Data zmiany nieznana"

@KoinViewModel
class SyncViewModel(
    private val coordinator: SyncCoordinator,
    private val authorization: SyncAuthorization,
    private val scheduler: SyncWorkScheduler,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val local = MutableStateFlow(SyncLocalState())

    val sync: StateFlow<SyncUiState> = combine(coordinator.state, local) { state, localState ->
        state.toUi(localState, coordinator.archivedPlans().map { it.toUi() })
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    val changes: StateFlow<SyncChangesUi> = local.map { localState ->
        SyncChangesUi(
            differences = localState.differences?.items.orEmpty(),
            picks = localState.picks,
            problem = localState.problem,
            flagged = localState.flagged,
            isSaving = localState.isWorking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncChangesUi())

    init {
        // Each new question gets its own descriptions; picks made for an earlier one are dropped.
        viewModelScope.launch {
            coordinator.state.map { it.pendingChoice }.distinctUntilChanged().collect { question ->
                val described = question?.takeIf { it.differences.isNotEmpty() }?.let { describe(it) }
                local.update {
                    it.copy(differences = described, picks = emptyMap(), problem = null, flagged = emptySet())
                }
            }
        }
    }

    private suspend fun describe(question: PendingSyncChoice): DescribedDifferences? = try {
        withContext(Dispatchers.IO) {
            val plans = coordinator.pendingPlans() ?: return@withContext null
            val labels = SyncDifferenceLabels(plans.phone, plans.drive)
            DescribedDifferences(question, question.differences.map(labels::describe), labels)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // Without descriptions the dialog falls back to keeping a whole version.
        null
    }

    fun pick(index: Int, side: PlanSide) = local.update {
        it.copy(picks = it.picks + (index to side), flagged = it.flagged - index)
    }

    fun savePicks() {
        val described = local.value.differences ?: return
        val picks = local.value.picks
        if (picks.size != described.items.size) return
        val byDifference = described.question.differences.withIndex().associate { (index, difference) ->
            difference to picks.getValue(index)
        }
        local.update { it.copy(problem = null, flagged = emptySet()) }
        work {
            when (val outcome = coordinator.resolveWithPicks(byDifference)) {
                is SyncOutcome.MergeProblems -> {
                    val indexOf = described.question.differences.withIndex().associate { it.value to it.index }
                    local.update { state ->
                        state.copy(
                            problem = outcome.problems.map { described.labels?.describe(it) ?: GENERIC_MERGE_PROBLEM }
                                .distinct().joinToString("\n"),
                            flagged = outcome.problems.flatMap {
                                listOfNotNull(it.childDifference?.let(indexOf::get), it.parentDifference?.let(indexOf::get))
                            }.toSet()
                        )
                    }
                }
                is SyncOutcome.MergeInvalid -> local.update {
                    it.copy(problem = "Tych zmian nie da się połączyć: ${outcome.errors.joinToString(" ")}")
                }
                else -> report(outcome)
            }
        }
    }

    // Google consent screens the route must launch; the result comes back through onAuthorizationResult.
    private val consentRequests = Channel<PendingIntent>(Channel.BUFFERED)
    val authorizationRequests = consentRequests.receiveAsFlow()

    // Only the result of the latest consent request is accepted; a late or foreign result is ignored.
    private var pendingAuthorization: PendingAuthorization? = null

    fun connect() = startAuthorization(pinned = null)

    fun reconnect() = startAuthorization(pinned = coordinator.state.value.account)

    fun onAuthorizationResult(succeeded: Boolean, data: Intent?) {
        val pending = pendingAuthorization ?: return
        pendingAuthorization = null
        local.update { it.copy(isWorking = false) }
        // Cancelling consent never disconnects an account that is already connected.
        if (!succeeded || data == null) return
        work {
            val attempt = if (pending.pinned == null) {
                authorization.completeAccountSelection(data)
            } else {
                authorization.completePinnedAuthorization(data, pending.pinned)
            }
            handle(attempt, pending.pinned)
        }
    }

    fun syncNow() = work { report(coordinator.synchronize()) }

    fun isWorkingNow(): Boolean = local.value.isWorking

    fun openChoice() = local.update { it.copy(showChoiceDialog = true) }

    fun dismissChoice() = local.update { it.copy(showChoiceDialog = false) }

    fun choose(choice: SyncChoice) {
        local.update { it.copy(showChoiceDialog = false) }
        work { report(coordinator.resolveChoice(choice)) }
    }

    fun requestDisconnect() = local.update { it.copy(showDisconnectDialog = true) }

    fun dismissDisconnect() = local.update { it.copy(showDisconnectDialog = false) }

    fun disconnect(deleteRemote: Boolean) {
        local.update { it.copy(showDisconnectDialog = false) }
        work {
            scheduler.cancel()
            try {
                coordinator.disconnect(deleteRemote)
            } catch (error: Exception) {
                scheduler.schedulePeriodic()
                throw error
            }
            feedbackSink.publish(UiFeedback("Wyłączono synchronizację", UiFeedbackKind.Success))
        }
    }

    fun exportArchivedPlan(
        id: String,
        openOutputStream: () -> OutputStream?
    ): Job? {
        if (local.value.isWorking) return null
        local.update { it.copy(isWorking = true, errorMessage = null) }
        return viewModelScope.launch {
            var exported = false
            try {
                withContext(Dispatchers.IO) {
                    val bytes = coordinator.archivedPlan(id) ?: throw IOException("Archived plan is unavailable")
                    val output = openOutputStream() ?: throw IOException("Could not open archive output")
                    output.use { it.write(bytes) }
                }
                exported = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (!currentCoroutineContext().isActive) {
                    throw CancellationException("Archive export was cancelled")
                }
                // Read, open, write, and close failures all leave the export unsuccessful.
            } finally {
                local.update { it.copy(isWorking = false) }
            }
            if (!currentCoroutineContext().isActive) return@launch
            val message = if (exported) "Zapisano poprzednią wersję planu" else "Nie udało się zapisać pliku."
            feedbackSink.publish(UiFeedback(message, if (exported) UiFeedbackKind.Success else UiFeedbackKind.Error))
        }
    }

    fun dismissError() = local.update { it.copy(errorMessage = null) }

    private fun startAuthorization(pinned: SyncAccount?) = work {
        val attempt = if (pinned == null) authorization.beginAccountSelection() else authorization.refreshPinnedAccount(pinned)
        handle(attempt, pinned)
    }

    private suspend fun handle(attempt: AuthorizationAttempt, pinned: SyncAccount?) {
        when (attempt) {
            is AuthorizationAttempt.NeedsResolution -> {
                pendingAuthorization = PendingAuthorization(pinned)
                consentRequests.trySend(attempt.pendingIntent)
            }
            is AuthorizationAttempt.Granted -> {
                if (pinned == null) coordinator.connect(attempt.account)
                scheduler.schedulePeriodic()
                report(coordinator.synchronize())
            }
        }
    }

    // Every archived version comes with a change of the coordinator state, which refreshes the list.
    private fun report(outcome: SyncOutcome) {
        val message = when (outcome) {
            SyncOutcome.Downloaded -> "Pobrano plan z Dysku Google"
            SyncOutcome.Uploaded -> "Wysłano plan na Dysk Google"
            SyncOutcome.UpToDate -> "Plan jest aktualny"
            SyncOutcome.ChoiceRequired -> {
                local.update { it.copy(showChoiceDialog = true) }
                null
            }
            SyncOutcome.WaitingForEditor -> "Plan z Dysku zostanie pobrany po zamknięciu formularza."
            SyncOutcome.RetryLater -> "Plan zmieniał się w trakcie synchronizacji. Spróbuj ponownie."
            SyncOutcome.NoAccount, SyncOutcome.NeedsAttention -> null
            // Only picks lead here; savePicks shows them on its screen.
            is SyncOutcome.MergeProblems, is SyncOutcome.MergeInvalid -> null
        }
        message?.let { feedbackSink.publish(UiFeedback(it, UiFeedbackKind.Success)) }
    }

    private fun work(block: suspend () -> Unit) {
        if (local.value.isWorking) return
        local.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                local.update { it.copy(errorMessage = error.toMessage()) }
            } finally {
                local.update { it.copy(isWorking = pendingAuthorization != null) }
            }
        }
    }

    private class PendingAuthorization(val pinned: SyncAccount?)
}

private fun Exception.toMessage(): String = when {
    this is IOException -> "Brak połączenia z internetem. Aplikacja spróbuje ponownie później."
    this is AccountMismatchException ->
        "Google zwrócił inne konto niż połączone. Wyłącz synchronizację i połącz konto ponownie."
    else -> "Nie udało się zsynchronizować planu."
}

private fun SyncState.toUi(local: SyncLocalState, archive: List<SyncArchiveItemUi>) = SyncUiState(
    accountEmail = account?.email,
    attention = if (account == null) null else issueText()?.let { text ->
        if (issue == SyncIssue.NEWER_REMOTE_PLAN) SyncAttentionUi(text, "Zaktualizuj", opensUpdate = true) else SyncAttentionUi(text, "Otwórz")
    }
        ?: pendingChoice?.let { SyncAttentionUi(CHOICE_TITLE, "Wybierz wersję") },
    lastSyncLabel = account?.let {
        lastSyncedAtMillis?.let { millis -> "Ostatnia synchronizacja: ${formatMillis(millis)}" }
            ?: "Jeszcze nie zsynchronizowano"
    },
    issue = issueText(),
    needsReconnect = issue == SyncIssue.AUTHORIZATION_REQUIRED,
    needsUpdate = issue == SyncIssue.NEWER_REMOTE_PLAN,
    choice = pendingChoice?.let { question ->
        SyncChoiceUi(
            phone = question.local.label(),
            drive = question.remote.label(),
            phoneChanged = changeLabel(question.phoneChangedAtMillis),
            driveChanged = changeLabel(question.driveChangedAtMillis),
            differences = local.differences?.takeIf { it.question == question }?.items.orEmpty()
        )
    },
    archive = archive,
    isWorking = local.isWorking,
    errorMessage = local.errorMessage,
    showChoiceDialog = local.showChoiceDialog && pendingChoice != null,
    showDisconnectDialog = local.showDisconnectDialog && account != null
)

private fun SyncState.issueText(): String? = when (issue) {
    SyncIssue.AUTHORIZATION_REQUIRED -> "Google wymaga ponownego potwierdzenia dostępu do Dysku."
    SyncIssue.INVALID_REMOTE_PLAN -> issueMessage ?: "Plik planu na Dysku jest niepoprawny."
    SyncIssue.NEWER_REMOTE_PLAN -> "Plan na Dysku pochodzi z nowszej wersji MAK. Zaktualizuj aplikację."
    SyncIssue.LOCAL_PLAN_TOO_LARGE -> issueMessage ?: "Lokalny plan przekracza limit 8 MiB i nie może zostać wysłany."
    SyncIssue.FAILED -> issueMessage ?: "Nie udało się zsynchronizować planu."
    null -> null
}

internal const val CHOICE_TITLE = "Plan różni się na telefonie i na Dysku"

private const val GENERIC_MERGE_PROBLEM = "Wybrane zmiany wykluczają się. Wybierz tę samą wersję przy powiązanych pozycjach."

private val changeTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val changeDayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl-PL"))

/** „Zmieniony dziś o 12:20”, „Zmieniony wczoraj o 9:05” or „Zmieniony 7 października o 18:05”. */
internal fun changeLabel(millis: Long?, zone: ZoneId = ZoneId.systemDefault(), today: java.time.LocalDate = java.time.LocalDate.now(zone)): String {
    if (millis == null) return UNKNOWN_CHANGE
    val moment = Instant.ofEpochMilli(millis).atZone(zone)
    val day = when (moment.toLocalDate()) {
        today -> "dziś"
        today.minusDays(1) -> "wczoraj"
        else -> changeDayFormatter.format(moment)
    }
    return "Zmieniony $day o ${changeTimeFormatter.format(moment)}"
}

private fun PlanSummary.label() = listOf(
    "Kierunki: $studyProgramCount",
    "Semestry: $semesterCount",
    "Zajęcia: $classCount"
)

private fun dev.retza.mak.sync.ArchivedPlan.toUi() = SyncArchiveItemUi(
    id = id,
    label = "${formatMillis(createdAtMillis)}, " + when (source) {
        ArchiveSource.PHONE -> "plan z telefonu"
        ArchiveSource.DRIVE -> "plan z Dysku"
    },
    fileName = "mak-poprzednia-wersja-${fileDate.format(Instant.ofEpochMilli(createdAtMillis))}.json"
)

private val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("pl-PL"))
private val fileDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm").withZone(ZoneId.systemDefault())

private fun formatMillis(millis: Long): String =
    displayFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
