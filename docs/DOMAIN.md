# Reguły planu i model domenowy

Ten dokument zawiera zaakceptowane reguły semestrów, kierunków, wystąpień i kolizji. Granice warstw i repozytoriów są w `ARCHITECTURE.md`.

## Semestr i tygodnie A/B

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

## Kierunki

Model `StudyProgram`:

- `id`;
- `name`;
- `color`.

Aplikacja nie jest ograniczona do dwóch kierunków. Kierunek istnieje niezależnie od semestru i może być użyty ponownie. `SemesterProgram` łączy go z semestrem oraz kalendarzem. Kolor kierunku jest widoczny na liście zajęć, w filtrach i w widgetach.

## Prowadzący

Prowadzący jest opcjonalnym polem tekstowym zajęć. Aplikacja nie utrzymuje osobnej kartoteki, identyfikatorów, adresów e-mail ani tytułów prowadzących.

## Model danych zajęć

Model aplikacyjny `ClassItem` oraz odpowiadający mu `ClassEntity` powinien zawierać:

- `id`;
- `semesterProgramId`;
- `name`;
- `type`;
- `teacherName` - opcjonalne;
- `dayOfWeek`;
- `startTime`;
- `endTime`;
- `room` - opcjonalne;
- `building` - opcjonalne;
- `group` - opcjonalne;
- `recurrence`;
- `date` - używane dla zajęć jednorazowych;
- `classNote` - opcjonalna notatka wspólna dla wszystkich wystąpień;

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
- `occurrenceDate` - data oryginalna terminu, czyli data wynikająca z planu cyklicznego, a dla zajęć jednorazowych ich data;
- `body`.

Notatka należy do terminu, nie do dnia w kalendarzu. Przeniesienie, zmiana, odwołanie i przywrócenie terminu nie zmieniają notatki: po przeniesieniu jest widoczna przy terminie w nowej dacie, po odwołaniu pozostaje zapisana i wraca po przywróceniu. W bazie obowiązuje najwyżej jedna notatka do danego wystąpienia zajęć. Notatka do zajęć jest przechowywana przy `ClassEntity`, a `OccurrenceNote` pozostaje osobną tabelą, aby zmiana jednej daty nie modyfikowała pozostałych wystąpień.

## Zmiany pojedynczych wystąpień

Stały wpis tygodniowy nie wystarcza do obsługi odwołanych i przeniesionych zajęć. Model `OccurrenceChange` opisuje zmianę jednego terminu zajęć cyklicznych:

- `id`;
- `classId`;
- `originalDate` - data wystąpienia wynikająca z planu cyklicznego;
- `kind`: `CANCELLED` albo `MODIFIED`;
- `targetDate` - używane przy przeniesieniu na inną datę;
- opcjonalnie nowa godzina rozpoczęcia i zakończenia, sala, budynek, prowadzący oraz notatka.

Z ekranu szczegółów konkretnego terminu użytkownik może wybrać:

- „Odwołaj ten termin”;
- „Zmień tylko ten termin”;
- „Przenieś ten termin”;
- „Przywróć termin”, jeśli wcześniej zapisano zmianę.

Odrabianie albo inne dodatkowe spotkanie jest zapisywane jako `ClassEntity` z `recurrence = ONCE` i konkretną datą. Formularz może skopiować nazwę, kierunek, prowadzącego i typ z istniejących zajęć, ale zapis pozostaje niezależny od cyklu.

Zmiany wystąpień są stosowane po rozwinięciu planu cyklicznego i przed wykrywaniem kolizji. Usunięcie albo edycja zmiany nie modyfikuje bazowego wpisu zajęć.

Wystąpienie identyfikują zajęcia i data oryginalna terminu, a nie data, w której termin faktycznie się odbywa. Para jest unikalna, bo zajęcia mają najwyżej jeden termin z danej daty planu. Dzięki temu termin przeniesiony na dzień, w którym te same zajęcia mają zwykły termin, pozostaje osobnym wystąpieniem z własną kolizją, zmianą i notatką.

## Okienka

Okienko jest przerwą dłuższą niż ustawiony próg między końcem jednego bloku zajęć a początkiem następnego. Liczyć je ze wspólnego planu wszystkich kierunków aktywnego semestru, także gdy kierunki używają różnych kalendarzy akademickich. Domyślny próg wynosi 30 minut, więc przerwa trwająca dokładnie 30 minut nie jest okienkiem. Nie liczyć czasu przed pierwszymi ani po ostatnich zajęciach dnia. Odwołane zajęcia pominąć, a zmiany i przeniesienia uwzględnić. Nakładające się zajęcia najpierw połączyć w blok czasu, aby kolizja nie tworzyła fałszywego okienka.

Obliczanie liczby okienek należy do domeny i korzysta z tego samego aktywnego planu co wykrywanie kolizji.

## Kolizje

Kolizja występuje, gdy dwa aktywne zajęcia tego samego dnia mają przedziały czasu, które się nakładają. Dotyczy to także zajęć z dwóch różnych kierunków. Przedziały stykające się końcem i początkiem, np. 10:00–11:00 oraz 11:00–12:00, nie są kolizją.

Przykład:

```text
Programowanie  10:00–11:30
Matematyka     11:00–12:30
```

Wynik: kolizja trwająca 30 minut.

Kolizję należy oznaczyć przy obu zajęciach i pokazać jej czas trwania po wejściu w szczegóły. Jest to ostrzeżenie i informacja o ograniczeniu planu, a nie komunikat o winie użytkownika. Aplikacja nie proponuje zmiany terminu, nie wybiera rozwiązania za użytkownika i nie modyfikuje planu automatycznie. Użytkownik sam decyduje, czy skontaktować się z uczelnią, opuścić jedno z zajęć albo ręcznie zapisać zmianę wystąpienia.
