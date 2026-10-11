# Plan najbliższych prac

Cel: wykonać wartościowe punkty przeglądu zmian z audytu z 2026-10-11 (I-91, I-92, I-94 w `QUEUE.md`). Plan powstał na polecenie użytkownika z 2026-10-11 („rozpisz plan, jeśli warto”). Najważniejszy jest krok 1: naprawia widoczny błąd tygodnia w „Planie” przy filtrze kierunku. Pozostałe kroki usuwają zbędną pracę (synchronizacja bez potrzeby, przebudowa całej aplikacji przy każdym znaku) i drobne kopie reguł.

## Zakres

W planie (punkty z wierszy `QUEUE.md`):

- I-91: punkty 1, 2, 3, 4, 6, 7, 8, 9, 10.
- I-92: punkty 1, 4, 5, 7.
- I-94: punkty 1, 4, 5 oraz 6 bez `OccurrenceStatusUi`.

Poza planem, odrzucone decyzją użytkownika z 2026-10-11 (`LOG.md`). Nie wykonuj ich:

- I-91 punkt 5 (wspólna reguła „wspólny kalendarz ma najniższy identyfikator”): trzy kopie działają na trzech różnych typach (`AcademicCalendar`, `AcademicCalendarRecord`, rekord w kreatorze). Wspólna funkcja wymagałaby abstrakcji większej niż kopie.
- I-92 punkty 2, 3, 6: zysk wydajności niezauważalny przy wielkości planu studenta, a punkt 3 wymaga nowej relacji Room.
- I-93 w całości: przebudowa dopuszczania edycji, które działa i nie ma znanego wyścigu, bez zmiany dla użytkownika. Opisana w przeglądzie różnica między lambdą `hasDraft` a kolektorem w `SemesterViewModel` nie jest błędem: kolektor woła `releaseIfNoDraft()`, a ta sprawdza lambdę z `courseFormOpen`. Wiersz usunięty z `QUEUE.md`.
- I-94 punkty 2 i 3: `ActivePlanSource` z trzema stanami i `TopBarSpec` w każdym celu nawigacji to zmiany w wielu plikach bez skutku dla użytkownika.
- I-94 punkt 6 w części `OccurrenceStatusUi`: ten typ ma własne nazwy, etykiety i ikony, więc nie jest kopią enuma domenowego z konwersją `valueOf`.
## Zasady dla wykonawcy

Przeczytaj tę sekcję przed każdym krokiem.

