package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dev.retza.mak.ui.occurrence.hasPlanDraft

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
        val editing = TrackPlanEditing()
        if (!editing.ready) {
            PlanEditingWait()
            return@composable
        }
        var opened by rememberSaveable(entry.id) { mutableStateOf(false) }
        var openSucceeded by rememberSaveable(entry.id) { mutableStateOf(false) }
        val classId = entry.arguments?.getString("classId")
        val date = entry.arguments?.getString("date")
        LaunchedEffect(editing.ready, classId, date, opened) {
            if (!opened) {
                openSucceeded = if (classId != null && date != null) {
                    classEditViewModel.openEditFromFreshPlan("$classId:$date")
                } else {
                    classEditViewModel.openNewFromFreshPlan(date?.let(LocalDate::parse))
                    true
                }
                opened = true
            }
        }
        if (!opened || !openSucceeded) {
            LaunchedEffect(opened, openSucceeded) {
                if (opened && !openSucceeded) onBack()
            }
            PlanEditingWait()
            return@composable
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
        val occurrenceId = entry.arguments?.getString("classId")?.let { classId ->
            entry.arguments?.getString("date")?.let { date -> "$classId:$date" }
        }
        val editing = TrackPlanEditing(
            active = occurrenceViewModel.details.collectAsStateWithLifecycle().value.hasPlanDraft(),
            isActiveNow = { occurrenceViewModel.details.value.hasPlanDraft() }
        )
        val classId = entry.arguments?.getString("classId")
        val date = entry.arguments?.getString("date")
        var opened by rememberSaveable(entry.id) { mutableStateOf(false) }
        LaunchedEffect(classId, date, opened) {
            if (!opened && classId != null && date != null) {
                occurrenceViewModel.open("$classId:$date")
                opened = true
            }
        }
        OccurrenceDetailsScreen(
            state = occurrenceViewModel.details.collectAsStateWithLifecycle().value,
            onDeleteBaseClass = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::deleteSelectedClass
            ),
            onDismissDeleteConfirmation = editing.callback(action = occurrenceViewModel::cancelClassDeletion),
            onOpenOccurrenceEdit = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::openEditDialog
            ),
            onDismissOccurrenceEdit = editing.callback(action = occurrenceViewModel::dismissEditDialog),
            onSaveOccurrenceChange = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::saveOccurrenceChange
            ),
            onRestoreOccurrence = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::restoreOccurrence
            ),
            onOccurrenceNoteDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::updateOccurrenceNoteDraft
            ),
            onSharedNoteDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = occurrenceViewModel::updateSharedNoteDraft
            ),
            onTargetDateDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = { value -> occurrenceViewModel.updateDraft { it.copy(targetDateDraft = value) } }
            ),
            onStartTimeDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = { value -> occurrenceViewModel.updateDraft { it.copy(startTimeDraft = value) } }
            ),
            onEndTimeDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = { value -> occurrenceViewModel.updateDraft { it.copy(endTimeDraft = value) } }
            ),
            onRoomDraftChanged = editing.callback(
                refreshBeforeFirstEdit = { if (occurrenceId != null) occurrenceViewModel.refreshForEdit(occurrenceId) else false },
                action = { value -> occurrenceViewModel.updateDraft { it.copy(roomDraft = value) } }
            ),
            onSaveSharedNote = editing.callback(action = occurrenceViewModel::saveSharedNote),
            onSaveOccurrenceNote = editing.callback(action = occurrenceViewModel::saveOccurrenceNote),
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
    navController: NavController
): List<Pair<String, () -> Unit>> {
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    if (route != MakRoutes.Occurrence) return emptyList()
    val details = occurrenceViewModel.details.collectAsStateWithLifecycle().value
    val editing = TrackPlanEditing(
        active = details.hasPlanDraft(),
        isActiveNow = { occurrenceViewModel.details.value.hasPlanDraft() }
    )
    return listOfNotNull(
        if (details.canCancelOccurrence) {
            "Odwołaj termin" to editing.callback(
                refreshBeforeFirstEdit = {
                    val classId = occurrenceViewModel.selectedClassId.value
                    val originalDate = occurrenceViewModel.details.value.baseDate
                    if (classId == null || originalDate.isBlank()) false
                    else occurrenceViewModel.refreshForEdit("$classId:$originalDate")
                },
                action = occurrenceViewModel::cancelOccurrence
            )
        } else null,
        if (details.canEditBaseClass) {
            "Edytuj wszystkie terminy" to {
                occurrenceViewModel.selectedClassId.value?.let { id ->
                    navController.navigate(editRoute(id, details.targetDateDraft))
                }
            }
        } else null,
        if (details.canDeleteBaseClass) {
            "Usuń zajęcia" to editing.callback(
                refreshBeforeFirstEdit = {
                    val classId = occurrenceViewModel.selectedClassId.value
                    val originalDate = occurrenceViewModel.details.value.baseDate
                    if (classId == null || originalDate.isBlank()) false
                    else occurrenceViewModel.refreshForEdit("$classId:$originalDate")
                },
                action = occurrenceViewModel::requestClassDeletion
            )
        } else null
    )
}

private fun recurrenceLabelForUi(id: String): String = when (id) {
    "a_week" -> "Tydzień A"
    "b_week" -> "Tydzień B"
    "once" -> "Jednorazowo"
    else -> "Co tydzień"
}
