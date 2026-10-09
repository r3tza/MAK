# Plan najbliższych prac

Cel: I-79 (ukrywanie kierunków na telefonie) na gałęzi `task/I-79-course-toggles`, w kolejności kroków poniżej. Decyzje użytkownika z 2026-10-09 są w `DOMAIN.md` (sekcja „Kierunki”), `FEATURES.md` (sekcja „Ustawienia i dane”) i `LOG.md`; makiety D4 do D7 na stronie „Decyzje” artefaktu „MAK: dni wolne” (https://claude.ai/artifact/8vbdCFuK7duXsocfhzE7ap).

Wspólna zasada: każdy widok planu (`TodayViewModel`, `ScheduleViewModel`, `OccurrenceViewModel`), widget (`WidgetPlanLoader`) i powiadomienia (`CollisionNotificationPlanner`, `CollisionAlarmScheduler`, `CollisionAlarmReceiver`) biorą dane planu z `ActivePlanSource` jako `ActivePlanInputs` (dane razem z `PlanDisplaySettings`) i liczą plan przez `ActivePlanProvider.resolve(inputs, date)`. Ustawienie wpływające na wynik (minimalna przerwa, ukryte kierunki) trafia do `PlanDisplaySettings` i `SettingsPreferences.planDisplay`, a nie do osobnych parametrów w każdym miejscu.

## 1. Filtruj plan według ukrytych kierunków (I-79)

Stan: zrobione. Testy JVM i na emulatorze `RoomPlanSyncGatewayTest`, `SettingsPreferencesTest`, `KoinGraphTest` i `CollisionAlarmSchedulerTest` przechodzą; ręczne sprawdzenie zapisanego wyboru przeniesione do kroku 2, bo bez checkboxa nie da się go zapisać na emulatorze bez roota. `PlanBackupServiceTest` i `StudyProgramsViewModelTest` sprawdzają czyszczenie wyboru po imporcie i usunięciu kierunku. Po decyzji użytkownika widget przy wszystkich ukrytych kierunkach pokazuje „Wszystkie kierunki są ukryte na tym telefonie.”, a `visibleTo` usuwa tylko zajęcia, nie przypisania (krok 2, `LOG.md`).

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

## 2. Dodaj checkboxy kierunków i stany ukrycia w interfejsie (I-79)

Stan: zrobione. Zamiast pól `hiddenProgramNames` w `ScheduleUiState` „Plan” ma tylko `allProgramsHidden`, bo podpowiedź z nazwami jest wyłącznie na „Dzisiaj”. Wspólne elementy są w `ui/HiddenPrograms.kt`; `MakSectionHeader` ma `supportingContent`, a `MakHelperText` opcjonalną ikonę.

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
