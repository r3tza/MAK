# Plan

Poprawki po przeglądzie aplikacji na emulatorze z 2026-10-13 (`LOG.md`, wpis z 2026-10-14). Kroki wykonuje się na gałęzi `task/settings-and-gap-notifications`, bo poprawiają kod z tej gałęzi. Docelowe zachowanie i teksty są już w `FEATURES.md` i `DOMAIN.md`; wykonawca zmienia tylko kod, testy i status w `QUEUE.md`.

Zasady dla wykonawcy (subagent z limitem kontekstu):

- Krok dzieli się na wycinki. Jeden wycinek to jedna warstwa i 1-3 pliki. Czytaj tylko podane zakresy linii; numery są przybliżone, więc najpierw znajdź podaną nazwę funkcji przez `rg -n`.
- Ścieżki w krokach są względne do `app/src/main/java/dev/retza/mak/`, testy JVM do `app/src/test/java/dev/retza/mak/`, testy Android do `app/src/androidTest/java/dev/retza/mak/`.
- Wyjście Gradle obcinaj: w Bash `2>&1 | grep -E "^e: |BUILD|FAILED|tests completed"`, w PowerShell `| Select-Object -Last 30`. Nie czytaj całych raportów testów; przy błędzie otwórz tylko plik XML nieudanej klasy z `app/build/test-results/`.
- Testy JVM: `gradlew.bat --offline :app:testDebugUnitTest --tests "<pełna nazwa klasy>"`. Testy Android tylko wskazanych klas: `gradlew.bat --offline connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=<pełna nazwa klasy>"`, na jednym emulatorze `Medium_Phone_A12` uruchomionym z `-memory 2048`. Pełnych zestawów nie uruchamiaj.
- Teksty interfejsu kopiuj dokładnie z kroku, z polskimi cudzysłowami „”. Identyfikatory i komentarze w kodzie po angielsku, w stylu otaczającego kodu.
- Po kroku: kompilacja, zielone testy kroku, status kroku w `QUEUE.md` zmieniony na `odbiór otwarty` z listą zrobionych zmian i tym, co zostało do sprawdzenia na telefonie, jeden commit `fix: ...` po angielsku, bez stopki `Co-authored-by`. Bez push.

## 1. Wróć z kreatora semestru do ekranu ustawień, z którego go otwarto (I-99)

Cel: kreator otwarty z „Ustawień” albo „Semestrów” ma stać w historii nawigacji nad tym ekranem. Dziś `openSetup` czyści historię do „Dzisiaj”, więc `ReturnToSettings` nie widzi pod kreatorem ustawień i kładzie nowe „Ustawienia” nad kreatorem; gest wstecz z nich wraca do pustego kreatora i tak w kółko. Pliki: `ui/MakRoutes.kt` (linie 60-90), `ui/SetupRoutes.kt` (linie 21-110), `ui/MakNavHostApp.kt` (linie 215-245); test `ui/MakNavigationTest.kt`. Jeden wycinek.

Zmiany:

1. W `ui/MakRoutes.kt` pod `shouldHandleSetupEffect` dodaj funkcję z komentarzem KDoc „The wizard opened from settings stands above that screen, so leaving it goes back to it.”:
   `internal fun returnsToSettingsByPop(previousRoute: String?): Boolean = previousRoute == MakRoutes.Settings || previousRoute == MakRoutes.SettingsSemesters`
2. W `ui/SetupRoutes.kt` zmień sygnaturę na `internal fun openSetup(navController: NavController, resumeExisting: Boolean = false, fromSettings: Boolean = false)`. Gdy `fromSettings` jest `true`, wywołaj `navController.navigate(setupRoute(resumeExisting)) { launchSingleTop = true }`, bez `popUpTo`. Gdy `false`, zostaw obecny kod bez zmian.
3. W `SetupEffects` (ten sam plik), w gałęzi `SetupEffect.ReturnToSettings`: jeśli `returnsToSettingsByPop(navController.previousBackStackEntry?.destination?.route)`, wywołaj `navController.popBackStack()`. W przeciwnym razie `navController.navigate(MakRoutes.Settings) { popUpTo(MakRoutes.SetupRoute) { inclusive = true } }`. Jeśli import `openSettings` przestanie być używany w pliku, usuń go.
4. W `ui/MakNavHostApp.kt`, w wywołaniu `settingsRoute(...)`, zmień `onAddSemester = { openSetup(navController) }` na `openSetup(navController, fromSettings = true)`. `startSetup` (wejście z „Dzisiaj”) zostaje bez zmian.

Testy: w `MakNavigationTest` dodaj `returnToSettingsPopsOnlyOverSettingsScreens`: `true` dla `MakRoutes.Settings` i `MakRoutes.SettingsSemesters`, `false` dla `MakRoutes.Today`, `MakRoutes.SetupRoute` i `null`.

Przypadki brzegowe:

- Kreator z „Dzisiaj” („Skonfiguruj plan” albo „Dodaj” bez semestru) nadal zastępuje historię do „Dzisiaj”, a „Zakończ” przy aktywnym semestrze prowadzi na „Dzisiaj” (`FinishToToday`) jak dotąd.
- „Wróć do ustawień” w kreatorze otwartym z „Dzisiaj” otwiera „Ustawienia” i usuwa kreator z historii, więc gest wstecz z „Ustawień” prowadzi na „Dzisiaj”.
- „Ustaw jako aktywny i dodaj zajęcia” nadal otwiera formularz zajęć nad kreatorem.

Weryfikacja: JVM `dev.retza.mak.ui.MakNavigationTest` i `dev.retza.mak.ui.setup.SetupViewModelTest`. Na emulatorze po `gradlew.bat installDebug`: „Ustawienia”, „Zarządzaj semestrami”, „Dodaj semestr”, potem gest wstecz trzy razy; kolejne ekrany to „Semestry”, „Ustawienia”, „Dzisiaj”. Drugi przebieg: utwórz semestr z datami 1.03.2027-30.06.2027, wybierz istniejący kierunek, w kroku „Dodaj zajęcia” naciśnij „Zakończ”; widać „Semestry”, a gest wstecz prowadzi do „Ustawień”, potem na „Dzisiaj”. Po sprawdzeniu usuń semestr testowy.

Kryterium zakończenia: żadna sekwencja gestów wstecz ani przycisków „Wróć do ustawień” i „Zakończ” w kreatorze otwartym z ustawień nie otwiera kreatora ponownie; testy JVM zielone.

## 2. Ogranicz korekty tygodnia do kalendarza i popraw komunikaty „Planu” (I-100)

Cel: korekta tygodnia A/B nie może powstać poza zakresem swojego kalendarza; wiersz tygodnia poza semestrem nie jest akcją i nie podpowiada wyboru kierunku; pusty dzień ma komunikat zależny od filtra; w dniu, który widok pokazuje jako wolny, nie ma akcji „Oznacz jako wolny”. Teksty są w `FEATURES.md`, sekcje „Widok „Lista””, „Dni wolne” i akapit o ekranie „Semestry”. Trzy wycinki.

Wycinek A (stan „Planu”). Pliki: `ui/schedule/ScheduleViewModel.kt` (linie 183-235 i 294-410), `ui/schedule/ScheduleScreen.kt` (tylko `data class ScheduleUiState`, linie 113-135); test `ui/schedule/ScheduleViewModelTest.kt` (wzór: testy około linii 340-405).

1. W `ScheduleUiState` dodaj pola `val weekCorrectionHint: String? = null` i `val emptyDayMessage: String = "Brak zajęć w tym dniu."`.
2. W `buildSchedule` po `shownWeek` dodaj `val weekOutside = if (weekCalendarId == null) selected.weekStatus == WeekStatus.OutsideSemester else shownWeek == null`.
3. `weekSourceLabel`: pierwszy warunek `weekOutside` daje pusty tekst `""`, dalej obecne warunki bez zmian.
4. `canCorrectWeek = weekCalendarId != null && shownWeek != null` (zastępuje `visibleWeekCalendarId(data, activeFilter) != null`).
5. `weekCorrectionHint = if (weekCalendarId == null && !weekOutside) "Wybierz kierunek w polu „Kierunek”, aby zmienić tydzień." else null`.
6. `emptyDayMessage = if (activeFilter == null) "Brak zajęć w tym dniu." else "Brak zajęć w tym dniu dla wybranego kierunku."`.
7. W `saveVisibleWeekOverride` po wyznaczeniu `calendarId` dodaj `if (!schedule.value.canCorrectWeek) return`.

Testy wycinka A: kalendarz demonstracyjny z `FakeRepository` i data poza jego zakresem (przejdź tygodniami przez `changeWeek`); przy filtrze z jednym kalendarzem `canCorrectWeek == false`, `weekSourceLabel == ""`, `weekCorrectionHint == null`. Dwa kalendarze, filtr „Wszystkie”, data w zakresie: podpowiedź równa tekstowi z kroku 5. `emptyDayMessage` dla filtra „Wszystkie” i dla wybranego kierunku. Istniejące asercje `canCorrectWeek` i `weekSourceLabel` mają nadal przechodzić.

Wycinek B (ekran „Planu”). Plik: `ui/schedule/ScheduleScreen.kt` (linie 236-250, 285-400, 515-545); test `ui/schedule/ScheduleScreenTest.kt` (Android).

1. `WeekTypeBadge` dostaje parametr `hint: String?`; wywołanie przekazuje `state.weekCorrectionHint`. Blok `if (!canCorrect) { Text("Wybierz kierunek ...") }` zastąp blokiem `hint?.let { Text(text = it, ...) }` z tym samym stylem.
2. W `labels` rysuj tekst `weekSourceLabel` tylko, gdy `weekSourceLabel.isNotBlank()`.
3. Komunikat pustego dnia na liście (`MakEmptyState("Brak zajęć w tym dniu dla wybranego kierunku.")`) i w kalendarzu (`MakEmptyState("Brak zajęć w tym dniu.")`) zastąp `MakEmptyState(state.emptyDayMessage)`.
4. `MakSecondaryAction` z tekstem „Oznacz jako wolny” rysuj tylko, gdy `state.calendarFreeDay == null`.

