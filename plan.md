# MAK — plan aplikacji

## 1. Cel i zakres

MAK (Mobilny Akademicki Kalendarz) to lekka aplikacja mobilna na Androida do zarządzania planem zajęć, szczególnie przy studiowaniu na dwóch lub większej liczbie kierunków.

Główne założenia:

- szybki podgląd zajęć na dziś;
- prosty plan tygodniowy;
- obsługa wielu kierunków;
- automatyczne tygodnie A/B z ręcznymi korektami;
- wykrywanie kolizji;
- widget na ekranie głównym;
- działanie całkowicie offline;
- małe zużycie baterii;
- brak kont, logowania, backendu i synchronizacji w chmurze.

Po jednorazowym skonfigurowaniu planu użytkownik powinien korzystać głównie z ekranu „Dzisiaj” i widgetu.

## 2. Technologie

- Kotlin;
- Jetpack Compose;
- Room nad SQLite;
- Jetpack Glance do widgetu;
- kotlinx.serialization do importu i eksportu JSON;
- `java.time` do dat, godzin i obliczania tygodni.

Aplikacja przechowuje wszystkie dane lokalnie. Nie wymaga dostępu do internetu.

## 3. Nawigacja

Dolny pasek zawiera trzy pozycje:

- **Dzisiaj** — zajęcia z bieżącego dnia;
- **Plan** — tygodniowy plan zajęć;
- **Dodaj** — formularz nowych zajęć.

Ustawienia są dostępne z menu w prawym górnym rogu. Kliknięcie zajęć na ekranie „Dzisiaj” lub „Plan” otwiera ekran szczegółów i edycji.

## 4. Ekran „Dzisiaj”

To domyślny ekran otwierany po uruchomieniu aplikacji.

Powinien pokazywać:

- dzień tygodnia i pełną datę;
- liczbę zajęć;
- wszystkie aktywne zajęcia w kolejności od najwcześniejszego;
- kolor kierunku;
- oznaczenie kolizji, gdy ta funkcja zostanie dodana;
- stan pusty, gdy danego dnia nie ma zajęć.

Przykładowy element:

```text
08:00–09:30
Programowanie
Laboratorium · Informatyka
Sala L204 · dr Jan Kowalski
```

Po przekroczeniu północy ekran pokazuje plan nowego dnia przy ponownym otwarciu lub wznowieniu aplikacji. Odczyt aktualnej daty nie wymaga ciągłego działania aplikacji w tle.

## 5. Ekran „Plan”

Ekran oferuje dwa równorzędne sposoby przeglądania planu: **Lista** i **Kalendarz**. Ostatnio wybrany sposób może być zapamiętany lokalnie.

### Widok „Lista”

Widok listy na telefonie składa się z:

- wyboru dnia: poniedziałek–niedziela;
- przesuwania między dniami gestem;
- przechodzenia między tygodniami oraz powrotu do bieżącego tygodnia;
- daty i oznaczenia A/B widocznego przy przeglądanym tygodniu;
- akcji „Zmień tydzień A/B”, z wyborem zakresu „Tylko ten tydzień” lub „Od tego tygodnia”;
- filtrów: „Wszystkie” oraz poszczególne kierunki;
- listy zajęć posortowanej według godziny;
- oznaczeń kolizji, gdy ta funkcja zostanie dodana.

Plan uwzględnia aktualny semestr, automatyczne lub ręcznie skorygowane oznaczenie A/B, zmiany pojedynczych wystąpień oraz zajęcia jednorazowe. Ręcznie skorygowany tydzień jest wyraźnie oznaczony; użytkownik może usunąć korektę i wrócić do automatycznego wyniku.

### Widok „Kalendarz”

Widok kalendarza pokazuje jeden miesiąc i zawiera:

- przejście do poprzedniego i następnego miesiąca;
- powrót do bieżącej daty;
- oznaczenie liczby lub kolorów zajęć w każdym dniu;
- czytelne oznaczenie dni z odwołanymi, zmienionymi lub jednorazowymi zajęciami;
- wybór dnia i listę jego aktywnych zajęć pod kalendarzem;
- akcję dodania nowych zajęć jednorazowych dla wybranej daty.

Kalendarz pokazuje wynik `ScheduleResolver`, dlatego musi być zgodny z ekranem „Dzisiaj”, listą planu i widgetem. Odwołane zajęcia mogą pozostać widoczne jako przekreślone tylko wtedy, gdy użytkownik włączy opcję „Pokaż odwołane”. Domyślnie kalendarz pokazuje plan aktywny.

