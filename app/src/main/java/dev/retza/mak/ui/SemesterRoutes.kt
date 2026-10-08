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
import androidx.navigation.navArgument
import dev.retza.mak.ui.semester.SemesterCalendarsScreen
import dev.retza.mak.ui.semester.SemesterCourseAddScreen
import dev.retza.mak.ui.semester.SemesterCourseEditScreen
import dev.retza.mak.ui.semester.SemesterCoursesScreen
import dev.retza.mak.ui.semester.SemesterEffect
import dev.retza.mak.ui.semester.SemesterScreen
import dev.retza.mak.ui.semester.SemesterViewModel
import dev.retza.mak.ui.semester.hasPlanDraft
import dev.retza.mak.ui.semester.SemesterWeekOverridesScreen
import dev.retza.mak.ui.programs.StudyProgramEditorUi
import dev.retza.mak.ui.programs.StudyProgramsViewModel

internal fun openSemesterConfiguration(
    navController: NavController,
    id: String
) {
    navController.navigate(semesterRoute(id))
}

internal fun NavGraphBuilder.semesterRoutes(
    semesterViewModel: SemesterViewModel,
    navController: NavController,
    studyProgramsViewModel: StudyProgramsViewModel
) {
    composable(
        route = MakRoutes.Semester,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val editing = TrackPlanEditing(
            active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
            isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
        )
        val semesterId = entry.arguments?.getString("semesterId")
        var opened by rememberSaveable(entry.id) { mutableStateOf(false) }
        LaunchedEffect(semesterId, opened) {
            if (!opened && semesterId != null) {
                semesterViewModel.open(semesterId)
                opened = true
            }
        }
        SemesterScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onSemesterNameChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(name = value)) } },
            onSemesterStartDateChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) } },
            onSemesterEndDateChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) } },
            onSemesterFirstWeekChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value -> semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) } },
            onSaveSemester = semesterViewModel::saveSemester,
            onOpenCourses = { semesterId?.let { navController.navigate(semesterCoursesRoute(it)) } },
            onOpenOverrides = { semesterId?.let { navController.navigate(semesterOverridesRoute(it)) } },
            onOpenCalendars = { semesterId?.let { navController.navigate(semesterCalendarsRoute(it)) } },
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterCourses,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val editing = TrackPlanEditing(
            active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
            isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
        )
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterCoursesScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onAddCourse = {
                editing.run(
                    refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                    holdIfInactive = true
                ) {
                    semesterId?.let {
                    semesterViewModel.resetCourseDraft()
                    navController.navigate(semesterCourseAddRoute(it))
                    }
                }
            },
            onEditCourse = { assignmentId ->
                editing.run(
                    refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                    holdIfInactive = true
                ) {
                    semesterId?.let {
                    // Explicit entry starts from the saved values, so an abandoned draft does not return.
                    semesterViewModel.semester.value.courseItems
                        .firstOrNull { item -> item.assignmentId == assignmentId }
                        ?.programId
                        ?.toLongOrNull()
                        ?.let(studyProgramsViewModel::openEdit)
                    navController.navigate(semesterCourseEditRoute(it, assignmentId))
                    }
                }
            },
            onDeleteCourse = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::requestCourseDeletion
            ),
            onConfirmCourseDeletion = semesterViewModel::confirmCourseDeletion,
            onCancelCourseDeletion = semesterViewModel::cancelCourseDeletion,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterCourseAdd,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val editing = TrackPlanEditing()
        if (!editing.ready) {
            PlanEditingWait()
            return@composable
        }
        val semesterId = entry.arguments?.getString("semesterId")
        var opened by rememberSaveable(entry.id) { mutableStateOf(false) }
        var openSucceeded by rememberSaveable(entry.id) { mutableStateOf(false) }
        LaunchedEffect(editing.ready, semesterId, opened) {
            if (!opened) {
                openSucceeded = semesterId != null && semesterViewModel.refreshForEdit(semesterId)
                opened = true
            }
        }
        if (!opened || !openSucceeded) {
            LaunchedEffect(opened, openSucceeded) {
                if (opened && !openSucceeded) navController.popBackStack()
            }
            PlanEditingWait()
            return@composable
        }
        SemesterCourseAddScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCourseNameChanged = semesterViewModel::updateCourseName,
            onCourseColorChanged = semesterViewModel::updateCourseColor,
            onProgramModeChanged = semesterViewModel::setCourseProgramMode,
            onSelectProgram = semesterViewModel::selectCourseProgram,
            onCourseModeChanged = semesterViewModel::setCourseCalendarMode,
            onCourseCalendarChanged = semesterViewModel::selectCourseCalendar,
            onAddCourse = semesterViewModel::addCourse,
            onCancel = {
                semesterViewModel.resetCourseDraft()
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterCourseEdit,
        arguments = listOf(
            navArgument("semesterId") { type = NavType.StringType },
            navArgument("assignmentId") { type = NavType.StringType }
        )
    ) { entry ->
        val editing = TrackPlanEditing()
        if (!editing.ready) {
            PlanEditingWait()
            return@composable
        }
        val semesterId = entry.arguments?.getString("semesterId")
        val assignmentId = entry.arguments?.getString("assignmentId")
        var opened by rememberSaveable(entry.id) { mutableStateOf(false) }
        var openSucceeded by rememberSaveable(entry.id) { mutableStateOf(false) }
        LaunchedEffect(editing.ready, semesterId, assignmentId, opened) {
            if (!opened) {
                val semesterLoaded = semesterId != null && semesterViewModel.refreshForEdit(semesterId)
                val programId = semesterViewModel.semester.value.courseItems
                    .firstOrNull { it.assignmentId == assignmentId }
                    ?.programId
                    ?.toLongOrNull()
                val programLoaded = programId != null && studyProgramsViewModel.openEditFromRoom(programId)
                openSucceeded = semesterLoaded && programLoaded
                opened = true
            }
        }
        if (!opened || !openSucceeded) {
            LaunchedEffect(opened, openSucceeded) {
                if (opened && !openSucceeded) navController.popBackStack()
            }
            PlanEditingWait()
            return@composable
        }
        val semesterState = semesterViewModel.semester.collectAsStateWithLifecycle().value
        val programId = semesterState.courseItems
            .firstOrNull { it.assignmentId == assignmentId }
            ?.programId
            ?.toLongOrNull()
        val programsState = studyProgramsViewModel.programs.collectAsStateWithLifecycle().value
        val editor = if (programId != null && programsState.editor.id == programId) {
            programsState.editor
        } else {
            StudyProgramEditorUi()
        }
        SemesterCourseEditScreen(
            state = semesterState,
            editor = editor,
            onNameChanged = studyProgramsViewModel::updateName,
            onColorChanged = studyProgramsViewModel::updateColor,
            onSave = studyProgramsViewModel::save,
            onCancel = {
                studyProgramsViewModel.closeEditor()
                navController.popBackStack()
            },
            onSeparateCourse = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::separateCourseCalendar
            ),
            onRequestReconnect = { assignmentId, calendarId ->
                editing.run(
                    refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                    holdIfInactive = true
                ) {
                    semesterViewModel.requestReconnect(assignmentId, calendarId)
                }
            },
            onConfirmReconnect = semesterViewModel::confirmReconnect,
            onCancelReconnect = semesterViewModel::cancelReconnect,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterOverrides,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val editing = TrackPlanEditing(
            active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
            isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
        )
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterWeekOverridesScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCalendarSelected = semesterViewModel::selectCalendar,
            onWeekStartDateChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::updateWeekStartDate
            ),
            onWeekTypeChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value ->
                semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekType = value)) }
            },
            onScopeChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value ->
                semesterViewModel.update { it.copy(overrideForm = it.overrideForm.copy(scope = value)) }
            },
            onNewOverride = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::newWeekOverride
            ),
            onEditOverride = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::editWeekOverride
            ),
            onSaveOverride = semesterViewModel::saveWeekOverride,
            onDeleteOverride = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::requestWeekOverrideDeletion
            ),
            onConfirmOverrideDeletion = semesterViewModel::confirmWeekOverrideDeletion,
            onCancelOverrideDeletion = semesterViewModel::cancelWeekOverrideDeletion,
            onCancelOverrideEdit = semesterViewModel::cancelWeekOverrideEdit,
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.SemesterCalendars,
        arguments = listOf(navArgument("semesterId") { type = NavType.StringType })
    ) { entry ->
        val editing = TrackPlanEditing(
            active = semesterViewModel.semester.collectAsStateWithLifecycle().value.hasPlanDraft(),
            isActiveNow = { semesterViewModel.semester.value.hasPlanDraft() }
        )
        val semesterId = entry.arguments?.getString("semesterId")
        LaunchedEffect(semesterId) {
            semesterId?.let(semesterViewModel::openIfNeeded)
        }
        SemesterCalendarsScreen(
            state = semesterViewModel.semester.collectAsStateWithLifecycle().value,
            onCalendarSelected = semesterViewModel::selectCalendar,
            onStartDateChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(startDate = value)) }
            },
            onEndDateChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(endDate = value)) }
            },
            onFirstWeekChanged = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false }
            ) { value ->
                semesterViewModel.update { it.copy(semester = it.semester.copy(firstWeek = value)) }
            },
            onSaveCalendar = semesterViewModel::saveCalendar,
            onDeleteCalendar = editing.callback(
                refreshBeforeFirstEdit = { if (semesterId != null) semesterViewModel.refreshForEdit(semesterId) else false },
                action = semesterViewModel::requestCalendarDeletion
            ),
            onConfirmCalendarDeletion = semesterViewModel::confirmCalendarDeletion,
            onCancelCalendarDeletion = semesterViewModel::cancelCalendarDeletion,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun SemesterEffects(
    semesterViewModel: SemesterViewModel,
    navController: NavController
) {
    LaunchedEffect(semesterViewModel, navController) {
        semesterViewModel.effects.collect { effect ->
            when (effect) {
                SemesterEffect.CloseConfiguration -> {
                    if (shouldCloseSemesterConfiguration(navController.currentBackStackEntry?.destination?.route)) {
                        navController.popBackStack()
                    }
                }
                SemesterEffect.CourseAdded -> {
                    if (navController.currentBackStackEntry?.destination?.route == MakRoutes.SemesterCourseAdd) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
