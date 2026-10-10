# Plan najbliższych prac

Cel: wykonać I-87 (model kopii zapasowej bez encji Room, kroki 1 i 2), potem pierwsze trzy etapy I-86 (ViewModele w zakresie celu nawigacji, kroki 3 do 5). Raport audytu: `docs/audit_09102026.md`; oznaczenia w nawiasach (F4, B1 i podobne) to jego punkty. Decyzje użytkownika z 2026-10-10 są w `LOG.md`. I-87 idzie pierwsze, bo blokuje I-88 i I-81. Pozostałe etapy I-86 są wypisane w `QUEUE.md` i trafią do następnego planu.

## Zasady dla wykonawcy

Przeczytaj tę sekcję przed każdym krokiem.

- Wszystkie kroki wykonuj na gałęzi `task/I-79-hide-study-programs` (decyzja użytkownika z 2026-10-10). Przed pierwszym krokiem sprawdź `git branch --show-current`; jeśli to inna gałąź, przełącz się poleceniem `git switch task/I-79-hide-study-programs` i nie twórz nowej.
- Ścieżki kodu są względne wobec `app/src/main/java/dev/retza/mak/`, testów JVM wobec `app/src/test/java/dev/retza/mak/`, testów Android wobec `app/src/androidTest/java/dev/retza/mak/`.
- Przed zmianą otwórz każdy plik z listy kroku i znajdź opisany fragment po nazwie funkcji, nie po numerze wiersza. Jeśli fragmentu nie ma albo wygląda inaczej niż w opisie, zrób zmianę zgodną z celem kroku i opisz różnicę w commicie. Zatrzymaj się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Polecenia uruchamiaj w katalogu repozytorium w PowerShellu. Testy JVM jednej klasy: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.PlanMergeTest"`. Jeśli Gradle zgłosi `No cached version ... available for offline mode`, uruchom to samo polecenie raz bez `--offline`, potem wróć do `--offline`.
- Po każdej zmianie kodu: `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`. Kompilacja testów Android nie oznacza ich uruchomienia.
- Testy Android: najwyżej jeden emulator naraz, z 2048 MiB RAM. Obraz `Medium_Phone_A12` (`STACK.md`, sekcja 6); na komputerze, który go nie ma, `Medium_Phone` (testy używają aplikacji `dev.retza.mak.debug`, a dane odbioru synchronizacji są w `dev.retza.mak`; nie czyść ich). Jedna klasa na wywołanie: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.sync.RoomPlanSyncGatewayTest"`; lista klas po przecinku w PowerShellu uruchamia tylko pierwszą. Przed testami Android cofnij zmiany ekranu ze zrzutów (`adb shell wm size reset`), bo przy 320 dp część testów nie znajduje elementów. Testy Android przeinstalowują aplikację debug i zerują jej dane, więc zrzuty do porównania rób przed nimi albo odtwórz dane po nich.
- Nie uruchamiaj pełnego zestawu testów aplikacji bez polecenia użytkownika.
- Każdy krok kończy się jednym commitem z podanym tematem, bez stopki `Co-authored-by`. Do commita dołącz zmiany `PLAN.md`, `QUEUE.md` i dokumentów, których dotyczy krok. Po commicie dopisz pod nagłówkiem kroku linię `Stan: zrobione.` i krótko opisz różnice wobec planu; ta zmiana trafia do commita następnego kroku.
- Nie wypychaj gałęzi, nie otwieraj pull requesta i nie scalaj bez wyraźnego polecenia użytkownika.
- Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py`; wymagany kod 0.
- Teksty interfejsu i dokumentacja po polsku, identyfikatory i komentarze w kodzie po angielsku. Bez znaków `·`, `—` i strzałek.

## 1. Zastąp encje Room w BackupData rekordami repozytorium (I-87)

Stan: zrobione (`ac3918d`). Fragment `references()` w planie wołał `withReferences` na odbiorcy `buildList`; poprawione na `this@references.withReferences`. `DomainMappers.kt` nie wymagał zmian.

Cel: kontrakt kopii zapasowej i synchronizacji używa zwykłych klas danych, a encje Room nie wychodzą poza `data` (F4, `ARCHITECTURE.md`, sekcja 5: „Publiczne kontrakty nie wystawiają encji Room”). Rekordy z mapowaniem w obie strony już istnieją (`data/repository/SemesterRecords.kt`, `ScheduleRecords.kt`, funkcje `toRecord()` i `toEntity()` w `DomainMappers.kt`), więc krok nie dodaje nowego modelu, tylko zmienia typy. Przy okazji `PlanMerge.references()` powstaje z `withReferences` (F5). Format pliku JSON się nie zmienia.

Pliki: `data/repository/BackupData.kt`, `data/repository/PlanBackupGateway.kt`, `data/repository/DomainMappers.kt` (tylko jeśli brakuje mapowania), `data/repository/OccurrenceNoteRemap.kt`, `export/ExportImporter.kt`, `export/ExportSnapshot.kt`, `sync/PlanMerge.kt`, `sync/PlanDifferences.kt`, `ui/settings/SyncDifferenceLabels.kt`; testy JVM `export/BackupDataFixtures.kt`, `sync/PlanFixtures.kt`, `sync/PlanMergeTest.kt`, `sync/SyncCoordinatorTest.kt`, `ui/FakeRepository.kt` i inne wskazane przez kompilator; testy Android `data/RoomPersistenceTest.kt`, `sync/RoomPlanSyncGatewayTest.kt`.

Zmiany:

1. `BackupData.kt`: typy pól zamień na rekordy, nazwy pól zostają:
   - `BackupData.studyPrograms: List<StudyProgramRecord>`;
   - `SemesterBackup.semester: SemesterRecord`, `calendars: List<AcademicCalendarRecord>`, `programs: List<SemesterProgramRecord>`, `classes: List<ClassRecord>`, `weekOverrides: List<WeekOverrideRecord>`, `occurrenceNotes: List<OccurrenceNoteRecord>`, `occurrenceChanges: List<OccurrenceChangeRecord>`.
   - Usuń importy `dev.retza.mak.data.entity.*`. `activeSemesterId` działa bez zmian, bo `SemesterRecord` ma `isActive`.
2. `RoomPlanBackupGateway`: w `snapshot()` mapuj każdą encję przez `toRecord()` (`studyPrograms.getAll().map { it.toRecord() }`, `data.semester.toRecord()`, `data.academicCalendars.map { it.toRecord() }` i tak dalej). W `replaceAll` wstawiaj `it.toEntity()`. Brakujące funkcje `toRecord()` albo `toEntity()` dopisz w `DomainMappers.kt` obok istniejących, tym samym wzorem.
3. Odbiorcy przechodzą z enumów `dev.retza.mak.data.entity` na enumy domenowe o tych samych nazwach i wartościach (`dev.retza.mak.domain.WeekType`, `WeekOverrideScope`, `Recurrence`, `OccurrenceChangeKind`). Rekordy już ich używają.
4. Pola zmiany terminu mają w rekordzie krótsze nazwy. Przy przepisywaniu `OccurrenceChangeEntity` na `OccurrenceChangeRecord` zamień: `newStartTime` na `startTime`, `newEndTime` na `endTime`, `newRoom` na `room`, `newBuilding` na `building`, `newTeacherName` na `teacherName`, `newNote` na `note`. Pozostałe rekordy mają te same nazwy pól co encje.
5. `OccurrenceNoteRemap.kt`: importy `OccurrenceChangeKind` i `Recurrence` z `dev.retza.mak.domain`. `remapOccurrenceNotesToOriginalDates` i typy `NoteRow`, `ChangeRow`, `ClassRow` zostają bez zmian, bo używa ich też `Migrations.kt`.
6. `ExportImporter.kt`: buduj rekordy zamiast encji (`StudyProgramRecord(...)`, `AcademicCalendarRecord(...)` i tak dalej, z argumentami nazwanymi). `ExportSnapshot.kt`: funkcje `from(entity: XEntity)` przyjmują rekordy (`from(record: XRecord)`).
7. `PlanMerge.kt`: w `withId`, `withReferences`, `toBackupData` i rzutowaniach w `mergePlans` (`as ClassEntity`) zamień encje na rekordy. `references()` zastąp:
   ```kotlin
   /** The rows this row needs in the plan, read from the same table as [withReferences]. */
   internal fun Any.references(): List<PlanRowKey> = buildList {
       this@references.withReferences { kind, id ->
           add(PlanRowKey(kind, id))
           id
       }
   }
   ```
8. `PlanDifferences.kt` (`ClassField.read`, `ClassField.copy`, `classOf`) i `SyncDifferenceLabels.kt`: te same zamiany typów, gałęzie `is XEntity` na `is XRecord`.
9. Testy i atrapy: `PlanFixtures`, `BackupDataFixtures`, `SyncCoordinatorTest`, `PlanMergeTest` i miejsca w `FakeRepository`, które tworzą `BackupData` albo `SemesterBackup`, budują rekordy. `FakeRepository` może dalej trzymać encje we własnych listach i mapować je w `snapshot()` i `replaceAll` przez `toRecord()` i `toEntity()` (funkcje są `internal`, testy są w tym samym module). `RoomPersistenceTest` i `RoomPlanSyncGatewayTest` tworzą `BackupData` z rekordów; wstawianie przez DAO zostaje na encjach.

Testy:

- `PlanMergeTest`, nowy test `referencesFollowWithReferences`: dla jednego rekordu każdego rodzaju (`AcademicCalendarRecord`, `SemesterProgramRecord`, `ClassRecord`, `WeekOverrideRecord`, `OccurrenceNoteRecord`, `OccurrenceChangeRecord`) lista `references()` zawiera klucze wszystkich pól odwołań, a dla `StudyProgramRecord` i `SemesterRecord` jest pusta. Przykład: `ClassRecord(id = 5, semesterId = 1, semesterProgramId = 2, ...)` daje `listOf(PlanRowKey(PlanRowKind.SEMESTER, 1), PlanRowKey(PlanRowKind.SEMESTER_PROGRAM, 2))`.
- `JsonExportCodecTest`, `ExportImporterTest`, `PlanBackupServiceTest`, `PlanDifferencesTest`, `PlanMergeTest`, `SyncCoordinatorTest`: istniejące testy przechodzą bez zmiany oczekiwań. To sprawdza, że format pliku się nie zmienił.

Przypadki brzegowe: plik eksportu w wersji 2 nadal przepina notatki przez `withOccurrenceNotesOnOriginalDates`; scalanie, które przenumerowuje wpis z Dysku, przepina też jego dzieci, bo `references()` i `withReferences` czytają teraz jedną tabelę odwołań; puste `room` i `building` w zmianie terminu dalej są zapisane jako `null`.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.export.*" --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.ui.settings.*" --tests "dev.retza.mak.data.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.data.RoomPersistenceTest`, `dev.retza.mak.sync.RoomPlanSyncGatewayTest` i `dev.retza.mak.ui.settings.SyncViewModelTest`.