## 6. Dodawanie i edycja zajęć

Formularz powinien zawierać:

### Podstawowe dane

- nazwa przedmiotu;
- typ: wykład, ćwiczenia, laboratorium, projekt, seminarium lub inne;
- kierunek;
- prowadzący.

### Termin

- dzień tygodnia;
- godzina rozpoczęcia;
- godzina zakończenia;
- powtarzanie: co tydzień, tydzień A, tydzień B lub jednorazowo;
- konkretna data, gdy zajęcia są jednorazowe.

### Lokalizacja i dodatkowe dane

- sala;
- budynek;
- grupa — pole opcjonalne;
- notatka do zajęć — opcjonalna, wspólna dla każdego wystąpienia tego wpisu;
- notatka do wybranego wystąpienia — opcjonalna, przypięta do jednej konkretnej daty zajęć.

Podczas dodawania notatki użytkownik wybiera zakres: „Do tych zajęć” albo „Tylko do tego terminu”. Notatkę do konkretnego wystąpienia można dodać z ekranu szczegółów zajęć na ekranie „Dzisiaj” lub „Plan”. Jeśli istnieją oba typy, aplikacja pokazuje je osobno i nie nadpisuje notatki wspólnej.

Prowadzący zapisani wcześniej są proponowani podczas wpisywania. Formularz powinien walidować, że nazwa, kierunek, godzina rozpoczęcia i zakończenia są uzupełnione, a godzina zakończenia jest późniejsza od rozpoczęcia.

Po usunięciu zajęć aplikacja powinna wymagać potwierdzenia. Edycja i usuwanie muszą aktualizować widget.

## 7. Semestr i tygodnie A/B

Ustawienia semestru są potrzebne od pierwszej wersji, ponieważ określają automatyczny rytm tygodni A/B.

Model semestru:

- nazwa, np. „Semestr zimowy 2026/27”;
- data rozpoczęcia;
- data zakończenia;
- oznaczenie pierwszego tygodnia: A lub B.

Kalkulator tygodnia powinien:

1. sprawdzić, czy data mieści się w zakresie semestru;
2. znaleźć poniedziałek tygodnia zawierającego datę rozpoczęcia semestru;
3. obliczyć liczbę pełnych tygodni od tego poniedziałku i naprzemiennie przypisywać A/B;
4. zastosować ostatnią korektę „Od tego tygodnia” obowiązującą dla wskazanego tygodnia, licząc naprzemienność od jej daty;
5. jeśli istnieje korekta „Tylko ten tydzień”, zastosować ją zamiast wyniku z poprzedniego kroku.

Każda korekta dotyczy jednego semestru i wskazuje poniedziałek tygodnia, oznaczenie A/B oraz zakres. Zmiana pojedyncza nie wpływa na kolejny tydzień. Zmiana przyszła zaczyna nową sekwencję; następna zmiana przyszła może ją ponownie przestawić. Dla jednego tygodnia i zakresu obowiązuje najwyżej jedna korekta, którą można edytować lub usunąć. Tydzień A/B jest wspólny dla wszystkich kierunków; zmiany konkretnych wystąpień są odrębną funkcją.

Przykład: pierwszy tydzień semestru to A, więc następny to B. Jeśli trzeci tydzień zostanie jednorazowo oznaczony B, czwarty nadal będzie B według automatycznej sekwencji. Jeśli trzeci tydzień zostanie oznaczony B „Od tego tygodnia”, czwarty będzie A.

Poza zakresem semestru aplikacja powinna jasno pokazać, że nie ma aktywnego semestru. Nie należy opierać działania wyłącznie na numerze tygodnia ISO, ponieważ uczelniana numeracja może zaczynać się w innym miejscu.

## 8. Kierunki

Model `Course`:

- `id`;
- `name`;
- `color`.

Aplikacja nie jest ograniczona do dwóch kierunków. Kolor kierunku jest widoczny na liście zajęć, w filtrach i w widgetach.

## 9. Prowadzący

Model `Teacher`:

- `id`;
- `name`.

Pola `email` i `academicTitle` można dodać później, ale nie są potrzebne w MVP. Warto zachować prowadzących jako osobną tabelę, aby autouzupełnianie nie tworzyło wielu kopii tej samej osoby.