Testy wycinka B: w teście około linii 80-92 ustaw `weekCorrectionHint` na tekst podpowiedzi, bo bez niego tekst nie powstanie. Nowy test: stan z `canCorrectWeek = false`, `weekSourceLabel = ""`, `weekCorrectionHint = null` nie pokazuje podpowiedzi ani „Zmień”. Nowy test: widok kalendarza z ustawionym `calendarFreeDay` nie ma węzła „Oznacz jako wolny”, a bez niego ma.

Wycinek C (formularz korekty w semestrze). Pliki: `ui/semester/SemesterScreen.kt` (linie 331-380 i 705-860), `ui/semester/SemesterViewModel.kt` (funkcja `saveWeekOverride`, linie 610-650); test `ui/semester/SemesterViewModelTest.kt` (wzór: istniejący test zapisu korekty, znajdź `saveWeekOverride`).

1. `WeekOverridesSection` dostaje parametr `calendar: SemesterCalendarUi?`; `SemesterWeekOverridesScreen` przekazuje `state.calendars.firstOrNull { it.id == state.selectedCalendarId }`. Oba wywołania `WeekOverrideForm` przekazują `minDate = calendar?.startDate?.toLocalDateOrNull()` i `maxDate = calendar?.endDate?.toLocalDateOrNull()`; `WeekOverrideForm` dostaje te parametry i przekazuje je do `MakDatePickerField`.
2. W `saveWeekOverride` po sprawdzeniu `date == null`: znajdź `state.value.calendars.firstOrNull { it.id == currentCalendarId }`, odczytaj jego `startDate` i `endDate` przez `toLocalDateOrNull()`. Gdy obie daty są znane i `date.plusDays(6).isBefore(start) || date.isAfter(end)`, ustaw `weekStartDateError = "Wybierz tydzień z zakresu kalendarza."` i zakończ bez zapisu.

Testy wycinka C: zapis korekty z poniedziałkiem poza zakresem kalendarza ustawia błąd i nie zapisuje; tydzień, który zaczyna się przed datą początku kalendarza, ale ją obejmuje, zapisuje się.

Przypadki brzegowe:

- Filtr „Wszystkie” z kilkoma kalendarzami i data poza wszystkimi: „Poza semestrem”, bez źródła i bez podpowiedzi.
- Korekta zapisana wcześniej poza zakresem (stary plik albo synchronizacja) dalej jest widoczna na liście i da się ją usunąć; edycja bez zmiany daty pokaże błąd z kroku C.2, co jest oczekiwane.
- Dzień wolny tylko w części kalendarzy przy filtrze „Wszystkie” nie jest dniem wolnym widoku, więc akcja „Oznacz jako wolny” zostaje.

Weryfikacja: JVM `dev.retza.mak.ui.schedule.ScheduleViewModelTest` i `dev.retza.mak.ui.semester.SemesterViewModelTest`; Android `dev.retza.mak.ui.schedule.ScheduleScreenTest` i `dev.retza.mak.ui.semester.SemesterScreenTest`. Na emulatorze z danymi demonstracyjnymi: przy filtrze „Wszystkie” pusta niedziela pokazuje „Brak zajęć w tym dniu.”, przy filtrze „Informatyka” tekst z dopiskiem; w kalendarzu na dniu wolnym 1 listopada (po „Dodaj święta ustawowe”) nie ma „Oznacz jako wolny”; „Następny tydzień” poza koniec semestru demonstracyjnego daje wiersz „Poza semestrem” bez „Zmień”; formularz „Dodaj korektę” nie pozwala wybrać daty poza zakresem.

Kryterium zakończenia: korekty nie da się zapisać poza zakresem kalendarza ani z „Planu”, ani z ustawień semestru; teksty zgodne z `FEATURES.md`; wymienione testy zielone.

## 3. Oznacz odwołane terminy i pozwól odwołać zmieniony termin (I-101)

Cel: kalendarz „Planu” pokazuje odwołany termin krzyżykiem w kolorze kierunku, zawsze, za znacznikami zajęć, które się odbywają; legenda ma „Odwołany termin”, gdy w siatce jest taki znacznik. Termin zmieniony albo przeniesiony można odwołać po potwierdzeniu, a „Przywróć termin” zawsze pyta o potwierdzenie. Reguły i teksty: `FEATURES.md`, „Widok „Kalendarz”” i „Struktura karty zajęć”; `DOMAIN.md`, „Zmiany pojedynczych wystąpień”. Cztery wycinki.

Wycinek A (znaczniki w stanie kalendarza). Pliki: `ui/components/PresentationModels.kt` (linie 41-70), `ui/schedule/ScheduleViewModel.kt` (`calendarGrid` linie 253-292, wywołanie `calendarLegend` około linii 371, `calendarLegend` i `calendarAccessibilityLabel` linie 421-456); test `ui/schedule/ScheduleViewModelTest.kt` (wzór: test znaczników zmienionego terminu około linii 480-515).