- Kroki wykonuj na gałęzi `task/I-79-hide-study-programs`, bo kod zależy od niescalonych zmian I-89 i I-90. Przed pierwszym krokiem sprawdź `git branch --show-current` i `git log --oneline origin/main -1`. Jeśli `origin/main` zawiera już scalone I-90 (commit `keep row ids as Long in ui state`), utwórz nową gałąź: `git fetch origin`, potem `git switch -c task/audit-review-fixes origin/main`. Jeśli jesteś na innej gałęzi i I-90 nie jest scalone, przełącz się poleceniem `git switch task/I-79-hide-study-programs`.
- Ścieżki kodu są względne wobec `app/src/main/java/dev/retza/mak/`, testów JVM wobec `app/src/test/java/dev/retza/mak/`, testów Android wobec `app/src/androidTest/java/dev/retza/mak/`.
- Przed zmianą otwórz każdy plik z listy kroku i znajdź opisany fragment po nazwie funkcji, nie po numerze wiersza. Jeśli fragmentu nie ma albo wygląda inaczej niż w opisie, zrób zmianę zgodną z celem kroku i opisz różnicę w commicie. Zatrzymaj się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Polecenia uruchamiaj w katalogu repozytorium w PowerShellu. Testy JVM jednej klasy: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.OccurrenceViewModelTest"`. Kilka klas: powtórz `--tests` dla każdej. Jeśli Gradle zgłosi `No cached version ... available for offline mode`, uruchom to samo polecenie raz bez `--offline`, potem wróć do `--offline`.
- Po każdej zmianie kodu: `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`. Kompilacja testów Android nie oznacza ich uruchomienia.
- Testy Android: najwyżej jeden emulator naraz, z 2048 MiB RAM. Obraz `Medium_Phone_A12` (`STACK.md`, sekcja 6); na komputerze, który go nie ma, `Medium_Phone` (testy używają aplikacji `dev.retza.mak.debug`, a dane odbioru synchronizacji są w `dev.retza.mak`; nie czyść ich). Jedna klasa na wywołanie: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.di.KoinGraphTest"`; lista klas po przecinku w PowerShellu uruchamia tylko pierwszą. Przed testami Android cofnij zmiany ekranu ze zrzutów (`adb shell wm size reset`). Testy Android przeinstalowują aplikację debug i zerują jej dane, więc ręczne sprawdzenie i zrzuty rób po `installDebug`, przed testami Android.
- Zrzuty przy 320 dp: procedura w `STACK.md`, sekcja 6. Zrzuty zapisuj poza repozytorium.
- Nie uruchamiaj pełnego zestawu testów aplikacji bez polecenia użytkownika.
- Każdy krok kończy się jednym commitem z podanym tematem, bez stopki `Co-authored-by`. Do commita dołącz zmiany `PLAN.md`, `QUEUE.md` i dokumentów, których dotyczy krok. Pod nagłówkiem zrobionego kroku dopisz linię `Stan: zrobione.` z krótkim opisem różnic wobec planu.
- Nie wypychaj gałęzi, nie otwieraj pull requesta i nie scalaj bez wyraźnego polecenia użytkownika.
- Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py`; wymagany kod 0.
- Teksty interfejsu i dokumentacja po polsku, identyfikatory i komentarze w kodzie po angielsku. Bez znaków `·`, `—` i strzałek.
- Nie używaj aliasów importu (`import x.Y as Z`). Gdy w jednym pliku testu potrzebne są dwa typy o tej samej nazwie (na przykład `dev.retza.mak.data.entity.WeekType` i `dev.retza.mak.domain.WeekType`), zostaw import tego, który już jest, a drugi pisz pełną nazwą (`dev.retza.mak.domain.WeekType.B`).

## 1. Pokazuj w „Planie” tydzień kalendarza wybranego kierunku (I-94)

Stan: zrobione. Wykonał subagent, recenzja i testy Android: agent planujący. Konwersje `valueOf` między typami encji i domeny w `data` zostały, bo to mapowania między warstwami. Zrzutów nie zrobiono: dane demo mają jeden kalendarz, a teksty wiersza tygodnia sprawdzają `ScheduleScreenTest` i nowy test JVM. Testy Android: `ScheduleScreenTest` 12/12, `SemesterScreenTest` 14/14, `SetupWizardTest` 11/11, `KoinGraphTest` 3/3.

Cel: wiersz tygodnia w „Planie” pokazuje tydzień kalendarza, którego dotyczy filtr kierunku, a nie wynik wszystkich kalendarzy. Dziś przy dwóch kalendarzach z różnym tygodniem i filtrze na jeden kierunek wiersz jest akcją „Zmień”, ale pokazuje „Różne tygodnie” i „Różne kalendarze”, a dialog korekty zaznacza zawsze A (`FEATURES.md`, sekcja „Widok „Lista””). Przy okazji `WeekStatus` staje się jedynym polem tygodnia w `ResolvedSchedule` (punkt 4), a stan interfejsu używa domenowych `WeekType` i `WeekOverrideScope` zamiast `WeekTypeUi` i `WeekOverrideScopeUi` (punkt 6). Punkt 4 dotyka pól, z których skorzysta I-81, więc idzie przed nim.

Pliki: `domain/Models.kt` (`ResolvedSchedule`), `domain/ScheduleResolver.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/semester/SemesterScreen.kt` (definicje `WeekTypeUi` i `WeekOverrideScopeUi`), `ui/semester/SemesterViewModel.kt`, `ui/semester/SemesterCourseScreens.kt`, `ui/components/WeekFields.kt`, `ui/setup/SetupViewModel.kt`, `ui/setup/SetupWizard.kt`; testy JVM `domain/MultiCalendarResolverTest.kt`, `domain/ScheduleResolverTest.kt`, `widget/WidgetPresenterTest.kt`, `ui/schedule/ScheduleViewModelTest.kt`, `ui/semester/SemesterViewModelTest.kt`, `ui/setup/SetupViewModelTest.kt`; testy Android `ui/schedule/ScheduleScreenTest.kt`, `ui/semester/SemesterScreenTest.kt`, `ui/setup/SetupWizardTest.kt`.

Zmiany:

1. Zanim zmienisz kod, zrób zrzuty przy 320 dp: „Plan” w widoku listy z filtrem „Wszystkie” i z filtrem na jeden kierunek, dialog „Zmień tydzień”, konfiguracja semestru z listą korekt, krok kalendarza w kreatorze. Będą porównaniem w punkcie 9.
2. Usuń `enum class WeekTypeUi` i `enum class WeekOverrideScopeUi` z `ui/semester/SemesterScreen.kt`. W całym `app/src` zamień `WeekTypeUi` na `dev.retza.mak.domain.WeekType`, a `WeekOverrideScopeUi` na `dev.retza.mak.domain.WeekOverrideScope` (importy `dev.retza.mak.ui.semester.WeekTypeUi` i `dev.retza.mak.ui.semester.WeekOverrideScopeUi` zamień na importy domenowe). Listę miejsc daje `rg -n "WeekTypeUi|WeekOverrideScopeUi" app/src`. Wartości są te same (`A`, `B`, `ONE_WEEK`, `FROM_WEEK`).
3. Usuń konwersje, które po punkcie 2 nic nie robią: `WeekTypeUi.valueOf(x.name)` i `WeekType.valueOf(x.name)` oraz `WeekOverrideScope.valueOf(scope.name)` zastąp samą wartością `x` (`rg -n "valueOf\(.*\.name\)" app/src/main/java/dev/retza/mak/ui` i przejrzyj każde trafienie; zostaw konwersje innych typów, na przykład `ClassEditOpenState.valueOf`). W `ScheduleViewModel.saveVisibleWeekOverride` i `clearVisibleWeekOverride` usuń zmienną `entityScope` i używaj parametru `scope`.
4. `ui/components/WeekFields.kt`: funkcje `WeekTypeUi.label()` i `WeekOverrideScopeUi.label()` stają się `WeekType.label()` i `WeekOverrideScope.label()` z tymi samymi tekstami. Sprawdź kompilację: jeśli nazwa `label` zderzy się z inną funkcją, zostaw nazwę i dodaj właściwy import.
5. `domain/Models.kt`, klasa `ResolvedSchedule`: usuń pola `week` i `hasMixedWeekTypes` oraz właściwości `weekType` i `correction`. `weekStatus` staje się zwykłym polem konstruktora `val weekStatus: WeekStatus` (bez wartości domyślnej), umieszczonym po `occurrences`. Zostają `date`, `semesterId`, `occurrences`, `cancelled`, `weeksByCalendarId` i `items`.
6. `domain/ScheduleResolver.kt`, funkcja `resolve`: po zbudowaniu `weeks` policz `val weekTypes = weeks.values.map { it.weekType }.distinct()` i `weekStatus`: `WeekStatus.OutsideSemester`, gdy `weekTypes` jest puste; `WeekStatus.Mixed`, gdy ma więcej niż jeden element; w przeciwnym razie `WeekStatus.Single(weekTypes.single())`. Usuń `hasMixedWeekTypes` i `sharedWeek`; przekaż `weekStatus` do konstruktora.
7. `ui/schedule/ScheduleViewModel.kt`, funkcja `buildSchedule`: po wyliczeniu `relevantCalendarIds` dodaj:
   - `val weekCalendarId = visibleWeekCalendarId(data, activeFilter)`,
   - `val shownWeek = weekCalendarId?.let { selected.weeksByCalendarId[it] }`,
   - `val relevantWeeks = selected.weeksByCalendarId.filterKeys { it in relevantCalendarIds }.values`.

   Przenieś obliczenie `relevantCalendarIds` wyżej, jeśli stoi za miejscem użycia. Pola stanu:
   - `weekTypeLabel`: gdy `weekCalendarId == null`, `selected.weekStatus.label()`; gdy `shownWeek == null`, `WeekStatus.OutsideSemester.label()`; w przeciwnym razie `WeekStatus.Single(shownWeek.weekType).label()`;
   - `weekSourceLabel`: „Różne kalendarze”, gdy `weekCalendarId == null` i `selected.weekStatus == WeekStatus.Mixed`; „Korekta ręczna”, gdy `relevantWeeks.any { it.overrideSource != null }`; w przeciwnym razie „Wyliczony automatycznie”;
   - `weekType = shownWeek?.weekType`.

   Bez filtra przy jednym kalendarzu `visibleWeekCalendarId` zwraca ten kalendarz, więc wynik się nie zmienia. Przy kilku kalendarzach i „Wszystkie” etykiety zostają jak dziś, z jedną różnicą: „Korekta ręczna” pojawia się, gdy korektę ma którykolwiek z kalendarzy, a nie tylko pierwszy.
8. Testy, które czytały usunięte pola: `assertTrue(x.hasMixedWeekTypes)` zastąp `assertEquals(WeekStatus.Mixed, x.weekStatus)`, `assertFalse(x.hasMixedWeekTypes)` razem z `assertEquals(WeekType.A, x.weekType)` zastąp `assertEquals(WeekStatus.Single(WeekType.A), x.weekStatus)`, `assertEquals(WeekType.B, result.weekType)` zastąp `assertEquals(WeekStatus.Single(WeekType.B), result.weekStatus)`, a `assertNull(x.week)` usuń. Miejsca: `rg -n "hasMixedWeekTypes|\.week\b|schedule\.weekType|result\.weekType" app/src/test`.
9. Zainstaluj aplikację (`installDebug`) i zrób te same zrzuty co w punkcie 1. Mają być identyczne, bo dane demo mają jeden kalendarz.

Testy:

- `ScheduleViewModelTest.filteredProgramShowsTheWeekOfItsCalendar`: `FakeRepository()`, drugi kierunek przez `repository.addSeparatedSemesterProgram(semesterId = 1L, studyProgram = StudyProgramEntity(name = "Fizyka", color = "#000000"), sourceCalendarId = 1L)` (wzór: `weekCanBeCorrectedOnlyWithOneVisibleCalendar`), potem przed utworzeniem ViewModelu `repository.weekOverrides += WeekOverrideEntity(id = 50L, semesterId = 1L, academicCalendarId = second.academicCalendarId, weekStartDate = LocalDate.of(2026, 9, 21), weekType = WeekType.A, scope = WeekOverrideScope.ONE_WEEK)`. W kalendarzu 1 tydzień od 2026-09-21 to B (pierwszy tydzień A zaczyna się 2026-08-31), a drugi kalendarz jest jego kopią, więc korekta A daje dwa różne tygodnie. Sprawdź kolejno: przy „Wszystkie” `weekTypeLabel == "Różne tygodnie"` i `weekSourceLabel == "Różne kalendarze"`; po `selectCourseFilter(second.semesterProgramId)` `weekTypeLabel == "Tydzień A"`, `weekSourceLabel == "Korekta ręczna"`, `weekType == dev.retza.mak.domain.WeekType.A`; po `selectCourseFilter(1L)` (przypisanie pierwszego kierunku w `FakeRepository`) `weekTypeLabel == "Tydzień B"` i `weekSourceLabel == "Wyliczony automatycznie"`.
- `ScheduleResolverTest.weekStatusReportsMixedAndOutsideSemester` zostaje bez zmian i ma przejść.

Przypadki brzegowe: filtr na kierunek, którego kalendarz nie obejmuje przeglądanego tygodnia, pokazuje „Poza semestrem” i „Wyliczony automatycznie”, a dialog zaznacza A (jak dziś przy braku tygodnia); „Dzisiaj” i widget używają dalej `weekStatus` wszystkich kalendarzy.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzuty z punktów 1 i 9 identyczne.
4. Na emulatorze klasy `dev.retza.mak.ui.schedule.ScheduleScreenTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.setup.SetupWizardTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; `rg "WeekTypeUi|WeekOverrideScopeUi|hasMixedWeekTypes" app/src` nic nie znajduje; `rg "valueOf\(" app/src/main/java/dev/retza/mak/ui/schedule` nic nie znajduje. Commit: `fix: show the week of the filtered calendar in plan (I-94)`. W tym commicie w `QUEUE.md` zmień status I-94 na `w toku`.

