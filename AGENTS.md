# Zasady pracy agentów

Te zasady obowiązują w całym repozytorium. Szczegóły produktu znajdują się w dokumentacji, a nie w tym pliku.

## Źródła prawdy

- `ARCHITECTURE.md` — kształt systemu, zakres, terminologia, zasady interfejsu i pytania otwarte.
- `STACK.md` — języki, narzędzia, środowisko, testy i odrzucone alternatywy.
- `CHANGELOG.md` — zmiany wydane użytkownikom.
- `JOURNAL.md` — fakty, decyzje, uzasadnienia i odrzucone alternatywy.
- `plan.md` — zaakceptowany plan produktu i przyszłych etapów.
- `WRITING.md` — zasady pisania dokumentacji i tekstów dla użytkownika.
- `README.md` — punkt wejścia do dokumentacji; nie zastępuje źródeł prawdy.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Identyfikatory i komentarze w kodzie: angielski.

## Pisanie

Stosuj `WRITING.md` do dokumentacji i tekstów dla użytkownika, chyba że bezpośrednia instrukcja użytkownika albo zaakceptowana zasada produktu stanowi inaczej.

Nie używaj w nowych dokumentach, tekstach interfejsu ani odpowiedziach znaku `·`, em dash `—`, emotek ani ozdobników i schematycznych zwrotów kojarzonych z AI-slop. Wybieraj zwykłą interpunkcję i konkretne sformułowania.

Zasady interfejsu z `ARCHITECTURE.md` są kryteriami akceptacji dla każdego widoku. Sprawdzaj je razem z zachowaniem funkcjonalnym.

## Interfejs i mockup

- Przed zmianą interfejsu przeczytaj zasady UI w `ARCHITECTURE.md` i obejrzyj `mockup.html`.
- `mockup.html` jest aktualną referencją kierunku wizualnego i interakcji. Dokumenty pozostają źródłem prawdy dla zakresu i zachowania.
- Jeśli zaakceptowana zmiana istotnie wpływa na nawigację, układ, formularze albo widget, zaktualizuj także mockup, aby nie pokazywał starego przebiegu.
- Nie kopiuj kodu mockupu bezpośrednio do aplikacji. Odtwórz jego zachowanie przy użyciu komponentów i wzorców Compose.

## Decyzje

Przeczytaj odpowiednie sekcje `ARCHITECTURE.md` i `STACK.md` przed zmianą struktury, modułów, narzędzi lub metodologii.

Nie traktuj propozycji z rozmowy jako decyzji, dopóki użytkownik jej nie zaakceptuje i nie zostanie zapisana w odpowiednim pliku.

Nie implementuj elementów oznaczonych jako pytania otwarte lub poza zakresem bez decyzji użytkownika.

Po zaakceptowanej zmianie architektury, stosu lub zasad pracy zaktualizuj właściwy dokument i dodaj wpis do `JOURNAL.md`. `CHANGELOG.md` aktualizuj dopiero po wydaniu zmiany użytkownikom.

## Zmiany w kodzie

- Najpierw prześledź kod i przepływ danych, którego dotyczy zmiana.
- Używaj istniejących wzorców i bibliotek platformy przed dodaniem nowej abstrakcji lub zależności.
- Waliduj dane na granicach systemu i chroń dane użytkownika przed utratą.
- Dla nietrywialnej logiki zostaw najmniejszy sensowny test uruchamialny. Szczegóły: sekcja Testy.
- Traktuj dostępność, małe ekrany i stan błędu jako część implementacji.

## Testy

Agenci sprawdzają działanie aplikacji testami, które da się uruchomić lokalnie. Priorytet ma logika domenowa na JVM. Zakres wymagań testowych pozostaje w `STACK.md`; ta sekcja mówi, jak je realizować.

Po zmianie `WeekCalculator`, `ScheduleResolver`, `CollisionDetector`, eksportu JSON albo walidacji formularza uruchom `gradlew.bat test`. Nie czekaj na emulator.

Pisz testy razem z logiką, nie jako osobny etap. Jeden test na jedną regułę z `ARCHITECTURE.md`, `STACK.md` albo `JOURNAL.md`. Przykładowe testy szablonu usuń, gdy pojawią się prawdziwe.

### JVM

Pokryj czystym Kotlinem i `java.time`, bez Compose i Room:

- `WeekCalculator`: semestr od środka tygodnia, A/B, data poza semestrem, `ONE_WEEK`, `FROM_WEEK`, nakładanie korekt.
- `ScheduleResolver`: cykle, `ONCE`, odwołanie, zmiana, przeniesienie, przywrócenie, rozdział notatki wspólnej od notatki do daty.
- `CollisionDetector`: nakładka jest kolizją, stykanie godzin nie jest; kolizje po zmianach wystąpień.
- eksport JSON: `schemaVersion` i round-trip modelu.
- walidacja: nazwa, kierunek, godziny; koniec później niż start; zajęcia przechodzące przez północ są odrzucane.

Ten sam `ScheduleResolver` jest źródłem planu dla listy, kalendarza, ekranu „Dzisiaj” i widgetu. Nie powielaj reguł w testach widoków.

Nie pisz testów rozstrzygających pytania, które nadal pozostają otwarte w `ARCHITECTURE.md`.

### Room

Warstwa bazy jest cienka. Nie pisz testów CRUD dla każdego DAO.

Wystarczy:

- jeden test, że zapisany semestr, korekta, `OccurrenceChange` i `OccurrenceNote` wracają po nowej instancji bazy;
- test migracji dopiero przy rzeczywistej zmianie schematu, na zachowanych danych.

Nie testuj osobno widgetu Glance. Sprawdź, że widget woła ten sam `ScheduleResolver`.

### Compose

Emulator jest wolny. Zostaw kilka przebiegów z wstrzykniętą datą, nie `LocalDate.now()`:

- brak aktywnego semestru;
- ekran „Dzisiaj” po dodaniu semestru, kierunku i zajęć;
- odwołanie i przywrócenie jednego terminu;
- notatka do zajęć kontra notatka do daty;
- szerokość 320 px: brak poziomego przewijania, akcje widoczne.

Kontrast, `reduced motion` i motyw ciemny sprawdzaj w kodzie i na mockupie, dopóki nie ma stałego urządzenia w CI.

## Git

Po zakończeniu zadania możesz samodzielnie utworzyć commit obejmujący jego logiczną zmianę. Wypychaj zmiany, twórz gałęzie i zmieniaj historię tylko na wyraźne polecenie użytkownika.

Nie dodawaj stopki `Co-authored-by:` przypisującej pracę agentowi.

Używaj krótkiego, konkretnego tematu w trybie rozkazującym. Dozwolone są prefiksy `feat:`, `fix:`, `chore:` i `docs:`.

## Zakres

Nie przywracaj alternatyw odrzuconych w `STACK.md` lub `JOURNAL.md`. Nie rozszerzaj zakresu poza `ARCHITECTURE.md`, aby pozornie domknąć pracę.

## Pliki narzędziowe

Zasady pracy trzymaj w `AGENTS.md`. `CLAUDE.md` zawiera wyłącznie import tego pliku.
