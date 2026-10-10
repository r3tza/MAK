# Plan najbliższych prac

Cel: dokończyć I-86 (ViewModele w zakresie celu nawigacji, etapy 4 do 7 z `QUEUE.md`). Raport audytu: `docs/audit_09102026.md`; oznaczenia w nawiasach (C1, D2 i podobne) to jego punkty. Decyzja użytkownika z 2026-10-10 o zakresie ViewModeli jest w `LOG.md`. Etapy 1 do 3 są zrobione: wspólny strumień planu, górny pasek deklarowany przez cel nawigacji, ViewModele „Dzisiaj” i „Planu” w celach nawigacji. Po kroku 5 w `MainActivity` zostają tylko `AppViewModel`, `SettingsViewModel`, `UpdateViewModel` i `SyncViewModel`.

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

## 1. Pobieraj ViewModele szczegółów terminu i formularza zajęć w ich celach nawigacji (I-86)

Stan: zrobione. Pomocnik `open(routeId)` w teście pomija niepoprawne identyfikatory zamiast `!!`, bo istniejące testy sprawdzają takie wejście. `openOnce` czyta datę bez wyjątku przy złym formacie. Ręcznie: odwołanie i przywrócenie terminu, „Edytuj wszystkie terminy” z danymi zajęć, szkic nazwy po dwukrotnym odtworzeniu aktywności (tryb nocny), zapis wraca do szczegółów z nową nazwą, pusty formularz „Dodaj zajęcia”, termin jednorazowy z datą dnia kalendarza.

Cel: `OccurrenceViewModel` i `ClassEditViewModel` powstają w swoich celach nawigacji i czytają argumenty z `SavedStateHandle` (C1). Szczegóły przestają liczyć się po zamknięciu ekranu, bo ich ViewModel znika razem z celem (C2). Znikają flagi `opened` i `openSucceeded` w trasach (D4). Zachowanie widoczne dla użytkownika się nie zmienia.

Pliki: `MainActivity.kt`, `ui/MakNavHostApp.kt`, `ui/ClassOccurrenceRoutes.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/edit/ClassEditViewModel.kt`; testy `ui/occurrence/OccurrenceViewModelTest.kt`, `ui/edit/ClassEditViewModelTest.kt`, Android `di/KoinGraphTest.kt`.

Zmiany:

1. `OccurrenceViewModel`:
   - Dodaj parametr konstruktora `savedState: SavedStateHandle` (ostatni). W `init` odczytaj argumenty: `OccurrenceArgs.parse("${savedState.get<String>("classId")}:${savedState.get<String>("date")}")`; jeśli są poprawne, wywołaj `open(args)`, w przeciwnym razie ustaw `state.value = derive(emptyOccurrenceDetails().copy(notFound = true))`.
   - `fun open(args: OccurrenceArgs)` zmień na `internal fun open(args: OccurrenceArgs)`; usuń publiczne `fun open(routeId: String)`. W `OccurrenceViewModelTest` dodaj na końcu pliku `private fun OccurrenceViewModel.open(routeId: String) = open(OccurrenceArgs.parse(routeId)!!)`, aby istniejące wywołania w testach zostały bez zmian. Pomocnik tworzący ViewModel w teście przekazuje `SavedStateHandle()` (bez argumentów, więc `init` ustawia `notFound`, a test woła `open` sam).
2. `ClassEditViewModel`:
   - Dodaj stan otwarcia trzymany w `SavedStateHandle`, aby przetrwał zmianę konfiguracji i zakończenie procesu:
     ```kotlin
     enum class ClassEditOpenState { Opening, Open, Missing }

     private val openState = MutableStateFlow(savedState.get<String>(KEY_OPEN_STATE)?.let(ClassEditOpenState::valueOf) ?: ClassEditOpenState.Opening)
     val opened: StateFlow<ClassEditOpenState> = openState.asStateFlow()
     ```
   - Dodaj funkcję, którą trasa woła po dopuszczeniu edycji:
     ```kotlin
     /** Loads the form for this destination once; a restored draft is kept (`ARCHITECTURE.md`, section 5). */
     suspend fun openOnce() {
         if (openState.value != ClassEditOpenState.Opening) return
         val classId = savedState.get<String>("classId")?.toLongOrNull()
         val date = savedState.get<String>("date")
         val result = if (classId != null && date != null) {
             if (openEditFromFreshPlan(OccurrenceArgs(classId, LocalDate.parse(date)).toRouteId())) ClassEditOpenState.Open else ClassEditOpenState.Missing
         } else {
             openNewFromFreshPlan(date?.toLocalDateOrNull())
             ClassEditOpenState.Open
         }
         openState.value = result
         savedState[KEY_OPEN_STATE] = result.name
     }
     ```
     `KEY_OPEN_STATE` dodaj obok `KEY_EDITING_CLASS_ID`. Sprawdź w `MakRoutes.Edit` i `editRoute`, jak wygląda argument `date` dla edycji istniejących zajęć; jeśli nie jest datą `LocalDate`, użyj tej samej konwersji co dzisiejsza trasa (`entry.occurrenceArgs()`).
