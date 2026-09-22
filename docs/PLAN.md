# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są dwa. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Uporządkować karty zajęć (I-03)

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

## 2. Dodać kontrolę dokumentacji `scripts/check_map.py` (I-07)

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
