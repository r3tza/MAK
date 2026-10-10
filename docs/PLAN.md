# Plan najbliższych prac

Cel: wykonać pierwszą część I-89 (pozostałe porządki z audytu: E2, E4, H2, H4, H5, H6, J2, B6). Raport audytu: `docs/audit_09102026.md`. Polecenie użytkownika z 2026-10-10: wykonać wszystkie poprawki z audytu. K1 (`ActivePlanSource`, `SettingsPreferences` i `polishPlural` poza pakietem `ui`) trafi do następnego planu razem z I-90, bo oba przenoszą typy między pakietami.

## Zasady dla wykonawcy

Przeczytaj tę sekcję przed każdym krokiem.

- Wszystkie kroki wykonuj na gałęzi `task/I-79-hide-study-programs` (decyzja użytkownika z 2026-10-10). Przed pierwszym krokiem sprawdź `git branch --show-current`; jeśli to inna gałąź, przełącz się poleceniem `git switch task/I-79-hide-study-programs` i nie twórz nowej.
- Ścieżki kodu są względne wobec `app/src/main/java/dev/retza/mak/`, testów JVM wobec `app/src/test/java/dev/retza/mak/`, testów Android wobec `app/src/androidTest/java/dev/retza/mak/`.
- Przed zmianą otwórz każdy plik z listy kroku i znajdź opisany fragment po nazwie funkcji, nie po numerze wiersza. Jeśli fragmentu nie ma albo wygląda inaczej niż w opisie, zrób zmianę zgodną z celem kroku i opisz różnicę w commicie. Zatrzymaj się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Polecenia uruchamiaj w katalogu repozytorium w PowerShellu. Testy JVM jednej klasy: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.OccurrenceViewModelTest"`. Jeśli Gradle zgłosi `No cached version ... available for offline mode`, uruchom to samo polecenie raz bez `--offline`, potem wróć do `--offline`.
- Po każdej zmianie kodu: `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`. Kompilacja testów Android nie oznacza ich uruchomienia.
- Testy Android: najwyżej jeden emulator naraz, z 2048 MiB RAM. Obraz `Medium_Phone_A12` (`STACK.md`, sekcja 6); na komputerze, który go nie ma, `Medium_Phone` (testy używają aplikacji `dev.retza.mak.debug`, a dane odbioru synchronizacji są w `dev.retza.mak`; nie czyść ich). Jedna klasa na wywołanie: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.di.KoinGraphTest"`; lista klas po przecinku w PowerShellu uruchamia tylko pierwszą. Przed testami Android cofnij zmiany ekranu ze zrzutów (`adb shell wm size reset`). Testy Android przeinstalowują aplikację debug i zerują jej dane, więc ręczne sprawdzenie rób po `installDebug`, przed testami Android, albo odtwórz dane po nich.
- Graf `NavHost` zapamiętuje argumenty funkcji `NavGraphBuilder.xxxRoute(...)` z chwili zbudowania. Wartości, które się zmieniają w czasie, przekazuj do trasy jako funkcję (`() -> T`) albo strumień i czytaj je w `composable` (wzór: `pendingPlanDate` w `TodayScheduleRoutes.kt`).
- ViewModel pobrany w `composable` przez `koinViewModel()` dostaje `SavedStateHandle` celu nawigacji z jego argumentami (klucze jak w trasie, na przykład `"classId"` i `"date"`). Wzór pobrania: `todayRoute` w `TodayScheduleRoutes.kt`.
- Nie uruchamiaj pełnego zestawu testów aplikacji bez polecenia użytkownika.
- Każdy krok kończy się jednym commitem z podanym tematem, bez stopki `Co-authored-by`. Do commita dołącz zmiany `PLAN.md`, `QUEUE.md` i dokumentów, których dotyczy krok. Pod nagłówkiem zrobionego kroku dopisz linię `Stan: zrobione.` z krótkim opisem różnic wobec planu.
- Nie wypychaj gałęzi, nie otwieraj pull requesta i nie scalaj bez wyraźnego polecenia użytkownika.
- Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py`; wymagany kod 0.
- Teksty interfejsu i dokumentacja po polsku, identyfikatory i komentarze w kodzie po angielsku. Bez znaków `·`, `—` i strzałek.

## 1. Zapisuj zmiany terminów i korekty bez wczytywania planu (I-89)

Stan: zrobione. Usunięte nieużywane `deleteOccurrenceNote(id)`, `deleteOccurrenceChange(id)` i ich `deleteById` w DAO; `deleteWeekOverride(id)` zostaje dla ekranu semestru. Przywrócenie terminu bez zmiany publikuje teraz „Przywrócono termin” zamiast nic nie robić; przycisk jest widoczny tylko przy zmienionym terminie. Ręcznie: odwołanie i przywrócenie, zapis i wyczyszczenie notatki do terminu.

Cel: ViewModele nie czytają całego planu, aby znaleźć identyfikator wiersza przed zapisem (E2). Zapis przekazuje `id = 0`, a repozytorium robi upsert po kluczu naturalnym, co już potrafi. Usuwanie odbywa się po kluczu naturalnym. Reguła tożsamości wiersza zostaje w jednym miejscu, w `data`. Zachowanie się nie zmienia.

Pliki: `data/database/Daos.kt`, `data/repository/ScheduleRepository.kt`, `data/repository/SemesterRepository.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/schedule/ScheduleViewModel.kt`; atrapy `ui/FakeRepository.kt`, `ui/FakeSemesterRepository.kt` i inne implementacje wskazane przez kompilator; testy `ui/occurrence/OccurrenceViewModelTest.kt`, `ui/schedule/ScheduleViewModelTest.kt`, Android `data/RoomPersistenceTest.kt`.

Zmiany:

1. DAO: dodaj `@Query("DELETE FROM occurrence_changes WHERE class_id = :classId AND original_date = :originalDate") suspend fun deleteForOccurrence(classId: Long, originalDate: LocalDate)`, tak samo dla `occurrence_notes` (kolumna `occurrence_date`) i `week_overrides` (`deleteForWeek(calendarId, weekStartDate, scope)`). Sprawdź nazwy kolumn w `data/entity/Entities.kt` i istniejące zapytania `findForOccurrence` i `findForWeek`.
2. `ScheduleRepository`: `suspend fun deleteOccurrenceChange(classId: Long, originalDate: LocalDate)` i `suspend fun deleteOccurrenceNote(classId: Long, occurrenceDate: LocalDate)`; `SemesterRepository`: `suspend fun deleteWeekOverride(calendarId: Long, weekStartDate: LocalDate, scope: WeekOverrideScope)`. Wersje po `id` zostają, jeśli używa ich inny kod (`rg -n "deleteOccurrenceChange\(|deleteOccurrenceNote\(|deleteWeekOverride\(" app/src/main`); nieużywane usuń.
3. `OccurrenceViewModel`: w `cancelOccurrence`, `restoreOccurrence`, `saveOccurrenceNote` i `saveOccurrenceChange` usuń wyszukiwanie istniejącego wiersza w `freshPlanData()`; zapis z `id = 0`; usuwanie przez nowe funkcje po kluczu (`classId` i `originalDate` albo data notatki). `restoreOccurrence` bez istniejącej zmiany kończy się jak dziś bez błędu (zapytanie DELETE bez wierszy niczego nie zmienia). `saveSharedNote` dalej czyta zajęcia, bo zapisuje cały wiersz zajęć; jeśli `freshPlanData()` służy już tylko temu, zostaw je. Usuń `freshPlanData()`, jeśli nic go nie używa.
4. `ScheduleViewModel.saveVisibleWeekOverride`: zapis z `id = 0` bez wyszukiwania `existing`. `clearVisibleWeekOverride`: usuwanie po kluczu (kalendarz, poniedziałek, zakres) bez wyszukiwania.
5. Atrapy repozytoriów w testach dostają nowe funkcje usuwania po kluczu.

Testy:

- `OccurrenceViewModelTest`: istniejące testy odwołania, przywrócenia, przeniesienia i notatek przechodzą bez zmiany oczekiwań. Nowy `restoreWithoutChangeDoesNothing`: przywrócenie terminu bez zmiany nie publikuje błędu.
- `ScheduleViewModelTest`: istniejące testy korekty tygodnia przechodzą; nowy `clearingWeekOverrideDeletesByWeekAndScope`: po zapisie korekty `ONE_WEEK` i `FROM_WEEK` dla tego samego tygodnia usunięcie `ONE_WEEK` zostawia `FROM_WEEK`.
- `RoomPersistenceTest` (Android): nowy `deleteByNaturalKeyRemovesOnlyThatRow` dla zmiany terminu, notatki i korekty.

Przypadki brzegowe: zapis zmiany terminu dla terminu z istniejącą zmianą aktualizuje ten sam wiersz (upsert), a nie tworzy drugi; usunięcie pustej notatki działa po kluczu; korekta zapisana w innym kalendarzu tego samego tygodnia zostaje.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.*" --tests "dev.retza.mak.ui.schedule.*" --tests "dev.retza.mak.data.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.data.RoomPersistenceTest` i `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`.
4. Ręcznie: odwołanie i przywrócenie terminu, notatka do terminu zapisana i wyczyszczona, korekta tygodnia w „Planie” zapisana i usunięta.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; w `OccurrenceViewModel` i `ScheduleViewModel` nie ma `occurrenceChanges.firstOrNull`, `occurrenceNotes.firstOrNull` ani `weekOverrides.firstOrNull` przed zapisem. Commit: `chore: write occurrence rows and week overrides by natural key (I-89)`. W tym commicie w `QUEUE.md` zmień status I-89 na `w toku`.