## 10. Model danych zajęć

Model aplikacyjny `ClassItem` oraz odpowiadający mu `ClassEntity` powinien zawierać:

- `id`;
- `name`;
- `type`;
- `courseId`;
- `teacherId` — opcjonalne;
- `dayOfWeek`;
- `startTime`;
- `endTime`;
- `room` — opcjonalne;
- `building` — opcjonalne;
- `group` — opcjonalne;
- `recurrence`;
- `date` — używane dla zajęć jednorazowych;
- `classNote` — opcjonalna notatka wspólna dla wszystkich wystąpień;

`recurrence` przyjmuje wartości:

```text
EVERY_WEEK
A_WEEK
B_WEEK
ONCE
```

W kodzie warto używać nazwy `ClassItem` zamiast samego `Class`, ponieważ `Class` koliduje znaczeniowo z podstawowym typem Kotlin/Java i utrudnia czytanie kodu.

Model korekty tygodnia `WeekOverride` zawiera `id`, `semesterId`, `weekStartDate` (poniedziałek), `weekType` (`A` lub `B`) oraz `scope` (`ONE_WEEK` albo `FROM_WEEK`). Korekty są przechowywane osobno od zajęć i obejmowane eksportem.

Model notatki do konkretnego wystąpienia `OccurrenceNote` zawiera:

- `id`;
- `classId`;
- `occurrenceDate` — konkretna data zajęć;
- `body`.

W bazie obowiązuje najwyżej jedna notatka do danego wystąpienia zajęć. Notatka do zajęć jest przechowywana przy `ClassEntity`, a `OccurrenceNote` pozostaje osobną tabelą, aby zmiana jednej daty nie modyfikowała pozostałych wystąpień.

## 11. Zmiany pojedynczych wystąpień

Stały wpis tygodniowy nie wystarcza do obsługi odwołanych i przeniesionych zajęć. Model `OccurrenceChange` opisuje zmianę jednego terminu zajęć cyklicznych:

- `id`;
- `classId`;
- `originalDate` — data wystąpienia wynikająca z planu cyklicznego;
- `kind`: `CANCELLED` albo `MODIFIED`;
- `targetDate` — używane przy przeniesieniu na inną datę;
- opcjonalnie nowa godzina rozpoczęcia i zakończenia, sala, budynek, prowadzący oraz notatka.

Z ekranu szczegółów konkretnego terminu użytkownik może wybrać:

- „Odwołaj ten termin”;
- „Zmień tylko ten termin”;
- „Przenieś ten termin”;
- „Przywróć termin”, jeśli wcześniej zapisano zmianę.

Odrabianie albo inne dodatkowe spotkanie jest zapisywane jako `ClassEntity` z `recurrence = ONCE` i konkretną datą. Formularz może skopiować nazwę, kierunek, prowadzącego i typ z istniejących zajęć, ale zapis pozostaje niezależny od cyklu.

Zmiany wystąpień są stosowane po rozwinięciu planu cyklicznego i przed wykrywaniem kolizji. Usunięcie albo edycja zmiany nie modyfikuje bazowego wpisu zajęć.

## 12. Kolizje

Kolizja występuje, gdy dwa aktywne zajęcia tego samego dnia mają przedziały czasu, które się nakładają. Przedziały stykające się końcem i początkiem, np. 10:00–11:00 oraz 11:00–12:00, nie są kolizją.

Przykład:

```text
Programowanie  10:00–11:30
Matematyka     11:00–12:30
```

Wynik: kolizja trwająca 30 minut.

Kolizję należy oznaczyć czerwonym symbolem przy obu zajęciach i pokazać jej czas trwania po wejściu w szczegóły. Aplikacja wykrywa problem, ale nie próbuje go automatycznie rozwiązywać.

## 13. Widget

Widget pokazuje dzisiejsze zajęcia bez otwierania aplikacji.

### Mały widget

Pokazuje datę oraz jedno lub dwa najbliższe zajęcia, np. godzinę, nazwę, kierunek i salę.

### Duży widget

Pokazuje pełną listę dzisiejszych zajęć z godzinami, kierunkami, salami i opcjonalnie prowadzącymi.

Kliknięcie widgetu otwiera ekran „Dzisiaj”.

Widget powinien otrzymywać żądanie odświeżenia po:

- zmianie danych planu;
- dodaniu, edycji lub usunięciu zajęć;
- zmianie semestru, kierunku lub korekty tygodnia A/B;
- zmianie rozmiaru albo konfiguracji widgetu;
- okresowym sygnale systemu jako zabezpieczeniu przy zmianie daty.

System może opóźnić odświeżenie po północy. Widget nie obiecuje zmiany dokładnie o 00:00; przy każdym odświeżeniu odczytuje aktualną datę i aktywny plan. Nie należy używać ciągłego serwisu w tle, dokładnych alarmów ani odświeżania co minutę. Tekst „Następne: 10:15” jest wystarczający i ogranicza zużycie baterii. Układ musi dostosować liczbę widocznych zajęć do faktycznego rozmiaru widgetu.

## 14. Ustawienia i dane

Ustawienia powinny zawierać:

- konfigurację semestru i korekty tygodni A/B;
- listę kierunków i ich kolorów;
- eksport planu;
- import planu;
- informację o wersji aplikacji.

Eksport i import mogą używać lokalnego pliku JSON. Format powinien mieć pole `schemaVersion`, aby można było zmieniać model danych bez utraty zgodności ze starszymi eksportami.

Eksport jest dostępny od wersji 0.1. Użytkownik wybiera miejsce zapisu przez systemowy wybór dokumentu. Plik zawiera semestr, kierunki, prowadzących, zajęcia, korekty tygodni, notatki i zmiany wystąpień. Import pojawia się w wersji 0.3.

Import powinien:

- sprawdzić poprawność struktury pliku;
- pokazać podsumowanie danych przed zapisaniem;
- obsłużyć konflikt istniejących identyfikatorów;
- nie nadpisywać bieżącego planu bez potwierdzenia.

## 15. Modularność i orientacyjny podział kodu

Poniższy podział pokazuje odpowiedzialności. Nazwy plików i katalogów można dopasować podczas implementacji bez zmiany granic między danymi, logiką planu, interfejsem i widgetem.

```text
app/
├── data/
│   ├── database/
│   │   ├── AppDatabase
│   │   ├── ClassDao
│   │   ├── TeacherDao
│   │   ├── CourseDao
│   │   ├── SemesterDao
│   │   ├── WeekOverrideDao
│   │   ├── OccurrenceNoteDao
│   │   └── OccurrenceChangeDao
│   ├── entity/
│   │   ├── ClassEntity
│   │   ├── TeacherEntity
│   │   ├── CourseEntity
│   │   ├── SemesterEntity
│   │   ├── WeekOverrideEntity
│   │   ├── OccurrenceNoteEntity
│   │   └── OccurrenceChangeEntity
│   └── repository/
├── domain/
│   ├── WeekCalculator
│   ├── ScheduleResolver
│   └── CollisionDetector
├── ui/
│   ├── today/
│   ├── schedule/
│   ├── edit/
│   ├── settings/
│   └── components/
├── widget/
│   ├── TodayWidget
│   └── TodayWidgetReceiver
└── export/
```

Logika obliczania planu, tygodni i kolizji powinna być niezależna od Compose. Ułatwi to testowanie i zapewni spójne dane na ekranie oraz w widgetach. ViewModele należą do `ui`. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Pakiet `export` nie jest częścią Room. Szczegóły granic: `ARCHITECTURE.md`.

## 16. Kolejność wdrożenia

### Wersja 0.1 — działający plan offline

- podstawowa konfiguracja projektu;
- Room, eksport schematu bazy i przygotowanie testów przyszłych migracji;
- kierunki z kolorami;
- semestr, automatyczny kalkulator tygodni A/B oraz korekty pojedyncze i „Od tego tygodnia”;
- dodawanie, edycja i usuwanie zajęć;
- zajęcia cotygodniowe oraz przypisane do tygodnia A lub B;
- ekran „Dzisiaj”;
- ekran „Plan” z nawigacją między tygodniami i oznaczeniem korekt;
- widok miesięcznego kalendarza z wyborem dnia;
- filtrowanie po kierunku;
- odwoływanie, zmiana i przenoszenie pojedynczego wystąpienia;
- dodawanie zajęć jednorazowych, w tym odrabiania;
- notatki wspólne dla zajęć i notatki przypięte do konkretnych wystąpień;
- eksport JSON z `schemaVersion` przez systemowy wybór pliku;
- walidacja formularza, stany puste i podstawowe wymagania dostępności.

### Wersja 0.2 — szybki dostęp

