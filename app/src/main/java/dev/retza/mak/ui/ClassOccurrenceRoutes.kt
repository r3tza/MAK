package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.time.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import dev.retza.mak.ui.edit.ClassEditEffect
import dev.retza.mak.ui.edit.ClassEditOpenState
import dev.retza.mak.ui.edit.ClassEditScreen
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.occurrence.OccurrenceDetailsScreen
import dev.retza.mak.ui.occurrence.OccurrenceEffect
import dev.retza.mak.ui.occurrence.OccurrenceViewModel

internal fun openOccurrence(navController: NavController, occurrenceId: String) {
    navController.navigate(occurrenceRoute(occurrenceId))
}

internal fun openClassEditor(
    navController: NavController,
    date: LocalDate? = null
) {
    navController.navigate(newClassRoute(date))
}

internal fun NavGraphBuilder.classEditRoute(
    navController: NavController,
    onBack: () -> Unit,
    topBar: TopBarState
) {
    composable(
        route = MakRoutes.Edit,
        arguments = listOf(
            navArgument("classId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("date") {
                type = NavType.StringType
                nullable = true
            }
        )
    ) { _ ->
        val classEditViewModel: ClassEditViewModel = koinViewModel()
        val openState = classEditViewModel.opened.collectAsStateWithLifecycle().value
        LaunchedEffect(Unit) { classEditViewModel.openOnce() }
        if (openState != ClassEditOpenState.Open) {
            LaunchedEffect(openState) {
                if (openState == ClassEditOpenState.Missing) onBack()
            }
            PlanEditingWait()
            return@composable
        }
        ClassEditEffects(classEditViewModel, navController)
        val editorState = classEditViewModel.editor.collectAsStateWithLifecycle().value
        DeclareTopBar(topBar, TopBarSpec(MakRoutes.Edit, title = editorState.title))
        ClassEditScreen(
            state = editorState,
            onNameChanged = { value -> classEditViewModel.update { it.copy(name = value) } },
            onCourseChanged = classEditViewModel::selectCourse,
            onTypeChanged = { value -> classEditViewModel.update { it.copy(type = value) } },
            onDayChanged = { value -> classEditViewModel.update { it.copy(dayLabel = value) } },
            onStartTimeChanged = { value -> classEditViewModel.update { it.copy(startTime = value) } },
            onEndTimeChanged = { value -> classEditViewModel.update { it.copy(endTime = value) } },
            onRecurrenceChanged = { value -> classEditViewModel.update { it.copy(recurrence = value) } },
            onOccurrenceDateChanged = { value -> classEditViewModel.update { it.copy(occurrenceDate = value) } },
            onRoomChanged = { value -> classEditViewModel.update { it.copy(room = value) } },
            onBuildingChanged = { value -> classEditViewModel.update { it.copy(building = value) } },
            onGroupChanged = { value -> classEditViewModel.update { it.copy(group = value) } },
            onTeacherChanged = { value -> classEditViewModel.update { it.copy(teacher = value) } },
            onNoteChanged = { value -> classEditViewModel.update { it.copy(note = value) } },
            onSave = classEditViewModel::save,
            onConfirmHiddenData = classEditViewModel::confirmSaveWithHiddenData,
            onDismissHiddenData = classEditViewModel::dismissHiddenData,
            onCancel = onBack,
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun NavGraphBuilder.occurrenceDetailsRoute(
    navController: NavController,
    topBar: TopBarState
) {
    composable(
        route = MakRoutes.Occurrence,
        arguments = listOf(
            navArgument("classId") { type = NavType.StringType },
            navArgument("date") { type = NavType.StringType }
        )
    ) { _ ->
        val occurrenceViewModel: OccurrenceViewModel = koinViewModel()
        OccurrenceEffects(occurrenceViewModel, navController)
        val details = occurrenceViewModel.details.collectAsStateWithLifecycle().value
        val actions: List<Pair<String, () -> Unit>> = remember(details.canCancelOccurrence, details.canEditBaseClass, details.canDeleteBaseClass) {
            listOfNotNull(
                if (details.canCancelOccurrence) "Odwołaj termin" to { occurrenceViewModel.cancelOccurrence() } else null,
                if (details.canEditBaseClass) {
                    "Edytuj wszystkie terminy" to {
                        occurrenceViewModel.selectedClassId.value?.let { id ->
                            navController.navigate(editRoute(id, occurrenceViewModel.details.value.targetDateDraft))
                        }
                    }
                } else null,
                if (details.canDeleteBaseClass) "Usuń zajęcia" to { occurrenceViewModel.requestClassDeletion() } else null
            )
        }
        DeclareTopBar(topBar, TopBarSpec(MakRoutes.Occurrence, actions = actions))
        OccurrenceDetailsScreen(
            state = details,
            onDeleteBaseClass = occurrenceViewModel::deleteSelectedClass,
            onDismissDeleteConfirmation = occurrenceViewModel::cancelClassDeletion,
            onOpenOccurrenceEdit = occurrenceViewModel::openEditDialog,
            onDismissOccurrenceEdit = occurrenceViewModel::dismissEditDialog,
            onSaveOccurrenceChange = occurrenceViewModel::saveOccurrenceChange,
            onRestoreOccurrence = occurrenceViewModel::restoreOccurrence,
            onOccurrenceNoteDraftChanged = occurrenceViewModel::updateOccurrenceNoteDraft,
            onSharedNoteDraftChanged = occurrenceViewModel::updateSharedNoteDraft,
            onTargetDateDraftChanged = occurrenceViewModel::updateTargetDateDraft,
            onStartTimeDraftChanged = occurrenceViewModel::updateStartTimeDraft,
            onEndTimeDraftChanged = occurrenceViewModel::updateEndTimeDraft,
            onRoomDraftChanged = occurrenceViewModel::updateRoomDraft,
            onSaveSharedNote = occurrenceViewModel::saveSharedNote,
            onSaveOccurrenceNote = occurrenceViewModel::saveOccurrenceNote,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun OccurrenceEffects(
    occurrenceViewModel: OccurrenceViewModel,
    navController: NavController
) {
    LaunchedEffect(occurrenceViewModel, navController) {
        occurrenceViewModel.effects.collect { effect ->
            when (effect) {
                OccurrenceEffect.CloseDetails -> {
                    if (shouldCloseOccurrenceDetails(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
internal fun ClassEditEffects(
    classEditViewModel: ClassEditViewModel,
    navController: NavController
) {
    LaunchedEffect(classEditViewModel, navController) {
        classEditViewModel.effects.collect { effect ->
            when (effect) {
                ClassEditEffect.CloseEditor -> {
                    if (shouldCloseClassEditor(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