3. `ClassOccurrenceRoutes.kt`:
   - `classEditRoute`: usuń parametr `classEditViewModel`; w `composable` pierwsza linia `val classEditViewModel: ClassEditViewModel = koinViewModel()`. Usuń `opened`, `openSucceeded`, `args`, `date` i oba `LaunchedEffect` z nimi. Po `if (!editing.ready) { ... }`:
     ```kotlin
     val openState = classEditViewModel.opened.collectAsStateWithLifecycle().value
     LaunchedEffect(Unit) { classEditViewModel.openOnce() }
     if (openState != ClassEditOpenState.Open) {
         LaunchedEffect(openState) { if (openState == ClassEditOpenState.Missing) onBack() }
         PlanEditingWait()
         return@composable
     }
     ClassEditEffects(classEditViewModel, navController)
     ```
     Trasa dostaje parametr `navController: NavController`.
   - `occurrenceDetailsRoute`: usuń parametr `occurrenceViewModel`; pierwsza linia `val occurrenceViewModel: OccurrenceViewModel = koinViewModel()`. Usuń `opened` i `LaunchedEffect(args, opened)`. `occurrenceId` licz jak dziś z `entry.occurrenceArgs()`. Dodaj `OccurrenceEffects(occurrenceViewModel, navController)`.
4. `MakNavHostApp.kt`: usuń parametry `occurrenceViewModel` i `classEditViewModel` z `MakApp`, wywołania `OccurrenceEffects(...)` i `ClassEditEffects(...)` oraz te argumenty w `classEditRoute` i `occurrenceDetailsRoute` (dodaj `navController = navController` do `classEditRoute`).
5. `MainActivity`: usuń pola `occurrenceViewModel` i `classEditViewModel` i ich argumenty w `MakApp(...)`.
6. `KoinGraphTest`: `OccurrenceViewModel` rozwiązuj tak jak `ClassEditViewModel`, z `parametersOf(SavedStateHandle())`, z tym samym komentarzem.

Testy:

- `OccurrenceViewModelTest`, nowy `detailsOpenFromSavedStateArguments`: ViewModel z `SavedStateHandle(mapOf("classId" to "1", "date" to "2026-09-21"))`, `advanceUntilIdle()`; `details.value.notFound` jest fałszem, a `subjectName` to nazwa zajęć 1 z danych testu.
- `OccurrenceViewModelTest`, nowy `missingArgumentsShowNotFound`: `SavedStateHandle()` daje `notFound == true`.
- `ClassEditViewModelTest`, nowy `openOnceLoadsTheClassFromArguments`: `SavedStateHandle(mapOf("classId" to "1", "date" to "2026-09-21"))`, `openOnce()`; `opened.value == Open`, `editor.value.name` to nazwa zajęć 1.
- `ClassEditViewModelTest`, nowy `openOnceKeepsARestoredDraft`: `SavedStateHandle` z argumentami i zapisanym `KEY_OPEN_STATE = "Open"` oraz szkicem (wzór zapisu szkicu w istniejącym teście odtwarzania po zakończeniu procesu); `openOnce()` nie zmienia `editor.value.name` ze szkicu.
- `ClassEditViewModelTest`, nowy `openOnceForMissingClassReportsMissing`: `classId` = `999`; `opened.value == Missing`.

Przypadki brzegowe: obrót ekranu w formularzu nie wczytuje zajęć ponownie, bo `openState` jest `Open`; po zakończeniu procesu szkic i `Open` wracają z `SavedStateHandle`; zajęcia usunięte w tle przed otwarciem formularza dają `Missing` i powrót; zamknięte szczegóły nie przeliczają się po zapisie planu, bo ich ViewModel jest wyczyszczony.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.*" --tests "dev.retza.mak.ui.edit.*" --tests "dev.retza.mak.ui.MakNavigationTest"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Ręcznie (wersja debug, przed testami Android): otwórz szczegóły terminu z „Planu”, odwołaj i przywróć termin, zmień salę; „Edytuj wszystkie terminy” otwiera formularz z danymi zajęć; zapis wraca do szczegółów z nowymi danymi; „Dodaj” otwiera pusty formularz; „Dodaj termin jednorazowy” w kalendarzu wpisuje datę dnia; odtworzenie aktywności w formularzu (`adb shell cmd uimode night yes`, potem `night no`; telefon ma zablokowany pion) nie gubi wpisanej nazwy.
4. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.ui.PlanEditingTest`, `dev.retza.mak.ui.edit.ClassEditScreenTest` i `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest`.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "occurrenceViewModel|classEditViewModel" app/src/main/java/dev/retza/mak/MainActivity.kt app/src/main/java/dev/retza/mak/ui/MakNavHostApp.kt` nic nie znajduje; w `ClassOccurrenceRoutes.kt` nie ma `rememberSaveable`. Commit: `chore: scope occurrence and class form view models to their destinations (I-86)`.

## 2. Pobieraj ViewModele kreatora i kierunków w ich celach nawigacji (I-86)

Stan: zrobione. Stan otwarcia formularza kierunku jest w pamięci ViewModelu, bez `SavedStateHandle`: ViewModel przetrwa odtworzenie aktywności, a szkicu po zakończeniu procesu `ARCHITECTURE.md` nie wymaga. Usunięte testy: `SetupViewModelTest.startDuringSaveResetsTheWizardWithoutFeedback` (ponowny start w jednym ViewModelu już nie istnieje), `StudyProgramsViewModelTest.explicitOpenDiscardsAbandonedDraft` i `restoringRouteAfterExplicitOpenKeepsDraft` (`openEdit` usunięte). `KoinGraphTest` rozwiązuje `SetupViewModel` z `SavedStateHandle`. Ręcznie: edycja kierunku w ustawieniach (szkic po odtworzeniu aktywności, zapis), edycja kierunku w semestrze (zapis), otwarcie kreatora. Ekran „Kierunki” semestru po zapisie nazwy pokazuje starą nazwę, bo `SemesterViewModel` wczytuje dane raz; tak było przed zmianą, poprawia to krok 3.

