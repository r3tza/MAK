# Plan

Porządki po przeglądzie gałęzi `task/settings-and-gap-notifications` z 2026-10-12 (powtórne użycie, uproszczenia, wydajność, poziom poprawki). Zlecenie użytkownika z 2026-10-12: wprowadzić pozycje z tabel „Warto zrobić” i „Do rozważenia”. Żaden krok nie zmienia zachowania ani tekstów widocznych dla użytkownika, chyba że opis kroku mówi inaczej. Poza zakresem (decyzja w przeglądzie): klucze i settery `SettingsPreferences` oraz `DataStoreSettingsPreferences` (zapisane dane użytkownika i wszystkie atrapy w testach), dzień wolny w „Planie” według filtru (decyzja z `FEATURES.md`), identyfikator grupy dni wolnych w bazie, wydajność siatki „Planu”.

## 1. Uprość planowanie i odbiór powiadomień (I-98)

Cel: jedna ścieżka planowania dla kolizji i okienek, jedno wyliczenie grup okienek i jedna funkcja kanału.

Pliki:
- `app/src/main/java/dev/retza/mak/domain/CollisionNotificationPlanner.kt`
- `app/src/main/java/dev/retza/mak/notifications/CollisionNotificationContract.kt`
- `app/src/main/java/dev/retza/mak/notifications/CollisionAlarmReceiver.kt`
- `app/src/test/java/dev/retza/mak/domain/CollisionNotificationPlannerTest.kt`

Zmiany:
1. W `CollisionNotificationPlanner.kt` dodaj `data class NotificationTopicSettings(val eveningEnabled: Boolean, val beforeClassEnabled: Boolean, val eveningHour: LocalTime = LocalTime.of(20, 0), val leadMinutes: Long = 30)`. `CollisionNotificationSettings` dostaje pola `collisions: NotificationTopicSettings` i `gaps: NotificationTopicSettings` (domyślnie gaps z obu `false`) zamiast ośmiu pól `evening*`, `beforeClass*`, `leadMinutes`, `gap*`; zostają `enabled`, `horizonDays`, `eveningWindowMinutes`, `beforeClassWindowMinutes`.
2. Dodaj `fun gapNotificationGroups(plan: ActivePlan, display: PlanDisplaySettings): List<CollisionNotificationGroup>`, która woła istniejące `gapNotificationGroups(plan.schedule.occurrences, findGaps(plan.schedule.occurrences, display.gapThresholdMinutes.toLong()))`.
3. W `plan()` zastąp dwa bloki jedną prywatną funkcją `planTopic(date, groups, topic, eveningKind, beforeKind, now): List<PlannedCollisionNotification>`; wywołaj ją dla kolizji (`collisionNotificationGroups(plan.collisions)`, `EVENING`, `BEFORE_CLASS`) i dla okienek (nowa funkcja z punktu 2, `GAP_EVENING`, `GAP_BEFORE_CLASS`). Grupy okienek licz tylko, gdy `settings.gaps` ma włączony któryś rodzaj.
4. `toPlannerSettings()` buduje dwa `NotificationTopicSettings` z pól `CollisionNotificationPreferences`.
5. `CollisionAlarmReceiver.showNotification`: „czy rodzaj włączony” czytaj z odpowiedniego tematu przez `toPlannerSettings()`; grupy okienek z funkcji z punktu 2. Zamiast dwóch kopii ciała `ensureCollisionChannel` i `ensureGapChannel` jedna prywatna `ensureChannel(context, id, name, description)`; publiczne funkcje zostają jako jednolinijkowe wywołania (używa ich `MakApplication`).

Testy: `CollisionNotificationPlannerTest` dostosuj do nowych pól (`settings.copy(collisions = settings.collisions.copy(eveningEnabled = false))` itp.); bez zmiany oczekiwanych wyników.

Przypadki brzegowe: tylko okienka włączone; oba tematy wyłączone przy `enabled = true`; identyfikatory alarmów (`stableId`) bez zmian.

Weryfikacja: `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.domain.CollisionNotificationPlannerTest" --tests "dev.retza.mak.notifications.*"`.

Kryterium zakończenia: testy zielone, w planerze nie ma powtórzonego bloku wieczór i przed zajęciami. Commit: `chore: share one planning path for collision and gap notifications`.

