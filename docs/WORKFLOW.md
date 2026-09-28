# Workflow pracy nad MAK

Ten dokument opisuje kolejność pracy. Cel i kryteria produktu są w `PRODUCT.md`, reguły planu w `DOMAIN.md`, zachowanie ekranów w `FEATURES.md`, a bieżące statusy w `QUEUE.md`. `PLAN.md` wybiera najwyżej pięć najbliższych kroków. Bezpośrednie polecenie użytkownika ma pierwszeństwo przed domyślną kolejnością kolejki.

## Rozpoczęcie zadania

1. Sprawdź `git status`. Zachowaj cudze i niezwiązane zmiany.
2. Znajdź zadanie w `QUEUE.md`. Jeśli należy do najbliższych kroków, przeczytaj `PLAN.md`. Jeśli użytkownik wskazał inne zadanie, pracuj nad nim w uzgodnionym zakresie.
3. Skorzystaj z `MAP.md`. Otwórz tylko dokumenty i kod dotyczące tego obszaru.
4. Przed większą zmianą ustal granicę zadania, przypadki brzegowe, potrzebne testy i ryzyko utraty danych. Nie rozszerzaj zakresu przy okazji.

## Wykonanie i sprawdzenie

1. Zmieniaj kod zgodnie z jedną odpowiedzialnością i wspólnymi regułami domenowymi. Operacje wieloetapowe pozostaw atomowe.
2. Dodaj odpowiedni test do nietrywialnej nowej reguły lub poprawki błędu. Uruchom tylko kontrole proporcjonalne do zmiany i dostępnego środowiska. Jeśli użytkownik zabronił testów, uszanuj to i odnotuj brak uruchomienia.
3. Przy zmianie interfejsu sprawdź kryteria UI z `ARCHITECTURE.md`: szerokość 320 dp, dostępność, motyw ciemny, focus i stany błędu. Kompilacja testów Android nie oznacza odbioru na urządzeniu.
4. Przy zmianie danych sprawdź migrację, zachowanie istniejących danych i rollback. Przy imporcie lub powiadomieniach sprawdź odpowiednie scenariusze z `FEATURES.md` i reguły z `DOMAIN.md`.
5. Zrób przegląd diffu. Zaktualizuj odpowiednią dokumentację, jeśli zmieniło się zachowanie, architektura albo zakres. Ważną decyzję zapisz w `LOG.md`, a starszy dwudziesty wpis przenieś do archiwum.
6. Przy zmianie `README.md`, `AGENTS.md`, `CLAUDE.md` albo `docs/*.md` uruchom `python3 scripts/check_map.py` (na Windowsie `python` albo `py`). Przy zmianie `scripts/check_map.py` uruchom też `python3 scripts/test_check_map.py`. Nie dodawaj hooka ani bramki CI.

Przed zakończeniem zmiany interfejsu wykonaj przegląd redukcyjny w zakresie zadania. Sprawdź, czy kartę, ikonę, kolor, status, obramowanie albo animację można usunąć bez utraty informacji, hierarchii, dostępności lub działania, i zgłoś takie miejsca w podsumowaniu. Potwierdź też, że hierarchia pozostaje czytelna dzięki typografii, odstępom i wyrównaniu. Elementów wymienionych w `ARCHITECTURE.md` jako zaakceptowane nie zgłaszaj.

Sprawdź też odwrotnie, czy ekran nie gubi informacji: rzadkich stanów (odwołane, zmienione, jednorazowe), dużej liczby elementów, długich nazw, pustych danych i sytuacji, gdy element sterujący lub stan znika z ekranu przy przewijaniu.

## Zakończenie i status

- `do implementacji`: kod wymagany przez kryteria jeszcze nie istnieje.
- `w toku`: praca rozpoczęta, ale nie spełnia jeszcze kryteriów implementacji.
- `odbiór otwarty`: kod istnieje, lecz wymagany test lub ręczny odbiór na urządzeniu nie został wykonany. Wpisz dokładnie, co pozostało.
- `gotowe`: wszystkie kryteria zadania, w tym wymagany odbiór, zostały potwierdzone.
- `zablokowane`: dalszy postęp wymaga decyzji użytkownika lub zmiany stanu zewnętrznego. Zapisz konkretny bloker i nie zgaduj.

Zaktualizuj jeden wiersz w `QUEUE.md` po zmianie statusu. Wiersz ze statusem `gotowe` przenieś do tabeli w sekcji „Zakończone”. W odpowiedzi podaj wynik, zakres weryfikacji i otwarte ograniczenia. Commit może obejmować jedną logiczną zmianę. Nie wypychaj, nie twórz PR ani nie zmieniaj historii bez wyraźnego polecenia użytkownika.
