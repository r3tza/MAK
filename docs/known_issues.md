# Znane problemy interfejsu

Stan na 2026-09-22. Ten rejestr obejmuje braki potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `plan.md` i `ARCHITECTURE.md`. Nie traktować poniższych punktów jako wyniku testu na urządzeniu.

## Wymagają implementacji

1. **Podsumowanie „Dzisiaj”.** `TodayViewModel` przekazuje tylko liczbę zajęć, a `MakSummaryCard` pokazuje jedną wartość. Brakuje trzech kolumn z zajęciami, unikalnymi kolizjami i okienkami, domenowego liczenia okienek ze wszystkich kierunków oraz globalnego progu w ustawieniach. Szczegóły: `plan.md`, sekcja 4.
2. **Karty zajęć.** Istnieją kolumna czasu, pasek koloru kierunku i pill, lecz `PlanMapping.kt` scala notatkę do zajęć z notatką do daty w jedno pole. Brakuje dwóch oznaczonych rodzajów notatek, wyraźniejszego podziału metadanych i separatorów sekcji. Szczegóły: `plan.md`, sekcja 4.
3. **Sekcje rozwijane i odstępy.** `MakExpandableSection` nadal używa `secondaryContainer` po rozwinięciu. Docelowo oba stany mają neutralne tło, a kontrolki i przyciski zachowują ustalone odstępy. Szczegóły: `plan.md`, sekcja 1.2.
4. **Ustawienia.** Główny ekran nadal zawiera powtórzony nagłówek, zarządzanie semestrem oraz rozwijane bloki danych i powiadomień. Brakuje krótkich sekcji i osobnych ekranów „Semestry”, „Powiadomienia” i „Dane”. Szczegóły: `plan.md`, sekcja 14.
5. **Widget.** Bazowy układ, separatory i licznik kolizji istnieją, ale duży wariant nie wykorzystuje w pełni miejsca. Brakuje oddzielnego wiersza kierunku, lekkiego ostrzeżenia o kolizji, odrębnego nagłówka i stanów „Teraz” oraz „Następne”. Szczegóły: `plan.md`, sekcje 13.1 i 13.2.

## Wymagają odbioru na urządzeniu

- Sprawdzić TalkBack, kolejność i widoczność focusu, obsługę klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Sprawdzić obszary dotyku, kontrast małych tekstów i pilli, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Sprawdzić widget na launcherze w małym, pośrednim i dużym rozmiarze, w obu motywach i z długimi nazwami.
- Sprawdzić skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach. Pełna lista scenariuszy jest w `plan.md`, sekcja 1.2.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