## 2. Uprość ekran powiadomień w ustawieniach (I-97)

Cel: stan i dialogi ekranu „Powiadomienia” opisane jednym typem tematu, dialog wybierany typem zamiast tekstu.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/settings/SettingsScreen.kt` (`NotificationSettingsUi`, `notificationsStatus`, `SettingsNotificationsScreen`, `NotificationTopicSection`)
- `app/src/main/java/dev/retza/mak/ui/settings/SettingsViewModel.kt` (budowa `NotificationSettingsUi`, ok. l. 416-440)
- `app/src/test/java/dev/retza/mak/ui/settings/SettingsViewModelTest.kt`
- `app/src/androidTest/java/dev/retza/mak/ui/settings/SettingsScreenTest.kt`

Zmiany:
1. `data class NotificationTopicUi(val eveningEnabled: Boolean, val beforeClassEnabled: Boolean, val eveningHourOptions: List<SettingsOptionUi> = emptyList(), val leadOptions: List<SettingsOptionUi> = emptyList())`. `NotificationSettingsUi` ma `enabled`, `collisions: NotificationTopicUi` (domyślnie oba `true`) i `gaps: NotificationTopicUi` (domyślnie oba `false`); usuń `kindStates`.
2. `notificationsStatus` liczy włączone rodzaje z czterech pól obu tematów, a liczbę wszystkich rodzajów jako stałą `NOTIFICATION_KIND_COUNT = 4` z komentarzem.
3. `enum class NotificationDialog { CollisionEvening, CollisionLead, GapEvening, GapLead }` zamiast `String?`; stan `rememberSaveable { mutableStateOf<NotificationDialog?>(null) }` (enum jest `Serializable`, więc przeżywa obrót).
4. `NotificationTopicSection` przyjmuje `title`, `topic: NotificationTopicUi`, `onEveningCheckedChange`, `onLeadCheckedChange`, `onOpenEvening`, `onOpenLead`; wartości „O {godzina}” i „{n} min wcześniej” liczy sama z `topic`.
5. Cztery wywołania `NotificationChoiceDialog` zastąp jednym, z tytułem, opcjami, etykietą i `onSave` wybranymi przez `when (dialog)`.
6. Publiczne parametry `SettingsNotificationsScreen` (callbacki) zostają bez zmian, więc `SettingsRoutes.kt` się nie zmienia.

Testy: `SettingsViewModelTest` i `SettingsScreenTest` budują i czytają nowe pola (`notifications.gaps.eveningHourOptions` itd.); oczekiwane teksty bez zmian.

Przypadki brzegowe: podsumowanie „Wyłączone”, „Włączone: 2 z 4”, „Wszystkie włączone”; obrót z otwartym dialogiem.

Weryfikacja: `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.settings.*"`, `.\gradlew.bat :app:compileDebugAndroidTestKotlin`; `SettingsScreenTest` w pełnym zestawie Android na końcu.

Kryterium zakończenia: testy zielone, w ekranie nie ma literałów `"evening"`, `"lead"`, `"gapEvening"`, `"gapLead"`. Commit: `chore: describe notification topics with one type in settings`.

## 3. Uprość zapisy w repozytorium semestrów (I-95)

Cel: jedna reguła wspólnego kalendarza, jeden upsert korekty tygodnia, wspólna walidacja i zapis dni wolnych, dodanie kierunku w jednej transakcji.

Pliki:
- nowy `app/src/main/java/dev/retza/mak/domain/SharedCalendar.kt`
- `app/src/main/java/dev/retza/mak/data/repository/SemesterRepository.kt`
- `app/src/main/java/dev/retza/mak/ui/SemesterCalendar.kt`, `ui/semester/SemesterViewModel.kt` (ok. l. 680), `ui/setup/SetupViewModel.kt` (ok. l. 227), `ui/programs/StudyProgramsViewModel.kt` (`createProgram`)
- `app/src/test/java/dev/retza/mak/ui/FakeSemesterRepository.kt`, `app/src/test/java/dev/retza/mak/data/repository/DemoDataSeederTest.kt` (atrapa), atrapa w `app/src/test/java/dev/retza/mak/ui/programs/StudyProgramsViewModelTest.kt`
- `app/src/androidTest/java/dev/retza/mak/data/RoomPersistenceTest.kt`

