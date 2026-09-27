# Znane problemy interfejsu

Stan na 2026-09-27. Ten rejestr obejmuje braki potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`. Nie traktować poniższych punktów jako wyniku testu na urządzeniu.

## Błędy potwierdzone audytem kodu z 2026-09-23

Zadania i kryteria są w `QUEUE.md`, a kroki wykonawcze w `PLAN.md`.

1. **Systemowa kopia zapasowa (I-14).** Baza i ustawienia mogą trafiać do kopii Google. Czeka na decyzję.

## Problemy potwierdzone audytem kodu z 2026-09-27

Audyt objął przegląd domeny, repozytoriów, importu, powiadomień, widgetu i ViewModeli, `gradlew.bat test` (333 testy, 0 błędów) oraz lint (0 błędów, 4 ostrzeżenia o wersjach, `targetSdk` i grafice podglądu widgetu). Zadania i kryteria są w `QUEUE.md`, a kroki w `PLAN.md`.

1. **Konfiguracja semestru aktywuje go (I-23).** Naprawione 2026-09-27: konfiguracja nie zmienia aktywnego semestru i zachowuje niezapisane zmiany po powrocie z podekranów.
2. **Zmiana tygodnia bez zapisu (I-24).** Naprawione 2026-09-27: wiersz tygodnia nie jest akcją, gdy nie ma jednego kalendarza do korekty.
3. **Odwołane terminy w „Planie” (I-25).** Naprawione 2026-09-27: reguła w domenie, filtr kierunku, licznik bez odwołanych.
4. **Drobne błędy (I-26).** Ponowne przejście z powiadomienia albo widgetu po przywróceniu procesu, pusty ekran szczegółów usuniętych zajęć, błędny komunikat przy zapisie progu okienka, równoległe odświeżanie alarmów i nieusuwany kalendarz po przepięciu przypisania.

## Problemy potwierdzone audytem interfejsu z 2026-09-27

Audyt objął przegląd wspólnych komponentów i ekranów, obliczenie kontrastu kolorów motywu oraz zrzuty z emulatora (Android 16) przy 320 dp, skalach czcionki 1,0, 1,3 i 2,0, w motywie jasnym i ciemnym, i przejście klawiszem Tab. TalkBack nie był uruchamiany; semantykę sprawdzono w kodzie.

1. **Focus klawiatury (I-27).** Naprawione 2026-09-27: jeden przystanek Tab z ramką 2 dp; sprawdzone na emulatorze.
2. **Powiększony tekst i 320 dp (I-28).** Naprawione 2026-09-27; sprawdzone na zrzutach przy skalach 1,0, 1,3 i 2,0.
3. **Semantyka i kontrast kart (I-29).** Naprawione 2026-09-27; odczyt TalkBackiem na urządzeniu należy do O-05.
4. **Fałszywy stan pusty przy starcie (I-30).** Naprawione 2026-09-27: ekran ładowania z pełną nazwą i ekran startowy w motywie aplikacji.
5. **Granice kontrolek (I-31).** Naprawione 2026-09-27; motyw ciemny do obejrzenia na urządzeniu (O-05).
6. **Nazwy notatek (I-32).** Naprawione 2026-09-27: „Notatka do zajęć” i „Notatka do terminu”.
7. **Rozmiary dni i widgetu (I-33).** Wyjątki zapisane i wdrożone 2026-09-27; widget do sprawdzenia na launcherze (O-06).

Rozbieżność do rozstrzygnięcia przy odbiorze O-05: `FEATURES.md` („Struktura karty zajęć”) wymaga subtelnego pionowego separatora między kolumną godzin a danymi, a karta go nie ma.


## Wymagają odbioru na urządzeniu

Poniższe pozycje mają gotowy kod; brakuje potwierdzenia na urządzeniu. Szczegóły scenariuszy są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.

1. **Podsumowanie „Dzisiaj”.** Kod istnieje: `countGaps` i `uniqueCollisionCount` są w domenie, próg jest w `SettingsPreferences` (domyślnie 30 minut), a `MakSummaryCard` pokazuje trzy kolumny „Zajęcia”, „Kolizje” i „Okienka”. Do potwierdzenia (O-05): trzy etykiety i liczby, zera przy pustym dniu, brak karty bez aktywnego semestru, 320 dp, motyw jasny i ciemny oraz TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ekran Dzisiaj”; `DOMAIN.md`, sekcja „Okienka”.
2. **Karty zajęć.** Kod istnieje: `ClassItemUi` ma osobne `classNote` i `occurrenceNote`, karta ma sekcje z cienkimi separatorami, pill kierunku w kolorze kierunku i dwa oznaczone wiersze notatek („Notatka do zajęć”, „Notatka do terminu”), a semantyka czyta obie notatki z etykietami. Do potwierdzenia (O-05): zawijanie długich nazw i notatek przy 320 dp, kontrast pilli i etykiet, kolejność sekcji oraz TalkBack bez spłaszczania notatek. Szczegóły: `FEATURES.md`, sekcja „Struktura karty zajęć”.
3. **Sekcje rozwijane i odstępy.** Kod istnieje: `MakExpandableSection` używa jednego neutralnego tła `surfaceContainerLow` w obu stanach, treść ma 16 dp paddingu, a obramowanie jest subtelne; przyciski pełnej szerokości mają co najmniej 12 dp odstępu. Do potwierdzenia (O-05): kontrast i odstępy w motywie jasnym i ciemnym, 320 dp oraz TalkBack. Szczegóły: `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
4. **Ustawienia.** Kod istnieje: ekran główny ma neutralne sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane” i „O aplikacji” bez lokalnego nagłówka i rozwijanych formularzy, a „Semestry”, „Powiadomienia” i „Dane” są osobnymi trasami w jednym `NavHost`. Do potwierdzenia (O-05): sekcje i odstępy, powrót systemowy, brak resetu stanu po powrocie, 320 dp, motyw ciemny i TalkBack. Szczegóły: `FEATURES.md`, sekcja „Ustawienia i dane”.
5. **Widget.** Kod istnieje: nagłówek z datą 16 sp, etykietą tygodnia i liczbą zajęć, wiersze z paskiem koloru kierunku, stałą kolumną czasu, nazwą, metadanymi (kierunek, sala, prowadzący), krótkim alertem kolizji i etykietą notatki, fazy „Teraz”/„Następne”, separatory oraz warianty kompaktowy i rozszerzony. Do potwierdzenia (O-06): mały, pośredni i duży rozmiar na launcherze, zmiana rozmiaru, motyw jasny i ciemny, kliknięcie oraz odświeżenie po zmianie danych. Szczegóły: `FEATURES.md`, sekcja „Widget”.

Wspólne sprawdzenia:

- TalkBack, kolejność i widoczność focusu, obsługa klawiatury, powrót systemowy oraz powrót focusu po zamknięciu dialogów.
- Obszary dotyku, kontrast małych tekstów i pilli, motyw ciemny, długie etykiety oraz brak obciętych akcji i poziomego przewijania przy 320 dp.
- Skutki importu, konfiguracji kalendarzy i powiadomień w rzeczywistych przepływach.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
