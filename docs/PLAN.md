# Plan

Kroki dni wolnych (I-81, I-82) według decyzji z 2026-10-11 (`LOG.md`). Reguły są w `DOMAIN.md` i `FEATURES.md`, sekcje „Dni wolne”. Wszystkie kroki na gałęzi `task/settings-and-gap-notifications`.

## 1. Dodaj dni wolne do domeny i resolvera (I-81)

Cel: `ScheduleResolver` pomija zajęcia cykliczne w dniach wolnych kalendarza i zwraca dni wolne daty, więc kolizje, okienka, powiadomienia i widoki dostają je z jednego miejsca. Pliki: `domain/Models.kt`, `domain/ScheduleResolver.kt`, `domain/ActivePlanProvider.kt`, `test/.../domain/ScheduleResolverTest.kt`.

1. `Models.kt`: `data class FreeDay(id: Long, academicCalendarId: Long, startDate: LocalDate, endDate: LocalDate, name: String?)` z `require(!endDate.isBefore(startDate))` i `operator fun contains(date: LocalDate)`.
2. `ResolvedSchedule` dostaje `freeDaysByCalendarId: Map<Long, FreeDay> = emptyMap()`: dzień wolny obejmujący datę dla każdego kalendarza semestru, który ma tę datę w zakresie (`weeksByCalendarId.keys`). Funkcja `fun ResolvedSchedule.freeDayOf(calendarIds: Collection<Long>): FreeDay?` zwraca dzień wolny, gdy lista nie jest pusta i każdy kalendarz z listy ma dzień wolny; właściwość `val freeDay: FreeDay?` to `freeDayOf(weeksByCalendarId.keys)`.
3. `ScheduleResolver.resolve` dostaje parametr `freeDays: Collection<FreeDay> = emptyList()`. Wystąpienie zajęć cyklicznych (nie `ONCE`) jest pomijane, gdy jego data pierwotna jest dniem wolnym kalendarza zajęć i nie zostało przeniesione (`change?.targetDate` jest `null` albo równe dacie pierwotnej). Dotyczy to też odwołań, więc nie trafiają do `cancelled`. Przeniesione na dzień wolny i przeniesione z dnia wolnego zostają.
4. `ActivePlanData` dostaje `freeDays: Collection<FreeDay> = emptyList()`, a `ActivePlanProvider.schedule` przekazuje je do resolvera.

Testy (`ScheduleResolverTest`): pominięcie w środku zakresu i na obu granicach, brak pominięcia dzień przed i dzień po; zajęcia jednorazowe zostają; przeniesione na dzień wolny zostają; przeniesione z dnia wolnego zostają w dniu docelowym; zmienione bez przeniesienia są pomijane; odwołanie w dniu wolnym nie trafia do `cancelled`; dwa kalendarze, wolne tylko w jednym (zajęcia drugiego zostają, `freeDay` jest `null`, `freeDayOf` jednego kalendarza zwraca dzień); kolizja i okienko znikają, gdy jedne z zajęć są pominięte (`ActivePlanProvider`).

Przypadki brzegowe: zakres jednodniowy; dzień wolny poza zakresem kalendarza; dni wolne innego semestru albo kalendarza nie wpływają na plan.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.domain.*"`.

Kryterium zakończenia: testy domeny zielone, reszta aplikacji się kompiluje (`compileDebugKotlin`).

## 2. Zapisz dni wolne w Room, kopii, eksporcie i synchronizacji (I-81)

Cel: dni wolne są trwałe, trafiają do planu aktywnego semestru, kopii, pliku JSON w wersji 4 i porównania przy synchronizacji. Pliki: `data/entity/Entities.kt`, `data/database/*`, `data/repository/*`, `export/*`, `sync/PlanDifferences.kt`, `sync/PlanMerge.kt`, `ui/settings/SyncDifferenceLabels.kt`, schemat `app/schemas/.../4.json`.