Zmiany:
1. `SharedCalendar.kt`: `/** The shared calendar of a semester is its calendar with the lowest id. */ fun <T> sharedCalendarOf(calendars: Collection<T>, id: (T) -> Long): T? = calendars.minByOrNull(id)`. Wszystkie pięć miejsc (`observeSharedCalendars`, `ActivePlanData.sharedCalendar`, `SemesterConfiguration.sharedCalendar`, `SetupViewModel`, `StudyProgramsViewModel`) woła tę funkcję.
2. Nowa operacja `SemesterRepository.saveStudyProgramInActiveSemester(program: StudyProgramRecord): SemesterRecord?`: w jednej transakcji znajduje aktywny semestr i jego wspólny kalendarz; jeśli oba są, zapisuje przypisanie jak `saveStudyProgramAssignment` i zwraca semestr; w przeciwnym razie zapisuje sam kierunek jak `saveStudyProgram` i zwraca `null`. `createProgram` w ViewModelu wywołuje tylko ją i wybiera komunikat po wyniku. Atrapy dostają implementację.
3. `upsertFromWeekOverride` usuń; w `saveFreeDayGroup` wywołuj `saveWeekOverride(WeekOverrideRecord(id = 0, semesterId, academicCalendarId = calendarId, weekStartDate = monday, weekType = weekAfterBreak, scope = FROM_WEEK))` (sprawdź nazwy pól `WeekOverrideRecord`). Transakcja Room jest zagnieżdżalna.
4. Prywatne `requireCalendarsOfSemester(semesterId, calendarIds)` (semestr istnieje, lista niepusta, każdy kalendarz należy do semestru) i `insertFreeDays(semesterId, calendarIds, start, end, name)`; używają ich `saveFreeDayGroup` i `addHolidays`. Komunikaty `require` zostają.

Testy: `StudyProgramsViewModelTest` sprawdza komunikaty jak dziś (atrapa z wydarzeniem `saveStudyProgramInActiveSemester`); `RoomPersistenceTest`: nowe testy `saveStudyProgramInActiveSemesterAssignsSharedCalendar` (dwa kalendarze, kierunek trafia do kalendarza o niższym id) i `saveStudyProgramInActiveSemesterWithoutSemesterSavesProgramOnly`.

Przypadki brzegowe: brak aktywnego semestru; semestr bez kalendarza; korekta tygodnia już istnieje (aktualizacja zamiast drugiego wiersza).

Weryfikacja: `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.programs.*" --tests "dev.retza.mak.ui.freedays.*" --tests "dev.retza.mak.ui.semester.*" --tests "dev.retza.mak.ui.setup.*" --tests "dev.retza.mak.data.*"`, `.\gradlew.bat :app:compileDebugAndroidTestKotlin`; `RoomPersistenceTest` w pełnym zestawie Android.

Kryterium zakończenia: `rg "minByOrNull \{ it.id \}|minBy \{ it.id \}" app/src/main` znajduje tylko `OccurrenceNoteRemap.kt`; testy zielone. Commit: `chore: share the shared calendar rule and repository writes`.

## 4. Połącz etykiety dat i dni wolnych (I-81)

Cel: jedna funkcja zakresu dat z rokiem, jedna liczba dni z oboma końcami i jedna odmiana „dzień/dni”, teksty dni wolnych w jednym pliku, „Tydzień A/B” z `WeekType.label()`.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/PlanMapping.kt`
- `app/src/main/java/dev/retza/mak/domain/FreeDayRules.kt`
- `app/src/main/java/dev/retza/mak/ui/semester/SemesterViewModel.kt` (`calendarRangeLabel`)
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDaysViewModel.kt`, `FreeDayEditorViewModel.kt`, `FreeDayGroups.kt`, `FreeDayDialog.kt`
- `app/src/main/java/dev/retza/mak/widget/WidgetPresenter.kt` (`freeDayDetail`)
- testy, które importują przeniesione funkcje