Cel: `SetupViewModel` i `StudyProgramsViewModel` powstają w swoich celach nawigacji (C1). Kreator otwiera się raz na swój ViewModel, więc znikają token sesji i flaga `opened` (D4). Formularz kierunku w ustawieniach i w semestrze dostaje własny ViewModel zamiast wspólnego z listą.

Pliki: `MainActivity.kt`, `ui/MakNavHostApp.kt`, `ui/SetupRoutes.kt`, `ui/StudyProgramRoutes.kt`, `ui/SemesterRoutes.kt` (tylko `SemesterCourses` i `SemesterCourseEdit`), `ui/setup/SetupViewModel.kt`, `ui/programs/StudyProgramsViewModel.kt`; testy `ui/setup/SetupViewModelTest.kt`, `ui/programs/StudyProgramsViewModelTest.kt`.

Zmiany:

1. `SetupViewModel`:
   - Dodaj parametr konstruktora `savedState: SavedStateHandle` (ostatni) i pole `private var opened = false`.
   - Dodaj `suspend fun openOnce() { if (opened) return; opened = true; startFromRoom(savedState.get<Boolean>("resume") ?: false) }`.
   - Usuń `sessionToken`, `saveJob` (jeśli służy tylko do anulowania przy nowym starcie) i każdy argument `isCurrent = { token == sessionToken }` oraz warunki `if (token != sessionToken) ...` i `if (token == sessionToken)`. W `onFinish` zostaje samo `state.update { it.copy(isSaving = false) }`. Sesja kreatora trwa tyle co ViewModel, więc nie ma czego unieważniać.
   - Publiczne `fun start(resume)` usuń, jeśli po zmianie wołają je tylko testy; testy przejdą na `startFromRoom` albo `openOnce`.
2. `SetupRoutes.kt`, `setupRoute`: usuń parametr `setupViewModel`, dodaj `navController: NavController`. W `composable`: `val setupViewModel: SetupViewModel = koinViewModel()`; po `if (!editing.ready) { ... }` zastąp `opened` i `LaunchedEffect` przez `LaunchedEffect(Unit) { setupViewModel.openOnce() }` i dodaj `SetupEffects(setupViewModel, navController)`. Ekran oczekiwania przed `openOnce` nie jest potrzebny, bo `SetupWizard` pokazuje stan domyślny, który `openOnce` zaraz podmienia; jeśli zrzut pokaże mignięcie pustego kroku, zostaw `PlanEditingWait()` do chwili, gdy `setup.value` ma ustawione `programOptions` (sprawdź pole w `SetupWizardUiState`).
3. `StudyProgramsViewModel`: usuń `openEdit`, `openEditIfNeeded` i `loadEditor`, jeśli po zmianach z punktu 5 nic ich nie woła (`rg -n "openEdit\b|openEditIfNeeded|loadEditor" app/src`). Zostaje `openEditFromRoom`.
4. `StudyProgramRoutes.kt`:
   - Usuń parametr `studyProgramsViewModel`. W `composable(MakRoutes.StudyPrograms)` i `composable(MakRoutes.StudyProgramEdit)` pierwsza linia `val studyProgramsViewModel: StudyProgramsViewModel = koinViewModel()`.
   - W formularzu kierunku stan otwarcia trzymaj w ViewModelu tym samym wzorem co `ClassEditOpenState` z kroku 1 (`StudyProgramEditOpenState`, `openOnce(id)` zapisane w `SavedStateHandle`, `programId` z argumentu trasy). Usuń `opened` i `openSucceeded`.
   - W formularzu dodaj `StudyProgramEffects(studyProgramsViewModel, navController)`.
5. `SemesterRoutes.kt`:
   - `SemesterCourseEdit`: `val studyProgramsViewModel: StudyProgramsViewModel = koinViewModel()` w `composable`; ładowanie programu (`studyProgramsViewModel.openEditFromRoom(programId)`) przenieś do `openOnce(programId)` z punktu 4 i usuń flagi tej trasy. Dodaj `StudyProgramEffects(studyProgramsViewModel, navController)`.
   - `SemesterCourses`, `onEditCourse`: usuń wywołanie `studyProgramsViewModel::openEdit` (formularz w nowym celu ładuje program sam).
   - Usuń parametr `studyProgramsViewModel` z `semesterRoutes`.
6. `MakNavHostApp.kt` i `MainActivity.kt`: usuń `setupViewModel`, `studyProgramsViewModel`, `SetupEffects(...)` i `StudyProgramEffects(...)` z powłoki; `setupRoute` dostaje `navController`.

Testy:

- `SetupViewModelTest`: konstrukcja z `SavedStateHandle()`; testy, które sprawdzały unieważnienie zapisu po ponownym `start`, usuń i wymień w commicie, bo ponowny start w tym samym ViewModelu nie istnieje. Nowy `openOnceStartsOnlyOnce`: dwa wywołania `openOnce()` po zmianie nazwy semestru w stanie między nimi nie czyszczą wpisanej nazwy.
- `SetupViewModelTest`, nowy `openOnceResumesFromArgument`: `SavedStateHandle(mapOf("resume" to true))` przy aktywnym semestrze bez przypisań ustawia krok `Course` (wzór danych w istniejącym teście `startFromRoom(resumeExisting = true)`).
- `StudyProgramsViewModelTest`: testy `openEdit` i `openEditIfNeeded` przepisz na `openOnce`; nowy `openOnceForMissingProgramReportsMissing`.

