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

Stan na 2026-09-22:

- Warstwa danych, kalkulator tygodni A/B, resolver planu, kolizje, Room oraz eksport i import JSON są zaimplementowane. Testy Room są przygotowane, ale wymagają uruchomienia na urządzeniu.
- Kreator pierwszej konfiguracji, semestry, kierunki, zajęcia, notatki, zmiany pojedynczych wystąpień, ekran „Dzisiaj”, plan, kalendarz i ustawienia są dostępne w aplikacji Compose.
- Nawigacja korzysta z `NavHost` i back stacku. Ekrany podrzędne mają argumenty tras, tytuł topbara i przewidywalny powrót przez przycisk oraz systemowy back.
- Edge-to-edge korzysta z insetów Material 3. Topbar i dolna nawigacja uwzględniają bezpieczny obszar ekranu.
- Daty i godziny są wybierane przez pickery Material 3 z ograniczeniami zakresu, akcją semantyczną i przywracaniem focusu. Kolory kierunków wybiera się z nazwanej palety.
- Własne kontrolki mają minimalny obszar dotyku 48 dp, semantykę, widoczny focus i ikony Material. Odstępy ekranów korzystają z tokenów `MakSpacing`.
- Dodano testy tras, pickerów, topbara i układu dla szerokości 320 dp. Testy JVM oraz kompilacja testów Android przechodzą.
- Build debug zawiera bezpieczny seed demonstracyjny dla pustej bazy, aby można było od razu obejrzeć wszystkie główne stany interfejsu.
- Kod widgetu ma loader, presenter, stany puste i błędu, układ responsywny Glance oraz odświeżanie po zmianach bazy.

Pozostaje implementacja:

- etap 11: podział `MakRepository` według granic danych;
- ekran „Dzisiaj”: trzy wartości w karcie podsumowania, domenowe liczenie okienek i ustawienie ich progu;
- karty zajęć: osobne oznaczenia obu rodzajów notatek, podział informacji na sekcje i dopracowanie koloru kierunku;
- etap 7 sekcji rozwijanych: odstępy i neutralny kolor stanu rozwiniętego;
- reorganizacja głównego ekranu ustawień i osobne ekrany jego obszarów;
- dokończenie układu widgetu według 13.1 i wizualne dopracowanie według 13.2.

Pozostaje odbiór bez zmiany logiki: testy instrumentacyjne i migracji Room na urządzeniu, kontrola nawigacji, dostępności, motywów i szerokości 320 dp, działanie importu i powiadomień na urządzeniu oraz zachowanie widgetu na launcherze. Lista scenariuszy znajduje się w sekcji 1.2.

## 1.2. Pozostałe prace i kolejność

Wykonane zmiany architektury są opisane w `ARCHITECTURE.md`, a ich uzasadnienia w `JOURNAL.md`. Ten plan obejmuje dalsze prace i odbiór.

1. Podzielić `MakRepository` na `SemesterRepository` i `ScheduleRepository`. Pozostawić `SettingsPreferences` jako osobną granicę. Dodać `PlanBackupService` dla pełnego snapshotu i atomowego importu. Nie wystawiać encji Room w publicznych kontraktach. Przenieść widget i powiadomienia na nowe kontrakty, a stary interfejs usunąć po ostatnim konsumencie. Zachować transakcje i testy rollbacku.
2. Przebudować podsumowanie „Dzisiaj” i dodać domenowe liczenie okienek według sekcji 4. Udostępnić globalny próg w ustawieniach. Następnie dokończyć strukturę kart zajęć, w tym dwa rodzaje notatek.
3. Poprawić odstępy i sekcje rozwijane. Duże sekcje rozdziela 16 dp, elementy powiązane 12 dp, etykietę od wartości 8 dp, a przycisk pełnej szerokości ma co najmniej 12 dp wolnego miejsca nad i pod nim. Sekcja rozwijana ma neutralne tło w obu stanach, wspólny kontener nagłówka i treści oraz 16 dp wewnętrznego paddingu. Stan przekazują tekst, ikona i semantyka, bez zmiany na kolor akcentowy.
4. Przebudować ustawienia według sekcji 14. Ekran główny ma prowadzić do osobnych ekranów zarządzania semestrami, powiadomieniami oraz danymi. Usunąć powtórzony nagłówek i rozwijane formularze.
5. Dokończyć responsywny układ widgetu według sekcji 13.1 i wizualne zmiany z sekcji 13.2. Porównać warianty na launcherze przed ustaleniem odcieni i gęstości.

