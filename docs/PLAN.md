# Plan

Poprawki po pierwszym odbiorze dni wolnych na telefonie użytkownika (2026-10-12). Gałąź: `task/settings-and-gap-notifications`. Decyzje użytkownika z 2026-10-12: margines między akcjami, krótsze teksty w dwóch dialogach, `DateRangePicker` z Material 3 zostaje (przewijanie miesięcy w pionie bez zmian), ale tytuł i nagłówek z datami nie mogą się łamać. Pola „Od” i „Do” w dialogu zostają bez zmian. Dalsze decyzje z 2026-10-12: wybór zakresu dłuższego niż jeden dzień musi działać (krok 3), zmiana tygodnia po przerwie jest dostępna dla każdego zakresu, także jednego dnia (krok 4), a święta Bożego Narodzenia i Nowy Rok są w dialogu świąt jednym zakresem od 24 grudnia do 2 stycznia (krok 5).

## 1. Skróć teksty i popraw odstępy na ekranach „Dni wolne” i „Semestry” (I-81)

Cel (dotyczy też I-82, dialog świąt): na ekranie „Dni wolne” akcje „Dodaj dni wolne” i „Dodaj święta ustawowe” nie stykają się. Dialog „Święta ustawowe” ma tylko zdanie o zakresie semestru, a dialog „Dodaj dni wolne” pod polami pokazuje tylko liczbę dni, bo zasadę dni wolnych opisuje już ekran.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDaysScreen.kt`
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDaysViewModel.kt`
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDayEditorViewModel.kt`
- `app/src/test/java/dev/retza/mak/ui/freedays/FreeDayEditorViewModelTest.kt`
- `app/src/test/java/dev/retza/mak/ui/freedays/FreeDaysViewModelTest.kt` (jeśli sprawdza `intro`)
- `docs/FEATURES.md`, sekcja „Dni wolne”
- `app/src/main/java/dev/retza/mak/ui/settings/SettingsScreen.kt`, funkcja `SettingsSemestersScreen`
- `app/src/androidTest/java/dev/retza/mak/ui/settings/SettingsScreenTest.kt`
- `docs/FEATURES.md`, sekcja „Ustawienia i dane”, opis ekranu „Semestry”
- `docs/QUEUE.md`, wiersze I-81 i I-82

Zmiany:
1. `FreeDaysScreen.kt`, funkcja `FreeDaysScreen`: drugiemu `MakSecondaryAction` („Dodaj święta ustawowe”) dodaj `modifier = Modifier.padding(top = MakSpacing.sm)`, a pod nim `Column` z kartami dostaje `Modifier.padding(top = MakSpacing.lg)`, tak jak `StudyProgramsScreen` oddziela akcję od listy. Nie zmieniaj `MakSecondaryAction` ani `MakScreenContent`, bo używają ich inne ekrany.
2. `FreeDaysViewModel.kt`, budowa `HolidaysDialogUi.intro`: usuń zdanie „ Odznacz te, w które uczelnia prowadzi zajęcia.”. Wynik: „Święta w czasie semestru, 14 wrz 2026 - 21 lut 2027.” (z kropką na końcu).
3. `FreeDayEditorViewModel.kt`, właściwość `FreeDayEditorUiState.description`: zwracaj tylko `"$days $noun"`, bez kropki i bez zdania o zajęciach cyklicznych. Popraw komentarz KDoc, jeśli opisuje treść zdania.
4. `docs/FEATURES.md`, sekcja „Dni wolne”: w opisie dialogu „Dodaj dni wolne” zamień „pod polami liczba dni i zdanie „Zajęcia cykliczne w tym czasie się nie odbędą. Terminy jednorazowe i przeniesione zostaną.”” na „pod polami liczba dni, na przykład „1 dzień” albo „12 dni””; w opisie dialogu „Święta ustawowe” zostaw tylko zdanie „Święta w czasie semestru, {od} - {do}.”. Dopisz przy ekranie „Dni wolne”, że między akcjami jest odstęp 8 dp.

5. Ekran „Semestry” (decyzja użytkownika z 2026-10-12, zastępuje układ z 2026-09-28 tylko w tym punkcie): w `SettingsSemestersScreen` usuń `MakNoteBanner` z tekstem „Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru.”. Ten sam tekst pokaż jako `MakHelperText` bezpośrednio pod `ActiveSemesterField`, tak jak pomocnicze teksty pod polami w `OccurrenceDetailsScreen`. Pole i tekst umieść razem w `Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs))` wewnątrz istniejącego `if (state.semesters.isNotEmpty())`, aby tekst był przypięty do pola, a bez semestrów nie pokazywał się wcale.
6. `docs/FEATURES.md`, opis ekranu „Semestry”: w zdaniu „Układ od góry (decyzja użytkownika z 2026-09-28): …” zamień „neutralny komunikat bez tytułu „Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru.”” na „pod polem tekst pomocniczy „Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru.” (decyzja użytkownika z 2026-10-12: komunikat w osobnej karcie nie pasował do reszty aplikacji)”.

Testy:
- `FreeDayEditorViewModelTest.descriptionCountsDaysInclusively`: oczekuj dokładnie `"1 dzień"` i `"12 dni"` (`assertEquals`), dodaj przypadek `"2 dni"` i `"5 dni"` dla odmiany.
- Jeśli `FreeDaysViewModelTest` sprawdza `intro`, oczekuj tekstu bez drugiego zdania; jeśli nie sprawdza, dodaj test `holidaysIntroNamesOnlySemesterRange`, który sprawdza, że `intro` kończy się datą końca semestru i kropką oraz nie zawiera „Odznacz”.

Przypadki brzegowe:
- jeden dzień: „1 dzień”; 22 dni: „22 dni”; brak którejkolwiek daty: `description == null`, tekst się nie pokazuje.
- ekran „Semestry” bez semestrów: nie ma pola ani tekstu pomocniczego.
- ekran bez grup dni wolnych: odstęp pod drugą akcją nie tworzy pustej przestrzeni większej niż 16 dp.

Weryfikacja:
- `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.freedays.*"`
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.settings.SettingsScreenTest"` (test sprawdzający tekst ekranu „Semestry” nadal znajduje go pod polem; jeśli test sprawdza kartę, dostosuj go)
- `python scripts/check_map.py`
- zrzut ekranu „Dni wolne”, dialogu „Dodaj dni wolne” i ekranu „Semestry” na emulatorze (`-memory 2048`) przy 320 dp i w motywie ciemnym.

