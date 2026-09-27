# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Kroki pochodzą z audytu kodu i audytu interfejsu z 2026-09-27. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

I-31, I-32 i I-33 wdrożono 2026-09-27 poza tymi krokami: granice kontrolek używają `outline` o kontraście 3:1, notatki nazywają się „Notatka do zajęć” i „Notatka do terminu”, a komórki siatki dni mają co najmniej 40 dp szerokości.

## 1. Jeden przystanek focusu na kontrolkę (I-27)

Problem: `Modifier.clickable` i `Modifier.selectable` same tworzą cel focusu. Dodatkowe `.focusable()` tworzy drugi, niewidoczny przystanek klawisza Tab. `onFocusChanged` stoi w łańcuchu za celem focusu, więc własna ramka focusu się nie włącza.

1. W `ui/components/MakComponents.kt` popraw `MakIconButton` (obie wersje), `MakRoundButton` (obie wersje), `MakNavButton`, `ClassCard`, nagłówek `MakExpandableSection`, `MakChoiceRow` i `MakCheckbox`. W każdym usuń `.focusable(...)` i przenieś `.onFocusChanged { focused = it.isFocused }` przed `.clickable(...)` albo `.selectable(...)`.
2. Tak samo popraw `WeekTypeBadge` (około wiersza 306) i nagłówek filtrów (około wiersza 381) w `ui/schedule/ScheduleScreen.kt`.
3. W `ClassCard` zachowaj brak celu focusu, gdy `onClick == null`: karta bez akcji nie może być przystankiem Tab.
4. Nie zmieniaj `MakSnackbarHost` (`ui/feedback/MakSnackbarHost.kt`, wiersz 99). Komunikat nie ma akcji kliknięcia, jego jedynym celem focusu jest `.focusable()`, a `onFocusChanged` już stoi przed nim.
5. Dodaj test Compose w `app/src/androidTest/java/dev/retza/mak/ui/components/MakFormControlsTest.kt`: dla `MakIconButton`, `MakChoiceRow` i `MakCheckbox` wyślij `KeyEvent` Tab przez `performKeyInput` i sprawdź, że jedno naciśnięcie przenosi focus na następną kontrolkę. Sprawdź też `assertIsFocused()` na węźle z etykietą kontrolki.

Przypadki brzegowe: nieaktywny `MakRoundButton` nie może być przystankiem; `MakCheckbox` i `MakChoiceRow` po focusie przełączają się klawiszem Enter i spacją.

Sprawdzenie: `gradlew.bat test` i `gradlew.bat connectedDebugAndroidTest` na emulatorze. Na emulatorze przy 320 dp przejdź klawiszem Tab od ikony ustawień do „Dodaj” na ekranie „Dzisiaj” (`adb shell input keyevent 61`).

Kryterium zakończenia: na ekranie „Dzisiaj” ikona ustawień, „Dzisiaj”, „Plan” i „Dodaj” wymagają po jednym naciśnięciu Tab, a każda pokazuje ramkę 2 dp w kolorze `primary`. Testy przechodzą.

## 2. Tekst powiększony i szerokość 320 dp (I-28)