### Odbiór na urządzeniu

Kod konfiguracji wielu kalendarzy, importu, powiadomień, migracji Room, ekranu „Plan”, szczegółów terminu i bazowego widgetu już istnieje. Ich testy Android są przygotowane, ale kompilacja testów nie zastępuje uruchomienia ich na urządzeniu.

- Uruchomić test migracji Room i sprawdzić otwarcie bazy z wersji 1 oraz zachowanie danych.
- Sprawdzić rozdzielanie i ponowne łączenie kalendarzy, wybór kalendarza korekty oraz plan dwóch kierunków z różnych uczelni.
- Sprawdzić import poprawnego i błędnego pliku, anulowanie oraz rollback po błędzie zapisu.
- Sprawdzić zgodę na powiadomienia, alarm wieczorny i przed zajęciami, restart, zmianę czasu, anulowanie kolizji i kliknięcie powiadomienia.
- Sprawdzić nawigację i powrót systemowy, insety, TalkBack, klawiaturę, focus, motyw ciemny, długie treści i szerokość 320 dp. Potwierdzić, że po błędzie formularz zachowuje dane i pokazuje bezpieczny komunikat.
- Sprawdzić widget na launcherze w małym, pośrednim i dużym rozmiarze, w obu motywach, po zmianie danych i po kliknięciu. Zweryfikować jego stany puste, kolizje i długie nazwy.

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

Jeśli aplikacja nie ma jeszcze semestru, ekran „Dzisiaj” pokazuje stan pusty z przyciskiem „Skonfiguruj plan”. Kreator otwiera się po tej akcji i tworzy pierwszy semestr, kierunek, kalendarz oraz przypisanie. Ustawienia pozwalają później dodawać, wybierać, konfigurować i usuwać semestry.

## 4. Ekran „Dzisiaj”

To domyślny ekran otwierany po uruchomieniu aplikacji.

Status: przebudowa podsumowania niewykonana. Ekran pokazuje liczbę zajęć w obecnej karcie gradientowej, ale nie ma trzech kolumn, liczby unikalnych kolizji, liczby okienek ani globalnego progu okienka w ustawieniach. Karty zajęć mają już podział na czas i dane oraz pasek koloru kierunku. Pozostaje rozdzielenie metadanych i notatek na sekcje, osobne oznaczenie notatki wspólnej i notatki wystąpienia oraz dopracowanie koloru pilla kierunku. Szczegóły poniżej opisują stan docelowy.

Powinien pokazywać:

- dzień tygodnia i pełną datę;
- gradientową kartę „Twój plan na dziś” z trzema równymi kolumnami „Zajęcia”, „Kolizje” i „Okienka”; każda kolumna ma etykietę u góry i liczbę poniżej;
- czerwony kolor liczby kolizji, gdy jest większa od zera, oraz zielony, gdy wynosi zero; pozostałe liczby zachowują neutralny kolor;
- wszystkie aktywne zajęcia w kolejności od najwcześniejszego;
- kolor kierunku;
- oznaczenie wykrytych kolizji;
- stan pusty, gdy danego dnia nie ma zajęć.

Okienko jest przerwą dłuższą niż ustawiony próg między końcem jednego bloku zajęć a początkiem następnego. Liczyć je ze wspólnego planu wszystkich kierunków aktywnego semestru, także gdy kierunki używają różnych kalendarzy akademickich. Domyślny próg wynosi 30 minut, więc przerwa trwająca dokładnie 30 minut nie jest okienkiem. Użytkownik może zmienić próg globalnie w ustawieniach. Nie liczyć czasu przed pierwszymi ani po ostatnich zajęciach dnia. Odwołane zajęcia pominąć, a zmiany i przeniesienia uwzględnić. Nakładające się zajęcia najpierw połączyć w blok czasu, aby kolizja nie tworzyła fałszywego okienka.

