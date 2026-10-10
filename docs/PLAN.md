# Plan najbliższych prac

Cel: dokończyć poprawki z audytu (polecenie użytkownika z 2026-10-10): K1 z I-89 (typy poza pakietem `ui`) i I-90 (jeden typ identyfikatora w domenie i stanie interfejsu, K2). Raport audytu: `docs/audit_09102026.md`. Po tym planie wszystkie zaakceptowane punkty audytu są zrobione; poza zakresem zostaje mniej rozmiarów widgetu (druga część G1), zapisana w pomysłach do rozważenia.

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

## 1. Przenieś ActivePlanSource, ustawienia telefonu i odmianę liczebników poza pakiet ui (I-89)

Stan: zrobione. `InMemorySettingsPreferences` przeszła do pakietu `settings` w testach; `ActivePlanSourceTest`, `SettingsPreferencesTest` i `SettingsScreenTest` zostały w dotychczasowych pakietach (zmienione tylko importy).

Cel: `widget`, `notifications`, `sync` i `export` nie zależą od pakietu `ui` przez typy, które nie są interfejsem (K1). Zachowanie się nie zmienia; to przeniesienie plików i importów.

Pliki: `ui/ActivePlanSource.kt` (przenieść), `ui/settings/SettingsPreferences.kt` (przenieść), `ui/settings/DataStoreSettingsPreferences.kt` (przenieść), `ui/PolishPlural.kt` (przenieść) i wszystkie pliki z importami tych nazw (main, test, androidTest; listę daje `rg -l "ui.ActivePlanSource|ui.settings.SettingsPreferences|ui.settings.ThemeMode|ui.settings.CollisionNotificationPreferences|ui.settings.themeModeFromStored|ui.settings.DataStoreSettingsPreferences|ui.polishPlural" app/src`); dokument `docs/ARCHITECTURE.md`.

Zmiany:

1. `ActivePlanSource` do pakietu `dev.retza.mak.data` (plik `data/ActivePlanSource.kt`), bez zmian treści poza pakietem i importami.
2. Nowy pakiet `dev.retza.mak.settings` (katalog `settings/`): przenieś `SettingsPreferences.kt` (`ThemeMode`, `CollisionNotificationPreferences`, `SettingsPreferences`, `themeModeFromStored`) i `DataStoreSettingsPreferences.kt`. Funkcja `ThemeMode.label()` to tekst interfejsu, więc zostaje w `ui/settings` w nowym pliku `ui/settings/ThemeModeLabels.kt`. Atrapa `InMemorySettingsPreferences` w testach przechodzi do pakietu `dev.retza.mak.settings` w `app/src/test/java/dev/retza/mak/settings/`, jeśli dziś leży w `ui/settings` (zostaw ją tam, jeśli przeniesienie wymaga zmian w wielu testach; opisz w commicie).
3. `polishPlural` do nowego pliku `text/PolishPlural.kt` w pakiecie `dev.retza.mak.text`, widoczność `internal` jak dziś; test `PolishPluralTest` przenieś do `app/src/test/java/dev/retza/mak/text/`.
4. Popraw importy we wszystkich plikach z listy. Nie zmieniaj niczego poza pakietami i importami.
5. `docs/ARCHITECTURE.md`, sekcja 5, akapit „Pakiety w jednym module Gradle”: dopisz pakiety `settings` (ustawienia telefonu w DataStore) i `text` (wspólne reguły tekstu, na razie odmiana liczebników), oraz że `ActivePlanSource` leży w `data`.

Testy: wszystkie istniejące przechodzą bez zmian poza importami i pakietem przeniesionych testów.

Przypadki brzegowe: Koin znajduje `DataStoreSettingsPreferences` przez `@ComponentScan("dev.retza.mak")`, więc nowy pakiet jest objęty skanowaniem; `KoinGraphTest` to potwierdza.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest`. Ten krok zmienia importy w całym module, więc wyjątkowo uruchom wszystkie testy JVM (to nie są testy urządzenia).
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest` i `dev.retza.mak.ui.settings.SettingsPreferencesTest` (pakiet testu może się zmienić na `dev.retza.mak.settings`).

