# Plan najbliższych prac

Cel: I-84 (porządki po audycie kodu z 2026-10-09) na gałęzi `task/I-79-hide-study-programs` (decyzja użytkownika z 2026-10-10), w kolejności kroków poniżej. Raport: `docs/audit_09102026.md`; oznaczenia w nawiasach (I1, E1 i podobne) to punkty raportu. Decyzje użytkownika z 2026-10-10 są w `LOG.md`. Porządki trafią do tego samego pull requesta co I-79, bo zmieniają te same pliki.

Wspólna zasada: kroki nie zmieniają zachowania ani wyglądu, poza blokadą podwójnego dotknięcia w kroku 3. Ścieżki plików są względne wobec `app/src/main/java/dev/retza/mak/`, a testów wobec `app/src/test/java/dev/retza/mak/` albo `app/src/androidTest/java/dev/retza/mak/`. Testy JVM uruchamiaj filtrowane, na przykład `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.today.TodayViewModelTest"`. Kolejne zadania z audytu (I-85 do I-90) są w `QUEUE.md`; ich plan powstanie po I-84.

## 1. Usuń martwy kod i ścieżki używane tylko w testach (I-84)

Stan: zrobione. Usunięte też zapytania DAO `observeForSemester` notatek i zmian terminów, nieużywane po usunięciu metod repozytorium. Testy eksportu budują snapshot przez `exportSnapshotOf` w `BackupDataFixtures.kt`, który przechodzi przez produkcyjne `ExportSnapshot.from(BackupData)`. Test `lastOpenEditWinsWhenDataArrivesLate` usunięty, bo sprawdzał anulowanie przez `openJob`; anulowanie zapewnia `LaunchedEffect` trasy. `openEditIfNeeded` w `StudyProgramsViewModel` jest używane w produkcji i zostaje. Filtrowane testy JVM (`ui`, `export`, `update`, `data`, `ScheduleResolverTest`) przechodzą; testy Android kompilują się, nie były uruchamiane.

Cel: kod, którego produkcja nie woła, znika, a testy sprawdzają ścieżki produkcyjne (I1 do I4).

Pliki: `ui/components/PresentationModels.kt`, `ui/components/MakComponents.kt`, `ui/today/TodayScreen.kt`, `ui/today/TodayViewModel.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/edit/ClassEditScreen.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/semester/SemesterScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsPreferences.kt`, `ui/setup/SetupWizard.kt`, `ui/setup/SetupViewModel.kt`, `ui/*Routes.kt`, `data/repository/ScheduleRepository.kt`, `data/repository/SemesterRepository.kt`, `data/database/Daos.kt`, `data/database/Relations.kt`, `domain/ScheduleResolver.kt`, `widget/WidgetLayoutPolicy.kt`, `export/ExportSnapshot.kt`, `update/UpdateViewModel.kt` i testy.

Zmiany:

1. `ScreenStatus` (I1): usuń całą klasę z `PresentationModels.kt` i `MakStateMessage` z `MakComponents.kt`. Gałąź ładowania przenieś do nowego `MakLoadingMessage(modifier)` z tym samym wyglądem (`CircularProgressIndicator` i „Ładowanie”). `TodayUiState.status` zastąp polem `isLoading: Boolean = false`; `TodayViewModel` zaczyna od `isLoading = true` (obecne `ScreenStatus.Loading` w wierszu około 100). `TodayClasses` pokazuje `MakLoadingMessage` przy `isLoading`, a pusty stan przez `MakEmptyState(state.emptyMessage)`. Usuń pola `status` z `ClassEditUiState`, `ScheduleUiState`, `SemesterUiState`, `SettingsUiState` i `SetupWizardUiState`, gałęzie `when (state.status)` zamień na treść gałęzi `Ready`, usuń parametry `onRetry` ekranów `TodayScreen`, `ScheduleScreen`, `ClassEditScreen`, `SemesterScreen`, `SettingsScreen` i `SetupWizard` oraz `onRetry = {}` w trasach. `UpdateScreen.DownloadError` ma własne `onRetry` i zostaje.
2. Repozytoria (I2): usuń z interfejsu i implementacji `ScheduleRepository` metody `observeClasses`, `observeOccurrenceNotes`, `observeOccurrenceChanges`, `observeOccurrenceNotesForClass` i `observeOccurrenceChangesForClass`, a z `SemesterRepository` metody `updateSemester`, `clearActiveSemester`, `deleteSemester` i `deleteCalendarIfUnused`. Usuń z `Daos.kt` zapytania używane tylko przez nie (dwa `observeForClass`) oraz zapytania bez wywołań (I3): `AcademicCalendarDao.getForSemester`, `ClassDao.observeWithDetailsForSemester`, `ClassDao.observeWithDetails`, `WeekOverrideDao.observeForCalendar`, a potem relację `ClassWithDetails` z `Relations.kt`. Usuń te metody z atrap (`FakeRepository.kt`, `FakeSemesterRepository.kt` i innych, które wskaże kompilator). Testy, które sprawdzały tylko usunięte metody, usuń; testy, które ich używały do przygotowania danych, przepnij na `saveSemester`, `deleteSemesterAndSelectFallback` albo bezpośredni stan atrapy.
3. Kod bez wywołań (I3): usuń `MakReadOnlyField`, `resolveOccurrences` w `ScheduleResolver.kt`, `ThemeMode.toId()` i `widgetNameCharacterLimit`.
4. Ścieżki tylko dla testów (I4): w `ClassEditViewModel` usuń `openNew`, `openEdit`, `openEditIfNeeded` i pole `openJob`; zostają `openNewFromFreshPlan` i `openEditFromFreshPlan`. Testy `ClassEditViewModelTest` przepnij na te funkcje w `runTest`. Test odtworzenia szkicu (obecnie `restored.openEditIfNeeded`) sprawdza, że ViewModel utworzony z tym samym `SavedStateHandle` ma szkic bez wywołania otwarcia, bo trasa nie otwiera edytora drugi raz (`opened` w `ClassOccurrenceRoutes.kt`). W `ExportSnapshot.kt` usuń przeciążenia `from(SemesterWithData)` i przepnij testy na przeciążenie `BackupData`. W `SettingsViewModel` usuń `themeMode`; testy czytają `loadedThemeMode`. W `UpdateViewModel` parametr `preferences: UpdatePreferences?` zmień na niepusty i usuń gałęzie `if (preferences != null)`; `UpdateViewModelTest` przekazuje atrapę `UpdatePreferences`.

Testy: brak nowych reguł. Poprawione: `TodayViewModelTest` (stan ładowania przez `isLoading`), `ClassEditViewModelTest`, `SettingsViewModelTest`, `UpdateViewModelTest`, testy eksportu; testy Compose ekranów bez `onRetry`.

Przypadki brzegowe: `TodayScreen` przy `isLoading` nie pokazuje pustego stanu ani akcji „Skonfiguruj plan”; `ClassEditViewModel` nie potrzebuje `openJob`, bo funkcje otwarcia są `suspend` i działają w `LaunchedEffect` trasy, który anuluje je przy zamknięciu ekranu.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.export.*" --tests "dev.retza.mak.update.*"`; `gradlew.bat --offline :app:compileDebugAndroidTestKotlin`; `rg "ScreenStatus|MakStateMessage|onRetry = \{\}|openEditIfNeeded|ClassWithDetails" app/src` zwraca tylko `DownloadError` w `UpdateScreen.kt`.

Kryterium zakończenia: kod i testy kompilują się, filtrowane testy JVM przechodzą, polecenie `rg` z weryfikacji nie znajduje usuniętych nazw. Commit: `chore: remove dead code found by the code audit (I-84)`, razem z `LOG.md`, `QUEUE.md` i tym planem.

## 2. Zastąp stałe pola stanu i kopie funkcji (I-84)

Stan: zrobione. Różnice wobec opisu: lista kierunków kreatora używa istniejącego `SetupProgramOptionUi` zamiast nowego typu, a `semesterProgramNames` jest właściwością obliczaną; `canSaveOccurrenceEdit` zostaje w `derive()`, bo zależy od prywatnej logiki edycji terminu w `OccurrenceViewModel`, a obliczane są tylko flagi zapisu notatek; `includeTeacher` zostaje polem polityki widgetu ustawianym z `wide`; ręczne sprawdzenie aktualizacji nadal zawsze pokazuje baner (`withCheckResult`), a automatyczne i preferencje liczą go przez `showsBanner`. Trasy budują identyfikator terminu przez `NavBackStackEntry.occurrenceArgs()` i `OccurrenceViewModel.shownRouteId()`. Filtrowane testy JVM (`ui`, `widget`, `update`: 293 testy) przechodzą; testy Android kompilują się, nie były uruchamiane.

