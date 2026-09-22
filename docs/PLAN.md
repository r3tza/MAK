# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są trzy. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Dokończyć podsumowanie „Dzisiaj” (I-02)

Granice: `DOMAIN.md` sekcja „Okienka”; `FEATURES.md` sekcja „Ekran Dzisiaj”; `ARCHITECTURE.md` sekcja „Kolizja nie jest winą użytkownika”. Nie przebudowuj całego ekranu ustawień (to I-05). Nie zmieniaj karty zajęć (to I-03). Nie licz okienek w Compose.

Obecny stan: `TodayViewModel` ustawia tylko `summaryLabel` z liczby zajęć. `MakSummaryCard` w `MakComponents.kt` pokazuje jedną wartość i ozdobną ikonę. Brak funkcji domenowej dla okienek. `SettingsPreferences` nie ma progu.

Kolejność:

1. Dodaj w `domain` czystą funkcję liczenia okienek, np. `countGaps(occurrences, thresholdMinutes)`. Wejście to aktywne `PlannedOccurrence` z `ActivePlan.schedule.occurrences` (odwołane są już pominięte przez resolver).
   1. Posortuj według `startTime`.
   2. Połącz nakładające się przedziały w bloki: następne zajęcia wchodzą do bloku, gdy `next.startTime < block.end`. Styczność godzin (`next.start == block.end`) nie jest nakładką i nie tworzy okienka.
   3. Dla kolejnych bloków policz przerwę `Duration.between(block.end, next.start)`. Okienko istnieje tylko gdy liczba minut jest ściśle większa od progu.
   4. Przy mniej niż dwóch blokach wynik to 0. Nie licz czasu przed pierwszym ani po ostatnim bloku.
2. Dodaj w `domain` funkcję `uniqueCollisionCount(collisions)`. Unikalna kolizja to para posortowanych identyfikatorów wystąpień plus `overlapStart` i `overlapEnd`. Ta sama kolizja przy obu zajęciach liczy się raz. Użyj tego samego wzoru co `widgetCollisionKey` w `WidgetPresenter.kt` i przepnij presenter, żeby liczby się nie rozjechały.
3. Zapisz próg w `SettingsPreferences` / `DataStoreSettingsPreferences` / `InMemorySettingsPreferences`. Klucz DataStore np. `gap_threshold_minutes`, typ `int`, brakująca wartość 30, zakres 5..180. Domyślny próg to 30.
4. W `TodayViewModel` wstrzyknij `SettingsPreferences`. Do stanu wystaw trzy liczby: zajęcia = `schedule.occurrences.size`, kolizje = `uniqueCollisionCount(plan.collisions)`, okienka = `countGaps(...)`. Usuń zależność prezentacji od `summaryLabel` jako jedynej wartości.
5. Zmień `MakSummaryCard` na trzy równe kolumny „Zajęcia”, „Kolizje”, „Okienka”: etykieta nad liczbą, subtelne pionowe separatory, bez ikony. Liczba kolizji jest czerwona, gdy większa od zera, i zielona, gdy wynosi zero. Pozostałe liczby zostają neutralne na gradiencie. Przy braku zajęć karta zostaje i pokazuje zera oraz tekst „Dziś bez zajęć”.
6. Na obecnym `SettingsScreen` dodaj `MakSelectField` progu w bloku planu (obok aktywnego semestru), opcje 15, 20, 30, 45, 60 minut. Nie wydzielaj nowych tras ustawień.

Przypadki brzegowe do testu JVM w nowym `GapCounterTest` oraz w `TodayViewModelTest`:

- przerwa 30 minut: 0 okienek; 31 minut: 1 okienko;
- dwa nakładające się zajęcia (kolizja) tworzą jeden blok, więc nie ma okienka wewnątrz nakładki;
- styk 10:00–11:00 i 11:00–12:00: 0 okienek;
- odwołanie usuwa zajęcia z aktywnego planu, więc nie tworzy okienka ani nie zasłania przerwy;
- przeniesienie i zmiana godzin wchodzą do liczenia, bo są w wyniku `ActivePlanProvider`;
- dwa kierunki z różnymi kalendarzami: liczenie na połączonej liście dnia;
- brak aktywnego semestru: dotychczasowy stan pusty kreatora, nie karta z zerami dnia;
- dzień bez zajęć przy istniejącym semestrze: „Dziś bez zajęć” i trzy zera.

