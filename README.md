# MAK

MAK (Mobilny Akademicki Kalendarz) to offline’owa aplikacja na Androida do szybkiego podglądu i zarządzania planem zajęć dla wielu kierunków.

Nazwa produktu to MAK. Rozwinięcie „Mobilny Akademicki Kalendarz” wyjaśnia skrót i nie zastępuje go w interfejsie.

## Dokumenty

- [MAP.md](docs/MAP.md) - gdzie znaleźć informacje o zadaniu
- [PRODUCT.md](docs/PRODUCT.md) - cel, zakres i kryteria MVP
- [DOMAIN.md](docs/DOMAIN.md) - reguły planu i model domenowy
- [FEATURES.md](docs/FEATURES.md) - zachowanie ekranów i widgetu
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — granice systemu i zasady interfejsu
- [STACK.md](docs/STACK.md) — narzędzia, środowisko i testy
- [AGENTS.md](AGENTS.md) — zasady pracy agentów
- [CLAUDE.md](CLAUDE.md) — adapter dla Claude Code
- [WRITING.md](docs/WRITING.md) — zasady pisania
- [PLAN.md](docs/PLAN.md) - najwyżej pięć najbliższych kroków wykonawczych
- [QUEUE.md](docs/QUEUE.md) — bieżące zadania i osobny status odbioru
- [WORKFLOW.md](docs/WORKFLOW.md) — sposób prowadzenia i kończenia zadań
- [CHANGELOG.md](docs/CHANGELOG.md) — zmiany wydane użytkownikom
- [LOG.md](docs/LOG.md) — najnowsze fakty, decyzje i uzasadnienia; starsze wpisy w [archiwum](docs/log_archive/2026.md)
- [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) — aktualny rejestr problemów interfejsu i odbioru

## Status

Aktualny kod zawiera lokalny plan zajęć, wiele odizolowanych semestrów i kierunków, osobne lub współdzielone kalendarze akademickie, tygodnie A/B, zmiany pojedynczych terminów, notatki, kolizje, eksport i import JSON, widget oraz powiadomienia o kolizjach.

Kod obejmuje nawigację `NavHost`, osobne ViewModele dla przepływów ekranów, Koin, Room v2 i wspólną logikę planu dla ekranów i widgetu. Bieżące statusy implementacji i odbioru są w [QUEUE.md](docs/QUEUE.md), a najbliższe kroki w [PLAN.md](docs/PLAN.md). Nie należy traktować kompilacji testów Android jako potwierdzenia ich działania na urządzeniu.

W buildzie debug pusta baza otrzymuje automatycznie semestr demonstracyjny z przykładowymi kierunkami, zajęciami, kolizją, korektą tygodnia A/B, terminem jednorazowym, notatką i zmienionym wystąpieniem. Seed nie nadpisuje istniejących danych i nie działa w buildzie release.
