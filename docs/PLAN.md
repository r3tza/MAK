# Plan najbliższych prac

Cel: naprawić ustalenia z odbioru synchronizacji Google (I-69), a potem dodać wybór zmian przy konflikcie wersji. Odbiór I-69 przeszedł na wersji debug; pozostały punkt (klient OAuth release) wymaga udziału użytkownika i jest opisany w `QUEUE.md`.

## 1. Pozwól usunąć nieużywany kierunek i skróć opis aktualizacji (I-73)

Cel: na ekranie „Edytuj kierunek” można usunąć kierunek, który nie jest przypisany do żadnego semestru; kierunek przypisany jest chroniony z wyjaśnieniem (decyzja użytkownika z 2026-10-08). W tym samym kroku I-74: przełącznik „Sprawdzaj przy uruchomieniu” w stanie włączonym nie ma opisu. Pliki: `data/database/Daos.kt`, `data/repository/SemesterRepository.kt`, `ui/programs/StudyProgramsViewModel.kt`, `ui/programs/StudyProgramsScreen.kt`, `ui/StudyProgramRoutes.kt`, `ui/settings/SettingsScreen.kt`; testy `app/src/test/.../ui/FakeSemesterRepository.kt`, `FakeRepository.kt`, `data/repository/DemoDataSeederTest.kt`, nowy test w `ui/programs/StudyProgramsViewModelTest.kt`, `androidTest/.../ui/settings/SettingsScreenTest.kt`, `androidTest/.../data/RoomPersistenceTest.kt`; dokumenty `docs/FEATURES.md`, `docs/QUEUE.md`.

1. `StudyProgramDao`: zapytanie `observeSemesterNames(id: Long): Flow<List<String>>` zwracające nazwy semestrów z przypisaniem kierunku (`JOIN semester_programs`), posortowane po nazwie, bez powtórzeń.
2. `SemesterRepository`: `fun observeStudyProgramSemesters(id: Long): Flow<List<String>>`; implementacje testowe zwracają nazwy z ich list przypisań.
3. `StudyProgramEditorUi`: pola `usedInSemesters: List<String> = emptyList()`, `showDeleteConfirmation: Boolean = false`, `isDeleting: Boolean = false`. `StudyProgramsViewModel` obserwuje nazwy semestrów edytowanego kierunku. Metody: `requestDelete()` (tylko gdy lista jest pusta), `cancelDelete()`, `confirmDelete()`; ta ostatnia wywołuje `deleteStudyProgram`, publikuje „Usunięto kierunek”, czyści edytor i wysyła `StudyProgramsEffect.CloseEditor`. Błąd z repozytorium (kierunek przypisano w międzyczasie) publikuje „Nie udało się usunąć kierunku.” i zostawia edytor.
4. `StudyProgramEditScreen`: pod przyciskami formularza osobna sekcja. Gdy `usedInSemesters` jest pusta: `MakSecondaryAction("Usuń kierunek", destructive = true)` otwiera `MakConfirmDeletionDialog` z tytułem „Usunąć kierunek?” i tekstem „Kierunek {nazwa} zniknie z listy kierunków. Tej operacji nie można cofnąć.”. Gdy nie jest pusta: przycisk nieaktywny, a pod nim `MakHelperText` „Kierunek jest używany w semestrach: {nazwy}. Aby go usunąć, najpierw usuń go z tych semestrów na ekranie Kierunki semestru.”.
5. I-74: w `SettingsScreen.kt` przełącznik „Sprawdzaj przy uruchomieniu” ma `details = null` przy włączonym przełączniku; opis „Nowe wersje nie pojawią się same.” zostaje przy wyłączonym.
6. `FEATURES.md`: opis ekranu „Kierunki” (usuwanie nieużywanego kierunku i blokada przypisanego) oraz opis przełącznika aktualizacji.

Testy: `StudyProgramsViewModelTest`: usunięcie nieużywanego kierunku po potwierdzeniu, brak usunięcia bez potwierdzenia, brak możliwości prośby o usunięcie kierunku przypisanego. `RoomPersistenceTest`: `observeSemesterNames` zwraca nazwę semestru przypisania. `SettingsScreenTest`: po włączeniu przełącznika opisu nie ma.

Przypadki brzegowe: kierunek przypisany w innym semestrze w czasie otwartego dialogu (błąd repozytorium, komunikat); ostatni kierunek na liście; dwa semestry o tej samej nazwie.

