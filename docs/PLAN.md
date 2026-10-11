# Plan najbliższych prac

Cel: poprawki ustawień zgłoszone przez użytkownika 2026-10-11. Dodawanie kierunku po kreatorze jest zbyt schowane (I-95), karta semestru jest nieczytelna i nie pokazuje roku (I-96), a podsumowanie powiadomień pokazuje to samo bez względu na włączone rodzaje (I-97). Ekran „Powiadomienia” dostaje układ, który pomieści kolejne rodzaje, i powiadomienia o okienkach (I-98). Przy okazji znikają opisy, które powtarzają to, co widać na ekranie.

## Decyzje użytkownika z 2026-10-11

1. „Dodaj kierunek” jest na ekranie „Kierunki” w ustawieniach. Formularz ma nazwę i kolor. Nowy kierunek trafia od razu do aktywnego semestru i dostaje jego wspólny kalendarz. Bez aktywnego semestru kierunek powstaje bez przypisania.
2. Ekran „Dodaj kierunek” w konfiguracji semestru zostaje (nowy albo istniejący kierunek, wspólne albo osobne daty i tygodnie). Znikają z niego dwa teksty pomocnicze: „Nazwa i kolor istniejącego kierunku są wspólne dla wszystkich semestrów.” i „Powstanie kopia dat, rytmu i korekt wybranego kalendarza.”.
3. Odrzucone: lista kierunków semestru z checkboxami zamiast formularza „Dodaj kierunek”. Obecny formularz wystarcza, a checkboxy miałyby na dwóch ekranach „Kierunki” dwa różne znaczenia.
4. Znikają opisy: „Zakres dat, pierwszy tydzień A/B, kierunki i ręczne korekty tygodni.” (ekran „Semestr”), „Ustaw oznaczenie tylko dla jednego tygodnia albo od wybranego tygodnia w przyszłość.” i „Każda korekta dotyczy poniedziałku wybranego tygodnia.” (ekran „Korekty tygodni”). Interfejs sam pokazuje, co można zrobić.
5. Karta semestru według wariantu C z artefaktu „MAK: semestr i powiadomienia”: wiersze z ikonami zamiast jednego zdania. Aktywny semestr: ikona kalendarza z zakresem dat i pierwszym tygodniem, ikona kierunku z nazwami kierunków, ikona listy z liczbą zajęć. Nieaktywny semestr: ikona kalendarza z zakresem dat. Wysokość karty nie jest ograniczeniem.
6. Daty semestru pokazują rok: „7 wrz 2026 - 14 lut 2027”, a gdy oba końce są w tym samym roku, rok stoi raz: „22 lut - 15 cze 2027”.
7. Kolejność semestrów w ustawieniach: aktywny na górze, pod nim pozostałe od najnowszego do najstarszego.
8. Ekran „Powiadomienia” zawsze pokazuje wszystkie rodzaje powiadomień. Główny przełącznik włącza albo wyłącza wszystkie naraz: jest włączony, gdy włączony jest co najmniej jeden rodzaj, i wyłączony, gdy żaden.

9. Podsumowanie powiadomień na głównym ekranie ustawień pokazuje liczbę włączonych rodzajów: „Wszystkie włączone”, „Włączone: 1 z 2” albo „Wyłączone”. Godzina i wyprzedzenie są tylko na ekranie „Powiadomienia”. Odrzucone: warianty A i C z drugiego rzędu artefaktu, bo przy kolejnych rodzajach stają się nieczytelne.
10. Wiersz „Powiadomienia o kolizjach” na głównym ekranie ustawień nazywa się „Powiadomienia”, bo obejmie też inne rodzaje.
11. Ekran „Powiadomienia” w wariancie C z artefaktu: przełącznik „Wszystkie powiadomienia”, sekcje tematów („Kolizje”, „Okienka”), w nich wiersze rodzajów z przełącznikiem i wartością pod nazwą, która otwiera dialog wyboru. Odrzucone: wariant A (pola wyboru pod każdym rodzajem) i B (grupowanie według pory ze wspólną godziną i wyprzedzeniem).
12. Nowe powiadomienia o okienkach: „Dzień wcześniej” i „Przed zajęciami”, jak przy kolizjach, z własną godziną i wyprzedzeniem.
13. Powiadomienie o okienku przed zajęciami przychodzi przed zajęciami, po których zaczyna się okienko, na przykład „Po Analizie masz okienko 10:30-12:00”.
14. Plan wykonuje się na nowej gałęzi od `main` po scaleniu gałęzi I-79.

Bez zmian: kreator i ekran „Edytuj kierunek” w semestrze.

## Zasady dla wykonawcy

Przeczytaj tę sekcję przed każdym krokiem.

