# Plan najbliższych prac

Cel: dokończyć I-84 (porządki po audycie kodu z 2026-10-09, kroki 1 i 2), potem zacząć I-85 (reguły planu w `ScheduleResolver`, kroki 3 do 5). Raport audytu: `docs/audit_09102026.md`; oznaczenia w nawiasach (E1, A2 i podobne) to jego punkty. Decyzje użytkownika z 2026-10-10 są w `LOG.md`. Kroki 1 i 2 z pierwszej wersji planu I-84 (martwy kod, stałe pola stanu) są zrobione w commitach `02ba0c3` i `f37a1f9`.

## Zasady dla wykonawcy

Przeczytaj tę sekcję przed każdym krokiem.

- Gałęzie:
  - Wszystkie kroki wykonuj na gałęzi `task/I-79-hide-study-programs` (decyzja użytkownika z 2026-10-10: poprawki z audytu, I-84 do I-90, trafiają na gałąź I-79). Przed pierwszym krokiem sprawdź `git branch --show-current`; jeśli to inna gałąź, przełącz się poleceniem `git switch task/I-79-hide-study-programs` i nie twórz nowej.
- Ścieżki kodu są względne wobec `app/src/main/java/dev/retza/mak/`, testów JVM wobec `app/src/test/java/dev/retza/mak/`, testów Android wobec `app/src/androidTest/java/dev/retza/mak/`.
- Przed zmianą otwórz każdy plik z listy kroku i znajdź opisany fragment po nazwie funkcji, nie po numerze wiersza. Jeśli fragmentu nie ma albo wygląda inaczej niż w opisie, zrób zmianę zgodną z celem kroku i opisz różnicę w commicie. Zatrzymaj się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Polecenia uruchamiaj w katalogu repozytorium w PowerShellu. Testy JVM jednej klasy: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.today.TodayViewModelTest"`. Jeśli Gradle zgłosi `No cached version ... available for offline mode`, uruchom to samo polecenie raz bez `--offline`, potem wróć do `--offline`.
- Po każdej zmianie kodu: `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`. Kompilacja testów Android nie oznacza ich uruchomienia.
- Testy Android: najwyżej jeden emulator naraz, z 2048 MiB RAM, obraz `Medium_Phone_A12` (`STACK.md`, sekcja 6). Jedna klasa: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.PlanEditingTest"`. Nie czyść danych emulatora `Medium_Phone`.
- Nie uruchamiaj pełnego zestawu testów aplikacji bez polecenia użytkownika.
- Każdy krok kończy się jednym commitem z podanym tematem, bez stopki `Co-authored-by`. Do commita dołącz zmiany `PLAN.md`, `QUEUE.md` i dokumentów, których dotyczy krok. Po commicie dopisz pod nagłówkiem kroku linię `Stan: zrobione.` i krótko opisz różnice wobec planu; ta zmiana trafia do commita następnego kroku.
- Nie wypychaj gałęzi, nie otwieraj pull requesta i nie scalaj bez wyraźnego polecenia użytkownika.
- Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py`; wymagany kod 0.
- Teksty interfejsu i dokumentacja po polsku, identyfikatory i komentarze w kodzie po angielsku. Bez znaków `·`, `—` i strzałek.

## 1. Przenieś akcje zapisu na launchUiOperation i usuń koinInject z TrackPlanEditing (I-84)

Stan: zrobione (`84fdd76`). Wywołań `TrackPlanEditing` jest 12, nie 13; parametr trackera dostała też `occurrenceTopBarActions`. Testy Android uruchomione na `Medium_Phone` (na tym komputerze nie ma `Medium_Phone_A12`). Zrzutów obu dialogów przy 320 dp nie wykonano.

Cel: każda akcja zapisu z interfejsu ma blokadę podwójnego wywołania i flagę trwania przez `launchUiOperation` (E1, `ARCHITECTURE.md`, sekcja 5, I-64), zależność `PlanEditTracker` przychodzi jawnie z granicy hosta (D3, `STACK.md`, sekcja o Koin), a dwa dialogi rysuje `MakDialog` (H7, `LOG.md`, I-72). Dla użytkownika zmienia się tylko jedno: w dialogu „Usuń zajęcia” przyciski są nieaktywne, dopóki trwa usuwanie.

Pliki: `ui/schedule/ScheduleViewModel.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceDetailsScreen.kt`, `ui/PlanEditing.kt`, `MainActivity.kt`, `ui/MakNavHostApp.kt`, `ui/ClassOccurrenceRoutes.kt`, `ui/SemesterRoutes.kt`, `ui/SettingsRoutes.kt`, `ui/SetupRoutes.kt`, `ui/StudyProgramRoutes.kt`, `ui/edit/ClassEditScreen.kt`, `ui/semester/SemesterScreen.kt`; testy `ui/schedule/ScheduleViewModelTest.kt`, `ui/occurrence/OccurrenceViewModelTest.kt` (JVM), `ui/edit/ClassEditScreenTest.kt`, `ui/semester/SemesterScreenTest.kt`, `ui/occurrence/OccurrenceDetailsScreenTest.kt`, `ui/PlanEditingTest.kt` (Android).

Zmiany:

1. Korekta tygodnia w `ScheduleViewModel`:
   - Dodaj pole `private var weekOverrideOperationRunning = false` obok `controls`. Wzór: `occurrenceStateOperationRunning` w `OccurrenceViewModel`. Flaga nie trafia do `ScheduleUiState`, bo `WeekCorrectionDialog` w `ScheduleScreen.kt` zamyka się zaraz po dotknięciu „Zapisz” albo „Usuń korektę” (`showWeekDialog = false`).
   - W `saveVisibleWeekOverride` jako pierwsza linia: `if (weekOverrideOperationRunning) return`. Dotychczasowe wyliczenie `data`, `calendarId`, `monday`, `entityScope` i `existing` zostaje bez zmian. Potem `weekOverrideOperationRunning = true` i zamiast `viewModelScope.launch { runReportingFailure(...) { ... } }`:
     ```kotlin
     viewModelScope.launchUiOperation(
         feedbackSink = feedbackSink,
         errorMessage = "Nie udało się zapisać korekty tygodnia.",
         onFinish = { weekOverrideOperationRunning = false }
     ) {
         semesterRepository.saveWeekOverride(/* dotychczasowy WeekOverrideRecord */)
     }
     ```
   - `clearVisibleWeekOverride` tak samo, z `errorMessage = "Nie udało się usunąć korekty tygodnia."` i dotychczasowym `semesterRepository.deleteWeekOverride(existing.id.toLong())` w bloku. Flagę ustaw dopiero po `?: return` przy `existing`.
   - Usuń funkcję `runReportingFailure`. Dodaj import `dev.retza.mak.ui.feedback.launchUiOperation`. Usuń importy, które kompilator albo IDE wskaże jako nieużywane (prawdopodobnie `CancellationException`, `UiFeedback`, `UiFeedbackKind`).
2. Usunięcie zajęć w `OccurrenceViewModel`:
   - W `OccurrenceDetailsUiState` (`OccurrenceDetailsModels.kt`) dodaj pole `val isDeletingClass: Boolean = false` po `showDeleteConfirmation`. W funkcji `hasPlanDraft()` dopisz `|| isDeletingClass` na końcu wyrażenia.
   - W funkcji `withFreshData` w `OccurrenceViewModel` (kopiuje z bieżącego stanu flagi zapisu i dialogów) dopisz `isDeletingClass = isDeletingClass` obok `showDeleteConfirmation = showDeleteConfirmation`.
   - Zastąp ciało `deleteSelectedClass()`:
     ```kotlin
     fun deleteSelectedClass() {
         if (state.value.isDeletingClass) return
         val classId = selectedClassIdState.value ?: return
         update { it.copy(isDeletingClass = true) }
         viewModelScope.launchUiOperation(
             feedbackSink = feedbackSink,
             errorMessage = "Nie udało się usunąć zajęć.",
             onFinish = { update { it.copy(isDeletingClass = false) } },
             onError = { update { it.copy(showDeleteConfirmation = false) } }
         ) {
             scheduleRepository.deleteClass(classId)
             effectsChannel.trySend(OccurrenceEffect.CloseDetails)
         }
     }
     ```
   - W `OccurrenceDetailsScreen.kt` w wywołaniu `MakConfirmDeletionDialog` zamień `isDeleting = false` na `isDeleting = state.isDeletingClass` i usuń komentarz nad tą linią.
3. Bez zmian zostają: `SemesterViewModel.requestCourseDeletion` (odczyt liczby zajęć przed potwierdzeniem, nie zapis; ma już blokadę `isPreparingCourseDeletion`), `SemesterViewModel.open`, blok w `init` `StudyProgramsViewModel` i bloki `try` w `SyncViewModel` (odczyty albo stan synchronizacji, wymienione w sekcji „Pominięte” raportu). Zapisz to w treści commita.
4. `PlanEditTracker` bez `koinInject()`:
   - `ui/PlanEditing.kt`, funkcja `TrackPlanEditing`: nowa sygnatura `internal fun TrackPlanEditing(tracker: PlanEditTracker, active: Boolean = true, isActiveNow: () -> Boolean = { active }): PlanEditingController`. Usuń import `org.koin.compose.koinInject`.
   - `MainActivity.kt`: obok `private val feedbackController: FeedbackController by inject()` dodaj `private val planEditTracker: PlanEditTracker by inject()` (import `dev.retza.mak.sync.PlanEditTracker`; binding daje `providePlanEditTracker` w `di/AppModule.kt`). W wywołaniu `MakApp(...)` dodaj argument `planEditTracker = planEditTracker`.
   - `ui/MakNavHostApp.kt`, funkcja `MakApp`: dodaj parametr `planEditTracker: PlanEditTracker` po `updateViewModel`. Przekaż go nazwanym argumentem `planEditTracker = planEditTracker` do `occurrenceTopBarActions`, `classEditRoute`, `occurrenceDetailsRoute`, `semesterRoutes`, `settingsRoute`, `studyProgramRoutes` i `setupRoute`.
   - Każda z tych siedmiu funkcji (w `ClassOccurrenceRoutes.kt`, `SemesterRoutes.kt`, `SettingsRoutes.kt`, `SetupRoutes.kt`, `StudyProgramRoutes.kt`) dostaje pierwszy parametr `planEditTracker: PlanEditTracker`. Każde wywołanie `TrackPlanEditing(` w tych plikach zmień na `TrackPlanEditing(planEditTracker, ` z dotychczasowymi argumentami nazwanymi; `TrackPlanEditing()` bez argumentów staje się `TrackPlanEditing(planEditTracker)`. Wywołań jest 13; sprawdź to poleceniem `rg -c "TrackPlanEditing\(" app/src/main`.
   - `PlanEditingTest.kt` już przekazuje `tracker = tracker` nazwanym argumentem; jeśli kompilacja zgłosi błąd kolejności, zamień na nazwane argumenty.
5. Dialogi przez `MakDialog`:
   - `ClassEditScreen.kt`, blok `state.pendingHiddenData?.let { warning -> AlertDialog(...) }` zastąp:
     ```kotlin
     state.pendingHiddenData?.let { warning ->
         MakDialog(
             title = "Część danych przestanie być widoczna",
             description = hiddenDataMessage(warning),
             onDismiss = { if (!state.isSaving) onDismissHiddenData() },
             dismissAction = MakDialogAction("Anuluj", onDismissHiddenData, enabled = !state.isSaving),
             confirmAction = MakDialogAction("Zapisz", onConfirmHiddenData, enabled = !state.isSaving)
         )
     }
     ```
   - `SemesterScreen.kt`, funkcja `ReconnectCalendarDialog`: ciało zastąp `MakDialog(title = "Połączyć kalendarze?", onDismiss = { if (!isSaving) onCancel() }, dismissAction = MakDialogAction("Anuluj", onCancel, enabled = !isSaving), confirmAction = MakDialogAction("Połącz", onConfirm, enabled = !isSaving)) { ... }`. Do lambdy `content` przenieś dotychczasową zawartość `text = { Column(...) { ... } }`, czyli dwa teksty i błąd w kolorze `MaterialTheme.colorScheme.error`, bez otaczającej `Column` (`MakDialog` już układa treść w kolumnie).
   - Dodaj importy `dev.retza.mak.ui.components.MakDialog` i `MakDialogAction`, jeśli ich brak. Usuń nieużywane importy `AlertDialog` i `TextButton`.

Testy:

- `ScheduleViewModelTest`, nowy test `weekOverrideSaveRunsOnceWhileTheFirstIsPending`: ustaw dane jak w istniejącym teście, który woła `saveVisibleWeekOverride`; przed wywołaniem `repository.saveGate = CompletableDeferred()`; wywołaj `saveVisibleWeekOverride(WeekTypeUi.B, WeekOverrideScopeUi.ONE_WEEK)` dwa razy, potem `repository.saveGate?.complete(Unit)` i `advanceUntilIdle()`; sprawdź `assertEquals(1, repository.events.count { it == "saveWeekOverride" })`.
- `ScheduleViewModelTest`, nowy test `weekOverrideFailurePublishesErrorAndAllowsRetry`: `repository.failSaves = true`, zapis, `advanceUntilIdle()`; komunikat „Nie udało się zapisać korekty tygodnia.” w `RecordingFeedbackSink` (wzór w `OccurrenceViewModelTest`); potem `repository.failSaves = false`, drugi zapis, `advanceUntilIdle()`; w `events` jest `saveWeekOverride`.
- `OccurrenceViewModelTest`, nowy test `deleteSelectedClassRunsOnceWhileTheFirstIsPending`: wzór `deleteSelectedClassEmitsCloseExactlyOnce`, ale z `repository.saveGate = CompletableDeferred()` przed dwoma wywołaniami `deleteSelectedClass()`; po pierwszym `advanceUntilIdle()` sprawdź `assertTrue(viewModel.details.value.isDeletingClass)`; po `saveGate?.complete(Unit)` i `advanceUntilIdle()` jedno `deleteClass` w `events` i `effects == listOf(OccurrenceEffect.CloseDetails)`.
- W istniejącym teście błędu usuwania (ten z `repository.failSaves = true` i `deleteSelectedClass()`) dopisz `assertFalse(viewModel.details.value.isDeletingClass)`.
- `ClassEditScreenTest`, nowy test `hiddenDataDialogBlocksActionsWhileSaving` przy szerokości 320 dp (wzór ustawienia ekranu w tym pliku): stan z `pendingHiddenData = HiddenDataWarningUi(changeCount = 1, noteCount = 0)` i `isSaving = true`; `onNodeWithText("Część danych przestanie być widoczna").assertIsDisplayed()`, `onNodeWithText("Zapisz").assertIsNotEnabled()`, `onNodeWithText("Anuluj").assertIsNotEnabled()`.
- `OccurrenceDetailsScreenTest`, nowy test `deleteDialogBlocksActionsWhileDeleting`: stan z `showDeleteConfirmation = true` i `isDeletingClass = true`; w dialogu przycisk „Usuń zajęcia” jest nieaktywny. Uwaga: tekst „Usuń zajęcia” jest też tytułem dialogu, więc użyj `onAllNodesWithText("Usuń zajęcia")` i sprawdź węzeł z akcją kliknięcia (`hasClickAction()`).
- `SemesterScreenTest`: istniejący test z „Połączyć kalendarze?” ma przejść bez zmian.

Przypadki brzegowe: zapis korekty bez widocznego kalendarza wraca przed ustawieniem flagi, więc jej nie blokuje; anulowanie korutyny przy zamknięciu ViewModelu nie publikuje błędu (zapewnia to `launchUiOperation`); po udanym usunięciu szczegóły się zamykają, a flaga wraca do `false` w `onFinish`; w niskim oknie oba dialogi się przewijają, bo robi to `MakDialog`.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.schedule.ScheduleViewModelTest" --tests "dev.retza.mak.ui.occurrence.OccurrenceViewModelTest"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze kolejno klasy `dev.retza.mak.ui.PlanEditingTest`, `dev.retza.mak.ui.edit.ClassEditScreenTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest` i `dev.retza.mak.di.KoinGraphTest`.
4. Zrzuty obu dialogów przy 320 dp w motywie jasnym i ciemnym (`STACK.md`, sekcja 6). Dialog ukrytych danych: edytuj zajęcia z notatką do terminu i zmień dzień tygodnia. Dialog łączenia: „Kierunki” w semestrze z dwoma kalendarzami. Porównaj z dialogiem „Zmień tydzień A/B” w „Planie”: ten sam kształt, odstępy i kolejność przycisków.

Kryterium zakończenia: wszystkie testy z weryfikacji przechodzą; `rg "koinInject|runReportingFailure" app/src/main` nic nie znajduje; `rg "AlertDialog\(" app/src/main` znajduje tylko `MakConfirmDeletionDialog` w `ui/components/MakComponents.kt` (poza zakresem H7). Commit: `fix: guard week override and class deletion writes (I-84)`.

## 2. Ogranicz zbędne odświeżanie powiadomień i preferencji (I-84)

Stan: zrobione. `refresh()` bez argumentów czyta plan tylko przy włączonych powiadomieniach. `SettingsPreferencesTest` używa `runBlocking` z kolektorem na `Dispatchers.Default`, bo testy Android w tym pliku nie używają `runTest`.

Cel: zmiana motywu, zapis czasu sprawdzenia aktualizacji albo ukrycie banera nie przelicza planu ani alarmów, a przy wyłączonych powiadomieniach o kolizjach aplikacja nie subskrybuje planu (B3, B5). Zachowanie widoczne dla użytkownika się nie zmienia.

Pliki: `ui/settings/DataStoreSettingsPreferences.kt`, `notifications/CollisionAlarmScheduler.kt`, `MakApplication.kt`; testy `notifications/CollisionAlarmSchedulerTest.kt` i `ui/settings/SettingsPreferencesTest.kt` (Android).

Zmiany:

1. `DataStoreSettingsPreferences`: dopisz `.distinctUntilChanged()` na końcu definicji `theme`, `collisionNotifications` i `gapThresholdMinutes`, tak jak ma `planDisplay`. Import `kotlinx.coroutines.flow.distinctUntilChanged` już jest.
2. `CollisionAlarmScheduler`:
   - Dodaj publiczną funkcję, która dostaje gotowe dane:
     ```kotlin
     /** Schedules from values the caller already has, so a collector does not read the plan twice. */
     suspend fun refresh(settings: CollisionNotificationPreferences, inputs: ActivePlanInputs?) =
         mutex.withLock { refreshLocked(settings, inputs) }
     ```
   - Ciało obecnego `refresh()` przenieś do `private fun refreshLocked(settings: CollisionNotificationPreferences, inputs: ActivePlanInputs?)` bez odczytów `first()`. `refresh()` bez parametrów zostaje dla odbiorników (`CollisionAlarmReceiver`, `NotificationRescheduleReceiver`): `suspend fun refresh() = mutex.withLock { refreshLocked(preferences.collisionNotifications.first(), activePlanSource.observeActive().first()) }`. Przy `enabled == false` odczyt planu jest zbędny; wolno go pominąć, czytając plan dopiero gdy `settings.enabled`.
   - W `refreshLocked` zamień końcowe `store(plannedIds)` na `if (storedIds() != plannedIds) store(plannedIds)`.
3. `MakApplication`: zastąp kolektor z `combine(activePlanSource.observeActive(), preferences.collisionNotifications)`:
   ```kotlin
   initializationScope.launch {
       preferences.collisionNotifications
           .flatMapLatest { settings ->
               if (!settings.enabled) flowOf(settings to null)
               else activePlanSource.observeActive().map { inputs -> settings to inputs }
           }
           .debounce(NOTIFICATION_REFRESH_DEBOUNCE_MILLIS)
           .collect { (settings, inputs) ->
               try {
                   scheduler.refresh(settings, inputs)
               } catch (error: CancellationException) {
                   throw error
               } catch (error: Exception) {
                   // Keep collecting; the next data or settings change retries the refresh.
               }
           }
   }
   ```
   Dodaj importy `flatMapLatest`, `flowOf`, `map` z `kotlinx.coroutines.flow` i `@OptIn(ExperimentalCoroutinesApi::class)` przy klasie albo funkcji (wzór: `FlowPreview` już jest). Usuń nieużywany import `combine`.

Testy:

- `CollisionAlarmSchedulerTest`: nowy test `disabledSettingsCancelStoredAlarms`. Najpierw `refresh(enabledSettings, inputs)` z danymi z `seedCollidingClasses()` (wzór w `parallelRefreshesStoreExactlyThePlannedAlarms`), potem `refresh(enabledSettings.copy(enabled = false), null)`; zapisany zbiór identyfikatorów w `SharedPreferences` jest pusty.
- `CollisionAlarmSchedulerTest`: nowy test `sameInputsKeepStoredIds`: dwa razy `refresh(settings, inputs)` z tymi samymi danymi; zbiór zapisanych identyfikatorów jest taki sam po obu wywołaniach i niepusty.
- `SettingsPreferencesTest`: nowy test `otherKeyDoesNotReemitTheme`: zbieraj `theme` do listy w `backgroundScope`, zapisz `setGapThresholdMinutes(45)`, po zapisie lista ma jedną wartość.

Przypadki brzegowe: włączenie powiadomień po starcie z wyłączonymi zaczyna subskrypcję planu i planuje alarmy bez ponownego uruchomienia aplikacji; wyłączenie anuluje zapisane alarmy, bo `refreshLocked` przy `enabled == false` woła `cancelAllLocked()`.

Weryfikacja:

1. `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`.
2. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.CollisionNotificationPlannerTest"`.
3. Na emulatorze klasy `dev.retza.mak.notifications.CollisionAlarmSchedulerTest`, `dev.retza.mak.ui.settings.SettingsPreferencesTest` i `dev.retza.mak.di.KoinGraphTest`.
4. Ręcznie na emulatorze z wersją debug (dane demonstracyjne mają kolizje): w ustawieniach włącz powiadomienia o kolizjach, potem `adb shell dumpsys alarm | findstr dev.retza.mak` pokazuje alarm; po wyłączeniu to samo polecenie nie pokazuje alarmów aplikacji.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki. Commit: `fix: skip plan refreshes when nothing changed (I-84)`. W tym commicie w `QUEUE.md` przenieś I-84 do tabeli „Zakończone” ze statusem `gotowe` i listą zrealizowanych punktów audytu oraz uruchomionych testów.

## 3. Zwracaj odwołane terminy, status terminu i stan tygodnia z ScheduleResolver (I-85)

Stan: zrobione. `OccurrenceNoteRemapTest` nie tworzy `ClassItemUi`, więc się nie zmienił; `isModified` i `isOneOff` zostają tylko w warstwie danych (`OccurrenceNoteRemap.kt`, `Migrations.kt`) i nie dotyczą kryterium. Zrzuty przy 320 dp („Dzisiaj”, lista i kalendarz „Planu” z odwołanym terminem, „Plan” i szczegóły przeniesionego terminu, szczegóły odwołanego) przed zmianą i po niej są identyczne piksel po pikselu.

Cel: reguły odwołanych terminów, statusu terminu i etykiety tygodnia są w jednym miejscu (A2, A3, A4; decyzja użytkownika z 2026-10-10 w `LOG.md`). `cancelledOccurrences` znika, a karta, kalendarz i widget biorą status i tydzień z wyniku resolvera. Wygląd ekranów się nie zmienia.

Gałąź: `task/I-79-hide-study-programs`, zobacz „Zasady dla wykonawcy”.

Pliki: `domain/Models.kt`, `domain/ScheduleResolver.kt`, `domain/CancelledOccurrences.kt` (usunąć), `ui/components/PresentationModels.kt`, nowy `ui/components/OccurrenceStatusUi.kt`, `ui/components/MakComponents.kt`, `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceDetailsScreen.kt`, `ui/PlanMapping.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/today/TodayViewModel.kt`, `widget/WidgetPresenter.kt`; testy `domain/ScheduleResolverTest.kt`, `domain/CancelledOccurrencesTest.kt`, `ui/components/ClassCardDescriptionTest.kt`, `data/repository/OccurrenceNoteRemapTest.kt`, `ui/PlanMappingTest.kt`, `ui/schedule/ScheduleViewModelTest.kt`, `ui/today/TodayViewModelTest.kt`, `widget/WidgetPresenterTest.kt`; dokument `docs/ARCHITECTURE.md`.

Zmiany:

1. Status terminu w domenie (`domain/Models.kt`):
   ```kotlin
   enum class OccurrenceStatus { SCHEDULED, CANCELLED, MOVED, MODIFIED, ONE_OFF }
   ```
   W `PlannedOccurrence` dodaj właściwość obliczaną; kolejność warunków jest regułą i musi zostać taka:
   ```kotlin
   val status: OccurrenceStatus
       get() {
           val change = occurrenceChange
           return when {
               change?.kind == OccurrenceChangeKind.CANCELLED -> OccurrenceStatus.CANCELLED
               classItem.recurrence == Recurrence.ONCE -> OccurrenceStatus.ONE_OFF
               change?.targetDate != null && change.targetDate != change.originalDate -> OccurrenceStatus.MOVED
               change != null -> OccurrenceStatus.MODIFIED
               else -> OccurrenceStatus.SCHEDULED
           }
       }
   ```
2. Stan tygodnia w domenie (`domain/Models.kt`):
   ```kotlin
   sealed interface WeekStatus {
       data class Single(val type: WeekType) : WeekStatus
       data object Mixed : WeekStatus
       data object OutsideSemester : WeekStatus
   }
   ```
   `ResolvedSchedule` dostaje dwa pola z wartościami domyślnymi, aby testy tworzące go ręcznie dalej się kompilowały: `val cancelled: List<PlannedOccurrence> = emptyList()` i `val weeksByCalendarId: Map<String, WeekCalculation> = emptyMap()`, oraz właściwość obliczaną:
   ```kotlin
   val weekStatus: WeekStatus
       get() = when {
           hasMixedWeekTypes -> WeekStatus.Mixed
           week != null -> WeekStatus.Single(week.weekType)
           else -> WeekStatus.OutsideSemester
       }
   ```
3. `ScheduleResolver`:
   - W `resolveClass` usuń linię `if (change?.kind == OccurrenceChangeKind.CANCELLED) return@mapNotNull null`. Odwołany termin ma `targetDate == null`, więc przechodzi przez warunek `actualDate != date` tylko w swoim pierwotnym dniu; `render` dostaje `change` i bierze godziny i salę z zajęć, bo odwołanie ich nie ma.
   - W `resolve` zamień zmienną `occurrences` na `resolved` i podziel listę: `val (cancelled, occurrences) = resolved.partition { it.status == OccurrenceStatus.CANCELLED }`. Sortowanie zostaje przed podziałem.
   - Do `ResolvedSchedule(...)` przekaż `cancelled = cancelled` i `weeksByCalendarId = weeks`.
   - Zajęcia jednorazowe nie mają odwołań, bo `resolveClass` dla `Recurrence.ONCE` wraca przed odczytem zmian. To ta sama reguła co w `cancelledOccurrences`.
4. Usuń `domain/CancelledOccurrences.kt`. `CancelledOccurrencesTest.kt` przepisz na wynik resolvera i zmień nazwę pliku i klasy na `ResolvedCancelledOccurrencesTest`. Cztery testy zostają z tymi samymi danymi, a wywołanie `cancelledOccurrences(data, date)` zastąp funkcją pomocniczą w teście:
   ```kotlin
   private fun cancelled(data: ActivePlanData, date: LocalDate) =
       ActivePlanProvider().schedule(data.visibleTo(PlanDisplaySettings.DEFAULT), date).cancelled
   ```
   W pierwszym teście sprawdzaj `occurrenceNote?.body` zamiast pola `occurrenceNote` z `CancelledOccurrence`. Dopisz piąty test `cancelledTermIsNotAmongOccurrences`: ta sama data i odwołanie, `schedule(...).occurrences` jest puste, a `cancelled` ma jeden element ze statusem `OccurrenceStatus.CANCELLED`.
5. Status w interfejsie, nowy plik `ui/components/OccurrenceStatusUi.kt`:
   - Przenieś tu z `ui/occurrence/OccurrenceDetailsModels.kt` enum `OccurrenceStatusUi` i funkcję `OccurrenceStatusUi.label()` bez zmian treści.
   - Dodaj mapowanie z domeny:
     ```kotlin
     fun OccurrenceStatus.toUi(): OccurrenceStatusUi = when (this) {
         OccurrenceStatus.SCHEDULED -> OccurrenceStatusUi.Scheduled
         OccurrenceStatus.CANCELLED -> OccurrenceStatusUi.Cancelled
         OccurrenceStatus.MOVED -> OccurrenceStatusUi.Moved
         OccurrenceStatus.MODIFIED -> OccurrenceStatusUi.Changed
         OccurrenceStatus.ONE_OFF -> OccurrenceStatusUi.OneOff
     }
     ```
   - Dodaj tekst plakietki karty; przeniesiony termin ma na karcie „Zmienione”, jak dziś:
     ```kotlin
     /** The word on a class card and in its description; null for an ordinary term. */
     fun OccurrenceStatusUi.cardBadge(): String? = when (this) {
         OccurrenceStatusUi.Cancelled -> "Odwołane"
         OccurrenceStatusUi.OneOff -> "Jednorazowe"
         OccurrenceStatusUi.Changed, OccurrenceStatusUi.Moved -> "Zmienione"
         OccurrenceStatusUi.Scheduled -> null
     }
     ```
   - Przenieś tu z `MakComponents.kt` funkcję `classStatusIcon` jako `@Composable internal fun occurrenceStatusIcon(status: OccurrenceStatusUi): Pair<ImageVector, Color>?`: `Cancelled` daje `Icons.Outlined.EventBusy` z `MaterialTheme.colorScheme.error`, `Changed` i `Moved` dają `Icons.Outlined.EditCalendar` z `modifiedTint()`, `OneOff` daje `Icons.Outlined.LooksOne` z `oneOffTint()`, `Scheduled` daje `null`.
   - W plikach pakietu `ui.occurrence` i ich testach, które używają `OccurrenceStatusUi` albo `label()`, dodaj import `dev.retza.mak.ui.components.OccurrenceStatusUi` i `dev.retza.mak.ui.components.label`. Znajdź je poleceniem `rg -l "OccurrenceStatusUi" app/src`.
6. `ClassItemUi` (`ui/components/PresentationModels.kt`): usuń parametry `statusBadge`, `isCancelled`, `isModified` i `isOneOff`; dodaj parametr `val status: OccurrenceStatusUi = OccurrenceStatusUi.Scheduled` oraz w ciele klasy:
   ```kotlin
   val isCancelled: Boolean
       get() = status == OccurrenceStatusUi.Cancelled
   val statusBadge: String?
       get() = status.cardBadge()
   ```
   W `MakComponents.kt` zastąp wywołania `classStatusIcon(item)` przez `occurrenceStatusIcon(item.status)` i usuń starą funkcję. `item.isCancelled` i `item.statusBadge` w `MakComponents.kt` działają dalej bez zmian.
7. `ui/PlanMapping.kt`, funkcja `PlannedOccurrence.toUi`: usuń zmienne `cancelled`, `modified`, `oneOff` oraz argumenty `statusBadge`, `isCancelled`, `isModified`, `isOneOff`; dodaj `status = status.toUi()`. Dodaj w tym pliku:
   ```kotlin
   internal fun WeekStatus.label(): String = when (this) {
       is WeekStatus.Single -> "Tydzień ${type.name}"
       WeekStatus.Mixed -> "Różne tygodnie"
       WeekStatus.OutsideSemester -> "Poza semestrem"
   }
   ```
8. `ScheduleViewModel`:
   - Usuń funkcję `cancelledItems` i import `cancelledOccurrences`.
   - W `buildSchedule` zamień `cancelledItems(data, control.scheduleDate, activeFilter)` na `selected.cancelled.filter { activeFilter == "all" || it.classItem.semesterProgramId == activeFilter }.map { it.toUi(conflictLabel = null) }`, a `cancelledItems(data, control.calendarDate, activeFilter)` na to samo wyrażenie z `calendarSchedule.cancelled`. Warunek `if (control.showCancelled)` zostaje.
   - Nowe zachowanie do sprawdzenia testem: odwołane terminy ukrytego kierunku już się nie pokazują, bo `selected` pochodzi z danych po `visibleTo`. Komentarz w `buildSchedule` już to obiecuje.
   - `weekTypeLabel = selected.weekStatus.label()`. `weekSourceLabel` i `weekType` zostają, ale warunek `selected.hasMixedWeekTypes` zapisz jako `selected.weekStatus == WeekStatus.Mixed`.
   - `calendarOccurrenceLabel`: zamień `when` na `val status = occurrence.status.toUi().cardBadge()?.let { ", ${it.lowercase()}" }.orEmpty()`. Daje te same teksty („, odwołane”, „, zmienione”, „, jednorazowe”).
9. `TodayViewModel.buildToday`: `weekLabel = schedule.weekStatus.label()`.
10. `WidgetPresenter`: zamiast `weekType`, `mixedWeekTypes` i `if (weekType == null && !mixedWeekTypes)` napisz `val weekStatus = plan.schedule.weekStatus`, `if (weekStatus == WeekStatus.OutsideSemester) return WidgetUiState.OutsideSemester(dateLabel, semesterName)` i `val weekLabel = weekStatus.label()`. Dodaj import `dev.retza.mak.ui.label`, `widget` już korzysta z funkcji z `ui`.
11. `OccurrenceDetailsScreen.kt`, ikona statusu: zamiast lokalnego `when (state.status)` użyj `occurrenceStatusIcon(state.status) ?: (Icons.Outlined.Event to MaterialTheme.colorScheme.onSurfaceVariant)`. Szczegóły dalej pokazują ikonę także dla zwykłego terminu.
12. `docs/ARCHITECTURE.md`, sekcja 7, punkt „Warstwa domenowa”: zdanie „Wynik resolvera zawiera plan aktywny, oznaczenie A/B i źródło korekty.” zamień na „Wynik resolvera zawiera terminy, które się odbywają, odwołane terminy dnia, stan tygodnia (jeden typ, różne tygodnie albo poza semestrem) z tygodniem każdego kalendarza i źródło korekty. Status terminu (zaplanowany, odwołany, przeniesiony, zmieniony, jednorazowy) wynika z terminu w domenie, a interfejs zamienia go na ikonę, plakietkę i tekst w jednym miejscu (`ui/components/OccurrenceStatusUi.kt`).”

Testy:

- `ScheduleResolverTest`: test `movedTermStatusIsMovedAndChangedTermIsModified` (termin przeniesiony na inną datę ma `OccurrenceStatus.MOVED`, termin ze zmianą godziny w tym samym dniu `MODIFIED`, zwykły `SCHEDULED`, jednorazowy `ONE_OFF`).
- `ScheduleResolverTest`: test `weekStatusReportsMixedAndOutsideSemester` (dwa kalendarze o różnym typie tygodnia dają `WeekStatus.Mixed`, data poza kalendarzem `OutsideSemester`, jeden kalendarz `Single(WeekType.A)`; `weeksByCalendarId` ma wpis dla każdego kalendarza obejmującego datę). Dane dwóch kalendarzy znajdziesz w `MultiCalendarResolverTest`.
- `ResolvedCancelledOccurrencesTest`, opisany w zmianie 4.
- `ClassCardDescriptionTest`, `OccurrenceNoteRemapTest`: zamień `statusBadge = "Odwołane", isCancelled = true` na `status = OccurrenceStatusUi.Cancelled` (i analogicznie dla innych wartości); oczekiwane teksty się nie zmieniają.
- `PlanMappingTest`: test `weekStatusLabels` dla trzech wartości `WeekStatus`.
- `ScheduleViewModelTest`: test `cancelledTermOfHiddenProgramIsNotShown` (ukryty kierunek z odwołanym terminem, `setShowCancelled(true)`, lista nie zawiera tego terminu); istniejące testy odwołanych terminów i filtra przechodzą bez zmian oczekiwań.
- `TodayViewModelTest` i `WidgetPresenterTest`: istniejące testy etykiet tygodnia przechodzą bez zmian oczekiwań.

Przypadki brzegowe: odwołanie terminu, który po edycji zajęć już nie wypada w danym dniu, nie pojawia się w `cancelled` (warunek `hasBaseOccurrenceOn`); termin przeniesiony na dzień z innym odwołanym terminem tych samych zajęć daje oba wpisy z różnymi identyfikatorami; odwołany termin nie wchodzi do kolizji ani okienek, bo `CollisionDetector` i `countGaps` czytają tylko `occurrences`; widget nie pokazuje odwołanych terminów.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*" --tests "dev.retza.mak.data.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.schedule.ScheduleScreenTest`, `dev.retza.mak.ui.today.TodayScreenTest` i `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`.
4. Zrzuty przy 320 dp przed zmianą i po niej: „Plan” z włączonym „Pokaż odwołane” w dniu z odwołanym terminem, „Dzisiaj”, szczegóły odwołanego i przeniesionego terminu. Mają być identyczne.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne, `rg "cancelledOccurrences|CancelledOccurrence\b|classStatusIcon|isModified|isOneOff" app/src` nic nie znajduje. Commit: `chore: resolve cancelled terms, term status and week status in one place (I-85)`. W tym commicie w `QUEUE.md` zmień status I-85 na `w toku`.

## 4. Buduj szczegóły terminu z wyniku ScheduleResolver (I-85)

Cel: szczegóły terminu nie składają godzin, sali, daty i statusu z surowych danych, tylko biorą `PlannedOccurrence` z wyniku resolvera (A1). Dzięki temu dni wolne z I-81 trzeba będzie dopisać tylko w resolverze. Tydzień w szczegółach pochodzi z kalendarza kierunku tych zajęć, a nie z całego semestru.

Pliki: `ui/occurrence/OccurrenceViewModel.kt`, `ui/occurrence/OccurrenceDetailsModels.kt` (bez zmian pól, tylko jeśli kompilator wymaga); testy `ui/occurrence/OccurrenceViewModelTest.kt`.

Zmiany:

1. W `OccurrenceViewModel.buildDetails` zastąp początek funkcji aż do wyliczenia `status` włącznie:
   ```kotlin
   val data = inputs.data
   val classId = args.classId
   val originalDate = args.originalDate
   val id = occurrenceId(classId.toString(), originalDate)
   val change = data.occurrenceChanges.firstOrNull {
       it.classId == classId.toString() && it.originalDate == originalDate
   }
   val effectiveDate = change?.targetDate ?: originalDate
   val plan = activePlanProvider.resolve(inputs, effectiveDate)
   val occurrence = (plan.schedule.occurrences + plan.schedule.cancelled).firstOrNull { it.id == id }
       ?: return null
   val base = occurrence.classItem
   val status = occurrence.status.toUi()
   ```
   `change` służy już tylko do wyznaczenia dnia, w którym resolver pokaże ten termin. Dalej w funkcji czytaj zmianę z `occurrence.occurrenceChange`.
2. W konstruktorze `OccurrenceDetailsUiState` w tej funkcji:
   - `dateLabel = occurrence.date.format(occurrenceDateFormatter)`, `currentDate = occurrence.date.toString()`, `targetDateDraft = occurrence.date.toString()`;
   - `startTime`, `endTime`, `startTimeDraft`, `endTimeDraft` z `occurrence.startTime` i `occurrence.endTime`;
   - `room = occurrence.room`, `roomDraft = occurrence.room.orEmpty()`, `building = occurrence.building`, `teacherName = occurrence.teacherName`;
   - `occurrenceNote = occurrence.occurrenceNoteBody`, `occurrenceNoteDraft = occurrence.occurrenceNoteBody.orEmpty()`, `sharedNote = occurrence.classNote`;
   - `originalDateLabel = occurrence.occurrenceChange?.originalDate?.toString()`, `targetDateLabel = occurrence.occurrenceChange?.targetDate?.toString()`, `canCancelOccurrence = canEdit && occurrence.occurrenceChange == null`, `canRestoreOccurrence = occurrence.occurrenceChange != null`;
   - tydzień z kalendarza kierunku: `weekLabel = data.calendarForAssignment(base.semesterProgramId)?.let { plan.schedule.weeksByCalendarId[it.id] }?.let { WeekStatus.Single(it.weekType).label() }`;
   - pola `base*` (stan bazowy do porównania zmian) zostają z `base` jak dziś;
   - usuń zmienne `effectiveStart`, `effectiveEnd`, `effectiveRoom` i `existingNote`, jeśli nic ich już nie czyta.
3. Usuń importy, które przestaną być używane (`OccurrenceChangeKind`, jeśli kompilator tak wskaże). Dodaj importy `dev.retza.mak.domain.WeekStatus`, `dev.retza.mak.ui.label`, `dev.retza.mak.ui.components.toUi`.

Testy (`OccurrenceViewModelTest`):

- `detailsOfHiddenProgramAreNotFound`: ukryj kierunek zajęć (`preferences.setStudyProgramHidden("1", hidden = true)`, wzór w `ClassEditViewModelTest`), `open("1:2026-09-21")`; `details.value.notFound` jest prawdą. To nowe zachowanie: zajęcia ukrytego kierunku nie istnieją w planie na tym telefonie (`DOMAIN.md`, „Kierunki”), więc szczegóły pokazują „Nie znaleziono tych zajęć.”.
- `detailsWeekComesFromTheClassCalendar`: dwa kalendarze z różnym typem tygodnia w tym samym dniu (dane jak w `MultiCalendarResolverTest`); szczegóły zajęć z kalendarza B mają `weekLabel == "Tydzień B"`.
- Istniejące testy szczegółów (odwołanie, przeniesienie, przywrócenie, notatki, zmiana sali) przechodzą bez zmian oczekiwań.

Przypadki brzegowe: odwołany termin jest w `plan.schedule.cancelled` w swoim pierwotnym dniu; przeniesiony termin jest w `occurrences` w dniu docelowym, a jego identyfikator nadal zawiera pierwotną datę; zmiana zapisana dla zajęć, które po edycji nie wypadają w pierwotnym dniu, daje „Nie znaleziono tych zajęć.”, bo takiego terminu nie ma w planie.

Weryfikacja: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.*"`; na emulatorze `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`; zrzuty szczegółów zwykłego, przeniesionego i odwołanego terminu przy 320 dp porównane z zrzutami z kroku 3.

Kryterium zakończenia: testy przechodzą, zrzuty się nie zmieniły, w `buildDetails` nie ma `change?.startTime`, `change?.room` ani `base.recurrence == Recurrence.ONCE`. Commit: `chore: build occurrence details from the resolved plan (I-85)`.

## 5. Przenieś próg okienek do PlanDisplaySettings i wydziel kopię kalendarza (I-85)

Cel: próg okienek płynie razem z planem jak minimalna przerwa, a liczbę okienek zwraca `ActivePlan` (A5, decyzja użytkownika z 2026-10-10: na wypadek pokazania okienek na widgecie albo w powiadomieniach). Granice 20 minut są w jednym obiekcie domenowym. Kopia kalendarza z korektami i sprzątanie osieroconego kalendarza są w jednej funkcji każde (J1), zanim I-81 doda do kalendarza dni wolne.

Pliki: `domain/PlanDisplaySettings.kt`, `domain/ActivePlanProvider.kt`, `domain/CollisionNotificationPlanner.kt`, `ui/settings/SettingsPreferences.kt`, `ui/settings/DataStoreSettingsPreferences.kt`, `ui/settings/SettingsViewModel.kt`, `ui/today/TodayViewModel.kt`, `ui/schedule/ScheduleViewModel.kt`, `data/repository/SemesterRepository.kt`; testy `domain/ActivePlanProviderTest.kt`, `ui/today/TodayViewModelTest.kt`, `ui/settings/SettingsViewModelTest.kt`, `ui/settings/InMemorySettingsPreferences.kt`, `ui/FakeSemesterRepository.kt` (jeśli implementuje `SettingsPreferences`), Android `notifications/CollisionAlarmSchedulerTest.kt`, `sync/RoomPlanSyncGatewayTest.kt`, `ui/settings/SettingsPreferencesTest.kt`, `data/RoomPersistenceTest.kt`; dokument `docs/ARCHITECTURE.md`.

Zmiany:

1. `domain/PlanDisplaySettings.kt`:
   ```kotlin
   data class PlanDisplaySettings(
       val minimumBreakMinutes: Int,
       val gapThresholdMinutes: Int = PlanDisplayLimits.DEFAULT_GAP_THRESHOLD_MINUTES,
       // Study programs hidden on this phone (`DOMAIN.md`, „Kierunki”); their classes are left out of the plan.
       val hiddenProgramIds: Set<String> = emptySet()
   ) {
       companion object {
           val DEFAULT = PlanDisplaySettings(minimumBreakMinutes = 0)
       }
   }

   /**
    * Choices for the two settings. A gap is longer than the threshold and a collision break is not longer
    * than the minimum break, so the smallest threshold is at least the largest minimum break and no break
    * is both (decision of 2026-10-09).
    */
   object PlanDisplayLimits {
       val minimumBreakOptions = listOf(0, 5, 10, 15, 20)
       val gapThresholdOptions = listOf(20, 30, 45, 60)
       const val DEFAULT_GAP_THRESHOLD_MINUTES = 30
       const val MAX_GAP_THRESHOLD_MINUTES = 180
       val maxMinimumBreakMinutes: Int get() = minimumBreakOptions.last()
       val minGapThresholdMinutes: Int get() = gapThresholdOptions.first()

       init {
           check(minGapThresholdMinutes >= maxMinimumBreakMinutes) {
               "The smallest gap threshold must not be below the largest minimum break"
           }
       }
   }
   ```
   Komentarz o regule przenieś z `DataStoreSettingsPreferences.kt` (stałe `minGapThresholdMinutes`, `maxGapThresholdMinutes`, `maxMinimumBreakMinutes`, `defaultGapThresholdMinutes`), a tamte stałe usuń.
2. `DataStoreSettingsPreferences`:
   - `planDisplay` czyta też próg: `gapThresholdMinutes = (stored[gapThresholdMinutesKey] ?: PlanDisplayLimits.DEFAULT_GAP_THRESHOLD_MINUTES).coerceIn(PlanDisplayLimits.minGapThresholdMinutes, PlanDisplayLimits.MAX_GAP_THRESHOLD_MINUTES)`; `minimumBreakMinutes` przez `coerceIn(0, PlanDisplayLimits.maxMinimumBreakMinutes)`.
   - Usuń właściwość `gapThresholdMinutes` (strumień). `setGapThresholdMinutes` i `setMinimumBreakMinutes` używają tych samych granic z `PlanDisplayLimits`.
3. `SettingsPreferences`: usuń `val gapThresholdMinutes: Flow<Int>`. Usuń ją też ze wszystkich implementacji w testach (`InMemorySettingsPreferences`, atrapy w `CollisionAlarmSchedulerTest`, `RoomPlanSyncGatewayTest` i inne, które wskaże kompilator); `InMemorySettingsPreferences.setGapThresholdMinutes` zapisuje teraz wartość do strumienia `planDisplay`.
4. `ActivePlanProvider`:
   - `ActivePlan` dostaje pole `val gapCount: Int`.
   - Zmień sygnaturę `fun resolve(visible: VisiblePlanData, date: LocalDate, minimumBreakMinutes: Int)` na `fun resolve(visible: VisiblePlanData, date: LocalDate, display: PlanDisplaySettings)`; w ciele `collisionDetector.detect(schedule, display.minimumBreakMinutes)` i `gapCount = countGaps(schedule.occurrences, display.gapThresholdMinutes.toLong())`.
   - Dwie pozostałe funkcje `resolve` wołają ją z `inputs.display` albo `display` zamiast `minimumBreakMinutes`.
   - Popraw wywołania: `CollisionNotificationPlanner` (`resolve(visible, date, inputs.display)`) i dwa w `ScheduleViewModel.buildSchedule` (`resolve(visible, ..., inputs.display)`; usuń zmienną `minimumBreak`).
5. `TodayViewModel`: usuń `preferences.gapThresholdMinutes` z `combine` (zostaje `combine(activePlan, date)`), usuń parametr `thresholdMinutes` z `buildToday` i ustaw `gapCount = plan.gapCount`. Jeśli parametr konstruktora `preferences` nie jest już nigdzie używany, usuń go z konstruktora i z `TodayViewModelTest` (Koin dopasuje się sam).
6. `SettingsViewModel`: w `preferencesSnapshot` usuń `preferences.gapThresholdMinutes` z `combine`, a `gapThresholdMinutes` w `PreferencesSnapshot` bierz z `display.gapThresholdMinutes`. Listy `gapThresholdOptions` i `minimumBreakOptions` na końcu pliku zastąp `PlanDisplayLimits.gapThresholdOptions` i `PlanDisplayLimits.minimumBreakOptions`.
7. `SemesterRepository` (`RoomSemesterRepository`), dwie prywatne funkcje na końcu klasy, wołane tylko wewnątrz `database.withTransaction`:
   ```kotlin
   /** A new calendar with the dates, rhythm and week overrides of [source] (`ARCHITECTURE.md`, section 7). */
   private suspend fun copyCalendarWithOverrides(source: AcademicCalendarEntity): Long {
       val calendarId = calendars.insert(
           AcademicCalendarEntity(
               semesterId = source.semesterId,
               startDate = source.startDate,
               endDate = source.endDate,
               firstWeekType = source.firstWeekType
           )
       )
       weekOverrides.getForCalendar(source.id).forEach { override ->
           weekOverrides.insert(override.copy(id = 0, academicCalendarId = calendarId))
       }
       return calendarId
   }

   /** Removes a calendar no study program uses any more, with its week overrides. */
   private suspend fun deleteCalendarIfOrphaned(calendarId: Long) {
       if (calendars.countAssignments(calendarId) == 0) calendars.deleteById(calendarId)
   }
   ```
   - W `addSeparatedSemesterProgram` i `separateSemesterProgramCalendar` zastąp blok `calendars.insert(...)` z pętlą `weekOverrides.getForCalendar(...)` wywołaniem `copyCalendarWithOverrides(source)`.
   - Pięć miejsc z wzorem `if (previousId != newId && calendars.countAssignments(previousId) == 0) { calendars.deleteById(previousId) }` (w `saveStudyProgramAssignment`, `reconnectSemesterProgram`, `saveSemesterProgram`, `saveSetupConfiguration`) zamień na `if (previousId != newId) deleteCalendarIfOrphaned(previousId)`, a w `deleteSemesterProgram` na `deleteCalendarIfOrphaned(existing.academicCalendarId)`. Sprawdź poleceniem `rg -n "countAssignments" app/src/main/java/dev/retza/mak/data/repository/SemesterRepository.kt`: zostają tylko `deleteCalendar` (wymaganie przed usunięciem), `deleteStudyProgram` (inne DAO) i nowa funkcja.
   - Dodaj import `dev.retza.mak.data.entity.AcademicCalendarEntity` i usuń pełne nazwy `dev.retza.mak.data.entity.AcademicCalendarEntity(` w tych miejscach.
8. `docs/ARCHITECTURE.md`: w akapicie o `ActivePlanProvider` (sekcja 5) zdanie „Wymagany parametr `PlanDisplaySettings` przenosi ustawienia telefonu wpływające na wynik (od I-80 minimalną przerwę).” zamień na „Wymagany parametr `PlanDisplaySettings` przenosi ustawienia telefonu wpływające na wynik: minimalną przerwę (I-80), ukryte kierunki (I-79) i próg okienek (I-85); `ActivePlan` zwraca kolizje i liczbę okienek. Dozwolone wartości obu progów są w `PlanDisplayLimits`.” W sekcji 7, „Warstwa domenowa”, po zdaniu o `countGaps` dopisz „Liczbę okienek liczy `ActivePlanProvider` z progiem z `PlanDisplaySettings`.”

Testy:

- `ActivePlanProviderTest`: `gapCountUsesThresholdFromDisplaySettings` (dwa bloki zajęć z przerwą 40 minut: przy progu 30 jedno okienko, przy progu 45 zero).
- `TodayViewModelTest`: istniejący test okienek ustawia próg przez `preferences.setGapThresholdMinutes(...)` albo `PlanDisplaySettings` i przechodzi.
- `SettingsViewModelTest`: zaznaczona opcja progu odpowiada wartości z `planDisplay`; lista opcji to `20, 30, 45, 60`.
- `SettingsPreferencesTest` (Android): zapis progu 15 jest czytany jako 20, a 45 jako 45, przez `planDisplay.first().gapThresholdMinutes`.
- `RoomPersistenceTest` (Android): istniejące testy rozdzielenia kalendarza, ponownego połączenia i usunięcia nieużywanego kalendarza przechodzą bez zmian oczekiwań.

Przypadki brzegowe: zmiana progu odświeża teraz widget i alarmy, bo zmienia `planDisplay`; to zamierzone (decyzja z 2026-10-10); zapisana wartość spoza zakresu jest przycinana do 20 albo 180 jak dziś; rozdzielenie kalendarza bez korekt kopiuje sam kalendarz.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze `dev.retza.mak.ui.settings.SettingsPreferencesTest`, `dev.retza.mak.data.RoomPersistenceTest`, `dev.retza.mak.notifications.CollisionAlarmSchedulerTest` i `dev.retza.mak.di.KoinGraphTest`.
4. Ręcznie: zmiana progu okienka w ustawieniach od razu zmienia liczbę okienek na „Dzisiaj” (dane demonstracyjne).

Kryterium zakończenia: testy przechodzą, `rg "gapThresholdMinutes: Flow|minGapThresholdMinutes =|maxMinimumBreakMinutes =" app/src/main` znajduje tylko `PlanDisplayLimits`, `py scripts/check_map.py` zwraca 0. Commit: `feat: carry the gap threshold with plan settings and share calendar copy (I-85)`. Potem I-85 w `QUEUE.md` przechodzi na `gotowe`, jeśli zrzuty z kroków 3 i 4 były identyczne; inaczej na `odbiór otwarty` z listą różnic. Następny plan dotyczy I-86 i I-87.
