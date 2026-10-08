# Plan najbliższych prac

Cel: I-80 (zajęcia bez przerwy jako kolizja), I-83 (komunikat o planie z nowszej wersji MAK) i I-79 (ukrywanie kierunków) na jednej gałęzi `task/collisions-version-notice-course-toggles`, w kolejności kroków poniżej. Decyzje użytkownika z 2026-10-09 są w `DOMAIN.md` (sekcje „Kolizje” i „Kierunki”), `FEATURES.md` (sekcja „Ustawienia i dane”), `SYNC.md` (sekcja „Plik na Dysku”) i `LOG.md`; makiety na stronie „Decyzje” artefaktu „MAK: dni wolne” (https://claude.ai/artifact/8vbdCFuK7duXsocfhzE7ap).

Wspólna zasada kroków 1, 2 i 4: każdy widok planu (`TodayViewModel`, `ScheduleViewModel`, `OccurrenceViewModel`), widget (`WidgetPlanLoader`) i powiadomienia (`CollisionNotificationPlanner`, `CollisionAlarmReceiver`) liczą plan przez `ActivePlanProvider.resolve`. Ustawienia wpływające na wynik (minimalna przerwa, ukryte kierunki) trafiają do niego jednym obiektem `PlanDisplaySettings`, a nie osobnymi parametrami w każdym miejscu.

## 1. Wykryj kolizje bez nakładania i opisz je (I-80)

Cel: `CollisionDetector` zgłasza też zajęcia tego samego dnia, między którymi przerwa nie jest dłuższa niż minimalna przerwa, a wspólne funkcje treści opisują je według wariantu B. Ten krok nie dodaje jeszcze ustawienia; wszystkie wywołania przekazują 0.

Pliki: `domain/Models.kt`, `domain/CollisionDetector.kt`, `domain/ActivePlanProvider.kt`, `domain/CollisionRanges.kt`, `domain/CollisionCount.kt`, `domain/CollisionNotificationPlanner.kt`, `notifications/CollisionAlarmReceiver.kt` oraz testy w `app/src/test/java/dev/retza/mak/domain/`.

Zmiany:

1. W `Models.kt` dodaj `enum class CollisionKind { OVERLAP, NO_BREAK }`. W `Collision` zmień nazwy pól `overlapStart` i `overlapEnd` na `start` i `end` i dodaj `kind: CollisionKind`. Komentarz KDoc: dla `OVERLAP` to zakres nakładania, dla `NO_BREAK` koniec wcześniejszych i początek późniejszych zajęć. `durationMinutes` zostaje dla nakładania; dodaj `breakMinutes: Long` (`Duration.between(start, end).toMinutes()`), wymagające `kind == NO_BREAK` przez `check`.
2. Dodaj `data class PlanDisplaySettings(val minimumBreakMinutes: Int)` w `domain/PlanDisplaySettings.kt` z `companion object { val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0) }` i `require(minimumBreakMinutes >= 0)`.
3. `CollisionDetector.detect(occurrences, minimumBreakMinutes: Int)` (oraz wersja z `ResolvedSchedule`): dla każdej pary tego samego dnia z różnym `id` najpierw sprawdź nakładanie jak dziś (`OVERLAP`). Jeśli się nie nakładają, weź zajęcia kończące się wcześniej jako `earlier`; gdy `earlier.endTime <= later.startTime` i przerwa w minutach mieści się w `0..minimumBreakMinutes`, dodaj `Collision(kind = NO_BREAK, start = earlier.endTime, end = later.startTime)`. Pętla wewnętrzna nadal przerywa się przy innej dacie, ale dla `NO_BREAK` nie może kończyć się na pierwszym zajęciu, które zaczyna się po końcu `first`: przerwij ją dopiero, gdy `second.startTime` jest późniejszy niż `first.endTime` plus `minimumBreakMinutes`. `hasCollision` dostaje ten sam parametr.
4. `ActivePlanProvider.resolve(data, date, display: PlanDisplaySettings)` przekazuje `display.minimumBreakMinutes` do detektora. Parametr jest wymagany. W tym kroku wszystkie wywołania przekazują `PlanDisplaySettings.DEFAULT`.
5. `CollisionRanges.kt`: `collisionRanges` bierze tylko kolizje `OVERLAP`. `collisionLabels` buduje dla każdych zajęć: część nakładania jak dziś („Kolizja 10:00-11:30” albo „Kolizje: 10:00-10:30, 11:00-11:15”) oraz części przerw posortowane po `start`, bez powtórzeń: „bez przerwy o HH:mm” dla 0 minut i „przerwa N min o HH:mm” dla N > 0, gdzie HH:mm to `start`. Gdy są tylko przerwy, połącz je „, ” i zacznij wielką literą („Bez przerwy o 09:45”, „Przerwa 5 min o 09:45, bez przerwy o 12:00”). Gdy są oba rodzaje: część nakładania, „, ” i części przerw („Kolizja 10:00-10:30, bez przerwy o 11:15”). `collisionPartnerNames` sortuje po `start`.
6. `CollisionCount.collisionKey` używa `start`, `end` i `kind`.
7. `CollisionNotificationGroup`: zastąp `overlapStart` i `overlapEnd` polem `label: String`. Gdy grupa ma kolizje `OVERLAP`, `label` to „HH:mm-HH:mm” od najmniejszego `start` do największego `end` tych kolizji; w przeciwnym razie tekst najwcześniejszej przerwy małą literą („bez przerwy o 09:45”, „przerwa 5 min o 09:45”). `CollisionAlarmReceiver` składa tekst wieczorny z `group.label`. Powiadomienie przed zajęciami bez zmian („{nazwy} od {earliestStart}”).
8. Popraw pozostałe użycia `overlapStart` i `overlapEnd` w kodzie i testach (`MultiCalendarResolverTest`, `ScheduleResolverTest`, `CollisionNotificationGuardTest`, `CollisionDetectorTest`).

Testy:

- `CollisionDetectorTest`: styk 09:45 i 09:45 przy 0 to `NO_BREAK` z `breakMinutes` 0; przerwa 1 minuty przy 0 nie jest kolizją; przerwa 10 minut przy 10 jest kolizją, 11 minut przy 10 nie; nakładanie dalej `OVERLAP`; zajęcia różnych kierunków; trzecie zajęcia po krótkim pierwszym i długim drugim nadal są sprawdzane z pierwszym.
- `CollisionLabelsTest`: same przerwy, przerwa N min, nakładanie z przerwą przy jednych zajęciach, kilka nakładań bez zmian względem dzisiejszego zapisu.
- `CollisionCountTest`: styk liczy się jako jedna kolizja.
- `CollisionNotificationPlannerTest` albo `CollisionNotificationGuardTest`: `label` grupy z samą przerwą i z nakładaniem.
- `ActivePlanProviderTest`: odwołany termin nie tworzy kolizji bez przerwy, a przeniesiony tworzy ją w nowym terminie.

Przypadki brzegowe: dwie pary z tym samym `start` przy jednych zajęciach dają jedną część opisu; zajęcia o identycznych godzinach to nakładanie, nie przerwa; zajęcia z innego dnia nigdy nie są parą.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*"` i `--tests "dev.retza.mak.widget.WidgetPresenterTest"`; `gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.

Kryterium zakończenia: testy powyżej przechodzą, kod się kompiluje, przy minimalnej przerwie 0 styk 09:45 i 09:45 daje na karcie treść „Bez przerwy o 09:45”. Commit: `feat: treat back-to-back classes as a collision (I-80)`.

## 2. Dodaj ustawienie minimalnej przerwy i przekaż je wszystkim widokom (I-80)

Cel: pole „Minimalna przerwa” pod „Próg okienka” zmienia wynik na „Dzisiaj”, „Planie”, w szczegółach terminu, na widgecie i w powiadomieniach.

Pliki: `ui/settings/SettingsPreferences.kt`, `ui/settings/DataStoreSettingsPreferences.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`, `ui/SettingsRoutes.kt`, `ui/today/TodayViewModel.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `widget/WidgetPlanLoader.kt`, `widget/MakTodayWidget.kt`, `notifications/CollisionAlarmScheduler.kt`, `domain/CollisionNotificationPlanner.kt`, `notifications/CollisionAlarmReceiver.kt`, `MakApplication.kt` i ich testy.

Zmiany:

1. `SettingsPreferences`: `val planDisplay: Flow<PlanDisplaySettings>` i `suspend fun setMinimumBreakMinutes(minutes: Int)`. `DataStoreSettingsPreferences`: klucz `intPreferencesKey("minimum_break_minutes")`, domyślnie 0, odczyt i zapis z `coerceIn(0, 15)`.
2. `SettingsViewModel`: lista `minimumBreakOptions = listOf(0, 5, 10, 15)`, typ `MinimumBreakOptionUi(id, label = "$minutes min", isSelected)`, pole stanu `minimumBreakOptions` i `isSavingMinimumBreak`, funkcja `setMinimumBreakMinutes(id: String)` na wzór `setGapThresholdMinutes`.
3. `SettingsScreen`, sekcja „Plan”: pod polem „Próg okienka” nowe `SettingsFieldItem` z `MakSelectField(label = "Minimalna przerwa", ...)` i pod nim `MakHelperText("Zajęcia z krótszą lub równą przerwą są kolizją.")`. Nowy parametr `onMinimumBreakSelected`, podłączony w `SettingsRoutes.kt`.
4. `TodayViewModel` i `ScheduleViewModel`: dołącz `preferences.planDisplay` do istniejącego `combine` i przekaż wartość do `activePlanProvider.resolve`. `ScheduleViewModel` przyjmuje `SettingsPreferences` w konstruktorze, jeśli go jeszcze nie ma.
5. `OccurrenceViewModel`: przyjmij `SettingsPreferences`, utrzymuj `planDisplay` jako `StateFlow` (`stateIn(viewModelScope, SharingStarted.Eagerly, PlanDisplaySettings.DEFAULT)`) i używaj jego wartości w `activePlan`. Tam, gdzie szczegóły są budowane w `combine` z `activePlanData`, dodaj `planDisplay`, aby zmiana ustawienia odświeżyła szczegóły.
6. `WidgetPlanLoader`: parametr `preferences: SettingsPreferences`, `preferences.planDisplay.first()` przed `resolve`; `MakTodayWidget` przekazuje `koin.get()`.
7. `CollisionNotificationPlanner.plan(data, settings, display: PlanDisplaySettings)`; `CollisionAlarmScheduler.refresh` czyta `preferences.planDisplay.first()`; `CollisionAlarmReceiver` też.
8. `MakApplication`: do `combine` odświeżającego alarmy dołącz `preferences.planDisplay`. Zachowaj obiekt `GlanceWidgetRefreshRequester` w zmiennej i dodaj kolektor `preferences.planDisplay.distinctUntilChanged().drop(1).collect { requester.request() }`, bo widget dziś odświeża tylko zmiana bazy.
9. `DOMAIN.md` nie wymaga zmian; zaktualizuj `KNOWN_ISSUES.md`, jeśli testy ujawnią rozbieżność.

Testy:

- `SettingsPreferencesTest` (androidTest): domyślnie 0, zapis 10, wartość spoza zakresu przycięta do 15.
- `SettingsViewModelTest`: opcje i zapis minimalnej przerwy.
- `TodayViewModelTest`: przy minimalnej przerwie 10 zajęcia z przerwą 5 minut liczą się jako kolizja, przy 0 nie.
- `WidgetPlanLoaderTest`: loader używa minimalnej przerwy z preferencji.
- `CollisionNotificationPlannerTest`: kolizja bez przerwy planuje powiadomienie.
- Test Compose ustawień przy 320 dp: pole „Minimalna przerwa” i opis są widoczne, wybór wywołuje `onMinimumBreakSelected`.

Przypadki brzegowe: zmiana ustawienia przy otwartych szczegółach terminu; brak aktywnego semestru; widget bez danych.

Weryfikacja: filtrowane testy JVM wymienionych klas; `gradlew.bat --offline :app:compileDebugAndroidTestKotlin lintDebug`; jedna klasa Android `SettingsPreferencesTest` na emulatorze (2048 MiB); zrzut ustawień przy 320 dp w obu motywach.

Kryterium zakończenia: na emulatorze zmiana „Minimalna przerwa” z 0 na 15 dodaje kolizję zajęciom z przerwą 15 minut na „Dzisiaj”, w „Planie” i na widgecie bez ponownego uruchomienia aplikacji. Commit: `feat: add the minimum break setting (I-80)`. Po tym kroku I-80 przechodzi w `QUEUE.md` na `odbiór otwarty` albo `gotowe`, zależnie od sprawdzenia powiadomień.

## 3. Pokaż komunikat o planie z nowszej wersji MAK (I-83)

Cel: plik eksportu albo plan na Dysku z wyższą wersją schematu nie daje ogólnego błędu odczytu, tylko komunikat z akcją aktualizacji.

Pliki: `export/JsonExportCodec.kt`, nowy `export/NewerExportVersionException.kt`, `export/PlanBackupService.kt`, `sync/SyncPlanFile.kt`, `sync/SyncCoordinator.kt`, `sync/SyncStateStore.kt`, `ui/settings/SyncViewModel.kt`, `ui/settings/SyncScreen.kt`, `ui/SettingsRoutes.kt`, `ui/today/TodayScreen.kt` i testy.

Zmiany:

1. `class NewerExportVersionException(val version: Int) : IllegalArgumentException(...)`. `JsonExportCodec.decode` odczytuje `schemaVersion` jak dziś i od razu po tym, przed pozostałymi `require` i przed `decodeFromJsonElement`, rzuca ten wyjątek, gdy wersja jest większa niż `ExportSchema.VERSION`.
2. `PlanBackupService.prepareImport` łapie `NewerExportVersionException` przed ogólnym `Exception` i zwraca `ImportPreparation.Invalid(listOf(NEWER_FILE_MESSAGE))`, gdzie `NEWER_FILE_MESSAGE = "Plik pochodzi z nowszej wersji MAK. Zaktualizuj aplikację."`.
3. `SyncPlanFile.decode` łapie `NewerExportVersionException` i rzuca nowy `NewerRemotePlanException` (osobna klasa, nie podklasa `InvalidRemotePlanException`).
4. `SyncIssue` dostaje `NEWER_REMOTE_PLAN`. `SyncCoordinator.run` łapie `NewerRemotePlanException` i zwraca `issue(SyncIssue.NEWER_REMOTE_PLAN, message = null)`; plan lokalny i plik na Dysku zostają bez zmian.
5. `SyncViewModel`: `issueText` dla `NEWER_REMOTE_PLAN` to „Plan na Dysku pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.”; `SyncAttentionUi` dostaje pole `opensUpdate: Boolean`, a dla tego problemu `action = "Zaktualizuj"` i `opensUpdate = true`; `SyncUiState` dostaje `needsUpdate`.
6. `SyncScreen`: banner problemu przy `needsUpdate` ma akcję `MakBannerAction("Zaktualizuj", onOpenUpdate)`. `SettingsRoutes` przekazuje `onOpenUpdate = { navController.navigate(MakRoutes.SettingsUpdate) }`.
7. `TodayScreen`: akcja ostrzeżenia synchronizacji wywołuje `onViewUpdate`, gdy `syncAttention.opensUpdate`, w przeciwnym razie `onOpenSync`.

Testy:

- `JsonExportCodecTest`: wersja 4 z nieznanym polem rzuca `NewerExportVersionException`; wersja 3 działa; wersja 1 nadal trafia do `ExportImporter` i daje „Nieobsługiwana wersja pliku”.
- Test `PlanBackupService` (dodaj w `export/`, jeśli nie istnieje): komunikat dla nowszego pliku.
- `SyncCoordinatorTest`: plik z nowszą wersją daje `NeedsAttention`, `SyncIssue.NEWER_REMOTE_PLAN`, brak zapisu lokalnego i brak wysłania; kolejna udana synchronizacja czyści problem.
- Test Compose `TodayScreen`: akcja „Zaktualizuj” wywołuje `onViewUpdate`.

Przypadki brzegowe: problem musi zniknąć po aktualizacji aplikacji i udanej synchronizacji; `FileSyncStateStore` zapisuje enum po nazwie, więc nowa wartość nie wymaga migracji.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.export.*" --tests "dev.retza.mak.sync.SyncCoordinatorTest"`; kompilacja testów Android; jedna klasa Compose `TodayScreenTest` na emulatorze.

Kryterium zakończenia: testy przechodzą; ręczny import pliku z `"schemaVersion": 4` pokazuje nowy komunikat. Commit: `feat: tell users when a plan comes from a newer MAK (I-83)`.

## 4. Filtruj plan według ukrytych kierunków (I-79)

Cel: lokalny wybór ukrytych kierunków usuwa ich zajęcia ze wszystkich widoków, widgetu, kolizji, okienek i powiadomień, a synchronizacja i import nie przenoszą go na złe kierunki.

Pliki: `domain/PlanDisplaySettings.kt`, `domain/ActivePlanProvider.kt`, `ui/settings/SettingsPreferences.kt`, `ui/settings/DataStoreSettingsPreferences.kt`, `ui/schedule/ScheduleViewModel.kt`, `sync/PlanSyncGateway.kt`, `export/PlanBackupService.kt`, `ui/programs/StudyProgramsViewModel.kt` i testy.

Zmiany:

1. `PlanDisplaySettings` dostaje `hiddenProgramIds: Set<String>` (domyślnie pusty w `DEFAULT`).
2. Funkcja `ActivePlanData.visibleTo(display: PlanDisplaySettings): ActivePlanData` w `ActivePlanProvider.kt`: usuwa `semesterPrograms` z `studyProgramId` w `hiddenProgramIds` i `classes` z `semesterProgramId` usuniętych przypisań; pozostałe pola bez zmian. `ActivePlanProvider.resolve` wywołuje ją przed rozwiązaniem planu.
3. `SettingsPreferences`: `planDisplay` łączy minimalną przerwę z `stringSetPreferencesKey("hidden_study_program_ids")`; nowe funkcje `setStudyProgramHidden(id: String, hidden: Boolean)`, `clearHiddenStudyPrograms()` i `retainHiddenStudyPrograms(existingIds: Set<String>)`.
4. `ScheduleViewModel` buduje cały stan (opcje pola „Kierunek”, odwołane terminy, kalendarze filtra) z `data.visibleTo(display)`, aby ukryty kierunek nie był opcją filtra.
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

## 5. Dodaj checkboxy kierunków i stany ukrycia w interfejsie (I-79)

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
