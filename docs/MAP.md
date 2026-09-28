# Mapa dokumentacji MAK

Zacznij od pytania, które rozwiązuje zadanie. Otwórz wskazany dokument i potrzebną sekcję, nie cały katalog. Gdy mapa nie wskazuje właściwego miejsca, popraw ją razem ze zmianą dokumentacji.

| Pytanie | Gdzie szukać |
|---|---|
| Po co powstaje MAK i co wchodzi do MVP? | [PRODUCT.md](PRODUCT.md): użytkownicy, cel, zakres pierwszej wersji, kryteria MVP i funkcje wyłączone |
| Jak ma działać ekran lub widget? | [FEATURES.md](FEATURES.md): start aplikacji i ekran ładowania, nawigacja, „Dzisiaj”, „Plan”, formularz zajęć, widget, ustawienia i scenariusze odbioru na urządzeniu |
| Jak obliczać plan? | [DOMAIN.md](DOMAIN.md): semestry i kalendarze, rytm A/B, zajęcia, zmiany wystąpień, okienka i kolizje |
| Jakie są granice kodu i zasady UI? | [ARCHITECTURE.md](ARCHITECTURE.md): repozytoria, wspólna logika planu, nawigacja, integracje i zasady interfejsu |
| Jakich narzędzi i testów używać? | [STACK.md](STACK.md): wersje bibliotek, środowisko, rodzaje kontroli i odrzucone alternatywy |
| Co wykonać teraz? | [PLAN.md](PLAN.md): najwyżej pięć najbliższych kroków z kryteriami zakończenia. [QUEUE.md](QUEUE.md) zawiera pozostałe zadania, zależności i statusy; zakończone są w osobnej tabeli na końcu |
| Co jest gotowe w kodzie, ale czeka na urządzenie? | [QUEUE.md](QUEUE.md): osobne pozycje odbioru; szczegółowe scenariusze w [FEATURES.md](FEATURES.md) |
| Jak rozpocząć, sprawdzić i zakończyć zadanie? | [WORKFLOW.md](WORKFLOW.md): kolejność pracy, szablon kroku planu i znaczenie statusów |
| Jakie zasady obowiązują przy zmianie widgetu albo powiadomień? | [ARCHITECTURE.md](ARCHITECTURE.md): sekcja 7, podsekcje „Zasady widgetu” i „Zasady powiadomień” |
| Co testować i jak uruchomić testy albo zrzuty na emulatorze? | [STACK.md](STACK.md): sekcja 5, „Sposób testowania”, i sekcja 6, „Narzędzia” |
| Jak ludzie zgłaszają błędy i pomysły? | [CONTRIBUTING.md](../CONTRIBUTING.md): tylko zgłoszenia, bez pull requestów od innych osób; szablon zgłoszenia błędu w `.github/ISSUE_TEMPLATE/blad.yml` |
| Co ostatnio zmieniono i dlaczego? | [LOG.md](LOG.md): najwyżej 20 ostatnich wpisów, czytaj kilka górnych. Starsze wpisy są w [archiwum](log_archive/2026.md); log nie zastępuje obowiązujących reguł |
| Co zostało wydane użytkownikom? | [CHANGELOG.md](CHANGELOG.md): techniczna historia wydań, nie bieżąca kolejka prac. Notatki dla użytkowników są w `app/src/main/assets/release_notes.json` ([STACK.md](STACK.md), sekcja „Wydania i licencja”) |
| Jakie braki potwierdzono w obecnym kodzie? | [KNOWN_ISSUES.md](KNOWN_ISSUES.md): otwarte problemy i brakujący odbiór, nie docelowa specyfikacja |
| Jak pisać komunikaty i dokumentację? | [WRITING.md](WRITING.md): język, styl i zasady redakcyjne |
| Jak recenzować commit albo etap planu? | [mak-code-review](../.cursor/skills/mak-code-review/SKILL.md): kryteria recenzji i podział pracy z Composerem |
| Czy dokumenty są spójne (linki, limity planu i logu, kolejka)? | `scripts/check_map.py`; testy `scripts/test_check_map.py`. Uruchomienie: [AGENTS.md](../AGENTS.md) i [WORKFLOW.md](WORKFLOW.md) |

`AGENTS.md` zawiera zasady pracy, `README.md` jest krótkim punktem wejścia, a `KNOWN_ISSUES.md` opisuje potwierdzone braki. Mapa nie zastępuje żadnego z tych dokumentów.
