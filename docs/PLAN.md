# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

Kroki audytu interfejsu z 2026-09-28 (I-54 do I-62) są wykonane; ich odbiór należy do O-05 i O-06. Poniższe kroki realizują I-64: wspólną obsługę błędów operacji zapisu w ViewModelach i uproszczenie ich testów (przegląd testów z 2026-09-29, `STACK.md`, sekcja 5, „Sposób testowania”). Tryb tabletowy (I-45 do I-47) trzeba rozpisać po I-64. Odbiór na telefonie (O-07, O-09, I-40, I-49) wykonuje użytkownik; listy kontrolne są w odpowiednich wierszach `QUEUE.md`.

## Zasady wspólne dla wszystkich kroków

- Krok 1 i przeniesienie `SemesterViewModel.saveSemester` z kroku 3 są wykonane na lokalnej gałęzi `task/I-64-shared-ui-operation` (próba z 2026-09-29: wszystkie dotychczasowe testy przechodzą bez zmian, a mutacje funkcji i akcji potwierdziły, które testy je pilnują). Kroki 1 do 4 wykonuj po kolei na jednej gałęzi `task/I-64-shared-ui-operation` od aktualnego `origin/main`, z jednym pull requestem na końcu. Każdy krok kończy się kompilującym się kodem, zielonymi testami i osobnym commitem (`feat:` dla kroku 1, `chore:` dla kroków 2 do 4). Gałąź i pull request prowadź według `WORKFLOW.md`, sekcja „Gałąź i pull request”.
- Przed zmianą przeczytaj wskazane pliki w całości. Numery linii są orientacyjne; szukaj po nazwie funkcji albo tekście.
- To refaktor bez zmiany zachowania. Teksty i rodzaje komunikatów, efekty nawigacyjne, ich kolejność względem komunikatów, stany widoku po sukcesie i po błędzie oraz kolejność wywołań repozytorium zostają takie same. Dozwolone są tylko dwie różnice: flaga trwania operacji jest zawsze czyszczona na końcu, także tam, gdzie dziś czyści ją tylko blok sukcesu i `catch` (`StudyProgramsViewModel.save`, `SetupViewModel.saveAnotherProgram`), a komunikat błędu `SetupViewModel.saveAnotherProgram` pomija się po zmianie sesji kreatora, jak w pozostałych akcjach kreatora.
- Blokada podwójnego wywołania zostaje w ViewModelu, tak jak dziś: `if (<flaga>) return` na początku akcji i ustawienie flagi tuż przed wywołaniem wspólnej funkcji. Wspólna funkcja nie przejmuje blokady, bo każda akcja ma własną flagę, a pomylenie flag przy przenoszeniu wykrywa tylko test tej akcji.
- Poza zakresem są operacje, które nie są zapisem uruchamianym przez użytkownika: zbieranie przepływów w `init` (`SetupViewModel`, `StudyProgramsViewModel`), `SemesterViewModel.open`, `SemesterViewModel.requestCourseDeletion` (odczyt liczby zajęć), `OccurrenceViewModel.deleteSelectedClass` (bez blokady i bez `finally`) oraz cały `ScheduleViewModel` z prywatną `runReportingFailure`.
- Drobne różnice między opisem a kodem rozwiąż zgodnie z celem kroku i opisz w treści commita. Zatrzymaj się i zapisz bloker w `QUEUE.md` przy I-64 tylko wtedy, gdy akcja nie pasuje do opisanego wzorca i nie da się jej przenieść bez zmiany zachowania.
- Testy usuwaj tylko te, które są wymienione w krokach. Jeśli wymieniony test ma asercję, której nie ma nigdzie indziej i która nie wynika ze wspólnej funkcji (anulowanie, jeden komunikat błędu, czyszczenie flagi), przenieś ją do wskazanego testu, który zostaje. Testy podwójnego wywołania zostają przy akcjach, których powtórzenie utworzyłoby zduplikowane dane albo wysłałoby drugi efekt nawigacyjny (drugie zamknięcie ekranu); usuwane są tylko przy akcjach, których powtórzenie niczego nie psuje (aktualizacja bez efektu, usunięcie, wybór ustawienia).
- W komentarzach kodu nie wpisuj identyfikatorów zadań. Testy JVM: `gradlew.bat test`. Testy na urządzeniu: `gradlew.bat connectedDebugAndroidTest` przy uruchomionym emulatorze (`adb devices`); jeśli przebieg przerwie się błędem emulatora (na przykład „Can't find service: package”), uruchom emulator ponownie i powtórz. Po zmianie `docs/*.md` uruchom `py scripts/check_map.py`.

## 1. Dodaj wspólną obsługę błędów operacji zapisu (I-64)

Cel: jedna funkcja realizuje część wzorca skopiowaną dziś w 27 akcjach sześciu ViewModeli: przepuszczenie anulowania bez komunikatu, jeden komunikat błędu, pominięcie reakcji na błąd po zmianie sesji i czyszczenie flagi w `finally`. Ten krok dodaje funkcję i jej testy, bez zmian w ViewModelach.

Pliki: nowe `app/src/main/java/dev/retza/mak/ui/feedback/UiOperation.kt` i `app/src/test/java/dev/retza/mak/ui/feedback/UiOperationTest.kt`; wzorce do przeczytania: `ui/feedback/FeedbackController.kt` (`FeedbackSink`), `ui/feedback/UiFeedback.kt`, `SettingsViewModel.confirmSemesterDeletion`.

1. W `UiOperation.kt` (pakiet `dev.retza.mak.ui.feedback`) dodaj:

   ```kotlin
   internal fun CoroutineScope.launchUiOperation(
       feedbackSink: FeedbackSink,
       errorMessage: String?,
       onFinish: () -> Unit,
       onError: () -> Unit = {},
       isCurrent: () -> Boolean = { true },
       block: suspend () -> Unit
   ): Job
   ```

   Zachowanie: uruchamia `launch` i zwraca jego `Job`; w korutynie wykonuje `block()`. `CancellationException` rzuca dalej, bez `onError` i bez komunikatu. Przy innym `Exception`, jeśli `isCurrent()` jest prawdą, woła `onError()`, a potem publikuje `UiFeedback(errorMessage, UiFeedbackKind.Error)`, o ile `errorMessage` nie jest `null`. W `finally` zawsze woła `onFinish()`. Komunikat sukcesu, efekty i zmiany stanu po sukcesie `block` robi sam, w dzisiejszej kolejności; po zmianie sesji kończy się przez `return@launchUiOperation`. Dodaj KDoc w jednym zdaniu po angielsku.
2. W `UiOperationTest.kt` napisz pięć testów JVM z `runTest`, `advanceUntilIdle()` i własnym prywatnym `RecordingFeedbackSink` (lista opublikowanych `UiFeedback`):
   - `successPublishesNothingAndFinishes`: `block` kończy się poprawnie; brak komunikatów od funkcji, `onError` nie wywołany, `onFinish` raz;
   - `failurePublishesOneErrorAfterOnErrorAndFinishes`: `block` rzuca `IllegalStateException`; `onError` wywołany przed publikacją, dokładnie jeden komunikat typu `Error` z podanym tekstem, `onFinish` raz;
   - `failureWithoutMessageOnlyRunsOnError`: `errorMessage = null`; `onError` raz, brak komunikatów;
   - `cancellationPublishesNothingAndFinishes`: `block` wstrzymany na `CompletableDeferred`, potem `job.cancel()`; brak komunikatów, `onError` nie wywołany, `onFinish` raz;
   - `staleFailureSkipsErrorHandlingButFinishes`: `isCurrent = { false }` i `block` rzucający wyjątek; brak komunikatów, `onError` nie wywołany, `onFinish` raz.

Przypadki brzegowe: `onFinish` po anulowaniu; `onError` nie może zostać wywołany po zmianie sesji.

Weryfikacja: `gradlew.bat test`.

Kryterium zakończenia: `launchUiOperation` istnieje, pięć testów przechodzi, żaden ViewModel jeszcze jej nie używa. Stan: wykonane.

## 2. Przenieś zapisy w szczegółach terminu i formularzu zajęć (I-64)

Cel: akcje zapisu w `OccurrenceViewModel` i `ClassEditViewModel` korzystają z `launchUiOperation`, a ich testy nie powtarzają przypadków sprawdzonych w kroku 1.

Pliki: `ui/occurrence/OccurrenceViewModel.kt`, `ui/edit/ClassEditViewModel.kt`, `test/.../ui/occurrence/OccurrenceViewModelTest.kt`, `test/.../ui/edit/ClassEditViewModelTest.kt`.

Sposób przeniesienia każdej akcji (dotyczy też kroków 3 i 4): blokada, walidacja i ustawienie flagi zostają bez zmian przed wywołaniem; `viewModelScope.launch { try { ... } catch ... finally { ... } }` zamień na `viewModelScope.launchUiOperation(...) { ... }`. Treść dzisiejszego `try` trafia do `block` bez zmian, poza zamianą `return@launch` na `return@launchUiOperation`. `onFinish` robi dokładnie to samo co dzisiejszy `finally`, razem z warunkami; na przykład w `SemesterViewModel` flaga jest czyszczona tylko przy aktualnej sesji i tak ma zostać. `onError` robi to samo co dzisiejszy `catch (Exception)` poza publikacją komunikatu, a `errorMessage` to tekst dzisiejszego komunikatu błędu.

1. `OccurrenceViewModel`: przenieś `cancelOccurrence`, `restoreOccurrence` (wspólna zmienna `occurrenceStateOperationRunning`; w `cancelOccurrence` `onError` ustawia `draftError = "Nie udało się odwołać terminu."`), `saveSharedNote`, `saveOccurrenceNote` i `saveOccurrenceChange` (`onError` ustawia odpowiednio `sharedNoteError`, `occurrenceNoteError` i `draftError` razem z wyzerowaniem flagi, tak jak dziś).
2. `ClassEditViewModel.save(confirmedHiddenData)`: `errorMessage = "Nie udało się zapisać zajęć."`; komunikat sukcesu i efekt `CloseEditor` zostają w `block` w dzisiejszej kolejności.
3. Usuń testy z `OccurrenceViewModelTest`: `repeatedSharedNoteSaveRunsOnce` i `repeatedRestoreRunsOnce` (powtórzenie niczego nie psuje), `cancellationDoesNotPublishErrorAndClearsSavingFlags`, `repositoryErrorPublishesSingleErrorAndKeepsDraft` (ten sam przypadek sprawdza `sharedNoteSaveErrorKeepsDraftAndClearsSaving`). Zostają `repeatedOccurrenceNoteSaveRunsOnce`, `repeatedOccurrenceChangeSaveRunsOnce` i `repeatedCancelRunsOnce`, bo powtórzenie mogłoby zapisać drugi rekord.
4. Usuń z `ClassEditViewModelTest` test `saveErrorPublishesOneErrorAndKeepsForm`. `saveRunsOnceAndEmitsCloseEffectOnce` zostaje, bo powtórzenie dodałoby drugie zajęcia.

Przypadki brzegowe: odwołanie i przywrócenie jednego terminu po sobie (wspólna zmienna blokady); notatka bez zmiany treści nie uruchamia operacji; zapis ukrywający dane najpierw pokazuje dialog i nie uruchamia operacji.

Weryfikacja: `gradlew.bat test`; `OccurrenceDetailsScreenTest` na emulatorze.

Kryterium zakończenia: sześć wymienionych akcji używa `launchUiOperation`; pięć wymienionych testów usunięto; pozostałe testy obu klas przechodzą bez zmiany asercji.

## 3. Przenieś zapisy konfiguracji semestru i kierunków (I-64)

Cel: akcje zapisu w `SemesterViewModel` i `StudyProgramsViewModel` korzystają z `launchUiOperation`.

Pliki: `ui/semester/SemesterViewModel.kt`, `ui/programs/StudyProgramsViewModel.kt` i ich testy JVM.

1. `SemesterViewModel`: przenieś `saveCalendar`, `addCourse`, `separateCourseCalendar`, `confirmReconnect`, `confirmCalendarDeletion`, `confirmCourseDeletion`, `saveWeekOverride` i `confirmWeekOverrideDeletion` według sposobu z kroku 2 i wzoru już przeniesionej `saveSemester`, z `isCurrent = { isCurrentSession(token) }` i tokenem pobranym przed wywołaniem, tak jak dziś. `onError` przenosi stan błędu tam, gdzie jest: `saveSemester` i `saveCalendar` (błąd przy formularzu dat), `confirmReconnect` (`reconnectError`).
2. `StudyProgramsViewModel.save`: `onFinish` zeruje `editor.isSaving`, reset edytora zostaje w `block`, `errorMessage = "Nie udało się zapisać kierunku."`.
3. Usuń testy z `SemesterViewModelTest`: `cancellationDoesNotPublishOrKeepSaving`, `addCourseErrorKeepsDraftAndPublishesError`, `addCourseCancellationDoesNotPublish`, `deleteCourseErrorKeepsItemAndPublishesError`, `saveOverrideErrorKeepsFormOpen`, `deleteOverrideCancellationDoesNotPublish`. Zostają `addCourseRunsOnce` i `saveOverrideRunsOnce` (powtórzenie dodałoby drugi rekord), `saveRunsOnceAndPublishesSuccessWithOneEffect` (powtórzenie zamknęłoby dwa ekrany), `saveErrorKeepsFormAndPublishesError` (stan błędu formularza dat) i `lateSaveFromPreviousSessionDoesNotCloseOrModifyCurrentSemester` (sesja na ścieżce sukcesu; ścieżki błędu po zmianie sesji dziś żaden test nie sprawdza).
4. Zmień w `SemesterViewModelTest` dwa testy akcji, których powtórzenie niczego nie psuje, na testy jednego wywołania: usuń drugie wywołanie i asercję o liczbie zapisów, zachowaj pozostałe asercje i nadaj nazwy `deleteCoursePublishesSuccessAndClearsPending` (z `deleteCourseRunsOnceAndPublishesSuccess`) i `deleteOverridePublishesSuccessAfterConfirmation` (z `deleteOverrideRunsOnceAndPublishesSuccess`).
5. Usuń z `StudyProgramsViewModelTest` test `failedSaveKeepsDraftAndPublishesError`. `doubleSaveWritesOnce` zostaje, bo zapis wysyła efekt `CloseEditor`, a powtórzenie zamknęłoby dwa ekrany.

Przypadki brzegowe: zapis z poprzedniej sesji semestru nie publikuje komunikatu ani nie zamyka nowej; dodanie kierunku z osobnym kalendarzem wybiera inną operację repozytorium niż kierunek ze wspólnym kalendarzem.

Weryfikacja: `gradlew.bat test`; `SemesterScreenTest` i `StudyProgramsScreenTest` na emulatorze.

Kryterium zakończenia: `saveSemester` i dziewięć wymienionych akcji używa `launchUiOperation`; siedem testów usunięto, a dwa zmieniono; pozostałe testy przechodzą.

## 4. Przenieś zapisy ustawień i kreatora oraz zamknij I-64 (I-64)

Cel: akcje zapisu w `SettingsViewModel` i `SetupViewModel` korzystają z `launchUiOperation`, a dokumentacja opisuje wzorzec.

Pliki: `ui/settings/SettingsViewModel.kt`, `ui/setup/SetupViewModel.kt`, ich testy JVM, `docs/ARCHITECTURE.md` (sekcja 5), `docs/QUEUE.md`, `docs/LOG.md`.

1. `SettingsViewModel`: przenieś `selectSemester`, `confirmSemesterDeletion`, `selectTheme`, `exportJson`, `setGapThresholdMinutes`, prywatną `saveNotifications`, `prepareImport` i `confirmImport`. `prepareImport` ma `errorMessage = null` i `onError` ustawiający `importErrorMessage = "Nie udało się odczytać pliku."`; gałąź `ImportPreparation.Invalid` zostaje w bloku. `confirmImport` ma `onError` ustawiający `importErrorMessage` i komunikat błędu jak dziś. Funkcja publikująca wynik zapisu pliku eksportu zostaje bez zmian.
2. `SetupViewModel`: przenieś `saveConfiguration`, `saveAnotherProgram` i `activateAndAddClass` z `isCurrent = { token == sessionToken }`; wynik przypisz do `saveJob` tam, gdzie dziś przypisywany jest wynik `launch`.
3. Usuń testy z `SettingsViewModelTest`: `themeWriteFailurePublishesSingleErrorAndClearsFlag`, `gapThresholdWriteFailurePublishesItsOwnError`, `doubleThemeSelectionRunsOneWrite`, `selectSemesterFailureKeepsActiveAndPublishesError`, `doubleSemesterSelectionRunsOneOperation`, `deleteFailureKeepsDialogAndPublishesError`, `doubleConfirmRunsOneDeletion`, `cancelledDeletionPublishesNoErrorAndClearsFlag`, `exportReadFailurePublishesSingleError`. Zostają `exportWriteFailurePublishesSingleError` (osobna ścieżka zapisu pliku), `notificationWriteFailurePublishesError` (stan przełącznika po błędzie) i `importReadErrorSetsMessage`.
4. Usuń z `SetupViewModelTest` testy `failedSavePublishesSingleError`, `failedSaveKeepsDraftsAndStep` i `failedAnotherProgramKeepsTheFormAndReportsError`. Zostają `doubleClickRunsOneTransactionAndOneFeedback` (powtórzenie utworzyłoby drugi semestr), `successfulSavePublishesSingleMessage` (komunikat zależy od aktywacji) i `startDuringSaveResetsTheWizardWithoutFeedback` (sesja).
5. W `ARCHITECTURE.md`, sekcja 5, dopisz zasadę: akcja zapisu uruchamiana z interfejsu ma własną blokadę i flagę, a obsługę błędu, anulowania i czyszczenia flagi przekazuje do `launchUiOperation`; test ViewModelu sprawdza własną logikę akcji (walidację, wybór operacji repozytorium, stan i komunikat po sukcesie, stan po błędzie ustawiany przez `onError`) oraz podwójne wywołanie tylko wtedy, gdy powtórzenie utworzyłoby zduplikowane dane. Dodaj wpis do `LOG.md` z liczbą testów JVM przed krokiem 1 i po kroku 4. Zmień status I-64 w `QUEUE.md` na `gotowe` i przenieś wiersz do sekcji „Zakończone”, bo refaktor bez zmiany zachowania nie wymaga odbioru na urządzeniu po zielonym pełnym przebiegu testów.

Przypadki brzegowe: podwójne dotknięcie „Dalej” w kreatorze w trakcie zapisu; rozpoczęcie kreatora od nowa w trakcie zapisu (anulowanie `saveJob`); import za dużego pliku (komunikat przy polu, bez komunikatu na dole ekranu).

Weryfikacja: `gradlew.bat test`, pełny `gradlew.bat connectedDebugAndroidTest`, `py scripts/check_map.py`; na emulatorze: zmiana motywu, eksport i import pliku, utworzenie semestru w kreatorze.

Kryterium zakończenia: 27 akcji z kroków 2 do 4 używa `launchUiOperation`, a w sześciu ViewModelach bloki `catch (error: CancellationException)` zostały tylko w operacjach wymienionych jako poza zakresem; wszystkie testy przechodzą; opis pull requesta podaje liczbę testów JVM przed zmianą i po niej; dokumentacja zaktualizowana.
