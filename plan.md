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
- powiadomienia systemowe;
- działanie całkowicie offline;
- małe zużycie baterii;
- brak kont, logowania, backendu i synchronizacji w chmurze.

Po jednorazowym skonfigurowaniu planu użytkownik powinien korzystać głównie z ekranu „Dzisiaj” i widgetu.

## 1.1. Stan wdrożenia

Stan na 2026-09-19:

- Warstwa danych, kalkulator tygodni A/B, resolver planu, kolizje, Room i eksport JSON są zaimplementowane i objęte testami JVM.
- Kreator pierwszej konfiguracji, semestry, kierunki, zajęcia, notatki, zmiany pojedynczych wystąpień, ekran „Dzisiaj”, plan, kalendarz i ustawienia są dostępne w aplikacji Compose.
- Nawigacja korzysta z `NavHost` i back stacku. Ekrany podrzędne mają argumenty tras, tytuł topbara i przewidywalny powrót przez przycisk oraz systemowy back.
- Edge-to-edge korzysta z insetów Material 3. Topbar i dolna nawigacja uwzględniają bezpieczny obszar ekranu.
- Daty i godziny są wybierane przez pickery Material 3 z ograniczeniami zakresu, akcją semantyczną i przywracaniem focusu. Kolory kierunków wybiera się z nazwanej palety.
- Własne kontrolki mają minimalny obszar dotyku 48 dp, semantykę, widoczny focus i ikony Material. Odstępy ekranów korzystają z tokenów `MakSpacing`.
- Dodano testy tras, pickerów, topbara i układu dla szerokości 320 dp. Testy JVM oraz kompilacja testów Android przechodzą.
- Build debug zawiera bezpieczny seed demonstracyjny dla pustej bazy, aby można było od razu obejrzeć wszystkie główne stany interfejsu.

Pozostaje do wykonania:

- uruchomienie testów instrumentacyjnych i wizualna kontrola insetów na emulatorze lub urządzeniu;
- sprawdzenie TalkBacka, klawiatury, gestu wstecz i motywu ciemnego w rzeczywistym środowisku Androida;
- funkcje zaplanowane na wersje 0.2 i 0.3, w szczególności pełna obsługa widgetu oraz import JSON, zgodnie z sekcjami wdrożenia poniżej.

## 1.2. Plan porządkowania architektury

Zmiany należy wprowadzać stopniowo podczas rozwoju wersji 0.2 i 0.3. Nie wymagają podziału projektu na osobne moduły Gradle ani dodania frameworka wstrzykiwania zależności.

Status: wspólny `ActivePlanProvider` oraz mapowanie Room poza ViewModelem są zaimplementowane. Pozostałe punkty realizować podczas zmian odpowiednich przepływów.

Kolejność prac:

1. Przed implementacją widgetu wydzielić wspólny `ActivePlanProvider`. Komponent przyjmuje dane domenowe i datę, wywołuje `ScheduleResolver` oraz w razie potrzeby `CollisionDetector`, a następnie zwraca aktywny plan. Mapowanie danych Room na modele domenowe przenieść z ViewModelu do wspólnej granicy danych. Kod nie może zależeć od Compose ani Glance. Ekran „Dzisiaj”, plan, kalendarz i widget mają korzystać z tej samej ścieżki obliczeń.
2. Ustawić `NavController` jako jedyne źródło bieżącej trasy. ViewModel może zgłaszać jednorazowy zamiar przejścia po zapisie, usunięciu albo zakończeniu konfiguracji, ale nie przechowuje kopii aktualnej trasy.
3. Rozdzielać `MakViewModel` według przepływów podczas zmian odpowiednich ekranów. Docelowy podział obejmuje `ScheduleViewModel`, `ClassEditViewModel`, `OccurrenceViewModel`, `SemesterViewModel`, `SetupViewModel` i `SettingsViewModel`. Stan nadrzędny może koordynować aktywny semestr i ustawienia wspólne, ale nie zawiera logiki formularzy poszczególnych ekranów.
4. Nie wystawiać typów Room w publicznym stanie UI. `SemesterWithData` i encje pozostają po stronie danych albo prywatnego składania stanu. Ekrany otrzymują modele prezentacyjne i identyfikatory potrzebne do akcji.
5. Zapisy obejmujące kilka rekordów wykonywać atomowo. Dotyczy to co najmniej zapisu zajęć z nowym prowadzącym, utworzenia semestru z pierwszym kierunkiem, zmiany aktywnego semestru po usunięciu oraz przyszłego importu. Import zapisuje cały plik albo nie zmienia bazy.
6. Wprowadzić wspólny stan operacji zapisu: `Saving`, `Saved`, `ValidationError` i `StorageError`. Błąd nie może zamknąć formularza ani usunąć wpisanych wartości. Komunikat wskazuje użytkownikowi pole albo operację, której dotyczy.
7. Zapisać trwałe preferencje, w tym motyw, poza pamięcią ViewModelu. Stan nawigacyjny, wybrana data, filtry i robocze wartości formularza powinny przetrwać odtworzenie procesu przy użyciu `SavedStateHandle` albo równoważnego mechanizmu.

