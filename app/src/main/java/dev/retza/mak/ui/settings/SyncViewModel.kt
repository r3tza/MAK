package dev.retza.mak.ui.settings

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.sync.AccountMismatchException
import dev.retza.mak.sync.ArchiveSource
import dev.retza.mak.sync.AuthorizationAttempt
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
    // Banner on Today; set only while an account is connected and something needs the user.
    val attention: SyncAttentionUi? = null,
    val choice: SyncChoiceUi? = null,
    val archive: List<SyncArchiveItemUi> = emptyList(),
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val showChoiceDialog: Boolean = false,
    val showDisconnectDialog: Boolean = false
)

data class SyncChoiceUi(val phone: List<String>, val drive: List<String>)

data class SyncAttentionUi(val text: String, val action: String)

private data class SyncLocalState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val showChoiceDialog: Boolean = false,
    val showDisconnectDialog: Boolean = false
)

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
    attention = if (account == null) null else issueText()?.let { SyncAttentionUi(it, "Otwórz") }
        ?: pendingChoice?.let { SyncAttentionUi(CHOICE_TITLE, "Wybierz wersję") },
    lastSyncLabel = account?.let {
        lastSyncedAtMillis?.let { millis -> "Ostatnia synchronizacja: ${formatMillis(millis)}" }
            ?: "Jeszcze nie zsynchronizowano"
    },
    issue = issueText(),
    needsReconnect = issue == SyncIssue.AUTHORIZATION_REQUIRED,
    choice = pendingChoice?.let { SyncChoiceUi(it.local.label(), it.remote.label()) },
    archive = archive,
    isWorking = local.isWorking,
    errorMessage = local.errorMessage,
    showChoiceDialog = local.showChoiceDialog && pendingChoice != null,
    showDisconnectDialog = local.showDisconnectDialog && account != null
)

private fun SyncState.issueText(): String? = when (issue) {
    SyncIssue.AUTHORIZATION_REQUIRED -> "Google wymaga ponownego potwierdzenia dostępu do Dysku."
    SyncIssue.INVALID_REMOTE_PLAN -> issueMessage ?: "Plik planu na Dysku jest niepoprawny."
    SyncIssue.LOCAL_PLAN_TOO_LARGE -> issueMessage ?: "Lokalny plan przekracza limit 8 MiB i nie może zostać wysłany."
    SyncIssue.FAILED -> issueMessage ?: "Nie udało się zsynchronizować planu."
    null -> null
}

internal const val CHOICE_TITLE = "Plan różni się na telefonie i na Dysku"

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