Przypadki brzegowe: powrót z „Dodaj zajęcia” do kreatora nie zaczyna go od nowa; kreator z „Ustawień” (`resume=false`) i z „Dzisiaj” po przerwanej konfiguracji (`resume=true`) dostają różne ViewModele; obrót ekranu w formularzu kierunku zostawia wpisaną nazwę; formularz kierunku w semestrze i w ustawieniach nie dzielą szkicu.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.setup.*" --tests "dev.retza.mak.ui.programs.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Ręcznie (przed testami Android): „Ustawienia”, „Semestry”, „Dodaj semestr” przechodzi kreator do końca; „Kierunki” w ustawieniach: odznaczenie kierunku, edycja nazwy i zapis; w semestrze „Kierunki”, edycja kierunku i zapis wraca do listy.
4. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.ui.setup.SetupWizardTest` (jeśli istnieje; sprawdź `rg -l "SetupWizard" app/src/androidTest`), `dev.retza.mak.ui.programs.StudyProgramsScreenTest` i `dev.retza.mak.ui.semester.SemesterScreenTest`.

Kryterium zakończenia: testy przechodzą; `rg "sessionToken" app/src/main/java/dev/retza/mak/ui/setup` nic nie znajduje; w `SetupRoutes.kt` i `StudyProgramRoutes.kt` nie ma `rememberSaveable`. Commit: `chore: scope setup and study program view models to their destinations (I-86)`.

## 3. Trzymaj SemesterViewModel w podgrafie semestru i zbieraj jeden strumień konfiguracji (I-86)

Cel: ekran semestru i jego podekrany dzielą jeden `SemesterViewModel` w zakresie podgrafu nawigacji (decyzja użytkownika z 2026-10-10). Stan pochodzi z jednego strumienia repozytorium `observeSemesterConfiguration(id)`, więc znikają ręczne `refresh(token)` po zapisach, tokeny sesji i pięć osobnych odczytów (E3), a pobranie planu z synchronizacji odświeża też ekran semestru.

Pliki: `data/repository/SemesterRecords.kt`, `data/repository/SemesterRepository.kt`, `data/database/Daos.kt` (tylko jeśli brakuje zapytania), `ui/MakRoutes.kt`, `ui/SemesterRoutes.kt`, `ui/MakNavHostApp.kt`, `MainActivity.kt`, `ui/semester/SemesterViewModel.kt`; testy `ui/FakeSemesterRepository.kt`, `ui/semester/SemesterViewModelTest.kt`, Android `data/RoomPersistenceTest.kt`.

Zmiany:

1. `SemesterRecords.kt`: dodaj
   ```kotlin
   /** One semester with everything its configuration screens show, read in one transaction. */
   data class SemesterConfiguration(
       val semester: SemesterRecord,
       val programs: List<SemesterProgramRecord>,
       val studyPrograms: List<StudyProgramRecord>,
       val calendars: List<AcademicCalendarRecord>,
       val overrides: List<WeekOverrideRecord>
   )
   ```
   `studyPrograms` to wszystkie globalne kierunki (do wyboru istniejącego), jak dziś w `SemesterSnapshot`.
2. `SemesterRepository`: `fun observeSemesterConfiguration(id: Long): Flow<SemesterConfiguration?>`. W `RoomSemesterRepository`: `combine(semesters.observeWithData(id), studyPrograms.observeAll())`, mapowanie encji przez `toRecord()`; `null`, gdy semestru nie ma. Sprawdź nazwę metody obserwującej wszystkie kierunki w `StudyProgramDao` (tę, której używa `observeStudyPrograms`). Pola `SemesterWithData` sprawdź w `data/database/Relations.kt` (korekty tygodni muszą tam być; jeśli nie ma, dodaj relację wzorem `academicCalendars`).
3. `FakeSemesterRepository`: implementacja przez `combine` istniejących strumieni atrapy (`observeSemester`, `observeSemesterPrograms`, `observeStudyPrograms`, `observeCalendars`, `observeWeekOverrides`), aby emitowała po zapisach tak jak one. Jeśli `observeSemester` w atrapie jest jednorazowym `flow {}`, zamień go na strumień oparty na tym samym źródle zmian co `observeSemesterPrograms`.
4. `MakRoutes.kt`: dodaj `const val SemesterGraph = "semester-graph/{semesterId}"` i `fun semesterGraphRoute(id: String): String = "semester-graph/$id"`. `openSemesterConfiguration` w `SemesterRoutes.kt` nawiguje do `semesterGraphRoute(id)` zamiast `semesterRoute(id)`.
5. `SemesterRoutes.kt`:
   - Wszystkie `composable` semestru (`Semester`, `SemesterCourses`, `SemesterCourseAdd`, `SemesterCourseEdit`, `SemesterOverrides`, `SemesterCalendars`) włóż do `navigation(route = MakRoutes.SemesterGraph, startDestination = MakRoutes.Semester, arguments = listOf(navArgument("semesterId") { type = NavType.StringType })) { ... }`. Cel startowy dostaje argument grafu.
   - Dodaj pomocnik:
     ```kotlin
     /** The semester screens share one view model kept by the semester graph entry. */
     @Composable
     private fun semesterViewModel(navController: NavController, entry: NavBackStackEntry): SemesterViewModel {
         val graphEntry = remember(entry) { navController.getBackStackEntry(MakRoutes.SemesterGraph) }
         return koinViewModel(viewModelStoreOwner = graphEntry)
     }
     ```
     i w każdym `composable` semestru pierwszą linią `val semesterViewModel = semesterViewModel(navController, entry)`. Usuń parametr `semesterViewModel` z `semesterRoutes`. W każdym `composable` dodaj `SemesterEffects(semesterViewModel, navController)`.
   - Usuń wywołania `semesterViewModel.open(...)`, `openIfNeeded(...)`, flagi `opened` w trasie `Semester` i ich `LaunchedEffect`. Wywołania `refreshForEdit(semesterId)` zmień na `refreshForEdit()` (punkt 6). Flagi `opened`/`openSucceeded` w `SemesterCourseAdd` i `SemesterCourseEdit` zastąp stanem otwarcia w ViewModelu wzorem z kroku 1 (`openCourseFormOnce()` zwraca wynik `refreshForEdit()`, zapamiętany w polu ViewModelu na czas życia celu nawigacji; wystarczy zwykłe pole, bo ViewModel przetrwa obrót).
6. `SemesterViewModel`:
   - Dodaj parametr konstruktora `savedState: SavedStateHandle` (ostatni). `private val semesterId: Long? = savedState.get<String>("semesterId")?.toLongOrNull()`; `semesterIdState` ustaw na tę wartość od razu.
   - Zastąp `open`, `openIfNeeded`, `openJob`, `sessionToken`, `isCurrentSession`, `refresh(token)` i `loadSnapshot` zbieraniem w `init`:
     ```kotlin
     init {
         val id = semesterId
         if (id != null) {
             viewModelScope.launch {
                 semesterRepository.observeSemesterConfiguration(id).collect { configuration ->
                     if (configuration == null) {
                         state.value = SemesterScreenUiState()
                     } else {
                         apply(configuration)
                     }
                 }
             }
         }
     }
     ```
     `apply(configuration)` to dzisiejsze ciało `refresh(token)` bez tokenów: wybór kalendarza, zachowanie szkicu kierunku (`courseNameDraft`, `courseColorDraft`, `courseCalendarMode`, `courseCalendarId`, `courseProgramMode`, `courseProgramId`) i formularza korekty (`overrideForm`). Ostatnią konfigurację trzymaj w `private var configuration: SemesterConfiguration?`.
   - Typ prywatny `SemesterSnapshot` zastąp `SemesterConfiguration` (te same pola), a jego funkcje rozszerzające (`sharedCalendar`, `toSemesterScreenState`) przenieś na `SemesterConfiguration` jako `private fun`.
   - `refreshForEdit(id: String)` zmień na `suspend fun refreshForEdit(): Boolean`: `semesterRepository.observeSemesterConfiguration(semesterId ?: return false).first()`, potem `apply(it)`; `null` czyści stan i zwraca `false`.
   - `selectCalendar(calendarId)`: zamiast `viewModelScope.launch { refresh(token) }` ustaw `selectedCalendarId` i wywołaj `apply(configuration ?: return)`.
   - Po każdym zapisie usuń `refresh(token)` i warunki `isCurrentSession(token)`; argumenty `isCurrent` w `launchUiOperation` usuń. Strumień sam przyniesie nowe dane.
7. `MakNavHostApp.kt` i `MainActivity.kt`: usuń `semesterViewModel` i `SemesterEffects(...)` z powłoki.
8. `titleForRoute` w `MakRoutes.kt` dalej rozpoznaje trasy celów (`MakRoutes.Semester` i podekrany), bo `currentRoute` to trasa celu, a nie grafu; nic nie zmieniaj, tylko sprawdź tytuł ręcznie.

Testy:

- `SemesterViewModelTest`: pomocnik tworzy ViewModel z `SavedStateHandle(mapOf("semesterId" to "1"))`. Wywołania `open("1")` i `openIfNeeded("1")` usuń (ViewModel sam zbiera dane). Testy przełączania między semestrami w jednym ViewModelu usuń i wymień w commicie. Testy, które po zapisie sprawdzały odświeżenie przez `refresh`, mają przejść bez zmiany oczekiwań dzięki strumieniowi atrapy.
- `SemesterViewModelTest`, nowy `externalChangeRefreshesTheScreen`: po otwarciu zapis do atrapy z pominięciem ViewModelu (na przykład nowa korekta tygodnia przez `repository.saveWeekOverride`), `advanceUntilIdle()`; lista korekt w stanie ma nową pozycję.
- `SemesterViewModelTest`, nowy `externalChangeKeepsTheCourseDraft`: wpisz nazwę w szkicu kierunku, zmień dane z zewnątrz; `courseNameDraft` zostaje.
- `RoomPersistenceTest` (Android), nowy `semesterConfigurationEmitsAfterAWrite`: `observeSemesterConfiguration(id)` po zapisie korekty emituje konfigurację z tą korektą.

Przypadki brzegowe: usunięcie semestru, którego konfiguracja jest otwarta, daje `null` i pusty stan, a efekt `CloseConfiguration` zamyka ekran jak dziś; powrót z podekranu do ekranu semestru zachowuje szkic formularza semestru; pobranie planu z synchronizacji przy otwartym semestrze odświeża listy (wcześniej nie odświeżało); otwarcie innego semestru tworzy nowy graf i nowy ViewModel.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.semester.*" --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Ręcznie (przed testami Android): „Ustawienia”, „Semestry”, „Konfiguruj”: zmiana nazwy semestru, „Kierunki” (dodanie kierunku z osobnym kalendarzem, edycja, usunięcie), „Korekty tygodni” (dodanie i usunięcie), „Kalendarze”; tytuły pasków jak przed zmianą; powrót z podekranu nie gubi niezapisanej nazwy semestru.
4. Na emulatorze klasy `dev.retza.mak.data.RoomPersistenceTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.di.KoinGraphTest` i `dev.retza.mak.ui.PlanEditingTest`.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "sessionToken|isCurrentSession|loadSnapshot|fun open" app/src/main/java/dev/retza/mak/ui/semester/SemesterViewModel.kt` nic nie znajduje; w `SemesterRoutes.kt` nie ma `rememberSaveable`. Commit: `chore: keep the semester view model in its navigation graph (I-86)`.