1. `CalendarMarkerUi` i `CalendarLegendUi` dostają pole `val isCancelled: Boolean = false`.
2. W `calendarGrid`: `val cancelled = schedule.cancelled.filter { matchesFilter(it, activeFilter) }` i `val shown = occurrences + cancelled`. Znaczniki budowane z `shown` z tą samą regułą liczby (`take(if (shown.size > 5) 4 else 5)`, `hasMoreMarkers = shown.size > 5`). Dla każdego: `isCancelled = it.status == OccurrenceStatus.CANCELLED`, `isChanged = it.occurrenceChange != null && it.status != OccurrenceStatus.CANCELLED`.
3. `calendarAccessibilityLabel` dostaje parametr `cancelled: List<PlannedOccurrence>`. Liczba zajęć dalej z `occurrences.size`, a szczegóły z `(occurrences + cancelled).take(3)`; `calendarOccurrenceLabel` dopisuje już „, odwołane”.
4. `calendarLegend` dostaje parametr `hasCancelled: Boolean`. Po pozycji „Zmieniony termin”, przed „Dzień wolny”, dodaj `CalendarLegendUi(label = "Odwołany termin", isCancelled = true)`, gdy `hasCancelled`. Wywołanie przekazuje `hasCancelled = calendarDays.any { it.markers.any(CalendarMarkerUi::isCancelled) }`.

Testy wycinka A: odwołanie (`OccurrenceChangeEntity` z `kind` równym `CANCELLED`) terminu klasy 1 w dniu z dwiema klasami: znaczniki w kolejności najpierw zajęcia odbywające się, potem odwołane, `isCancelled` tylko przy odwołanym, `isChanged` przy nim `false`; opis dnia zawiera „odwołane”, a licznik w nim liczy tylko zajęcia odbywające się; legenda zawiera „Odwołany termin”, a w miesiącu bez odwołań go nie ma. Odwołanie zajęć kierunku spoza filtra nie daje znacznika.

Wycinek B (rysunek krzyżyka). Plik: `ui/schedule/ScheduleScreen.kt` (`CalendarDay` linie 548-640, `CalendarMarker` 646-662, `CalendarLegend` 664-700); test `ui/schedule/ScheduleScreenTest.kt` (Android, wzór: testy znaczników około linii 340-365).

1. `CalendarMarker` dostaje parametr `cancelled: Boolean = false`. Przy `cancelled` zamiast tła i obramowania rysuj w `Modifier.drawBehind` dwie przekątne kolorem `color`: grubość `if (size >= 8.dp) 1.5.dp else 1.dp`, `cap = StrokeCap.Round`, odsunięcie od brzegów równe połowie grubości. Wymiar znacznika bez zmian.
2. `CalendarDay` przekazuje `cancelled = marker.isCancelled`; `testTag` zostaje `calendar-marker-${marker.id}`.
3. `CalendarLegend`: kolor `MaterialTheme.colorScheme.onSurfaceVariant` także dla `item.isCancelled`, i `cancelled = item.isCancelled`.

Testy wycinka B: legenda z pozycją `CalendarLegendUi("Odwołany termin", isCancelled = true)` pokazuje ten tekst; dzień ze znacznikiem `isCancelled` ma węzeł z jego `testTag`.

Wycinek C (stan szczegółów terminu). Pliki: `ui/occurrence/OccurrenceDetailsModels.kt` (linie 15-80), `ui/occurrence/OccurrenceViewModel.kt` (linie 200-300 i 499-590); test `ui/occurrence/OccurrenceViewModelTest.kt` (wzór: `cancelAndRestorePublishMessages` około linii 672; termin przeniesiony przygotuj przez `openEditDialog`, `updateTargetDateDraft` i `saveOccurrenceChange` albo przez wpis zmiany w `FakeRepository`, jak w innych testach tego pliku).

1. `OccurrenceDetailsUiState` dostaje pola `showRestoreConfirmation: Boolean = false`, `showCancelChangedConfirmation: Boolean = false` i `restoreTargetLabel: String = ""`. `hasPlanDraft()` uwzględnia też `showRestoreConfirmation || showCancelChangedConfirmation`.
2. W `buildDetails`: `canCancelOccurrence = canEdit` (zamiast `canEdit && occurrence.occurrenceChange == null`) i `restoreTargetLabel = "${originalDate.format(fullDateFormatter)}, ${base.startTime} - ${base.endTime}"`.
3. W `withFreshData` przenieś z bieżącego stanu `showRestoreConfirmation` i `showCancelChangedConfirmation`, jak `showDeleteConfirmation`.
4. Nowe funkcje, bez zagnieżdżania `editSession.run` w `editSession.run`:
   - `requestCancelOccurrence()`: gdy `!state.value.canCancelOccurrence`, nic; gdy `!state.value.canRestoreOccurrence` (zwykły termin), wywołaj `cancelOccurrence()`; w przeciwnym razie `editSession.run { update { it.copy(showCancelChangedConfirmation = true) } }`.
   - `dismissCancelChangedConfirmation()`: `editSession.run { update { it.copy(showCancelChangedConfirmation = false) } }`.
   - `requestRestoreOccurrence()`: gdy `state.value.canRestoreOccurrence`, `editSession.run { update { it.copy(showRestoreConfirmation = true) } }`.
   - `dismissRestoreConfirmation()`: jak wyżej z `false`.
