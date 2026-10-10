# Plan najbliższych prac

Cel: wykonać I-88 (synchronizacja i odświeżanie po zmianie planu, audyt F1, F2, F3, G1 i K3). Raport audytu: `docs/audit_09102026.md`. Decyzje użytkownika z 2026-10-10 są w `LOG.md`: próba „po otwarciu aplikacji” startuje z `MainActivity`, po zamknięciu formularza tylko wtedy, gdy poprzednia próba czekała na formularz, a własny zapis pobranego planu rozpoznaje skrót planu zamiast okna 2 sekund. Zmniejszenie liczby rozmiarów widgetu (druga część G1) wymaga osobnego wariantu i nie wchodzi do tego planu. Po I-88 kolejne plany obejmą I-89 i I-90 (polecenie użytkownika z 2026-10-10: wykonać wszystkie poprawki z audytu).

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

## 1. Zapisuj przyczynę problemu synchronizacji jako wartość, a tekst twórz w interfejsie (I-88)

Stan: zrobione. `issueFix` dla nowych wartości daje `null`, jak dotąd `FAILED`. Dodany test `otherDriveErrorIsFailedWithoutMessage`.

Cel: każda przyczyna problemu ma własną wartość `SyncIssue`, a polski tekst powstaje tylko w `SyncViewModel` (F2). Zmiana treści komunikatu nie wymaga edycji w `sync`, a stare zapisane komunikaty nie przetrwają aktualizacji. Teksty widoczne dla użytkownika się nie zmieniają.

Pliki: `sync/SyncStateStore.kt`, `sync/SyncCoordinator.kt`, `sync/SyncPlanFile.kt` (tylko jeśli buduje zdanie komunikatu), `ui/settings/SyncViewModel.kt`; testy `sync/SyncCoordinatorTest.kt`, `sync/FileSyncStateStoreTest.kt`, Android `ui/settings/SyncViewModelTest.kt`; dokument `docs/SYNC.md` (tylko jeśli wymienia `issueMessage`).

Zmiany:

1. `SyncIssue`: dodaj wartości `STORAGE_FULL`, `ACCESS_DENIED`, `ACCOUNT_MISMATCH`. `FAILED` zostaje dla błędu ogólnego. Kolejność istniejących wartości bez zmian.
2. `SyncState.issueMessage` zmienia znaczenie na szczegół błędu pliku: ustawia go tylko `INVALID_REMOTE_PLAN` (tekst błędu walidacji z `ExportImporter`). Pole zostaje pod tą samą nazwą w formacie zapisu, aby stary plik stanu się wczytał; dodaj nad nim komentarz o nowym znaczeniu.
3. `SyncCoordinator.run`: `AccountMismatchException` daje `issue(SyncIssue.ACCOUNT_MISMATCH, message = null)`; `isStorageFull()` daje `STORAGE_FULL`; `403` daje `ACCESS_DENIED`; pozostałe przypadki `FAILED` z `message = null`. `INVALID_REMOTE_PLAN` przekazuje szczegół błędu (punkt 4). `LOCAL_PLAN_TOO_LARGE` dostaje `message = null`. Usuń stałe `GENERIC_FAILURE`, `LOCAL_PLAN_TOO_LARGE_MESSAGE`, `ACCOUNT_MISMATCH` i funkcję `failed(message)`, jeśli nic ich już nie używa.
4. `SyncPlanFile.decode` buduje dziś zdania („Plik planu na Dysku jest za duży.”, „Nie udało się odczytać pliku planu z Dysku.”, „Plik planu na Dysku jest niepoprawny: ...”). Zostaw te zdania bez zmian w tym kroku, bo to opisy błędu pliku, które `SyncViewModel` pokazuje przy `INVALID_REMOTE_PLAN`; komentarz przy `issueMessage` mówi, że to jedyny tekst z `sync`.
5. `SyncViewModel`, `SyncState.issueText()`:
   - `INVALID_REMOTE_PLAN -> issueMessage ?: "Plik planu na Dysku jest niepoprawny."` (bez zmian);
   - `LOCAL_PLAN_TOO_LARGE -> "Lokalny plan przekracza limit 8 MiB i nie może zostać wysłany."`;
   - `STORAGE_FULL -> "Na Dysku Google brakuje miejsca. Zwolnij miejsce i synchronizuj ponownie."`;
   - `ACCESS_DENIED -> "Dysk Google odmówił dostępu. Jeśli to się powtarza, połącz konto ponownie."`;
   - `ACCOUNT_MISMATCH -> "Telefon ma inne konto Google niż połączone. Wyłącz synchronizację i połącz konto ponownie."`;
   - `FAILED -> "Nie udało się zsynchronizować planu."` (bez `issueMessage`, aby stary zapisany tekst nie wracał).
   - `issueFix()`: nowe wartości dostają tę samą poprawkę co dziś `FAILED` (sprawdź obecne gałęzie).
   - W `Exception.toMessage()` komunikat dla `AccountMismatchException` ujednolić z tekstem `ACCOUNT_MISMATCH` powyżej (audyt: dwa różne brzmienia).