Weryfikacja:

- Uruchom `gradlew.bat test`.
- Uruchom `gradlew.bat compileDebugAndroidTestKotlin`. W `TodayScreenTest` sprawdź trzy etykiety, zera, brak ikony i szerokość 320 dp.
- W `SettingsViewModelTest` sprawdź domyślne 30 i zapis progu.
- Nie uruchamiaj emulatora jako warunku zakończenia implementacji. Odbiór wyglądu na urządzeniu należy do kryterium I-02 przy okazji użycia aplikacji, ale sam krok implementacji zamyka test JVM plus kompilacja testu Compose.

Kryterium zakończenia: domena daje ten sam wynik okienek dla kolizji, przeniesień i odwołań. Ekran „Dzisiaj” pokazuje trzy wartości zgodnie z `FEATURES.md`. Próg jest trwały i domyślnie wynosi 30 minut. Widget używa tej samej funkcji unikalnych kolizji.

## 2. Uporządkować karty zajęć (I-03)

Granice: `FEATURES.md` sekcja „Struktura karty zajęć”; `ARCHITECTURE.md` ten sam temat. Nie zmieniaj widgetu (I-06). Nie scalaj notatek. Nie koloruj całej karty.

Obecny stan: `PlanMapping.kt` ustawia `note = occurrenceNoteBody ?: classNote`. `ClassItemUi.note` jest jednym polem. `ClassCard` w `MakComponents.kt` składa metadane w jeden ciąg `classMeta` i pokazuje jedną notatkę bez etykiety. `CoursePill` nie używa jaśniejszego wariantu koloru kierunku.

Kolejność:

1. W `ClassItemUi` zastąp `note` dwoma polami: `classNote` i `occurrenceNote`. Zaktualizuj wszystkie konstruktory, w tym `ScheduleViewModel.cancelledItems`, `TodayScreenTest` i `ScheduleScreenTest`.
2. W `PlannedOccurrence.toUi` przepisuj obie notatki osobno. Nie wybieraj jednej z nich operatorem `?:`.
3. W `ClassCard` zachowaj dwukolumnową siatkę: lewa kolumna 48 dp na start i koniec, pionowy separator, prawa kolumna w kolejności:
   1. nazwa;
   2. pill kierunku plus neutralny tekst typu;
   3. sala, budynek, prowadzący;
   4. status kolizji, jeśli jest;
   5. notatki, jeśli są.
4. Między grupą 1–3, kolizją i notatkami daj cienki poziomy separator. Nie obramowuj każdej komórki.
5. Pasek 4 dp przy krawędzi używa pełnego `courseColor`. Pill kierunku używa jaśniejszego wariantu tego samego koloru i tekstu o kontraście czytelnych na tym tle. Nazwa kierunku zostaje w pillu. Typ zajęć zostaje zwykłym tekstem obok.
6. Notatkę wspólną pokaż pillem „Notatka do zajęć” (niebieski albo indygo) i treścią obok albo pod etykietą. Notatkę wystąpienia pokaż pillem „Notatka na dziś” (fiolet) w osobnym wierszu. Treści nie zamykaj w pillu. Kolizja zostaje pomarańczowym ostrzeżeniem z istniejącym `conflictLabel`.
7. W `classCardDescription` odczytuj kolejno: godziny, nazwę, kierunek, typ, metadane, kolizję, etykietę i treść każdej notatki. Nie łącz obu notatek w jeden ciąg bez etykiet.

Przypadki brzegowe:

- tylko notatka wspólna; tylko notatka do daty; obie naraz;
- puste albo blank notatki: ukryj wiersz;
- długa nazwa i długa notatka: zawijanie, bez poziomego przewijania przy 320 dp;
- brak koloru kierunku: dotychczasowy zapasowy kolor paska, nazwa kierunku nadal widoczna;
- odwołane zajęcia: dotychczasowa przezroczystość i przekreślenie nazwy.

Weryfikacja:

- Dodaj test JVM dla `toUi`, np. `PlanMappingTest`: obie notatki, jedna notatka, brak notatek, kolizja nie nadpisuje notatek.
- Uruchom `gradlew.bat test`.
- W `TodayScreenTest` i `ScheduleScreenTest` (320 dp) sprawdź etykiety obu notatek i nazwę kierunku.
- Uruchom `gradlew.bat compileDebugAndroidTestKotlin`.

Kryterium zakończenia: oba rodzaje notatek są rozróżnialne etykietą i kolorem. Karta ma sekcje z `FEATURES.md` i mieści się w 320 dp bez poziomego przewijania. Semantyka nie spłaszcza dwóch notatek do jednego tekstu.

## 3. Dodać kontrolę dokumentacji `scripts/check_map.py` (I-07)

Zadanie nie zależy od zmian w aplikacji. Skrypt ma wyłącznie czytać repozytorium, używać standardowej biblioteki Pythona i nie uruchamiać Gradle ani sieci. Nie implementuj `scripts/check_text.py` ani CI.

Ustalony format, którego skrypt ma wymagać:

- krok w `docs/PLAN.md`: nagłówek `## N. tytuł (ID)`, gdzie `N` jest liczbą, a `ID` ma postać `I-01` albo `O-01`;
- tabela w `docs/QUEUE.md`: kolumny `ID`, `Status`, `Zadanie i kryterium zakończenia`, `Zależność`; status to wyłącznie `do implementacji`, `w toku`, `odbiór otwarty`, `gotowe` albo `zablokowane`;
- wpis w `docs/LOG.md`: nagłówek `## YYYY-MM-DD: tytuł`.

Aktywne dokumenty do kontroli linków: `README.md`, `AGENTS.md`, `CLAUDE.md` oraz pliki `docs/*.md` w katalogu `docs` bez podkatalogów. `docs/log_archive/` pomiń; historyczne ścieżki tam zostają.

Kolejność:

1. Utwórz `scripts/check_map.py`. Sprawdź lokalne odnośniki Markdown do plików, także wielkość liter ścieżek na systemach niewrażliwych na wielkość liter.
2. Dla `PLAN.md` odrzuć więcej niż pięć nagłówków kroków. Każdy identyfikator kroku musi istnieć w kolumnie `ID` tabeli `QUEUE.md`.
3. Dla `QUEUE.md` wykryj powtórzone identyfikatory, status spoza listy i zależność wskazującą nieistniejące zadanie. Wartość `brak` nie jest błędem.
4. Dla `LOG.md` odrzuć więcej niż 20 nagłówków datowanych wpisów.
5. Wypisz każdą niezgodność z nazwą pliku i numerem wiersza, gdy da się go ustalić. Zwróć kod 0 przy braku błędów i 1 przy błędach. Nie zapisuj plików.
6. Dodaj testy na katalogach tymczasowych, np. `scripts/test_check_map.py` przez `unittest`: poprawny zestaw, brakujący link, zła wielkość liter, szósty krok planu, brakujący identyfikator kroku, powtórzone ID, zły status, zależność do nieistniejącego ID, 21. wpis logu.
7. Uruchom testy skryptu, potem skrypt na bieżącym repozytorium. Obecne dokumenty mają przejść z kodem 0. Jeśli nie przechodzą, popraw dokumenty tylko wtedy, gdy naruszenie jest rzeczywiste i lokalne; nie poszerzaj zakresu o styl tekstu.

Kryterium zakończenia: skrypt i testy działają bez dodatkowych pakietów, wykrywają wymienione przypadki, a bieżące dokumenty zwracają kod 0. CI pozostaje pomysłem w `QUEUE.md`.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