- Kroki wykonuj na nowej gałęzi `task/settings-and-gap-notifications` od aktualnego `origin/main` (decyzja 14). Przed pierwszym krokiem: `git fetch origin` i sprawdź, że `origin/main` zawiera scaloną gałąź I-79 (`git log --oneline origin/main -5`, commit z I-79 i tym planem). Jeśli nie zawiera, zatrzymaj się i zapytaj użytkownika. Potem `git switch -c task/settings-and-gap-notifications origin/main`; przy kolejnych krokach `git switch task/settings-and-gap-notifications`.
- Ścieżki kodu są względne wobec `app/src/main/java/dev/retza/mak/`, testów JVM wobec `app/src/test/java/dev/retza/mak/`, testów Android wobec `app/src/androidTest/java/dev/retza/mak/`.
- Przed zmianą otwórz każdy plik z listy kroku i znajdź opisany fragment po nazwie funkcji, nie po numerze wiersza. Jeśli fragmentu nie ma albo wygląda inaczej niż w opisie, zrób zmianę zgodną z celem kroku i opisz różnicę w commicie. Zatrzymaj się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Polecenia uruchamiaj w katalogu repozytorium w PowerShellu. Testy JVM jednej klasy: `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.semester.SemesterViewModelTest"`. Kilka klas: powtórz `--tests` dla każdej. Jeśli Gradle zgłosi `No cached version ... available for offline mode`, uruchom to samo polecenie raz bez `--offline`, potem wróć do `--offline`.
- Po każdej zmianie kodu: `.\gradlew.bat --offline :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin`. Kompilacja testów Android nie oznacza ich uruchomienia.
- Testy Android: najwyżej jeden emulator naraz, z 2048 MiB RAM. Obraz `Medium_Phone_A12` (`STACK.md`, sekcja 6); na komputerze, który go nie ma, `Medium_Phone` (testy używają aplikacji `dev.retza.mak.debug`, a dane odbioru synchronizacji są w `dev.retza.mak`; nie czyść ich). Jedna klasa na wywołanie: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.di.KoinGraphTest"`; lista klas po przecinku w PowerShellu uruchamia tylko pierwszą. Przed testami Android cofnij zmiany ekranu ze zrzutów (`adb shell wm size reset`). Testy Android przeinstalowują aplikację debug i zerują jej dane, więc ręczne sprawdzenie i zrzuty rób po `installDebug`, przed testami Android.
- Zrzuty przy 320 dp: procedura w `STACK.md`, sekcja 6. Zrzuty zapisuj poza repozytorium.
- Nie uruchamiaj pełnego zestawu testów aplikacji bez polecenia użytkownika.
- Każdy krok kończy się jednym commitem z podanym tematem, bez stopki `Co-authored-by`. Do commita dołącz zmiany `PLAN.md`, `QUEUE.md` i dokumentów, których dotyczy krok. Pod nagłówkiem zrobionego kroku dopisz linię `Stan: zrobione.` z krótkim opisem różnic wobec planu.
- Nie wypychaj gałęzi, nie otwieraj pull requesta i nie scalaj bez wyraźnego polecenia użytkownika.
- Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py`; wymagany kod 0.
- Teksty interfejsu i dokumentacja po polsku, identyfikatory i komentarze w kodzie po angielsku. Bez znaków `·`, `—` i strzałek.
- Nie używaj aliasów importu (`import x.Y as Z`).

## 1. Dodaj „Dodaj kierunek” w ustawieniach i usuń zbędne opisy (I-95)

Cel: z ekranu „Kierunki” w ustawieniach można utworzyć kierunek z nazwą i kolorem. Nowy kierunek trafia do aktywnego semestru ze wspólnym kalendarzem, więc od razu można dodać do niego zajęcia. Przy okazji formularz „Dodaj kierunek” w semestrze traci dwa zbędne teksty pomocnicze (decyzja 2).

Pliki: `ui/programs/StudyProgramsScreen.kt`, `ui/programs/StudyProgramsViewModel.kt`, `ui/StudyProgramRoutes.kt`, `ui/MakRoutes.kt`, `ui/semester/SemesterCourseScreens.kt` (`SemesterCourseAddScreen`), `ui/semester/SemesterScreen.kt`; testy JVM `ui/programs/StudyProgramsViewModelTest.kt`; testy Android `ui/programs/StudyProgramsScreenTest.kt`, `ui/semester/SemesterScreenTest.kt`; `docs/FEATURES.md`, `docs/LOG.md`, `docs/log_archive/`, `docs/QUEUE.md`.

Zmiany:

1. Zanim zmienisz kod, zrób zrzuty przy 320 dp: ekran „Kierunki” w ustawieniach z danymi demo, ekran „Edytuj kierunek” ekran „Dodaj kierunek” w semestrze z wybranym istniejącym kierunkiem i osobnymi datami, ekran „Semestr” i „Korekty tygodni”.
2. `MakRoutes`: dodaj `const val StudyProgramAdd = "settings/program-new"` (osobna ścieżka, bo `settings/programs/{programId}` ma argument typu `Long`) i tytuł paska `MakRoutes.StudyProgramAdd -> "Dodaj kierunek"` obok `StudyProgramEdit`.
3. `StudyProgramsScreen`: dodaj parametr `onAddProgram: () -> Unit`. Pod `MakScreenIntro` dodaj `MakSecondaryAction(text = "Dodaj kierunek", icon = Icons.Outlined.Add, onClick = onAddProgram)`, tak samo jak w `CoursesBlock` w `ui/semester/SemesterScreen.kt`. Przycisk jest widoczny także przy pustej liście. Tekst pustego stanu zmień na „Nie masz jeszcze kierunków.”.
4. `StudyProgramsViewModel`:
   - `StudyProgramEditorUi` dostaje pole `activeSemesterName: String? = null`;
   - nowa funkcja `suspend fun openNewOnce()`: działa jak `openOnce`, ale zamiast `loadEditorFromRoom` ustawia `editor = StudyProgramEditorUi(id = null, color = suggestedCourseColor(<kolory wszystkich kierunków z observeStudyPrograms().first()>), activeSemesterName = <nazwa z semesterRepository.observeActiveSemester().first()>)` i zwraca `Open`;
   - `save()`: gdy `editor.id == null`, po walidacji nazwy (jak dziś) wywołaj nową prywatną funkcję `createProgram(name, color)`. Ta funkcja pobiera aktywny semestr (`observeActiveSemester().first()`) i jego wspólny kalendarz (`observeCalendars(semester.id).first().minByOrNull { it.id }`, ta sama reguła co `sharedCalendar()` w `SemesterViewModel`). Gdy oba istnieją, woła `semesterRepository.saveStudyProgramAssignment(semesterId, StudyProgramRecord(id = 0L, name, color), calendarId)` i publikuje „Dodano kierunek do semestru {nazwa semestru}”. W przeciwnym razie woła `saveStudyProgram(StudyProgramRecord(id = 0L, name, color))` i publikuje „Dodano kierunek”. Błąd: „Nie udało się dodać kierunku.”, formularz zostaje z wpisanymi danymi. Po sukcesie `CloseEditor` jak przy edycji.
5. `StudyProgramEditScreen`: gdy `editor.id == null`, nie pokazuj sekcji usuwania, akcja zapisu ma tekst „Dodaj kierunek”, a wstęp brzmi „Kierunek trafi do semestru {activeSemesterName}. Nazwa i kolor są wspólne dla wszystkich semestrów.”; bez aktywnego semestru „Kierunek pojawi się w planie po dodaniu go w ustawieniach semestru.”. Przy edycji wszystko zostaje jak dziś.
6. Komunikat przy kierunku przypisanym do semestrów (sekcja usuwania): „Kierunek jest używany w semestrach: {nazwy}. Aby go usunąć, najpierw usuń go z tych semestrów na ekranie Kierunki semestru.” zostaje bez zmian.
7. `StudyProgramRoutes`: nowy cel `composable(MakRoutes.StudyProgramAdd)` z tym samym przebiegiem co `StudyProgramEdit` (`openNewOnce` zamiast `openOnce`, `PlanEditingWait`, `StudyProgramEffects`). `StudyProgramEffects` zamyka ekran także dla `MakRoutes.StudyProgramAdd`. Ekran listy dostaje `onAddProgram = { navController.navigate(MakRoutes.StudyProgramAdd) }`.
8. `SemesterCourseAddScreen`: usuń `MakHelperText("Nazwa i kolor istniejącego kierunku są wspólne dla wszystkich semestrów.")` i `MakHelperText("Powstanie kopia dat, rytmu i korekt wybranego kalendarza.")` razem z warunkiem `if`, który obejmuje tylko ten drugi tekst. Usuń import `MakHelperText`, jeśli nie ma innego użycia w pliku.
9. `ui/semester/SemesterScreen.kt`: usuń `MakScreenIntro("Zakres dat, pierwszy tydzień A/B, kierunki i ręczne korekty tygodni.")`, `MakScreenIntro("Ustaw oznaczenie tylko dla jednego tygodnia albo od wybranego tygodnia w przyszłość.")` i `MakHelperText("Każda korekta dotyczy poniedziałku wybranego tygodnia.")` (decyzja 4). Tekst „Korekta obejmuje tydzień od poniedziałku {data}.” pod polem daty w formularzu korekty zostaje, bo pokazuje konkretną datę. Usuń nieużywane importy.
10. `FEATURES.md`, sekcja „Ustawienia i dane”, akapit o ekranie „Kierunki” w ustawieniach: dopisz przycisk „Dodaj kierunek”, formularz, przypisanie do aktywnego semestru ze wspólnym kalendarzem i teksty z punktów 3 do 6. W akapicie o kolorze kierunku zamień „na ekranach dodawania i edycji kierunku w semestrze oraz w edycji kierunku w ustawieniach” na „przy dodawaniu i edycji kierunku w ustawieniach”. W sekcji kreatora ostatnie zdanie akapitu o „Dodaj kolejny kierunek” zmień na „Później kierunki dodaje się na ekranie „Kierunki” w ustawieniach albo w ustawieniach semestru.”. `LOG.md`: nowy wpis na górze „2026-10-11: Dodawanie kierunków na ekranie „Kierunki” (I-95)” z decyzjami użytkownika 1 do 4 i 14 z początku tego planu; log ma 20 wpisów, więc najstarszy przenieś do pliku w `docs/log_archive/` według wzoru plików, które tam są. `QUEUE.md`: I-95 `odbiór otwarty` z listą zrobionego i tym, co zostaje do sprawdzenia na telefonie użytkownika.

Testy:

- `StudyProgramsViewModelTest.newProgramIsAssignedToSharedCalendarOfActiveSemester`: aktywny semestr z dwoma kalendarzami; po `openNewOnce`, `updateName("Fizyka")` i `save()` istnieje kierunek „Fizyka” przypisany do kalendarza o najniższym identyfikatorze.
- `StudyProgramsViewModelTest.newProgramWithoutActiveSemesterIsOnlyCreated`: brak aktywnego semestru; po zapisie kierunek istnieje, a przypisań nie ma.
- `StudyProgramsViewModelTest.newProgramRejectsBlankName`: pusta nazwa daje `FormErrorTexts.ProgramNameRequired` i nic nie zapisuje.
- `StudyProgramsViewModelTest.newProgramSuggestsColorAwayFromExistingPrograms`: kolor w edytorze równa się `suggestedCourseColor` kolorów istniejących kierunków.
- `StudyProgramsScreenTest.addActionIsVisibleWithAndWithoutProgramsAt320Dp` i `StudyProgramsScreenTest.newProgramFormHasNoDeleteSectionAt320Dp`. Popraw `emptyListExplainsWhereProgramsComeFrom` na nowy tekst pustego stanu.
- Testy, które szukają usuniętych opisów ekranów „Semestr” i „Korekty tygodni” (`rg -n "Zakres dat, pierwszy|Ustaw oznaczenie tylko|Każda korekta dotyczy" app/src`), zmień na `assertDoesNotExist()` albo usuń te asercje.
- `SemesterScreenTest.courseAddScreenExistingProgramShowsSelectionInsteadOfNameAndColorDraft`: zamień `assertIsDisplayed()` dla tekstu „Nazwa i kolor istniejącego kierunku...” na `assertDoesNotExist()`.

Przypadki brzegowe: aktywny semestr bez kalendarza (po usunięciu wszystkich jego kierunków) daje kierunek bez przypisania i komunikat „Dodano kierunek”; obrót ekranu w formularzu zachowuje wpisane dane (edytor żyje w ViewModelu); długa nazwa semestru we wstępie się zawija; kierunek o nazwie, która już istnieje, jest dozwolony jak dziś (lista pokazuje numery).

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.programs.StudyProgramsViewModelTest"`.
2. `installDebug`, dodanie kierunku z aktywnym semestrem: kierunek widać na „Planie” w polu „Kierunek” i w formularzu zajęć. Zrzuty ekranu „Kierunki” i formularza przy 320 dp w obu motywach.
3. `installDebug`, ekrany „Dodaj kierunek” w semestrze, „Semestr” i „Korekty tygodni” przy 320 dp: bez usuniętych tekstów, odstępy między polami bez zmian poza miejscem po tekstach; porównaj ze zrzutem z punktu 1.
4. Na emulatorze klasy `dev.retza.mak.ui.programs.StudyProgramsScreenTest`, `dev.retza.mak.ui.semester.SemesterScreenTest` i `dev.retza.mak.di.KoinGraphTest`.
5. `py scripts/check_map.py` z kodem 0.