Obliczanie liczby okienek należy do domeny i korzysta z tego samego aktywnego planu co wykrywanie kolizji. `TodayViewModel` umieszcza liczbę zajęć, unikalnych kolizji i okienek w stanie widoku. Karta zachowuje obecny gradient, ma subtelne pionowe separatory i nie pokazuje ozdobnej ikony. Przy braku zajęć pokazuje tekst „Dziś bez zajęć” oraz trzy wartości równe zero. Układ musi zachować czytelność i semantykę przy szerokości 320 dp oraz w motywie jasnym i ciemnym.

### Struktura karty zajęć

Kartę zajęć zbudować jako czytelną siatkę z dwiema głównymi kolumnami. Lewa kolumna ma stałą szerokość i pokazuje godzinę rozpoczęcia oraz zakończenia. Prawa zawiera dane zajęć. Kolumny rozdziela subtelny pionowy separator. W prawej części użyć cienkich poziomych separatorów między informacjami podstawowymi, statusem kolizji i notatkami. Nie obramowywać każdej komórki osobno.

Kolejność sekcji w prawej kolumnie:

1. Nazwa zajęć.
2. Pill z nazwą kierunku oraz neutralny tekst typu zajęć.
3. Sala, budynek i prowadzący.
4. Status kolizji, jeśli występuje.
5. Notatki, jeśli występują.

Pionowy pasek przy krawędzi karty używa pełnego koloru kierunku. Pill z nazwą kierunku używa jaśniejszego wariantu tego samego koloru i tekstu o sprawdzonym kontraście. Nie kolorować całej karty według kierunku. Typ zajęć pozostaje neutralnym tekstem obok pilla. Kolor kierunku zawsze występuje razem z jego nazwą, więc nie jest jedynym nośnikiem informacji.

Notatkę wspólną oznaczyć niebieskim lub indygo pillem „Notatka do zajęć”. Notatkę pojedynczego wystąpienia oznaczyć fioletowym pillem „Notatka na dziś”. Treść wyświetlić obok etykiety albo pod nią, bez zamykania całej długiej treści w pillu. Jeśli istnieją oba rodzaje notatek, pokazać dwa osobne wiersze. Kolizja zachowuje pomarańczowy styl ostrzegawczy. Kierunek, kolizja i oba rodzaje notatek mają osobne role kolorystyczne oraz jawne etykiety tekstowe.

Karta musi zachować wspólne wyrównanie wierszy, poprawne zawijanie długich nazw i notatek, kontrast oraz czytelność przy szerokości 320 dp. Semantyka czytnika ekranu ma przekazywać godziny, nazwę, kierunek, typ, metadane, kolizję i zakres każdej notatki w logicznej kolejności.

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
- oznaczeń wykrytych kolizji.

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

Prowadzący jest opcjonalnym tekstem zapisanym przy zajęciach. Aplikacja nie prowadzi osobnej bazy prowadzących. Formularz powinien walidować, że nazwa, kierunek, godzina rozpoczęcia i zakończenia są uzupełnione, a godzina zakończenia jest późniejsza od rozpoczęcia. Zajęcia przechodzące przez północ są nieprawidłowe.

Po usunięciu zajęć aplikacja powinna wymagać potwierdzenia. Edycja i usuwanie muszą aktualizować widget.

## 7. Semestr i tygodnie A/B

Ustawienia semestru są potrzebne od pierwszej wersji, ponieważ określają automatyczny rytm tygodni A/B.

Użytkownik może mieć wiele semestrów. Semestr grupuje kierunki i plan na dany okres, ale globalny kierunek może występować w wielu semestrach. Użytkownik wybiera aktywny semestr w ustawieniach; ekrany, widget i powiadomienia korzystają wyłącznie z jego planu.

`Semester` przechowuje identyfikator, nazwę, np. „Semestr zimowy 2026/27”, oraz stan aktywności. Daty i rytm A/B należą do `AcademicCalendar`.