## 2. Wybieraj semestr zastępczy jedną regułą w warstwie danych (I-89)

Stan: zrobione. Usunięty test `AppViewModelTest.selectsFirstSemesterWhenNoneActive` (zachowanie przeniesione do repozytorium i sprawdzane w `RoomPersistenceTest`). `DOMAIN.md` opisuje regułę wyboru semestru zastępczego.

Cel: zasada „jeśli są semestry, jeden jest aktywny” jest pilnowana w transakcjach `data` jedną funkcją domenową, a nie w `init` ViewModelu (E4). Usunięcie semestru, import i synchronizacja wybierają ten sam semestr: obejmujący dziś, a bez takiego ten, który zaczyna się najpóźniej, a bez kalendarzy pierwszy.

Pliki: `domain/SemesterActivation.kt`, `sync/SyncPlanFile.kt`, `data/repository/SemesterRepository.kt`, `data/repository/PlanBackupGateway.kt`, `ui/AppViewModel.kt`, `MakApplication.kt`; testy nowy `domain/FallbackSemesterTest.kt`, `sync/SyncCoordinatorTest.kt`, `ui/AppViewModelTest.kt` (jeśli istnieje), Android `data/RoomPersistenceTest.kt`; dokument `docs/DOMAIN.md` (sekcja o aktywnym semestrze, jeśli opisuje wybór po usunięciu).