Cel: stan ekranów nie przechowuje wartości stałych ani wynikających z innych pól, a widget i formularz używają wspólnych funkcji z `ui` (I5, G2, G3, H1, H3, B4 dla `AppViewModel`).

Pliki: `ui/AppViewModel.kt`, `ui/MakNavHostApp.kt`, `ui/setup/SetupViewModel.kt`, `ui/setup/SetupWizard.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceDetailsScreen.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/ClassOccurrenceRoutes.kt`, `update/UpdateViewModel.kt`, `widget/WidgetLayoutPolicy.kt`, `widget/MakTodayWidget.kt`, `widget/WidgetPresenter.kt` i testy.

Zmiany:

1. `AppViewModel` (I5, B4): `AppUiState.setupResume` zastąp `canResumeSetup: Boolean`. Zamiast `observeActivePlanData` zbieraj `semesterRepository.observeSemesterPrograms(semester.id).map { it.isEmpty() }`: `requiresSetup` jest prawdą bez aktywnego semestru albo bez przypisań, `canResumeSetup` przy aktywnym semestrze bez przypisań. Usuń `SetupCheck`. `MakNavHostApp` woła `openSetup(navController, resumeExisting = state.canResumeSetup)`. Efekt uboczny `setActiveSemester` w `init` zostaje do I-89 (E4).
2. `SetupViewModel` (I5): usuń `canSkipClasses` i parametr `canSkip` w `SetupWizard` (zawsze `true`). Trzy równoległe listy nazw, identyfikatorów i kolorów kierunków (wiersze 89-90 i 138 według raportu) zastąp jedną listą rekordów `SetupProgramDraftUi(name, id, color)`.
3. `OccurrenceViewModel` i `OccurrenceDetailsUiState` (I5): usuń `noteDate` (zawsze równe `originalDate`) i warunek, który go porównuje; `canChangeOccurrence` i `canMoveOccurrence` połącz w jedno `canChangeOccurrence`; usuń nieustawiane `statusLabel` i `title`; `canSaveSharedNote`, `canSaveOccurrenceNote` i trzecią flagę `canSave*` zamień na właściwości obliczane w `OccurrenceDetailsUiState` i usuń ich liczenie w `derive()`.
4. `ClassEditViewModel` (I5, H1, H3): kolektor w `init` woła `withActiveOptions` zamiast powtarzać jego ciało; `typeOptions`, `dayOptions` i `recurrenceOptions` przenieś ze stanu do stałych czytanych przez `ClassEditScreen`; `classEditDayNames` zastąp `dayNames` z `ui/PlanMapping.kt` (te same etykiety, ten sam porządek); identyfikator terminu parsuj przez `OccurrenceArgs.parse` zamiast `substringBefore(':')`. `title` zostaje w stanie, bo trafia do `SavedStateHandle` (zmiana w I-86).
5. Identyfikator terminu (H1): w `ClassOccurrenceRoutes.kt` i `ScheduleViewModel.kt` składaj go przez `OccurrenceArgs(classId, date).toRouteId()` zamiast `"$classId:$date"`. `recurrenceLabelForUi` w `ClassOccurrenceRoutes.kt` zastąp `classEditRecurrenceLabel`.
6. `UpdateViewModel` (I5): usuń blok `try { checker.check() } catch (c: CancellationException) { throw c }`; `showUpdateBanner` licz w jednej prywatnej funkcji.
7. Stałe teksty w stanie (I5): `SettingsUiState.notificationsDetails` i `ScheduleUiState.emptyMessage` zastąp stałymi w plikach ekranów. `ImportPreviewUi` zastąp bezpośrednio `ImportSummary`, jeśli ekran czyta te same pola; jeśli potrzebuje innych typów, zostaw i opisz powód w commicie.
8. Widget (G2, G3): `widgetShouldShowConflict` zwraca `item.conflictLabel != null` bez `when`; `headerSpacing` jest stałą 6.dp poza polityką; `includeTeacher` liczone z `wide`; gałęzie `Large` i `ExtraLarge` w jednym `WidgetHeightMode.Large, WidgetHeightMode.ExtraLarge ->`. `parseWidgetColor` zastąp `parseHexColor`, `widgetCountLabel` zastąp `classCountLabel`, a `widgetDateFormatter` zastąp `todayTitleFormatter`, jeśli wzorzec i wielkość liter są te same; inaczej zostaw i opisz różnicę w commicie.