Kryterium zakończenia: testy zielone, `check_map.py` zwraca 0, na zrzucie między obramowaniami obu akcji widać odstęp, dialog świąt ma jedno zdanie, dialog dni wolnych pod polami pokazuje tylko liczbę dni, ekran „Semestry” ma tekst pomocniczy pod polem „Aktywny semestr” bez karty. Commit: `fix: shorten free day texts and space the actions`.

## 2. Popraw tytuł i nagłówek kalendarza zakresu (I-81)

Cel: w oknie wyboru zakresu dat tytuł „Wybierz daty” i nagłówek „Data początkowa - Data końcowa” (po wyborze „14 paź 2026 - Data końcowa”) mieszczą się w jednej linii i są wyrównane do lewej krawędzi dialogu. Komponent `DateRangePicker` z Material 3 zostaje; zmieniamy tylko jego oficjalne parametry `title` i `headline`. Przyczyna: domyślny tytuł i nagłówek mają odstęp 64 dp od lewej, przewidziany dla trybu pełnoekranowego, a w `DatePickerDialog` brakuje przez to miejsca i tekst łamie się po literze.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/components/MakFormControls.kt`, funkcja `MakDateRangeFields`
- `docs/FEATURES.md`, sekcja „Dni wolne”

Zmiany:
1. W `MakDateRangeFields` wywołanie `DateRangePicker(state = pickerState)` zamień na wywołanie z parametrami:
   - `title = { DateRangePickerDefaults.DateRangePickerTitle(displayMode = pickerState.displayMode, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }`
   - `headline = { ProvideTextStyle(MaterialTheme.typography.titleLarge) { DateRangePickerDefaults.DateRangePickerHeadline(selectedStartDateMillis = pickerState.selectedStartDateMillis, selectedEndDateMillis = pickerState.selectedEndDateMillis, displayMode = pickerState.displayMode, dateFormatter = dateFormatter, modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)) } }`, gdzie `dateFormatter = remember { DatePickerDefaults.dateFormatter() }` przekazujesz też jako parametr `dateFormatter` do `DateRangePicker`.
   - `showModeToggle` zostaje domyślne (ikona ołówka).
   Wartości 24 dp i 12 dp odpowiadają odstępom, które Material 3 stosuje w `DatePicker` w `DatePickerDialog`. Sprawdź dokładne nazwy parametrów w wersji z BOM `2026.09.00` (podpowiedź IDE albo źródła `material3`); jeśli sygnatura `DateRangePickerHeadline` się różni, zachowaj ten sam cel: oficjalny nagłówek, mniejszy styl, odstęp 24 dp.
2. Jeśli na zrzucie przy 320 dp nagłówek nadal się łamie, zamiast `titleLarge` użyj `MaterialTheme.typography.titleMedium`. Nie pisz własnego kalendarza ani własnego nagłówka z innym tekstem.
3. Nie zmieniaj `MakDateField` (pojedyncza data), pól „Od” i „Do” ani logiki wyboru.
4. `docs/FEATURES.md`, opis dialogu „Dodaj dni wolne”: dopisz, że `DateRangePicker` przewija miesiące w pionie (zachowanie komponentu, decyzja użytkownika z 2026-10-12), a tytuł i nagłówek z datami mieszczą się w jednej linii przy 320 dp.

Testy: brak nowych testów JVM, bo zmiana dotyczy wyłącznie wyglądu komponentu platformy. Uruchom istniejący `FreeDaysScreenTest` na emulatorze, aby sprawdzić, że dialog nadal się otwiera.

Przypadki brzegowe:
- brak wybranych dat, wybrany tylko początek, wybrany cały zakres;
- tryb wpisywania z klawiatury (ikona ołówka): nagłówek i pola mieszczą się bez łamania;
- szerokość 320 dp i duża czcionka (skala 1,3): tekst może się skrócić, ale nie łamie się po literze;
- motyw jasny i ciemny.

Weryfikacja:
- `.\gradlew.bat :app:compileDebugKotlin`
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.freedays.FreeDaysScreenTest"` na jednym emulatorze `-memory 2048`
- zrzuty okna zakresu przed zmianą i po niej: bez dat, z samym początkiem, z zakresem; przy 320 dp i w motywie ciemnym.