Zmiany:
1. `calendarRangeLabel` przenieś z `SemesterViewModel.kt` do `PlanMapping.kt` jako `internal fun dateRangeLabel(start, end)` („24 gru 2026 - 2 sty 2027”); używa jej `SemesterViewModel`, `FreeDaysViewModel.rangeLabel` i `intro` dialogu świąt. `dateRangeWithYearLabel` (rok raz, lista semestrów w ustawieniach) zostaje, bo ma inny, przyjęty format.
2. W `FreeDayRules.kt` `fun inclusiveDays(start: LocalDate, end: LocalDate): Int`; w `PlanMapping.kt` `internal fun daysLabel(count: Int): String` („1 dzień”, „12 dni”). Używają ich `FreeDayEditorUiState.description`, `rangeLabel`, `HolidaysDialogUi.checkedDayCount` i `confirmLabel`, `freeDaysSummary` (zbiór dat dla nakładających się zakresów zostaje).
3. `freeDayDetail` przenieś z `WidgetPresenter.kt` do `PlanMapping.kt` obok `todayNote` i `listNote`; teksty bez zmian (widget pokazuje „do {data}”, „Dzisiaj” zakres; tak opisuje `FEATURES.md`).
4. `FreeDayDialog.weekAfterBreakLabel`: dla `A` i `B` `type.label()` z `ui/components/WeekFields.kt`, dla `null` „Bez zmiany”.

Testy: istniejące testy etykiet (`FreeDaysViewModelTest`, `FreeDayEditorViewModelTest`, `FreeDayGroupsTest`, `SemesterViewModelTest`, `WidgetPresenterTest`) bez zmian oczekiwanych tekstów; jeśli test importuje przeniesioną funkcję, popraw import. Dodaj `FreeDayRulesTest.inclusiveDaysCountsBothEnds`.

Weryfikacja: `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*" --tests "dev.retza.mak.domain.*"`.

Kryterium zakończenia: testy zielone, `rg "\"dzień\", \"dni\", \"dni\"" app/src/main` znajduje tylko `daysLabel`. Commit: `chore: share date range and day count labels`.

## 5. Połącz kontrolki formularza i usuń martwy warunek (I-82)

Cel: wspólny zakres dat w dwóch pickerach, wiersz święta jako `MakCheckbox`, brak martwego warunku w edytorze dni wolnych.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/components/MakFormControls.kt`
- `app/src/main/java/dev/retza/mak/ui/components/MakComponents.kt` (`MakCheckbox` z etykietą, ok. l. 1274)
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDaysScreen.kt` (`HolidayOptionRow`)
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDayEditorViewModel.kt` (`weekAfterBreakDescription`)

Zmiany:
1. `MakFormControls.kt`: prywatne `@Composable rememberSelectableDates(minDate: LocalDate?, maxDate: LocalDate?): SelectableDates` z obecnym ciałem; oba miejsca (ok. l. 106 i 213) je wołają.
2. `MakCheckbox(label, checked, onCheckedChange, modifier)` dostaje `enabled: Boolean = true` i `supportingText: String? = null`. Wiersz używa `toggleable(value = checked, enabled = enabled, role = Role.Checkbox)` z `semantics(mergeDescendants = true) { stateDescription = ... }`, przy `enabled = false` krycie 0,38; przy `supportingText` etykieta i tekst w `Column`, tekst w `onSurfaceVariant`. Bez `supportingText` wygląd jak dziś.
3. `HolidayOptionRow` usuń; `HolidaysDialog` woła `MakCheckbox(label = option.name, supportingText = option.detail, checked = option.checked, enabled = option.enabled && !dialog.isSaving, onCheckedChange = { onToggle(option.date) })`.
4. `weekAfterBreakDescription`: usuń linię `if (!showsWeekAfterBreak) return null` (po decyzji z 2026-10-12 oznacza tylko „obie daty wybrane”, a `end` już jest sprawdzony).

Testy: `FreeDaysScreenTest` (dialog świąt) i testy ekranów z `MakCheckbox` w pełnym zestawie Android; `FreeDayEditorViewModelTest` bez zmian.

Przypadki brzegowe: wyłączone święto nie reaguje na dotknięcie; czytnik ekranu czyta nazwę, datę i stan jako jeden element.

Weryfikacja: `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.freedays.*"`, `.\gradlew.bat :app:compileDebugAndroidTestKotlin`, na końcu pełne zestawy JVM i Android.

Kryterium zakończenia: pełne zestawy zielone. Commit: `chore: share form controls for dates and checkboxes`.