5. W `cancelOccurrence()` i `restoreOccurrence()`, w istniejącym `update { it.copy(isSaving = true, draftError = null) }`, ustaw też odpowiednio `showCancelChangedConfirmation = false` i `showRestoreConfirmation = false`. Zapis odwołania zostaje bez zmian: repozytorium nadpisuje istniejącą zmianę tego terminu.

Testy wycinka C: termin przeniesiony ma `canCancelOccurrence == true`; `requestCancelOccurrence()` ustawia tylko flagę; `cancelOccurrence()` daje status `Cancelled` w dacie z planu i `canRestoreOccurrence == true`; `restoreOccurrence()` daje status `Scheduled` w dacie z planu. Zwykły termin: `requestCancelOccurrence()` odwołuje od razu, bez flagi. `requestRestoreOccurrence()` ustawia flagę tylko przy `canRestoreOccurrence`. Notatka do terminu przeniesionego zostaje po odwołaniu. Istniejące testy z `restoreOccurrence()` mają przechodzić bez zmian.

Wycinek D (ekran szczegółów terminu). Pliki: `ui/ClassOccurrenceRoutes.kt` (linie 90-140), `ui/occurrence/OccurrenceDetailsScreen.kt` (linie 49-161); test `ui/occurrence/OccurrenceDetailsScreenTest.kt` (Android).

1. W menu trasy akcja „Odwołaj termin” wywołuje `occurrenceViewModel.requestCancelOccurrence()`.
2. `OccurrenceDetailsScreen` dostaje parametry `onRequestRestore`, `onDismissRestore`, `onConfirmCancelChanged` i `onDismissCancelChanged`; trasa podaje `requestRestoreOccurrence`, `dismissRestoreConfirmation`, `cancelOccurrence` i `dismissCancelChangedConfirmation`. Przycisk „Przywróć termin” wywołuje `onRequestRestore`.
3. Przy `state.showRestoreConfirmation`: `MakDialog(title = "Przywrócić termin?", description = "Termin wróci do planu: ${state.restoreTargetLabel}.", onDismiss = onDismissRestore, dismissAction = MakDialogAction("Anuluj", onDismissRestore, enabled = !state.isSaving), confirmAction = MakDialogAction("Przywróć", onRestoreOccurrence, enabled = !state.isSaving))`.
4. Przy `state.showCancelChangedConfirmation`: `MakConfirmDeletionDialog(title = "Odwołać zmieniony termin?", text = "Zmiana tego terminu zostanie usunięta, a termin będzie odwołany. Notatka do terminu zostanie.", isDeleting = state.isSaving, onConfirm = onConfirmCancelChanged, onCancel = onDismissCancelChanged, confirmLabel = "Odwołaj")`.

Testy wycinka D: „Przywróć termin” wywołuje `onRequestRestore`, a nie `onRestoreOccurrence`; stan z `showRestoreConfirmation` pokazuje tytuł i treść z etykietą, „Przywróć” wywołuje `onRestoreOccurrence`; stan z `showCancelChangedConfirmation` pokazuje dialog, „Odwołaj” wywołuje `onConfirmCancelChanged`.

Przypadki brzegowe:

- Termin jednorazowy i odwołany nie mają „Odwołaj termin” (`canEdit` jest `false`).
- Odwołanie przeniesionego terminu usuwa go z nowej daty i pokazuje jako odwołany w dacie z planu (przy „Pokaż odwołane”); krzyżyk stoi w dacie z planu.
- Termin z planu wypadający w dzień wolny nie trafia do listy odwołanych, więc nie ma krzyżyka (`DOMAIN.md`, „Dni wolne”).

Weryfikacja: JVM `dev.retza.mak.ui.schedule.ScheduleViewModelTest` i `dev.retza.mak.ui.occurrence.OccurrenceViewModelTest`; Android `dev.retza.mak.ui.schedule.ScheduleScreenTest` i `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`. Na emulatorze z danymi demonstracyjnymi: odwołaj poniedziałkowe „Programowanie aplikacji” w następnym tygodniu i zrób zrzut kalendarza w motywie jasnym i ciemnym oraz przy 320 dp; otwórz przeniesioną „Analizę danych”, odwołaj ją przez menu (dialog), potem „Przywróć termin” (dialog z datą z planu).

Kryterium zakończenia: krzyżyk i legenda zgodne z `FEATURES.md` w obu motywach; zmieniony termin da się odwołać i przywrócić, oba kroki z dialogiem; testy zielone.

## 4. Pytaj przed odrzuceniem zmian w formularzu zajęć i dialogu terminu (I-102)