Kryterium zakończenia: na zrzutach tytuł „Wybierz daty” i nagłówek są w jednej linii, zaczynają się 24 dp od lewej krawędzi dialogu, kalendarz nadal przewija się w pionie, `FreeDaysScreenTest` zielony. Po kroku zaktualizuj wiersz I-81 w `QUEUE.md` (poprawki z 2026-10-12 wykonane, odbiór na telefonie otwarty). Commit: `fix: fit the date range picker header in the dialog`.

## 3. Napraw wybór zakresu dłuższego niż jeden dzień (I-81)

Cel: użytkownik może wybrać zakres kilku dni. Dziś po wyborze samego początku w „Od” zapis ustawia koniec na ten sam dzień, a ponowne otwarcie przez „Do” startuje z zaznaczonym zakresem, więc pierwsze dotknięcie zaczyna nowy zakres od dotkniętego dnia i oba pola dostają tę samą datę.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/components/MakFormControls.kt`, funkcja `MakDateRangeFields`
- nowy plik `app/src/main/java/dev/retza/mak/ui/components/DateRangeSelection.kt`
- nowy test `app/src/test/java/dev/retza/mak/ui/components/DateRangeSelectionTest.kt`
- `docs/FEATURES.md`, sekcja „Dni wolne”

Zmiany:
1. W `DateRangeSelection.kt` dodaj czystą funkcję `internal fun resolvePickedRange(pickedStart: LocalDate, pickedEnd: LocalDate?, previousEnd: LocalDate?): Pair<LocalDate, LocalDate>`:
   - `pickedEnd != null`: zwraca `pickedStart to pickedEnd`;
   - `pickedEnd == null` i `previousEnd` nie jest przed `pickedStart`: zwraca `pickedStart to previousEnd` (zmiana samego początku zachowuje koniec);
   - w pozostałych przypadkach: `pickedStart to pickedStart`.
2. W `MakDateRangeFields` stan pickera zależy od pola, z którego go otwarto (`startTapped`):
   - z „Od”: `initialSelectedStartDateMillis` jak dziś, `initialSelectedEndDateMillis = null`, `initialDisplayedMonthMillis` z miesiąca `startDate`;
   - z „Do”: `initialSelectedStartDateMillis` z `startDate`, `initialSelectedEndDateMillis = null`, `initialDisplayedMonthMillis` z miesiąca `endDate` (albo `startDate`, gdy brak końca). Pierwsze dotknięcie dnia po początku ustawia wtedy koniec zakresu, zgodnie z zachowaniem `DateRangePicker`.
   - `rememberDateRangePickerState` wywołuj tak, aby każde otwarcie miało świeży stan (picker powstaje tylko przy `open == true`, więc wystarczy, że stan nie jest zapamiętany poza tym blokiem).
3. Przycisk „Wybierz” wywołuje `onRangeChange` z wynikiem `resolvePickedRange(start, pickedEnd, endDate)`, zamiast ustawiać koniec na początek.
4. `docs/FEATURES.md`, opis dialogu „Dodaj dni wolne”: pole „Do” otwiera kalendarz z zaznaczonym początkiem, więc dotknięty dzień staje się końcem zakresu; wybór samego początku zachowuje koniec, jeśli nie jest przed nowym początkiem.

Testy (`DateRangeSelectionTest`, każdy sprawdza jedną regułę):
- `pickedRangeKeepsBothDates`;
- `startOnlyKeepsPreviousEndWhenNotBeforeStart`;
- `startOnlyAfterPreviousEndGivesOneDay`;
- `startOnlyWithoutPreviousEndGivesOneDay`.

Przypadki brzegowe: dotknięcie w „Do” dnia przed początkiem (Material 3 traktuje go jako nowy początek; po „Wybierz” wynik według reguły z punktu 1); początek i koniec w różnych miesiącach i latach (grudzień - styczeń); `minDate` i `maxDate` semestru nadal blokują dni poza semestrem.

Weryfikacja:
- `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.ui.components.DateRangeSelectionTest"`
- na emulatorze: „Od” 14 paź, „Wybierz”, potem „Do” 20 paź, „Wybierz”; pola pokazują 14.10.2026 i 20.10.2026, a pod nimi „7 dni”. Ten sam scenariusz z zakresem 23 gru - 2 sty.