## 2. Nie uruchamiaj synchronizacji po echu pobranego planu i wybieraj semestr zastępczy tylko w data (I-92)

Stan: zrobione. Wykonał subagent, recenzja i testy Android: agent planujący. Bez odchyleń od planu. Testy Android: `RoomPlanSyncGatewayTest` 10/10, `SyncViewModelTest` 12/12, `KoinGraphTest` 3/3.

Cel: po pobraniu planu z Dysku zapis w Room wywołuje dziś `recordLocalChange`, który rozpoznaje echo, a mimo to `SyncRoomChangeObserver` zleca kolejny przebieg z zapytaniem do Dysku (I-92 punkt 7). Po zmianie echo nie zleca przebiegu. Semestr zastępczy wybiera tylko `replaceAll` w transakcji `data`; synchronizacja przekazuje najwyżej zachowany lokalny aktywny semestr (I-94 punkt 1). Reguła wyboru się nie zmienia (`SYNC.md`, „Przebieg synchronizacji”, punkt 2). `retainExistingHiddenPrograms` przechodzi z `ui` do `data` (I-94 punkt 5).

Pliki: `sync/SyncCoordinator.kt`, `sync/SyncWork.kt` (`SyncRoomChangeObserver`), `sync/PlanSyncGateway.kt`, `sync/PlanMerge.kt`, `ui/settings/HiddenStudyPrograms.kt` (przeniesienie do `data/HiddenStudyPrograms.kt`), `MakApplication.kt`; testy JVM `sync/SyncCoordinatorTest.kt`, `sync/PlanMergeTest.kt`, `sync/GoogleAuthorizationFailureTest.kt`, `ui/settings/HiddenStudyProgramsTest.kt` (przeniesienie do `data/HiddenStudyProgramsTest.kt`); testy Android `sync/RoomPlanSyncGatewayTest.kt`, `ui/settings/SyncViewModelTest.kt`.

