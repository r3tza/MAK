package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.retza.mak.ui.programs.StudyProgramEditOpenState
import dev.retza.mak.ui.programs.StudyProgramEditScreen
import dev.retza.mak.ui.programs.StudyProgramsEffect
import dev.retza.mak.ui.programs.StudyProgramsScreen
import dev.retza.mak.ui.programs.StudyProgramsViewModel

internal fun NavGraphBuilder.studyProgramRoutes(
    navController: NavController,
    onBack: () -> Unit
) {
    composable(MakRoutes.StudyPrograms) {
        val studyProgramsViewModel: StudyProgramsViewModel = koinViewModel()
        val state by studyProgramsViewModel.programs.collectAsStateWithLifecycle()
        StudyProgramsScreen(
            state = state,
            onOpenProgram = { id ->
                navController.navigate(studyProgramEditRoute(id))
            },
            onVisibleChange = studyProgramsViewModel::setVisible,
            onAddProgram = { navController.navigate(MakRoutes.StudyProgramAdd) },
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.StudyProgramEdit,
        arguments = listOf(navArgument("programId") { type = NavType.LongType })
    ) { entry ->
        val programId = entry.arguments?.getLong("programId")
        StudyProgramEditDestination(
            navController = navController,
            onBack = onBack,
            openKey = programId,
            open = { viewModel -> programId?.let { viewModel.openOnce(it) } }
        )
    }

    composable(MakRoutes.StudyProgramAdd) {
        StudyProgramEditDestination(
            navController = navController,
            onBack = onBack,
            openKey = Unit,
            open = { viewModel -> viewModel.openNewOnce() }
        )
    }
}

@Composable
private fun StudyProgramEditDestination(
    navController: NavController,
    onBack: () -> Unit,
    openKey: Any?,
    open: suspend (StudyProgramsViewModel) -> Unit
) {
    val studyProgramsViewModel: StudyProgramsViewModel = koinViewModel()
    val openState by studyProgramsViewModel.opened.collectAsStateWithLifecycle()
    LaunchedEffect(openKey) {
        open(studyProgramsViewModel)
    }
    if (openState != StudyProgramEditOpenState.Open) {
        LaunchedEffect(openState) {
            if (openState == StudyProgramEditOpenState.Missing) onBack()
        }
        PlanEditingWait()
        return
    }
    StudyProgramEffects(studyProgramsViewModel, navController)
    val state by studyProgramsViewModel.programs.collectAsStateWithLifecycle()
    StudyProgramEditScreen(
        editor = state.editor,
        onNameChanged = studyProgramsViewModel::updateName,
        onColorChanged = studyProgramsViewModel::updateColor,
        onSave = studyProgramsViewModel::save,
        onCancel = {
            studyProgramsViewModel.closeEditor()
            onBack()
        },
        onRequestDelete = studyProgramsViewModel::requestDelete,
        onConfirmDelete = studyProgramsViewModel::confirmDelete,
        onCancelDelete = studyProgramsViewModel::cancelDelete,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
internal fun StudyProgramEffects(
    studyProgramsViewModel: StudyProgramsViewModel,
    navController: NavController
) {
    LaunchedEffect(studyProgramsViewModel, navController) {
        studyProgramsViewModel.effects.collect { effect ->
            when (effect) {
                StudyProgramsEffect.CloseEditor -> {
                    val route = navController.currentBackStackEntry?.destination?.route
                    if (
                        route == MakRoutes.StudyProgramEdit ||
                        route == MakRoutes.StudyProgramAdd ||
                        route == MakRoutes.SemesterCourseEdit
                    ) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
