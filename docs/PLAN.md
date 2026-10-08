# Plan najbliższych prac

Cel: hierarchia przycisków w kreatorze i systemowy gest wstecz w kreatorze (I-72). Makietę kreatora użytkownik zaakceptował 2026-10-08 (`ARCHITECTURE.md`, sekcja 4; `FEATURES.md`, sekcja kreatora). Polityka prywatności jest opublikowana i wpisana w Google Auth Platform (2026-10-08).

Przed pierwszym wydaniem z synchronizacją: sprawdzić logowanie Google na podpisanym APK release (`KNOWN_ISSUES.md`), przełączyć aplikację w Google Auth Platform na tryb produkcyjny (Audience, „Opublikuj aplikację”) i przygotować wpis w `release_notes.json` do akceptacji użytkownika.

Weryfikacja na AVD `Medium_Phone_A12` (decyzja użytkownika z 2026-10-08), bo na `Medium_Phone` brakuje miejsca na instalację APK debug.

## 1. Zamień akcje wyjścia kreatora na przyciski tekstowe (I-72)

Cel: „Wstecz” i „Wróć do ustawień” w kreatorze są przyciskami tekstowymi wyrównanymi do lewej pod pozostałymi akcjami kroku. Główna akcja zostaje `MakPrimaryAction`, równorzędne wybory zostają `MakSecondaryAction`. Pliki: `app/src/main/java/dev/retza/mak/ui/setup/SetupWizard.kt`, `app/src/androidTest/java/dev/retza/mak/ui/setup/SetupWizardTest.kt`.

Zmiany:

1. Przed zmianą zrób zrzuty kreatora w krokach „Utwórz semestr” (z „Wróć do ustawień”), „Dodaj kierunek”, „Dodaj zajęcia” (semestr aktywny i nieaktywny) oraz „Dodaj kolejny kierunek”, motyw jasny, szerokość 320 dp (`STACK.md`, sekcja 5, zrzuty). Zapisz je w `build/screenshots/i72-before/`.
2. `SemesterStep`: `MakSecondaryAction(text = "Wróć do ustawień", ...)` zamień na `MakTextAction(text = "Wróć do ustawień", onClick = onReturnToSettings)`.
3. `CourseStep`: `MakSecondaryAction(text = "Wstecz", onClick = onBack, enabled = !state.isSaving)` zamień na `MakTextAction` z tymi samymi argumentami.
4. `InactiveSemesterClassesStep` i `ClassesStep`: „Wstecz” zamień na `MakTextAction` z tymi samymi argumentami (`enabled = !isActivating` w pierwszym). „Dodaj kolejny kierunek”, „Zakończ” i „Przejdź do Dzisiaj” zostają `MakSecondaryAction`.
5. Nie dodawaj `Modifier.align` ani `fillMaxWidth`: `Column` wyrównuje do początku, a `MakTextAction` ma szerokość treści i wysokość co najmniej 48 dp.

Testy w `SetupWizardTest`:

- `exitActionIsTextButtonAtStartAt320Dp`: krok `Classes`; węzeł „Wstecz” z akcją kliknięcia ma lewą krawędź najwyżej 4 dp od lewej krawędzi „Dodaj zajęcia” i jest węższy niż ten przycisk; kliknięcie wywołuje `onBack` raz.
- `semesterStepOffersReturnToSettingsAsTextButtonAt320Dp`: krok `Semester` z `showReturnToSettings = true`; te same warunki dla „Wróć do ustawień” i `onReturnToSettings`.
- Rozszerz pomocniczą `showWizard` o parametry `onBack`, `onReturnToSettings` i `showReturnToSettings` z wartościami domyślnymi.

Przypadki brzegowe: duża czcionka (200%) przy 320 dp nie ucina etykiet; w trakcie zapisu („Zapisz kierunek”) i aktywacji przycisk tekstowy jest wyłączony jak dotąd; motyw ciemny ma czytelny kolor akcentu.

Weryfikacja: `gradlew.bat --offline :app:compileDebugAndroidTestKotlin`, potem `gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.setup.SetupWizardTest"`. Zrzuty po zmianie w tych samych stanach do `build/screenshots/i72-after/`.