1. `ClassCard` w `MakComponents.kt`: zamień `Modifier.width(48.dp)` kolumny czasu na `Modifier.widthIn(min = 48.dp)` i usuń ucinanie godzin. Kolumna ma mieścić „12:00” przy skali czcionki 2,0. Nie zawijaj godziny.
2. `MakSummaryCard`: etykiety „Zajęcia”, „Kolizje” i „Okienka” nie mogą łamać się w środku słowa. Wyświetl je przez `BasicText` z `maxLines = 1`, `softWrap = false` i `autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 13.sp)` (Compose BOM 2026.09.00 to udostępnia). Jeśli przy skali 2,0 etykieta nadal się nie mieści, pozwól na wielokropek; pełna etykieta zostaje w `contentDescription` kolumny. Liczby zachowują 28 sp.
3. `MakRowTitle`: nadaj tytułowi `Modifier.weight(1f, fill = false)`, a metadanym `maxLines = 1` z wielokropkiem. Przy braku miejsca metadane mają przejść pod tytuł w `FlowRow`, a nie łamać się w środku słowa.
4. Pasek dni w `ScheduleScreen.kt` (około wiersza 460): zamiast `getDisplayName(TextStyle.SHORT, ...)` w `ScheduleViewModel` użyj tych samych dwuliterowych skrótów co nagłówek kalendarza („Pn”, „Wt”, „Śr”, „Cz”, „Pt”, „So”, „Nd”). Pełna nazwa zostaje w `accessibilityLabel`.
5. `WeekTypeBadge`: tekst źródła tygodnia ma `maxLines = 2` bez wielokropka. Opis semantyczny zostaje bez zmian.
6. Pola „Od” i „Do” w `ui/edit/ClassEditScreen.kt` (wiersz 180), `ui/occurrence/OccurrenceDetailsScreen.kt` (wiersz 168), `ui/semester/SemesterScreen.kt` (wiersz 498) i `ui/setup/SetupWizard.kt` (wiersz 129) umieść w `MakFieldPair`. Komunikat błędu pola zostaje pod polem.
7. Dodaj test Compose w `app/src/androidTest/java/dev/retza/mak/ui/today/TodayScreenTest.kt` przy szerokości 320 dp i `Density(fontScale = 2f)`: tekst „12:00” karty jest wyświetlony w całości (`assertTextEquals` i brak `TextLayoutResult.hasVisualOverflow`).

Przypadki brzegowe: nazwa zajęć o długości 80 znaków; skala 1,3 i 2,0; motyw ciemny.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`. Zrzuty z emulatora przy `adb shell wm density 540` (320 dp dla ekranu 1080 px) i `adb shell settings put system font_scale 1.3` oraz `2.0` dla ekranów „Dzisiaj”, „Plan” (lista i kalendarz) i formularza zajęć. Po sprawdzeniu przywróć `wm density reset` i `font_scale 1.0`.

Kryterium zakończenia: na zrzutach przy skalach 1,0, 1,3 i 2,0 żadna godzina nie jest ucięta, żadne słowo nie łamie się w środku, źródło tygodnia jest widoczne w całości przy skali 1,0, a pola „Od” i „Do” stoją jedno pod drugim przy 320 dp.

## 3. Semantyka i kontrast kart (I-29)

1. W `MakComponents.kt` zmień `classCardDescription` na `internal` i buduj opis z pominięciem pustych części. Kolejność: godziny i nazwa; status (`statusBadge`), jeśli jest; kierunek i typ, jeśli niepuste; tydzień (`weekLabel`), jeśli jest; metadane; kolizja; obie notatki z etykietami. Nie wstawiaj samego przecinka dla pustego pola.
2. Odwołana karta: usuń `.alpha(0.68f)` z całej karty. Stan odwołania pokazują przekreślenie nazwy, pill „Odwołane” i pasek w kolorze błędu. Tekst zachowuje zwykłe kolory.
3. `SummaryColumn`: etykieta ma kolor `Color.White` bez obniżonego krycia. Kontrast z `MakSummaryStart` wynosi wtedy 5,61:1.
4. `MakNavButton` „Dodaj”: nadaj rolę `Role.Button` i nie ustawiaj `selected`. „Dzisiaj” i „Plan” zachowują `Role.Tab`. Dodaj parametr roli do `MakNavButton` zamiast osobnego komponentu.
5. Dodaj test JVM `app/src/test/java/dev/retza/mak/ui/components/ClassCardDescriptionTest.kt`: odwołane zajęcia mają w opisie „Odwołane”; zajęcia co tydzień A mają „Tydzień A”; pusty typ i pusty kierunek nie tworzą „, ,”.
6. Popraw test Compose, który sprawdza opis karty, jeśli po zmianie oczekuje starego tekstu.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`. Na emulatorze zrzut odwołanej karty w obu motywach (włącz „Pokaż odwołane” w kalendarzu).

Kryterium zakończenia: test JVM przechodzi, odwołana karta nie ma przyciemnienia całej powierzchni, a etykiety karty podsumowania mają kontrast co najmniej 4,5:1.

## 4. Odwołane terminy liczone w domenie (I-25)