Kryteria zakończenia porządkowania:

- widoki Compose i widget otrzymują ten sam wynik planu dla tej samej daty oraz danych;
- domena nie zależy od Room, Compose ani Glance;
- systemowy back i odtworzenie procesu nie rozjeżdżają trasy ze stanem ekranu;
- awaria operacji wieloetapowej nie zostawia częściowo zapisanych danych;
- błąd zapisu pozostawia formularz otwarty z zachowanymi wartościami;
- ponowne utworzenie procesu przywraca trwałe preferencje i istotny stan roboczy.

## 1.3. Plan poprawy ekranu „Plan”

Zmiany dotyczą widoku listy na ekranie „Plan”. Widok kalendarza zachowuje obecny zakres funkcji. Zmiany wykonać przy użyciu komponentów Material 3 i istniejących tokenów `MakSpacing`.

Status: punkty 1-7 są zaimplementowane i mają testy kompilujące się dla Androida. Pozostaje uruchomienie testów instrumentacyjnych i ręczna weryfikacja z punktu 8 na emulatorze albo urządzeniu.

### 1. Skrócenie nagłówka

W `ScheduleScreen` zastąpić `MakSectionHeader(eyebrow = "Plan", title = "Twoje zajęcia")` zwartym nagłówkiem „Plan zajęć”. Nie wyświetlać osobnej etykiety „PLAN”, ponieważ aktywna pozycja dolnej nawigacji wskazuje bieżący ekran. Zachować globalny topbar z logo MAK i ustawieniami oraz dolną nawigację bez zmian. Odstęp między nagłówkiem i `MakViewSwitch` nie może przekraczać `MakSpacing.md`.

### 2. Wspólne sterowanie tygodniem

W `ScheduleScreen.kt` utworzyć prywatny komponent `WeekNavigationHeader`. Komponent zawiera:

- przycisk poprzedniego tygodnia o obszarze dotyku co najmniej 48 dp;
- wyśrodkowany zakres `weekRangeLabel` i drugą linię `weekSubtitle`;
- przycisk następnego tygodnia o obszarze dotyku co najmniej 48 dp;
- pod zakresem dwie zwarte informacje: `weekTypeLabel` oraz `weekSourceLabel`.

Całość umieścić w jednej sekcji. Usunąć osobny `MakNoteBanner` z widoku listy. `weekTypeLabel` i `weekSourceLabel` nie mogą wyglądać jak karta o tej samej wadze co lista zajęć. Użyć małych odznak albo jednego wiersza na tle `surfaceVariant` lub `secondaryContainer`. Tekst musi korzystać z kolorów `onSurfaceVariant` albo `onSecondaryContainer`.

Odznaka tygodnia ma być przyciskiem otwierającym istniejący `WeekCorrectionDialog`. Nadać jej opis dostępności „Zmień oznaczenie tygodnia, obecnie: {weekTypeLabel}, źródło: {weekSourceLabel}”. Zachować widoczny focus, obsługę klawiatury i minimum 48 dp obszaru aktywnego.

### 3. Usunięcie odłączonego menu

Usunąć `MakActionMenu` zawierające akcję „Zmień A/B” z `ListView`. Nie zostawiać osobnego przycisku z trzema kropkami. Jedynym wejściem do `WeekCorrectionDialog` w widoku listy jest odznaka tygodnia w `WeekNavigationHeader`. Jeśli w przyszłości pojawią się inne akcje tygodnia, umieścić je w tej samej sekcji sterowania tygodniem.

### 4. Czytelny stan filtrów

Zastąpić ogólną etykietę „Pokaż filtry” prywatnym komponentem `ScheduleFilterSection`. Nagłówek sekcji ma pokazywać:

- „Filtry”, gdy wybrano „Wszystkie”;
- „Filtry: {nazwa kierunku}”, gdy wybrano konkretny kierunek.

Dodać ikonę filtra oraz ikonę rozwinięcia i zwinięcia z Material Icons. Semantyka przycisku zawiera bieżący wybór oraz stan „Rozwinięte” albo „Zwinięte”. Po rozwinięciu pozostawić `MakSelectField` z pojedynczym wyborem kierunku. Po wybraniu kierunku zwinąć sekcję i pozostawić nazwę wyboru w nagłówku. Długą nazwę kierunku ograniczyć do jednej linii z wielokropkiem, bez poziomego przewijania.

### 5. Neutralna i konkretna informacja o kolizji

W `ClassItemUi` zastąpić `hasConflict: Boolean` polem `conflictLabel: String?`. Podczas budowy stanu zebrać z `CollisionDetector` zakresy nakładania dla każdego wystąpienia. Zakres formatować jako `HH:mm-HH:mm`.

Etykiety:

- jedna kolizja: „Kolizja 09:00-09:30”;
- kilka kolizji: „Kolizje: 09:00-09:30, 10:00-10:15”.

Zakresy posortować, usunąć duplikaty i przypisać do obu zajęć uczestniczących w kolizji. Nie obliczać zakresu ponownie w `ClassCard`.

W `ClassCard` pokazać `conflictLabel` z ikoną ostrzeżenia. Użyć `tertiaryContainer` i `onTertiaryContainer` albo innej pary tokenów spełniającej kontrast. Nie używać `error`, czerwonego tekstu ani sformułowania sugerującego błąd użytkownika. Pełna etykieta kolizji ma wejść do opisu semantycznego karty.

### 6. Układ informacji w karcie zajęć

Zachować godzinę rozpoczęcia i zakończenia w lewej kolumnie oraz nazwę zajęć jako pierwszy element prawej kolumny. Poniżej ułożyć informacje w tej kolejności:

1. odznaka kierunku i typ zajęć;
2. sala, budynek i prowadzący w osobnym tekście metadanych;
3. informacja o kolizji;
4. notatka.

Dodać `building: String?` do `ClassItemUi` i wypełniać je z `PlannedOccurrence`. Pomijać puste elementy metadanych. Gdy nie podano sali, zachować tekst „Sala niepodana”. Nie umieszczać odznaki kierunku oraz wszystkich metadanych w jednym wierszu. Nazwa zajęć może mieć dwie linie, metadane dwie linie, a notatka trzy linie. Karta nie może przewijać się poziomo przy szerokości 320 dp i długiej nazwie kierunku albo prowadzącego.

Pozostawić kolorowy pasek kierunku, lecz nie używać go jako jedynego oznaczenia. Pasek ma być przycięty tym samym kształtem co karta. Ograniczyć dekoracyjne obramowania do karty i elementów interaktywnych. Nie dodawać osobnego obramowania każdemu wierszowi metadanych.

### 7. Czytelność wyboru dnia

W `DaySelector` zwiększyć rozmiar skrótu dnia z 10 sp do co najmniej 11 sp, a numeru dnia z 13 sp do co najmniej 14 sp. Zachować siedem równych kolumn, minimum 48 dp wysokości aktywnego obszaru oraz tekstowy opis daty w semantyce. Kolor nieaktywnego skrótu musi spełniać kontrast dla zwykłego tekstu. Stan wybrany nadal używa `primary` i `onPrimary`. Nie dodawać poziomego przewijania.

### 8. Testy i weryfikacja

Dodać `ScheduleScreenTest` w `app/src/androidTest/java/dev/retza/mak/ui/schedule`. Testy mają używać wstrzykniętego `ScheduleUiState` i obejmować:

- szerokość 320 dp z długą nazwą kierunku, zajęć i prowadzącego;
- widoczność zakresu tygodnia, oznaczenia A/B i źródła korekty w jednej sekcji;
- otwarcie `WeekCorrectionDialog` przez odznakę tygodnia;
- brak osobnego menu z trzema kropkami w widoku listy;
- nagłówek filtra bez wyboru oraz po wybraniu konkretnego kierunku;
- kartę z dokładnym zakresem jednej kolizji oraz kartę z kilkoma zakresami;
- zachowanie kolejności: kierunek i typ, metadane, kolizja, notatka;
- motyw ciemny przy szerokości 390 dp;
- dostępne akcje klawiatury, widoczny focus i obszary dotyku co najmniej 48 dp.