Kryterium zakończenia: testy przechodzą; `rg "import dev.retza.mak.ui" app/src/main/java/dev/retza/mak/widget app/src/main/java/dev/retza/mak/notifications app/src/main/java/dev/retza/mak/sync app/src/main/java/dev/retza/mak/export` nie znajduje `ActivePlanSource`, `SettingsPreferences`, `ThemeMode`, `CollisionNotificationPreferences` ani `polishPlural`. Commit: `chore: move plan source, phone settings and plurals out of ui (I-89)`. W tym commicie w `QUEUE.md` przenieś I-89 do tabeli „Zakończone” ze statusem `gotowe` i listą punktów audytu (E2, E4, H2, H4, H5, H6, J2, B6 częściowo, K1).

## 2. Używaj identyfikatorów Long w modelach domenowych (I-90)

Stan: zrobione. Kompilator wymagał też `ResolvedSchedule.weeksByCalendarId` jako `Map<Long, ...>`, `ClassForm.semesterProgramId: Long?` i `PlanDisplaySettings.hiddenProgramIds: Set<Long>` (DataStore zapisuje dalej napisy, odczyt pomija wartości nie będące liczbą). `SemesterViewModel.saveWeekOverride` bez identyfikatora formularza tworzy nową korektę, a niepoprawny identyfikator przerywa zapis. 40 miejsc oznaczonych `// I-90 step 3`. Ręcznie: szczegóły terminu, formularz edycji z kierunkiem, filtr kierunku w „Planie”, ukrycie i przywrócenie kierunku.

Cel: modele domenowe i rekordy repozytorium używają tego samego typu identyfikatora `Long` (K2, decyzja użytkownika z 2026-10-10). Mapowanie w `data` przestaje zamieniać liczby na napisy, a ViewModele przestają zamieniać napisy domeny na liczby przed zapisem. Interfejs, trasy nawigacji i format pliku JSON się nie zmieniają.

Pliki: `domain/Models.kt` i pliki domeny, które porównują identyfikatory (`ScheduleResolver.kt`, `CollisionDetector.kt`, `ActivePlanProvider.kt`, `ClassEditImpact` i inne wskazane przez kompilator), `data/repository/DomainMappers.kt`, `ui/SemesterCalendar.kt`, `ui/PlanMapping.kt`, ViewModele w `ui`, `widget/*`, `notifications/*`; testy domeny i ViewModeli oraz atrapy.

Zmiany:

1. `domain/Models.kt`: wszystkie pola identyfikatorów (`Semester.id`, `StudyProgram.id`, `AcademicCalendar.id`, `SemesterProgram.id`, `semesterId`, `studyProgramId`, `academicCalendarId`, `ClassItem.id`, `semesterId`, `semesterProgramId`, `WeekOverride.id`, `academicCalendarId`, `OccurrenceNote.id`, `classId`, `OccurrenceChange.id`, `classId`, `ResolvedSchedule.semesterId` i obliczane w `PlannedOccurrence`) zmień na `Long`. `occurrenceId(classId: Long, originalDate: LocalDate): String` zostaje napisem, bo to identyfikator terminu w trasie i w interfejsie.
2. `DomainMappers.kt`: usuń `toString()` i `toLong()` przy identyfikatorach.
3. Kompilator wskaże pozostałe miejsca. Zasada dla każdego: domena i `data` używają `Long`; w `ui` usuń konwersje `toLong()`, `toLongOrNull() ?: 0` i `toString()` tam, gdzie po zmianie łączą dwa `Long`; tam, gdzie stan interfejsu nadal trzyma identyfikator jako `String` (opcje pól wyboru, filtr „all”, trasy), zostaw w tym kroku jedną konwersję na granicy stanu (`toString()` przy budowie stanu albo `toLongOrNull()` przy odczycie wyboru) i oznacz ją komentarzem `// I-90 step 3`. Nigdy nie zamieniaj nieudanej konwersji na `0` (to dawniej oznaczało „wstaw nowy”): przy `null` akcja się nie wykonuje.
4. Testy: konstrukcje modeli domenowych z `id = "1"` zmień na `id = 1L` (i analogicznie dla pozostałych pól). Oczekiwania się nie zmieniają, poza typem.

Testy: wszystkie istniejące testy JVM przechodzą; nowy test w `ClassEditViewModelTest` `invalidCourseSelectionDoesNotInsert`: wybór kierunku o identyfikatorze, którego nie ma, i zapis nie wywołują `saveClass` z `id = 0`.