## 4. Steruj dopuszczaniem edycji z ViewModelu (I-86)

Cel: dopuszczanie edycji wobec synchronizacji (`PlanEditTracker`) trzyma ViewModel, a nie kompozycja trasy (D2). ViewModel zajmuje klucz przy pierwszej akcji tworzącej szkic, raz odświeża dane bazowe i zwalnia klucz, gdy szkic znika. Trasy przekazują zwykłe funkcje ViewModelu, więc nowe pole nie może pominąć dopuszczenia. Zachowanie widoczne dla użytkownika się nie zmienia.

Pliki: nowy `ui/PlanEditSession.kt`, `ui/PlanEditing.kt` (usunąć `TrackPlanEditing` i `PlanEditingController`, zostaje `PlanEditingWait`), ViewModele `ui/occurrence/OccurrenceViewModel.kt`, `ui/edit/ClassEditViewModel.kt`, `ui/semester/SemesterViewModel.kt`, `ui/setup/SetupViewModel.kt`, `ui/programs/StudyProgramsViewModel.kt`, `ui/settings/SettingsViewModel.kt`, trasy `ui/ClassOccurrenceRoutes.kt`, `ui/SemesterRoutes.kt`, `ui/SettingsRoutes.kt`, `ui/SetupRoutes.kt`, `ui/StudyProgramRoutes.kt`, `ui/MakNavHostApp.kt`, `MainActivity.kt` (parametr `planEditTracker` przestaje być potrzebny w trasach); testy: nowy `ui/PlanEditSessionTest.kt` (JVM), Android `ui/PlanEditingTest.kt` (przepisać albo usunąć), testy ViewModeli zmienionych klas.