Weryfikacja: `gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.programs.*" --tests "dev.retza.mak.data.repository.DemoDataSeederTest"`, kompilacja testów Android, `SettingsScreenTest` i `RoomPersistenceTest` na emulatorze przez `am instrument`.

Kryterium: kontrole przechodzą, nieużywany kierunek z danych przykładowych da się usunąć na emulatorze, a I-73 i I-74 mają status `gotowe`.

## 2. Odświeżaj szczegóły terminu po zmianie danych (I-75)

Cel: szczegóły terminu pokazują aktualny stan po zapisie formularza zajęć i po pobraniu planu przez synchronizację, bez utraty niezapisanych notatek i edycji. Pliki: `ui/occurrence/OccurrenceViewModel.kt`, test `ui/occurrence/OccurrenceViewModelTest.kt`, `app/src/test/.../ui/FakeRepository.kt`, `docs/KNOWN_ISSUES.md`, `docs/QUEUE.md`.

1. Dodaj pole `currentArgs: OccurrenceArgs?`, ustawiane w `open` i `reload` (po przeniesieniu terminu argumenty wskazują nową datę).
2. W `open` zamiast `activePlanData.first { it != null }` zbieraj kolejne niepuste wartości `activePlanData` do końca `openJob`. Pierwsza wartość buduje stan jak dziś. Każda następna buduje świeży stan dla `currentArgs` i łączy go z bieżącym przez nową funkcję `OccurrenceDetailsUiState.withFreshData(fresh)`:
   - szkic notatki wspólnej i notatki do terminu zostaje, gdy różni się od zapisanej wartości (`noteContentChanged`), a w przeciwnym razie przyjmuje nową wartość;
   - przy otwartym oknie edycji terminu zostają pola `targetDateDraft`, `startTimeDraft`, `endTimeDraft`, `roomDraft`, a `originalDate` i `noteDate` nie zmieniają się;
   - flagi zapisu, błędy szkiców, `showEditDialog` i `showDeleteConfirmation` zostają.
   Brak terminu w nowych danych daje stan „nie znaleziono” tak jak `clearMissingOccurrence`.
3. `editPlanData` przyjmuje każdą nową wartość.

Testy: w `FakeRepository` dodaj `fun notifyDataChanged()`, które powoduje ponowną emisję `observeSemesterData` (licznik `MutableStateFlow` i `flatMapLatest`), bez zmiany istniejących zachowań. W `OccurrenceViewModelTest`: zmiana sali zajęć po otwarciu szczegółów jest widoczna; niezapisany szkic notatki wspólnej zostaje po zmianie sali; usunięcie zajęć po otwarciu daje „nie znaleziono”.

Przypadki brzegowe: termin przeniesiony w szczegółach, a potem zmiana danych w tle; zmiana danych podczas zapisu notatki; zmiana aktywnego semestru.

Weryfikacja: `gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.occurrence.*"`; ręcznie na emulatorze: szczegóły, edycja sali w formularzu zajęć, powrót.

Kryterium: testy przechodzą, scenariusz ręczny pokazuje nową salę, a I-75 ma status `gotowe` i zniknął z `KNOWN_ISSUES.md`.

Kroki 3 do 5 realizują I-76 i I-77 według makiety zaakceptowanej 2026-10-08 (`LOG.md`, wpis z tego dnia). Wykonuj je po kolei; każdy kończy się osobnym commitem.

## 3. Policz różnice i połącz dwie wersje planu (I-77)

Cel: czysta logika Kotlin, bez Androida, która z planu telefonu, planu z Dysku i ostatniego wspólnego planu (bazy) tworzy listę różnic, a z wyborów użytkownika tworzy poprawny połączony plan. Pliki: nowe `app/src/main/java/dev/retza/mak/sync/PlanDifferences.kt` i `sync/PlanMerge.kt`, nowe testy `app/src/test/java/dev/retza/mak/sync/PlanDifferencesTest.kt` i `PlanMergeTest.kt`. Model danych: `BackupData` i encje z `data/entity/Entities.kt`.