Kryterium zakończenia: testy przechodzą; `rg "data\.entity" app/src/main/java/dev/retza/mak/export app/src/main/java/dev/retza/mak/sync app/src/main/java/dev/retza/mak/ui` nic nie znajduje; w `data/repository/BackupData.kt` nie ma słowa `Entity`. Commit: `chore: use plain records in the backup contract (I-87)`.

## 2. Sprawdzaj i zastępuj plan synchronizacji w jednej transakcji w data (I-87)

Stan: zrobione. Kryterium `rg "withTransaction|AppDatabase"` w `sync` znajduje jeszcze `SyncRoomChangeObserver` w `SyncWork.kt`, który obserwuje zmiany tabel; zostaje do I-88 (jeden strumień zmian planu).

Cel: sprawdzenie, że plan na telefonie się nie zmienił, i zastąpienie go planem z Dysku są jedną transakcją otwieraną w `data`, a `RoomPlanSyncGateway` nie zna `AppDatabase` (F4). Skrót planu liczy dalej `SyncPlanFile` w `sync`, bo `data` nie zna formatu pliku, więc warunek przychodzi do `data` jako funkcja.

Pliki: `data/repository/PlanBackupGateway.kt`, `sync/PlanSyncGateway.kt`; testy `export/PlanBackupServiceTest.kt` i `ui/FakeRepository.kt` (atrapy `PlanBackupGateway`), Android `sync/RoomPlanSyncGatewayTest.kt`; dokumenty `docs/SYNC.md`, `docs/QUEUE.md`.