Cel: gest wstecz, strzałka w górnym pasku i „Anuluj” w formularzu zajęć oraz zamknięcie dialogu „Edytuj ten termin” gestem wstecz albo dotknięciem poza nim pytają „Odrzucić zmiany?” tylko wtedy, gdy któreś pole po `trim()` różni się od stanu z chwili otwarcia (`FEATURES.md`, „Dodawanie i edycja zajęć”). Trzy wycinki.

Wycinek A (stan formularza). Pliki: `ui/edit/ClassEditViewModel.kt` (linie 44-200 i 355-417), `ui/edit/ClassEditScreen.kt` (tylko `data class ClassEditUiState`, linie 70-100); test `ui/edit/ClassEditViewModelTest.kt` (wzór: testy odtwarzania szkicu z tego samego `SavedStateHandle` i odbioru efektu `CloseEditor`).

1. `ClassEditUiState` dostaje pole `val showDiscardConfirmation: Boolean = false` (nie trafia do `draftFields`).
2. Pod `draftFields` dodaj `private val comparedDraftFields = draftFields.filter { it.first != "title" && it.first != "courseName" }` i `private fun ClassEditUiState.comparableDraft(): HashMap<String, String> = HashMap(comparedDraftFields.associate { it.first to it.second(this).orEmpty().trim() })`.
3. W klasie: stała `KEY_BASELINE = "class_edit_baseline"` obok pozostałych kluczy; pole `private var baseline: HashMap<String, String>? = savedState[KEY_BASELINE]` z setterem zapisującym do `savedState`, jak `editingClassId`; `private fun rememberBaseline() { baseline = state.value.comparableDraft() }`; `fun hasUnsavedChanges(): Boolean = baseline?.let { state.value.comparableDraft() != it } ?: false`.
4. Wywołaj `rememberBaseline()` na końcu `openNewFromFreshPlan` i w `openEditFromFreshPlan` zaraz po `state.value = classEditState(item, data)`.
5. Nowe funkcje: `requestClose()` (gdy `isSaving`, nic; gdy `hasUnsavedChanges()`, `state.update { it.copy(showDiscardConfirmation = true) }`; w przeciwnym razie `effectsChannel.trySend(ClassEditEffect.CloseEditor)`), `confirmDiscard()` (zdejmuje flagę i wysyła `CloseEditor`) i `dismissDiscard()` (zdejmuje flagę).

Testy wycinka A: nowy formularz bez zmian i z samymi spacjami w nazwie zamyka się bez dialogu; nazwa „A” ustawia `showDiscardConfirmation`; edycja otwarta i zamknięta bez zmian zamyka się bez dialogu; zmiana sali i powrót do pierwotnej wartości zamyka się bez dialogu; formularz z datą jednorazową (`openNewFromFreshPlan(date)`) bez zmian zamyka się bez dialogu; po odtworzeniu ViewModelu z tego samego `SavedStateHandle` wpisana zmiana dalej daje dialog.

Wycinek B (trasa i górny pasek). Pliki: `ui/TopBar.kt` (linie 10-17), `ui/MakNavHostApp.kt` (linie 128-140), `ui/ClassOccurrenceRoutes.kt` (linie 35-88); oraz `ui/edit/ClassEditScreen.kt` (parametry ekranu około linii 100-120 i dialog ukrytych danych około linii 270-292); test `ui/edit/ClassEditScreenTest.kt` (Android).

1. `TopBarSpec` dostaje opcjonalne pole `onBack` z wartością domyślną `null`, typu funkcji bez argumentów jak `MakDialogAction.onClick`. W `MakNavHostApp` przy `MakTopBar` ustaw `onBack = spec?.onBack ?: ::navigateBack`.
2. W `classEditRoute`, za blokiem stanu `Open`: `val requestClose = remember(classEditViewModel) { { classEditViewModel.requestClose() } }`, `BackHandler(onBack = requestClose)` (import `androidx.activity.compose.BackHandler`), `TopBarSpec(MakRoutes.Edit, title = editorState.title, onBack = requestClose)` i `onCancel = requestClose`. Przekaż do ekranu `onConfirmDiscard = classEditViewModel::confirmDiscard` i `onDismissDiscard = classEditViewModel::dismissDiscard`.
3. `ClassEditScreen` dostaje te dwa parametry i przy `state.showDiscardConfirmation` rysuje `MakConfirmDeletionDialog(title = "Odrzucić zmiany?", text = "Wpisane dane nie zostaną zapisane.", isDeleting = false, onConfirm = onConfirmDiscard, onCancel = onDismissDiscard, confirmLabel = "Odrzuć")`.

Testy wycinka B: stan z `showDiscardConfirmation` pokazuje tytuł i treść; „Odrzuć” wywołuje `onConfirmDiscard`, „Anuluj” w dialogu `onDismissDiscard`; „Anuluj” formularza wywołuje `onCancel`. Popraw istniejące wywołania `ClassEditScreen` w testach o nowe parametry.

