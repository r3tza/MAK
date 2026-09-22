package dev.retza.mak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.export.ExportImporter
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.ImportSnapshotResult
import dev.retza.mak.export.JsonExportCodec
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
    private val repository: MakRepository,
    private val preferences: SettingsPreferences,
    private val feedbackSink: FeedbackSink,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val local = MutableStateFlow(SettingsLocalState())

    private val effectsChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var pendingImport: BackupData? = null

    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val themeMode: StateFlow<ThemeMode> = preferences.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    val settings: StateFlow<SettingsUiState> = combine(
        repository.observeSemesters(),
        activeSemesterData,
        preferences.theme,
        preferences.collisionNotifications,
        local
    ) { semesters, activeData, theme, notifications, state ->
        buildSettingsState(semesters, activeData, theme, notifications, state)
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
                repository.setActiveSemester(semesterId)
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
                repository.deleteSemesterAndSelectFallback(id)
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
                    JsonExportCodec.encode(
                        ExportSnapshot.from(
                            repository.getAllSemesterData(),
                            repository.getAllStudyPrograms()
                        )
                    )
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
                val result = withContext(backgroundDispatcher) {
                    ExportImporter.prepare(JsonExportCodec.decode(bytes))
                }
                when (result) {
                    is ImportSnapshotResult.Invalid -> local.update {
                        it.copy(
                            importErrorMessage = result.errors.firstOrNull()
                                ?: "Nieprawidłowy plik kopii."
                        )
                    }

                    is ImportSnapshotResult.Ready -> {
                        pendingImport = result.data
                        local.update { it.copy(importPreview = result.data.toImportPreviewUi()) }
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
        val data = pendingImport ?: return
        if (local.value.isReplacingData) return
        local.update { it.copy(isReplacingData = true, importErrorMessage = null) }
        viewModelScope.launch {
            try {
                repository.replaceAllData(data)
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

private fun BackupData.toImportPreviewUi(): ImportPreviewUi = ImportPreviewUi(
    semesterCount = semesters.size,
    programCount = studyPrograms.size,
    classCount = semesters.sumOf { it.classes.size },
    overrideCount = semesters.sumOf { it.weekOverrides.size },
    noteCount = semesters.sumOf { it.occurrenceNotes.size },
    changeCount = semesters.sumOf { it.occurrenceChanges.size },
    activeSemesterName = semesters.firstOrNull { it.semester.isActive }?.semester?.name
)

private fun buildSettingsState(
    semesterList: List<SemesterEntity>,
    activeData: SemesterWithData?,
    theme: ThemeMode,
    notifications: CollisionNotificationPreferences,
    local: SettingsLocalState
): SettingsUiState = SettingsUiState(
    semesters = semesterList.map { semester ->
        val isActive = semester.id == activeData?.semester?.id
        val calendar = activeData?.academicCalendars?.minByOrNull { it.id }
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
        ThemeOptionUi("system", "Systemowy", theme == ThemeMode.System),
        ThemeOptionUi("light", "Jasny", theme == ThemeMode.Light),
        ThemeOptionUi("dark", "Ciemny", theme == ThemeMode.Dark)
    ),
    semesterToDeleteId = local.semesterToDeleteId,
    isDeletingSemester = local.isDeletingSemester,
    importPreview = local.importPreview,
    importErrorMessage = local.importErrorMessage,
    isPreparingImport = local.isPreparingImport,
    isReplacingData = local.isReplacingData,
    notifications = NotificationSettingsUi(
        enabled = notifications.enabled,
        eveningEnabled = notifications.eveningEnabled,
        beforeClassEnabled = notifications.beforeClassEnabled,
        eveningHourOptions = notificationHourOptions.map { hour ->
            val id = "%02d:00".format(hour)
            NotificationOptionUi(id, id, notifications.eveningHour.hour == hour)
        },
        leadOptions = notificationLeadOptions.map { minutes ->
            NotificationOptionUi(
                id = minutes.toString(),
                label = "$minutes min",
                isSelected = notifications.leadMinutes == minutes
            )
        }
    )
)

private val notificationHourOptions = listOf(18, 19, 20, 21, 22)
private val notificationLeadOptions = listOf(15L, 30L, 45L, 60L)

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl-PL"))

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}
