# Znane problemy

Stan na 2026-09-28. Ten rejestr obejmuje otwarte problemy potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`, a zadania i statusy `QUEUE.md`. Naprawione problemy nie są tu powtarzane; ich historia jest w sekcji „Zakończone” w `QUEUE.md` i w `LOG.md`.

## Otwarte problemy

1. **Systemowa kopia zapasowa (I-14).** Aplikacja ma `allowBackup="true"`, a reguły kopii to szablony, więc baza i ustawienia mogą trafiać do kopii Google. Czeka na decyzję użytkownika.
2. **Separator w karcie zajęć (O-05).** `FEATURES.md` („Struktura karty zajęć”) wymaga subtelnego pionowego separatora między kolumną godzin a danymi, a karta go nie ma. Do rozstrzygnięcia przy odbiorze: dodać separator albo zmienić opis.
3. **Role komunikatów (I-54).** `MakNoteBanner` używa akcentowego `primaryContainer` dla zwykłych instrukcji, informacji o stanie, ostrzeżeń, blokady systemowej i błędów aktualizacji. Kolor nie ma przez to stałego znaczenia, a zwykła informacja dostaje nieuzasadnioną wagę.
4. **Nadmiar kart na ekranie „O aplikacji” (I-55).** Sekcje „Możliwości” oraz „Dane i prywatność” mają jednocześnie nagłówki, duże odstępy i osobne obramowane karty. Hierarchia nie wymaga wszystkich trzech środków naraz.

## Wymagają odbioru na urządzeniu

Poniższe pozycje mają gotowy kod; brakuje potwierdzenia na urządzeniu. Szczegóły scenariuszy są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.

1. **Podsumowanie „Dzisiaj”.** Kod istnieje: `countGaps` i `uniqueCollisionCount` są w domenie, próg jest w `SettingsPreferences` (domyślnie 30 minut), a `MakSummaryCard` pokazuje trzy kolumny „Zajęcia”, „Kolizje” i „Okienka”. Do potwierdzenia (O-05): trzy etykiety i liczby, zera przy pustym dniu, brak karty bez aktywnego semestru, 320 dp, motyw jasny i ciemny oraz TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ekran Dzisiaj”; `DOMAIN.md`, sekcja „Okienka”.
2. **Karty zajęć.** Kod istnieje: `ClassItemUi` ma osobne `classNote` i `occurrenceNote`, karta pokazuje kierunek jako tekst w jego kolorze, stan jako ikonę oraz oba rodzaje notatek jako osobne wiersze z ikoną i treścią. Do potwierdzenia (O-05): zawijanie długich nazw i notatek przy 320 dp, kontrast, kolejność sekcji oraz TalkBack bez spłaszczania notatek. Szczegóły: `FEATURES.md`, sekcja „Struktura karty zajęć”.
3. **Sekcje rozwijane i odstępy.** Kod istnieje: `MakExpandableSection` używa jednego neutralnego tła `surfaceContainerLow` w obu stanach, treść ma 16 dp paddingu, a obramowanie jest subtelne; przyciski pełnej szerokości mają co najmniej 12 dp odstępu. Do potwierdzenia (O-05): kontrast i odstępy w motywie jasnym i ciemnym, 320 dp oraz TalkBack. Szczegóły: `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
4. **Ustawienia.** Kod istnieje: ekran główny ma neutralne sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane”, „Aktualizacje” i „O aplikacji” bez lokalnego nagłówka i rozwijanych formularzy, a „Semestry”, „Powiadomienia” i „Dane” są osobnymi trasami w jednym `NavHost`. Do potwierdzenia (O-05): sekcje i odstępy, powrót systemowy, brak resetu stanu po powrocie, 320 dp, motyw ciemny i TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ustawienia i dane”.
5. **Widget.** Kod istnieje: nagłówek z datą 16 sp i tekstem tygodnia oraz liczby zajęć, wiersze z paskiem koloru kierunku, stałą kolumną czasu, nazwą, metadanymi, krótkim alertem kolizji, fazy „Teraz” i „Następne” oraz treścią notatek z ikonami w wariantach rozszerzonych. Do potwierdzenia (O-06): mały, pośredni i duży rozmiar na launcherze, zmiana rozmiaru, motyw jasny i ciemny, kliknięcie oraz odświeżenie po zmianie danych. Szczegóły: `FEATURES.md`, sekcja „Widget”.

Wspólne sprawdzenia:

- TalkBack, kolejność i widoczność focusu, obsługa klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Obszary dotyku, kontrast małych tekstów i ikon, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
