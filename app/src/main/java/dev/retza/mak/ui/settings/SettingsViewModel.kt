package dev.retza.mak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.JsonExportCodec
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class SettingsLocalState(
    val semesterToDeleteId: String? = null,
    val isDeletingSemester: Boolean = false,
    val isSelectingSemester: Boolean = false,
    val isSavingTheme: Boolean = false,
    val isExporting: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val repository: MakRepository,
    private val preferences: SettingsPreferences,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val local = MutableStateFlow(SettingsLocalState())

    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val themeMode: StateFlow<ThemeMode> = preferences.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    val settings: StateFlow<SettingsUiState> = combine(
        repository.observeSemesters(),
        activeSemesterData,
        preferences.theme,
        local
    ) { semesters, activeData, theme, state ->
        buildSettingsState(semesters, activeData, theme, state)
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
                onReady(JsonExportCodec.encode(ExportSnapshot.from(repository.getAllSemesterData())))
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

    class Factory(
        private val repository: MakRepository,
        private val preferences: SettingsPreferences,
        private val feedbackSink: FeedbackSink
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(repository, preferences, feedbackSink) as T
        }
    }
}

private fun buildSettingsState(
    semesterList: List<SemesterEntity>,
    activeData: SemesterWithData?,
    theme: ThemeMode,
    local: SettingsLocalState
): SettingsUiState = SettingsUiState(
    semesters = semesterList.map { semester ->
        val isActive = semester.id == activeData?.semester?.id
        SemesterUi(
            id = semester.id.toString(),
            name = semester.name,
            dateRangeLabel = "${semester.startDate.format(shortDateFormatter)} - " +
                semester.endDate.format(shortDateFormatter),
            firstWeekLabel = "Pierwszy tydzień ${semester.firstWeekType.name}",
            courseCountLabel = if (isActive) "${activeData.courses.size} kierunków" else "Dane odizolowane",
            classCountLabel = if (isActive) classCountLabel(activeData.classes.size) else "Osobny plan",
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
    isDeletingSemester = local.isDeletingSemester
)

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl-PL"))

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}