`AcademicCalendar` przechowuje datę rozpoczęcia, datę zakończenia, oznaczenie pierwszego tygodnia oraz korekty. `SemesterProgram` łączy semestr z globalnym kierunkiem i wskazuje kalendarz. Kilka kierunków może używać tego samego kalendarza; kierunek z innej uczelni może mieć osobny.

Kalkulator tygodnia powinien:

1. sprawdzić, czy data mieści się w zakresie kalendarza kierunku;
2. znaleźć poniedziałek tygodnia zawierającego datę rozpoczęcia kalendarza;
3. obliczyć liczbę pełnych tygodni od tego poniedziałku i naprzemiennie przypisywać A/B;
4. zastosować ostatnią korektę „Od tego tygodnia” obowiązującą dla wskazanego tygodnia, licząc naprzemienność od jej daty;
5. jeśli istnieje korekta „Tylko ten tydzień”, zastosować ją zamiast wyniku z poprzedniego kroku.

Każda korekta dotyczy jednego `AcademicCalendar` i wskazuje poniedziałek tygodnia, oznaczenie A/B oraz zakres. Zmiana pojedyncza nie wpływa na kolejny tydzień. Zmiana przyszła zaczyna nową sekwencję; następna zmiana przyszła może ją ponownie przestawić. Dla jednego tygodnia i zakresu obowiązuje najwyżej jedna korekta. Kierunki wskazujące ten sam kalendarz zawsze współdzielą daty, rytm A/B i korekty jako jeden zestaw.

Przykład: pierwszy tydzień semestru to A, więc następny to B. Jeśli trzeci tydzień zostanie jednorazowo oznaczony B, czwarty nadal będzie B według automatycznej sekwencji. Jeśli trzeci tydzień zostanie oznaczony B „Od tego tygodnia”, czwarty będzie A.

Poza zakresem semestru aplikacja powinna jasno pokazać, że nie ma aktywnego semestru. Nie należy opierać działania wyłącznie na numerze tygodnia ISO, ponieważ uczelniana numeracja może zaczynać się w innym miejscu.

Ustawienia semestrów umożliwiają dodanie, edycję, wybór i usunięcie semestru. Usunięcie wymaga potwierdzenia, usuwa plan, powiązania i nieużywane kalendarze, ale zachowuje globalne kierunki. Po usunięciu aktywnego semestru aplikacja wybiera inny istniejący semestr. Jeśli nie ma żadnego, pokazuje stan pusty z przyciskiem „Skonfiguruj plan” i nie otwiera kreatora automatycznie.

## 8. Kierunki

Model `StudyProgram`:

- `id`;
- `name`;
- `color`.

Aplikacja nie jest ograniczona do dwóch kierunków. Kierunek istnieje niezależnie od semestru i może być użyty ponownie. `SemesterProgram` łączy go z semestrem oraz kalendarzem. Kolor kierunku jest widoczny na liście zajęć, w filtrach i w widgetach.

## 9. Prowadzący

Prowadzący jest opcjonalnym polem tekstowym zajęć. Aplikacja nie utrzymuje osobnej kartoteki, identyfikatorów, adresów e-mail ani tytułów prowadzących.

## 10. Model danych zajęć

Model aplikacyjny `ClassItem` oraz odpowiadający mu `ClassEntity` powinien zawierać:

- `id`;
- `semesterProgramId`;
- `name`;
- `type`;
- `teacherName` — opcjonalne;
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

Model korekty tygodnia `WeekOverride` zawiera `id`, `academicCalendarId`, `weekStartDate` (poniedziałek), `weekType` (`A` lub `B`) oraz `scope` (`ONE_WEEK` albo `FROM_WEEK`). Korekty są przechowywane osobno od zajęć i obejmowane eksportem.

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

### 13.1. Plan unowocześnienia widgetu

Zmiany mają poprawić hierarchię informacji, czytelność i wykorzystanie dostępnego miejsca. Widget zachowuje jasne tło, wysoki kontrast i prosty układ. Nie dodawać zdjęć, gradientów, cieni ani ozdobnych ikon.