1. `FreeDayEntity` (tabela `free_days`: `id`, `semester_id`, `academic_calendar_id`, `start_date`, `end_date`, `name` nullable; klucze obce do semestru i kalendarza z `CASCADE`; indeksy jak w `WeekOverrideEntity`), `FreeDayDao`, `AppDatabase` w wersji 4 z `MIGRATION_3_4` (`CREATE TABLE` i indeksy), relacja `freeDays` w `SemesterWithData`.
2. `FreeDayRecord` w `SemesterRecords.kt`, mapowanie encja, rekord i domena w `DomainMappers.kt`, `freeDays` w `toActivePlanData()` i w `SemesterConfiguration`.
3. `SemesterRepository`: `observeFreeDays(semesterId)`, `saveFreeDayGroup(semesterId, replacedIds, startDate, endDate, name, calendarIds)` w jednej transakcji (usuwa `replacedIds`, wstawia zakres w każdym kalendarzu, waliduje kalendarze semestru i daty; parametr `weekAfterBreak` dochodzi w kroku 5) i `deleteFreeDays(ids)`. Kopia kalendarza (`copyCalendarWithOverrides`) kopiuje też dni wolne.
4. `SemesterBackup.freeDays`, `PlanBackupGateway` (snapshot, czyszczenie i wstawianie), `ExportSchema.VERSION = 4`, `FreeDaySnapshot`, `SemesterSnapshot.freeDays` z wartością domyślną `emptyList()` (pliki 2 i 3 bez pola), `ExportImporter`: wersje 2, 3 i 4, unikalne identyfikatory, kalendarz z tego semestru, poprawne daty i koniec nie przed początkiem.
5. Synchronizacja: `PlanRowKind.FREE_DAY`, wiersze w `rows()`, `withId`, `withReferences`, `toBackupData`; etykiety różnicy: tytuł nazwa albo „Dni wolne”, podtytuł „Semestr {nazwa}”, wartość zakres dat.

Testy: `RoomMigrationTest` 3 do 4 na zachowanych danych; `RoomPersistenceTest` zapisu grupy i usunięcia kalendarza z dniami wolnymi; `JsonExportCodecTest` i `ExportImporterTest` (wersja 4 w obie strony, plik w wersji 3 bez `freeDays`, błędne daty, obcy kalendarz); `PlanMergeTest` dla dnia wolnego.

Weryfikacja: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.export.*" --tests "dev.retza.mak.sync.*" --tests "dev.retza.mak.data.*"`; na emulatorze `RoomMigrationTest` i `RoomPersistenceTest`.

Kryterium zakończenia: testy zielone, wygenerowany schemat `4.json` w repozytorium.

## 3. Dodaj dialog i ekran „Dni wolne” (I-81)

Cel: użytkownik dodaje, edytuje i usuwa dni wolne z konfiguracji semestru i z kalendarza „Planu”. Pliki: nowy pakiet `ui/freedays/` (`FreeDayEditorViewModel`, `FreeDayDialog`, `FreeDaysViewModel`, `FreeDaysScreen`), `ui/components/MakFormControls.kt` (`MakDateRangeFields`), `ui/MakRoutes.kt`, `ui/SemesterRoutes.kt`, `ui/semester/SemesterScreen.kt`, `ui/TodayScheduleRoutes.kt`, `ui/schedule/ScheduleScreen.kt`.

1. `MakDateRangeFields`: pola „Od” i „Do” obok siebie, oba otwierają `DateRangePicker` w `DatePickerDialog` z akcjami „Wybierz” i „Anuluj”, z ograniczeniem do zakresu kalendarzy semestru.
2. `FreeDayEditorViewModel`: `startNew(semesterId, date)`, `startEdit(semesterId, group)`, zmiany pól, `save()`, `cancel()`; walidacja dat i co najmniej jednego kalendarza; podwójne kliknięcie zapisu nie zapisuje dwa razy.
3. `FreeDayDialog` według `FEATURES.md` (tytuły, liczba dni, zdanie o zajęciach, checkboxy kalendarzy tylko przy kilku kalendarzach).
4. Ekran „Dni wolne” (`semester/{semesterId}/free-days`) z kartami grup, edycją, usuwaniem z potwierdzeniem; wiersz „Dni wolne” w `SemesterScreen` pod „Korekty tygodni”.
5. „Plan”, widok kalendarza: obrysowana akcja „Oznacz jako wolny” pod „Dodaj termin jednorazowy” otwiera dialog z wybranym dniem.