Kryterium zakończenia: z ekranu „Kierunki” w ustawieniach da się dodać kierunek, który od razu jest w aktywnym semestrze; formularz w semestrze nie ma obu tekstów, ekrany „Semestr” i „Korekty tygodni” nie mają opisów z decyzji 4; testy przechodzą. Commit: `feat: add study programs from the programs screen (I-95)`.

## 2. Pokaż kartę semestru w wierszach z ikonami i ułóż semestry od najnowszego (I-96)

Cel: karta semestru na ekranie „Semestry” pokazuje informacje w osobnych wierszach z ikonami (decyzja 5), daty z rokiem (decyzja 6), a lista ma aktywny semestr na górze i pozostałe od najnowszego (decyzja 7).

Pliki: `data/database/Daos.kt` (DAO kalendarzy), `data/repository/SemesterRepository.kt`, `ui/components/PresentationModels.kt` (`SemesterUi`), `ui/settings/SettingsViewModel.kt` (`buildSettingsState`), `ui/settings/SettingsScreen.kt` (`SemesterRow` i podsumowanie wiersza „Zarządzaj semestrami”); testy JVM `ui/settings/SettingsViewModelTest.kt`, fałszywe repozytoria w `ui/` i `data/repository/DemoDataSeederTest.kt`; testy Android `ui/settings/SettingsScreenTest.kt`, `data/RoomPersistenceTest.kt`; `docs/FEATURES.md`, `docs/LOG.md`, `docs/QUEUE.md`.

