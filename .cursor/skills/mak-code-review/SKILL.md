---
name: mak-code-review
description: Reviews MAK commits and plan stages against AGENTS.md, ARCHITECTURE.md, STACK.md and PLAN.md. Use when the user runs /code-review, asks for recenzja, code review, or go/no-go after an implementer commit.
---

# Recenzja kodu MAK

Nie zmieniaj kodu, chyba że użytkownik o to poprosi. Odpowiadaj po polsku. Nie uruchamiaj Gradle; implementer podaje weryfikację.

## Kryteria recenzji

Niezgodność z `AGENTS.md`, `ARCHITECTURE.md`, `STACK.md` albo zaakceptowanym planem to bug do naprawy, nie nit.

Ryzyko przyszłej komplikacji (rozjazd zależności, drugi zegar, drugi składany graf, luka w teście która nie złapie padu na starcie) też oznaczaj jako bug do naprawy.

Nie zamykaj etapu werdyktem „można iść dalej”, dopóki takie rzeczy wiszą. Zadanie ma być wykonane tak, żeby później nie wracać do tego samego błędu.

## Delegowanie

Przy `/code-review` recenzję pisze rodzic. Sprawdzanie plików, git show, Grep, odczyt konstruktorów i wyszukiwanie symboli wykonuje subagent Composer 2.5 (`composer-2.5-fast`, typ `explore` albo `generalPurpose`). Rodzic nie robi sam Grep/Read po całym drzewie, jeśli Composer może zebrać cytaty.

Zwracaj: ścieżki, numery linii, dosłowne fragmenty, wynik `git show`. Bez werdyktu recenzji.

Nie oddawaj całego `/code-review` Composerowi. Werdykt, severity i zgodność z AGENTS.md/ARCHITECTURE/STACK/plan zostają u rodzica.

## Przebieg

1. Wczytaj ten skill. Composer zbiera `git show`, pliki commita, cytaty z planu i architektury.
2. Oceń zachowanie, regresje, bezpieczeństwo i brakujące testy wymagane przez `docs/STACK.md` oraz `AGENTS.md`.
3. Znaleziska pierwsze, od najpoważniejszych. Potem krótka poprawka dla agenta implementującego.

## Odpowiedź

Zacznij od werdyktu: trzeba poprawić, albo etap jest zamknięty.

Dla błędu podaj: co łamie (dokument albo przyszły rozjazd), gdzie w kodzie, co zrobić. Nie zamykaj etapu, jeśli wisi bug z sekcji Kryteria.
