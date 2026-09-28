# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

Kroki z audytu interfejsu z 2026-09-28 dla I-54, I-56, I-57, I-58, I-60 i I-61 są wykonane; ich odbiór należy do O-05 i O-06. Poniższy krok domyka ten audyt. Odbiór na telefonie (O-07, I-40, I-49) wykonuje użytkownik; listy kontrolne są w odpowiednich wierszach `QUEUE.md`.

## Zasady wspólne dla wszystkich kroków

- Każda część kroku kończy się kompilującym się kodem, zielonymi testami i osobnym commitem (`feat:` albo `fix:`). Gałąź i pull request prowadź według `WORKFLOW.md`, sekcja „Gałąź i pull request”.
- Przed zmianą przeczytaj wskazane pliki w całości. Numery linii w opisie są orientacyjne; szukaj po nazwie funkcji albo tekście.
- Nie zmieniaj niczego poza opisanym zakresem. Drobne różnice między opisem a kodem (inna nazwa parametru, przesunięta funkcja, inny import) rozwiąż zgodnie z celem kroku i opisz w treści commita. Zatrzymaj się i zapisz bloker w `QUEUE.md` tylko wtedy, gdy nie da się ustalić zamierzonego zachowania albo zmiana wymagałaby decyzji spoza planu.
- Polecenia testów i zrzutów są w `STACK.md`, sekcja „Narzędzia”. Testy na urządzeniu czyszczą dane wersji debug, a aplikacja tworzy potem nowe dane demonstracyjne z datami liczonymi od dnia uruchomienia. Zrzuty „przed zmianą” rób więc przed testami na urządzeniu, w tych samych ustawieniach co zrzuty „po zmianie”.
- Po zmianie `docs/*.md` uruchom `py scripts/check_map.py`.
- Przed commitem wykonaj przegląd redukcyjny i sprawdzenie brakującej informacji z `WORKFLOW.md`. Elementów zaakceptowanych w `ARCHITECTURE.md`, sekcja „Czytelność ponad dekorację”, w tym cieni karty zajęć i przełącznika Lista/Kalendarz, nie zmieniaj.

## 1. Popraw usterki z przeglądu i ujednolić wzorce (I-59)

Cel: naprawić trzy usterki znalezione w przeglądzie wykonanych kroków (I-62) i domknąć audyt spójnością wzorców (I-59 razem z I-55). Część A i część B mają osobne commity i mogą trafić do jednego pull requesta.

### Część A: usterki z przeglądu (I-62)

Pliki: `ui/schedule/ScheduleScreen.kt`, `ui/programs/StudyProgramsViewModel.kt`, `ui/StudyProgramRoutes.kt`, `ui/SemesterRoutes.kt`, `ui/semester/SemesterScreen.kt`, testy `androidTest/.../ui/schedule/ScheduleScreenTest.kt`, `androidTest/.../ui/semester/SemesterScreenTest.kt` i `test/.../ui/programs/StudyProgramsViewModelTest.kt`.

1. Numer dnia w kalendarzu nie może maleć przy dużej czcionce. `ARCHITECTURE.md` dopuszcza tekst mniejszy niż 12 sp tylko w widgecie, a `CalendarDay` przy `largeFont` rysuje numer w 11 sp. Zmiany w `CalendarDay`:
   - numer ma zawsze `fontSize = 13.sp`; usuń nadpisany `lineHeight` i import `TextUnit`, jeśli przestanie być używany;
   - zamień `Box` na `Column` z `horizontalAlignment = Alignment.CenterHorizontally` i `verticalArrangement = Arrangement.SpaceBetween`; usuń `align(...)` z numeru i wiersza znaczników;
   - zamień `.height(48.dp)` na `.heightIn(min = 48.dp)`, bo wyjątek rozmiaru w `ARCHITECTURE.md` wymaga co najmniej 48 dp, a przy dużej czcionce komórka może urosnąć;
   - wiersz znaczników rysuj zawsze, także bez znaczników, z wysokością 8 dp i `Modifier.padding(top = 2.dp)`, aby wszystkie komórki tygodnia miały tę samą wysokość;
   - pusty `Box` dopełniający tydzień dostaje `heightIn(min = 48.dp)` zamiast `height(48.dp)`;
   - reguła rozmiaru znaczników zostaje (6 dp przy `largeFont`).
   Test przy `fontScale = 2f` w `ScheduleScreenTest`: zamień asercję dokładnej wysokości 48 dp na „komórka ma co najmniej 48 dp”; zostaw asercje, że każdy znacznik i plus leżą w komórce, a górna krawędź znacznika jest pod dolną krawędzią numeru; dodaj asercję, że komórka wybranego dnia i sąsiedniego dnia tego samego tygodnia mają równą wysokość (tolerancja 1 px).