Zmiany:

1. Zanim zmienisz kod, zrób zrzuty przy 320 dp: ekran „Semestry” z danymi demo i z dwoma dodatkowymi semestrami (jeden wcześniejszy, jeden późniejszy od demonstracyjnego) oraz główny ekran ustawień.
2. Funkcja zakresu dat: w pliku z `shortDateFormatter` (znajdź przez `rg -n "val shortDateFormatter" app/src/main`) dodaj `fun dateRangeWithYearLabel(start: LocalDate, end: LocalDate): String`. Gdy `start.year == end.year`, wynik to obie daty w formacie `shortDateFormatter` i rok raz na końcu („22 lut - 15 cze 2027”); w przeciwnym razie rok stoi po każdej dacie („7 wrz 2026 - 14 lut 2027”).
3. Dane dla wszystkich semestrów: dziś `buildSettingsState` zna kalendarz tylko aktywnego semestru. W DAO kalendarzy dodaj `@Query("SELECT * FROM academic_calendars ORDER BY id") fun observeAll(): Flow<List<AcademicCalendarEntity>>`. W `SemesterRepository` dodaj `fun observeSharedCalendars(): Flow<Map<Long, AcademicCalendarRecord>>`: dla każdego semestru kalendarz o najniższym identyfikatorze (ta sama reguła co `sharedCalendar()`), kluczem jest identyfikator semestru. Dodaj tę funkcję do fałszywych implementacji `SemesterRepository` w testach.
4. `SemesterUi`: zamiast `dateRangeLabel`, `firstWeekLabel`, `courseCountLabel` i `classCountLabel` pola:
   - `dateLabel: String`: zakres z punktu 2; przy aktywnym semestrze z dopiskiem „, pierwszy tydzień A” albo „B”; pusty, gdy semestr nie ma kalendarza;
   - `programsLabel: String`: nazwy kierunków aktywnego semestru po przecinku, z `distinctLabels`; „Brak kierunków”, gdy nie ma żadnego; pusty dla nieaktywnego;
   - `classCountLabel: String`: jak dziś; pusty dla nieaktywnego.

   Zakres dat ma teraz też nieaktywny semestr. Użycia starych pól znajdzie `rg -n "dateRangeLabel|firstWeekLabel|courseCountLabel" app/src`; wiersz „Zarządzaj semestrami” na głównym ekranie ustawień pokazuje `dateLabel` aktywnego semestru.
5. Kolejność w `buildSettingsState`: aktywny semestr pierwszy, potem pozostałe według daty początku wspólnego kalendarza malejąco, semestry bez kalendarza na końcu, przy równych datach wyższy identyfikator wyżej. Ta kolejność dotyczy listy na ekranie „Semestry” i opcji pola „Aktywny semestr”. Kolejności w DAO nie zmieniaj, bo używa jej wybór semestru zastępczego.
6. `SemesterRow`: zamiast jednego tekstu z przecinkami kolumna wierszy z odstępem 6 dp. Wiersz to ikona 18 dp w kolorze `onSurfaceVariant` i tekst `bodyMedium` w kolorze `onSurface`, odstęp 10 dp, ikona wyrównana do pierwszej linii tekstu: `Icons.Outlined.CalendarMonth` z `dateLabel`, `Icons.Outlined.School` z `programsLabel`, `Icons.Outlined.FormatListBulleted` z `classCountLabel`. Pusty tekst ukrywa swój wiersz. Ikony mają `contentDescription = null`, bo tekst wystarcza. Nazwa, „Aktywny” i akcje zostają bez zmian.
7. `FEATURES.md`, akapit o ekranie „Semestry”: zamiast „Semestr nieaktywny pokazuje samą nazwę i akcje.” opisz kartę według punktów 4 i 6, regułę roku i kolejność z punktu 5. Dopisz decyzje 5 do 7 do wpisu w `LOG.md` z kroku 1. `QUEUE.md`: I-96 `odbiór otwarty`.

