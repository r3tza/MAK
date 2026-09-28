# Znane problemy

Stan na 2026-09-28. Ten rejestr obejmuje otwarte problemy potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`, a zadania i statusy `QUEUE.md`. Naprawione problemy nie są tu powtarzane; ich historia jest w sekcji „Zakończone” w `QUEUE.md` i w `LOG.md`.

## Otwarte problemy

1. **Systemowa kopia zapasowa (I-14).** Aplikacja ma `allowBackup="true"`, a reguły kopii to szablony, więc baza i ustawienia mogą trafiać do kopii Google. Czeka na decyzję użytkownika.
2. **Karta podsumowania w motywie ciemnym (I-63).** Karta „Dzisiaj” ma w obu motywach ten sam gradient, więc w motywie ciemnym jest najjaśniejszym elementem ekranu, a czerwona i zielona liczba mają na niej kontrast 3,3:1 i 3,5:1. Użytkownik przyjął 2026-09-29 ciemniejszy wariant (`FEATURES.md`, sekcja „Ekran Dzisiaj”); czeka na implementację. Pozostałe dwie kwestie z audytu (obramowane przyciski ikon w górnym pasku i trzy style zaznaczenia na „Planie”) użytkownik zamknął bez zmian (`LOG.md`, 2026-09-29).

## Wymagają odbioru na urządzeniu

Poniższe pozycje mają gotowy kod; brakuje potwierdzenia na urządzeniu. Szczegóły scenariuszy są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.

1. **Podsumowanie „Dzisiaj”.** Kod istnieje: `countGaps` i `uniqueCollisionCount` są w domenie, próg jest w `SettingsPreferences` (domyślnie 30 minut), a `MakSummaryCard` pokazuje trzy kolumny „Zajęcia”, „Kolizje” i „Okienka”. Do potwierdzenia (O-05): trzy etykiety i liczby, zera przy pustym dniu, brak karty bez aktywnego semestru, 320 dp, motyw jasny i ciemny oraz TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ekran Dzisiaj”; `DOMAIN.md`, sekcja „Okienka”.
2. **Karty zajęć.** Kod istnieje: `ClassItemUi` ma osobne `classNote` i `occurrenceNote`, karta pokazuje kierunek jako tekst w jego kolorze, stan jako ikonę oraz oba rodzaje notatek jako osobne wiersze z ikoną i treścią. Do potwierdzenia (O-05): zawijanie długich nazw i notatek przy 320 dp, kontrast, kolejność sekcji oraz TalkBack bez spłaszczania notatek. Szczegóły: `FEATURES.md`, sekcja „Struktura karty zajęć”.
3. **Sekcje rozwijane i odstępy.** Kod istnieje: `MakExpandableSection` używa jednego neutralnego tła `surfaceContainerLow` w obu stanach, treść ma 16 dp paddingu, a obramowanie jest subtelne; przyciski pełnej szerokości mają co najmniej 12 dp odstępu. Do potwierdzenia (O-05): kontrast i odstępy w motywie jasnym i ciemnym, 320 dp oraz TalkBack. Szczegóły: `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
4. **Ustawienia.** Kod istnieje: ekran główny ma neutralne sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane”, „Aktualizacje” i „O aplikacji” bez lokalnego nagłówka i rozwijanych formularzy, a „Semestry”, „Powiadomienia” i „Dane” są osobnymi trasami w jednym `NavHost`. Do potwierdzenia (O-05): sekcje i odstępy, powrót systemowy, brak resetu stanu po powrocie, 320 dp, motyw ciemny i TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ustawienia i dane”.
5. **Widget.** Kod istnieje: nagłówek z datą 16 sp i tekstem tygodnia oraz liczby zajęć, wiersze z paskiem koloru kierunku, stałą kolumną czasu, nazwą, metadanymi, krótkim alertem kolizji, fazy „Teraz” i „Następne” oraz treścią notatek z ikonami w wariantach rozszerzonych. Do potwierdzenia (O-06): mały, pośredni i duży rozmiar na launcherze, zmiana rozmiaru, motyw jasny i ciemny, kliknięcie oraz odświeżenie po zmianie danych. Szczegóły: `FEATURES.md`, sekcja „Widget”.
6. **Poprawki audytu interfejsu z 2026-09-28 (I-54, I-56, I-57, I-58, I-60, I-61).** Kod istnieje: role komunikatów `MakNoteBanner` i przełączniki powiadomień; „Dzisiaj” i „Plan” bez nadtytułów, z jednym polem „Kierunek” i akcją „Dodaj termin jednorazowy” nad listą dnia; znaczniki kalendarza w kolorze kierunku z pierścieniem dla zmian i plusem od sześciu zajęć; kolizja z nazwą drugich zajęć na karcie i w szczegółach oraz pomarańczowy alert w widgecie; osobne ekrany dodawania i edycji kierunku w semestrze; paleta z niezależnym odcieniem i jasnością oraz kolor kierunku dopasowywany do tła przy rysowaniu. Do potwierdzenia (O-05, O-06): 320 dp, motyw jasny i ciemny, skala czcionki 2,0, TalkBack oraz widget na launcherze. Szczegóły: wiersze tych zadań w `QUEUE.md`.
7. **Spójność wzorców i poprawki po przeglądzie (I-55, I-59, I-62).** Kod istnieje: numer dnia w kalendarzu ma zawsze 13 sp, a komórka rośnie od 48 dp; porzucony szkic „Edytuj kierunek” nie wraca po ponownym wejściu; wiersze konfiguracji semestru nie są nagłówkami; karta zajęć ma pionowy separator; pięć dialogów usuwania używa `MakConfirmDeletionDialog`; „Anuluj” stoi przed zapisem w formularzu korekty; daty kalendarzy, nagłówek dnia i opcje „Tydzień A/B” w kreatorze są czytelne; „O aplikacji” ma kartę tylko przy historii wydań; godzina w widgecie nie używa akcentu. Do potwierdzenia (O-05, O-06): 320 dp, motyw jasny i ciemny, skala czcionki 2,0, TalkBack w dialogach usuwania, wyjście z edycji kierunku gestem wstecz oraz widget na launcherze. Szczegóły: wiersze tych zadań w `QUEUE.md`.

Wspólne sprawdzenia:

- TalkBack, kolejność i widoczność focusu, obsługa klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Obszary dotyku, kontrast małych tekstów i ikon, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
