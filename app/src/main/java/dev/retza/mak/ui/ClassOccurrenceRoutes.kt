package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import java.time.LocalDate
import dev.retza.mak.ui.edit.ClassEditEffect
import dev.retza.mak.ui.edit.ClassEditScreen
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.occurrence.OccurrenceDetailsScreen
import dev.retza.mak.ui.occurrence.OccurrenceEffect
import dev.retza.mak.ui.occurrence.OccurrenceViewModel

internal fun openOccurrence(
    occurrenceViewModel: OccurrenceViewModel,
    navController: NavController,
    occurrenceId: String
) {
    occurrenceViewModel.open(occurrenceId)
    navController.navigate(occurrenceRoute(occurrenceId))
}

internal fun openClassEditor(
    classEditViewModel: ClassEditViewModel,
    navController: NavController,
    date: LocalDate? = null
) {
    classEditViewModel.openNew(date)
    navController.navigate(MakRoutes.Edit)
}

internal fun NavGraphBuilder.classEditRoute(
    classEditViewModel: ClassEditViewModel,
    onBack: () -> Unit
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
    ) { entry ->
        val classId = entry.arguments?.getString("classId")
        val date = entry.arguments?.getString("date")
        LaunchedEffect(classId, date) {
            if (classId != null && date != null) {
                classEditViewModel.openEditIfNeeded("$classId:$date")
            }
        }
        ClassEditScreen(
            state = classEditViewModel.editor.collectAsStateWithLifecycle().value,
            onNameChanged = { value -> classEditViewModel.update { it.copy(name = value) } },
            onCourseChanged = classEditViewModel::selectCourse,
            onTypeChanged = { value -> classEditViewModel.update { it.copy(type = value) } },
            onDayChanged = { value -> classEditViewModel.update { it.copy(dayLabel = value) } },
            onStartTimeChanged = { value -> classEditViewModel.update { it.copy(startTime = value) } },
            onEndTimeChanged = { value -> classEditViewModel.update { it.copy(endTime = value) } },
            onRecurrenceChanged = { value ->
                classEditViewModel.update {
                    it.copy(
                        recurrenceId = value,
                        recurrenceLabel = recurrenceLabelForUi(value)
                    )
                }
            },
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
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun NavGraphBuilder.occurrenceDetailsRoute(
    occurrenceViewModel: OccurrenceViewModel
) {
    composable(
        route = MakRoutes.Occurrence,
        arguments = listOf(
            navArgument("classId") { type = NavType.StringType },
            navArgument("date") { type = NavType.StringType }
        )
    ) { entry ->
        val classId = entry.arguments?.getString("classId")
        val date = entry.arguments?.getString("date")
        LaunchedEffect(classId, date) {
            if (classId != null && date != null) {
                occurrenceViewModel.open("$classId:$date")
            }
        }
        OccurrenceDetailsScreen(
            state = occurrenceViewModel.details.collectAsStateWithLifecycle().value,
            onDeleteBaseClass = occurrenceViewModel::deleteSelectedClass,
            onDismissDeleteConfirmation = occurrenceViewModel::cancelClassDeletion,
            onOpenOccurrenceEdit = occurrenceViewModel::openEditDialog,
            onDismissOccurrenceEdit = occurrenceViewModel::dismissEditDialog,
            onSaveOccurrenceChange = occurrenceViewModel::saveOccurrenceChange,
            onRestoreOccurrence = occurrenceViewModel::restoreOccurrence,
            onOccurrenceNoteDraftChanged = occurrenceViewModel::updateOccurrenceNoteDraft,
            onSharedNoteDraftChanged = occurrenceViewModel::updateSharedNoteDraft,
            onTargetDateDraftChanged = { value ->
                occurrenceViewModel.updateDraft { it.copy(targetDateDraft = value) }
            },
            onStartTimeDraftChanged = { value ->
                occurrenceViewModel.updateDraft { it.copy(startTimeDraft = value) }
            },
            onEndTimeDraftChanged = { value ->
                occurrenceViewModel.updateDraft { it.copy(endTimeDraft = value) }
            },
            onRoomDraftChanged = { value ->
                occurrenceViewModel.updateDraft { it.copy(roomDraft = value) }
            },
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

@Composable
internal fun occurrenceTopBarActions(
    occurrenceViewModel: OccurrenceViewModel,
    classEditViewModel: ClassEditViewModel,
    navController: NavController
): List<Pair<String, () -> Unit>> {
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    if (route != MakRoutes.Occurrence) return emptyList()
    val details = occurrenceViewModel.details.collectAsStateWithLifecycle().value
    return listOfNotNull(
        if (details.canCancelOccurrence) {
            "Odwołaj termin" to occurrenceViewModel::cancelOccurrence
        } else null,
        if (details.canEditBaseClass) {
            "Edytuj wszystkie terminy" to {
                occurrenceViewModel.selectedClassId.value?.let { id ->
                    classEditViewModel.openEdit("$id:${details.targetDateDraft}")
                    navController.navigate(editRoute(id, details.targetDateDraft))
                }
            }
        } else null,
        if (details.canDeleteBaseClass) {
            "Usuń zajęcia" to occurrenceViewModel::requestClassDeletion
        } else null
    )
}

private fun recurrenceLabelForUi(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}