W testach JVM dodać osobne przypadki mapowania jednej oraz wielu kolizji na etykiety obu zajęć. Zachować istniejące testy `CollisionDetector`, ponieważ to one rozstrzygają samo nakładanie przedziałów.

Po implementacji uruchomić `gradlew.bat test` oraz kompilację testów Android. Na emulatorze albo urządzeniu sprawdzić szerokości 320 i 390 dp, motyw jasny i ciemny, TalkBack, klawiaturę, gest wstecz oraz brak obciętych akcji.

### 9. Kryteria akceptacji

- Pierwsza karta zajęć jest widoczna wyżej niż w obecnym układzie przy tej samej wysokości ekranu.
- Sterowanie tygodniem, oznaczenie A/B i źródło korekty tworzą jedną sekcję.
- Widok listy nie pokazuje odłączonego przycisku z trzema kropkami.
- Zwinięty filtr pokazuje aktywny kierunek.
- Kolizja jest neutralną informacją i zawiera dokładny zakres czasu.
- Długie dane karty zawijają się bez poziomego przewijania i bez zasłaniania odznaki kierunku.
- Wszystkie akcje mają semantyczne etykiety, widoczny focus i obszar dotyku co najmniej 48 dp.
- Układ pozostaje czytelny w motywie jasnym i ciemnym przy szerokości 320-390 dp.

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

Jeśli aplikacja nie ma jeszcze semestru, zamiast ekranu „Dzisiaj” otwiera kreator pierwszej konfiguracji. Kreator tworzy pierwszy semestr i co najmniej jeden kierunek, a po zakończeniu prowadzi do dodawania zajęć. Ustawienia pozwalają później dodawać, wybierać, konfigurować i usuwać semestry.

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

Formularz ogranicza zajęcia do jednego dnia kalendarzowego. Godzina zakończenia musi być późniejsza od rozpoczęcia; zajęcia przechodzące przez północ są odrzucane.

### Lokalizacja i dodatkowe dane

- sala;
- budynek;
- grupa — pole opcjonalne;
- notatka do zajęć — opcjonalna, wspólna dla każdego wystąpienia tego wpisu;
- notatka do wybranego wystąpienia — opcjonalna, przypięta do jednej konkretnej daty zajęć.

Podczas dodawania notatki użytkownik wybiera zakres: „Do tych zajęć” albo „Tylko do tego terminu”. Notatkę do konkretnego wystąpienia można dodać z ekranu szczegółów zajęć na ekranie „Dzisiaj” lub „Plan”. Jeśli istnieją oba typy, aplikacja pokazuje je osobno i nie nadpisuje notatki wspólnej.

Prowadzący zapisani wcześniej są proponowani podczas wpisywania. Formularz powinien walidować, że nazwa, kierunek, godzina rozpoczęcia i zakończenia są uzupełnione, a godzina zakończenia jest późniejsza od rozpoczęcia. Zajęcia przechodzące przez północ są nieprawidłowe.

Po usunięciu zajęć aplikacja powinna wymagać potwierdzenia. Edycja i usuwanie muszą aktualizować widget.

## 7. Semestr i tygodnie A/B

Ustawienia semestru są potrzebne od pierwszej wersji, ponieważ określają automatyczny rytm tygodni A/B.

Użytkownik może mieć wiele semestrów. Każdy semestr ma własne kierunki, prowadzących, zajęcia, korekty tygodni, notatki i zmiany wystąpień. Dane nie przechodzą między semestrami automatycznie. Użytkownik wybiera aktywny semestr w ustawieniach; ekrany, widget i powiadomienia korzystają wyłącznie z jego planu.

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

Ustawienia semestrów umożliwiają dodanie, edycję, wybór i usunięcie semestru. Usunięcie wymaga potwierdzenia i usuwa dane tego semestru. Gdy użytkownik usunie aktywny semestr, aplikacja wybiera inny istniejący semestr albo otwiera kreator, jeśli nie ma już żadnego.

## 8. Kierunki

Model `Course`:

- `id`;
- `semesterId`;
- `name`;
- `color`.

Aplikacja nie jest ograniczona do dwóch kierunków. Kolor kierunku jest widoczny na liście zajęć, w filtrach i w widgetach.

## 9. Prowadzący

Model `Teacher`:

- `id`;
- `semesterId`;
- `name`.

Pola `email` i `academicTitle` można dodać później, ale nie są potrzebne w MVP. Warto zachować prowadzących jako osobną tabelę, aby autouzupełnianie nie tworzyło wielu kopii tej samej osoby.

## 10. Model danych zajęć

Model aplikacyjny `ClassItem` oraz odpowiadający mu `ClassEntity` powinien zawierać:

- `id`;
- `semesterId`;
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

Kolizja występuje, gdy dwa aktywne zajęcia tego samego dnia mają przedziały czasu, które się nakładają. Dotyczy to także zajęć z dwóch różnych kierunków. Przedziały stykające się końcem i początkiem, np. 10:00–11:00 oraz 11:00–12:00, nie są kolizją.

Przykład:

```text
Programowanie  10:00–11:30
Matematyka     11:00–12:30
```

Wynik: kolizja trwająca 30 minut.

Kolizję należy oznaczyć przy obu zajęciach i pokazać jej czas trwania po wejściu w szczegóły. Jest to ostrzeżenie i informacja o ograniczeniu planu, a nie komunikat o winie użytkownika. Aplikacja nie proponuje zmiany terminu, nie wybiera rozwiązania za użytkownika i nie modyfikuje planu automatycznie. Użytkownik sam decyduje, czy skontaktować się z uczelnią, opuścić jedno z zajęć albo ręcznie zapisać zmianę wystąpienia.

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

### 13.1. Etapowy plan implementacji widgetów

Każdy etap kończy się kompilującym przyrostem i testem logiki, którą da się uruchomić bez launchera. Nie rozpoczynać kolejnego etapu, jeśli poprzedni nie spełnia swoich kryteriów.

#### Etap 1: stan widgetu i wspólna ścieżka planu

1. Utworzyć pakiet `widget` w module `app`.
2. Dodać czyste modele `WidgetUiState` dla stanów: brak aktywnego semestru, data poza semestrem, brak zajęć, plan dostępny i błąd odczytu.
3. Utworzyć `WidgetPlanLoader`, który otrzymuje `MakRepository`, `ActivePlanProvider` i `Clock`. Loader odczytuje aktywny semestr, pobiera jego dane, mapuje je raz przez `toActivePlanData()` i oblicza plan dla `LocalDate.now(clock)`.
4. Nie wywoływać DAO ani `ScheduleResolver` bezpośrednio z widgetu. Widget korzysta z tej samej instancji logiki co ekrany przez `ActivePlanProvider`.
5. Utworzyć czysty `WidgetPresenter`, który zamienia `ActivePlan` na `WidgetUiState`. Presenter ustala kolejność zajęć, skrócone metadane, oznaczenie tygodnia, informację o kolizji i wskaźnik notatki.

Kryterium etapu: test JVM potwierdza zgodność identyfikatorów i kolejności zajęć z wynikiem `ActivePlanProvider` oraz wszystkie stany puste.

#### Etap 2: rejestracja i minimalny widget

1. Dodać zależności `androidx.glance:glance-appwidget` i `androidx.glance:glance-material3` z istniejącego katalogu wersji.
2. Utworzyć `TodayWidget` dziedziczący po `GlanceAppWidget` oraz `TodayWidgetReceiver` dziedziczący po `GlanceAppWidgetReceiver`.
3. Zarejestrować receiver w `AndroidManifest.xml` dla `APPWIDGET_UPDATE` i wskazać metadane providera.
4. Dodać `res/xml/today_widget_info.xml` z `initialLayout` biblioteki Glance, kategorią `home_screen`, zmianą rozmiaru w obu osiach oraz wartościami `targetCellWidth`, `targetCellHeight`, `minWidth`, `minHeight`, `minResizeWidth` i `minResizeHeight`.
5. Użyć `SizeMode.Responsive` z co najmniej dwoma nazwanymi progami rozmiaru: małym i dużym. Wartości progów zapisać w jednym miejscu i dobrać po sprawdzeniu launchera na Androidzie 12 lub nowszym.
6. Pierwsza wersja renderuje datę, oznaczenie tygodnia oraz jeden z jednoznacznych stanów pustych. Nie dodawać jeszcze listy zajęć.

Kryterium etapu: widget można dodać do ekranu głównego, zmienić jego rozmiar i zobaczyć poprawny stan dla pustej bazy oraz braku zajęć.