- widget mały;
- widget duży;
- odświeżanie widgetu po zmianach danych;
- otwieranie ekranu „Dzisiaj” po kliknięciu;
- ponowny odczyt daty przy odświeżaniu oraz okresowe odświeżenie jako zabezpieczenie;
- układ widgetu dostosowany do dostępnego rozmiaru.

### Wersja 0.3 — kolizje i import danych

- wykrywanie kolizji;
- import JSON;
- odczyt obsługiwanych wersji formatu eksportu podczas importu.

## 17. Kryteria ukończenia MVP

MVP można uznać za gotowe, gdy użytkownik potrafi:

1. utworzyć semestr i co najmniej dwa kierunki;
2. dodać zajęcia z pełnymi podstawowymi danymi;
3. zobaczyć właściwe zajęcia dla bieżącej daty i automatycznie wyliczonego tygodnia A/B;
4. zmienić oznaczenie jednego tygodnia, ustawić zmianę od wskazanego tygodnia oraz usunąć korektę;
5. przejść do dowolnego dnia i tygodnia w zakresie semestru;
6. filtrować plan po kierunku;
7. edytować i usuwać wpisy;
8. przełączyć plan między listą i kalendarzem oraz wybrać dzień miesiąca;
9. odwołać, zmienić, przenieść i przywrócić pojedynczy termin bez zmiany cyklu;
10. dodać jednorazowe zajęcia dla konkretnej daty;
11. dodać notatkę do wszystkich wystąpień zajęć;
12. dodać inną notatkę tylko do wybranej daty;
13. zamknąć i ponownie otworzyć aplikację bez utraty danych;
14. wyeksportować plan wraz z korektami, zmianami wystąpień i notatkami do pliku JSON;
15. korzystać z aplikacji bez połączenia z internetem;
16. obsłużyć podstawowe czynności na ekranie o szerokości 320–390 px, bez obciętych akcji i poziomego przewijania, z etykietami semantycznymi, widocznym focusem oraz bez utraty wpisanych danych.

## 18. Funkcje poza pierwszym zakresem

Na początku nie dodawać:

- logowania i kont użytkowników;
- backendu, Firebase i synchronizacji w chmurze;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu.

## 19. Sugestie projektowe

1. **Utrzymać semestr i korekty tygodni w MVP.** Rytm A/B wymaga punktu odniesienia, a ręczne zmiany muszą być niezależne od zmian pojedynczych wystąpień.
2. **Obsługiwać zmiany wystąpień bez modyfikowania cyklu.** Odwołanie, przeniesienie i odrabianie dotyczą konkretnej daty i nie mogą zmieniać bazowego wpisu zajęć.
3. **Rozdzielić warstwę danych od logiki harmonogramu.** Jeden `ScheduleResolver` powinien wyliczać aktywne zajęcia dla konkretnej daty. Z tego samego wyniku powinny korzystać ekran, kolizje i widget.
4. **Stosować spójne reguły dat.** Tygodnie zaczynają się w poniedziałek, a ekran „Dzisiaj” odczytuje datę przy otwarciu lub wznowieniu. Widget może aktualizować się z opóźnieniem narzuconym przez system.
5. **Zacząć od prostego formularza.** Prowadzący, sala, budynek, grupa i notatka mogą być opcjonalne, aby dodanie podstawowego zajęcia trwało kilka sekund.
6. **Dodać testy logiki domenowej.** Szczególnie sprawdzić początek semestru w środku tygodnia, pojedynczą korektę, zmianę od wskazanego tygodnia, nakładanie korekt, przejście przez północ i kolizje.
7. **Chronić plan od pierwszego wydania.** Eksport JSON i zachowane schematy Room pozwalają zabezpieczyć dane przed późniejszymi zmianami modelu.
8. **Utrzymać mały zakres pierwszego wydania.** Import i kolizje można wdrażać etapami, gdy podstawowy przepływ dodawania, zmiany i przeglądania planu będzie stabilny.
9. **Budować modułowo, ale bez przedwczesnego podziału na moduły Gradle.** Pakiety i interfejsy wystarczą do szybkich zmian, a osobny moduł Gradle warto dodać dopiero przy niezależnym cyklu życia części projektu.
10. **Rozdzielić notatkę wspólną od notatki do wystąpienia.** Dzięki temu dopisek „przynieść projektor” może dotyczyć wszystkich zajęć, a „kolokwium” tylko jednej daty.