Kryterium zakończenia: testy zielone, scenariusz z emulatora daje zakres siedmiu dni. Commit: `fix: keep the range when picking the end date`.

## 4. Pozwól zmienić tydzień po każdym zakresie dni wolnych (I-82)

Cel: pole „Po przerwie zacznij od tygodnia” jest w dialogu dni wolnych zawsze, gdy wybrano obie daty, także dla jednego dnia (na przykład święta w czwartek). Korekta „Od tego tygodnia” nadal zaczyna się w pierwszy poniedziałek po ostatnim dniu zakresu.

Pliki:
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDayEditorViewModel.kt`
- `app/src/main/java/dev/retza/mak/data/repository/SemesterRepository.kt`, funkcja `saveFreeDayGroup`
- `app/src/main/java/dev/retza/mak/domain/FreeDayRules.kt`
- `app/src/test/java/dev/retza/mak/domain/FreeDayRulesTest.kt`
- `app/src/test/java/dev/retza/mak/ui/freedays/FreeDayEditorViewModelTest.kt`
- `docs/DOMAIN.md` i `docs/FEATURES.md`, sekcje „Dni wolne”; `docs/LOG.md`

Zmiany:
1. `FreeDayEditorUiState.showsWeekAfterBreak`: zwraca `startDate != null && endDate != null`. Popraw KDoc.
2. `SemesterRepository.saveFreeDayGroup`: usuń `require(coversWorkWeek(...))`.
3. `FreeDayRules.kt`: usuń `coversWorkWeek` (nie ma innych użyć) i jego testy w `FreeDayRulesTest`; `firstMondayAfter` zostaje.
4. `docs/DOMAIN.md`, sekcja „Dni wolne”: zamień „formularz dni wolnych obejmujących co najmniej jeden pełny tydzień od poniedziałku do piątku może ją utworzyć sam” na „formularz dni wolnych może ją utworzyć sam dla każdego zakresu, także jednego dnia (decyzja użytkownika z 2026-10-12)”. `docs/FEATURES.md`: to samo w opisie pola „Po przerwie zacznij od tygodnia”.
5. `docs/LOG.md`: wpis z 2026-10-12 o decyzjach użytkownika z tego kroku i kroku 5; jeśli wpisów jest więcej niż 20, przenieś najstarszy do `docs/log_archive/`.

Testy:
- `FreeDayEditorViewModelTest`: zamień test oczekujący `showsWeekAfterBreak == false` dla krótkiego zakresu na `weekAfterBreakShownForOneDay` (czwartek 12 lis 2026: pole widoczne, opis „Od 16 lis 2026 tydzień A, …”) i sprawdź, że zapis przekazuje `weekAfterBreak` do repozytorium.
- `FreeDayRulesTest`: zostaw testy `firstMondayAfter`; dodaj przypadek niedzieli (następny dzień) i poniedziałku (tydzień później), jeśli ich nie ma.

Przypadki brzegowe: dzień wolny w niedzielę (korekta od następnego dnia); poniedziałek po zakresie poza kalendarzem (korekty nie ma, jak dziś); edycja jednodniowej grupy dodanej z dialogu świąt też pokazuje pole.

Weryfikacja:
- `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.domain.FreeDayRulesTest" --tests "dev.retza.mak.ui.freedays.*"`
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.data.RoomPersistenceTest"`; jeśli test oczekuje wyjątku dla krótkiego zakresu, zamień go na sprawdzenie zapisu korekty.
- `python scripts/check_map.py`