Status: częściowo wykonane. Są progi rozmiaru, stała kolumna czasu, pasek kierunku i separatory. Duży wariant nadal nie wykorzystuje miejsca na pełniejsze metadane, kierunek i lokalizacja są łączone w jednym wierszu, a kolor kierunku nie wyróżnia jego nazwy. Statyczny podgląd i odbiór na launcherze pozostają do sprawdzenia.

#### Etap 1: nagłówek i wiersze zajęć

1. Pokazać datę jako główny tekst nagłówka o rozmiarze 16 sp.
2. Pokazać tydzień A/B jako małą etykietę z delikatnym tłem.
3. Umieścić liczbę zajęć obok etykiety tygodnia albo wyrównać ją do prawej strony nagłówka.
4. Dodać do wiersza zajęć wąski pasek w kolorze kierunku. Zachować tekstową nazwę kierunku, ponieważ kolor nie może być jedynym nośnikiem informacji.
5. Użyć stałej kolumny czasu. Obok niej pokazać nazwę zajęć oraz osobny wiersz metadanych z salą i prowadzącym.
6. Pokazać kolizję i notatkę jako krótkie etykiety tekstowe o czytelnym kontraście.
7. Oddzielić zajęcia subtelnymi separatorami. Nie umieszczać każdego zajęcia w osobnej karcie.
8. Dopasować tło do systemowego promienia widgetów Androida.

#### Etap 2: układ responsywny

1. Traktować widget jako kompaktowy, gdy ma mniej niż 240 dp szerokości albo mniej niż 160 dp wysokości.
2. W wariancie kompaktowym pokazać przewijaną listę dzisiejszych zajęć. Pokazać nazwę, czas i salę, pominąć prowadzącego oraz osobną etykietę notatki, ale zachować alert kolizji.
3. Jeśli wariant kompaktowy nie mieści wszystkich zajęć, użyć przewijanej listy zamiast tekstu „Jeszcze {liczba}”.
4. W wariancie rozszerzonym pokazać przewijaną listę dzisiejszych zajęć wraz z salą, prowadzącym i statusem kolizji albo notatki.
5. Wiersze powinny wykorzystać pełną szerokość widgetu. Wariant rozszerzony ma używać większych odstępów i pełniejszych metadanych, a nie tylko zwiększać wysokość listy.
6. Ograniczać prowadzącego i lokalizację wielokropkiem. Nie ucinać czasu. Nazwa zajęć może zająć dwa wiersze, jeśli pozwala na to wysokość wariantu.
7. Ograniczyć maksymalny rozmiar widgetu do 360 na 420 dp, aby launcher nie tworzył nadmiernie pustego układu.
8. Dodać statyczny podgląd używany przez systemowy wybór widgetów.

#### Etap 3: zachowanie i odświeżanie

1. Zachować kliknięcie całego widgetu i każdego wiersza prowadzące do ekranu „Dzisiaj”. Nie dodawać osobnych akcji w tej wersji.
2. Ustawić `updatePeriodMillis` na 60 minut. Natychmiastowe odświeżanie po zmianie danych nadal realizować przez obserwację Room.
3. Przy każdym odświeżeniu pobierać bieżącą datę przez `Clock` i używać wspólnego `ActivePlanProvider`.
4. Sprawdzić zimny i ciepły start po kliknięciu widgetu. Trasa docelowa nie może zależeć od ekranu otwartego wcześniej w aplikacji.
5. Nie gwarantować aktualizacji dokładnie o północy i nie dodawać dokładnych alarmów ani stałego procesu w tle.

#### Etap 4: Gradle i dokumentacja

1. Zachować Gradle 9.7.1, jeśli pełny zestaw kontroli nadal przechodzi po zmianach widgetu.
2. Dodać oficjalną sumę SHA-256 dystrybucji, `validateDistributionUrl=true` oraz limit czasu pobierania do konfiguracji wrappera.
3. Uruchomić Gradle z `--warning-mode all`. Przypisać ostrzeżenia o przestarzałych API do kodu projektu albo użytych wtyczek.
4. Zaktualizować `STACK.md` i `JOURNAL.md`, aby zapisać wersję Gradle, wynik kontroli oraz powód aktualizacji.
5. Zaktualizować stan wdrożenia w tym pliku dopiero po zakończeniu kontroli na launcherze.