Zmiany:

1. `SyncCoordinator.recordLocalChange(changedAtMillis: Long): Boolean`. Zwraca `false` bez konta i gdy skrót planu równa się `localFingerprint` (echo albo cofnięta zmiana); po zapisie czasu zwraca `true`. Uzupełnij komentarz KDoc o znaczenie wyniku: „true when the plan differs from the one last synchronized, so a run is due”.
2. `SyncRoomChangeObserver.observe`: zleć `scheduler.enqueueImmediate()` tylko wtedy, gdy `recordLocalChange` zwrócił `true`. Gdy rzuci wyjątek inny niż `CancellationException`, zleć przebieg jak dziś (nie wiadomo, czy plan się zmienił). Wzór:
   ```kotlin
   val due = try {
       coordinator.recordLocalChange(changedAt)
   } catch (error: CancellationException) {
       throw error
   } catch (_: Exception) {
       // Only a label in the version choice depends on the time; the run still checks the plan.
       true
   }
   if (due) scheduler.enqueueImmediate()
   ```
   Zaktualizuj KDoc klasy: przebieg startuje po zmianie planu, która nie jest echem ostatniej synchronizacji.
3. `PlanSyncGateway.replaceIfUnchanged`: usuń parametr `fallbackActiveId`. Funkcja `activeSemesterAfterReplace(localActiveId, data, keepLocalActive)` zwraca `localActiveId`, gdy `keepLocalActive` i semestr o tym numerze jest w `data`, w przeciwnym razie `null`. Plan bez aktywnego semestru dostaje semestr zastępczy w `RoomPlanBackupGateway.replaceAll` (tam już jest ta reguła). Popraw KDoc interfejsu: „otherwise no semester is marked active and `replaceAll` picks the fallback one in the same transaction”.
4. `SyncCoordinator`: w `download` usuń `fallbackActive` i przekaż trzy argumenty; w `merge` usuń `today` i argument `merged.semesterCovering(today)`; usuń import `semesterCovering`.
5. `sync/PlanMerge.kt`, funkcja `mergePlans`: usuń parametr `today`. Aktywny semestr: `data.withActiveSemester(activeId?.takeIf { merged.containsKey(PlanRowKey(PlanRowKind.SEMESTER, it)) })`. Bez takiego semestru plan nie ma aktywnego, a wybór robi `replaceAll` przy zapisie. Pole aktywnego semestru nie trafia do pliku na Dysku (`SyncPlanFile.shared`), więc skrót się nie zmienia. Usuń import `semesterCovering` i nieużywany import `LocalDate`, jeśli zostanie. W `SyncCoordinator.merge` usuń argument `today` z wywołania.
6. Przenieś `ui/settings/HiddenStudyPrograms.kt` do `data/HiddenStudyPrograms.kt` poleceniem `git mv`, zmień pakiet na `dev.retza.mak.data` i popraw import w `MakApplication.kt`. Tak samo przenieś test do `app/src/test/java/dev/retza/mak/data/HiddenStudyProgramsTest.kt` z pakietem `dev.retza.mak.data`.
7. Testy z własną implementacją `PlanSyncGateway` (`SyncCoordinatorTest.FakeGateway`, `GoogleAuthorizationFailureTest.EmptyPlan`, `SyncViewModelTest.EmptyPlan`): usuń parametr `fallbackActiveId`. W `FakeGateway.replaceIfUnchanged` po ustaleniu aktywnego semestru dodaj to, co robi `replaceAll`: gdy wynik nie ma aktywnego semestru, `withActiveSemester(semesterCovering(LocalDate.of(2026, 9, 30)))` (data zegara testu), z komentarzem „Like RoomPlanBackupGateway.replaceAll”. `PlanMergeTest`: usuń argument `TODAY` z wywołań `mergePlans`; stałą `TODAY` zostaw, jeśli służy do dat w danych.
8. `RoomPlanSyncGatewayTest`: w wywołaniach usuń `fallbackActiveId = ...`. Test `firstReplacementUsesTheFallbackSemester` zmień na `firstReplacementActivatesTheSemesterCoveringToday`: plan `plan("X", "Y", "Z")`, w którym semestry 1 i 3 dostają kalendarz od 2025-10-01 do 2026-01-31 (`copy` na `calendars`), a semestr 2 zostaje z kalendarzem obejmującym 2026-10-10 (dzień zegara `backup`). Oczekiwany aktywny semestr: 2.