Testy: `AppViewModelTest` (bez semestru: konfiguracja bez wznowienia; semestr bez przypisań: wznowienie; z przypisaniem: brak konfiguracji); poprawione `SetupViewModelTest`, `OccurrenceDetailsModelsTest`, `OccurrenceViewModelTest`, `ClassEditViewModelTest`, `UpdateViewModelTest`, `WidgetPresenterTest`, `MakTodayWidgetCompositionTest`.

Przypadki brzegowe: formularz zajęć odczytuje dzień zapisanego szkicu z `SavedStateHandle` po etykiecie, więc etykiety `dayNames` muszą być identyczne z dotychczasowymi; widget z nieprawidłowym kolorem kierunku nadal rysuje kolor zastępczy.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*" --tests "dev.retza.mak.update.*"`; `gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.

Kryterium zakończenia: testy przechodzą, `rg "setupResume|canSkipClasses|noteDate|classEditDayNames|parseWidgetColor|widgetCountLabel" app/src` nic nie znajduje. Commit: `chore: derive constant state and reuse ui helpers (I-84)`.

## 3. Przenieś akcje zapisu na launchUiOperation i usuń koinInject z TrackPlanEditing (I-84)

Cel: każda akcja zapisu z interfejsu ma blokadę podwójnego wywołania i flagę trwania przez `launchUiOperation` (E1, `ARCHITECTURE.md`, sekcja 5), zależność `PlanEditTracker` przychodzi jawnie z granicy hosta (D3, `STACK.md`), a dialogi rysuje `MakDialog` (H7, `LOG.md`, I-72).

Pliki: `ui/schedule/ScheduleViewModel.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceDetailsScreen.kt`, `ui/semester/SemesterViewModel.kt`, `ui/PlanEditing.kt`, `MainActivity.kt`, `ui/MakNavHostApp.kt`, `ui/*Routes.kt`, `ui/edit/ClassEditScreen.kt`, `ui/semester/SemesterScreen.kt` i testy.

Zmiany:

1. `ScheduleViewModel`: dodaj prywatne `weekOverrideSaving = MutableStateFlow(false)` i pole `ScheduleUiState.isSavingWeekOverride`, dołączone w `combine` stanu. `saveVisibleWeekOverride` i `clearVisibleWeekOverride` wracają od razu, gdy flaga jest prawdą, ustawiają ją i wołają `viewModelScope.launchUiOperation(feedbackSink, errorMessage = "Nie udało się zapisać korekty tygodnia." albo "Nie udało się usunąć korekty tygodnia.", onFinish = { weekOverrideSaving.value = false })`. Usuń `runReportingFailure`. `WeekCorrectionDialog` dostaje `isSaving` i wyłącza „Zapisz”, „Usuń korektę” i „Anuluj” w czasie zapisu.
2. `OccurrenceViewModel.deleteSelectedClass`: pole `OccurrenceDetailsUiState.isDeletingClass`; funkcja wraca, gdy jest prawdą, i używa `launchUiOperation` z `errorMessage = "Nie udało się usunąć zajęć."`, `onError = { update { it.copy(showDeleteConfirmation = false) } }` i `onFinish` czyszczącym flagę; `CloseDetails` wysyła po sukcesie wewnątrz bloku. `OccurrenceDetailsScreen` przekazuje `isDeleting = state.isDeletingClass` do `ConfirmDeletionDialog` zamiast `false`.
3. `SemesterViewModel` (wiersze około 499-520 według raportu): jeśli akcja ma własny `try/catch` zamiast `launchUiOperation`, przenieś ją tak samo; jeśli kształt różni się celowo (na przykład kilka komunikatów), zostaw i opisz w commicie.
4. `TrackPlanEditing`: parametr `tracker: PlanEditTracker` bez wartości domyślnej, usuń import `koinInject`. `MainActivity` pobiera `private val planEditTracker: PlanEditTracker by inject()` i przekazuje go do `MakApp(planEditTracker = ...)`. `MakApp` przekazuje go do `classEditRoute`, `occurrenceDetailsRoute`, `occurrenceTopBarActions`, `semesterRoutes`, `settingsRoute`, `setupRoute` i `studyProgramRoutes`, a te do każdego `TrackPlanEditing(tracker = planEditTracker, ...)`. `PlanEditingTest` tworzy `PlanEditTracker` jawnie.
5. Dialogi (H7): `AlertDialog` ukrytych danych w `ClassEditScreen` i `ReconnectCalendarDialog` w `SemesterScreen` zamień na `MakDialog` z `dismissAction = MakDialogAction("Anuluj", ..., enabled = !isSaving)` i `confirmAction = MakDialogAction("Zapisz" albo "Połącz", ..., enabled = !isSaving)`; treść i błąd w `content`.