Przypadki brzegowe: identyfikator terminu w trasie (`"1:2026-09-21"`) bez zmian; eksport i import JSON bez zmian (ExportSnapshot używa rekordów, które już mają `Long`).

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest` (zmiana typu w całej domenie, wyjątkowo wszystkie testy JVM).
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Na emulatorze klasy `dev.retza.mak.di.KoinGraphTest`, `dev.retza.mak.ui.today.TodayScreenTest`, `dev.retza.mak.ui.schedule.ScheduleScreenTest` i `dev.retza.mak.data.RoomPersistenceTest`.
4. Ręcznie: szczegóły terminu, edycja zajęć i zapis, filtr kierunku w „Planie”, konfiguracja semestru (zapis nazwy, dodanie kierunku).

Kryterium zakończenia: testy i ręczne sprawdzenie dają opisane wyniki; `rg "toLongOrNull\(\) \?: 0" app/src/main` nic nie znajduje; w `DomainMappers.kt` nie ma `toString()` przy identyfikatorach. Commit: `chore: use Long ids in domain models (I-90)`. W tym commicie w `QUEUE.md` zmień status I-90 na `w toku`.

## 3. Trzymaj identyfikatory w stanie interfejsu jako Long (I-90)

Cel: stan interfejsu trzyma identyfikatory semestrów, kierunków, przypisań i kalendarzy jako `Long`, więc ViewModele nie konwertują ich przez `toString()` i `toLongOrNull()` (K2). Wyjątki: identyfikator terminu w trasie (`"klasa:data"`) i identyfikatory dni (`"2026-10-12"`), które nie są numerami wierszy.

Pliki: modele stanu w `ui` z identyfikatorami jako `String` (`ClassCourseOptionUi.id`, `SemesterUi.id`, `SemesterCalendarUi.id`, `SemesterProgramOptionUi.id`, `ScheduleFilterUi.id`, pola `courseCalendarId` i `courseProgramId` w stanie semestru, identyfikatory w stanie kierunków i ustawień), ViewModele i ekrany, które je czytają, trasy semestru (`semesterId` i `assignmentId` jako argumenty `NavType.LongType`, jeśli zmiana nie psuje istniejących tras; inaczej zostaw `String` w trasie i parsuj raz w `SavedStateHandle`); testy ViewModeli i ekranów; dokument `docs/QUEUE.md`.

Zmiany:

1. Każde pole identyfikatora wiersza w stanie `ui` zmień na `Long` (albo `Long?`, gdy może go nie być). Filtr kierunku w „Planie”: `ScheduleFilterUi.id: Long?`, gdzie `null` znaczy „Wszystkie”; `ScheduleControls.courseFilterId: Long?` z `null` zamiast `"all"`.
2. Usuń konwersje oznaczone w kroku 2 komentarzem `// I-90 step 3` i pozostałe `toString()`/`toLongOrNull()` przy identyfikatorach wierszy w ViewModelach (`rg -n "toLongOrNull\(\)|\.toString\(\)" app/src/main/java/dev/retza/mak/ui` i przejrzyj każde trafienie).
3. Ekrany przekazują `Long` w wywołaniach zwrotnych (`onOpenProgram: (Long) -> Unit` i podobne).
4. `docs/QUEUE.md`: I-90 do tabeli „Zakończone” ze statusem `gotowe`, z listą zrealizowanych zmian i testów.

Testy: istniejące testy ViewModeli i ekranów przechodzą po zmianie typów w wywołaniach; nowy test w `ScheduleViewModelTest` `allProgramsFilterIsNull`.

Przypadki brzegowe: argument trasy nie będący liczbą (stara trasa w zapisanym stanie nawigacji po aktualizacji) daje stan „nie znaleziono”, a nie zapis z `id = 0`; wybór „Wszystkie” w filtrze działa jak dziś.

Weryfikacja:

1. `.\gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.ui.*"`.
2. `.\gradlew.bat --offline :app:compileDebugAndroidTestKotlin`.
3. Zrzuty przy 320 dp przed zmianą i po niej (przed testami Android): „Plan” z filtrem kierunku i bez, ekran semestru, „Kierunki” w ustawieniach. Mają być identyczne.
4. Na emulatorze klasy `dev.retza.mak.ui.schedule.ScheduleScreenTest`, `dev.retza.mak.ui.semester.SemesterScreenTest`, `dev.retza.mak.ui.programs.StudyProgramsScreenTest`, `dev.retza.mak.ui.settings.SettingsScreenTest` i `dev.retza.mak.di.KoinGraphTest`.

Kryterium zakończenia: testy przechodzą, zrzuty są identyczne; `rg "I-90 step 3" app/src` nic nie znajduje; `rg "\"all\"" app/src/main/java/dev/retza/mak/ui/schedule` nic nie znajduje. Commit: `chore: keep row ids as Long in ui state (I-90)`.