2. Edytor kierunku nie może przywracać porzuconych zmian. Dziś po wyjściu gestem wstecz z ekranu „Edytuj kierunek” (w ustawieniach albo w semestrze) szkic nazwy i koloru zostaje w `StudyProgramsViewModel`, a `openEditIfNeeded` przy ponownym wejściu na ten sam kierunek go nie odświeża.
   - W `StudyProgramsViewModel` wydziel wczytanie kierunku z `openEditIfNeeded` do prywatnej funkcji `loadEditor(id: Long)`. `openEditIfNeeded` zostaje bez zmian w zachowaniu (warunek `editor.id == id`) i dalej służy odtworzeniu ekranu po obrocie i po zakończeniu procesu.
   - Dodaj `fun openEdit(id: Long)`: najpierw synchronicznie ustawia pusty `StudyProgramEditorUi()`, potem woła `loadEditor(id)`.
   - Wołaj `openEdit` przy jawnym wejściu na ekran edycji, przed `navigate`: w `onOpenProgram` w `StudyProgramRoutes.kt` oraz w `onEditCourse` w `SemesterRoutes.kt`. W semestrze `programId` weź z `semesterViewModel.semester.value.courseItems` po `assignmentId`, tak jak robi to trasa edycji; gdy go nie ma, tylko nawiguj.
   - Nie czyść edytora w `onDispose` ani w `LaunchedEffect` trasy: dispose zachodzi też przy obrocie i skasowałby szkic.
   - Test JVM w `StudyProgramsViewModelTest` (wzór danych weź z istniejących testów tej klasy): po `openEdit(id)`, `updateName("Szkic")` i ponownym `openEdit(id)` nazwa wraca do zapisanej; po `openEdit(id)`, `updateName("Szkic")` i `openEditIfNeeded(id)` nazwa to nadal „Szkic”.
3. Wiersze nawigacji w konfiguracji semestru (`SemesterNavigationRow` w `SemesterScreen.kt`):
   - usuń `semantics { heading() }` z tytułu, bo cały wiersz jest przyciskiem, a nagłówek w przycisku myli nawigację TalkBack po nagłówkach;
   - zamiast wyboru odmiany przez porównanie tytułu (`when (title)`) zamień parametr `count: Int` na `countLabel: String`; trzy wywołania przekazują gotowy tekst, na przykład `"${state.overrideCount} ${polishPlural(state.overrideCount, "korekta", "korekty", "korekt")}"`;
   - w `SemesterScreenTest` sprawdź, że wiersze „Kierunki” i „Korekty tygodni” pokazują liczbę z odmianą, mają rolę przycisku i nie mają roli nagłówka (`SemanticsProperties.Heading` nie występuje w węźle tytułu, szukanym z `useUnmergedTree = true`).

Przypadki brzegowe: skala czcionki 1,0 i 2,0 w kalendarzu przy 320 dp (przy 1,0 komórka ma nadal 48 dp); dzień bez zajęć obok dnia z zajęciami w jednym tygodniu; wybrany dzień i dzisiejszy dzień z obramowaniem przy dużej czcionce; wyjście z edycji kierunku strzałką w pasku, gestem wstecz i przyciskiem „Anuluj”; obrót ekranu w trakcie edycji (szkic zostaje); zero korekt („0 korekt”).

