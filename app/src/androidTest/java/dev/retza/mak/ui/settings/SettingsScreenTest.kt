package dev.retza.mak.ui.settings

import dev.retza.mak.settings.ThemeMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.export.ImportSummary
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
        id = 1L,
        name = "Semestr zimowy",
        dateLabel = "1 paź - 28 lut 2027, pierwszy tydzień A",
        programsLabel = "Informatyka, Zarządzanie",
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
                            activeSemesterId = 1L,
                            theme = ThemeMode.System,
                            gapThresholdOptions = listOf(SettingsOptionUi("30", "30 min", true)),
                            minimumBreakOptions = listOf(SettingsOptionUi("0", "0 min", true)),
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(SettingsOptionUi("21:00", "21:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("15", "15 min", true))
                                )
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
                        onMinimumBreakSelected = {},
                        notificationsBlocked = false,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Semestry i wygląd").assertDoesNotExist()
        composeTestRule.onNodeWithText("USTAWIENIA").assertDoesNotExist()
        composeTestRule.onNodeWithText("Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wygląd").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Powiadomienia").assertCountEquals(2)
        composeTestRule.onNodeWithText("Dane").assertIsDisplayed()
        composeTestRule.onNodeWithText("O aplikacji").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wersja").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wszystkie włączone").assertIsDisplayed()

        composeTestRule.onNodeWithText("Minimalna przerwa").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Zajęcia z krótszą lub równą przerwą są kolizją.").performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Zarządzaj semestrami").performScrollTo().performClick()
        assertEquals("semesters", opened)
        composeTestRule.onNodeWithText("Wszystkie włączone").performScrollTo().performClick()
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
                            activeSemesterId = 1L,
                            theme = ThemeMode.System,
                            gapThresholdOptions = listOf(SettingsOptionUi("30", "30 min", true))
                        ),
                        onOpenSemesters = {},
                        onOpenPrograms = {},
                        onOpenNotifications = {},
                        onOpenData = {},
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        onMinimumBreakSelected = {},
                        notificationsBlocked = false,
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
                            gapThresholdOptions = listOf(SettingsOptionUi("30", "30 min", true))
                        ),
                        onOpenSemesters = {},
                        onOpenPrograms = {},
                        onOpenNotifications = {},
                        onOpenData = {},
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        onMinimumBreakSelected = {},
                        notificationsBlocked = false,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dodaj semestr, aby rozpocząć pracę z planem.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj semestr").assertIsDisplayed()
    }

    @Test
    fun activeSemesterFieldSelectsRepeatedNameByIdAt320Dp() {
        var selected = 0L
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsSemestersScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester, semester.copy(id = 2L, isActive = false)),
                            activeSemesterId = 1L
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

        assertEquals(2L, selected)
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
                            activeSemesterId = 1L
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
    fun semesterCardShowsIconRowsAt320Dp() {
        val longPrograms = "Informatyka, Zarządzanie, Ekonomia, Socjologia, Filologia angielska, " +
            "Pedagogika opiekuńczo-wychowawcza, Gospodarka przestrzenna"
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SettingsSemestersScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester.copy(programsLabel = longPrograms)),
                            activeSemesterId = 1L
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

        composeTestRule.onNodeWithText("1 paź - 28 lut 2027, pierwszy tydzień A").assertIsDisplayed()
        composeTestRule.onNodeWithText(longPrograms).assertIsDisplayed()
        composeTestRule.onNodeWithText("5 zajęć").assertIsDisplayed()
    }

    @Test
    fun notificationsScreenGroupsKindsByTopicAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(SettingsOptionUi("21:00", "21:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("15", "15 min", true))
                                )
                            )
                        ),
                        notificationsBlocked = true,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wszystkie powiadomienia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolizje").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Dzień wcześniej").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Przed zajęciami").assertCountEquals(2)
        composeTestRule.onNodeWithText("O 20:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min wcześniej").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Godzina wieczorna").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Wyprzedzenie przed zajęciami").assertCountEquals(0)
        composeTestRule.onNodeWithText("Zablokowane przez system").assertIsDisplayed()
        composeTestRule.onNodeWithText("Otwórz ustawienia aplikacji").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Android może opóźnić powiadomienie o kilkanaście minut, aby oszczędzać baterię."
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Włączone").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Wyłączone").assertCountEquals(0)
    }

    @Test
    fun notificationSummaryCountsEnabledKinds() {
        val notifications = mutableStateOf(
            NotificationSettingsUi(
                enabled = true,
                collisions = NotificationTopicUi(
                    eveningEnabled = true,
                    beforeClassEnabled = true,
                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                ),
                gaps = NotificationTopicUi(eveningEnabled = true, beforeClassEnabled = true)
            )
        )
        val blocked = mutableStateOf(false)

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsScreen(
                        state = SettingsUiState(
                            semesters = listOf(semester),
                            activeSemesterId = 1L,
                            theme = ThemeMode.System,
                            gapThresholdOptions = listOf(SettingsOptionUi("30", "30 min", true)),
                            minimumBreakOptions = listOf(SettingsOptionUi("0", "0 min", true)),
                            notifications = notifications.value
                        ),
                        onOpenSemesters = {},
                        onOpenPrograms = {},
                        onOpenNotifications = {},
                        onOpenData = {},
                        onOpenAbout = {},
                        onSemesterSelected = {},
                        onAddSemester = {},
                        onThemeSelected = {},
                        onGapThresholdSelected = {},
                        onMinimumBreakSelected = {},
                        notificationsBlocked = blocked.value,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wszystkie włączone").assertIsDisplayed()

        composeTestRule.runOnIdle {
            notifications.value = notifications.value.copy(
                collisions = notifications.value.collisions.copy(beforeClassEnabled = false),
                gaps = notifications.value.gaps.copy(beforeClassEnabled = false, eveningEnabled = true)
            )
            blocked.value = true
        }
        composeTestRule.onNodeWithText("Włączone: 2 z 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zablokowane przez system").assertIsDisplayed()

        composeTestRule.runOnIdle {
            notifications.value = notifications.value.copy(gaps = notifications.value.gaps.copy(eveningEnabled = false))
        }
        composeTestRule.onNodeWithText("Włączone: 1 z 4").assertIsDisplayed()

        composeTestRule.runOnIdle {
            notifications.value = notifications.value.copy(enabled = false, collisions = notifications.value.collisions.copy(eveningEnabled = false))
            blocked.value = false
        }
        composeTestRule.onNodeWithText("Wyłączone").assertIsDisplayed()
    }

    @Test
    fun notificationKindsAreVisibleWhenAllAreOffAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = false,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(SettingsOptionUi("21:00", "21:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("15", "15 min", true))
                                )
                            )
                        ),
                        notificationsBlocked = false,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("Dzień wcześniej").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Przed zajęciami").assertCountEquals(2)
    }

    @Test
    fun notificationsScreenKeepsValuesOfDisabledKind() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(SettingsOptionUi("21:00", "21:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("15", "15 min", true))
                                )
                            )
                        ),
                        notificationsBlocked = false,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("Dzień wcześniej").assertCountEquals(2)
        composeTestRule.onNodeWithText("O 20:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min wcześniej").assertIsDisplayed()
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
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = { permissionRequested = true },
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wszystkie powiadomienia").performClick()
        assertTrue(permissionRequested)
    }

    @Test
    fun kindValueOpensChoiceDialogAndSaves() {
        val savedLeads = mutableListOf<String>()
        setNotificationsContent(onBeforeClassLeadSelected = { savedLeads.add(it) })

        composeTestRule.onNodeWithText("30 min wcześniej").performClick()
        composeTestRule.onNodeWithText("Kolizje: przed zajęciami").assertIsDisplayed()
        composeTestRule.onNodeWithText("45 min wcześniej").performClick()
        composeTestRule.onNodeWithText("Zapisz").performClick()
        assertEquals(listOf("45"), savedLeads)

        composeTestRule.onNodeWithText("30 min wcześniej").performClick()
        composeTestRule.onNodeWithText("60 min wcześniej").performClick()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        assertEquals(listOf("45"), savedLeads)
    }

    @Test
    fun kindValueAndSwitchAreSeparateTargets() {
        val eveningChanges = mutableListOf<Boolean>()
        setNotificationsContent(onEveningNotificationsEnabled = { eveningChanges.add(it) })

        composeTestRule.onNodeWithText("O 20:00").performClick()
        assertTrue(eveningChanges.isEmpty())
        composeTestRule.onNodeWithText("Kolizje: dzień wcześniej").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()

        composeTestRule.onNodeWithContentDescription("Kolizje: Dzień wcześniej").performClick()
        assertEquals(listOf(false), eveningChanges)
        composeTestRule.onAllNodesWithText("Kolizje: dzień wcześniej").assertCountEquals(0)
    }

    @Test
    fun gapSectionShowsBothKindsAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(SettingsOptionUi("20:00", "20:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("30", "30 min", true))
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(SettingsOptionUi("21:00", "21:00", true)),
                                    leadOptions = listOf(SettingsOptionUi("15", "15 min", true))
                                )
                            )
                        ),
                        notificationsBlocked = false,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = {},
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = {},
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Okienka").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Okienka: Dzień wcześniej").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Okienka: Przed zajęciami").assertIsDisplayed()
        composeTestRule.onNodeWithText("O 21:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 min wcześniej").assertIsDisplayed()
    }

    private fun setNotificationsContent(
        onEveningNotificationsEnabled: (Boolean) -> Unit = {},
        onBeforeClassLeadSelected: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SettingsNotificationsScreen(
                        state = SettingsUiState(
                            notifications = NotificationSettingsUi(
                                enabled = true,
                                collisions = NotificationTopicUi(
                                    eveningEnabled = true,
                                    beforeClassEnabled = true,
                                    eveningHourOptions = listOf(
                                        SettingsOptionUi("18:00", "18:00"),
                                        SettingsOptionUi("19:00", "19:00"),
                                        SettingsOptionUi("20:00", "20:00", true),
                                        SettingsOptionUi("21:00", "21:00"),
                                        SettingsOptionUi("22:00", "22:00")
                                    ),
                                    leadOptions = listOf(15, 30, 45, 60).map { minutes ->
                                        SettingsOptionUi("$minutes", "$minutes min", minutes == 30)
                                    }
                                ),
                                gaps = NotificationTopicUi(
                                    eveningEnabled = false,
                                    beforeClassEnabled = false,
                                    eveningHourOptions = listOf(
                                        SettingsOptionUi("18:00", "18:00"),
                                        SettingsOptionUi("19:00", "19:00"),
                                        SettingsOptionUi("20:00", "20:00"),
                                        SettingsOptionUi("21:00", "21:00", true),
                                        SettingsOptionUi("22:00", "22:00")
                                    ),
                                    leadOptions = listOf(15, 30, 45, 60).map { minutes ->
                                        SettingsOptionUi("$minutes", "$minutes min", minutes == 15)
                                    }
                                )
                            )
                        ),
                        notificationsBlocked = false,
                        onNotificationsEnabled = {},
                        onEveningNotificationsEnabled = onEveningNotificationsEnabled,
                        onBeforeClassNotificationsEnabled = {},
                        onEveningHourSelected = {},
                        onBeforeClassLeadSelected = onBeforeClassLeadSelected,
                        onGapEveningNotificationsEnabled = {},
                        onGapBeforeClassNotificationsEnabled = {},
                        onGapEveningHourSelected = {},
                        onGapLeadSelected = {},
                        onRequestNotificationPermission = {},
                        onOpenAppSettings = {}
                    )
                }
            }
        }
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
                            importPreview = ImportSummary(
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