1. W `domain` dodaj funkcję `cancelledOccurrences(data: ActivePlanData, date: LocalDate): List<CancelledOccurrence>`. Zwraca zmiany `CANCELLED` z `originalDate == date` tylko dla zajęć cyklicznych, które mają termin bazowy w tym dniu według `ClassItem.hasBaseOccurrenceOn` i kalendarza swojego przypisania. `CancelledOccurrence` zawiera `ClassItem`, `StudyProgram?`, datę i notatkę do terminu pod datą oryginalną.
2. `ScheduleViewModel.cancelledItems` zastąp wywołaniem tej funkcji i mapowaniem na `ClassItemUi`. Zastosuj ten sam filtr kierunku co dla aktywnych zajęć, także w widoku kalendarza.
3. `selectedDayCountLabel` i `calendarSelectedDayCountLabel` liczą tylko aktywne zajęcia, tak jak „Dzisiaj” i widget.
4. Testy JVM w `app/src/test/java/dev/retza/mak/domain/`: odwołanie zajęć, które po edycji nie mają terminu w tym dniu, nie jest zwracane; odwołanie w dniu spoza kalendarza nie jest zwracane. Test w `ScheduleViewModelTest`: przy filtrze kierunku B odwołanie kierunku A nie pojawia się; licznik dnia nie wlicza odwołanych.

Przypadki brzegowe: zajęcia `ONCE` nie mają zmian wystąpień i nie są zwracane; zajęcia usunięte kaskadą nie mają zmian.

Sprawdzenie: `gradlew.bat test`.

Kryterium zakończenia: `ScheduleViewModel` nie zawiera własnej reguły wyboru odwołanych terminów, a nowe testy przechodzą.

## 5. Zmiana tygodnia tylko wtedy, gdy da się ją zapisać (I-24)

1. W `ScheduleUiState` dodaj `canCorrectWeek: Boolean`. `ScheduleViewModel.buildSchedule` ustawia go na `true`, gdy `visibleWeekCalendarId(data)` nie jest `null`.
2. `WeekTypeBadge` w `ScheduleScreen.kt`: gdy `canCorrectWeek == false`, wiersz nie jest klikalny i nie jest przystankiem focusu, nie pokazuje ikony edycji ani „Zmień”, a pod oznaczeniem tygodnia pokazuje „Wybierz kierunek w filtrach, aby zmienić tydzień.”. Opis semantyczny nie zaczyna się wtedy od „Zmień”.
3. Test JVM w `ScheduleViewModelTest`: przy filtrze „Wszystkie” i dwóch kalendarzach `canCorrectWeek == false`; przy wybranym kierunku `true`; przy jednym kalendarzu i filtrze „Wszystkie” `true`.
4. Test Compose w `app/src/androidTest/java/dev/retza/mak/ui/schedule/ScheduleScreenTest.kt` przy 320 dp: przy `canCorrectWeek == false` nie ma węzła z akcją kliknięcia dla wiersza tygodnia i widać tekst podpowiedzi.
5. Dopisz zachowanie do `FEATURES.md`, sekcja „Widok Lista”.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`; `python scripts/check_map.py` (na tym komputerze `py`).

Kryterium zakończenia: dialog zmiany tygodnia nie otwiera się w stanie, w którym zapis zostałby pominięty, a testy przechodzą.

## Po tych krokach

Następne w kolejności jest I-26 (drobne poprawki z audytu kodu); I-30 zrobiono 2026-09-27. I-42 (logo i animacja startu) wdrożono 2026-09-27; zostaje odbiór na urządzeniu. Następnie aktualizacje w aplikacji w kolejności z `QUEUE.md`: I-36 można zacząć od razu, bo jest testowane na JVM i nie wymaga klucza; I-34 zaczyna się, gdy użytkownik wygeneruje klucz podpisu; I-35 po I-34 i upublicznieniu repozytorium; dalej I-37, I-38, I-39, I-40 i I-41. Przy przenoszeniu tych zadań do planu rozpisz je według kryteriów z `QUEUE.md` i punktu „Aktualizacje” w `ARCHITECTURE.md`. I-14 czeka na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