Testy: `ScheduleViewModelTest` (drugie wywołanie zapisu korekty w trakcie pierwszego nie wysyła drugiego zapisu; błąd publikuje komunikat i czyści flagę); `OccurrenceViewModelTest` (podwójne `deleteSelectedClass` daje jedno usunięcie i jeden `CloseDetails`; błąd zamyka potwierdzenie). Poprawione `PlanEditingTest`, `ClassEditScreenTest` i `SemesterScreenTest`, jeśli szukają przycisków dialogu po innym drzewie.

Przypadki brzegowe: zamknięcie dialogu korekty w trakcie zapisu nie zostawia flagi ustawionej; anulowanie korutyny przy zamknięciu ekranu nie publikuje błędu.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.schedule.ScheduleViewModelTest" --tests "dev.retza.mak.ui.occurrence.OccurrenceViewModelTest" --tests "dev.retza.mak.ui.semester.SemesterViewModelTest"`; na emulatorze (2048 MiB) `PlanEditingTest`, `ClassEditScreenTest`, `SemesterScreenTest` i `KoinGraphTest`; zrzut obu dialogów przy 320 dp.

Kryterium zakończenia: testy przechodzą, `rg "koinInject|runReportingFailure" app/src/main` nic nie znajduje, a `rg "AlertDialog\(" app/src/main` znajduje tylko `MakComponents.kt`. Commit: `fix: guard week override and class deletion writes (I-84)`.

## 4. Ogranicz zbędne odświeżanie powiadomień i preferencji (I-84)

Cel: zmiana motywu, zapis czasu sprawdzenia aktualizacji albo ukrycie banera nie przelicza planu, a kolektor powiadomień nie trzyma subskrypcji planu przy wyłączonych powiadomieniach (B3, B5).

Pliki: `ui/settings/DataStoreSettingsPreferences.kt`, `MakApplication.kt`, `notifications/CollisionAlarmScheduler.kt` i testy.

Zmiany:

1. `DataStoreSettingsPreferences`: dodaj `.distinctUntilChanged()` do `theme`, `collisionNotifications` i `gapThresholdMinutes`, tak jak ma `planDisplay`.
2. `CollisionAlarmScheduler`: wydziel `refresh(settings: CollisionNotificationPreferences, inputs: ActivePlanInputs?)` pod tym samym `mutex`; obecne `refresh()` czyta obie wartości przez `first()` i woła nową funkcję (odbiornik restartu i alarm podtrzymania dalej wołają `refresh()`). Zapis `store(plannedIds)` pomija, gdy `storedIds() == plannedIds`.
3. `MakApplication`: kolektor powiadomień ma postać `preferences.collisionNotifications.flatMapLatest { settings -> if (!settings.enabled) flowOf(settings to null) else activePlanSource.observeActive().map { settings to it } }`, z dotychczasowym `debounce`, i woła `scheduler.refresh(settings, inputs)`. Wyłączone powiadomienia nadal wywołują `cancelAll` przez `refresh` z `enabled == false`.

Testy: `CollisionAlarmSchedulerTest` na emulatorze (wyłączone powiadomienia anulują alarmy; dwa wywołania z tymi samymi danymi nie zmieniają zapisanych identyfikatorów); `SettingsPreferencesTest` na emulatorze (zapis innego klucza nie emituje ponownie `theme`).

Przypadki brzegowe: włączenie powiadomień po starcie z wyłączonymi zaczyna subskrypcję planu i planuje alarmy bez ponownego uruchomienia aplikacji.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.CollisionNotificationPlannerTest"`; na emulatorze `CollisionAlarmSchedulerTest`, `SettingsPreferencesTest` i `KoinGraphTest`; ręcznie: włączenie powiadomień w ustawieniach planuje alarm (`adb shell dumpsys alarm | findstr dev.retza.mak`).

Kryterium zakończenia: testy przechodzą, a ręczne sprawdzenie pokazuje alarm po włączeniu i brak alarmów po wyłączeniu. Commit: `fix: skip plan refreshes when nothing changed (I-84)`. Potem I-84 przechodzi w `QUEUE.md` na `gotowe` (zmiany bez wpływu na wygląd poza dialogami sprawdzonymi zrzutami), a `PLAN.md` dostaje kroki I-85.