Zmiany:

1. Nowy `ui/PlanEditSession.kt`:
   ```kotlin
   /**
    * Edit admission held by a view model (`ARCHITECTURE.md`, section 7). A sync replacement waits while the key
    * is held. The first action that starts a draft reloads the base data after admission; the key is released
    * when [hasDraft] turns false and when the view model is cleared.
    */
   class PlanEditSession(
       private val tracker: PlanEditTracker,
       private val scope: CoroutineScope,
       private val refreshBase: suspend () -> Boolean,
       private val hasDraft: () -> Boolean
   ) {
       private val key = UUID.randomUUID().toString()
       private val mutex = Mutex()
       private var admitted = false

       /** Admits a screen that is a draft from its start and reloads its base; false when the base is gone. */
       suspend fun admitForm(): Boolean = mutex.withLock {
           tracker.beginEdit(key)
           admitted = true
           refreshBase().also { loaded -> if (!loaded) release() }
       }

       /** Runs an action that may start a draft or a confirmation. */
       fun run(action: suspend () -> Unit) {
           scope.launch {
               mutex.withLock {
                   tracker.beginEdit(key)
                   val first = !admitted
                   admitted = true
                   try {
                       if (first && !refreshBase()) {
                           release()
                           return@withLock
                       }
                       action()
                   } finally {
                       if (currentCoroutineContext().isActive && admitted && !hasDraft()) release()
                   }
               }
           }
       }

       /** Releases the key once the draft is gone, for example after a save that finished later. */
       suspend fun releaseIfNoDraft() = mutex.withLock { if (admitted && !hasDraft()) release() }

       fun close() {
           if (admitted) tracker.release(key)
       }

       private suspend fun release() {
           tracker.endEdit(key)
           admitted = false
       }
   }
   ```