#### Testy i odbiór

Testy JVM powinny obejmować:

- wariant kompaktowy i rozszerzony;
- przewijaną listę wszystkich zajęć bez stopki „Jeszcze {liczba}”;
- pusty dzień, brak semestru, datę poza semestrem i błąd odczytu;
- kolizję, notatkę i długie metadane;
- zgodność kolejności zajęć z `ActivePlanProvider`.

Po implementacji uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.

Na launcherze Androida 12 lub nowszego sprawdzić:

- mały, duży i pośredni rozmiar oraz zmianę rozmiaru w obu osiach;
- jasny i ciemny motyw;
- brak semestru, pusty dzień, jedno i wiele zajęć;
- długą nazwę zajęć, sali oraz prowadzącego;
- kolizję i notatkę;
- odświeżenie po zmianie danych bez ponownego dodawania widgetu;
- otwarcie ekranu „Dzisiaj” po zimnym i ciepłym starcie.

Kryterium zakończenia: żaden wariant nie ucina czasu, nie nakłada tekstów i nie wymaga koloru do zrozumienia informacji. Duży wariant wykorzystuje dodatkowe miejsce na pełniejsze metadane i czytelniejsze odstępy.

### 13.2. Wizualne dopracowanie widgetu

Zmiana ma poprawić hierarchię i atrakcyjność widgetu bez zwiększania liczby informacji ani pogarszania czytelności. Ostateczne odcienie, odstępy i gęstość zatwierdzić po porównaniu na launcherze w jasnym i ciemnym motywie.

Status: niewykonane. Wcześniejsze etapy widgetu nie obejmują osobnego tła nagłówka, pilla liczby kolizji, kolorowej nazwy kierunku, lekkiego ostrzeżenia ani stanów „Teraz” i „Następne”.

1. Wydzielić nagłówek subtelnie odmiennym neutralnym tłem. Data pozostaje informacją główną. W drugim wierszu pokazać pill tygodnia A/B, liczbę zajęć oraz mały jasnoczerwony pill liczby kolizji, gdy liczba jest większa od zera. Ten układ zastępuje wcześniejszą decyzję o czerwonym tekście kolizji bez pilla w nagłówku.
2. Zachować spokojne neutralne tło całego widgetu i zaokrąglony kontener. Nie tworzyć osobnej pełnej karty dla każdego wpisu, ponieważ ograniczałoby to miejsce na plan.
3. Połączyć pionowy pasek kierunku z wizualną linią czasu. Dopuszczalna jest mała kropka przy początku przedziału i cienka linia w pełnym kolorze kierunku. Czas pozostaje w stałej kolumnie i nie może być ucinany.
4. Nazwę kierunku oznaczyć jego kolorem. W wąskim wariancie użyć krótkiego kolorowego tekstu albo małego pilla. Nazwa zawsze towarzyszy kolorowi.
5. Rozdzielić kierunek od lokalizacji. W pierwszym wierszu metadanych pokazać kierunek, w następnym najważniejszą lokalizację. Prowadzącego pokazywać tylko w wariantach, w których mieści się bez wypierania czasu, nazwy, kierunku, sali lub kolizji.
6. Zmniejszyć wizualny ciężar kolizji przy zajęciach. Zamiast dużego czerwonego bloku użyć jasnego tła ostrzegawczego, małej ikony lub znacznika, tekstu „Kolizja {zakres}” oraz nazwy drugich zajęć w kolejnym wierszu. Zakres i druga nazwa muszą pozostać dostępne bez polegania na kolorze.
7. Separator renderować wyłącznie między zajęciami. Wewnątrz wpisu budować hierarchię przez odstępy, wagę tekstu i role kolorów.
8. Dodać prezentacyjne stany „Teraz” i „Następne” tylko wtedy, gdy mieszczą się w danym progu rozmiaru. Zakończone zajęcia można lekko przygasić. Stan wynika z czasu odczytanego przez wstrzyknięty `Clock` podczas odświeżenia i nie może sugerować aktualizacji co minutę.
9. Zachować role kolorów: kolor kierunku dla osi czasu i nazwy kierunku, czerwony dla kolizji, niebieski lub indygo dla tygodnia A/B oraz neutralny dla lokalizacji, prowadzącego i zakończonych zajęć.
10. Dla każdego progu rozmiaru ustalić jawnie widoczne metadane, maksymalną liczbę linii oraz obecność stanów „Teraz” i „Następne”. Nie polegać na przypadkowym przycinaniu przez `RemoteViews`.

