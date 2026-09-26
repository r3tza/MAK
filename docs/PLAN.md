# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest pięć poprawek z audytu z 2026-09-25, w kolejności wagi. I-20 (drobne poprawki) zostaje w `QUEUE.md` jako następne zadanie. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 5. Ostrzeżenie o ukrytych zmianach i notatkach przy edycji zajęć (I-19)

Decyzja z 2026-09-26: edycja zajęć zachowuje ich zmiany wystąpień i notatki, a formularz przed zapisem informuje, ile z nich przestanie być widocznych (`DOMAIN.md`, sekcja „Zmiany pojedynczych wystąpień”).

Kolejność:

1. Domena, jedna reguła terminu: wydziel z prywatnej `ScheduleResolver.isBaseOccurrence` publiczną funkcję, np. `ClassItem.hasBaseOccurrenceOn(date, calendar, overrides, weekCalculator)`, która sprawdza zakres kalendarza, dzień tygodnia, cykl A/B i datę zajęć jednorazowych. `ScheduleResolver` używa tej funkcji; testy resolvera przechodzą bez zmian.
2. Domena: `hiddenByClassEdit(before: ClassItem, after: ClassItem, beforeCalendar, afterCalendar, overrides, changes, notes): ClassEditImpact(changeCount, noteCount)`. Liczy zmiany wystąpień (po `originalDate`) i notatki do wystąpień (po `occurrenceDate`, czyli dacie oryginalnej) tych zajęć, których data jest terminem `before`, a nie jest terminem `after`. Kalendarz `after` wynika z nowego kierunku, bo zmiana kierunku może zmienić rytm A/B.
3. `ClassEditViewModel.save`: przy edycji istniejących zajęć policz wpływ z danych `activePlanData`. Gdy suma jest większa od zera i użytkownik nie potwierdził, ustaw w stanie `pendingEditImpact` i zakończ bez zapisu. `confirmSaveWithHiddenData()` zapisuje, `dismissEditImpact()` czyści stan. Nowe zajęcia i edycje bez wpływu zapisują się jak dotąd.
4. `ClassEditScreen`: dialog „Część danych przestanie być widoczna” z treścią „Nowy termin ukryje {n} zmian terminów i {m} notatek przypiętych do dotychczasowych dat. Dane zostaną zachowane i wrócą, jeśli przywrócisz poprzedni termin.”; pomiń zerową część, odmień liczebniki, akcje „Anuluj” i „Zapisz”.
5. Szkic w `SavedStateHandle` (I-12) nie obejmuje dialogu. Po odtworzeniu procesu dialog znika, a ponowny zapis pokaże go znowu.

Przypadki brzegowe: zmiana tylko godzin, sali albo nazwy (brak dialogu); zmiana z „co tydzień” na „tydzień A” (ukryte tylko terminy z tygodni B); zmiana dnia (wszystkie terminy); zmiana daty zajęć jednorazowych (notatka ze starej daty); zmiana kierunku na kierunek z innym kalendarzem; zmiany i notatki z dat poza semestrem (liczą się tylko, jeśli były terminem `before`).

Weryfikacja: testy JVM funkcji wpływu dla każdego przypadku brzegowego; `ClassEditViewModelTest`: dialog przed zapisem, brak zapisu bez potwierdzenia, zapis po potwierdzeniu, brak dialogu przy zmianie godzin; `ScheduleResolverTest` bez zmian. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: edycja, która ukrywa zmiany albo notatki, wymaga potwierdzenia z ich liczbą, żadne dane nie są usuwane, a reguła terminu ma jedno źródło w domenie.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`: I-20 (drobne poprawki); I-14 czeka na decyzję o kopii zapasowej. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