Testy:

- `SettingsViewModelTest.semestersAreOrderedActiveFirstThenNewest`: trzy semestry (2025/26, aktywny 2026/27, letni 2027); kolejność 2026/27, 2027, 2025/26.
- `SettingsViewModelTest.inactiveSemesterShowsDateRangeOnly`: nieaktywny ma `dateLabel` z rokiem, puste `programsLabel` i `classCountLabel`.
- `SettingsViewModelTest.activeSemesterListsProgramNames`: `programsLabel == "Informatyka, Zarządzanie"`, a bez kierunków „Brak kierunków”.
- Test JVM funkcji z punktu 2: ten sam rok i przejście roku.
- `RoomPersistenceTest.sharedCalendarsMapLowestCalendarPerSemester`.
- `SettingsScreenTest`: popraw testy, które szukają starego tekstu karty (`rg -n "pierwszy tydzień" app/src/androidTest`), i dodaj `semesterCardShowsIconRowsAt320Dp` z długą listą kierunków, która się zawija.

Przypadki brzegowe: semestr z kilkoma kalendarzami pokazuje zakres wspólnego (najniższy identyfikator), jak dziś; długie nazwy kierunków zawijają się pod tekstem, nie pod ikoną; duża czcionka; brak aktywnego semestru (wszystkie według daty).

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.settings.SettingsViewModelTest"` i klasa testu funkcji z punktu 2.
2. `installDebug`, zrzuty z punktu 1 po zmianie w obu motywach; porównaj z wariantem C w artefakcie.
3. Na emulatorze klasy `dev.retza.mak.ui.settings.SettingsScreenTest`, `dev.retza.mak.data.RoomPersistenceTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: karta wygląda jak wariant C, daty mają rok według reguły, kolejność zgadza się z decyzją 7, testy przechodzą. Commit: `feat: show semester cards with icon rows (I-96)`.

## 3. Powiąż główny przełącznik z rodzajami powiadomień i licz włączone w podsumowaniu (I-97)

Cel: główny przełącznik włącza albo wyłącza wszystkie rodzaje naraz i jest włączony wtedy i tylko wtedy, gdy włączony jest co najmniej jeden rodzaj (decyzja 8). Znika stan, w którym główny jest włączony, żaden rodzaj nie jest, powiadomienia nie przychodzą, a ustawienia pokazują „Włączone”. Wiersz na głównym ekranie ustawień nazywa się „Powiadomienia” i pokazuje liczbę włączonych rodzajów (decyzje 9 i 10).

Pliki: `settings/SettingsPreferences.kt`, `settings/DataStoreSettingsPreferences.kt`, `ui/settings/SettingsScreen.kt` (`SettingsNotificationsScreen`, `notificationsStatus`, wiersz „Powiadomienia o kolizjach” w sekcji „Powiadomienia”), `ui/settings/SettingsViewModel.kt` (`NotificationSettingsUi`); testy JVM `settings/InMemorySettingsPreferences.kt`, `ui/settings/SettingsViewModelTest.kt`; testy Android `ui/settings/SettingsPreferencesTest.kt`, `ui/settings/SettingsScreenTest.kt`, `notifications/CollisionAlarmSchedulerTest.kt` (`EnabledNotifications`); `docs/FEATURES.md`, `docs/LOG.md`, `docs/QUEUE.md`.

Zmiany:

1. Odczyt (`toNotificationPreferences`): zapisane `enabled == false` znaczy, że wszystkie rodzaje są wyłączone, bo starsze wersje zostawiały przy wyłączonej funkcji domyślne `true` rodzajów. Licz `eveningEnabled = zapisane enabled && zapisane evening`, `beforeClassEnabled = zapisane enabled && zapisane before`, `enabled = eveningEnabled || beforeClassEnabled`. Dane ze starszych wersji nie wymagają migracji.
2. Zapis, każdy w jednym `dataStore.edit`:
   - `setCollisionNotificationsEnabled(x)`: `enabled` i wszystkie rodzaje równe `x`;
   - `setEveningNotificationsEnabled(x)`: policz pozostałe rodzaje regułą z punktu 1, zapisz `evening = x`, pozostałe rodzaje jawnie (usuwa starą wartość domyślną) i `enabled = x || którykolwiek pozostały`;
   - `setBeforeClassNotificationsEnabled(x)`: symetrycznie.

   Opisz regułę w KDoc `CollisionNotificationPreferences`. Te same reguły wprowadź w `InMemorySettingsPreferences` i `EnabledNotifications`. Krok 5 doda dwa rodzaje okienek według tej samej reguły.
3. `SettingsNotificationsScreen`: sekcje rodzajów i tekst o opóźnieniu Androida są widoczne zawsze (usuń warunki `if (notifications.enabled)`). Godzina i wyprzedzenie zostają widoczne tylko przy włączonym danym rodzaju (krok 4 zmieni cały układ). Włączenie dowolnego przełącznika przy `notificationsBlocked` wywołuje `onRequestNotificationPermission()`, jak dziś główny. Komunikat „Zablokowane przez system” pokazuj jak dziś, gdy `notifications.enabled && notificationsBlocked`.
4. `NotificationSettingsUi`: dodaj `kindStates: List<Boolean>` (dziś `[eveningEnabled, beforeClassEnabled]`; krok 5 dopisze okienka). `notificationsStatus` zwraca na podstawie tej listy:
   - żaden włączony: „Wyłączone”;
   - wszystkie włączone: „Wszystkie włączone”;
   - część: „Włączone: {liczba włączonych} z {rozmiar listy}”, na przykład „Włączone: 1 z 2”;
   - przy co najmniej jednym włączonym i blokadzie systemu druga linia „Zablokowane przez system”, jak dziś.

   Druga linia z godziną i wyprzedzeniem znika.
5. Wiersz na głównym ekranie ustawień: tytuł „Powiadomienia o kolizjach” zmień na „Powiadomienia”. Sprawdź `rg -n "Powiadomienia o kolizjach" app/src`: zmień też opis ekranu dla czytnika, jeśli go powtarza. Tytuł ekranu „Powiadomienia” i nazwa sekcji zostają.
6. `FEATURES.md`: w sekcji „Ustawienia i dane”, punkt „Powiadomienia”, zastąp pozycję „Powiadomienia o kolizjach” i drugi wiersz podsumowania regułami z punktów 4 i 5. W akapicie o ekranie „Powiadomienia” zdanie „Sekcje obu rodzajów są widoczne tylko przy włączonej funkcji...” zastąp decyzją 8 i punktem 3. Dopisz decyzje 8 do 10 do wpisu w `LOG.md` z kroku 1. `QUEUE.md`: I-97 `w toku`.

Testy:

- `SettingsPreferencesTest`: `turningMainOnEnablesAllKinds`, `turningMainOffDisablesAllKinds`, `turningOffLastKindTurnsMainOff`, `turningOnOneKindTurnsMainOnAndLeavesOtherOff`, `legacyDisabledFunctionReadsAsAllKindsOff` (zapisane `enabled = false` bez kluczy rodzajów daje wszystko wyłączone).
- `SettingsScreenTest.notificationSummaryCountsEnabledKinds`: oba („Wszystkie włączone”), jeden („Włączone: 1 z 2”), żaden („Wyłączone”), blokada („Zablokowane przez system”). Popraw testy szukające „Powiadomienia o kolizjach” albo „dzień wcześniej, ... przed zajęciami” (`rg -n "Powiadomienia o kolizjach|dzień wcześniej" app/src/androidTest`).
- `SettingsScreenTest.notificationKindsAreVisibleWhenAllAreOffAt320Dp`.
- `CollisionAlarmSchedulerTest` i test `CollisionNotificationPlanner` przechodzą bez zmian logiki.

Przypadki brzegowe: szybkie przełączanie blokuje `isSavingNotifications`, jak dziś; synchronizacja i kopia zapasowa nie przenoszą tych ustawień; po wyłączeniu wszystkiego alarmy są usuwane, jak dziś po wyłączeniu głównego.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.settings.SettingsViewModelTest"` i test planera powiadomień (`rg -l "CollisionNotificationPlanner" app/src/test`).
2. `installDebug`, ręcznie cztery reguły z decyzji 8; zrzuty wiersza „Powiadomienia” w trzech stanach przy 320 dp.
3. Na emulatorze klasy `dev.retza.mak.ui.settings.SettingsPreferencesTest`, `dev.retza.mak.ui.settings.SettingsScreenTest` i `dev.retza.mak.notifications.CollisionAlarmSchedulerTest`.

