package dev.retza.mak.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.retza.mak.ui.settings.SettingsScreen
import dev.retza.mak.ui.settings.SettingsViewModel

internal fun openSettings(navController: NavController) {
    navController.navigate(MakRoutes.Settings)
}

internal fun NavGraphBuilder.settingsRoute(
    settingsViewModel: SettingsViewModel,
    navController: NavController,
    onAddSemester: () -> Unit,
    onExport: () -> Unit
) {
    composable(MakRoutes.Settings) {
        val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
        SettingsScreen(
            state = settingsState,
            onSemesterSelected = settingsViewModel::selectSemester,
            onAddSemester = onAddSemester,
            onConfigureSemester = { id -> openSemesterConfiguration(navController, id) },
            onDeleteSemester = settingsViewModel::requestSemesterDeletion,
            onConfirmDelete = settingsViewModel::confirmSemesterDeletion,
            onCancelDelete = settingsViewModel::cancelSemesterDeletion,
            onThemeSelected = settingsViewModel::selectTheme,
            onExport = onExport,
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