Zmiany:

1. `PlanBackupGateway`: dodaj
   ```kotlin
   /**
    * Reads the plan and replaces it in one transaction. [prepare] gets the current plan and returns the plan
    * to write, or null to leave the database unchanged. It runs inside the transaction, so it must not suspend.
    * True when the plan was replaced.
    */
   suspend fun replaceIfCurrent(prepare: (current: BackupData) -> BackupData?): Boolean
   ```
   W `RoomPlanBackupGateway`:
   ```kotlin
   override suspend fun replaceIfCurrent(prepare: (current: BackupData) -> BackupData?): Boolean =
       database.withTransaction {
           val next = prepare(snapshot()) ?: return@withTransaction false
           replaceAll(next)
           true
       }
   ```
   Room pozwala zagnieździć `withTransaction` z `snapshot()` i `replaceAll` w tej transakcji.
2. `RoomPlanSyncGateway`: usuń parametr konstruktora `database: AppDatabase` i importy `androidx.room.withTransaction` i `AppDatabase`. Ciało `replaceIfUnchanged` do wyliczenia `replaced`:
   ```kotlin
   val replaced = backup.replaceIfCurrent { current ->
       if (SyncPlanFile.fingerprint(current) != expectedFingerprint) {
           null
       } else {
           data.withActiveSemester(
               activeSemesterAfterReplace(current.activeSemesterId, data, keepLocalActive, fallbackActiveId)
           )
       }
   }
   ```
   Dalsza część (czyszczenie ukrytych kierunków po pierwszym pobraniu) zostaje bez zmian.