Testy:

- `SyncCoordinatorTest.echoOfADownloadIsNotADueChange`: `drive.put(plan("Dysk"))`, `coordinator.synchronize()`, potem `assertFalse(coordinator.recordLocalChange(now))`.
- `SyncCoordinatorTest.localChangeIsDue`: bez synchronizacji `assertTrue(coordinator.recordLocalChange(now))` (wzór: `localChangesAreRecordedButNotTheEchoOfADownload`).
- Istniejące `firstDownloadActivatesTheSemesterOfToday` i `laterDownloadKeepsTheLocalActiveSemester` mają przejść.

Przypadki brzegowe: cofnięcie zmiany do planu z ostatniej synchronizacji nie zleca przebiegu; okresowa synchronizacja dalej sprawdza Dysk co godzinę; scalenie bez aktywnego semestru po stronie telefonu kończy się semestrem obejmującym dziś.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.data.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.sync.RoomPlanSyncGatewayTest`, `dev.retza.mak.ui.settings.SyncViewModelTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą; `rg "semesterCovering|fallbackActive" app/src/main/java/dev/retza/mak/sync` nic nie znajduje; `rg "retainExistingHiddenPrograms" app/src/main/java/dev/retza/mak/ui` nic nie znajduje. Commit: `fix: skip sync run for echo of a download (I-92, I-94)`. W tym commicie w `QUEUE.md` zmień status I-92 na `w toku`.

## 3. Sprawdzaj datę terminu kalendarzem kierunku i uprość edycję terminu (I-91)

Stan: zrobione. Wykonało dwóch subagentów (parsowanie dat oraz logika edycji), recenzja i testy Android: agent planujący. Test `ClassEditViewModelTest.saveRejectsOneOffDateOutsideSemester` ma nazwę `saveRejectsOneOffDateOutsideCalendar`, zgodną z regułą. Testy Android: `OccurrenceDetailsScreenTest` 7/7, `ClassEditScreenTest` 2/2, `KoinGraphTest` 3/3.

Cel: przeniesiony termin spoza kalendarza kierunku dostaje ten sam komunikat co zajęcia jednorazowe, „Data musi należeć do kalendarza kierunku.”, zamiast mylącego „Data musi należeć do aktywnego semestru.” (ViewModel i tak sprawdza zakres kalendarza kierunku). Edycja terminu i zajęć traci zbędne kroki: składanie i rozbieranie tekstu `"classId:date"`, własne parsowanie dat, ręczną normalizację tekstu i ponowne wczytanie planu po zapisie zajęć.

Pliki: `ui/PlanMapping.kt`, nowy `domain/IsoText.kt`, `domain/OccurrenceEdit.kt`, `domain/SemesterActivation.kt`, `ui/FormErrorTexts.kt`, `ui/occurrence/OccurrenceArgs.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/edit/ClassEditViewModel.kt` oraz pliki importujące `dev.retza.mak.ui.toLocalDateOrNull` lub `dev.retza.mak.ui.toLocalTimeOrNull` (`rg -l "ui.toLocal(Date|Time)OrNull" app/src`); testy JVM `domain/FormValidationTest.kt`, `ui/PlanMappingTest.kt`, nowy `domain/IsoTextTest.kt`, `ui/occurrence/OccurrenceArgsTest.kt`, `ui/occurrence/OccurrenceViewModelTest.kt`, `ui/edit/ClassEditViewModelTest.kt`.

Zmiany:

1. Utwórz `domain/IsoText.kt` z publicznymi funkcjami `fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(trim()) }.getOrNull()` i `fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(trim()) }.getOrNull()`, z KDoc: „ISO text from a form or a route; surrounding spaces are ignored, any other text gives null.” Usuń obie funkcje z `ui/PlanMapping.kt` i zmień importy w plikach z listy na `dev.retza.mak.domain.toLocalDateOrNull` i `dev.retza.mak.domain.toLocalTimeOrNull`. Prywatnych kopii w `export/ExportImporter.kt` nie ruszaj: import pliku ma zostać ścisły.
2. `domain/OccurrenceEdit.kt`: usuń prywatne `parseDate` i `parseTime`; `decideOccurrenceEdit` używa `draftDate.toLocalDateOrNull()` i `draftStartTime.toLocalTimeOrNull()`. Zmień `private fun normalizeNote` na `internal fun normalizeNote`.
3. `domain/OccurrenceEdit.kt`: `OccurrenceDraftError.DATE_OUTSIDE_SEMESTER` zmień na `DATE_OUTSIDE_CALENDAR`. `validateOccurrenceDraft(slot: OccurrenceSlot, calendar: AcademicCalendar?)` dodaje ten błąd, gdy `!dateInCalendar(slot.date, calendar)`. Usuń KDoc o granicach semestru i napisz: „Without a calendar the date is not restricted, like [dateInCalendar].”
4. `ui/FormErrorTexts.kt`: usuń `DateOutsideSemester`.
5. `OccurrenceViewModel`, funkcja `saveOccurrenceChange`: przed `occurrenceDraftErrors` ustal `val calendar = data.calendarForAssignment(data.classes.firstOrNull { it.id == classId }?.semesterProgramId)`. Funkcja `occurrenceDraftErrors(slot: OccurrenceSlot, calendar: AcademicCalendar?)` woła `validateOccurrenceDraft(slot, calendar)` i dla `DATE_OUTSIDE_CALENDAR` wstawia `FormErrorTexts.DateOutsideCalendar`. Pola stanu `semesterStartDate` i `semesterEndDate` zostają: ograniczają wybór daty w dialogu.
6. `OccurrenceViewModel`: w `saveSharedNote` i `saveOccurrenceNote` zamiast `draft.xxxNoteDraft.trim().ifEmpty { null }` użyj `normalizeNote(draft.xxxNoteDraft)`; w `buildDetails` zamiast `base.room?.trim()?.ifEmpty { null }` użyj `normalizeRoom(base.room)`.
7. `ui/occurrence/OccurrenceArgs.kt`: usuń `toRouteId()` i `parse(routeId)`. Dodaj w obiekcie towarzyszącym `fun from(classId: String?, date: String?): OccurrenceArgs?`, które zwraca `null`, gdy `classId?.toLongOrNull()` albo `date?.toLocalDateOrNull()` jest `null`.
8. `OccurrenceViewModel`:
   - `init` tworzy argumenty przez `OccurrenceArgs.from(savedState.get<String>("classId"), savedState.get<String>("date"))`;
   - `shownRouteId()` zastąp `private fun shownArgs(): OccurrenceArgs?`, które bierze `selectedClassIdState.value` i `details.value.baseDate.toLocalDateOrNull()` (bez `runCatching`);
   - `refreshForEdit(routeId: String)` zastąp `refreshForEdit(args: OccurrenceArgs)`;
   - `refreshBase` sesji edycji woła `shownArgs()?.let { refreshForEdit(it) } ?: false`.