2. W każdym ViewModelu z listy plików:
   - dodaj parametr konstruktora `planEditTracker: PlanEditTracker` (zależność z grafu Koin, bez wartości domyślnej, `STACK.md`, sekcja o Koin);
   - utwórz `private val editSession = PlanEditSession(planEditTracker, viewModelScope, refreshBase = { ... }, hasDraft = { ... })`, gdzie `refreshBase` to dzisiejsza funkcja odświeżenia przed edycją (`refreshForEdit` dla szczegółów i semestru, `canRequestSemesterDeletion` dla usuwania semestru w ustawieniach, `{ true }` dla formularzy, które same czytają bazę w `openOnce`), a `hasDraft` to dzisiejszy warunek `active` z trasy (`details.value.hasPlanDraft()`, `semester.value.hasPlanDraft() || courseFormOpen`, `settings.value.semesterToDeleteId != null`, `{ true }` dla formularzy zawsze aktywnych);
   - w `init` zbieraj `state.map { hasDraft() }.distinctUntilChanged()` i przy `false` wołaj `editSession.releaseIfNoDraft()`;
   - w `onCleared()` wołaj `editSession.close()`;
   - każdą publiczną akcję, która w trasie była opakowana w `editing.callback(...)` albo `editing.run(...)`, opakuj w ViewModelu w `editSession.run { ... }`; akcje bez opakowania zostają bez zmian;
   - formularze zawsze aktywne (`ClassEditViewModel`, `SetupViewModel`, formularz kierunku w `StudyProgramsViewModel`, `SemesterCourseAdd` i `SemesterCourseEdit` w `SemesterViewModel`) wołają `editSession.admitForm()` na początku `openOnce()` (albo `openCourseFormOnce()`) i kończą z `Missing`, gdy zwróci `false`.
3. `SemesterViewModel`: pole `private var courseFormOpen = false`; `openCourseFormOnce()` ustawia je na `true` po udanym `admitForm()`; anulowanie i udany zapis formularza kierunku (`resetCourseDraft`, efekt `CourseAdded`, zapis edycji) ustawiają `false` i wołają `editSession.releaseIfNoDraft()`. Zastępuje to dzisiejsze `holdIfInactive = true` w `SemesterCourses`.
4. Trasy: usuń `TrackPlanEditing(...)`, `editing.ready`, `editing.callback(...)`, `editing.run(...)` i parametr `planEditTracker`; przekazuj referencje funkcji ViewModelu (`occurrenceViewModel::cancelOccurrence`, `{ value -> semesterViewModel.updateCourseName(value) }` i podobne). Ekrany formularzy pokazują `PlanEditingWait()`, dopóki stan otwarcia z kroków 1 do 3 nie jest `Open`.
5. Usuń `TrackPlanEditing` i `PlanEditingController` z `ui/PlanEditing.kt`. Z powłoki i `MainActivity` usuń `planEditTracker`, jeśli nic poza trasami go nie używało.
6. `docs/ARCHITECTURE.md`, sekcja 7: znajdź opis dopuszczania edycji (`rg -n "TrackPlanEditing|PlanEditTracker|dopuszcz" docs/ARCHITECTURE.md docs/SYNC.md`) i zastąp nazwę `TrackPlanEditing` opisem: „ViewModel ekranu z edycją planu trzyma `PlanEditSession`: zajmuje klucz w `PlanEditTracker` przy pierwszej akcji tworzącej szkic, raz odświeża dane bazowe i zwalnia klucz, gdy szkic znika albo ViewModel jest czyszczony.”

Testy:

- `PlanEditSessionTest` (nowy, JVM), z `PlanEditTracker(backgroundScope)` i atrapami funkcji:
  - `firstActionRefreshesOnce`: dwie akcje `run` wołają `refreshBase` raz;
  - `failedRefreshSkipsTheActionAndReleases`: `refreshBase` zwraca `false`; akcja się nie wykonuje, `tracker.isEditing` jest fałszem;
  - `keyStaysWhileDraftExists`: `hasDraft` zwraca `true` po akcji; `tracker.isEditing` jest prawdą; po zmianie na `false` i `releaseIfNoDraft()` jest fałszem;
  - `replacementWaitsForAnAdmittedForm`: po `admitForm()` `tracker.withReplacement { }` zwraca `PlanReplacementResult.Editing`.
- `PlanEditingTest` (Android): przypadki, które sprawdzały kompozycyjny `TrackPlanEditing`, przenieś do `PlanEditSessionTest`; klasę Android usuń, jeśli nic w niej nie zostanie, i wymień to w commicie.
- Testy ViewModeli zmienionych klas przekazują `PlanEditTracker(backgroundScope)`; istniejące oczekiwania się nie zmieniają. W `OccurrenceViewModelTest` dodaj `noteDraftHoldsTheKeyUntilSaved`: wpisanie notatki zajmuje klucz, zapis notatki go zwalnia.

Przypadki brzegowe: synchronizacja czeka, gdy formularz zajęć jest otwarty (klucz zajęty od `openOnce`); po obrocie ekranu klucz zostaje, bo ViewModel trwa; zamknięcie celu nawigacji zwalnia klucz od razu przez `onCleared` (okres karencji `PlanEditTracker.release` dalej obejmuje odtworzenie aktywności); nieudane odświeżenie przed edycją szczegółów pokazuje „Nie znaleziono tych zajęć.” jak dziś.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*" --tests "dev.retza.mak.sync.PlanEditTrackerTest"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Ręcznie (przed testami Android): w szczegółach wpisanie notatki i zapis, odwołanie i przywrócenie terminu, zmiana sali; formularz zajęć zapis i anulowanie; kreator do końca; semestr: dodanie kierunku z listy, edycja i usunięcie kierunku, korekta tygodnia; usunięcie semestru w ustawieniach z potwierdzeniem i anulowaniem.
4. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.sync.RoomPlanSyncGatewayTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.occurrence.OccurrenceDetailsScreenTest` i `dev.retza.mak.ui.edit.ClassEditScreenTest`.

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "TrackPlanEditing|PlanEditingController|refreshBeforeFirstEdit|holdIfInactive" app/src` nic nie znajduje. Commit: `chore: hold plan edit admission in view models (I-86)`.