Kryterium zakończenia: nie da się ustawić głównego przełącznika włączonego przy wszystkich rodzajach wyłączonych; rodzaje są zawsze widoczne; podsumowanie liczy włączone rodzaje; testy przechodzą. Commit: `fix: tie the main notification switch to its kinds (I-97)`.

## 4. Przebuduj ekran „Powiadomienia” na zwartą listę według tematu (I-97)

Cel: ekran „Powiadomienia” w wariancie C z artefaktu „MAK: semestr i powiadomienia” (decyzja 11): u góry przełącznik „Wszystkie powiadomienia”, niżej sekcje tematów, w każdej rodzaje jako wiersze z przełącznikiem i wartością pod nazwą. Wartość otwiera dialog wyboru zamiast rozwijanego pola. Układ ma pomieścić kolejne rodzaje bez wydłużania ekranu o pola wyboru.

Pliki: `ui/settings/SettingsScreen.kt` (`SettingsNotificationsScreen`, `SettingsSwitchRow` albo nowy wiersz obok niego), `ui/SettingsRoutes.kt`; test Android `ui/settings/SettingsScreenTest.kt`; `docs/FEATURES.md`, `docs/ARCHITECTURE.md` (sekcja 4, jeśli nowy wiersz jest nowym wzorcem), `docs/QUEUE.md`.

Zmiany:

1. Zanim zmienisz kod, zrób zrzut ekranu „Powiadomienia” przy 320 dp z włączonymi rodzajami.
2. Układ od góry:
   - karta bez nagłówka z wierszem przełącznika „Wszystkie powiadomienia” (dotychczasowy główny przełącznik; zmień tylko tekst);
   - nagłówek „Kolizje” i karta z dwoma wierszami rodzajów oddzielonymi separatorem `outlineVariant`: „Dzień wcześniej” z wartością „O {godzina}” i „Przed zajęciami” z wartością „{n} min wcześniej”;
   - komunikat „Zablokowane przez system” zostaje nad listą, jak dziś;
   - tekst o opóźnieniu Androida pod sekcjami.

   Nagłówki „Kolizje w planie” i „Dzień wcześniej”, „Przed zajęciami” jako sekcje znikają, a pola „Godzina wieczorna” i „Wyprzedzenie przed zajęciami” znikają z ekranu.