3. Atrapy `PlanBackupGateway` w testach (`PlanBackupServiceTest`, `FakeRepository`, obiekt `blockingBackup` w `RoomPlanSyncGatewayTest`) dostają `replaceIfCurrent`. W atrapach JVM wystarczy `prepare(snapshot())?.let { replaceAll(it); true } ?: false`. W `blockingBackup` blokada (`replacementEntered`, `finishReplacement`) przechodzi do `replaceIfCurrent` przed wywołaniem `backup.replaceIfCurrent(prepare)`. Wywołania `RoomPlanSyncGateway(database, backup, preferences)` w tym teście zmień na `RoomPlanSyncGateway(backup, preferences)`.
4. `docs/SYNC.md`, punkt 2 listy o zastosowaniu planu z Dysku („Zastosowanie planu z Dysku to jedna transakcja `replaceAll`...”): zamień na „Zastosowanie planu z Dysku to jedna transakcja w `data` (`replaceIfCurrent`), która najpierw sprawdza, że skrót planu lokalnego się nie zmienił.”, resztę punktu zostaw.
5. `docs/QUEUE.md`, wiersz I-87: w kryteriach zamień `replaceAll(data, expected)` na `replaceIfCurrent(prepare)`, przenieś wiersz do tabeli „Zakończone” ze statusem `gotowe`, listą zrealizowanych punktów (F4, F5) i uruchomionych testów.

Testy:

- `RoomPlanSyncGatewayTest`: istniejące testy przechodzą, w tym `editorStartedDuringRoomReplacementReadsTheCommittedPlan` i test, w którym plan zmienia się między odczytem skrótu a zastąpieniem (oczekuje `false` i niezmienionego planu).
- `RoomPlanSyncGatewayTest`, nowy test `replaceIfCurrentWritesNothingWhenPrepareReturnsNull`: `backup.replaceAll(plan("Lokalny", active = 1))`, potem `backup.replaceIfCurrent { null }` zwraca `false`, a `backup.snapshot()` jest równe planowi sprzed wywołania.
- `SyncCoordinatorTest`: istniejące testy przechodzą bez zmian oczekiwań.

Przypadki brzegowe: wyjątek w `prepare` albo w `replaceAll` wycofuje całą transakcję, więc plan zostaje w poprzednim stanie; `prepare` nie może wołać funkcji `suspend`, co pilnuje kompilator przez typ parametru; zmiana planu w trakcie sprawdzenia daje `false` jak dziś.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.export.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.sync.RoomPlanSyncGatewayTest`, `dev.retza.mak.data.RoomPersistenceTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą; `rg "withTransaction|AppDatabase" app/src/main/java/dev/retza/mak/sync` nic nie znajduje; `py scripts/check_map.py` zwraca 0. Commit: `chore: check and replace the synced plan in one data transaction (I-87)`.

## 3. Udostępnij jeden współdzielony strumień aktywnego planu (I-86)

Stan: zrobione. `OccurrenceViewModel.refreshForEdit` czytał `observeActive().first()`, które przy aktywnym kolektorze zwraca ostatnią wartość ze wspólnego strumienia zamiast stanu bazy; teraz czyta aktywny semestr i `observe(id).first()`, jak `reload`. `ActivePlanSourceTest` używa zakresu na `StandardTestDispatcher(testScheduler)` zamiast `backgroundScope`. Ręcznie: usunięcie zajęć od razu zmienia „Dzisiaj”, „Plan” i liczbę zajęć w „Semestrach”; widgetu nie sprawdzano.

Cel: ekrany, widget i powiadomienia czytają plan aktywnego semestru z jednego zapytania Room zamiast każdy z własnego (B1), a ustawienia nie wczytują osobno całego planu dla kilku liczb (B4 dla ustawień). Wygląd i zachowanie się nie zmieniają.

Pliki: `ui/ActivePlanSource.kt`, `di/AppModule.kt`, `ui/settings/SettingsViewModel.kt`; testy `ui/FakeSemesterRepository.kt` (funkcja `activePlanSource`), `ui/FakeRepository.kt`, nowy `ui/ActivePlanSourceTest.kt`, `widget/WidgetPlanLoaderTest.kt`, `ui/settings/SettingsViewModelTest.kt`, Android `notifications/CollisionAlarmSchedulerTest.kt`.

Zmiany:

1. `ActivePlanSource`:
   - Usuń adnotację `@org.koin.core.annotation.Single` z klasy i dodaj ostatni parametr konstruktora `scope: CoroutineScope`.
   - `observeActive()` zwraca jedno pole klasy:
     ```kotlin
     // One Room query for every collector. The cache is dropped when nobody listens, so a later first()
     // from a receiver reads the database again instead of an old plan.
     private val active: Flow<ActivePlanInputs?> = semesterRepository.observeActiveSemester()
         .flatMapLatest { semester -> if (semester == null) flowOf(null) else observe(semester.id) }
         .distinctUntilChanged()
         .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0), replay = 1)

     fun observeActive(): Flow<ActivePlanInputs?> = active
     ```
   - `observe(semesterId)` zostaje zimnym strumieniem; używają go widget i `OccurrenceViewModel` dla podanego semestru.
2. `di/AppModule.kt`: dodaj, wzorem `providePlanEditTracker`:
   ```kotlin
   @Single
   fun provideActivePlanSource(
       semesterRepository: SemesterRepository,
       scheduleRepository: ScheduleRepository,
       preferences: SettingsPreferences
   ): ActivePlanSource = ActivePlanSource(
       semesterRepository,
       scheduleRepository,
       preferences,
       CoroutineScope(SupervisorJob() + Dispatchers.Default)
   )
   ```
3. `SettingsViewModel`: zamień parametr konstruktora `scheduleRepository: ScheduleRepository` na `activePlanSource: ActivePlanSource` (jeśli `rg -n "scheduleRepository" app/src/main/java/dev/retza/mak/ui/settings/SettingsViewModel.kt` pokaże inne użycia, zostaw oba). Pole `activePlanData` zastąp wyrażeniem `activePlanSource.observeActive().map { it?.data }`. `buildSettingsState` dostaje te same dane co dziś (`ActivePlanData?`).
4. Nie przepinaj odczytów `scheduleRepository.observeActivePlanData(semester.id).first()` w `ClassEditViewModel` i `OccurrenceViewModel` (funkcje przygotowania edycji): mają czytać bazę z chwili przed zapisem, a nie wspólny strumień.
5. Testy: funkcja `activePlanSource(...)` w `FakeSemesterRepository.kt` dostaje parametr `scope: CoroutineScope = CoroutineScope(Dispatchers.Main)` i przekazuje go do konstruktora. W testach ViewModeli `Dispatchers.Main` podmienia `MainDispatcherRule`, więc `advanceUntilIdle()` obejmuje współdzielony strumień. Testy bez tej reguły (sprawdź `WidgetPlanLoaderTest`) przekazują `backgroundScope`. `SettingsViewModelTest` buduje `SettingsViewModel` z `activePlanSource(...)`. Trzy wywołania w `CollisionAlarmSchedulerTest` przekazują `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, anulowany w `@After`.

Testy:

- `ActivePlanSourceTest` (nowy, JVM): `twoCollectorsShareOneQuery`: w `FakeRepository` dodaj licznik `activePlanDataQueries`, zwiększany w `observeActivePlanData`; dwóch kolektorów `observeActive()` w `backgroundScope`, `advanceUntilIdle()`, licznik równa się 1.
- `ActivePlanSourceTest`: `sameDataIsNotEmittedTwice`: kolektor zbiera do listy, potem `preferences.setTheme(ThemeMode.Dark)` (nie zmienia `planDisplay`), `advanceUntilIdle()`; lista ma jeden element.
- `SettingsViewModelTest`: istniejące testy przechodzą bez zmian oczekiwań.

Przypadki brzegowe: `CollisionAlarmReceiver` i `CollisionAlarmScheduler.refresh()` wołają `observeActive().first()` bez innych kolektorów; dzięki `replayExpirationMillis = 0` dostają świeży odczyt bazy, a nie plan sprzed godzin; zmiana aktywnego semestru przełącza zapytanie przez `flatMapLatest` jak dziś; przez 5 s po zamknięciu ostatniego ekranu zapytanie dalej działa, więc powrót na ekran nie czyta bazy ponownie.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.widget.*" --tests "dev.retza.mak.domain.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.notifications.CollisionAlarmSchedulerTest` i `dev.retza.mak.ui.settings.SettingsScreenTest`.
4. Ręcznie (wersja debug): dodanie zajęć w formularzu od razu zmienia „Dzisiaj”, „Plan” i widget na ekranie głównym; „Ustawienia” pokazują liczbę kierunków i zajęć aktywnego semestru jak przed zmianą.

Kryterium zakończenia: testy przechodzą; `rg -n "observeActivePlanData" app/src/main` znajduje tylko `ScheduleRepository.kt`, `ActivePlanSource.kt` i odczyty przed edycją w `ClassEditViewModel` i `OccurrenceViewModel`. Commit: `chore: share one active plan stream (I-86)`.

## 4. Niech cel nawigacji deklaruje tytuł i akcje górnego paska (I-86)

