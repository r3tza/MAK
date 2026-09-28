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

## Planowanie

Krok w `PLAN.md` ma nagłówek `## N. Tytuł w trybie rozkazującym (ID)` i zawiera:

1. cel w jednym akapicie oraz listę plików;
2. numerowane zmiany w kolejności wykonania, z nazwami funkcji, parametrów i tekstów interfejsu;
3. testy do dodania albo poprawienia;
4. przypadki brzegowe;
5. weryfikację: polecenia, klasy testów i zrzuty;
6. kryterium zakończenia, które da się sprawdzić bez zgadywania.

Każdy krok kończy się kompilującym się kodem, zielonymi testami i osobnym commitem. Duży refaktor dziel na małe etapy o jawnej kolejności, odpowiedzialności, kryterium zakończenia i stanie możliwym do sprawdzenia. Drobne różnice między opisem kroku a kodem wykonawca rozwiązuje zgodnie z celem i opisuje w commicie; zatrzymuje się tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.

Przy istotnej zmianie wyglądu najpierw opisz wariant i role informacji, najlepiej jako makietę do porównania. Po akceptacji użytkownika zapisz decyzję, zaimplementuj ją i porównaj zrzuty przed zmianą i po niej, zanim utrwalisz drobne szczegóły wizualne.

## Gałąź i pull request

Zasady ogólne są w `AGENTS.md`, sekcja „Git”. `main` jest chroniona: zmiany trafiają tam tylko przez pull request z zielonym testem `checks` (`.github/workflows/checks.yml`).

1. Na początku zadania pobierz zmiany (`git fetch origin`) i utwórz gałąź od `origin/main` (`git switch -c task/I-56-dzisiaj-plan origin/main`). Jeśli w katalogu są niezatwierdzone zmiany spoza zadania, nie przenoś ich do gałęzi i zapytaj użytkownika.
2. Pracuj i twórz commity na gałęzi. Aktualizacja statusu w `QUEUE.md`, wpis w `LOG.md` i zmiany w `KNOWN_ISSUES.md` należą do tej samej gałęzi.
3. Przed pull requestem uruchom kontrole z sekcji „Wykonanie i sprawdzenie”. Jeśli `origin/main` poszła do przodu i są konflikty, scal ją do gałęzi (`git merge origin/main`); nie przepisuj historii wypchniętej gałęzi. W konflikcie w `QUEUE.md`, `LOG.md` albo `KNOWN_ISSUES.md` zachowaj oba wpisy i popraw limity.
4. Wypchnij gałąź i otwórz pull request do `main`. Tytuł ma format tematu commita. Opis zawiera: identyfikator zadania i krok `PLAN.md`, jeśli jest; listę zmian; uruchomione testy z wynikiem; zrzuty przed zmianą i po niej przy zmianie interfejsu; to, czego nie sprawdzono, na przykład odbiór na urządzeniu.
5. Po otwarciu poczekaj na wynik `checks`. Jeśli test nie przejdzie, popraw go na tej samej gałęzi.
6. Pull request scala użytkownik przez squash. Po scaleniu przełącz się na `main`, pobierz ją (`git pull --ff-only`) i usuń lokalną gałąź zadania.

## Zakończenie i status

- `do implementacji`: kod wymagany przez kryteria jeszcze nie istnieje.
- `w toku`: praca rozpoczęta, ale nie spełnia jeszcze kryteriów implementacji.
- `odbiór otwarty`: kod istnieje, lecz wymagany test lub ręczny odbiór na urządzeniu nie został wykonany. Wpisz dokładnie, co pozostało.
- `gotowe`: wszystkie kryteria zadania, w tym wymagany odbiór, zostały potwierdzone.
- `zablokowane`: dalszy postęp wymaga decyzji użytkownika lub zmiany stanu zewnętrznego. Zapisz konkretny bloker i nie zgaduj.

Zaktualizuj jeden wiersz w `QUEUE.md` po zmianie statusu. Wiersz ze statusem `gotowe` przenieś do tabeli w sekcji „Zakończone”. W odpowiedzi podaj wynik, zakres weryfikacji i otwarte ograniczenia. Commit może obejmować jedną logiczną zmianę. Nie wypychaj, nie twórz PR ani nie zmieniaj historii bez wyraźnego polecenia użytkownika.
