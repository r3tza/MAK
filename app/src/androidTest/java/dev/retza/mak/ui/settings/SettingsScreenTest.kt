package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        firstWeekLabel = "pierwszy tydzień A",
        courseCountLabel = "2 kierunki",
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
                            gapThresholdOptions = listOf(GapThresholdOptionUi("30", "30 min", true)),
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                eveningHourOptions = listOf(NotificationOptionUi("20:00", "20:00", true)),
                                leadOptions = listOf(NotificationOptionUi("30", "30 min", true))
                            )
                        ),
                        onOpenSemesters = { opened = "semesters" },
                        onOpenPrograms = { opened = "programs" },
                        onOpenNotifications = { opened = "notifications" },
                        onOpenData = { opened = "data" },
                        onOpenAbout = { opened = "about" },
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

        composeTestRule.onNodeWithText("Semestry i wygląd").assertDoesNotExist()
        composeTestRule.onNodeWithText("USTAWIENIA").assertDoesNotExist()
        composeTestRule.onNodeWithText("Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wygląd").assertIsDisplayed()
        composeTestRule.onNodeWithText("Powiadomienia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dane").assertIsDisplayed()
        composeTestRule.onNodeWithText("O aplikacji").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wersja").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Włączone").assertIsDisplayed()
        composeTestRule.onNodeWithText("20:00 dzień wcześniej, 30 min przed zajęciami")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Zarządzaj semestrami").performScrollTo().performClick()
        assertEquals("semesters", opened)
        composeTestRule.onNodeWithText("Powiadomienia o kolizjach").performScrollTo().performClick()
        assertEquals("notifications", opened)
        composeTestRule.onNodeWithText("Kopia zapasowa i import").performScrollTo().performClick()
        assertEquals("data", opened)
        composeTestRule.onNodeWithText("Wersja").performScrollTo().performClick()
        assertEquals("about", opened)
    }

    @Test
    fun updatesSectionChecksTogglesAndOpensPendingReleaseAt320Dp() {
        var checked = false
        var openedUpdate = false
        var automatic: Boolean? = null
        showUpdates(
            UpdateSettingsUi(
                installedVersion = "0.1.0",
                checkSummary = "Dostępna wersja 0.2.0",
                pendingVersion = "0.2.0"
            ),
            onCheck = { checked = true },
            onOpenUpdate = { openedUpdate = true },
            onAutomatic = { automatic = it }
        )

        composeTestRule.onNodeWithText("Aktualizacje").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprawdź aktualizacje").performScrollTo().performClick()
        assertTrue(checked)
        composeTestRule.onNodeWithText("Aktualizacja do 0.2.0").performScrollTo().performClick()
        assertTrue(openedUpdate)
        composeTestRule.onNodeWithText("Nowe wersje nie pojawią się same.").assertExists()
        composeTestRule.onNodeWithText("Sprawdzaj przy uruchomieniu").performScrollTo().performClick()
        assertEquals(true, automatic)
    }

    @Test
    fun enabledAutomaticChecksNameOnlyTheInterval() {
        showUpdates(UpdateSettingsUi(installedVersion = "0.1.0", automaticChecks = true))

        composeTestRule.onNodeWithText("Sprawdzaj przy uruchomieniu").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprawdzanie raz dziennie.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nowe wersje nie pojawią się same.").assertDoesNotExist()
        composeTestRule.onNodeWithText("GitHub", substring = true).assertDoesNotExist()
    }

    @Test
    fun updatesSectionDisablesCheckAndHidesPendingRowWhileChecking() {
        showUpdates(UpdateSettingsUi(installedVersion = "0.1.0", checkSummary = "Sprawdzanie...", canCheck = false))

        composeTestRule.onNodeWithText("Sprawdzanie...").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprawdź aktualizacje")
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Aktualizacja do", substring = true).assertDoesNotExist()
    }

    private fun showUpdates(
        updates: UpdateSettingsUi,
        onCheck: () -> Unit = {},
        onOpenUpdate: () -> Unit = {},
        onAutomatic: (Boolean) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester),
                            activeSemesterId = "1",
                            themeOptions = listOf(ThemeOptionUi("system", "Systemowy", true)),
                            gapThresholdOptions = listOf(GapThresholdOptionUi("30", "30 min", true))
                        ),
                        onOpenSemesters = {},
                        onOpenPrograms = {},
                        onOpenNotifications = {},
                        onOpenData = {},
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        notificationsBlocked = false,
                        onRetry = {},
                        updates = updates,
                        onCheckUpdates = onCheck,
                        onOpenUpdate = onOpenUpdate,
                        onAutomaticChecksChanged = onAutomatic
                    )
                }
            }
        }
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
                        onOpenPrograms = {},
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
    fun activeSemesterFieldSelectsRepeatedNameByIdAt320Dp() {
        var selected = ""
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsSemestersScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester, semester.copy(id = "2", isActive = false)),
                            activeSemesterId = "1"
                        ),
                        onSemesterSelected = { selected = it },
                        onAddSemester = {},
                        onConfigureSemester = {},
                        onDeleteSemester = {},
                        onConfirmDelete = {},
                        onCancelDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Semestr zimowy (1)").performClick()
        composeTestRule.onNodeWithText("Semestr zimowy (2)").performClick()

        assertEquals("2", selected)
    }

    @Test
    fun semestersScreenKeepsEmptyStateAndAddActionTogether() {
        var added = false
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsSemestersScreen(
                        state = SettingsUiState(),
                        onSemesterSelected = {},
                        onAddSemester = { added = true },
                        onConfigureSemester = {},
                        onDeleteSemester = {},
                        onConfirmDelete = {},
                        onCancelDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dodaj semestr, aby rozpocząć pracę z planem.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj semestr").assertIsDisplayed().performClick()
        assertTrue(added)
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

        // The name is shown both in the active semester field and in the semester card.
        composeTestRule.onAllNodesWithText("Semestr zimowy").assertCountEquals(2)
        composeTestRule.onNodeWithText("Aktywny").assertIsDisplayed()
        composeTestRule.onNodeWithText("Konfiguruj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usuń").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lista semestrów").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj semestr").assertIsDisplayed()
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
                        notificationsBlocked = true,
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

        composeTestRule.onNodeWithText("Kolizje w planie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dzień wcześniej").assertIsDisplayed()
        composeTestRule.onNodeWithText("Przed zajęciami").assertIsDisplayed()
        composeTestRule.onNodeWithText("Godzina wieczorna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wyprzedzenie przed zajęciami").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zablokowane przez system").assertIsDisplayed()
        composeTestRule.onNodeWithText("Otwórz ustawienia aplikacji").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Android może opóźnić powiadomienie o kilkanaście minut, aby oszczędzać baterię."
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Powiadomienia o kolizjach").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Włączone").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Wyłączone").assertCountEquals(0)
    }

    @Test
    fun notificationsScreenHidesSettingsOfDisabledKind() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                eveningEnabled = false,
                                beforeClassEnabled = true,
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

        composeTestRule.onNodeWithText("Powiadomienie wieczorne").assertIsDisplayed()
        composeTestRule.onNodeWithText("Godzina wieczorna").assertDoesNotExist()
        composeTestRule.onNodeWithText("Wyprzedzenie przed zajęciami").assertIsDisplayed()
    }

    @Test
    fun enablingBlockedNotificationsRequestsSystemPermission() {
        var permissionRequested = false
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(notifications = NotificationSettingsUi(enabled = false)),
                        notificationsBlocked = true,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onRequestNotificationPermission = { permissionRequested = true },
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Powiadomienia o kolizjach").performClick()
        assertTrue(permissionRequested)
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
        composeTestRule.onNodeWithText(
            "Import zastępuje wszystkie lokalne dane. Tej operacji nie można cofnąć."
        ).assertIsDisplayed()
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