Weryfikacja: `gradlew.bat test`, `ScheduleScreenTest`, `SemesterScreenTest`, `StudyProgramsScreenTest`; zrzut kalendarza przy 320 dp i skali czcionki 2,0 w obu motywach; na emulatorze: zmień nazwę kierunku w „Edytuj kierunek”, wyjdź gestem wstecz, wejdź ponownie i potwierdź zapisaną nazwę, osobno w ustawieniach i w semestrze.

Kryterium zakończenia części A: numer dnia ma 13 sp przy każdej skali i nie nachodzi na znaczniki, a komórki jednego tygodnia mają równą wysokość; porzucone zmiany w edytorze kierunku nie wracają po ponownym wejściu, a obrót ich nie kasuje; wiersze konfiguracji semestru nie są nagłówkami i dostają gotową etykietę liczby; testy przechodzą.

### Część B: spójność wzorców i drobne błędy (I-59, z I-55)

Wykonuj punkty po kolei. Każdy punkt może być osobnym commitem.

1. Karta zajęć (`ClassCard` w `MakComponents.kt`, decyzja użytkownika z 2026-09-28: separator zostaje): między kolumną godzin a kolumną danych dodaj `VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)`. Zewnętrzny `Row` karty ma już `.height(IntrinsicSize.Min)`; wewnętrzny `Row` z treścią dostaje `.fillMaxHeight()`, a separator też `.fillMaxHeight()`, aby sięgał od góry do dołu treści. Cień i obramowanie karty zostają. Zmiana zamyka rozbieżność O-05 z `FEATURES.md`, sekcja „Struktura karty zajęć”.
2. Dialogi usuwania: przenieś `ConfirmDeletionDialog` z `SemesterScreen.kt` do `ui/components` jako `MakConfirmDeletionDialog(title, text, confirmLabel = "Usuń", isDeleting, onConfirm, onCancel)` (Material 3 `AlertDialog`, przyciski tekstowe, potwierdzenie w kolorze błędu). Użyj go w trzech miejscach w `SemesterScreen.kt` (korekta, kalendarz, `CourseDeletionDialog`), w `DeleteSemesterDialog` w `SettingsScreen.kt` i w dialogu „Usuń zajęcia” w `OccurrenceDetailsScreen.kt` (`confirmLabel = "Usuń zajęcia"`). Treść i tytuły zostają. Zmienia się tylko tych pięć dialogów potwierdzenia usunięcia. Pozostałe dialogi zostają bez zmian: błąd importu, „Część danych przestanie być widoczna”, zmiana tygodnia A/B, edycja terminu i połączenie kalendarzy (`ReconnectCalendarDialog`).
3. `WeekOverrideForm` w `SemesterScreen.kt`: kolejność przycisków „Anuluj”, potem przycisk zapisu („Dodaj korektę” albo „Zapisz zmiany”), jak w pozostałych formularzach z przyciskami w jednym wierszu.
4. `CalendarCard` w `SemesterScreen.kt`: daty przez `asCalendarDate()` zamiast surowego zapisu ISO (`"${calendar.startDate} - ${calendar.endDate}"`).
5. Nagłówek wybranego dnia w kalendarzu zaczyna się wielką literą, jak w liście („Poniedziałek, 28 września 2026”). W `ScheduleViewModel` zmień tylko wartość `calendarSelectedDayLabel` (`replaceFirstChar { it.titlecase(Locale.forLanguageTag("pl-PL")) }`); nie zmieniaj `fullDateFormatter`, bo tworzy też opis dnia dla czytnika ekranu. Popraw test, który sprawdza ten tekst.
6. Kreator: pole „Pierwszy tydzień” w `SetupWizard.kt` (dziś `options = listOf("A", "B")`) pokazuje wartość „Tydzień {A albo B}” oraz opcje „Tydzień A” i „Tydzień B”, jak w konfiguracji semestru; do `onFirstWeekChanged` przekazuj ostatni znak opcji, bo stan kreatora przechowuje samą literę. Popraw `SetupWizardTest`, jeśli wybiera opcję „A” albo „B”.
7. „O aplikacji” (I-55, `AboutScreen.kt`): usuń `AboutCard` wokół nagłówka aplikacji oraz wokół sekcji „Możliwości” i „Dane i prywatność”; zostaw nagłówki sekcji i odstęp 24 dp. W `AppHeader` zmień `gapColor` znaku maku z `surfaceContainerLow` na `MaterialTheme.colorScheme.background`, bo znak nie stoi już na karcie. Kartę zachowuje tylko lista „Ostatnie zmiany”. Punktory `BulletList` mają kolor `onSurfaceVariant` zamiast `primary`. `StatusText` („Zainstalowana”, „Aktywny”) zostaje bez zmian. W `UpdateScreen.kt` zastąp dwa wywołania `AboutSection` („Nowa wersja”, „Dostępna wersja …”) nagłówkiem `titleMedium` z `heading()` i treścią bez karty; usuń `AboutSection`, gdy przestanie być używana.
8. Widget (`MakTodayWidget.kt`): godzina nadchodzących zajęć w `WidgetOccurrenceRow` ma kolor `onBackground` zamiast `primary`; kolor akcentu zostaje tylko dla „Teraz” i „Następne”.
9. Martwy kod: usuń wersje `MakIconButton` i `MakRoundButton` z parametrem `symbol` (`MakComponents.kt`), `MakSummaryStartDark` i `MakSummaryEndDark` (`Color.kt`), pole `TodayUiState.showPlanAction`, parametr `onOpenPlan` ekranu `TodayScreen` razem z jego przekazywaniem w `todayRoute` (`TodayScheduleRoutes.kt`) i w `MakApp` (`MakNavHostApp.kt`), pole `TodayUiState.emptyTitle` oraz parametr `emptyTitle` w `MakStateMessage`. `SettingsInfoRow` usunięto już w I-54. Przed usunięciem sprawdź `rg`, że nic poza testami z tego nie korzysta, i popraw testy.

