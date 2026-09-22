package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val semester = SemesterUi(
        id = "1",
        name = "Semestr zimowy",
        dateRangeLabel = "1 paź - 28 lut",
        firstWeekLabel = "Pierwszy tydzień A",
        courseCountLabel = "2 kierunków",
        classCountLabel = "5 zajęć",
        isActive = true
    )

    @Test
    fun settingsScreenShowsSectionsAndOpensSubscreensAt320Dp() {
        var opened = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester),
                            activeSemesterId = "1",
                            themeOptions = listOf(ThemeOptionUi("system", "Systemowy", true)),
                            gapThresholdOptions = listOf(GapThresholdOptionUi("30", "30 min", true))
                        ),
                        onOpenSemesters = { opened = "semesters" },
                        onOpenNotifications = { opened = "notifications" },
                        onOpenData = { opened = "data" },
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        notificationsBlocked = false,
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wygląd").assertIsDisplayed()
        composeTestRule.onNodeWithText("Powiadomienia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dane").assertIsDisplayed()
        composeTestRule.onNodeWithText("O aplikacji").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wersja").assertIsDisplayed()

        composeTestRule.onNodeWithText("Zarządzaj semestrami").performClick()
        assertEquals("semesters", opened)
        composeTestRule.onNodeWithText("Powiadomienia o kolizjach").performClick()
        assertEquals("notifications", opened)
        composeTestRule.onNodeWithText("Kopia zapasowa i import").performClick()
        assertEquals("data", opened)
    }

    @Test
    fun settingsScreenWithoutSemesterOffersSetupAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsScreen(
                        state = SettingsUiState(
                            gapThresholdOptions = listOf(GapThresholdOptionUi("30", "30 min", true))
                        ),
                        onOpenSemesters = {},
                        onOpenNotifications = {},
                        onOpenData = {},
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        notificationsBlocked = false,
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dodaj semestr, aby rozpocząć pracę z planem.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj semestr").assertIsDisplayed()
    }

    @Test
    fun semestersScreenShowsManageActionsAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsSemestersScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester),
                            activeSemesterId = "1"
                        ),
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onConfigureSemester = {},
                        onDeleteSemester = {},
                        onConfirmDelete = {},
                        onCancelDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Semestr zimowy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aktywny").assertIsDisplayed()
        composeTestRule.onNodeWithText("Konfiguruj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usuń").assertIsDisplayed()
    }

    @Test
    fun notificationsScreenShowsTogglesAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                eveningHourOptions = listOf(NotificationOptionUi("20:00", "20:00", true)),
                                leadOptions = listOf(NotificationOptionUi("30", "30 min", true))
                            )
                        ),
                        notificationsBlocked = false,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Godzina wieczorna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wyprzedzenie przed zajęciami").assertIsDisplayed()
    }

    @Test
    fun dataScreenOffersExportAndImportAt320Dp() {
        var imported = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    SettingsDataScreen(
                        state = SettingsUiState(),
                        onExport = {},
                        onImport = { imported += 1 },
                        onDismissImportError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Eksportuj plan do JSON").assertIsDisplayed()
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