Odbiór na launcherze obejmuje mały, pośredni i duży rozmiar, oba motywy, brak kolizji, jedną i kilka kolizji, długie nazwy, trwające, następne i zakończone zajęcia oraz brak danych. Porównać co najmniej dwa warianty odcieni nagłówka i intensywności tła kolizji.

Kryterium zakończenia: użytkownik najpierw odczytuje datę, czas i nazwę zajęć, następnie kierunek oraz lokalizację, a dopiero później szczegóły kolizji. Żaden wariant nie ucina czasu, nie wymaga koloru do zrozumienia stanu ani nie oddaje kolizji większej powierzchni niż podstawowym informacjom o zajęciach.

## 14. Ustawienia i dane

Główny ekran ustawień ma służyć do szybkiego odczytu i przejścia do właściwego obszaru. Nie umieszczać na nim rozbudowanych formularzy, list zarządzania ani rozwijanych bloków. Topbar „Ustawienia” jest jedynym nagłówkiem strony. Usunąć lokalną etykietę „USTAWIENIA”, nagłówek „Semestry i wygląd” oraz opis powtarzający zakres ekranu.

Status: reorganizacja niewykonana. Działają wybór semestru, motyw, eksport, import i ustawienia powiadomień, ale ich kontrolki nadal znajdują się na głównym ekranie. Widoczne są stary nagłówek i rozwijane sekcje danych oraz powiadomień. Brakuje osobnych ekranów „Semestry”, „Powiadomienia” i „Dane” oraz ustawienia progu okienka. Podgląd importu ma już własny ekran.

Ekran główny dzieli ustawienia na sekcje:

1. „Plan”:
   - wybór aktywnego semestru, ponieważ wpływa na ekrany, widget i powiadomienia;
   - pozycja „Zarządzaj semestrami” prowadząca do osobnego ekranu.
2. „Wygląd”:
   - pozycja „Motyw” z bieżącą wartością „Systemowy”, „Jasny” albo „Ciemny”; dopóki jest to jedyne ustawienie wyglądu, wybór może pozostać na ekranie głównym.
3. „Powiadomienia”:
   - pozycja „Powiadomienia o kolizjach” z wartością „Włączone” albo „Wyłączone”;
   - drugi wiersz podsumowania pokazuje ustawioną godzinę i wyprzedzenie, np. „20:00 dzień wcześniej, 30 min przed zajęciami”;
   - pozycja prowadzi do osobnego ekranu ustawień powiadomień.
4. „Dane”:
   - pozycja „Kopia zapasowa i import” prowadząca do osobnego ekranu danych.
5. „O aplikacji”:
   - informacja o wersji jako zwarty wiersz; osobny ekran dodać dopiero wraz z licencjami albo większą liczbą informacji.

Sekcje umieścić w neutralnych kontenerach i rozdzielić odstępem 16 dp. Wiersze tej samej sekcji mogą używać subtelnych separatorów. Każdy wiersz pokazuje nazwę, bieżącą wartość lub krótkie podsumowanie i ikonę przejścia, jeśli otwiera ekran podrzędny. Ikony Material są pomocnicze i nie zastępują tekstu. Nie nadawać wszystkim pozycjom wagi przycisku głównego.

