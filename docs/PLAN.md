# Plan najbliższych prac

Cel: odebrać opcjonalną synchronizację Google na prawdziwym koncie. Kod i lokalne regresje I-70 są gotowe; I-69 wymaga projektu Google Cloud oraz udziału użytkownika.

## 1. Odbierz synchronizację Google na dwóch klientach (I-69)

Cel: sprawdzić rzeczywiste OAuth i Drive na dwóch klientach z tym samym kontem testowym. Pliki: `docs/STACK.md` (sekcja „Konfiguracja synchronizacji Google”), `docs/QUEUE.md`, `docs/KNOWN_ISSUES.md`, `docs/PRIVACY.md`; kod tylko przy znalezionym błędzie (`app/src/main/java/dev/retza/mak/sync/`, `ui/settings/SyncViewModel.kt`).

1. Użytkownik wykonuje punkty 1 do 4 z `STACK.md`, sekcja „Konfiguracja synchronizacji Google”, i udostępnia konto testowe.
2. Użyj najwyżej jednego emulatora naraz, skonfigurowanego z 2048 MiB RAM. Drugi klient to telefon fizyczny albo osobny AVD uruchomiony po całkowitym zatrzymaniu pierwszego. Każdy AVD ma własne lokalne dane i 2048 MiB RAM, a oba korzystają ze wspólnego stanu Drive.
3. Wykonaj scenariusze z punktu 5 tej sekcji w podanej kolejności. Przy każdym zapisz wynik i zrzut ekranu „Synchronizacja Google” w `build/sync-review`.
4. Po znalezionym błędzie dodaj test JVM odtwarzający go w `SyncCoordinatorTest` albo `DrivePlanTransportTest`, popraw kod i powtórz dotknięty scenariusz.

Testy: tylko filtrowane regresje klas zmienionych w odpowiedzi na znaleziony błąd. Nie uruchamiaj pełnego zestawu aplikacji dla tego odbioru.

Przypadki brzegowe: dwa pliki `mak-plan.json` po równoczesnym pierwszym wysłaniu, brak `md5Checksum` w odpowiedzi Drive, aktualizacja pliku przez `X-HTTP-Method-Override: PATCH`, cofnięta zgoda w ustawieniach konta Google, plik usunięty ręcznie z folderu aplikacji.

Weryfikacja: odpowiednie filtrowane regresje lokalne oraz scenariusze z punktu 5 w `STACK.md` na obu klientach; zrzuty w `build/sync-review`.

Kryterium: wszystkie scenariusze z punktu 5 przeszły na obu klientach; wyniki są w `QUEUE.md`, a I-69 ma status `gotowe`.