Stan: zrobione. Atrapa `MemoryStore` w `SyncViewModelTest` przyjmuje stan początkowy, bo koordynator czyta stan przy tworzeniu. Zrzuty przy 320 dp (formularz „Dodaj zajęcia”, szczegóły, menu „Więcej opcji”, „Edytuj zajęcia” i powrót do szczegółów) przed zmianą i po niej są identyczne.

Cel: `MakApp` nie zbiera stanu formularza zajęć, szczegółów terminu ani całego stanu synchronizacji, więc wpisanie znaku w formularzu nie przebudowuje powłoki (D1). Tytuł formularza i menu szczegółów deklarują ich cele nawigacji, a „Dzisiaj” zbiera tylko informację o problemie synchronizacji. To warunek dalszych etapów I-86, w których ViewModele formularza i szczegółów znikną z `MakApp`. Wygląd się nie zmienia.

Pliki: nowy `ui/TopBar.kt`, `ui/MakNavHostApp.kt`, `ui/MakRoutes.kt`, `ui/ClassOccurrenceRoutes.kt`, `ui/TodayScheduleRoutes.kt`, `ui/settings/SyncViewModel.kt`; testy `ui/MakNavigationTest.kt`, nowy `ui/TopBarStateTest.kt`, Android `ui/settings/SyncViewModelTest.kt`.

Zmiany:

1. Nowy plik `ui/TopBar.kt`:
   ```kotlin
   /** What a destination adds to the shared top bar while it is shown. */
   internal data class TopBarSpec(
       val route: String,
       val title: String? = null,
       val actions: List<Pair<String, () -> Unit>> = emptyList()
   )

   /** Holds the spec of the destination on screen; a leaving destination clears only its own spec. */
   @Stable
   internal class TopBarState {
       private var spec by mutableStateOf<TopBarSpec?>(null)

       fun show(spec: TopBarSpec) {
           this.spec = spec
       }

       fun clear(route: String) {
           if (spec?.route == route) spec = null
       }

       fun specFor(route: String?): TopBarSpec? = spec?.takeIf { it.route == route }
   }

   /** Declares the top bar of the calling destination for as long as it stays composed. */
   @Composable
   internal fun DeclareTopBar(state: TopBarState, spec: TopBarSpec) {
       SideEffect { state.show(spec) }
       DisposableEffect(state, spec.route) { onDispose { state.clear(spec.route) } }
   }
   ```
2. `MakRoutes.kt`, `titleForRoute`: usuń parametr `editorTitle`; gałąź `MakRoutes.Edit -> "Zajęcia"`.
3. `MakNavHostApp.kt`, funkcja `MakApp`:
   - Usuń `val syncState = syncViewModel.sync.collectAsStateWithLifecycle().value` i `val editor = classEditViewModel.editor.collectAsStateWithLifecycle().value`.
   - Dodaj `val topBar = remember { TopBarState() }` i `val topBarSpec = topBar.specFor(currentRoute)`.
   - Usuń wywołanie `occurrenceTopBarActions(...)` i zmienną `occurrenceActions`.
   - W `MakTopBar`: `title = topBarSpec?.title ?: titleForRoute(currentRoute)`; `actions` buduj z `topBarSpec?.actions.orEmpty()` tym samym warunkiem co dziś z `occurrenceActions` (pusta lista daje `null`).
   - `classEditRoute` dostaje argument `topBar = topBar`; `occurrenceDetailsRoute` dostaje `topBar = topBar` i `navController = navController`.
   - `todayRoute` dostaje `syncAttention = syncViewModel.attention` (strumień z punktu 6) zamiast lambdy.
4. `ClassOccurrenceRoutes.kt`:
   - `classEditRoute` dostaje parametr `topBar: TopBarState`. Po warunkach `if (!opened || !openSucceeded)`, przed `ClassEditScreen`, zbierz stan raz (`val editorState = classEditViewModel.editor.collectAsStateWithLifecycle().value`), przekaż go jako `state = editorState` i dodaj `DeclareTopBar(topBar, TopBarSpec(MakRoutes.Edit, title = editorState.title))`.
   - `occurrenceDetailsRoute` dostaje parametry `topBar: TopBarState` i `navController: NavController`. Treść funkcji `occurrenceTopBarActions` przenieś do tej trasy, po wyliczeniu `editing`, i użyj tego samego kontrolera `editing` zamiast drugiego `TrackPlanEditing`. Wynik podaj przez `DeclareTopBar(topBar, TopBarSpec(MakRoutes.Occurrence, actions = actions))`. Usuń funkcję `occurrenceTopBarActions` i importy, których nic już nie używa.