Wycinek C (dialog „Edytuj ten termin”). Pliki: `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceViewModel.kt` (linie 200-220 i 560-590), `ui/occurrence/OccurrenceDetailsScreen.kt` (linie 105-115 i 163-180) oraz wywołanie w `ui/ClassOccurrenceRoutes.kt`; testy `OccurrenceViewModelTest` (JVM) i `OccurrenceDetailsScreenTest` (Android).

1. `OccurrenceDetailsUiState` dostaje `showDiscardEditConfirmation: Boolean = false`; `withFreshData` przenosi ją z bieżącego stanu.
2. `requestDismissEditDialog()`: gdy `isSaving`, nic. Pola są bez zmian, gdy `targetDateDraft == currentDate`, `startTimeDraft == startTime`, `endTimeDraft == endTime` i `roomDraft.trim() == room.orEmpty().trim()`; wtedy wywołaj `dismissEditDialog()`, w przeciwnym razie `editSession.run { update { it.copy(showDiscardEditConfirmation = true) } }`.
3. `confirmDiscardEdit()`: `editSession.run { update { it.copy(showDiscardEditConfirmation = false, showEditDialog = false, draftErrors = emptyMap(), draftError = null) } }`. `keepEditing()`: `editSession.run { update { it.copy(showDiscardEditConfirmation = false) } }`. `dismissEditDialog()` ustawia też `showDiscardEditConfirmation = false`.
4. `OccurrenceEditDialog` dostaje osobny parametr `onRequestDismiss`: `MakDialog(onDismiss = { if (!state.isSaving) onRequestDismiss() })`, a „Anuluj” dalej wywołuje `onDismiss`. Przy `state.showDiscardEditConfirmation` ekran rysuje `MakConfirmDeletionDialog(title = "Odrzucić zmiany?", text = "Wpisane dane nie zostaną zapisane.", isDeleting = false, onConfirm = onConfirmDiscardEdit, onCancel = onKeepEditing, confirmLabel = "Odrzuć")`. Trasa podaje `requestDismissEditDialog`, `confirmDiscardEdit` i `keepEditing`.

Testy wycinka C: zamknięcie bez zmian i ze zmianą tylko spacji w sali zamyka dialog; zmiana godziny ustawia flagę, `confirmDiscardEdit()` zamyka dialog, `keepEditing()` zostawia go otwartego z wpisanymi wartościami.

Przypadki brzegowe:

- Zapis formularza nadal zamyka go bez pytania (`CloseEditor` z `save`).
- Dialog ukrytych danych („Część danych przestanie być widoczna”) i dialog „Odrzucić zmiany?” nie występują naraz, bo pierwszy powstaje tylko przy zapisie.
- `BackHandler` trasy formularza ma pierwszeństwo przed `BackHandler` w `MakNavHostApp`, bo rejestruje się później; sprawdź to na emulatorze.

Weryfikacja: JVM `dev.retza.mak.ui.edit.ClassEditViewModelTest` i `dev.retza.mak.ui.occurrence.OccurrenceViewModelTest`; Android `dev.retza.mak.ui.edit.ClassEditScreenTest`, `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest` i `dev.retza.mak.di.KoinGraphTest`. Na emulatorze: „Dodaj”, gest wstecz od razu (bez dialogu); „Dodaj”, trzy spacje w nazwie, gest wstecz (bez dialogu); „Dodaj”, nazwa „Test”, strzałka w górnym pasku (dialog, „Anuluj” zostawia formularz, „Odrzuć” zamyka); „Edytuj wszystkie terminy” i wyjście bez zmian (bez dialogu); „Dodaj termin jednorazowy” z kalendarza i wyjście bez zmian (bez dialogu); „Zmień termin”, zmiana sali, dotknięcie poza dialogiem (dialog).

Kryterium zakończenia: dialog pojawia się dokładnie w przypadkach z `FEATURES.md`; testy zielone.

## 5. Popraw pola dat dni wolnych, dialog usunięcia semestru, import i dane demonstracyjne (I-103)

Cel: daty w dialogu dni wolnych nie łamią się; dialog usunięcia semestru podaje nazwę; import odrzuca kolor kierunku spoza `#RRGGBB` i pokazuje liczbę dni wolnych; dane demonstracyjne nie zapisują notatki w polu zmiany terminu, którego aplikacja nie pokazuje. Trzy wycinki.

Wycinek A (pola dat). Plik: `ui/components/MakFormControls.kt` (`MakDateRangeFields`, linie 188-232, i `RangeDateField`); test JVM `ui/components/DateRangeSelectionTest.kt`.

1. Dodaj `internal val DateRangeSideBySideMinWidth = 344.dp` i `internal fun stackDateRangeFields(maxWidth: Dp, fontScale: Float): Boolean = maxWidth / fontScale < DateRangeSideBySideMinWidth` z komentarzem: dwa pola obok siebie mieszczą datę „14.10.2026” i ikonę tylko od tej szerokości przy skali czcionki 1.
2. Zastąp `Row` w `MakDateRangeFields` przez `BoxWithConstraints(modifier.fillMaxWidth())`: gdy `stackDateRangeFields(maxWidth, LocalDensity.current.fontScale)`, `Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm))` z polami `Modifier.fillMaxWidth()`, w przeciwnym razie obecny `Row` z `Modifier.weight(1f)`.