3. Wiersz rodzaju: tytuł `bodyLarge` pogrubiony, pod nim wartość w kolorze `primary` z małą ikoną rozwinięcia (`Icons.Outlined.ExpandMore`), przełącznik po prawej. Wartość jest osobnym celem dotyku o wysokości co najmniej 48 dp z opisem dla czytnika „{tytuł sekcji} {tytuł wiersza}, {wartość}, zmień” i otwiera dialog. Przełącznik zostaje osobnym celem, jak w `SettingsSwitchRow`. Wartość jest widoczna i aktywna także przy wyłączonym rodzaju, aby można było ustawić ją przed włączeniem.
4. Dialog wyboru: `MakDialog` z tytułem „{sekcja}: {wiersz}” (np. „Kolizje: przed zajęciami”), lista opcji jednokrotnego wyboru (istniejący komponent wyboru używany w formularzach, np. `MakChoiceRow`; znajdź przez `rg -n "fun MakChoiceRow|fun MakRadio" app/src/main`), akcje „Anuluj” i „Zapisz” według zasad dialogu z formularzem z `ARCHITECTURE.md`, sekcja 4. Opcje godziny: dotychczasowe `eveningHourOptions` z etykietą „O 20:00”; opcje wyprzedzenia: `leadOptions` z etykietą „{n} min wcześniej”. Zapis woła dotychczasowe `onEveningHourSelected` albo `onBeforeClassLeadSelected`. Stan otwartego dialogu trzymaj w `rememberSaveable`, aby przetrwał obrót.
5. `FEATURES.md`: przepisz akapit o ekranie „Powiadomienia” według punktów 2 do 4. Dopisz decyzję 11 do wpisu w `LOG.md` z kroku 1. `QUEUE.md`: I-97 `odbiór otwarty`, z listą tego, co zostaje do sprawdzenia na telefonie.

Testy:

- `SettingsScreenTest.notificationsScreenGroupsKindsByTopicAt320Dp`: przełącznik „Wszystkie powiadomienia”, nagłówek „Kolizje”, wiersze z wartościami „O 20:00” i „30 min wcześniej”, brak pól „Godzina wieczorna” i „Wyprzedzenie przed zajęciami”.
- `SettingsScreenTest.kindValueOpensChoiceDialogAndSaves`: dotknięcie „30 min wcześniej”, wybór „45 min wcześniej”, „Zapisz” woła `onBeforeClassLeadSelected("45")`; „Anuluj” nic nie woła.
- `SettingsScreenTest.kindValueAndSwitchAreSeparateTargets`.
- Popraw testy, które szukają usuniętych nagłówków i pól.

Przypadki brzegowe: duża czcionka (wartość i tytuł się zawijają, przełącznik nie zmienia rozmiaru); obrót przy otwartym dialogu; motyw jasny i ciemny; focus klawiatury przechodzi przez wartość, potem przełącznik.

Weryfikacja:

1. `installDebug`, zrzuty przy 320 dp w obu motywach: ekran, dialog godziny, dialog wyprzedzenia; porównaj z wariantem C w artefakcie i zrzutem z punktu 1.
2. Na emulatorze klasy `dev.retza.mak.ui.settings.SettingsScreenTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: ekran wygląda jak wariant C, wartości zmienia się w dialogu, testy przechodzą. Commit: `feat: group notification kinds by topic (I-97)`.

## 5. Dodaj powiadomienia o okienkach dzień wcześniej i przed zajęciami (I-98)

Cel: dwa nowe rodzaje powiadomień w sekcji „Okienka” ekranu „Powiadomienia”, działające jak powiadomienia o kolizjach (decyzje 12 i 13). „Dzień wcześniej” o wybranej godzinie wymienia jutrzejsze okienka. „Przed zajęciami” przychodzi z wybranym wyprzedzeniem przed zajęciami, po których zaczyna się okienko. Okienko liczy się według `DOMAIN.md`, sekcja „Okienka”, z progiem z ustawień planu.

Przeczytaj przed zmianą: `DOMAIN.md`, sekcja „Okienka”; `ARCHITECTURE.md`, sekcja 7, podsekcja „Zasady powiadomień”, i sekcja 8, punkt 12.

Pliki: `domain/GapCounter.kt`, `domain/ActivePlanProvider.kt`, `domain/CollisionNotificationPlanner.kt`, `notifications/CollisionAlarmReceiver.kt`, `notifications/CollisionAlarmScheduler.kt`, `notifications/CollisionNotificationContract.kt`, `settings/SettingsPreferences.kt`, `settings/DataStoreSettingsPreferences.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`, `ui/SettingsRoutes.kt`; testy JVM `domain/` (test licznika okienek i planera), `settings/InMemorySettingsPreferences.kt`, `ui/settings/SettingsViewModelTest.kt`; testy Android `ui/settings/SettingsPreferencesTest.kt`, `ui/settings/SettingsScreenTest.kt`, `notifications/CollisionAlarmSchedulerTest.kt`; `docs/DOMAIN.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/LOG.md`, `docs/QUEUE.md`.

Zmiany:

1. `domain/GapCounter.kt`: dodaj `data class Gap(val start: LocalTime, val end: LocalTime, val beforeOccurrenceIds: List<String>)` i `fun findGaps(occurrences: Collection<PlannedOccurrence>, thresholdMinutes: Long): List<Gap>` z tym samym łączeniem nakładających się zajęć w bloki co dziś `countGaps`. `beforeOccurrenceIds` to identyfikatory zajęć bloku, który kończy się na początku okienka, a `start` to koniec tego bloku. `countGaps` zwraca `findGaps(...).size`, aby reguła była w jednym miejscu.
2. `CollisionNotificationKind`: dodaj `GAP_EVENING` i `GAP_BEFORE_CLASS`. Zostaw istniejące `EVENING` i `BEFORE_CLASS` bez zmiany nazw, bo nazwa rodzaju jest zapisana w zaplanowanych alarmach (`EXTRA_KIND`). Nazwy klas z „Collision” zostają; zmiana nazw nie należy do tego kroku.
3. `CollisionNotificationSettings` i preferencje: rodzaje `gapEveningEnabled`, `gapBeforeClassEnabled` (domyślnie `false`, nowe klucze w DataStore) oraz własne `gapEveningHour` (domyślnie 20:00) i `gapLeadMinutes` (domyślnie 30), z tymi samymi opcjami co kolizje. Reguła głównego przełącznika z kroku 3 obejmuje teraz cztery rodzaje; `kindStates` ma cztery elementy, więc podsumowanie pokazuje np. „Włączone: 2 z 4”.
4. Planer (`CollisionNotificationPlanner.plan`): dla każdego dnia horyzontu, obok kolizji, policz `findGaps` z zajęć widocznego planu (ten sam `resolve` i te same ukryte kierunki co kolizje) i progu `inputs.display.gapThresholdMinutes`. Pomijaj odwołane zajęcia, jak `countGaps`.
   - `GAP_EVENING`: jedno powiadomienie na dzień z okienkami, w oknie od `gapEveningHour` dnia poprzedniego, jak `EVENING`;
   - `GAP_BEFORE_CLASS`: jedno powiadomienie na okienko, kończące się `gapLeadMinutes` przed początkiem pierwszych zajęć bloku przed okienkiem (najwcześniejszy `startTime` z `beforeOccurrenceIds`), z oknem `beforeClassWindowMinutes`, jak `BEFORE_CLASS`.

   Stabilny identyfikator liczy się jak dziś z rodzaju, daty i identyfikatorów zajęć.
5. Odbiornik (`CollisionAlarmReceiver`): przed pokazaniem sprawdź, że okienko nadal istnieje (analogicznie do `shouldShowCollisionNotification`: dla `GAP_EVENING` ten sam dzień poprzedni i nadal są okienka z tymi zajęciami; dla `GAP_BEFORE_CLASS` nadal jest okienko po tych zajęciach i zajęcia się nie skończyły). Teksty:
   - `GAP_EVENING`: tytuł „Okienka jutro”, treść „{liczba} {okienko|okienka|okienek}: 10:30-12:00, 14:00-15:30”;
   - `GAP_BEFORE_CLASS`: tytuł „Okienko po zajęciach”, treść „Po {nazwa zajęć} masz okienko {start}-{end}”.

   Nazwę rodzaju z `EXTRA_KIND` odczytuj tak, aby nieznana wartość kończyła obsługę bez błędu.
6. Kanał: osobny kanał „Okienka w planie” (`GAP_NOTIFICATION_CHANNEL_ID`, opis „Powiadomienia o przerwach między zajęciami.”), aby można go wyciszyć w systemie niezależnie od kolizji. Utwórz go tam, gdzie `ensureCollisionChannel`.
7. Ekran „Powiadomienia”: sekcja „Okienka” pod „Kolizjami”, z wierszami „Dzień wcześniej” i „Przed zajęciami” w układzie z kroku 4 i dialogami wartości. Nowe zdarzenia w `SettingsRoutes` i `SettingsViewModel`: `setGapEveningNotificationsEnabled`, `setGapBeforeClassNotificationsEnabled`, `setGapEveningHour`, `setGapLeadMinutes`.
8. Dokumentacja: `DOMAIN.md`, sekcja „Okienka”: reguła z punktów 1, 4 i 5 (które zajęcia wyznaczają powiadomienie, próg z ustawień, pominięcie odwołanych i ukrytych kierunków). `FEATURES.md`: sekcja „Okienka” na ekranie „Powiadomienia” i teksty powiadomień. `ARCHITECTURE.md`, sekcja 8, punkt 12: powiadomienia obejmują kolizje i okienka. `LOG.md`: dopisz decyzje 12 i 13 do wpisu z kroku 1. `QUEUE.md`: I-98 `odbiór otwarty` z listą do sprawdzenia na telefonie (alarm wieczorny i przed zajęciami, wyciszenie kanału).

Testy:

- `GapCounterTest` (albo istniejąca klasa testu `countGaps`): `findGapsReturnsRangeAndClassesBeforeGap`, `overlappingClassesFormOneBlockBeforeGap`, `breakEqualToThresholdIsNotGap` (przerwa równa progowi nie jest okienkiem, `DOMAIN.md`), `countGapsMatchesFindGaps`.
- Test planera: `gapEveningIsPlannedAtItsOwnHour`, `gapBeforeClassFiresLeadMinutesBeforeClassBeforeGap`, `cancelledClassDoesNotCreateGapNotification`, `hiddenProgramDoesNotCreateGapNotification`, `gapKindsOffPlanNothingForGaps`.
- Test funkcji sprawdzającej z punktu 5: okienko zniknęło po dodaniu zajęć, więc powiadomienie się nie pokazuje.
- `SettingsPreferencesTest.mainSwitchCoversGapKinds` i `gapKindsAreOffAfterUpdate` (dane bez nowych kluczy dają wyłączone okienka, a kolizje bez zmian).
- `SettingsScreenTest.gapSectionShowsBothKindsAt320Dp`, podsumowanie „Włączone: 2 z 4”.

Przypadki brzegowe: dzień bez okienek nie planuje nic; okienko po ostatnich zajęciach nie istnieje (`DOMAIN.md`); dwa okienka jednego dnia dają dwa powiadomienia przed zajęciami i jedno wieczorne; zmiana progu okienka w ustawieniach planu przelicza alarmy (sprawdź, czy zmiana `PlanDisplaySettings` już wyzwala ponowne planowanie; jeśli nie, dodaj to); zajęcia przeniesione albo zmienione liczą się po zmianie.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*" --tests "dev.retza.mak.ui.settings.SettingsViewModelTest"`.
2. `installDebug`, dane demo z okienkiem, wyprzedzenie 15 min i godzina wieczorna ustawiona na najbliższą pełną godzinę: oba powiadomienia przychodzą z poprawnym tekstem; zrzuty ekranu „Powiadomienia” przy 320 dp.
3. Na emulatorze klasy `dev.retza.mak.ui.settings.SettingsPreferencesTest`, `dev.retza.mak.ui.settings.SettingsScreenTest`, `dev.retza.mak.notifications.CollisionAlarmSchedulerTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: oba rodzaje okienek można włączyć, ustawić i otrzymać na emulatorze; testy przechodzą; dokumenty opisują reguły. Commit: `feat: notify about gaps between classes (I-98)`.