Zmiany:

1. `domain/SemesterActivation.kt`: dodaj
   ```kotlin
   /** A semester with the date ranges of its calendars, enough to pick a fallback. */
   data class SemesterSpan(val id: Long, val calendars: List<ClosedRange<LocalDate>>)

   /**
    * The semester to make active when none is: the one whose calendar covers [today], otherwise the one
    * that starts last, otherwise the first one without calendars.
    */
   fun fallbackActiveSemester(semesters: List<SemesterSpan>, today: LocalDate): Long? {
       val withCalendars = semesters.filter { it.calendars.isNotEmpty() }
       val covering = withCalendars.filter { span -> span.calendars.any { today in it } }
       return covering.ifEmpty { withCalendars }.maxByOrNull { span -> span.calendars.minOf { it.start } }?.id
           ?: semesters.firstOrNull()?.id
   }
   ```
2. `SyncPlanFile.semesterCovering(today)` zwraca `fallbackActiveSemester(semesters.map { SemesterSpan(it.semester.id, it.calendars.map { c -> c.startDate..c.endDate }) }, today)`.
3. `RoomSemesterRepository.deleteSemesterAndSelectFallback`: gdy po usunięciu nie ma aktywnego semestru, wybierz go przez `fallbackActiveSemester` z pozostałych semestrów i ich kalendarzy (w tej samej transakcji). Repozytorium dostaje `clock: Clock` w konstruktorze (zależność z grafu, bez wartości domyślnej), aby znać dzisiejszą datę.
4. `RoomPlanBackupGateway.replaceAll(data)`: gdy `data.activeSemesterId == null` i są semestry, przed wstawieniem ustaw aktywny `data.withActiveSemester(data.semesterCovering(LocalDate.now(clock)))`. Bramka dostaje `clock: Clock`. Funkcje `withActiveSemester` i `semesterCovering` są w `sync/SyncPlanFile.kt`; przenieś je do `data/repository/BackupData.kt` (te same nazwy), bo `data` nie może zależeć od `sync`, i popraw importy.
5. `SemesterRepository`: dodaj `suspend fun ensureActiveSemester()` wykonujące w transakcji to samo co punkt 3, gdy nie ma aktywnego semestru, a są semestry. `MakApplication.onCreate` woła je raz w `initializationScope` (dla danych z wcześniejszych wersji).
6. `AppViewModel`: usuń wywołanie `setActiveSemester(list.first().id)` z `init`.