Testy: JVM `FreeDayEditorViewModelTest` (walidacja, domyślne kalendarze, edycja grupy, podwójny zapis) i `FreeDaysViewModelTest` (grupowanie, etykiety dat i liczebniki); Compose `FreeDaysScreenTest`.

Weryfikacja: filtrowane testy JVM `dev.retza.mak.ui.freedays.*`; na emulatorze `FreeDaysScreenTest`, `SemesterScreenTest`, `KoinGraphTest`.

Kryterium zakończenia: dodanie, edycja i usunięcie dni wolnych działa z obu wejść, testy zielone.

## 4. Pokaż dni wolne na „Dzisiaj”, w „Planie” i na widgecie (I-81)

Cel: dzień wolny jest widoczny wszędzie, gdzie widać plan dnia. Pliki: `ui/today/*`, `ui/schedule/*`, `ui/components/PresentationModels.kt` i komponent kalendarza, `widget/*`.

1. „Dzisiaj”: nagłówek karty podsumowania „Dzień wolny” i baner w sekcji „Zajęcia” (`schedule.freeDay`).
2. „Plan”: `CalendarDayUi.freeDay` z tłem łączącym kolejne dni, legenda „Dzień wolny”, etykieta dostępności z „dzień wolny”; baner w liście i pod nagłówkiem dnia kalendarza; kalendarze widoku według filtru (`freeDayOf`).
3. Widget: „Dzień wolny” i nazwa z datą końca.

Testy: JVM `TodayViewModelTest`, `ScheduleViewModelTest`, `WidgetPresenterTest` dla dnia wolnego i filtru z jednym kalendarzem wolnym; Compose `TodayScreenTest` i `ScheduleScreenTest` dla banera.

Weryfikacja: filtrowane testy JVM i Compose; zrzuty „Dzisiaj”, kalendarza „Planu” i widgetu przy 320 dp.

Kryterium zakończenia: testy zielone, zrzuty zgodne z makietami.

## 5. Dodaj święta ustawowe i tydzień po przerwie (I-82)

Cel: szybkie dodanie polskich świąt i ustawienie rytmu A/B po przerwie. Pliki: `domain/PolishHolidays.kt`, `ui/freedays/*`, `data/repository/SemesterRepository.kt`.

1. `PolishHolidays.between(start, end): List<Holiday>` (`Holiday(date, name)`): stałe święta, Wielkanoc (algorytm Meeusa/Jonesa/Butchera), Poniedziałek Wielkanocny, Zielone Świątki (+49), Boże Ciało (+60), Wigilia od 2025.
2. Dialog „Święta ustawowe” na ekranie „Dni wolne” według `FEATURES.md`; zapis jednodniowych dni wolnych we wszystkich kalendarzach semestru jedną transakcją.
3. Pole „Po przerwie zacznij od tygodnia” w `FreeDayDialog` przy pełnym tygodniu Pn-Pt; `saveFreeDayGroup(..., weekAfterBreak)` tworzy korektę `FROM_WEEK` w pierwszy poniedziałek po zakresie w każdym wybranym kalendarzu, który go obejmuje.

Testy: `PolishHolidaysTest` (Wielkanoc i Boże Ciało dla 2024, 2025, 2026 i 2027, Wigilia przed 2025 i od 2025, zakres przez przełom roku), test warunku pełnego tygodnia i daty poniedziałku, test zapisu korekty w Room.

Weryfikacja: filtrowane testy JVM; na emulatorze `RoomPersistenceTest`, `FreeDaysScreenTest`.

Kryterium zakończenia: testy zielone, I-81 i I-82 w `QUEUE.md` mają status `odbiór otwarty` z opisem tego, co sprawdzić na telefonie.