5. `TodayScheduleRoutes.kt`, `todayRoute`: parametr `syncAttention: StateFlow<SyncAttentionUi?>`; w `composable` `val attention = syncAttention.collectAsStateWithLifecycle().value` i `syncAttention = attention` w `TodayScreen`.
6. `SyncViewModel`:
   - Wydziel z `SyncState.toUi` wyliczenie pola `attention` do `private fun SyncState.attentionUi(): SyncAttentionUi?` i użyj jej w `toUi`.
   - Dodaj `val attention: StateFlow<SyncAttentionUi?> = coordinator.state.map { it.attentionUi() }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)`. Ten strumień nie listuje archiwum, w przeciwieństwie do `sync`.

Testy:

- `MakNavigationTest`: asercje z `titleForRoute(MakRoutes.Edit, "...")` zamień na `assertEquals("Zajęcia", titleForRoute(MakRoutes.Edit))`; asercję `titleForRoute(MakRoutes.Schedule, "Dodaj zajęcia")` na `titleForRoute(MakRoutes.Schedule)`.
- `TopBarStateTest` (nowy, JVM): `leavingDestinationDoesNotClearTheNextOne`: `show(TopBarSpec(MakRoutes.Edit, title = "Edytuj zajęcia"))`, potem `clear(MakRoutes.Occurrence)`; `specFor(MakRoutes.Edit)?.title == "Edytuj zajęcia"`. `specForOtherRouteIsNull`: po `show` dla `Edit` `specFor(MakRoutes.Today)` jest `null`.
- `SyncViewModelTest` (Android): nowy test `attentionMatchesSyncAttention`: przy stanie z problemem autoryzacji (wzór w istniejących testach tej klasy) `attention.value?.text` równa się `sync.value.attention?.text`.

