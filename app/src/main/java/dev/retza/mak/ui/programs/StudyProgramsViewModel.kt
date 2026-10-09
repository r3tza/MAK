package dev.retza.mak.ui.programs

import dev.retza.mak.ui.components.DefaultCourseColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.settings.SettingsPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class StudyProgramUi(
    val id: Long,
    val name: String,
    val color: String
)

data class StudyProgramEditorUi(
    val id: Long? = null,
    val name: String = "",
    val color: String = DefaultCourseColor,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    // An assigned program is protected; the names explain where it is still used.
    val usedInSemesters: List<String> = emptyList(),
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false
) {
    val canDelete: Boolean get() = id != null && usedInSemesters.isEmpty() && !isSaving && !isDeleting
}

data class StudyProgramsUiState(
    val programs: List<StudyProgramUi> = emptyList(),
    val editor: StudyProgramEditorUi = StudyProgramEditorUi()
)

sealed interface StudyProgramsEffect {
    data object CloseEditor : StudyProgramsEffect
}

/** Global study programs: the list in settings, editing of their name and color, and deletion of unused ones. */
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class StudyProgramsViewModel(
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink,
    private val preferences: SettingsPreferences
) : ViewModel() {
    private val state = MutableStateFlow(StudyProgramsUiState())
    val programs: StateFlow<StudyProgramsUiState> = state.asStateFlow()

    private val effectsChannel = Channel<StudyProgramsEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                semesterRepository.observeStudyPrograms().collect { records ->
                    state.update { current ->
                        current.copy(programs = records.map { StudyProgramUi(it.id, it.name, it.color) })
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                feedbackSink.publish(UiFeedback("Nie udało się wczytać kierunków.", UiFeedbackKind.Error))
            }
        }
        viewModelScope.launch {
            state.map { it.editor.id }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null) flowOf(null) else semesterRepository.observeStudyProgramSemesters(id).map { id to it }
                }
                .catch { emit(null) }
                .collect { usage ->
                    if (usage == null) return@collect
                    val (id, names) = usage
                    state.update { current ->
                        if (current.editor.id != id) current
                        else current.copy(editor = current.editor.copy(usedInSemesters = names))
                    }
                }
        }
    }

    /** Starts a fresh edit of the program: discards any earlier draft, then loads the saved values. */
    fun openEdit(id: Long) {
        state.update { it.copy(editor = StudyProgramEditorUi()) }
        loadEditor(id)
    }

    /** Starts a new route edit from the latest Room row after the route has acquired its edit key. */
    suspend fun openEditFromRoom(id: Long): Boolean {
        state.update { it.copy(editor = StudyProgramEditorUi()) }
        val program = semesterRepository.observeStudyPrograms().first()
            .firstOrNull { it.id == id }
            ?.let { StudyProgramUi(it.id, it.name, it.color) }
        if (program == null) return false
        state.update {
            it.copy(editor = StudyProgramEditorUi(id = program.id, name = program.name, color = program.color))
        }
        return true
    }

    /**
     * Restores the screen after rotation or process death: keeps the current draft when the route
     * is shown again for the same program.
     */
    fun openEditIfNeeded(id: Long) {
        if (state.value.editor.id == id) return
        loadEditor(id)
    }

    private fun loadEditor(id: Long) {
        viewModelScope.launch {
            val program = semesterRepository.observeStudyPrograms().first()
                .firstOrNull { it.id == id }
                ?.let { StudyProgramUi(it.id, it.name, it.color) } ?: return@launch
            state.update {
                val current = it.editor
                if (current.id == id && (current.name != program.name || current.color != program.color)) {
                    it
                } else {
                    // Same program: the usage observer does not emit again, so its names are kept.
                    val usedIn = if (current.id == id) current.usedInSemesters else emptyList()
                    it.copy(
                        editor = StudyProgramEditorUi(
                            id = program.id,
                            name = program.name,
                            color = program.color,
                            usedInSemesters = usedIn
                        )
                    )
                }
            }
        }
    }

    fun updateName(value: String) = state.update {
        it.copy(editor = it.editor.copy(name = value, nameError = null))
    }

    fun updateColor(value: String) = state.update {
        it.copy(editor = it.editor.copy(color = value))
    }

    fun closeEditor() = state.update { it.copy(editor = StudyProgramEditorUi()) }

    fun requestDelete() = state.update {
        if (it.editor.canDelete) it.copy(editor = it.editor.copy(showDeleteConfirmation = true)) else it
    }

    fun cancelDelete() = state.update {
        if (it.editor.isDeleting) it else it.copy(editor = it.editor.copy(showDeleteConfirmation = false))
    }

    fun confirmDelete() {
        val editor = state.value.editor
        val id = editor.id ?: return
        if (!editor.showDeleteConfirmation || editor.isDeleting) return
        state.update { it.copy(editor = it.editor.copy(isDeleting = true)) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            // The repository refuses a program that was assigned while the dialog was open.
            errorMessage = "Nie udało się usunąć kierunku.",
            onFinish = {
                state.update {
                    if (it.editor.id != id) it
                    else it.copy(editor = it.editor.copy(isDeleting = false, showDeleteConfirmation = false))
                }
            }
        ) {
            semesterRepository.deleteStudyProgram(id)
            // A later program could get the same number and must not start hidden.
            preferences.setStudyProgramHidden(id.toString(), hidden = false)
            state.update { it.copy(editor = StudyProgramEditorUi()) }
            feedbackSink.publish(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success))
            effectsChannel.trySend(StudyProgramsEffect.CloseEditor)
        }
    }

    fun save() {
        val editor = state.value.editor
        val id = editor.id ?: return
        if (editor.isSaving) return
        val name = editor.name.trim()
        if (name.isBlank()) {
            state.update { it.copy(editor = it.editor.copy(nameError = "Podaj nazwę kierunku.")) }
            return
        }
        state.update { it.copy(editor = it.editor.copy(isSaving = true)) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać kierunku.",
            onFinish = { state.update { it.copy(editor = it.editor.copy(isSaving = false)) } }
        ) {
            semesterRepository.saveStudyProgram(StudyProgramRecord(id = id, name = name, color = editor.color))
            state.update { it.copy(editor = StudyProgramEditorUi()) }
            feedbackSink.publish(UiFeedback("Zapisano kierunek", UiFeedbackKind.Success))
            effectsChannel.trySend(StudyProgramsEffect.CloseEditor)
        }
    }
}
