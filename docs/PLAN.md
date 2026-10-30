# Plan

Gałąź `task/I-111-tomorrow-plan-and-ci` (decyzja użytkownika z 2026-10-21, `LOG.md`). Kroki wykonuj po kolei; każdy kończy się kompilującym się kodem, zielonymi testami i jednym commitem. Nie wypychaj gałęzi bez polecenia użytkownika.

Zasady dla wykonawcy:

- Windows: `gradlew.bat`, Python przez `py` (`STACK.md`, sekcja „Narzędzia”). Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py` (kod 0).
- Czytaj tylko pliki wymienione w kroku. Jeśli nazwa w kodzie różni się od planu, zachowaj cel kroku i opisz różnicę w commicie.
- Emulator: AVD telefonu dostępny na tym komputerze, z 2048 MiB (`STACK.md`, sekcja „Narzędzia”). `connectedDebugAndroidTest` odinstalowuje aplikację; przed ręcznym sprawdzeniem uruchom `gradlew.bat installDebug`.
- Teksty dla użytkownika piszesz dokładnie tak, jak w planie. Bez znaków `·`, `—` i strzałek.
- Po kroku zaktualizuj wiersz zadania w `QUEUE.md` (status i krótkie „Zrobione: główne klasy i testy”).

Wszystkie kroki tej gałęzi są wykonane. Następne kroki wybierz z `QUEUE.md` po decyzji użytkownika.