Przypadki brzegowe: przejście ze szczegółów do formularza („Edytuj wszystkie terminy”) pokazuje tytuł formularza, a zamknięcie szczegółów go nie czyści, bo `clear` sprawdza trasę; na ekranie oczekiwania przed otwarciem formularza tytuł to „Zajęcia”; menu szczegółów znika razem z celem nawigacji; pisanie w notatce szczegółów przebudowuje pasek, ale nie całą powłokę.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.ui.settings.SyncViewModelTest`, `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`, `dev.retza.mak.ui.edit.ClassEditScreenTest` i `dev.retza.mak.ui.today.TodayScreenTest`.
4. Ręcznie przy 320 dp, zanim uruchomisz testy Android (zerują dane): tytuł „Dodaj zajęcia” i „Edytuj zajęcia” w formularzu, menu „Więcej opcji” w szczegółach z „Odwołaj termin”, „Edytuj wszystkie terminy” i „Usuń zajęcia”, przejście ze szczegółów do formularza i z powrotem. Zrzuty paska formularza i szczegółów przed zmianą i po niej mają być identyczne.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; w `MakApp` nie ma `classEditViewModel.editor`, `syncViewModel.sync` ani `occurrenceTopBarActions`. Commit: `chore: let destinations declare their top bar (I-86)`.

## 5. Pobieraj ViewModele „Dzisiaj” i „Planu” w ich celach nawigacji (I-86)

Stan: zrobione. `scheduleRoute` dostaje `pendingPlanDate: () -> String?` zamiast wartości: graf `NavHost` zapamiętuje argumenty z chwili zbudowania, więc zwykła wartość zostawała `null` i data z powiadomienia nie działała. Ręcznie: tydzień, widok i wybrany dzień „Planu” zostają po przełączeniu zakładek i powrocie ze szczegółów; data z intencji otwiera „Plan” na właściwym dniu przy działającej aplikacji, po raz drugi i przy zimnym starcie; „Dodaj termin jednorazowy” otwiera formularz z datą dnia.

Cel: `TodayViewModel` i `ScheduleViewModel` powstają w swoich celach nawigacji przez `koinViewModel()` zamiast w `MainActivity` (C1, decyzja użytkownika z 2026-10-10). `MainActivity` nie odświeża już dnia tych ekranów przy powrocie aplikacji; robi to sam cel nawigacji. Zachowanie widoczne dla użytkownika się nie zmienia, także otwarcie „Planu” na dacie z powiadomienia.

Pliki: `MainActivity.kt`, `ui/MakNavHostApp.kt`, `ui/TodayScheduleRoutes.kt`; dokument `docs/ARCHITECTURE.md`.

Zmiany:

1. `MainActivity`: usuń pola `todayViewModel` i `scheduleViewModel`, ich argumenty w `MakApp(...)` i oba wywołania `refreshToday()` w obserwatorze `ON_RESUME` (obserwator zostaje dla `notificationsBlocked`).
2. `MakApp`: usuń parametry `todayViewModel` i `scheduleViewModel` oraz wywołanie `ScheduleEffects(...)`. Prośbę o datę z powiadomienia przechowaj w stanie powłoki zamiast wołać `scheduleViewModel.showDate`:
   ```kotlin
   var pendingPlanDate by rememberSaveable { mutableStateOf<String?>(null) }
   LaunchedEffect(openPlanRequests, navController) {
       openPlanRequests.collect { date ->
           pendingPlanDate = date
           openRoot(MakRoutes.Schedule)
       }
   }
   ```
   Do `scheduleRoute` przekaż `navController = navController`, `pendingPlanDate = pendingPlanDate` i `onPlanDateShown = { pendingPlanDate = null }`.
3. `todayRoute`: usuń parametr `todayViewModel`. W `composable(MakRoutes.Today)` na początku:
   ```kotlin
   val todayViewModel: TodayViewModel = koinViewModel()
   LifecycleResumeEffect(todayViewModel) {
       todayViewModel.refreshToday()
       onPauseOrDispose { }
   }
   ```
   Importy: `org.koin.compose.viewmodel.koinViewModel`, `androidx.lifecycle.compose.LifecycleResumeEffect`.
4. `scheduleRoute`: usuń parametr `scheduleViewModel`, dodaj `navController: NavController`, `pendingPlanDate: String?` i `onPlanDateShown: () -> Unit`. W `composable(MakRoutes.Schedule)` na początku:
   ```kotlin
   val scheduleViewModel: ScheduleViewModel = koinViewModel()
   LifecycleResumeEffect(scheduleViewModel) {
       scheduleViewModel.refreshToday()
       onPauseOrDispose { }
   }
   LaunchedEffect(pendingPlanDate) {
       pendingPlanDate?.let { date ->
           scheduleViewModel.showDate(date)
           onPlanDateShown()
       }
   }
   ScheduleEffects(scheduleViewModel, navController)
   ```
   Dalej bez zmian. `refreshToday()` nie zmienia stanu, gdy dzień jest ten sam, więc wywołanie przy każdym powrocie na ekran jest bezpieczne.
5. `docs/ARCHITECTURE.md`, sekcja 5: w miejscu o ViewModelach i `MainActivity` (`rg -n "MainActivity" docs/ARCHITECTURE.md`) dopisz: „`AppViewModel`, `SettingsViewModel`, `UpdateViewModel` i `SyncViewModel` żyją w zakresie `MainActivity`, bo obsługują powłokę. Pozostałe ViewModele ekranów są pobierane w celu nawigacji przez `koinViewModel()`, a `SemesterViewModel` w zakresie podgrafu semestru (I-86; na razie „Dzisiaj” i „Plan”).” Nawias poprawi ostatni etap I-86.

Testy:

- `TodayViewModelTest` i `ScheduleViewModelTest`: istniejące testy przechodzą bez zmian, bo ViewModele się nie zmieniają.
- `KoinGraphTest` (Android): przechodzi bez zmian i nadal rozwiązuje oba ViewModele.
- Testów powłoki z `MakApp` nie ma, więc nawigację sprawdź ręcznie (weryfikacja, punkt 4).

Przypadki brzegowe: przełączanie „Dzisiaj” i „Plan” paskiem nawigacji zachowuje wybrany tydzień i widok „Planu”, bo `openRoot` zapisuje stan celu (`saveState`, `restoreState`) razem z jego ViewModelem; data z powiadomienia przychodzi, zanim „Plan” jest złożony, i czeka w `pendingPlanDate`; po obrocie ekranu prośba nie wykonuje się drugi raz, bo `onPlanDateShown` ją czyści; po północy powrót do aplikacji przesuwa „Dzisiaj” i „Plan” na nowy dzień.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.today.*" --tests "dev.retza.mak.ui.schedule.*" --tests "dev.retza.mak.ui.MakNavigationTest"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.ui.today.TodayScreenTest` i `dev.retza.mak.ui.schedule.ScheduleScreenTest`.
4. Ręcznie (wersja debug): w „Planie” przejdź do następnego tygodnia i widoku kalendarza, przełącz na „Dzisiaj” i z powrotem: tydzień i widok zostają. Otwórz szczegóły terminu z „Planu” i wróć: wybrany dzień zostaje. „Dodaj zajęcia jednorazowe” z kalendarza otwiera formularz z datą dnia. `adb shell am start -n dev.retza.mak.debug/dev.retza.mak.MainActivity --es dev.retza.mak.extra.OPEN_PLAN_DATE 2026-10-15` pokazuje „Plan” na 15 października.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "todayViewModel|scheduleViewModel" app/src/main/java/dev/retza/mak/MainActivity.kt app/src/main/java/dev/retza/mak/ui/MakNavHostApp.kt` nic nie znajduje. Commit: `chore: scope today and plan view models to their destinations (I-86)`. W tym commicie w `QUEUE.md` przy I-86 zapisz, które etapy są zrobione.