9. `ClassEditViewModel`:
   - `openEditFromFreshPlan(occurrenceId: String)` zastąp `openEditFromFreshPlan(classId: Long)`; `openOnce` woła `openEditFromFreshPlan(classId)`, gdy `classId != null && date != null`, i nie importuje już `OccurrenceArgs`;
   - import `dev.retza.mak.domain.Recurrence as DomainRecurrence` zastąp zwykłym `import dev.retza.mak.domain.Recurrence` i zmień w pliku `DomainRecurrence` na `Recurrence`;
   - w `save` pole `recurrence = DomainRecurrence.valueOf(recurrence.name)` zmień na `recurrence = recurrence`;
   - po `scheduleRepository.saveClass(record)` usuń `val savedPlanData = freshActivePlanData()`, a stan ustaw jako `withActiveOptions(defaultClassEditState())`. Formularz i tak się zamyka efektem `CloseEditor`.
10. `domain/SemesterActivation.kt`, `shouldActivateNewSemester`: warunek zakresu zapisz jako `today in calendarStart..calendarEnd`.

Testy:

- `IsoTextTest`: przenieś z `PlanMappingTest` test `toLocalDateOrNullRejectsGarbage` (pakiet `dev.retza.mak.domain`) i dodaj `surroundingSpacesAreIgnored`: `" 2026-10-07 ".toLocalDateOrNull()` daje 2026-10-07, a `" 08:15".toLocalTimeOrNull()` daje 08:15.
- `FormValidationTest`: `occurrenceDateOutsideSemesterIsReported` zmień na `occurrenceDateOutsideCalendarIsReported` z kalendarzem jak w `dateOnCalendarBoundsIsAccepted` i oczekiwanym `DATE_OUTSIDE_CALENDAR`; `missingSemesterBoundsDoNotRestrictOccurrenceDate` zmień na `missingCalendarDoesNotRestrictOccurrenceDate` z `validateOccurrenceDraft(slot(...), null)`; w `occurrenceEndNotAfterStartIsReported` przekaż `null` jako kalendarz.
- `OccurrenceViewModelTest.outOfSemesterDateIsRejectedWithoutSaveOrFeedback` zmień na `dateOutsideProgramCalendarIsRejectedWithoutSaveOrFeedback` i dodaj asercję tekstu: `assertEquals("Data musi należeć do kalendarza kierunku.", viewModel.details.value.draftErrors[OccurrenceEditField.Date]?.message)` (nazwę pola tekstu sprawdź w `FieldErrorUi`). Data 2026-12-01 leży poza kalendarzem 1 w `FakeRepository` (2026-09-01 do 2026-10-31).
- `OccurrenceViewModelTest`: pomocnik `open(routeId: String)` buduje argumenty przez `OccurrenceArgs.from(routeId.substringBefore(':'), routeId.substringAfter(':', ""))`; `refreshForEdit("1:2026-09-21")` zmień na `refreshForEdit(OccurrenceArgs(1L, LocalDate.of(2026, 9, 21)))`.
- `OccurrenceArgsTest`: `fromRejectsInvalidInput` (`from("nope", "2026-10-05")`, `from("42", "not-a-date")`, `from(null, "2026-10-05")` i `from("42", null)` dają `null`) i `fromReadsRouteArguments` (`from("7", "2026-10-05") == OccurrenceArgs(7L, LocalDate.of(2026, 10, 5))`).
- `ClassEditViewModelTest`: `openEditFromFreshPlan("1:2026-09-21")` zmień na `openEditFromFreshPlan(1L)`, a `"2:2026-09-21"` na `2L`.

Przypadki brzegowe: wpisana data ze spacją na końcu jest przyjmowana jak bez spacji; zajęcia, których kierunek nie ma kalendarza, przyjmują każdą datę (jak `dateInCalendar` dla `null`); argument trasy niebędący liczbą albo datą daje stan „nie znaleziono”.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`, `dev.retza.mak.ui.edit.ClassEditScreenTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą; `rg "DateOutsideSemester|DATE_OUTSIDE_SEMESTER|toRouteId|DomainRecurrence|parseDate|parseTime" app/src` nic nie znajduje; `rg "runCatching \{ LocalDate" app/src/main/java/dev/retza/mak/ui/occurrence` znajduje tylko `OccurrenceDetailsModels.kt`. Commit: `fix: check occurrence dates against the program calendar (I-91, I-92)`. W tym commicie w `QUEUE.md` zmień status I-91 na `w toku`.

## 4. Nie przebudowuj całej aplikacji przy każdym znaku w szczegółach terminu (I-92)