## 5. Licz siatkę kalendarza „Planu” tylko w widoku kalendarza (I-86)

Cel: zmiana dnia, tygodnia albo filtra w widoku listy nie przelicza 42 dni kalendarza, a siatka liczy się ponownie tylko po zmianie planu, miesiąca, filtra albo dnia dzisiejszego (B2). Zaznaczenie dnia w kalendarzu nie przelicza siatki. Wygląd się nie zmienia. Po tym kroku I-86 jest gotowe.

Pliki: `ui/schedule/ScheduleViewModel.kt`; testy `ui/schedule/ScheduleViewModelTest.kt`; dokumenty `docs/ARCHITECTURE.md`, `docs/QUEUE.md`.

Zmiany:

1. W `ScheduleViewModel` dodaj prywatną funkcję `matchesFilter(occurrence: PlannedOccurrence, activeFilter: String): Boolean = activeFilter == "all" || occurrence.classItem.semesterProgramId == activeFilter` i użyj jej we wszystkich czterech miejscach z tym warunkiem (`buildSchedule` i `cancelledItems`).
2. Wydziel z `buildSchedule` budowę siatki do `private fun calendarGrid(visible: VisiblePlanData, month: YearMonth, activeFilter: String, today: LocalDate): List<CalendarDayUi>` z `isSelected = false` dla każdego dnia. Wyliczenie `activeFilter` (sprawdzenie, czy wybrany kierunek istnieje) wydziel do `private fun activeFilter(inputs: ActivePlanInputs, requested: String): String`.
3. Dodaj strumień siatki:
   ```kotlin
   private data class GridKey(val month: YearMonth, val filter: String, val today: LocalDate, val view: ScheduleView)

   // The grid resolves 42 days, so it is rebuilt only when what it shows changes, and only in the calendar view.
   private val calendarGrid: StateFlow<List<CalendarDayUi>> = combine(
       activePlan,
       controls.map { GridKey(it.calendarMonth, it.courseFilterId, it.today, it.scheduleView) }.distinctUntilChanged()
   ) { inputs, key ->
       if (inputs == null || key.view != ScheduleView.Calendar) {
           emptyList()
       } else {
           val visible = inputs.data.visibleTo(inputs.display)
           calendarGrid(visible, key.month, activeFilter(inputs, key.filter), key.today)
       }
   }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
   ```
   Sprawdź nazwę wartości widoku kalendarza w `ScheduleView` (`rg -n "enum class ScheduleView" app/src`).
4. `schedule` łączy trzy strumienie: `combine(activePlan, controls, calendarGrid) { inputs, control, grid -> buildSchedule(inputs, control, grid) }`. W `buildSchedule` zamiast budowy siatki: `calendarDays = grid.map { it.copy(isSelected = it.id == control.calendarDate.toString()) }`.
5. W `buildSchedule` dzień kalendarza (`calendarPlan`, `calendarFiltered`, etykiety kolizji kalendarza, `calendarItems`) licz tylko w widoku kalendarza; w widoku listy `calendarItems = emptyList()`, `calendarSelectedDayCountLabel = classCountLabel(0)`. Pozostałe pola bez zmian.
6. `docs/ARCHITECTURE.md`, sekcja 5: w zdaniu o zakresie ViewModeli dopisanym w poprzednim planie usuń nawias „(I-86; na razie „Dzisiaj” i „Plan”)”.
7. `docs/QUEUE.md`: I-86 przenieś do tabeli „Zakończone” ze statusem `gotowe`, listą zrealizowanych punktów audytu (C1, C2, D1, D2, D4, E3, B1, B2, B4 dla ustawień) i uruchomionych testów.

Testy:

- `ScheduleViewModelTest`: testy, które czytają `calendarDays` albo `calendarItems`, najpierw wołają `viewModel.selectView(ScheduleView.Calendar)`; oczekiwane wartości się nie zmieniają.
- `ScheduleViewModelTest`, nowy `listViewDoesNotBuildTheCalendarGrid`: w widoku listy `calendarDays` jest puste.
- `ScheduleViewModelTest`, nowy `selectingACalendarDayKeepsTheGrid`: w widoku kalendarza zapamiętaj `schedule.value.calendarDays.map { it.copy(isSelected = false) }`, wybierz inny dzień tego miesiąca; lista bez zaznaczenia jest równa poprzedniej, a zaznaczony jest nowy dzień.
- `ScheduleViewModelTest`, nowy `gridFollowsPlanChanges`: w widoku kalendarza dodanie zajęć w atrapie zmienia znaczniki dnia tych zajęć.

Przypadki brzegowe: przełączenie z listy do kalendarza pokazuje siatkę od razu po pierwszym przeliczeniu (stan początkowy `emptyList()` trwa jedną emisję); zmiana filtra kierunku przelicza siatkę; po północy `refreshToday()` zmienia `today` i przelicza siatkę.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.schedule.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzuty przy 320 dp przed zmianą i po niej (przed testami Android): kalendarz „Planu” w bieżącym miesiącu i w następnym, z filtrem kierunku i bez; lista „Planu”. Mają być identyczne.
4. Na emulatorze klasa `dev.retza.mak.ui.schedule.ScheduleScreenTest`.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne, `py scripts/check_map.py` zwraca 0; w `MainActivity` są tylko `AppViewModel`, `SettingsViewModel`, `UpdateViewModel` i `SyncViewModel`. Commit: `chore: build the calendar grid only in the calendar view (I-86)`.