Ekran „Semestry” zawiera listę semestrów, wybór aktywnego, konfigurację, usuwanie i akcję „Dodaj semestr”. Konfiguracja przypisań kierunków, wspólnych lub osobnych kalendarzy i korekt A/B pozostaje częścią przepływu wybranego semestru. Lista globalnych kierunków i kolorów jest dostępna z tego przepływu. „Dodaj semestr” nie jest główną akcją całych ustawień.

Ekran „Powiadomienia” zawiera główny przełącznik, osobne przełączniki obu rodzajów powiadomień, godzinę wieczorną oraz wyprzedzenie przed zajęciami.

Ekran „Dane” zawiera eksport, import oraz opis skutków pełnego zastąpienia danych. Globalny próg długości okienka, domyślnie 30 minut, umieścić w ustawieniach planu. Jeśli przybędzie więcej ustawień planu niezwiązanych z semestrem, wydzielić dla nich osobny ekran zamiast rozbudowywać ekran główny.

Ekrany podrzędne mają własne trasy w jednym `NavHost`, przewidywalny systemowy powrót i tytuł w topbarze. Stan ekranu głównego po powrocie nie może się resetować ani automatycznie otwierać innej sekcji.

Eksport i import używają lokalnego pliku JSON z polem `schemaVersion`. Import obecnie przyjmuje tylko wersję 2 formatu; zgodność ze starszymi eksportami nie jest wymagana przed udostępnieniem aplikacji testerom.

Eksport i import są zaimplementowane. Użytkownik wybiera plik przez systemowy wybór dokumentu. Format zawiera globalne kierunki, semestry, przypisania, kalendarze akademickie, zajęcia z tekstem prowadzącego, korekty, notatki i zmiany wystąpień.

Import powinien:

- sprawdzić poprawność struktury pliku;
- pokazać podsumowanie danych przed zapisaniem;
- ostrzec, że operacja zastąpi wszystkie lokalne dane;
- po potwierdzeniu wykonać pełne zastąpienie w jednej transakcji;
- przy błędzie pozostawić dotychczasową bazę bez zmian.

## 15. Modularność i orientacyjny podział kodu

Poniższy podział pokazuje odpowiedzialności. Nazwy plików i katalogów można dopasować podczas implementacji bez zmiany granic między danymi, logiką planu, interfejsem i widgetem.

```text
app/
├── data/
│   ├── database/
│   │   ├── AppDatabase
│   │   ├── ClassDao
│   │   ├── StudyProgramDao
│   │   ├── SemesterDao
│   │   ├── SemesterProgramDao
│   │   ├── AcademicCalendarDao
│   │   ├── WeekOverrideDao
│   │   ├── OccurrenceNoteDao
│   │   └── OccurrenceChangeDao
│   ├── entity/
│   │   ├── ClassEntity
│   │   ├── StudyProgramEntity
│   │   ├── SemesterEntity
│   │   ├── SemesterProgramEntity
│   │   ├── AcademicCalendarEntity
│   │   ├── WeekOverrideEntity
│   │   ├── OccurrenceNoteEntity
│   │   └── OccurrenceChangeEntity
│   ├── repository/
│   │   ├── SemesterRepository
│   │   ├── ScheduleRepository
│   │   ├── RoomSemesterRepository
│   │   └── RoomScheduleRepository
│   └── preferences/
│       └── SettingsPreferences
├── domain/
│   ├── WeekCalculator
│   ├── ScheduleResolver
│   ├── CollisionDetector
│   └── usecase/
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
    ├── JsonExportCodec
    └── PlanBackupService
```

Logika obliczania planu, tygodni i kolizji powinna być niezależna od Compose. Ułatwi to testowanie i zapewni spójne dane na ekranie oraz w widgetach. ViewModele należą do `ui`. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Repozytoria odpowiadają obszarom danych, nie pojedynczym tabelom ani ekranom. Pakiet `export` nie jest częścią Room. Koin składa zależności na granicy aplikacji. Szczegóły granic: `ARCHITECTURE.md`.

## 16. Kryteria ukończenia MVP

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

## 17. Funkcje poza pierwszym zakresem

Na początku nie dodawać:

- logowania i kont użytkowników;
- backendu, Firebase i synchronizacji w chmurze;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu.