#### Etap 3: mały widget

1. Dla małego progu pokazać datę, tydzień A/B oraz jedno lub dwa najbliższe zajęcia, zależnie od dostępnej wysokości.
2. Każdy wiersz zawiera godzinę rozpoczęcia, nazwę, kierunek i salę. Pomija puste metadane zamiast zostawiać separatory.
3. Długą nazwę zajęć ograniczyć do jednej linii, a drugorzędne informacje do jednej linii. Nie używać poziomego przewijania.
4. Kolor kierunku może być paskiem pomocniczym, ale nazwa kierunku pozostaje tekstem. Kolor nie może być jedyną informacją.
5. Jeśli zajęcia mają kolizję albo notatkę, pokazać krótki tekst lub dostępny wskaźnik. Pełną treść pozostawić ekranowi szczegółów w aplikacji.

Kryterium etapu: przy minimalnym rozmiarze wszystkie teksty mieszczą się bez nakładania, a brak miejsca ogranicza liczbę pozycji zamiast obcinać cały układ.

#### Etap 4: duży widget i rozmiary pośrednie

1. Dla dużego progu pokazać pełniejszą listę dzisiejszych zajęć. Liczbę wierszy wyliczać z wybranego progu rozmiaru, a nie z modelu launchera albo stałej liczby wszystkich zajęć.
2. Duży wiersz zawiera godzinę, nazwę, kierunek, salę oraz opcjonalnie prowadzącego. Notatkę przedstawia wskaźnik, nie pełny wielowierszowy tekst.
3. Jeśli zajęć jest więcej niż mieści układ, pokazać informację „Jeszcze {liczba}” zamiast ściskać wiersze.
4. Dla rozmiaru pośredniego użyć małego albo dużego wariantu wybranego przez `SizeMode.Responsive`. Nie tworzyć osobnego układu dla każdego możliwego wymiaru.
5. Sprawdzić promień tła widgetu, padding systemowy, motyw jasny i ciemny oraz kontrast małego tekstu.

Kryterium etapu: zmiana rozmiaru przełącza układ bez utraty daty, stanu tygodnia i najbliższych zajęć.

#### Etap 5: otwieranie aplikacji

1. Kliknięcie tła, nagłówka albo pustego stanu otwiera `MainActivity` na ekranie „Dzisiaj”.
2. Użyć jawnego intentu lub obsługiwanej akcji Glance. Trasa docelowa nie może zależeć od ostatnio otwartego ekranu aplikacji.
3. W pierwszej wersji kliknięcie wiersza także otwiera ekran „Dzisiaj”. Otwieranie szczegółów konkretnego wystąpienia dodać tylko po wprowadzeniu stabilnego kontraktu deep linków.
4. Wielokrotne szybkie kliknięcie nie tworzy kilku kopii aktywności w stosie.

Kryterium etapu: aplikacja uruchomiona z każdego wariantu widgetu pokazuje właściwą datę na ekranie „Dzisiaj”.

#### Etap 6: odświeżanie po zmianach i zmianie dnia

1. Wprowadzić niezależny od Glance interfejs żądania odświeżenia. Jego implementacja w `widget` wywołuje `TodayWidget().updateAll(context)`.
2. Wywoływać żądanie na jednej granicy po udanym zapisie danych wpływających na plan. Nie rozrzucać wywołań po ekranach i nie uruchamiać aktualizacji przed zakończeniem transakcji.
3. Odświeżać widget po zmianie zajęć, wystąpienia, notatki, semestru, aktywnego semestru, kierunku albo korekty tygodnia.
4. Przy każdym odświeżeniu odczytać datę przez wstrzyknięty `Clock`. Nie przechowywać bieżącej daty ani planu wyłącznie w pamięci procesu.
5. Ustawić `updatePeriodMillis` nie częściej niż raz na godzinę jako zabezpieczenie zmiany dnia. Nie dodawać WorkManagera, dokładnych alarmów ani osobnego serwisu, dopóki pomiary nie wykażą rzeczywistej potrzeby.
6. Zaakceptować opóźnienie systemowe. Tekst i dokumentacja nie obiecują aktualizacji dokładnie o północy.

Kryterium etapu: zapis w aplikacji aktualizuje wszystkie instancje widgetu, a okresowy sygnał odczytuje nową datę bez uruchamiania ciągłego procesu.

#### Etap 7: błędy, odporność i wydajność

