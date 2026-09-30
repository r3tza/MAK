# Plan najbliższych prac

Cel: potwierdzić synchronizację Google na prawdziwym koncie. Kod, testy lokalne i ekrany są w I-68 (`QUEUE.md`), razem z poprawkami z krytycznej oceny z 2026-10-01; projekt opisuje `SYNC_PROPOSAL.md`.

Bloker: brak projektu Google Cloud. Użytkownik 2026-09-30 odłożył czynności wymagające jego udziału. Do tego czasu krok 1 nie może się rozpocząć.

## 1. Odbierz synchronizację na dwóch telefonach (I-69)

Cel: sprawdzić rzeczywiste OAuth i Drive na dwóch klientach z tym samym kontem testowym. Pliki: `docs/STACK.md` (sekcja „Konfiguracja synchronizacji Google”), `docs/QUEUE.md`, `docs/KNOWN_ISSUES.md`, `docs/PRIVACY.md`; kod tylko przy znalezionym błędzie (`app/src/main/java/dev/retza/mak/sync/`, `ui/settings/SyncViewModel.kt`, `ui/settings/SyncScreen.kt`).

1. Użytkownik wykonuje punkty 1 do 4 z `STACK.md`, sekcja „Konfiguracja synchronizacji Google”, i przekazuje konto testowe.
2. Zainstaluj APK debug na dwóch emulatorach albo telefonach, zalogowanych na konto testowe.
3. Wykonaj scenariusze z punktu 5 tej sekcji w podanej kolejności. Przy każdym zapisz wynik i zrzut ekranu „Synchronizacja Google” w `build/sync-review`.
4. Po znalezionym błędzie dodaj test JVM odtwarzający go w `SyncCoordinatorTest` albo `DrivePlanTransportTest`, popraw kod i powtórz dotknięty scenariusz.

Testy: tylko regresje znalezionych błędów.

Przypadki brzegowe: dwa pliki `mak-plan.json` po równoczesnym pierwszym wysłaniu, brak `md5Checksum` w odpowiedzi Drive, aktualizacja pliku przez `X-HTTP-Method-Override: PATCH`, cofnięta zgoda w ustawieniach konta Google, plik usunięty ręcznie z folderu aplikacji.

Weryfikacja: `gradlew.bat test lintDebug`, scenariusze z punktu 5 na obu klientach, zrzuty w `build/sync-review`.

Kryterium: wszystkie scenariusze z punktu 5 przeszły na obu klientach; wyniki są w `QUEUE.md`, a I-69 ma status `gotowe`.