Testy wycinka A: `stackDateRangeFields(272.dp, 1f)` daje `true`, `(512.dp, 1f)` daje `false`, `(512.dp, 2f)` daje `true`.

Wycinek B (ustawienia i podsumowanie importu). Pliki: `ui/settings/SettingsScreen.kt` (wywołanie `DeleteSemesterDialog` około linii 472-477, funkcja 541-554, wiersze `ImportStatRow` około 832-838), `export/PlanBackupService.kt` (`ImportSummary` linie 9-17, `toSummary` 53-61); testy `export/PlanBackupServiceTest.kt` (JVM) i `ui/settings/SettingsScreenTest.kt` (Android, `ImportSummary` około linii 751).

1. `DeleteSemesterDialog` dostaje parametr `semesterName: String`; treść: `"Usunięcie semestru „$semesterName” usunie jego plan i dane. Tej operacji nie można cofnąć."`. Wywołanie przekazuje `state.semesters.firstOrNull { it.id == state.semesterToDeleteId }?.name.orEmpty()`.
2. `ImportSummary` dostaje `val freeDayCount: Int` po `changeCount`; `toSummary` liczy `semesters.sumOf { it.freeDays.distinctBy(::freeDayGroupKey).size }` z prywatną funkcją `freeDayGroupKey(day: FreeDayRecord) = Triple(day.startDate, day.endDate, day.name)`, bo ten sam zakres w kilku kalendarzach to jedna karta na ekranie „Dni wolne”.
3. Po `ImportStatRow("Zmiany wystąpień", ...)` dodaj `ImportStatRow("Dni wolne", preview.freeDayCount)`.

Testy wycinka B: podsumowanie liczy jeden zakres zapisany w dwóch kalendarzach jako 1; ekran podglądu pokazuje „Dni wolne”; dialog usunięcia pokazuje nazwę semestru. Popraw konstruktor `ImportSummary` w `SettingsScreenTest`.

Wycinek C (import i dane demonstracyjne). Pliki: `export/ExportImporter.kt` (linie 44-55), `data/repository/DemoDataSeeder.kt` (linie 160-185); testy `export/ExportImporterTest.kt` i `data/repository/DemoDataSeederTest.kt`.

1. W `ExportImporter`, w pętli po `snapshot.studyPrograms`, dodaj: gdy `!HexColor.matches(program.color)`, błąd `"Kierunek ${program.id} ma niepoprawny kolor."`. Stała na końcu pliku: `private val HexColor = Regex("^#[0-9A-Fa-f]{6}$")`. Przed zmianą sprawdź `rg -n "color = \"#" app/src/main` i `rg -n "\"#%" app/src/main`: jeśli aplikacja gdzieś zapisuje kolor w innym formacie niż `#RRGGBB`, zatrzymaj się i opisz to w odpowiedzi.
2. W `DemoDataSeeder` zmiana „Analizy danych” ma `note = null`, a za nią powstaje notatka do terminu: `scheduleRepository.saveOccurrenceNote(OccurrenceNoteRecord(semesterId = semesterId, classId = analyticsId, occurrenceDate = changeDate, body = "Przykład notatki, która przechodzi z przeniesionym terminem."))`.

Testy wycinka C: plik z kolorem `"zielony"` i z `"#12345"` jest odrzucany z błędem koloru, a `"#137b71"` przyjmowany. W `DemoDataSeederTest` notatka do terminu „Analizy danych” istnieje w dacie z planu, a zmiana nie ma `note`.

Przypadki brzegowe:

- Synchronizacja Google czyta plan tą samą walidacją; plan z Dysku z niepoprawnym kolorem zostanie odrzucony tak jak plik, co jest zamierzone.
- Dialog usunięcia otwarty dla semestru, który w międzyczasie zniknął, zamyka `canRequestSemesterDeletion`; pusta nazwa nie wystąpi w praktyce.

Weryfikacja: JVM `dev.retza.mak.ui.components.DateRangeSelectionTest`, `dev.retza.mak.export.PlanBackupServiceTest`, `dev.retza.mak.export.ExportImporterTest`, `dev.retza.mak.data.repository.DemoDataSeederTest`; Android `dev.retza.mak.ui.settings.SettingsScreenTest` i `dev.retza.mak.ui.freedays.FreeDaysScreenTest`. Zrzuty dialogu „Dodaj dni wolne” na emulatorze: 411 dp (pola jedno pod drugim, data w jednej linii), 320 dp ze skalą czcionki 2,0, tablet `adb shell wm size 2560x1600` i `adb shell wm density 320` (pola obok siebie); potem `wm size reset`, `wm density reset`, `font_scale 1.0`. Po `adb shell pm clear dev.retza.mak.debug` przeniesiona „Analiza danych” pokazuje notatkę do terminu.

Kryterium zakończenia: żaden z trzech zrzutów nie łamie daty; teksty zgodne z `FEATURES.md`; testy zielone.