Przypadki brzegowe: karta zajęć z długą nazwą i obiema notatkami (separator na całą wysokość); karta odwołana; dialog usuwania w trakcie usuwania (`isDeleting` blokuje przyciski); korekta w trybie edycji; kalendarz bez dat w karcie (jeśli pole może być puste, zostaw obecne zachowanie); widget w motywie ciemnym.

Weryfikacja: `gradlew.bat test` i pełny `gradlew.bat connectedDebugAndroidTest`. Zrzuty przy 320 dp w obu motywach: „Dzisiaj”, „Plan” (lista i kalendarz), szczegóły terminu z dialogiem usuwania, „Semestry”, konfiguracja semestru, „Korekty tygodni” z otwartym formularzem, kreator (krok semestru), „O aplikacji” i „Aktualizacja”. Widget wersji debug sprawdź na launcherze emulatora, jeśli da się go dodać; jeśli nie, zapisz to w opisie pull requesta.

Kryterium zakończenia części B: karta zajęć ma pionowy separator; pięć dialogów usuwania używa `MakConfirmDeletionDialog`; formularze z przyciskami w jednym wierszu mają tę samą kolejność; daty kalendarzy są czytelne; nagłówek dnia zaczyna się wielką literą; kreator pokazuje „Tydzień A” i „Tydzień B”; „O aplikacji” ma kartę tylko przy historii wydań; godzina w widgecie nie używa akcentu; martwy kod usunięty; testy przechodzą. Po tym kroku I-55, I-59 i I-62 mają status `odbiór otwarty` z odbiorem w ramach O-05 i O-06, a pozycje I-55 i I-59 przechodzą w `KNOWN_ISSUES.md` do sekcji „Wymagają odbioru na urządzeniu”.

Następne zadania to tryb tabletowy (I-45 do I-47). Ich kroki trzeba rozpisać po zakończeniu tego kroku.
