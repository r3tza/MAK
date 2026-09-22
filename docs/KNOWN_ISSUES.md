# Znane problemy interfejsu

Stan na 2026-09-22. Ten rejestr obejmuje braki potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`. Nie traktować poniższych punktów jako wyniku testu na urządzeniu.

## Wymagają implementacji

1. **Odbiór podsumowania „Dzisiaj”.** Kod istnieje: `countGaps` i `uniqueCollisionCount` są w domenie, próg jest w `SettingsPreferences` (domyślnie 30 minut), a `MakSummaryCard` pokazuje trzy kolumny „Zajęcia”, „Kolizje” i „Okienka”. Pozostaje odbiór na urządzeniu (O-05): trzy etykiety i liczby, zera przy pustym dniu, brak karty bez aktywnego semestru, 320 dp, motyw jasny i ciemny oraz TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ekran Dzisiaj”; `DOMAIN.md`, sekcja „Okienka”.
2. **Karty zajęć.** Istnieją kolumna czasu, pasek koloru kierunku i pill, lecz `PlanMapping.kt` scala notatkę do zajęć z notatką do daty w jedno pole. Brakuje dwóch oznaczonych rodzajów notatek, wyraźniejszego podziału metadanych i separatorów sekcji. Szczegóły: `FEATURES.md`, sekcja „Struktura karty zajęć”.
3. **Sekcje rozwijane i odstępy.** `MakExpandableSection` nadal używa `secondaryContainer` po rozwinięciu. Docelowo oba stany mają neutralne tło, a kontrolki i przyciski zachowują ustalone odstępy. Szczegóły: `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
4. **Ustawienia.** Główny ekran nadal zawiera powtórzony nagłówek, zarządzanie semestrem oraz rozwijane bloki danych i powiadomień. Brakuje krótkich sekcji i osobnych ekranów „Semestry”, „Powiadomienia” i „Dane”. Szczegóły: `FEATURES.md`, sekcja „Ustawienia i dane”.
5. **Widget.** Bazowy układ, separatory i licznik kolizji istnieją, ale duży wariant nie wykorzystuje w pełni miejsca. Brakuje oddzielnego wiersza kierunku, lekkiego ostrzeżenia o kolizji, odrębnego nagłówka i stanów „Teraz” oraz „Następne”. Szczegóły: `FEATURES.md`, sekcja „Widget”.

## Wymagają odbioru na urządzeniu

- Sprawdzić TalkBack, kolejność i widoczność focusu, obsługę klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Sprawdzić obszary dotyku, kontrast małych tekstów i pilli, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Sprawdzić widget na launcherze w małym, pośrednim i dużym rozmiarze, w obu motywach i z długimi nazwami.
- Sprawdzić skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach. Pełna lista scenariuszy jest w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
