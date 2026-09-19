# MAK

MAK (Mobilny Akademicki Kalendarz) to offline’owa aplikacja na Androida do szybkiego podglądu i zarządzania planem zajęć dla wielu kierunków.

Nazwa produktu to MAK. Rozwinięcie „Mobilny Akademicki Kalendarz” wyjaśnia skrót i nie zastępuje go w interfejsie.

## Dokumenty

- [ARCHITECTURE.md](ARCHITECTURE.md) — kształt systemu, zakres i zasady interfejsu
- [STACK.md](STACK.md) — narzędzia, środowisko i testy
- [AGENTS.md](AGENTS.md) — zasady pracy agentów
- [CLAUDE.md](CLAUDE.md) — adapter dla Claude Code
- [WRITING.md](WRITING.md) — zasady pisania
- [plan.md](plan.md) — plan produktu i etapów wdrożenia
- [mockup.html](mockup.html) — interaktywny podgląd ekranów i widgetu
- [CHANGELOG.md](CHANGELOG.md) — zmiany wydane użytkownikom
- [JOURNAL.md](JOURNAL.md) — fakty, decyzje i uzasadnienia

## Status

Wersja 0.1.0 zawiera działający lokalny plan zajęć, wiele odizolowanych semestrów, tygodnie A/B, zmiany pojedynczych terminów, notatki oraz eksport JSON.

Aktualny kod obejmuje także dopracowaną warstwę Compose: nawigację `NavHost` z back stackiem, topbar z obsługą insetów, pickery daty i godziny, paletę kolorów, wspólne tokeny odstępów, ikony Material oraz testy tras i kluczowych interakcji. Testy JVM i kompilacja testów Android przechodzą. Testy runtime na emulatorze pozostają do wykonania.

W buildzie debug pusta baza otrzymuje automatycznie semestr demonstracyjny z przykładowymi kierunkami, zajęciami, kolizją, korektą tygodnia A/B, terminem jednorazowym, notatką i zmienionym wystąpieniem. Seed nie nadpisuje istniejących danych i nie działa w buildzie release.
