package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.retza.mak.ui.programs.StudyProgramEditScreen
import dev.retza.mak.ui.programs.StudyProgramsEffect
import dev.retza.mak.ui.programs.StudyProgramsScreen
import dev.retza.mak.ui.programs.StudyProgramsViewModel

internal fun NavGraphBuilder.studyProgramRoutes(
    studyProgramsViewModel: StudyProgramsViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    composable(MakRoutes.StudyPrograms) {
        val state by studyProgramsViewModel.programs.collectAsStateWithLifecycle()
        StudyProgramsScreen(
            state = state,
            onOpenProgram = { id -> navController.navigate(studyProgramEditRoute(id)) },
            modifier = Modifier.fillMaxSize()
        )
    }

    composable(
        route = MakRoutes.StudyProgramEdit,
        arguments = listOf(navArgument("programId") { type = NavType.LongType })
    ) { entry ->
        val programId = entry.arguments?.getLong("programId")
        LaunchedEffect(programId) {
            programId?.let(studyProgramsViewModel::openEditIfNeeded)
        }
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
            modifier = Modifier.fillMaxSize()
        )
    }
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
                    if (route == MakRoutes.StudyProgramEdit || route == MakRoutes.SemesterCourseEdit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