1. W `PlanDifferences.kt` zdefiniuj `enum class PlanRowKind { STUDY_PROGRAM, SEMESTER, CALENDAR, SEMESTER_PROGRAM, CLASS, WEEK_OVERRIDE, OCCURRENCE_NOTE, OCCURRENCE_CHANGE }`, `data class PlanRowKey(val kind: PlanRowKind, val id: Long)` i `enum class PlanSide { PHONE, DRIVE }`.
2. Zdefiniuj `data class PlanDifference(val key: PlanRowKey, val phoneKey: PlanRowKey?, val driveKey: PlanRowKey?)`. Gdy wiersz o tym samym numerze istnieje po obu stronach i reprezentuje ten sam wpis, oba klucze są równe `key`. Gdy istnieje tylko po jednej stronie, drugi klucz jest `null`.
3. Funkcja `fun planDifferences(phone: BackupData, drive: BackupData, base: BackupData): List<PlanDifference>`:
   - Porównuj wiersze każdego rodzaju po numerze (`id`). Pole `SemesterEntity.isActive` pomijaj, bo jest lokalne (tak jak `SyncPlanFile.shared`).
   - Wiersz równy po obu stronach nie jest różnicą.
   - Kolizja numerów: wiersz o tym samym numerze istnieje po obu stronach, różni się i nie ma go w bazie. To dwa niezależnie dodane wpisy, więc zwróć dwie różnice: jedną z `phoneKey = key, driveKey = null` i drugą z `phoneKey = null, driveKey = key`. Odróżnij je w `key` przez rodzaj i numer oraz dodatkowe pole `val collision: Boolean = false` w `PlanDifference` (dodaj je).
   - Kolejność wyniku: zajęcia, notatki i zmiany terminów według dnia tygodnia i godziny zajęć, potem kierunki, semestry, kalendarze, przypisania i korekty tygodni według numeru. Ta kolejność jest kolejnością listy na ekranie.
4. W `PlanMerge.kt` zdefiniuj `sealed interface PlanMergeResult { data class Ready(val data: BackupData) ; data class Problems(val problems: List<PlanMergeProblem>) }` i `data class PlanMergeProblem(val difference: PlanDifference, val requires: PlanDifference)`.
5. Funkcja `fun mergePlans(phone: BackupData, drive: BackupData, differences: List<PlanDifference>, picks: Map<PlanDifference, PlanSide>): PlanMergeResult`:
   - Zaczyna od planu z telefonu. Dla każdej różnicy z wyborem `DRIVE` zastępuje wiersz wierszem z Dysku albo go usuwa, gdy na Dysku go nie ma; dla `PHONE` zostawia stan telefonu.
   - Dla kolizji numerów z wyborem zachowania wpisu z Dysku nadaje temu wpisowi nowy numer: największy numer danego rodzaju w obu planach plus jeden. Przenumeruj też odwołania w wierszach z Dysku, które trafiają do wyniku (na przykład `OccurrenceNoteEntity.classId`).
   - Brak wyboru przy którejkolwiek różnicy to błąd programisty: `require(picks.keys.containsAll(differences))`.
   - Po złożeniu sprawdź odwołania: zajęcia do przypisania i semestru, przypisanie do kierunku, kalendarza i semestru, kalendarz i korekta do semestru i kalendarza, notatka i zmiana terminu do zajęć. Każde brakujące odwołanie, którego przyczyną jest wybór przy innej różnicy, zwróć jako `PlanMergeProblem(dziecko, rodzic)`. Gdy problemów nie ma, przepuść wynik przez `ExportImporter.prepare(ExportSnapshot.from(wynik))`; `Invalid` traktuj jako błąd programisty (wyjątek), bo oznacza lukę w powyższym sprawdzeniu.
   - Aktywny semestr wyniku to aktywny semestr telefonu, jeśli istnieje w wyniku; w przeciwnym razie użyj `withActiveSemester` z regułą z `SyncPlanFile.kt` (semestr obejmujący dzisiejszą datę).

Testy (`PlanDifferencesTest`, `PlanMergeTest`, po jednej regule w teście):
- te same plany nie dają różnic; różnica tylko w `isActive` nie jest różnicą;
- zmieniona sala po jednej stronie i notatka po drugiej dają dwie różnice;
- zajęcia dodane po obu stronach z tym samym numerem i bez bazy w tym numerze dają dwie różnice z `collision = true`;
- zachowanie obu dodanych zajęć daje dwa wiersze, z których wpis z Dysku ma nowy numer, a jego notatka wskazuje nowy numer;
- wybór „Dysk” dla zajęć, których na Dysku nie ma, usuwa je razem z notatkami i zmianami terminów, które nie są osobnymi różnicami;
- zajęcia z Dysku zachowane przy kierunku z telefonu, którego nie ma, dają `PlanMergeProblem`;
- brak wyboru przy jednej różnicy kończy się wyjątkiem.

Przypadki brzegowe: plan bez semestrów po jednej stronie; usunięty semestr z zachowanymi zajęciami; kalendarz współdzielony przez dwa przypisania; korekta tygodnia przy kalendarzu usuniętym po drugiej stronie.