1. Błąd odczytu danych pokazuje krótki stan „Nie udało się wczytać planu” i pozwala otworzyć aplikację. Nie wyświetla surowego wyjątku.
2. Brak aktywnego semestru prowadzi do aplikacji, gdzie użytkownik może przejść konfigurację.
3. Jedno odświeżenie mapuje `SemesterWithData` do `ActivePlanData` tylko raz. Nie obliczać kolizji dla dat ani wariantów rozmiaru, których widget nie prezentuje.
4. Wiele instancji widgetu może korzystać z tego samego obliczonego stanu dnia, jeśli nie mają osobnej konfiguracji.
5. Nie zapisywać planu użytkownika w preferencjach Glance. Room pozostaje źródłem danych.

Kryterium etapu: usunięcie semestru, pusta baza, błąd odczytu i ponowne utworzenie procesu nie pozostawiają starego lub pustego `RemoteViews` bez wyjaśnienia.

#### Etap 8: testy i odbiór

1. Testy JVM obejmują stany `WidgetUiState`, sortowanie, limit pozycji, „Jeszcze {liczba}”, wskaźnik notatki, kolizję i datę poza semestrem.
2. Test integracyjny potwierdza, że loader korzysta z `ActivePlanProvider` i zwraca ten sam zestaw wystąpień co ekran „Dzisiaj” dla wstrzykniętej daty.
3. Nie powielać testów reguł tygodni A/B, zmian wystąpień i kolizji w testach Glance.
4. Uruchomić `gradlew.bat test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug`.
5. Na urządzeniu albo emulatorze sprawdzić dodanie, usunięcie i ponowne dodanie widgetu, mały i duży rozmiar, zmianę rozmiaru, motyw jasny i ciemny, pusty dzień, wiele zajęć, kolizję, notatkę oraz otwarcie aplikacji.
6. Zmienić dane planu przy widocznym widgetcie i potwierdzić aktualizację bez ponownego dodawania widgetu.

Kryterium zakończenia wersji 0.2: mały i duży widget pokazują plan z `ActivePlanProvider`, reagują na zmiany danych, ponownie odczytują datę, otwierają ekran „Dzisiaj” i pozostają czytelne we wszystkich zadeklarowanych rozmiarach.

## 14. Ustawienia i dane

Ustawienia powinny zawierać:

- listę semestrów z możliwością dodania, wyboru, konfiguracji i usunięcia;
- konfigurację aktywnego semestru i korekty tygodni A/B;
- listę kierunków i ich kolorów;
- ustawienia powiadomień;
- eksport planu;
- import planu;
- informację o wersji aplikacji.

Eksport i import mogą używać lokalnego pliku JSON. Format powinien mieć pole `schemaVersion`, aby można było zmieniać model danych bez utraty zgodności ze starszymi eksportami.

Eksport jest dostępny od wersji 0.1. Użytkownik wybiera miejsce zapisu przez systemowy wybór dokumentu. Plik zawiera wszystkie semestry oraz przypisane do nich kierunki, prowadzących, zajęcia, korekty tygodni, notatki i zmiany wystąpień. Import pojawia się w wersji 0.3.

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
- kreator pierwszej konfiguracji oraz zarządzanie wieloma odizolowanymi semestrami;
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

Szczegółowa kolejność i kryteria znajdują się w sekcji 13.1.

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
6. **Dodać testy logiki domenowej.** Szczególnie sprawdzić początek semestru w środku tygodnia, pojedynczą korektę, zmianę od wskazanego tygodnia, nakładanie korekt, odrzucenie zajęć przechodzących przez północ i kolizje.
7. **Chronić plan od pierwszego wydania.** Eksport JSON i zachowane schematy Room pozwalają zabezpieczyć dane przed późniejszymi zmianami modelu.
8. **Utrzymać mały zakres pierwszego wydania.** Import i kolizje można wdrażać etapami, gdy podstawowy przepływ dodawania, zmiany i przeglądania planu będzie stabilny.
9. **Budować modułowo, ale bez przedwczesnego podziału na moduły Gradle.** Pakiety i interfejsy wystarczą do szybkich zmian, a osobny moduł Gradle warto dodać dopiero przy niezależnym cyklu życia części projektu.
10. **Rozdzielić notatkę wspólną od notatki do wystąpienia.** Dzięki temu dopisek „przynieść projektor” może dotyczyć wszystkich zajęć, a „kolokwium” tylko jednej daty.
