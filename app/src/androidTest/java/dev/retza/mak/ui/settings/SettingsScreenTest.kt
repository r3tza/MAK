package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreenOffersImportAt320Dp() {
        var imported = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsScreen(
                        state = SettingsUiState(),
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onConfigureSemester = {},
                        onDeleteSemester = {},
                        onConfirmDelete = {},
                        onCancelDelete = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        onExport = {},
                        onImport = { imported += 1 },
                        onDismissImportError = {},
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        notificationsBlocked = false,
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("dane").performClick()
        composeTestRule.onNodeWithText("Importuj plan z JSON").assertIsDisplayed().performClick()
        assertEquals(1, imported)
    }

    @Test
    fun importPreviewShowsCountsAndWarningAt320Dp() {
        var confirmed = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    ImportPreviewScreen(
                        state = SettingsUiState(
                            importPreview = ImportPreviewUi(
                                semesterCount = 2,
                                programCount = 3,
                                classCount = 10,
                                overrideCount = 4,
                                noteCount = 1,
                                changeCount = 2,
                                activeSemesterName = "Semestr zimowy"
                            )
                        ),
                        onConfirm = { confirmed += 1 },
                        onCancel = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Zastąpisz wszystkie lokalne dane").assertIsDisplayed()
        composeTestRule.onNodeWithText("Semestry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zajęcia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aktywny semestr: Semestr zimowy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zastąp dane").assertIsDisplayed().performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun importPreviewShowsEmptyStateWithoutSnapshot() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    ImportPreviewScreen(
                        state = SettingsUiState(),
                        onConfirm = {},
                        onCancel = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Nie wybrano poprawnej kopii do importu.").assertIsDisplayed()
    }
}