Weryfikacja: `gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.sync.PlanDifferencesTest" --tests "dev.retza.mak.sync.PlanMergeTest"`.

Kryterium: oba pliki testów przechodzą, a w `PlanDifferences.kt` i `PlanMerge.kt` nie ma importów Androida.

## 4. Zapamiętuj wspólny plan i daty zmian (I-76)

Cel: koordynator ma bazę do porównania (potrzebną też w I-77) i daty obu wersji. Pliki: `sync/SyncCoordinator.kt`, `sync/SyncStateStore.kt`, `sync/DrivePlanTransport.kt`, nowy `sync/LocalPlanChangeRecorder.kt`, `widget/MakWidgetRefresh.kt`, `MakApplication.kt`, testy `SyncCoordinatorTest`, `DrivePlanTransportTest`.

1. Baza: w `SyncStateStore.kt` dodaj `class SyncBaseCopy(private val file: File)` z `read(): ByteArray?`, `write(bytes: ByteArray)` (przez istniejące `writeAtomically`) i `delete()`. Plik: `no_backup/google-sync/base.json`. Zarejestruj go w Koin tak jak `SyncArchive`.
2. W `SyncCoordinator` przekaż do `synced(...)` bajty planu, który obie strony mają po udanej synchronizacji (przy wysłaniu `localBytes`, przy pobraniu `remoteBytes`, przy `UpToDate` bez zmiany bazy, chyba że jej brak, wtedy zapisz `localBytes`), i zapisz je przez `SyncBaseCopy.write`. Przy rozłączeniu konta (`disconnect`) wywołaj `delete()`.
3. Data Dysku: w `DrivePlanTransport` dodaj `modifiedTime` do `fields` w `list` i `upload`, a do `RemotePlanFile` pole `val modifiedAtMillis: Long? = null` (parsowane z RFC 3339 przez `Instant.parse`; brak pola daje `null`).
4. Data telefonu: przenieś tablicę tabel z `MakWidgetRefresh.kt` do wspólnej stałej `PLAN_TABLES` w pakiecie `data.database` i użyj jej w obu miejscach. `LocalPlanChangeRecorder` rejestruje `InvalidationTracker.Observer(PLAN_TABLES)` i zapisuje `clock.millis()` w nowym polu `SyncState.localChangedAtMillis: Long? = null` przez metodę koordynatora `recordLocalChange(millis)`. Koordynator ignoruje wywołanie, gdy od ostatniej podmiany planu przez synchronizację minęło mniej niż 2000 ms (pole w pamięci `lastReplacementMillis` ustawiane w `download`), a `synced(...)` zeruje `localChangedAtMillis`.
5. `PendingSyncChoice` dostaje pola `phoneChangedAtMillis: Long? = null`, `driveChangedAtMillis: Long? = null` i `differences: List<PlanDifference> = emptyList()` (oznacz klasy z kroku 2 jako `@Serializable`). Gdy baza istnieje, koordynator wylicza różnice przy zapisie pytania; bez bazy lista zostaje pusta.

Testy: `SyncCoordinatorTest` sprawdza zapis bazy po wysłaniu i po pobraniu, usunięcie bazy przy rozłączeniu, pustą listę różnic bez bazy i trzy różnice przy znanej bazie; zmiana lokalna w oknie 2000 ms po podmianie nie ustawia daty. `DrivePlanTransportTest` sprawdza odczyt `modifiedTime` i `null` przy jego braku.

Przypadki brzegowe: pierwsze połączenie dwóch różnych planów (brak bazy); stan zapisany przez starszą wersję aplikacji bez nowych pól; plik bazy uszkodzony (traktuj jak brak).

Weryfikacja: `gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.sync.*"`.

Kryterium: testy pakietu `sync` przechodzą; stary `state.json` bez nowych pól wczytuje się bez błędu.

## 5. Pokaż różnice i ekran „Wybierz zmiany” (I-77)

Cel: interfejs według makiety z 2026-10-08, razem z dialogiem z I-76. Pliki: `sync/SyncCoordinator.kt`, `ui/settings/SyncViewModel.kt`, `ui/settings/SyncScreen.kt`, nowy `ui/settings/SyncChangesScreen.kt`, `ui/SettingsRoutes.kt`, `ui/MakRoutes.kt`, testy `SyncViewModelTest`, `SyncScreenTest`, nowy `SyncChangesScreenTest`; dokumenty `docs/FEATURES.md`, `docs/ARCHITECTURE.md` (sekcja o synchronizacji), `docs/SYNC_PROPOSAL.md`, `docs/QUEUE.md`.

