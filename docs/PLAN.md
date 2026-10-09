# Plan najbliższych prac

Cel: I-80 (zajęcia bez przerwy jako kolizja), I-83 (komunikat o planie z nowszej wersji MAK) i I-79 (ukrywanie kierunków) na jednej gałęzi `task/collisions-version-notice-course-toggles`, w kolejności kroków poniżej. Decyzje użytkownika z 2026-10-09 są w `DOMAIN.md` (sekcje „Kolizje” i „Kierunki”), `FEATURES.md` (sekcja „Ustawienia i dane”), `SYNC.md` (sekcja „Plik na Dysku”) i `LOG.md`; makiety na stronie „Decyzje” artefaktu „MAK: dni wolne” (https://claude.ai/artifact/8vbdCFuK7duXsocfhzE7ap).

Wspólna zasada: każdy widok planu (`TodayViewModel`, `ScheduleViewModel`, `OccurrenceViewModel`), widget (`WidgetPlanLoader`) i powiadomienia (`CollisionNotificationPlanner`, `CollisionAlarmScheduler`, `CollisionAlarmReceiver`) biorą dane planu z `ActivePlanSource` jako `ActivePlanInputs` (dane razem z `PlanDisplaySettings`) i liczą plan przez `ActivePlanProvider.resolve(inputs, date)`. Ustawienie wpływające na wynik (minimalna przerwa, ukryte kierunki) trafia do `PlanDisplaySettings` i `SettingsPreferences.planDisplay`, a nie do osobnych parametrów w każdym miejscu.

## 1. Uporządkuj kod I-80 i I-83 po przeglądzie (I-80)

Stan: zrobione. I-80 (kroki „wykryj kolizje bez nakładania” i „ustawienie minimalnej przerwy”) i I-83 są w kodzie; ich opis jest w `QUEUE.md` i `LOG.md`. Przegląd kodu z 2026-10-09 i decyzja użytkownika zmieniły część tamtych ustaleń:

- `Collision` jest typem zamkniętym `Collision.Overlap` i `Collision.NoBreak` (z `breakMinutes`) zamiast pola `kind`; zakres godzin formatuje jedna funkcja `rangeLabel`; `hasCollision` usunięte.
- `ActivePlanSource` (`ui/ActivePlanSource.kt`) łączy `observeActivePlanData` z `SettingsPreferences.planDisplay`; `CollisionNotificationPlanner.plan(inputs, settings)`. Siatka kalendarza w „Planie” używa `ActivePlanProvider.schedule(data, date)` bez liczenia kolizji.
- `registerMakWidgetRefresh` odświeża widget po zmianie Room i `planDisplay`.
- Synchronizacja: jedno pole `SyncIssueFix` (`RECONNECT`, `UPDATE`) w `SyncUiState.issueFix` i `SyncAttentionUi.fix` zamiast `needsReconnect`, `needsUpdate` i `opensUpdate`; `SyncCoordinator` łapie `NewerExportVersionException` bez osobnego `NewerRemotePlanException`.
- Ustawienia mają jeden typ opcji `SettingsOptionUi`.

## 2. Filtruj plan według ukrytych kierunków (I-79)

Cel: lokalny wybór ukrytych kierunków usuwa ich zajęcia ze wszystkich widoków, widgetu, kolizji, okienek i powiadomień, a synchronizacja i import nie przenoszą go na złe kierunki.

Pliki: `domain/PlanDisplaySettings.kt`, `domain/ActivePlanProvider.kt`, `ui/settings/SettingsPreferences.kt`, `ui/settings/DataStoreSettingsPreferences.kt`, `ui/schedule/ScheduleViewModel.kt`, `sync/PlanSyncGateway.kt`, `export/PlanBackupService.kt`, `ui/programs/StudyProgramsViewModel.kt` i testy.

Zmiany:

1. `PlanDisplaySettings` dostaje `hiddenProgramIds: Set<String>` (domyślnie pusty w `DEFAULT`).
2. Funkcja `ActivePlanData.visibleTo(display: PlanDisplaySettings): ActivePlanData` w `ActivePlanProvider.kt`: usuwa `semesterPrograms` z `studyProgramId` w `hiddenProgramIds` i `classes` z `semesterProgramId` usuniętych przypisań; pozostałe pola bez zmian. `ActivePlanProvider.resolve` wywołuje ją przed rozwiązaniem planu.
3. `SettingsPreferences`: `planDisplay` łączy minimalną przerwę z `stringSetPreferencesKey("hidden_study_program_ids")`; nowe funkcje `setStudyProgramHidden(id: String, hidden: Boolean)`, `clearHiddenStudyPrograms()` i `retainHiddenStudyPrograms(existingIds: Set<String>)`.
4. `ScheduleViewModel` buduje cały stan (opcje pola „Kierunek”, odwołane terminy, kalendarze filtra i znaczniki siatki kalendarza z `ActivePlanProvider.schedule`) z `inputs.data.visibleTo(inputs.display)`, aby ukryty kierunek nie był opcją filtra ani znacznikiem dnia.
5. `RoomPlanSyncGateway.replaceIfUnchanged` przyjmuje `SettingsPreferences`; po udanej podmianie wywołuje `clearHiddenStudyPrograms()`, gdy `keepLocalActive == false` (pierwsze pobranie), a w przeciwnym razie `retainHiddenStudyPrograms(data.studyPrograms.map { it.id.toString() }.toSet())`. `PlanBackupService.confirmImport` po imporcie wywołuje `clearHiddenStudyPrograms()`. Po udanym usunięciu kierunku `StudyProgramsViewModel.confirmDelete` wywołuje `setStudyProgramHidden(id, false)`.

Testy:

- `ActivePlanProviderTest`: ukryty kierunek nie ma zajęć w planie, nie tworzy kolizji ani okienek; pusty zbiór niczego nie zmienia; ukrycie wszystkich daje pusty plan.
- `ScheduleViewModelTest`: ukryty kierunek nie jest opcją filtra.
- `SettingsPreferencesTest`: zapis, czyszczenie i zachowanie tylko istniejących numerów.
- `RoomPlanSyncGatewayTest`: pierwsze pobranie czyści wybór, kolejne zachowuje istniejące numery.
- `WidgetPlanLoaderTest` i `CollisionNotificationPlannerTest`: ukryty kierunek pominięty.

Przypadki brzegowe: zajęcia ukrytego kierunku przeniesione na inny dzień też znikają; formularz zajęć i eksport nadal widzą wszystkie kierunki, bo czytają `observeActivePlanData` bez `visibleTo`.

Weryfikacja: filtrowane testy JVM wymienionych klas; kompilacja testów Android; `RoomPlanSyncGatewayTest` i `SettingsPreferencesTest` na emulatorze.

Kryterium zakończenia: testy przechodzą, a ręcznie zapisany wybór w preferencjach ukrywa zajęcia na „Dzisiaj”, w „Planie” i na widgecie. Commit: `feat: hide study programs from the plan on this phone (I-79)`.

## 3. Dodaj checkboxy kierunków i stany ukrycia w interfejsie (I-79)

Cel: użytkownik ukrywa kierunek checkboxem, formularz zajęć ostrzega przed wyborem ukrytego kierunku, a „Dzisiaj” i „Plan” wyjaśniają, dlaczego zajęć jest mniej.

Pliki: `ui/programs/StudyProgramsViewModel.kt`, `ui/programs/StudyProgramsScreen.kt`, `ui/StudyProgramRoutes.kt`, `ui/components/MakComponents.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/edit/ClassEditScreen.kt`, `ui/today/TodayViewModel.kt`, `ui/today/TodayScreen.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/MakNavHostApp.kt` i testy.

Zmiany:

1. `StudyProgramUi` dostaje `isHidden`; `StudyProgramsViewModel` łączy listę kierunków z `preferences.planDisplay` i ma `setVisible(id: Long, visible: Boolean)`.
2. `StudyProgramsScreen`: tekst wstępu „Odznacz kierunek, aby ukryć jego zajęcia na tym telefonie. Nazwa i kolor są wspólne dla wszystkich semestrów.”; `StudyProgramRow` ma na początku `Checkbox` w polu 48 dp z opisem „Pokazuj {nazwa}” i rolą pola wyboru; reszta wiersza (kropka, nazwa, strzałka) pozostaje jednym przyciskiem „{nazwa}, edytuj”; ukryty kierunek ma pod nazwą tekst „Ukryty na tym telefonie” w kolorze `onSurfaceVariant`.
3. `MakSelectField` dostaje opcjonalny `optionTrailing: (@Composable (T) -> Unit)? = null` na wzór `optionLeading`. `ClassCourseOptionUi` dostaje `isHidden`; `ClassEditViewModel.courseOptions` oznacza przypisania ukrytych kierunków; `ClassEditScreen` pokazuje przy takiej opcji „ukryty” w kolorze `onSurfaceVariant`, a pod polem przy wybranym ukrytym kierunku `MakHelperText` z ikoną: „Ten kierunek jest ukryty na tym telefonie. Zajęcia zapiszą się, ale nie pojawią się w planie, dopóki go nie włączysz.”
4. `TodayUiState` i `ScheduleUiState` dostają `hiddenProgramNames: List<String>` i `allProgramsHidden: Boolean`, liczone z danych przed `visibleTo` dla przypisań aktywnego semestru. `TodayScreen`: pod nazwą semestru wiersz z ikoną ukrycia „Ukryty kierunek: {nazwa}” albo „Ukryte kierunki: {nazwy}”, gdy lista nie jest pusta i nie wszystkie są ukryte. Przy `allProgramsHidden` „Dzisiaj” i „Plan” pokazują zamiast listy `MakNoteBanner` z rolą neutralną, tekstem „Wszystkie kierunki są ukryte na tym telefonie.” i akcją „Kierunki”, która otwiera `MakRoutes.StudyPrograms`.

Testy:

- `StudyProgramsViewModelTest`: `isHidden` i `setVisible`.
- Test Compose `StudyProgramsScreen` przy 320 dp: checkbox i przejście do edycji to osobne cele, opis dla czytnika ekranu, tekst „Ukryty na tym telefonie”.
- `ClassEditViewModelTest`: ukryte przypisanie ma `isHidden`; nowa klasa Compose `ClassEditScreenTest`: dopisek „ukryty” i wyjaśnienie pod polem przy 320 dp.
- `TodayViewModelTest`: podpowiedź z nazwami i stan wszystkich ukrytych; test Compose `TodayScreen`: akcja „Kierunki”.

Przypadki brzegowe: długie nazwy kierunków w podpowiedzi zawijają się; kierunek ukryty, ale nieprzypisany do aktywnego semestru, nie trafia do podpowiedzi; brak kierunków w semestrze to nadal stan konfiguracji, nie „wszystkie ukryte”.

Weryfikacja: filtrowane testy JVM; testy Compose wymienionych ekranów na emulatorze (2048 MiB); zrzuty „Kierunki”, formularza i „Dzisiaj” przy 320 dp w obu motywach porównane z makietami D4 do D7.

Kryterium zakończenia: na emulatorze odznaczenie kierunku od razu ukrywa jego zajęcia na „Dzisiaj”, w „Planie” i na widgecie, pokazuje podpowiedź, a ponowne zaznaczenie przywraca zajęcia. Commit: `feat: add study program checkboxes and hidden states (I-79)`. Potem I-79 przechodzi w `QUEUE.md` na `odbiór otwarty` z listą do sprawdzenia na telefonie.
