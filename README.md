# MAK

MAK (Mobilny Akademicki Kalendarz) to offline’owa aplikacja na Androida do szybkiego podglądu i zarządzania planem zajęć dla wielu kierunków.

Nazwa produktu to MAK. Rozwinięcie „Mobilny Akademicki Kalendarz” wyjaśnia skrót i nie zastępuje go w interfejsie.

## Dokumenty

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — kształt systemu, zakres i zasady interfejsu
- [STACK.md](docs/STACK.md) — narzędzia, środowisko i testy
- [AGENTS.md](AGENTS.md) — zasady pracy agentów
- [CLAUDE.md](CLAUDE.md) — adapter dla Claude Code
- [WRITING.md](docs/WRITING.md) — zasady pisania
- [plan.md](docs/plan.md) — wymagania produktu, pozostałe prace i odbiór
- [CHANGELOG.md](docs/CHANGELOG.md) — zmiany wydane użytkownikom
- [JOURNAL.md](docs/JOURNAL.md) — fakty, decyzje i uzasadnienia
- [known_issues.md](docs/known_issues.md) — aktualny rejestr problemów interfejsu i odbioru

## Status

Aktualny kod zawiera lokalny plan zajęć, wiele odizolowanych semestrów i kierunków, osobne lub współdzielone kalendarze akademickie, tygodnie A/B, zmiany pojedynczych terminów, notatki, kolizje, eksport i import JSON, widget oraz powiadomienia o kolizjach.

Kod obejmuje nawigację `NavHost`, osobne ViewModele dla przepływów ekranów, Koin, Room v2 i wspólną logikę planu dla ekranów i widgetu. Do wykonania pozostały podział `MakRepository` i poprawki interfejsu opisane w [plan.md](docs/plan.md). Testy instrumentacyjne i odbiór na urządzeniu, zwłaszcza migracji, importu, powiadomień i widgetu, pozostają otwarte. Nie należy traktować kompilacji testów Android jako potwierdzenia ich działania na urządzeniu.

W buildzie debug pusta baza otrzymuje automatycznie semestr demonstracyjny z przykładowymi kierunkami, zajęciami, kolizją, korektą tygodnia A/B, terminem jednorazowym, notatką i zmienionym wystąpieniem. Seed nie nadpisuje istniejących danych i nie działa w buildzie release.