Kryterium zakończenia: testy zielone, `check_map.py` zwraca 0, w dialogu z jednym dniem widać pole „Po przerwie zacznij od tygodnia”. Commit: `feat: allow a week change after any free day`.

## 5. Połącz święta Bożego Narodzenia i Nowy Rok w jeden zakres (I-82)

Cel: dialog „Święta ustawowe” pokazuje jedną pozycję „Przerwa świąteczna” od 24 grudnia do 2 stycznia zamiast osobnych pozycji Wigilii, Bożego Narodzenia, drugiego dnia świąt i Nowego Roku. Zaznaczona pozycja staje się jednym dniem wolnym z tym zakresem. Trzech Króli zostaje osobno.

Pliki:
- `app/src/main/java/dev/retza/mak/domain/PolishHolidays.kt`
- `app/src/main/java/dev/retza/mak/data/repository/SemesterRepository.kt` (`addHolidays`)
- `app/src/main/java/dev/retza/mak/ui/freedays/FreeDaysViewModel.kt` i `FreeDaysScreen.kt` (`HolidaysDialog`)
- `app/src/test/java/dev/retza/mak/domain/PolishHolidaysTest.kt`, `app/src/test/java/dev/retza/mak/ui/freedays/FreeDaysViewModelTest.kt` i testowa implementacja repozytorium w `app/src/test`
- `app/src/androidTest/java/dev/retza/mak/data/RoomPersistenceTest.kt`, `app/src/androidTest/java/dev/retza/mak/ui/freedays/FreeDaysScreenTest.kt`
- `docs/FEATURES.md`, sekcja „Dni wolne”