Testy:

- `FallbackSemesterTest` (JVM): semestr obejmujący dziś wygrywa z późniejszym; bez obejmującego wygrywa najpóźniej zaczynający się; bez kalendarzy pierwszy; pusta lista daje `null`.
- `SyncCoordinatorTest`: istniejące testy wyboru aktywnego semestru przechodzą bez zmian.
- Testy `AppViewModel` (jeśli sprawdzały ustawienie aktywnego semestru) przenieś na repozytorium.
- `RoomPersistenceTest` (Android): nowe `deletingActiveSemesterActivatesTheOneCoveringToday` i `importWithoutActiveSemesterActivatesOne`.

Przypadki brzegowe: usunięcie nieaktywnego semestru nie zmienia aktywnego; import pliku z aktywnym semestrem zachowuje go; usunięcie ostatniego semestru zostawia brak aktywnego; widget i powiadomienia po imporcie bez aktywnego semestru od razu mają aktywny semestr.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.export.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.data.RoomPersistenceTest`, `dev.retza.mak.sync.RoomPlanSyncGatewayTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą; `rg "setActiveSemester\(list" app/src/main` nic nie znajduje; `semesterCovering` liczy przez `fallbackActiveSemester`. Commit: `chore: choose the fallback active semester with one rule in data (I-89)`.

## 3. Uporządkuj wspólne pomocniki dat, tygodni, kalendarzy, DataStore i kolorów (I-89)

Cel: formatery dat, ustawienia regionalne, parsowanie dat i godzin, początek tygodnia, kalendarz i kierunek przypisania, zawieranie daty w kalendarzu, odczyt DataStore z ponawianiem i liczenie koloru kierunku są w jednym miejscu każde (H2, H6, J2, B6). Wygląd i teksty się nie zmieniają.

Pliki: `ui/PlanMapping.kt`, `ui/AppLocale.kt`, `ui/SemesterCalendar.kt`, `domain/Models.kt`, `domain/WeekCalculator.kt`, `di/AppModule.kt`, `ui/settings/DataStoreSettingsPreferences.kt`, `update/UpdatePreferences.kt`, `ui/components/CourseColors.kt`, `ui/components/CourseColorDisplay.kt`, `ui/schedule/ScheduleScreen.kt` i pliki z prywatnymi kopiami wymienione niżej; testy zmienionych klas.

Zmiany:

1. Ustawienia regionalne: usuń `polishLocale` z `ui/PlanMapping.kt`; jego użycia (`PlanMapping.kt`, `ScheduleViewModel.kt`, `SyncDifferenceLabels.kt`, `TodayViewModel.kt`) przenieś na `AppLocale` z `ui/AppLocale.kt`. Każde `Locale.forLanguageTag("pl-PL")` i `Locale.forLanguageTag("pl")` w `app/src/main` przy formaterach zastąp `AppLocale` (`rg -n "forLanguageTag" app/src/main`).
2. Formatery: w `ui/PlanMapping.kt` obok istniejących dodaj `internal val dayMonthFormatter = "d MMMM"`, `internal val shortDateWithYearFormatter = "d MMM yyyy"`, `internal val longDateFormatter = "d MMMM yyyy"`, `internal val timeFormatter = "HH:mm"` (wszystkie z `AppLocale`, poza `HH:mm`). Prywatne kopie zastąp: `OccurrenceViewModel` (`EEEE, d MMMM yyyy` na `fullDateFormatter`), `SettingsViewModel.shortDateFormatter` (na `shortDateFormatter`), `SemesterViewModel` i `SemesterScreen` (`d MMM yyyy`, `d MMMM yyyy`), `AboutScreen.releaseDateFormat`, `SyncViewModel.changeDayFormatter` i `changeTimeFormatter`, `SyncDifferenceLabels` i `OccurrenceDetailsModels.changeDateFormatter`. Zostają: `MakFormControls.polishDateFormatter` (`dd.MM.yyyy`, pole daty), `SyncViewModel.displayFormatter` i `fileDate` (jedyne użycia swoich wzorców) oraz `domain/CollisionRanges.kt` (domena nie zależy od `ui`).
3. Parsowanie: w `ui/PlanMapping.kt` dodaj `internal fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()` i `internal fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()`. Usuń prywatne kopie w pakiecie `ui` (`ClassEditViewModel`, `ClassEditScreen`, `ScheduleViewModel`, `SemesterScreen`, `SemesterViewModel`, `OccurrenceViewModel`, `OccurrenceDetailsScreen`, `MakFormControls`, `SetupViewModel`, `SetupWizard`); przed usunięciem porównaj ciało każdej kopii (kilka ma kilka linii) i zachowaj zachowanie, jeśli się różni. `export/ExportImporter.kt` zostawia swoje kopie, bo `export` nie zależy od `ui`.
4. Początek tygodnia: w `domain/WeekCalculator.kt` dodaj funkcję najwyższego poziomu `fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))` i niech `WeekCalculator.weekStart(date)` ją woła. Użycia `previousOrSame(DayOfWeek.MONDAY)` w `ScheduleViewModel`, `SemesterViewModel` i `DemoDataSeeder` zastąp `.weekStart()`.
5. Kalendarz: w `domain/Models.kt` dodaj do `AcademicCalendar` `operator fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)` i użyj go w `ClassOccurrenceRule.kt`, `WeekCalculator.kt`, `ScheduleViewModel.kt` i `ClassEditViewModel.kt` (warunek z zaprzeczeniem zapisz jako `date !in calendar`). W `ui/SemesterCalendar.kt` dodaj `internal fun ActivePlanData.programForAssignment(assignmentId: String?): StudyProgram?` (przypisanie, potem kierunek z `courses`) i użyj w `ClassEditViewModel` (wyszukiwanie przypisania i kierunku), `OccurrenceViewModel.studyProgramName` i tam, gdzie `ScheduleViewModel` szuka kierunku przypisania. `SettingsViewModel`: `activeData?.calendars?.firstOrNull()` zastąp kalendarzem wspólnym (`sharedCalendar()` zmień z `private` na `internal`), aby karta ustawień pokazywała ten sam kalendarz co formularze.
6. DataStore: w `di/AppModule.kt` obok dostawcy dodaj `internal fun DataStore<Preferences>.readWithRetry(): Flow<Preferences>` z dzisiejszym `retryWhen` z `DataStoreSettingsPreferences` (emituje puste preferencje przy `IOException`, czeka 100 ms, ponawia). `DataStoreSettingsPreferences` i `DataStoreUpdatePreferences` czytają przez nie. Jeśli `AppModule.kt` nie jest dobrym miejscem dla rozszerzenia, utwórz `di/SettingsDataStore.kt` i przenieś tam też `SETTINGS_DATASTORE_NAME` i rozszerzenie `Context.settingsDataStore`.
7. Kolor kierunku: w komponentach, które wołają `courseShapeColor` albo `courseTextColor` w funkcji `@Composable` (`MakComponents.ClassCard`, `CourseNameText`, `MakCourseColorPicker.ThemePreview`, `MakColorDot`, `ScheduleScreen.courseMarkerTint`, `CourseColorDisplay`), owiń wynik w `remember(kolor, tło) { ... }`. Widget zostaje bez zmian.

Testy:

- `CourseColorsTest`, `PlanMappingTest`: istniejące przechodzą. Nowe w `PlanMappingTest`: `toLocalDateOrNullRejectsGarbage` i formatery `dayMonthFormatter` dla 7 października („7 października”).
- `WeekCalculatorTest`: `weekStartOfSundayIsPreviousMonday`.
- Nowy test `AcademicCalendar.contains` dla granic zakresu.
- `SettingsViewModelTest`: karta semestru pokazuje kalendarz o najniższym identyfikatorze, gdy semestr ma dwa kalendarze dodane w odwrotnej kolejności.
- Testy JVM ViewModeli przechodzą bez zmiany oczekiwań.

Przypadki brzegowe: teksty dat w każdym ekranie identyczne jak przed zmianą (ten sam wzorzec i `pl-PL`; `Locale.forLanguageTag("pl")` daje te same nazwy miesięcy co `pl-PL`, sprawdź zrzutem szczegółów przeniesionego terminu); błędny odczyt DataStore aktualizacji nie zatrzymuje aplikacji.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.update.*" --tests "dev.retza.mak.widget.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzuty przy 320 dp przed zmianą i po niej (przed testami Android): „Dzisiaj”, lista i kalendarz „Planu”, szczegóły przeniesionego terminu, „Ustawienia”, „Semestry”, ekran semestru, „O aplikacji”. Mają być identyczne.
4. Na emulatorze klasy `dev.retza.mak.ui.settings.SettingsPreferencesTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; `rg "polishLocale|forLanguageTag\(\"pl\"\)" app/src/main` nic nie znajduje; `rg "private fun String.toLocalDateOrNull" app/src/main/java/dev/retza/mak/ui` nic nie znajduje; `rg "previousOrSame\(DayOfWeek.MONDAY\)" app/src/main` znajduje tylko `WeekCalculator.kt`. Commit: `chore: share date, week, calendar, DataStore and color helpers (I-89)`.

## 4. Trzymaj rytm, typ tygodnia i motyw w stanie jako typy (I-89)

Cel: formularze trzymają w stanie `Recurrence`, `WeekTypeUi` i `ThemeMode`, a pola wyboru dostają wartości typów z jedną funkcją etykiety, zamiast odczytywać wybór z tekstu (H4). Cztery kopie pola „Tydzień A/B” i dwie kopie wyboru zakresu korekty zastępują komponenty `WeekTypeSelectField` i `WeekOverrideScopeChoice`. Wygląd i teksty się nie zmieniają.

Pliki: `ui/edit/ClassEditScreen.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/ClassOccurrenceRoutes.kt`, `ui/semester/SemesterScreen.kt`, `ui/setup/SetupWizard.kt`, `ui/setup/SetupViewModel.kt`, `ui/SetupRoutes.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsPreferences.kt`, nowy `ui/components/WeekFields.kt`; testy zmienionych ViewModeli i ekranów.

Zmiany:

1. Nowy `ui/components/WeekFields.kt`:
   - `fun WeekTypeUi.label(): String = "Tydzień $name"`;
   - `@Composable fun WeekTypeSelectField(label: String, value: WeekTypeUi, onSelected: (WeekTypeUi) -> Unit, modifier: Modifier = Modifier)` = `MakSelectField(label, value.label(), WeekTypeUi.entries, onSelected, modifier, optionLabel = { it.label() })`;
   - `fun WeekOverrideScopeUi.label(): String` (`ONE_WEEK -> "Tylko ten tydzień"`, `FROM_WEEK -> "Od tego tygodnia w przyszłość"`);
   - `@Composable fun WeekOverrideScopeChoice(selected: WeekOverrideScopeUi, onSelected: (WeekOverrideScopeUi) -> Unit)` z dzisiejszą pętlą `MakChoiceRow` z `SemesterScreen.WeekOverrideForm` (skopiuj układ i odstępy dokładnie stamtąd).
   `WeekTypeUi` i `WeekOverrideScopeUi` zostają w `SemesterScreen.kt` (albo przenieś je do `WeekFields.kt`, jeśli importy staną się prostsze; opisz w commicie).
2. Typ tygodnia: cztery pola z `listOf("Tydzień A", "Tydzień B")` (`SemesterScreen.CalendarDateFields`, `SemesterScreen.WeekOverrideForm`, `ScheduleScreen.WeekCorrectionDialog`, `SetupWizard.SemesterStep`) zastąp `WeekTypeSelectField`. `SetupWizardUiState.firstWeekLabel: String` zamień na `firstWeek: WeekTypeUi = WeekTypeUi.A` (również w `SetupSemesterResume`); `WeekType.valueOf(current.firstWeekLabel)` na `WeekType.valueOf(current.firstWeek.name)`; `SetupRoutes` przekazuje `WeekTypeUi`.
3. Zakres korekty: pętle w `SemesterScreen.WeekOverrideForm` i `ScheduleScreen.WeekCorrectionDialog` zastąp `WeekOverrideScopeChoice`.
4. Rytm zajęć: `ClassEditUiState` trzyma `recurrence: Recurrence = Recurrence.EVERY_WEEK` (domenowy) zamiast `recurrenceId` i `recurrenceLabel`. Funkcja `fun Recurrence.formLabel(): String` (`EVERY_WEEK -> "Co tydzień"`, `A_WEEK -> "Tydzień A"`, `B_WEEK -> "Tydzień B"`, `ONCE -> "Jednorazowo"`) w `ClassEditScreen.kt`. Pole: `MakSelectField(label = "Powtarzanie", value = state.recurrence.formLabel(), options = Recurrence.entries, onSelected = onRecurrenceChanged, optionLabel = { it.formLabel() })`. Usuń `RecurrenceOptionUi`, `classRecurrenceOptions`, `classEditRecurrenceLabel`, `classEditRecurrenceFromId` i mapowanie na napisy w `ClassEditViewModel`. Szkic w `SavedStateHandle` zapisuje `recurrence.name`; odczyt starego szkicu z kluczem `recurrenceId` (wartości `every_week`, `a_week`, `b_week`, `once`) zamień na `Recurrence` jedną funkcją, aby szkic sprzed aktualizacji się odtworzył.
5. Motyw: `SettingsUiState` dostaje `theme: ThemeMode`; pole motywu `MakSelectField(label = "Motyw", value = state.theme.label(), options = ThemeMode.entries, onSelected = onThemeSelected, optionLabel = { it.label() })` z `fun ThemeMode.label()` (`System -> "Systemowy"`, `Light -> "Jasny"`, `Dark -> "Ciemny"`) obok `ThemeMode`. `selectTheme` przyjmuje `ThemeMode`; usuń `themeModeFromId`, `themeOptions` i identyfikatory `"system"`, `"light"`, `"dark"`, jeśli nic poza tym ich nie używa.

Testy:

- `ClassEditViewModelTest`: testy rytmu sprawdzają `recurrence`; nowy `oldDraftWithRecurrenceIdIsRestored`.
- `SetupViewModelTest`: testy pierwszego tygodnia przechodzą na `firstWeek`.
- `SettingsViewModelTest`: wybór motywu przez `ThemeMode`.
- Testy Android ekranów (`ClassEditScreenTest`, `SemesterScreenTest`, `SetupWizardTest`, `ScheduleScreenTest`, `SettingsScreenTest`) przechodzą; popraw tylko wywołania z zmienionymi parametrami.

Przypadki brzegowe: zmiana rytmu na „Jednorazowo” dalej pokazuje pole daty; szkic formularza zajęć zapisany przez poprzednią wersję odtwarza rytm; motyw z DataStore bez zmian formatu zapisu.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzuty przy 320 dp przed zmianą i po niej (przed testami Android): formularz zajęć z rozwiniętym polem „Powtarzanie”, kreator krok 1 z polem „Pierwszy tydzień”, ekran semestru, formularz korekty tygodnia, dialog „Zmień tydzień A/B” w „Planie”, ustawienia z rozwiniętym polem „Motyw”. Mają być identyczne.
4. Na emulatorze klasy `dev.retza.mak.ui.edit.ClassEditScreenTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.setup.SetupWizardTest`, `dev.retza.mak.ui.schedule.ScheduleScreenTest` i `dev.retza.mak.ui.settings.SettingsScreenTest`.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; `rg "endsWith\(\"B\"\)|every_week|firstWeekLabel|themeModeFromId" app/src/main` znajduje tylko odczyt starego szkicu. Commit: `chore: keep recurrence, week type and theme as types in form state (I-89)`.

## 5. Waliduj semestr, kierunek i edycję terminu czystymi funkcjami (I-89)

Cel: reguły formularzy semestru, kierunku i edycji terminu są czystymi funkcjami domenowymi z kodami błędów, jak `validateClassForm` (H5, `ARCHITECTURE.md`, sekcja 7: „Walidacja formularza to czysta funkcja wywoływana z ViewModelu”). ViewModele zamieniają kody na teksty. Jedno brzmienie dla tej samej reguły.

Pliki: nowy `domain/FormValidation.kt`, `ui/semester/SemesterViewModel.kt`, `ui/setup/SetupViewModel.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/occurrence/OccurrenceViewModel.kt`, nowy `ui/FormErrorTexts.kt`; testy nowy `domain/FormValidationTest.kt` i testy zmienionych ViewModeli; dokument `docs/QUEUE.md`.

Zmiany:

1. `domain/FormValidation.kt`:
   ```kotlin
   enum class DateRangeError { START_REQUIRED, END_REQUIRED, END_BEFORE_START }
   fun validateDateRange(start: LocalDate?, end: LocalDate?): Set<DateRangeError>
   fun validateRequiredName(name: String): Boolean  // false when blank
   fun dateInCalendar(date: LocalDate, calendar: AcademicCalendar?): Boolean
   ```
   Jeśli walidacja edycji terminu w `OccurrenceViewModel.occurrenceDraftErrors` (data poza aktywnym semestrem, koniec nie później niż początek) nie ma odpowiednika w `domain/OccurrenceEdit.kt`, dodaj tam `enum class OccurrenceDraftError { DATE_OUTSIDE_SEMESTER, END_NOT_AFTER_START }` i `fun validateOccurrenceDraft(...)`.
2. `ui/FormErrorTexts.kt`: jedno miejsce tekstów: `"Podaj nazwę semestru."`, `"Podaj nazwę kierunku."`, `"Wybierz kierunek."`, `"Podaj poprawną datę."`, `"Koniec nie może być wcześniejszy od początku."`, `"Data musi należeć do kalendarza kierunku."`, `"Data musi należeć do aktywnego semestru."`, `"Koniec musi być późniejszy niż początek tego samego dnia."`. Kreator dziś mówi „Wybierz poprawną datę.” i „Data końca nie może być wcześniejsza od początku.”; ujednolić z ekranem semestru na „Podaj poprawną datę.” i „Koniec nie może być wcześniejszy od początku.” (audyt: brzmienie już się rozjechało). To jedyna zmiana tekstu w kroku; wymień ją w commicie.
3. `SemesterViewModel.saveSemester`, `saveCalendar`, `addCourse`; `SetupViewModel.advanceFromSemester` i `programRecordFor`; `ClassEditViewModel.save` (warunek daty w kalendarzu kierunku); `OccurrenceViewModel.occurrenceDraftErrors`: wołają funkcje z punktu 1 i teksty z punktu 2 zamiast własnych warunków.
4. `docs/QUEUE.md`: przy I-89 zapisz zrobione punkty (E2, E4, H2, H4, H5, H6, J2, B6) i że K1 trafia do następnego planu razem z I-90.

Testy:

- `FormValidationTest` (JVM): zakres dat (brak początku, brak końca, koniec przed początkiem, ten sam dzień poprawny), pusta nazwa ze spacjami, data na granicy kalendarza.
- `SetupViewModelTest`: oczekiwane teksty błędów dat zmieniają się na ujednolicone (jedyna zmiana oczekiwań).
- Pozostałe testy ViewModeli przechodzą bez zmian oczekiwań.

Przypadki brzegowe: semestr kończący się w dniu rozpoczęcia jest poprawny; nazwa ze spacjami to brak nazwy; kreator pokazuje błąd przy polu końca, jak dziś.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.setup.SetupWizardTest`, `dev.retza.mak.ui.semester.SemesterScreenTest` i `dev.retza.mak.ui.edit.ClassEditScreenTest`.

Kryterium zakończenia: testy przechodzą; `rg "Wybierz poprawną datę|Data końca nie może" app/src/main` nic nie znajduje; teksty błędów z punktu 2 występują w `app/src/main` tylko w `ui/FormErrorTexts.kt`. Commit: `chore: validate semester, program and occurrence forms with pure functions (I-89)`.