Stan: zrobione. Wykonał subagent, recenzja i testy Android: agent planujący. Bez odchyleń w kodzie. Zrzutu i ręcznego sprawdzenia menu nie zrobiono; wygląd paska się nie zmienił, a menu akcji sprawdzają testy. Testy Android: `OccurrenceDetailsScreenTest` 7/7, `ClassEditScreenTest` 2/2, `MakNavigationChromeTest` 4/4.

Cel: `occurrenceDetailsRoute` tworzy przy każdej rekompozycji nową listę akcji z nowymi lambdami, więc `TopBarSpec` jest za każdym razem inny, a `MakApp` czyta go w ciele funkcji. Każdy wpisany znak notatki przebudowuje cały `Scaffold` aplikacji. Po zmianie specyfikacja zmienia się tylko razem z dostępnymi akcjami, a pasek czyta ją we własnym slocie (I-92 punkt 5). Wygląd i działanie bez zmian.

Pliki: `ui/ClassOccurrenceRoutes.kt` (`occurrenceDetailsRoute`), `ui/MakNavHostApp.kt` (`MakApp`).

Zmiany:

1. W `occurrenceDetailsRoute` zbuduj `actions` przez `remember(details.canCancelOccurrence, details.canEditBaseClass, details.canDeleteBaseClass) { listOfNotNull(...) }` z tymi samymi trzema pozycjami. Lambda „Edytuj wszystkie terminy” czyta datę w chwili kliknięcia: `occurrenceViewModel.details.value.targetDateDraft`, a nie zmienną `details` z kompozycji. Referencje `occurrenceViewModel::cancelOccurrence` i `occurrenceViewModel::requestClassDeletion` zastąp lambdami `{ occurrenceViewModel.cancelOccurrence() }` i `{ occurrenceViewModel.requestClassDeletion() }` wewnątrz `remember`, aby lista była tą samą instancją.
2. `MakApp`: zmień nazwę zmiennej `topBar` na `topBarState` (przekazywana do `classEditRoute` i `occurrenceDetailsRoute` jako `topBar = topBarState`). Usuń z ciała funkcji `topBarSpec` i `topBarActions`. W slocie `topBar = { ... }` w `Scaffold` odczytaj `val spec = topBarState.specFor(currentRoute)` i `val actions = spec?.actions.orEmpty()`, a resztę `MakTopBar` zostaw bez zmian.

Testy: zachowanie się nie zmienia, więc nie ma nowego testu JVM. Istniejące `TopBarStateTest` ma przejść.

Przypadki brzegowe: po odwołaniu terminu znika akcja „Odwołaj termin” (zmienia się klucz `remember`); „Edytuj wszystkie terminy” otwiera formularz z datą terminu jak dotąd.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.TopBarStateTest" --tests "dev.retza.mak.ui.MakNavigationTest"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzut przy 320 dp szczegółów terminu z otwartym menu akcji, przed zmianą i po niej; mają być identyczne.
4. Na emulatorze klasy `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`, `dev.retza.mak.ui.edit.ClassEditScreenTest` i `dev.retza.mak.ui.MakNavigationChromeTest`.
5. Ręcznie po `installDebug`: otwórz szczegóły terminu, wpisz kilka znaków w notatce, otwórz menu akcji i wybierz „Edytuj wszystkie terminy”; formularz zajęć otwiera się z datą terminu.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; `rg "specFor" app/src/main` znajduje jedno wywołanie, wewnątrz slotu `topBar` w `MakNavHostApp.kt`. Commit: `fix: keep top bar spec stable while typing (I-92)`.

## 5. Usuń pozostałe kopie i nieużywany kod z przeglądu (I-91)

Stan: zrobione. Wykonało dwóch subagentów (formularze i etykieta kalendarza oraz martwy kod, synchronizacja i widget), recenzja, kolejka i testy Android: agent planujący. Bez odchyleń od planu. Testy Android: `SemesterScreenTest` 14/14, `SettingsScreenTest` 14/14, `SetupWizardTest` 11/11, `KoinGraphTest` 3/3.

Cel: drobne porządki bez zmiany zachowania: jedno mapowanie błędów zakresu dat, jedna etykieta zakresu kalendarza, brak martwego kodu. Na końcu kolejka zamyka I-91, I-92 i I-94.

Pliki: `ui/FormErrorTexts.kt`, `domain/FormValidation.kt`, `ui/semester/SemesterViewModel.kt`, `ui/semester/SemesterScreen.kt`, `ui/setup/SetupViewModel.kt`, `ui/programs/StudyProgramsViewModel.kt`, `ui/MakRoutes.kt`, `ui/ClassOccurrenceRoutes.kt`, `export/ExportSnapshot.kt`, `ui/settings/SettingsScreen.kt`, `ui/components/MakComponents.kt`, `sync/SyncCoordinator.kt`, `widget/WidgetLayoutPolicy.kt`; testy JVM `domain/FormValidationTest.kt`, `ui/MakNavigationTest.kt`; test Android `ui/semester/SemesterScreenTest.kt`.

Zmiany:

1. `ui/FormErrorTexts.kt`: dodaj `data class DateRangeErrorTexts(val start: String?, val end: String?, val range: String?)` i w obiekcie `FormErrorTexts` funkcję `fun dateRange(errors: Set<DateRangeError>): DateRangeErrorTexts`: `start` to `DateRequired`, gdy jest `START_REQUIRED`; `end` to `DateRequired`, gdy jest `END_REQUIRED`; `range` to `EndBeforeStart`, gdy jest `END_BEFORE_START`; inaczej `null`.
2. `SemesterViewModel.saveSemester` i `saveCalendar`: zamiast trzech wyrażeń `if (DateRangeError.X in dateErrors) ...` użyj `val texts = FormErrorTexts.dateRange(dateErrors)` i pól `startDateError = texts.start`, `endDateError = texts.end`, `dateRangeError = texts.range`. `SetupViewModel.advanceFromSemester`: `texts.start` do `SetupField.StartDate`, a `texts.range ?: texts.end` do `SetupField.EndDate` (jak dziś: błąd kolejności nadpisuje brak daty, oba naraz nie występują).
3. Usuń `validateRequiredName` z `domain/FormValidation.kt` i test `blankNameIsRequired`. Wywołania w `SemesterViewModel` i `SetupViewModel` zastąp `isNotBlank()` na tym samym tekście (`!validateRequiredName(x)` staje się `x.isBlank()`). `StudyProgramsViewModel` już używa `isBlank()`.
4. Etykieta zakresu kalendarza: w `ui/semester/SemesterViewModel.kt` zamień prywatną `calendarLabel(calendar: AcademicCalendarRecord?)` na `internal fun calendarRangeLabel(start: LocalDate, end: LocalDate): String` z tym samym formatem (`shortDateWithYearFormatter`, separator `" - "`). Tam, gdzie była `calendarLabel(calendar)`, wstaw `calendar?.let { calendarRangeLabel(it.startDate, it.endDate) }.orEmpty()`. Do `SemesterCalendarUi` dodaj pole `val rangeLabel: String` (bez wartości domyślnej) wypełniane przez `calendarRangeLabel(calendar.startDate, calendar.endDate)`. W `SemesterScreen.kt` trzy miejsca `"${calendar.startDate.asCalendarDate()} - ${calendar.endDate.asCalendarDate()}"` zastąp `calendar.rangeLabel` i usuń `asCalendarDate`. W `SemesterScreenTest` pomocnik `calendar(...)` przekazuje `rangeLabel = calendarRangeLabel(LocalDate.parse(startDate), LocalDate.parse(endDate))`, więc asercje z tekstem „1 paź 2026 - 28 lut 2027” i „1 lis 2026 - 15 mar 2027” zostają bez zmian. Jeśli `internal` nie jest widoczne w testach Android, zrób `calendarRangeLabel` publiczną.
5. Usuń `semesterRoute` z `ui/MakRoutes.kt` i jego asercję z `MakNavigationTest.routeArgumentsPreserveOccurrenceAndEditorContext`.
6. Usuń nieużywane importy: `ui/ClassOccurrenceRoutes.kt`: `dev.retza.mak.ui.occurrence.hasPlanDraft`; `export/ExportSnapshot.kt`: `dev.retza.mak.data.repository.SemesterRecord`; `ui/settings/SettingsScreen.kt`: `androidx.compose.runtime.getValue` i `dev.retza.mak.text.polishPlural`; `ui/components/MakComponents.kt`: `androidx.compose.material3.DropdownMenu`, `androidx.compose.material.icons.filled.ArrowDropDown`, `androidx.compose.material.icons.outlined.ArrowBack`, `androidx.compose.material.icons.outlined.ArrowForward`, `androidx.compose.material.icons.outlined.MoreVert` i `dev.retza.mak.ui.theme.MakInk`. W `MakComponents.kt` zostaw `getValue` i `setValue`: używa ich `by remember`. Jeśli kompilacja zgłosi brak symbolu, przywróć tylko ten import.
7. `SyncCoordinator`: `private fun issue(issue: SyncIssue, message: String? = null)`. Usuń `authorizationRequired()` i `failed()`; ich wywołania zastąp `issue(SyncIssue.AUTHORIZATION_REQUIRED)` i `issue(SyncIssue.FAILED)`, a `message = null` w pozostałych wywołaniach usuń.
8. `widget/WidgetLayoutPolicy.kt`: usuń parametr konstruktora `includeTeacher` i trzy przypisania `includeTeacher = wide`. Dodaj do klasy `val wide: Boolean` jako parametr konstruktora i właściwość `val includeTeacher: Boolean get() = wide` z komentarzem „The teacher fits only next to the room in a wide widget.” W `widgetLayoutPolicy` przekaż `wide = wide` w trzech gałęziach.
9. `docs/QUEUE.md`: I-91, I-92 i I-94 przenieś do tabeli „Zakończone” ze statusem `gotowe`. Każdy wiersz wymienia zrealizowane punkty, uruchomione testy JVM i klasy testów Android oraz zachowuje już zapisaną adnotację o punktach pominiętych decyzją użytkownika.

Testy:

- `SemesterViewModelTest.calendarRangeLabelUsesShortDatesWithYear`: `assertEquals("1 lis 2026 - 15 mar 2027", calendarRangeLabel(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 15)))`. Ta reguła była dotąd sprawdzana tylko w `SemesterScreenTest`.
- Istniejące testy formularzy (`SemesterViewModelTest`, `SetupViewModelTest`), `SyncCoordinatorTest`, testy widgetu i `MakNavigationTest` przechodzą bez zmian asercji poza punktami 3 i 5.

Przypadki brzegowe: komunikaty błędów dat w semestrze i kreatorze są identyczne jak przed zmianą; widget wąski nadal nie pokazuje prowadzącego.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.widget.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.settings.SettingsScreenTest`, `dev.retza.mak.ui.setup.SetupWizardTest` i `dev.retza.mak.di.KoinGraphTest`.
4. `py scripts/check_map.py` z kodem 0.

Kryterium zakończenia: testy przechodzą; `rg "validateRequiredName|semesterRoute\(|asCalendarDate|fun failed\(|fun authorizationRequired\(" app/src` nic nie znajduje; `rg "includeTeacher = " app/src/main` nic nie znajduje. Commit: `chore: remove leftovers from the audit review (I-91)`.