1. Koordynator: `suspend fun resolveWithPicks(picks: Map<PlanDifference, PlanSide>): SyncOutcome`. Jak przy `KEEP_PHONE` sprawdza, że pytanie nadal dotyczy tych samych wersji; potem wywołuje `mergePlans`. Przy `Problems` zwraca nowy wynik `SyncOutcome.MergeProblems(problems)` i niczego nie zmienia. Przy `Ready` archiwizuje obie poprzednie wersje (`ArchiveSource.PHONE` i `ArchiveSource.DRIVE`), podmienia plan przez `gateway.replaceIfUnchanged` w `editTracker.withReplacement` i wysyła wynik z `expected = remote`.
2. Etykiety różnic (w `SyncViewModel`, funkcja `PlanDifference.toUi(phone, drive)`): tytuł to nazwa zajęć, kierunku albo semestru; podtytuł to rodzaj zmiany i termin, na przykład „Sala, czwartek, 16:00”, „Notatka do terminu, 8 października”, „Korekta tygodnia od 12 października”. Dni z `dayNames` w `PlanMapping.kt`, daty z `polishLocale`. Wartości po stronach: tylko pola, które się różnią; wpis tylko po jednej stronie ma tekst „Brak” po drugiej. Przy kolizji numerów oba wpisy mają przyciski „Zachowaj” i „Pomiń”.
3. Dialog (`SyncScreen.kt`): zamiast kart z liczbami dwie karty „Ten telefon” i „Dysk Google” z datą „Zmieniony dziś o 12:20” albo „Zmieniony 7 października o 18:05”; bez daty tekst „Data zmiany nieznana”. Pod nimi sekcja „Co się różni (N)” z najwyżej trzema pierwszymi różnicami i przycisk tekstowy „Wybierz zmiany”. Bez bazy (pusta lista różnic) sekcji i przycisku nie ma, a karty pokazują liczby jak dziś.
4. Ekran „Wybierz zmiany” (`SyncChangesScreen.kt`, trasa `MakRoutes.SettingsSyncChanges`): opis „Przy każdej różnicy wybierz wersję, którą chcesz zachować. Reszta planu zostaje bez zmian.”, lista różnic z dwoma przyciskami wyboru (`Telefon` i `Dysk` z wartością), domyślnie żaden nie jest wybrany, przyciski mają stan zaznaczenia dostępny dla czytnika ekranu. Na dole licznik „Wybrano 3 z 5” i przycisk „Zapisz plan”, nieaktywny, dopóki nie wybrano wszystkich. Przy `MergeProblems` ekran pokazuje komunikat błędu z opisem każdego problemu, na przykład „Zajęcia Statystyka wymagają kierunku Ekonometria z Dysku.”, i zaznacza dotknięte różnice. Po udanym zapisie wraca do ekranu synchronizacji.
5. Dokumenty: w `FEATURES.md`, `ARCHITECTURE.md` i `SYNC_PROPOSAL.md` opisz trzecią drogę rozwiązania konfliktu i bazę `base.json`; w `QUEUE.md` przenieś I-76 i I-77 do zakończonych z wynikami testów.

Testy: `SyncViewModelTest` sprawdza etykiety różnic dla zajęć, notatki i korekty oraz przekazanie wyborów; `SyncScreenTest` sprawdza dialog z trzema różnicami i bez bazy; `SyncChangesScreenTest` sprawdza nieaktywny zapis przy niepełnym wyborze, aktywny po wyborze wszystkich i dostępność przycisków przy skali czcionki 2,0.

Przypadki brzegowe: plan zmieniony na Dysku w trakcie wyboru (koordynator pyta od nowa i ekran pokazuje nową listę); powrót z ekranu bez zapisu zostawia pytanie otwarte; ponad 50 różnic (lista przewija się, licznik działa).

Weryfikacja: `gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.ui.settings.*"`, kompilacja testów Android, `SyncScreenTest` i `SyncChangesScreenTest` na jednym emulatorze z 2048 MiB RAM przez `am instrument`, a nie `connectedAndroidTest`, żeby nie usuwać danych klienta odbioru; odbiór na dwóch klientach: konflikt sali i notatki rozwiązany wyborem „Dysk” dla sali i „Telefon” dla notatki daje na obu klientach plan z obiema zmianami. Zrzuty w `build/sync-review`.

Kryterium: testy przechodzą, odbiór na dwóch klientach daje plan z wybranymi zmianami, a I-76 i I-77 mają status `gotowe`.