Kryterium zakończenia: `SetupWizardTest` przechodzi na emulatorze; zrzuty po zmianie pokazują „Wstecz” i „Wróć do ustawień” jako tekst bez ramki po lewej, a pozostałe przyciski bez zmian; commit `feat: show wizard exit actions as text buttons (I-72)` razem z dokumentacją decyzji (`ARCHITECTURE.md`, `FEATURES.md`, `LOG.md`, `QUEUE.md`, `STACK.md`, ten plan).

## 2. Obsłuż systemowy gest wstecz w kreatorze (I-72)

Cel: gest wstecz robi to samo co widoczna akcja wyjścia kroku. Dziś `MakNavHostApp.kt` wyłącza `BackHandler` dla tras kreatora (`showBack` jest fałszywe dla `isSetupRoute`), a `SetupRoutes.kt` nie ma własnej obsługi, więc gest zdejmuje trasę kreatora i wraca do „Dzisiaj” z każdego kroku. Pliki: `SetupWizard.kt`, `SetupWizardTest.kt`.

Zmiany:

1. Najpierw dopisz testy z listy niżej i uruchom je na kodzie bez poprawki. Oczekiwany wynik: test kroku `Classes` nie przechodzi, bo gest nie wywołuje `onBack`. Wynik zapisz w opisie commitu.
2. W `SetupWizard` przed `MakScreenContent` dodaj `BackHandler` z `androidx.activity.compose`:
   - `val exitsWizard = state.step == SetupStep.Semester && !state.isAddingAnotherProgram`;
   - `enabled = state.status == ScreenStatus.Ready && (!exitsWizard || showReturnToSettings)`;
   - w obsłudze: gdy `state.isSaving || state.isActivating`, nic nie rób; gdy `exitsWizard`, wywołaj `onReturnToSettings()`; w przeciwnym razie `onBack()`.
3. Krok „Utwórz semestr” bez „Wróć do ustawień” zostawia gest nawigacji (`enabled = false`), który zamyka kreator.

Testy w `SetupWizardTest` (gest przez `Espresso.pressBack()`, jak w `OccurrenceDetailsScreenTest`):

- `backGestureGoesToPreviousStepFromClassesStep`: krok `Classes`, `onBack` wywołane raz.
- `backGestureClosesAnotherProgramForm`: krok `Course` z `isAddingAnotherProgram = true`, `onBack` wywołane raz.
- `backGestureReturnsToSettingsFromSemesterStep`: krok `Semester`, `showReturnToSettings = true`, `onReturnToSettings` wywołane raz, `onBack` zero razy.
- `backGestureIsIgnoredWhileSaving`: krok `Course`, `isSaving = true`, `onBack` zero razy i krok nadal widoczny.

Przypadki brzegowe: otwarty wybór daty albo listy w kroku zamyka się gestem jak dotąd, bez cofania kroku; stan ładowania i błędu (`status != Ready`) nie przechwytuje gestu; gest w kroku `Classes` po zapisie semestru wraca do kroku `Course` tak jak przycisk „Wstecz”.

Weryfikacja: ta sama klasa testów na emulatorze; ręcznie na emulatorze: kreator z ustawień, krok 3, gest wstecz wraca do kroku 2, kolejny do kroku 1, kolejny do ustawień; kreator z pustego „Dzisiaj”, krok 1, gest zamyka kreator.

Kryterium zakończenia: cztery nowe testy przechodzą, testy z kroku 1 nadal przechodzą, ręczny przebieg zgadza się z opisem; commit `fix: step back through the wizard with the system back gesture (I-72)`.

## 3. Przygotuj makietę dialogów do akceptacji (I-72)

Cel: druga część I-72, czyli „Później” i „Anuluj” w dialogach `MakDialog` jako przyciski tekstowe. Bez akceptacji makiety nie zmieniaj kodu. Dotyczy dialogów w `SyncScreen.kt` (wybór wersji, wyłączenie synchronizacji), `ScheduleScreen.kt` (korekta tygodnia, rząd „Anuluj” i „Zapisz”), `SettingsScreen.kt` („Zamknij” po błędzie importu) i `OccurrenceDetailsScreen.kt` (edycja terminu). `MakConfirmDeletionDialog` już używa przycisków tekstowych.

Kryterium zakończenia: artefakt z porównaniem przed i po dla każdego dialogu i decyzja użytkownika zapisana w `LOG.md`; implementację zaplanuj wtedy jako kolejny krok.
