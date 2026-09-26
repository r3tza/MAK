package dev.retza.mak.ui.programs

import dev.retza.mak.ui.components.DefaultCourseColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val isSaving: Boolean = false
)

data class StudyProgramsUiState(
    val programs: List<StudyProgramUi> = emptyList(),
    val editor: StudyProgramEditorUi = StudyProgramEditorUi()
)

sealed interface StudyProgramsEffect {
    data object CloseEditor : StudyProgramsEffect
}

/** Global study programs: the list in settings and editing of their name and color. */
@KoinViewModel
class StudyProgramsViewModel(
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink
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
    }

    /** Keeps the current draft when the route is shown again for the same program. */
    fun openEditIfNeeded(id: Long) {
        if (state.value.editor.id == id) return
        viewModelScope.launch {
            val program = state.value.programs.firstOrNull { it.id == id }
                ?: semesterRepository.observeStudyPrograms().first()
                    .firstOrNull { it.id == id }
                    ?.let { StudyProgramUi(it.id, it.name, it.color) }
                ?: return@launch
            state.update {
                it.copy(editor = StudyProgramEditorUi(id = program.id, name = program.name, color = program.color))
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
        viewModelScope.launch {
            try {
                semesterRepository.saveStudyProgram(StudyProgramRecord(id = id, name = name, color = editor.color))
                state.update { it.copy(editor = StudyProgramEditorUi()) }
                feedbackSink.publish(UiFeedback("Zapisano kierunek", UiFeedbackKind.Success))
                effectsChannel.trySend(StudyProgramsEffect.CloseEditor)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(editor = it.editor.copy(isSaving = false)) }
                feedbackSink.publish(UiFeedback("Nie udało się zapisać kierunku.", UiFeedbackKind.Error))
            }
        }
    }
}
