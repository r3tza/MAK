# Znane problemy interfejsu

Stan na 2026-09-22. Ten rejestr obejmuje braki potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`. Nie traktować poniższych punktów jako wyniku testu na urządzeniu.

## Wymagają implementacji

1. **Odbiór podsumowania „Dzisiaj”.** Kod istnieje: `countGaps` i `uniqueCollisionCount` są w domenie, próg jest w `SettingsPreferences` (domyślnie 30 minut), a `MakSummaryCard` pokazuje trzy kolumny „Zajęcia”, „Kolizje” i „Okienka”. Pozostaje odbiór na urządzeniu (O-05): trzy etykiety i liczby, zera przy pustym dniu, brak karty bez aktywnego semestru, 320 dp, motyw jasny i ciemny oraz TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ekran Dzisiaj”; `DOMAIN.md`, sekcja „Okienka”.
2. **Odbiór kart zajęć.** Kod istnieje: `ClassItemUi` ma osobne `classNote` i `occurrenceNote`, karta ma sekcje z cienkimi separatorami, pill kierunku w kolorze kierunku i dwa oznaczone wiersze notatek („Notatka do zajęć”, „Notatka na dziś”), a semantyka czyta obie notatki z etykietami. Pozostaje odbiór na urządzeniu (O-05): zawijanie długich nazw i notatek przy 320 dp, kontrast pilli i etykiet, kolejność sekcji oraz TalkBack bez spłaszczania notatek. Szczegóły: `FEATURES.md`, sekcja „Struktura karty zajęć”.
3. **Odbiór sekcji rozwijanych i odstępów.** Kod istnieje: `MakExpandableSection` używa jednego neutralnego tła `surfaceContainerLow` w obu stanach, treść ma 16 dp paddingu, a obramowanie jest subtelne; przyciski pełnej szerokości mają co najmniej 12 dp odstępu. Pozostaje odbiór na urządzeniu (O-05): kontrast i odstępy w motywie jasnym i ciemnym, 320 dp oraz TalkBack. Szczegóły: `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
4. **Odbiór ustawień.** Kod istnieje: ekran główny ma neutralne sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane” i „O aplikacji” bez lokalnego nagłówka i rozwijanych formularzy, a „Semestry”, „Powiadomienia” i „Dane” są osobnymi trasami w jednym `NavHost`. Pozostaje odbiór na urządzeniu (O-05): sekcje i odstępy, powrót systemowy, brak resetu stanu po powrocie, 320 dp, motyw ciemny i TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ustawienia i dane”.
5. **Widget.** Bazowy układ, separatory i licznik kolizji istnieją, ale duży wariant nie wykorzystuje w pełni miejsca. Brakuje oddzielnego wiersza kierunku, lekkiego ostrzeżenia o kolizji, odrębnego nagłówka i stanów „Teraz” oraz „Następne”. Szczegóły: `FEATURES.md`, sekcja „Widget”.

## Wymagają odbioru na urządzeniu

- Sprawdzić TalkBack, kolejność i widoczność focusu, obsługę klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Sprawdzić obszary dotyku, kontrast małych tekstów i pilli, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Sprawdzić widget na launcherze w małym, pośrednim i dużym rozmiarze, w obu motywach i z długimi nazwami.
- Sprawdzić skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach. Pełna lista scenariuszy jest w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