Testy:

- `SyncCoordinatorTest`: testy, które sprawdzały `issueMessage` dla braku miejsca, odmowy dostępu, innego konta i błędu ogólnego, sprawdzają `issue` (`STORAGE_FULL`, `ACCESS_DENIED`, `ACCOUNT_MISMATCH`, `FAILED`) i `issueMessage == null`.
- `FileSyncStateStoreTest`: nowy `storedFailedMessageFromOlderVersionStillLoads`: plik stanu z `"issue":"FAILED","issueMessage":"stary tekst"` wczytuje się bez błędu.
- `SyncViewModelTest` (Android): nowy `issueTextsComeFromTheViewModel`: stan z każdą z nowych wartości daje tekst z punktu 5; stan `FAILED` z zapisanym `issueMessage` daje tekst ogólny.

Przypadki brzegowe: plik stanu z poprzedniej wersji z `FAILED` i starym tekstem pokazuje tekst ogólny; nowe wartości enum zna tylko nowa wersja, ale plik stanu jest lokalny i nie trafia na inne telefony.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.settings.SyncViewModelTest` i `dev.retza.mak.ui.settings.SyncScreenTest`.

Kryterium zakończenia: testy przechodzą; `rg "Dysk Google odmówił|brakuje miejsca|inne konto" app/src/main/java/dev/retza/mak/sync` nic nie znajduje. Commit: `chore: store sync issue causes as values (I-88)`. W tym commicie w `QUEUE.md` zmień status I-88 na `w toku`.

## 2. Obserwuj zmiany planu jednym strumieniem i rozpoznawaj własny zapis po skrócie (I-88)

Cel: widget i synchronizacja reagują na jeden strumień zmian planu na poziomie aplikacji (K3). Odświeżanie widgetu łączy żądania z opóźnieniem 1 s (G1). Czas zmiany planu na telefonie zapisuje się raz na serię zapisów, a własny zapis pobranego planu nie liczy się jako zmiana, bo skrót planu równa się zapisanemu po synchronizacji (F1, zamiast okna 2 sekund). Powiadomienia zostają na współdzielonym strumieniu `ActivePlanSource`, bo potrzebują treści planu.

Pliki: nowy `data/PlanChanges.kt`, `di/AppModule.kt`, `sync/SyncCoordinator.kt`, `sync/SyncWork.kt` (`SyncRoomChangeObserver`), `widget/MakWidgetRefresh.kt`, `MakApplication.kt`; testy `sync/SyncCoordinatorTest.kt`, nowy `widget/MakWidgetRefreshTest.kt` (jeśli taki plik już jest, dopisz do niego); dokumenty `docs/ARCHITECTURE.md`, `docs/SYNC.md`.

Zmiany:

1. Nowy `data/PlanChanges.kt`:
   ```kotlin
   /** One app-wide stream of plan writes in Room, shared by the widget refresh and synchronization. */
   class PlanChanges(changes: Flow<Unit>, scope: CoroutineScope) {
       val changes: Flow<Unit> = changes.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)
   }
   ```
   W `AppModule`: `@Single fun providePlanChanges(database: AppDatabase): PlanChanges = PlanChanges(database.invalidationTracker.createFlow(*PLAN_TABLES, emitInitialState = false).map { }, CoroutineScope(SupervisorJob() + Dispatchers.Default))`.
2. `registerMakWidgetRefresh`: zamiast `database` przyjmuje `planChanges: Flow<Unit>`; zamiast obserwatora `InvalidationTracker` i osobnego kolektora ustawień jeden kolektor:
   ```kotlin
   scope.launch {
       // The first settings value is the stored one, already shown by the widget.
       merge(planChanges, planDisplay.drop(1).map { })
           .debounce(WIDGET_REFRESH_DEBOUNCE_MILLIS)
           .collect { requester.request() }
   }
   ```
   z `private const val WIDGET_REFRESH_DEBOUNCE_MILLIS = 1_000L` i `@OptIn(FlowPreview::class)` przy funkcji. `MakApplication` przekazuje `planChanges.changes` (pole `planChanges: PlanChanges by inject()`).
3. `SyncCoordinator`:
   - Usuń `lastReplacementMillis`, jego ustawianie po zastąpieniu planu i `REPLACEMENT_ECHO_MILLIS`.
   - `recordLocalChange()` zmień na `suspend fun recordLocalChange(changedAtMillis: Long) = serialized { ... }`: bez konta wraca; liczy `SyncPlanFile.fingerprint(gateway.snapshot())`; gdy równa się `current.value.localFingerprint`, wraca bez zapisu (własny zapis pobranego planu albo zmiana cofnięta do stanu z synchronizacji); w przeciwnym razie `save(current.value.copy(localChangedAtMillis = changedAtMillis))`.
4. `SyncRoomChangeObserver`: konstruktor przyjmuje `planChanges: PlanChanges` i `clock: Clock` zamiast `database`. Dwa kolektory zmian zastąp jednym:
   ```kotlin
   scope.launch {
       planChanges.changes
           .map { clock.millis() }
           .debounce(1_000L)
           .collect { changedAt ->
               try {
                   coordinator.recordLocalChange(changedAt)
               } catch (error: CancellationException) {
                   throw error
               } catch (_: Exception) {
                   // Only a label in the version choice depends on it.
               }
               scheduler.enqueueImmediate()
           }
   }
   ```
   `debounce` zostawia czas ostatniego zapisu z serii, a plik stanu zapisuje się raz na serię. Kolektor `editTracker.editing` zostaje bez zmian w tym kroku (zmienia go krok 3).
5. Dokumenty: w `docs/ARCHITECTURE.md`, sekcja 7, i w `docs/SYNC.md` dopisz, że widget i synchronizacja obserwują jeden strumień `PlanChanges` z opóźnieniem 1 s, a własny zapis pobranego planu rozpoznaje skrót planu. Usuń wzmianki o oknie 2 sekund, jeśli są (`rg -n "2 s|2 sekund" docs`).

Testy:

- `SyncCoordinatorTest.localChangesAreRecordedButNotTheEchoOfADownload`: wywołania `recordLocalChange()` zmień na `recordLocalChange(<czas zegara testu>)`; oczekiwania bez zmian.
- `SyncCoordinatorTest`, nowy `changeBackToTheSyncedPlanIsNotRecorded`: po synchronizacji zmień plan na telefonie i przywróć go do stanu po synchronizacji; `recordLocalChange(...)` zostawia `localChangedAtMillis == null`.
- `SyncCoordinatorTest`, nowy `lateEchoOfADownloadIsNotRecorded`: po pobraniu planu `recordLocalChange` z czasem 10 s później nie zapisuje zmiany (dawniej okno 2 s by ją zapisało).
- `MakWidgetRefreshTest` (JVM): trzy emisje `planChanges` w odstępach 100 ms dają jedno `request()` po 1 s czasu wirtualnego; zmiana `planDisplay` po pierwszej wartości daje jedno `request()`.

Przypadki brzegowe: zapis formularza (kilka tabel w jednej transakcji) daje jedno odświeżenie widgetu i jeden zapis czasu zmiany; pobranie planu z Dysku nie ustawia „zmieniono na telefonie”, nawet gdy Room zgłosi zmianę po kilku sekundach; widget dalej odświeża się po zmianie minimalnej przerwy albo ukrytych kierunków.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.widget.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest` i `dev.retza.mak.sync.RoomPlanSyncGatewayTest`.
4. Ręcznie: widget na ekranie głównym zmienia się po odwołaniu dzisiejszego terminu w aplikacji (do 2 s) i po zmianie minimalnej przerwy w ustawieniach.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "REPLACEMENT_ECHO|lastReplacementMillis" app/src` i `rg "invalidationTracker" app/src/main/java/dev/retza/mak/sync app/src/main/java/dev/retza/mak/widget` nic nie znajdują. Commit: `chore: share plan changes and recognize own sync writes by fingerprint (I-88)`.

## 3. Zawęź wyzwalacze synchronizacji (I-88)

Cel: próba „po otwarciu aplikacji” startuje z `MainActivity`, a nie przy każdym starcie procesu (widget, alarmy, `SyncWorker`), a zamknięcie formularza uruchamia próbę tylko wtedy, gdy poprzednia próba czekała na formularz (F3, decyzja użytkownika z 2026-10-10). Zmiana po stronie telefonu dalej uruchamia próbę przez strumień z kroku 2.

Pliki: `MakApplication.kt`, `MainActivity.kt`, `sync/SyncCoordinator.kt`, `sync/SyncWork.kt`; testy `sync/SyncCoordinatorTest.kt`; dokumenty `docs/ARCHITECTURE.md`, `docs/SYNC.md`, `docs/QUEUE.md`.

Zmiany:

1. `MakApplication.onCreate`: usuń `syncWorkScheduler.scheduleForAppOpen()` i pole `syncWorkScheduler`, jeśli nic poza tym go nie używa.
2. `MainActivity`: `private val syncWorkScheduler: SyncWorkScheduler by inject()`; w `onCreate` po `super.onCreate`: `if (savedInstanceState == null) syncWorkScheduler.scheduleForAppOpen()` (odtworzenie aktywności po obrocie albo zmianie motywu nie uruchamia próby).
3. `SyncCoordinator`: pole `private val waitingForEditor = MutableStateFlow(false)` i publiczne `val waitsForEditor: StateFlow<Boolean> = waitingForEditor.asStateFlow()`. W miejscach zwracających `SyncOutcome.WaitingForEditor` ustaw `true`; na początku każdej próby (`run`) ustaw `false`.
4. `SyncRoomChangeObserver`, kolektor `editTracker.editing`: `if (!editing && coordinator.waitsForEditor.value) scheduler.enqueueImmediate()`.
5. Dokumenty: w `docs/ARCHITECTURE.md`, sekcja 7, i `docs/SYNC.md` (lista „WorkManager: jednorazowa praca po zmianie planu, po otwarciu aplikacji i po zamknięciu formularza...”) zapisz: „po zmianie planu, po otwarciu aplikacji (z `MainActivity`, nie przy każdym starcie procesu) i po zamknięciu formularza, jeśli poprzednia próba czekała na formularz”.
6. `docs/QUEUE.md`: I-88 przenieś do tabeli „Zakończone” ze statusem `gotowe`, listą punktów audytu (F1, F2, F3, G1 bez zmiany liczby rozmiarów, K3) i uruchomionych testów. Druga część G1 (mniej rozmiarów widgetu) dopisz do pomysłów do rozważenia w `QUEUE.md` z odwołaniem do audytu.

Testy:

- `SyncCoordinatorTest`, nowy `waitingForEditorIsRememberedUntilTheNextRun`: tracker z zajętym kluczem i plan z Dysku do pobrania; `synchronize()` zwraca `WaitingForEditor` i `waitsForEditor.value` jest prawdą; po zwolnieniu klucza kolejne `synchronize()` ustawia `false`.
- Istniejące testy `SyncCoordinatorTest` i `SyncWorkResultTest` przechodzą bez zmian.

Przypadki brzegowe: anulowany formularz bez oczekującego pobrania nie uruchamia próby; zapisany formularz uruchamia próbę przez strumień zmian planu (krok 2); otwarcie aplikacji z widgetu (zimny start `MainActivity`) uruchamia próbę, a sam widget albo alarm bez otwarcia aplikacji jej nie uruchamia; okresowa praca co 60 minut zostaje.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest` i `dev.retza.mak.ui.settings.SyncViewModelTest`.

Kryterium zakończenia: testy przechodzą; `rg "scheduleForAppOpen" app/src/main` znajduje tylko `SyncWork.kt` i `MainActivity.kt`; `py scripts/check_map.py` zwraca 0. Commit: `chore: narrow sync triggers (I-88)`.