Zmiany:
1. `PolishHolidays.kt`: `Holiday` dostaje pola `startDate`, `endDate` i `name` (zamiast `date`); święta ustawowe mają `startDate == endDate`. `between` zostaje listą świąt ustawowych.
2. Dodaj `PolishHolidays.freeDayOptions(start: LocalDate, end: LocalDate): List<Holiday>`: dla każdego roku `y` od `start.year - 1` do `end.year` buduje zakres 24 gru `y` - 2 sty `y + 1` o nazwie „Przerwa świąteczna”, przycina go do `start..end` i zostawia tylko wtedy, gdy przycięty zakres zawiera co najmniej jedno święto ustawowe. Z wyniku `between(start, end)` usuwa święta leżące w zostawionych zakresach, dokłada zakresy i sortuje po `startDate`.
3. `FreeDaysViewModel.toHolidaysDialog` używa `freeDayOptions`. `HolidayOptionUi` dostaje `startDate` i `endDate` zamiast `date`; `toggleHoliday` przyjmuje `startDate`. Pozycja jest wyłączona z dopiskiem „Już w „{nazwa}””, gdy w każdym kalendarzu jakiś dzień wolny obejmuje cały jej zakres. Opis pozycji: dla jednego dnia `singleDayLabel`, dla zakresu ten sam format co karty grup („24 gru 2026 - 2 sty 2027, 10 dni”); wyciągnij go z `dateLabelOf` do wspólnej funkcji prywatnej przyjmującej daty.
4. `HolidaysDialogUi.confirmLabel` liczy dni wszystkich zaznaczonych pozycji (suma długości zakresów), więc przy samej przerwie świątecznej brzmi „Dodaj 10 dni”.
5. `addHolidays` zapisuje `startDate` i `endDate` pozycji.
6. `docs/FEATURES.md`, opis dialogu „Święta ustawowe”: pozycja „Przerwa świąteczna” od 24 grudnia do 2 stycznia zastępuje Wigilię, oba dni Bożego Narodzenia i Nowy Rok; zakres jest przycinany do semestru i pomijany, gdy w semestrze nie ma żadnego z tych świąt; „Dodaj {n} dni” liczy dni wszystkich zaznaczonych pozycji.

Testy:
- `PolishHolidaysTest`: `holidaysAcrossNewYearAreSortedAndComplete` zostaje dla `between` (zmień tylko konstrukcję); nowe testy `freeDayOptionsMergeChristmasAndNewYear` (20 gru 2026 - 10 sty 2027: „Przerwa świąteczna” 24 gru - 2 sty, potem „Trzech Króli”), `freeDayOptionsClipBreakToSemester` (semestr do 28 gru 2026: zakres 24-28 gru), `freeDayOptionsSkipBreakWithoutHoliday` (semestr od 2 sty 2027: brak przerwy), `freeDayOptionsKeepBreakBefore2025` (2024: zakres od 24 gru, choć Wigilia nie była wtedy świętem).
- `FreeDaysViewModelTest`: przerwa świąteczna jako jedna pozycja z opisem zakresu; „Dodaj 10 dni” przy samej przerwie; pozycja wyłączona, gdy istniejący dzień wolny obejmuje cały zakres.
- `RoomPersistenceTest` i `FreeDaysScreenTest`: dostosuj konstrukcję `Holiday` i `HolidayOptionUi`; w `RoomPersistenceTest` sprawdź, że przerwa zapisuje się jako jeden wiersz na kalendarz z datami 24 gru i 2 sty.

Przypadki brzegowe: semestr zimowy kończący się przed 24 grudnia (brak pozycji); semestr letni od lutego (brak pozycji); semestr obejmujący dwie przerwy świąteczne (dwie pozycje); część przerwy jest już dniem wolnym w jednym kalendarzu (pozycja aktywna).

Weryfikacja:
- `.\gradlew.bat :app:testDebugUnitTest --tests "dev.retza.mak.domain.PolishHolidaysTest" --tests "dev.retza.mak.ui.freedays.*"`
- `.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.data.RoomPersistenceTest,dev.retza.mak.ui.freedays.FreeDaysScreenTest"` na jednym emulatorze `-memory 2048`
- zrzut dialogu „Święta ustawowe” dla semestru 14 wrz 2026 - 21 lut 2027 przy 320 dp
- `python scripts/check_map.py`

Kryterium zakończenia: testy zielone, dialog pokazuje „Przerwa świąteczna” z opisem „24 gru 2026 - 2 sty 2027, 10 dni” i nie pokazuje osobno Wigilii, Bożego Narodzenia, drugiego dnia świąt ani Nowego Roku; po dodaniu na ekranie „Dni wolne” jest jedna karta „Przerwa świąteczna”. Po kroku zaktualizuj wiersz I-82 w `QUEUE.md`. Commit: `feat: add the Christmas break as one holiday range`.
