package dev.retza.mak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.export.ImportHandle
import dev.retza.mak.export.ImportPreparation
import dev.retza.mak.export.ImportSummary
import dev.retza.mak.export.PlanBackupService
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsEffect {
    data object OpenImportPreview : SettingsEffect

    data object CloseImportPreview : SettingsEffect
}

private data class PreferencesSnapshot(
    val theme: ThemeMode,
    val notifications: CollisionNotificationPreferences,
    val gapThresholdMinutes: Int
)

private data class SettingsLocalState(
    val semesterToDeleteId: String? = null,
    val isDeletingSemester: Boolean = false,
    val isSelectingSemester: Boolean = false,
    val isSavingTheme: Boolean = false,
    val isExporting: Boolean = false,
    val importPreview: ImportPreviewUi? = null,
    val importErrorMessage: String? = null,
    val isPreparingImport: Boolean = false,
    val isReplacingData: Boolean = false,
    val isSavingNotifications: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class SettingsViewModel(
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val planBackupService: PlanBackupService,
    private val preferences: SettingsPreferences,
    private val feedbackSink: FeedbackSink,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val local = MutableStateFlow(SettingsLocalState())

    private val effectsChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var pendingImport: ImportHandle? = null

    private val activePlanData = semesterRepository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else scheduleRepository.observeActivePlanData(semester.id)
    }

    val themeMode: StateFlow<ThemeMode> = preferences.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    private val preferencesSnapshot = combine(
        preferences.theme,
        preferences.collisionNotifications,
        preferences.gapThresholdMinutes
    ) { theme, notifications, gapThresholdMinutes ->
        PreferencesSnapshot(theme, notifications, gapThresholdMinutes)
    }

    val settings: StateFlow<SettingsUiState> = combine(
        semesterRepository.observeSemesters(),
        activePlanData,
        preferencesSnapshot,
        local
    ) { semesters, activeData, preferencesState, state ->
        buildSettingsState(semesters, activeData, preferencesState, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun selectSemester(id: String) {
        if (local.value.isSelectingSemester) return
        val semesterId = id.toLongOrNull() ?: return
        local.update { it.copy(isSelectingSemester = true) }
        viewModelScope.launch {
            try {
                semesterRepository.setActiveSemester(semesterId)
                feedbackSink.publish(UiFeedback("Zmieniono aktywny semestr", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(
                    UiFeedback("Nie udało się zmienić aktywnego semestru.", UiFeedbackKind.Error)
                )
            } finally {
                local.update { it.copy(isSelectingSemester = false) }
            }
        }
    }

    fun requestSemesterDeletion(id: String) {
        local.update { it.copy(semesterToDeleteId = id) }
    }

    fun cancelSemesterDeletion() {
        if (local.value.isDeletingSemester) return
        local.update { it.copy(semesterToDeleteId = null) }
    }

    fun confirmSemesterDeletion() {
        if (local.value.isDeletingSemester) return
        val id = local.value.semesterToDeleteId?.toLongOrNull() ?: return
        local.update { it.copy(isDeletingSemester = true) }
        viewModelScope.launch {
            try {
                semesterRepository.deleteSemesterAndSelectFallback(id)
                local.update { it.copy(semesterToDeleteId = null) }
                feedbackSink.publish(UiFeedback("Usunięto semestr", UiFeedbackKind.Success))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(UiFeedback("Nie udało się usunąć semestru.", UiFeedbackKind.Error))
            } finally {
                local.update { it.copy(isDeletingSemester = false) }
            }
        }
    }

    fun selectTheme(id: String) {
        if (local.value.isSavingTheme) return
        val mode = themeModeFromId(id)
        local.update { it.copy(isSavingTheme = true) }
        viewModelScope.launch {
            try {
                preferences.setTheme(mode)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(UiFeedback("Nie udało się zapisać motywu.", UiFeedbackKind.Error))
            } finally {
                local.update { it.copy(isSavingTheme = false) }
            }
        }
    }

    fun exportJson(onReady: (ByteArray) -> Unit) {
        if (local.value.isExporting) return
        local.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val bytes = withContext(backgroundDispatcher) {
                    planBackupService.exportJson()
                }
                onReady(bytes)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(
                    UiFeedback("Nie udało się wyeksportować planu.", UiFeedbackKind.Error)
                )
            } finally {
                local.update { it.copy(isExporting = false) }
            }
        }
    }

    fun reportExportFinished(success: Boolean) {
        feedbackSink.publish(
            if (success) {
                UiFeedback("Wyeksportowano plan", UiFeedbackKind.Success)
            } else {
                UiFeedback("Nie udało się wyeksportować planu.", UiFeedbackKind.Error)
            }
        )
    }

    fun setNotificationsEnabled(enabled: Boolean) =
        saveNotifications { preferences.setCollisionNotificationsEnabled(enabled) }

    fun setEveningNotificationsEnabled(enabled: Boolean) =
        saveNotifications { preferences.setEveningNotificationsEnabled(enabled) }

    fun setBeforeClassNotificationsEnabled(enabled: Boolean) =
        saveNotifications { preferences.setBeforeClassNotificationsEnabled(enabled) }

    fun setEveningHour(id: String) {
        val time = runCatching { java.time.LocalTime.parse(id) }.getOrNull() ?: return
        saveNotifications { preferences.setEveningHour(time) }
    }

    fun setBeforeClassLeadMinutes(id: String) {
        val minutes = id.toLongOrNull() ?: return
        saveNotifications { preferences.setBeforeClassLeadMinutes(minutes) }
    }

    fun setGapThresholdMinutes(id: String) {
        val minutes = id.toIntOrNull() ?: return
        saveNotifications { preferences.setGapThresholdMinutes(minutes) }
    }

    private fun saveNotifications(block: suspend () -> Unit) {
        if (local.value.isSavingNotifications) return
        local.update { it.copy(isSavingNotifications = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(
                    UiFeedback("Nie udało się zapisać ustawień powiadomień.", UiFeedbackKind.Error)
                )
            } finally {
                local.update { it.copy(isSavingNotifications = false) }
            }
        }
    }

    fun prepareImport(bytes: ByteArray) {
        if (local.value.isPreparingImport) return
        local.update { it.copy(isPreparingImport = true, importErrorMessage = null) }
        viewModelScope.launch {
            try {
                val preparation = withContext(backgroundDispatcher) {
                    planBackupService.prepareImport(bytes)
                }
                when (preparation) {
                    is ImportPreparation.Invalid -> local.update {
                        it.copy(
                            importErrorMessage = preparation.errors.firstOrNull()
                                ?: "Nieprawidłowy plik kopii."
                        )
                    }

                    is ImportPreparation.Ready -> {
                        pendingImport = preparation.handle
                        local.update { it.copy(importPreview = preparation.summary.toImportPreviewUi()) }
                        effectsChannel.trySend(SettingsEffect.OpenImportPreview)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                local.update { it.copy(importErrorMessage = "Nie udało się odczytać pliku.") }
            } finally {
                local.update { it.copy(isPreparingImport = false) }
            }
        }
    }

    fun reportImportReadError() {
        local.update { it.copy(importErrorMessage = "Nie udało się odczytać pliku.") }
    }

    fun dismissImportError() = local.update { it.copy(importErrorMessage = null) }

    fun cancelImport() {
        if (local.value.isReplacingData) return
        pendingImport = null
        local.update { it.copy(importPreview = null, importErrorMessage = null) }
        effectsChannel.trySend(SettingsEffect.CloseImportPreview)
    }

    fun confirmImport() {
        val handle = pendingImport ?: return
        if (local.value.isReplacingData) return
        local.update { it.copy(isReplacingData = true, importErrorMessage = null) }
        viewModelScope.launch {
            try {
                planBackupService.confirmImport(handle)
                pendingImport = null
                local.update { it.copy(importPreview = null) }
                feedbackSink.publish(UiFeedback("Zaimportowano plan", UiFeedbackKind.Success))
                effectsChannel.trySend(SettingsEffect.CloseImportPreview)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                local.update { it.copy(importErrorMessage = "Nie udało się zaimportować planu.") }
                feedbackSink.publish(UiFeedback("Nie udało się zaimportować planu.", UiFeedbackKind.Error))
            } finally {
                local.update { it.copy(isReplacingData = false) }
            }
        }
    }
}

private fun ImportSummary.toImportPreviewUi(): ImportPreviewUi = ImportPreviewUi(
    semesterCount = semesterCount,
    programCount = programCount,
    classCount = classCount,
    overrideCount = overrideCount,
    noteCount = noteCount,
    changeCount = changeCount,
    activeSemesterName = activeSemesterName
)

private fun buildSettingsState(
    semesterList: List<SemesterRecord>,
    activeData: ActivePlanData?,
    preferences: PreferencesSnapshot,
    local: SettingsLocalState
): SettingsUiState = SettingsUiState(
    semesters = semesterList.map { semester ->
        val isActive = semester.id.toString() == activeData?.semester?.id
        val calendar = activeData?.calendars?.firstOrNull()
        SemesterUi(
            id = semester.id.toString(),
            name = semester.name,
            dateRangeLabel = if (isActive && calendar != null) {
                "${calendar.startDate.format(shortDateFormatter)} - " +
                    calendar.endDate.format(shortDateFormatter)
            } else {
                "Dane odizolowane"
            },
            firstWeekLabel = if (isActive && calendar != null) {
                "Pierwszy tydzień ${calendar.firstWeekType.name}"
            } else {
                "Osobny kalendarz"
            },
            courseCountLabel = if (isActive) {
                "${activeData?.semesterPrograms?.size ?: 0} kierunków"
            } else {
                "Dane odizolowane"
            },
            classCountLabel = if (isActive) {
                classCountLabel(activeData?.classes?.size ?: 0)
            } else {
                "Osobny plan"
            },
            isActive = isActive
        )
    },
    activeSemesterId = activeData?.semester?.id?.toString(),
    themeOptions = listOf(
        ThemeOptionUi("system", "Systemowy", preferences.theme == ThemeMode.System),
        ThemeOptionUi("light", "Jasny", preferences.theme == ThemeMode.Light),
        ThemeOptionUi("dark", "Ciemny", preferences.theme == ThemeMode.Dark)
    ),
    gapThresholdOptions = gapThresholdOptions.map { minutes ->
        GapThresholdOptionUi(
            id = minutes.toString(),
            label = "$minutes min",
            isSelected = preferences.gapThresholdMinutes == minutes
        )
    },
    semesterToDeleteId = local.semesterToDeleteId,
    isDeletingSemester = local.isDeletingSemester,
    importPreview = local.importPreview,
    importErrorMessage = local.importErrorMessage,
    isPreparingImport = local.isPreparingImport,
    isReplacingData = local.isReplacingData,
    notifications = NotificationSettingsUi(
        enabled = preferences.notifications.enabled,
        eveningEnabled = preferences.notifications.eveningEnabled,
        beforeClassEnabled = preferences.notifications.beforeClassEnabled,
        eveningHourOptions = notificationHourOptions.map { hour ->
            val id = "%02d:00".format(hour)
            NotificationOptionUi(id, id, preferences.notifications.eveningHour.hour == hour)
        },
        leadOptions = notificationLeadOptions.map { minutes ->
            NotificationOptionUi(
                id = minutes.toString(),
                label = "$minutes min",
                isSelected = preferences.notifications.leadMinutes == minutes
            )
        }
    )
)

private val notificationHourOptions = listOf(18, 19, 20, 21, 22)
private val notificationLeadOptions = listOf(15L, 30L, 45L, 60L)
private val gapThresholdOptions = listOf(15, 20, 30, 45, 60)

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl-PL"))

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}
