# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

Kroki audytu interfejsu z 2026-09-28 (I-54 do I-62) są wykonane; ich odbiór należy do O-05 i O-06. Poniższe kroki realizują I-64: wspólną obsługę operacji zapisu w ViewModelach i uproszczenie ich testów (przegląd testów z 2026-09-29, `STACK.md`, sekcja 5, „Sposób testowania”). Tryb tabletowy (I-45 do I-47) trzeba rozpisać po I-64. Odbiór na telefonie (O-07, O-09, I-40, I-49) wykonuje użytkownik; listy kontrolne są w odpowiednich wierszach `QUEUE.md`.

## Zasady wspólne dla wszystkich kroków

- Kroki 1 do 4 wykonuj po kolei na jednej gałęzi `task/I-64-shared-ui-operation` od aktualnego `origin/main`, z jednym pull requestem na końcu. Każdy krok kończy się kompilującym się kodem, zielonymi testami i osobnym commitem (`feat:` dla kroku 1, `chore:` dla kroków 2 do 4). Gałąź i pull request prowadź według `WORKFLOW.md`, sekcja „Gałąź i pull request”.
- Przed zmianą przeczytaj wskazane pliki w całości. Numery linii są orientacyjne; szukaj po nazwie funkcji albo tekście.
- To refaktor bez zmiany zachowania. Teksty komunikatów, rodzaje komunikatów (`UiFeedbackKind`), efekty nawigacyjne, stany widoku po sukcesie i po błędzie oraz kolejność wywołań repozytorium zostają takie same. Jedyna dozwolona różnica: flaga trwania operacji jest zawsze czyszczona na końcu, także tam, gdzie dziś czyści ją tylko blok sukcesu i `catch` (`StudyProgramsViewModel.save`, `SetupViewModel.saveAnotherProgram`).
- Drobne różnice między opisem a kodem rozwiąż zgodnie z celem kroku i opisz w treści commita. Zatrzymaj się i zapisz bloker w `QUEUE.md` przy I-64 tylko wtedy, gdy akcja nie pasuje do opisanego wzorca i nie da się jej przenieść bez zmiany zachowania.
- `ScheduleViewModel` zostaje poza zakresem: jego prywatna `runReportingFailure` obsługuje dwie operacje bez blokady i nie ma testów wzorca.
- Testy usuwaj tylko te, które są wymienione w krokach. Jeśli wymieniony test ma asercję, której nie ma nigdzie indziej i która nie wynika ze wzorca (blokada, anulowanie, jeden komunikat błędu, czyszczenie flagi), przenieś ją do wskazanego testu, który zostaje.
- Testy JVM: `gradlew.bat test`. Testy na urządzeniu: `gradlew.bat connectedDebugAndroidTest` przy uruchomionym emulatorze (`adb devices`); jeśli przebieg przerwie się błędem emulatora (na przykład „Can't find service: package”), uruchom emulator ponownie i powtórz.
- Po zmianie `docs/*.md` uruchom `py scripts/check_map.py`.

## 1. Dodaj wspólną funkcję operacji zapisu (I-64)

Cel: jedna funkcja realizuje wzorzec, który dziś jest skopiowany w 27 akcjach sześciu ViewModeli: blokada drugiego wywołania, przepuszczenie anulowania bez komunikatu, jeden komunikat błędu, pominięcie komunikatów po zmianie sesji i czyszczenie flagi. Ten krok dodaje funkcję i jej testy, bez zmian w ViewModelach.

Pliki: nowe `app/src/main/java/dev/retza/mak/ui/feedback/UiOperation.kt` i `app/src/test/java/dev/retza/mak/ui/feedback/UiOperationTest.kt`; wzorce do przeczytania: `ui/feedback/FeedbackController.kt` (`FeedbackSink`), `ui/feedback/UiFeedback.kt`, `SettingsViewModel.confirmSemesterDeletion`.

1. W `UiOperation.kt` (pakiet `dev.retza.mak.ui.feedback`) dodaj:

   ```kotlin
   internal fun CoroutineScope.launchUiOperation(
       isRunning: () -> Boolean,
       onStart: () -> Unit,
       onFinish: () -> Unit,
       feedbackSink: FeedbackSink,
       errorMessage: String?,
       onError: () -> Unit = {},
       isCurrent: () -> Boolean = { true },
       block: suspend () -> UiFeedback?
   ): Job?
   ```

   Zachowanie, w tej kolejności:
   - gdy `isRunning()` zwraca `true`, funkcja nic nie robi i zwraca `null`;
   - woła `onStart()` synchronicznie, przed uruchomieniem korutyny;
   - uruchamia `launch { ... }` i zwraca jego `Job`;
   - w korutynie wykonuje `block()`; jeśli zwrócił komunikat i `isCurrent()` jest prawdą, publikuje go przez `feedbackSink`;
   - `CancellationException` rzuca dalej, bez `onError` i bez komunikatu;
   - przy innym `Exception`, jeśli `isCurrent()` jest prawdą, woła `onError()`, a potem publikuje `UiFeedback(errorMessage, UiFeedbackKind.Error)`, o ile `errorMessage` nie jest `null`;
   - w `finally` zawsze woła `onFinish()`.

   Dodaj KDoc w jednym zdaniu po angielsku z odwołaniem do I-64. `block` sam robi zapis i zmiany stanu po sukcesie; gdy sesja się zmieniła, kończy się przez `return@launchUiOperation null`.
2. W `UiOperationTest.kt` napisz sześć testów JVM z `runTest` i własnym prywatnym `RecordingFeedbackSink` (lista opublikowanych `UiFeedback`), wywołując funkcję na `backgroundScope` albo na `this` z `advanceUntilIdle()`:
   - `secondCallWhileRunningIsIgnored`: `block` wstrzymany na `CompletableDeferred`; drugie wywołanie zwraca `null`, `block` wykonał się raz, `onStart` raz;
   - `successPublishesReturnedFeedbackAndFinishes`: jeden komunikat sukcesu, `onError` nie wywołany, `onFinish` raz;
   - `failurePublishesOneErrorAfterOnErrorAndFinishes`: `block` rzuca `IllegalStateException`; `onError` wywołany przed publikacją, dokładnie jeden komunikat typu `Error` z podanym tekstem, `onFinish` raz;
   - `failureWithoutMessageOnlyRunsOnError`: `errorMessage = null`; `onError` raz, brak komunikatów;
   - `cancellationPublishesNothingAndFinishes`: `job.cancel()` w trakcie wstrzymanego `block`; brak komunikatów, `onError` nie wywołany, `onFinish` raz;
   - `staleOperationSkipsFeedbackButFinishes`: `isCurrent = { false }`, osobno dla sukcesu i błędu; brak komunikatów, `onError` nie wywołany, `onFinish` wywołany.

Przypadki brzegowe: `onFinish` po anulowaniu; `onError` nie może zostać wywołany po zmianie sesji; `block` zwracający `null` przy sukcesie nie publikuje niczego.

Weryfikacja: `gradlew.bat test`.

Kryterium zakończenia: `launchUiOperation` istnieje, sześć testów przechodzi, żaden ViewModel jeszcze jej nie używa.

## 2. Przenieś zapisy w szczegółach terminu i formularzu zajęć (I-64)

Cel: akcje zapisu w `OccurrenceViewModel` i `ClassEditViewModel` korzystają z `launchUiOperation`, a ich testy nie powtarzają przypadków wzorca.

Pliki: `ui/occurrence/OccurrenceViewModel.kt`, `ui/edit/ClassEditViewModel.kt`, `test/.../ui/occurrence/OccurrenceViewModelTest.kt`, `test/.../ui/edit/ClassEditViewModelTest.kt`.

Sposób przeniesienia każdej akcji (dotyczy też kroków 3 i 4):
- sprawdzenie blokady, które dziś stoi przed walidacją, zostaje na początku funkcji jako `if (<flaga>) return`, aby podwójne dotknięcie w trakcie zapisu nie uruchamiało walidacji; `launchUiOperation` sprawdza blokadę ponownie z tym samym `isRunning`;
- walidacja i wczesne `return` zostają przed wywołaniem `launchUiOperation`;
- `onStart` robi to samo co dzisiejsza aktualizacja stanu przed `launch` (flaga i czyszczenie błędów);
- `onFinish` robi to samo co dzisiejszy `finally`;
- `onError` robi to samo co dzisiejszy `catch (Exception)` poza publikacją komunikatu, a `errorMessage` to tekst dzisiejszego komunikatu błędu;
- `block` robi zapis i zmiany po sukcesie i zwraca dzisiejszy komunikat sukcesu (`UiFeedback(..., UiFeedbackKind.Success)`) albo `null`, gdy go nie ma.

1. `OccurrenceViewModel`:
   - `cancelOccurrence` i `restoreOccurrence`: `isRunning = { occurrenceStateOperationRunning }`, `onStart = { occurrenceStateOperationRunning = true }`, `onFinish = { occurrenceStateOperationRunning = false }`; w `cancelOccurrence` `onError` ustawia `draftError = "Nie udało się odwołać terminu."`; w `restoreOccurrence` wczesny powrót z bloku, gdy nie ma zmiany terminu, zwraca `null`;
   - `saveSharedNote`, `saveOccurrenceNote` i `saveOccurrenceChange`: flagi `isSavingSharedNote`, `isSavingOccurrenceNote` i `isSaving`; `onError` ustawia odpowiednio `sharedNoteError`, `occurrenceNoteError` i `draftError`, tak jak dziś;
   - `deleteSelectedClass` zostaje bez zmian: nie ma blokady ani `finally`, a jego zachowanie sprawdzają osobne testy.
2. `ClassEditViewModel.save(confirmedHiddenData)`: flaga `isSaving`, `errorMessage = "Nie udało się zapisać zajęć."`, efekt zamknięcia i reset formularza w `block`.
3. Usuń testy z `OccurrenceViewModelTest`: `repeatedSharedNoteSaveRunsOnce`, `repeatedOccurrenceNoteSaveRunsOnce`, `repeatedOccurrenceChangeSaveRunsOnce`, `repeatedCancelRunsOnce`, `repeatedRestoreRunsOnce`, `cancellationDoesNotPublishErrorAndClearsSavingFlags`, `repositoryErrorPublishesSingleErrorAndKeepsDraft`. Zostają testy komunikatów zależnych od danych (`modifiedChangePublishesSuccessAfterSave`, `movedChangePublishesMovedMessage`, `cancelAndRestorePublishMessages`, `sharedNotePublishesSaveAndDeleteMessages`, `occurrenceNotePublishesSaveAndDeleteMessages`) oraz testy stanu po błędzie (`sharedNoteSaveErrorKeepsDraftAndClearsSaving`, `occurrenceChangeErrorKeepsDraftAndPublishesOneError`).
4. Usuń testy z `ClassEditViewModelTest`: `saveRunsOnceAndEmitsCloseEffectOnce` i `saveErrorPublishesOneErrorAndKeepsForm`. Jeśli `savePersistsNewClassAndResetsEditor` nie sprawdza efektu `ClassEditEffect.CloseEditor`, dopisz tę asercję.

Przypadki brzegowe: odwołanie i przywrócenie jednego terminu po sobie (wspólna flaga); notatka bez zmiany treści nie uruchamia operacji; zapis ukrywający dane najpierw pokazuje dialog i nie uruchamia operacji.

Weryfikacja: `gradlew.bat test`; `OccurrenceDetailsScreenTest` na emulatorze.

Kryterium zakończenia: w obu plikach nie ma już `catch (error: CancellationException)` w akcjach zapisu poza `deleteSelectedClass`; wymienione testy są usunięte; pozostałe testy przechodzą bez zmian asercji.

## 3. Przenieś zapisy konfiguracji semestru i kierunków (I-64)

Cel: akcje zapisu w `SemesterViewModel` i `StudyProgramsViewModel` korzystają z `launchUiOperation`.

Pliki: `ui/semester/SemesterViewModel.kt`, `ui/programs/StudyProgramsViewModel.kt` i ich testy JVM.

1. `SemesterViewModel`: przenieś `saveSemester`, `saveCalendar`, `addCourse`, `separateCourseCalendar`, `confirmReconnect`, `confirmCalendarDeletion`, `confirmCourseDeletion`, `saveWeekOverride` i `confirmWeekOverrideDeletion` według sposobu z kroku 2. Każda z nich przekazuje `isCurrent = { isCurrentSession(token) }` z tokenem pobranym przed wywołaniem, tak jak dziś. Dzisiejsze `if (!isCurrentSession(token)) return@launch` w bloku sukcesu zamień na `return@launchUiOperation null`. `onError` przenosi stan błędu tam, gdzie jest: `saveSemester` i `saveCalendar` (błąd przy formularzu dat), `confirmReconnect` (`reconnectError`). Bez zmian zostają `open` (wczytywanie) i `requestCourseDeletion` (odczyt liczby zajęć bez blokady).
2. `StudyProgramsViewModel.save`: flaga `editor.isSaving`, reset edytora w `block`, `errorMessage = "Nie udało się zapisać kierunku."`.
3. Usuń testy z `SemesterViewModelTest`: `cancellationDoesNotPublishOrKeepSaving`, `addCourseRunsOnce`, `addCourseErrorKeepsDraftAndPublishesError`, `addCourseCancellationDoesNotPublish`, `deleteCourseErrorKeepsItemAndPublishesError`, `saveOverrideRunsOnce`, `saveOverrideErrorKeepsFormOpen`, `deleteOverrideCancellationDoesNotPublish`.
4. Zmień w `SemesterViewModelTest` trzy testy na testy jednego wywołania, bez drugiego wywołania i bez asercji o liczbie zapisów: `saveRunsOnceAndPublishesSuccessWithOneEffect` na `saveSemesterPublishesSuccessAndClosesConfiguration`, `deleteCourseRunsOnceAndPublishesSuccess` na `deleteCoursePublishesSuccessAndClearsPending`, `deleteOverrideRunsOnceAndPublishesSuccess` na `deleteOverridePublishesSuccessAfterConfirmation` (asercja, że samo `requestWeekOverrideDeletion` niczego nie usuwa, zostaje). Zostają `saveErrorKeepsFormAndPublishesError` (stan błędu formularza) i `lateSaveFromPreviousSessionDoesNotCloseOrModifyCurrentSemester` (sesja).
5. Usuń testy z `StudyProgramsViewModelTest`: `doubleSaveWritesOnce` i `failedSaveKeepsDraftAndPublishesError`.

Przypadki brzegowe: zapis z poprzedniej sesji semestru nie publikuje komunikatu ani nie zamyka nowej; dodanie kierunku z osobnym kalendarzem wybiera inną operację repozytorium niż kierunek ze wspólnym kalendarzem.

Weryfikacja: `gradlew.bat test`; `SemesterScreenTest` i `StudyProgramsScreenTest` na emulatorze.

Kryterium zakończenia: wymienione akcje używają `launchUiOperation`; wymienione testy są usunięte albo zmienione; pozostałe testy przechodzą.

## 4. Przenieś zapisy ustawień i kreatora oraz zamknij I-64 (I-64)

Cel: akcje zapisu w `SettingsViewModel` i `SetupViewModel` korzystają z `launchUiOperation`, a dokumentacja opisuje wzorzec.

Pliki: `ui/settings/SettingsViewModel.kt`, `ui/setup/SetupViewModel.kt`, ich testy JVM, `docs/ARCHITECTURE.md` (sekcja 5), `docs/QUEUE.md`, `docs/LOG.md`.

1. `SettingsViewModel`: przenieś `selectSemester`, `confirmSemesterDeletion`, `selectTheme`, `exportJson`, `setGapThresholdMinutes`, prywatną `saveNotifications`, `prepareImport` i `confirmImport`. `exportJson` zwraca z bloku `null`, bo komunikat publikuje dziś osobna funkcja po zapisie pliku. `prepareImport` ma `errorMessage = null` i `onError` ustawiający `importErrorMessage = "Nie udało się odczytać pliku."`; gałąź `ImportPreparation.Invalid` zostaje w bloku. `confirmImport` ma `onError` ustawiający `importErrorMessage` i komunikat błędu jak dziś.
2. `SetupViewModel`: przenieś `saveConfiguration`, `saveAnotherProgram` i `activateAndAddClass` z `isCurrent = { token == sessionToken }`. Tam, gdzie dziś wynik trafia do `saveJob`, zapisz go tylko wtedy, gdy funkcja nie zwróciła `null` (`launchUiOperation(...)?.let { saveJob = it }`).
3. Usuń testy z `SettingsViewModelTest`: `themeWriteFailurePublishesSingleErrorAndClearsFlag`, `gapThresholdWriteFailurePublishesItsOwnError`, `doubleThemeSelectionRunsOneWrite`, `selectSemesterFailureKeepsActiveAndPublishesError`, `doubleSemesterSelectionRunsOneOperation`, `deleteFailureKeepsDialogAndPublishesError`, `doubleConfirmRunsOneDeletion`, `cancelledDeletionPublishesNoErrorAndClearsFlag`, `exportReadFailurePublishesSingleError`. Zostają `exportWriteFailurePublishesSingleError` (osobna ścieżka zapisu pliku), `notificationWriteFailurePublishesError` (stan przełącznika po błędzie) i `importReadErrorSetsMessage`.
4. Usuń testy z `SetupViewModelTest`: `doubleClickRunsOneTransactionAndOneFeedback`, `failedSavePublishesSingleError`, `failedSaveKeepsDraftsAndStep`, `failedAnotherProgramKeepsTheFormAndReportsError`. Zostają `successfulSavePublishesSingleMessage` (komunikat zależy od aktywacji) i `startDuringSaveResetsTheWizardWithoutFeedback` (sesja).
5. W `ARCHITECTURE.md`, sekcja 5, dopisz zasadę: akcja zapisu uruchamiana z interfejsu używa `launchUiOperation`; test ViewModelu sprawdza tylko własną logikę akcji (walidację, wybór operacji repozytorium, stan po zapisie, komunikat zależny od danych, stan po błędzie ustawiany przez `onError`), a nie blokadę, anulowanie ani pojedynczy komunikat błędu. Dodaj wpis do `LOG.md` z liczbą testów JVM przed krokiem 1 i po kroku 4. Zmień status I-64 w `QUEUE.md` na `gotowe`, bo zmiana nie wymaga odbioru na urządzeniu, i przenieś wiersz do sekcji „Zakończone”.

Przypadki brzegowe: podwójne dotknięcie „Dalej” w kreatorze w trakcie zapisu; rozpoczęcie kreatora od nowa w trakcie zapisu (anulowanie `saveJob`); import za dużego pliku (komunikat przy polu, bez komunikatu na dole ekranu).

Weryfikacja: `gradlew.bat test`, pełny `gradlew.bat connectedDebugAndroidTest`, `py scripts/check_map.py`; na emulatorze: zmiana motywu, eksport i import pliku, utworzenie semestru w kreatorze.

Kryterium zakończenia: w sześciu ViewModelach z I-64 jedynymi blokami `catch (error: CancellationException)` są `OccurrenceViewModel.deleteSelectedClass`, `SemesterViewModel.open` i `SemesterViewModel.requestCourseDeletion`; wszystkie testy przechodzą; opis pull requesta podaje liczbę testów JVM przed zmianą i po niej; dokumentacja zaktualizowana.
