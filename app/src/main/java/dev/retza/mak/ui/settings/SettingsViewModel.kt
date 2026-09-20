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
import java.time.format.DateTimeFormatter
import java.util.Locale
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

sealed interface SettingsEffect {
    data object OpenSetup : SettingsEffect
}

private data class SettingsLocalState(
    val semesterToDeleteId: String? = null,
    val isDeletingSemester: Boolean = false,
    val isSelectingSemester: Boolean = false,
    val isSavingTheme: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val repository: MakRepository,
    private val preferences: SettingsPreferences,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    private val local = MutableStateFlow(SettingsLocalState())

    private val effectsChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

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
        id.toLongOrNull()?.let { semesterId ->
            viewModelScope.launch { repository.setActiveSemester(semesterId) }
        }
    }

    fun requestSemesterDeletion(id: String) {
        local.update { it.copy(semesterToDeleteId = id) }
    }

    fun cancelSemesterDeletion() {
        local.update { it.copy(semesterToDeleteId = null) }
    }

    fun confirmSemesterDeletion() {
        val id = local.value.semesterToDeleteId?.toLongOrNull() ?: return
        val remaining = settings.value.semesters.firstOrNull { it.id != id.toString() }
        viewModelScope.launch {
            repository.deleteSemester(id)
            if (remaining != null) repository.setActiveSemester(remaining.id.toLong())
            else repository.clearActiveSemester()
            local.update { it.copy(semesterToDeleteId = null) }
            if (remaining == null) effectsChannel.trySend(SettingsEffect.OpenSetup)
        }
    }

    fun selectTheme(id: String) {
        viewModelScope.launch {
            runCatching { preferences.setTheme(themeModeFromId(id)) }
        }
    }

    fun exportJson(onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            onReady(JsonExportCodec.encode(ExportSnapshot.from(repository.getAllSemesterData())))
        }
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
    semesterToDeleteId = local.semesterToDeleteId
)

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl-PL"))

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}
