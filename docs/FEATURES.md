# Funkcje i zachowanie interfejsu

Ten dokument opisuje docelowe zachowanie ekranów i widgetu. Bieżące braki są w `KNOWN_ISSUES.md`, status zadań w `QUEUE.md`, a najbliższe kroki w `PLAN.md`.

## Nawigacja

Telefon działa w orientacji pionowej, a urządzenie o najkrótszym boku od 600 dp w obu orientacjach (`ARCHITECTURE.md`, „Praktyczne mobile-first”).

### Start aplikacji

Przy uruchomieniu Android 12+ pokazuje systemowy ekran startowy z ikoną aplikacji. Aplikacja przytrzymuje go, dopóki nie odczyta zapisanego motywu, najwyżej 1 s. Potem, do czasu wczytania aktywnego planu, widać ekran ładowania z pełną nazwą „Mój Akademicki Kalendarz” na tle motywu; znika on najpóźniej po 2 s i nie wraca po obrocie ekranu. Ekran „Dzisiaj” nie pokazuje stanu „Brak aktywnego semestru”, zanim baza odpowie. Wybrany w aplikacji motyw jest przekazywany systemowi (`UiModeManager.setApplicationNightMode`), więc ekran startowy i tło okna mają ten sam motyw co aplikacja. Ekran startowy pokazuje tylko tło motywu, bez ikony. Po jego zniknięciu ekran ładowania gra animację „Rozkwit” (1,6 s): najpierw pojawia się makówka na białym kole, potem pięć płatków kolejno wyrasta wokół niej z lekkim obrotem, a pod kwiatem od początku animacji rozwija się od środka pełna nazwa. Animacja gra przy zimnym starcie i zawsze do końca; plan wczytuje się w tym czasie w tle (decyzja z 2026-09-27). Jeśli dane nie są jeszcze gotowe, jaśniejsza fala obiega płatki co 1,5 s; ekran ładowania znika przez przenikanie (250 ms). Start w działającym procesie pokazuje od razu gotowy kwiat i nazwę tylko na czas wczytywania. Przy wyłączonych animacjach systemu (skala animacji 0) kwiat i nazwa są statyczne.

### Dolny pasek

Dolny pasek zawiera trzy pozycje:

- **Dzisiaj** - zajęcia z bieżącego dnia;
- **Plan** - tygodniowy plan zajęć;
- **Dodaj** - formularz nowych zajęć.

Od 600 dp szerokości okna te same pozycje są w bocznym pasku po lewej stronie, a dolnego paska nie ma.

Ustawienia są dostępne z menu w prawym górnym rogu. Kliknięcie zajęć na ekranie „Dzisiaj” lub „Plan” otwiera ekran szczegółów i edycji.

Jeśli aplikacja nie ma jeszcze semestru, ekran „Dzisiaj” pokazuje stan pusty z przyciskiem „Skonfiguruj plan” i akcją „Pobierz plan z konta Google”, która otwiera ekran synchronizacji. Gdy połączona synchronizacja wymaga działania (wybór wersji, ponowna zgoda Google albo trwały błąd), „Dzisiaj” pokazuje pod nagłówkiem ostrzeżenie prowadzące do ekranu synchronizacji: przy konflikcie wersji „Plan różni się na telefonie i na Dysku” z akcją „Wybierz wersję”, przy innym problemie jego opis z akcją „Otwórz”. Kreator otwiera się po tej akcji i tworzy pierwszy semestr, kierunek, kalendarz oraz przypisanie. Ustawienia pozwalają później dodawać, wybierać, konfigurować i usuwać semestry.

Gdy istnieją już globalne kierunki, krok „Dodaj kierunek” w kreatorze pozwala wybrać „Nowy kierunek” albo „Wybierz istniejący”. Wybrany istniejący kierunek zachowuje nazwę i kolor, a kreator nie tworzy jego kopii. Po zapisie kroku wybór kierunku jest zablokowany do końca kreatora, bo zmiana zmieniłaby nazwę współdzielonego kierunku albo zostawiła w semestrze drugie przypisanie; nazwę i kolor kierunku utworzonego w kreatorze nadal można poprawić.

Krok „Dodaj zajęcia” pokazuje kierunki zapisane w semestrze („Kierunek w semestrze” albo „Kierunki w semestrze” z listą nazw) i akcję „Dodaj kolejny kierunek” między dodaniem zajęć a zakończeniem kreatora, także dla semestru nieaktywnego. Akcja otwiera formularz „Dodaj kolejny kierunek” dla tego samego semestru: pusta nazwa, kolor o odcieniu odległym od kierunków już dodanych w kreatorze, wybór „Nowy kierunek” albo „Wybierz istniejący” bez kierunków już przypisanych do semestru oraz sekcja „Tygodnie A/B” z opcjami „Wspólne z pierwszym kierunkiem” (domyślnie, ten sam kalendarz i te same korekty) i „Osobne dla tego kierunku” (własny kalendarz z tymi samymi datami). Zapis używa tych samych operacji co dodanie kierunku w ustawieniach semestru, wraca do kroku „Dodaj zajęcia” i pokazuje „Dodano kierunek”; błąd zostawia formularz z wpisanymi danymi i pokazuje „Nie udało się dodać kierunku.”. „Wstecz” w tym formularzu wraca do kroku „Dodaj zajęcia” bez zapisu. Kierunki można też dodawać później w ustawieniach semestru.

Semestr utworzony w kreatorze staje się aktywny tylko wtedy, gdy dzisiejsza data mieści się w jego kalendarzu albo gdy nie ma aktywnego semestru (`DOMAIN.md`). W przeciwnym razie komunikat po zapisie brzmi „Utworzono semestr i kierunek. Aktywny semestr się nie zmienił.”, a krok „Dodaj zajęcia” pokazuje informację „Semestr zapisany” oraz akcje „Ustaw jako aktywny i dodaj zajęcia”, „Zakończ” (powrót do ustawień) i „Wstecz”. Formularz zajęć zapisuje do aktywnego semestru, więc kreator nie otwiera go dla semestru nieaktywnego.

„Wstecz” w krokach kreatora i „Wróć do ustawień” w kroku „Utwórz semestr” są przyciskami tekstowymi po lewej stronie, pod pozostałymi akcjami. Systemowy gest wstecz robi to samo co ta akcja; w kroku „Utwórz semestr” bez akcji „Wróć do ustawień” zamyka kreator (`ARCHITECTURE.md`, sekcja 4).

## Ekran „Dzisiaj”

To domyślny ekran otwierany po uruchomieniu aplikacji. Od 840 dp szerokości okna karta podsumowania stoi po lewej, a lista zajęć po prawej, łącznie najwyżej 1040 dp; bez aktywnego semestru ekran zostaje w jednej kolumnie.

Powinien pokazywać:

- dzień tygodnia i pełną datę;
- gradientową kartę podsumowania z nagłówkiem „Twój plan na dziś”, a w dniu bez zajęć „Dziś bez zajęć”, oraz trzema równymi kolumnami „Zajęcia”, „Kolizje” i „Okienka”; każda kolumna ma etykietę u góry i liczbę poniżej;
- czerwony kolor liczby kolizji, gdy jest większa od zera, oraz zielony, gdy wynosi zero; pozostałe liczby zachowują neutralny kolor;
- wszystkie aktywne zajęcia w kolejności od najwcześniejszego;
- kolor kierunku;
- oznaczenie wykrytych kolizji;
- stan pusty, gdy danego dnia nie ma zajęć.

Definicja okienka i reguła jego liczenia są w `DOMAIN.md`, sekcja „Okienka”. Użytkownik może zmienić próg globalnie w ustawieniach.

`TodayViewModel` umieszcza liczbę zajęć, unikalnych kolizji i okienek w stanie widoku. Karta zachowuje obecny gradient w motywie jasnym i nie pokazuje ozdobnej ikony. W motywie ciemnym używa ciemniejszego gradientu `#2C3F94` do `#1C2A6A` z krawędzią 1 dp w bieli o kryciu 8% zamiast cienia, aby nie była najjaśniejszym elementem ekranu (decyzja z 2026-09-29, I-63). Nagłówek karty jest pogrubiony, większy od etykiet kolumn (17 sp), w pełni biały i oznaczony semantycznie jako nagłówek. Przy braku zajęć nagłówek brzmi „Dziś bez zajęć” zamiast „Twój plan na dziś”, a karta pokazuje trzy wartości równe zero, bez dodatkowego tekstu pod liczbami. Pionowe separatory między kolumnami są wyraźnie widoczne na gradiencie (biel 50% krycia, 40 dp wysokości). Układ musi zachować czytelność i semantykę przy szerokości 320 dp oraz w motywie jasnym i ciemnym. Etykiety kolumn są w pełni białe, mieszczą się w jednej linii i przy dużej czcionce zmniejszają się do 12 sp; przy skali 2,0 dopuszczalny jest wielokropek, a pełną etykietę czyta czytnik ekranu. Nad kartą są tylko data oraz wiersz z tygodniem A/B i semestrem, bez nadtytułu „Dzisiaj”, a tytuł listy „Zajęcia” nie ma podpisu o kolejności (decyzja z 2026-09-28: te elementy powtarzały zaznaczoną pozycję paska i stały porządek; nagłówek karty podsumowania zostaje).

### Struktura karty zajęć

Kartę zajęć zbudować jako czytelną siatkę z dwiema głównymi kolumnami. Lewa kolumna ma stałą szerokość i pokazuje godzinę rozpoczęcia oraz zakończenia, a pod nimi ikonę stanu terminu, jeśli termin jest odwołany, zmieniony (także przeniesiony) albo jednorazowy (wariant bez pilli z 2026-09-28). Prawa zawiera dane zajęć. Kolumny rozdziela subtelny pionowy separator. W prawej części użyć cienkich poziomych separatorów między informacjami podstawowymi, statusem kolizji i notatkami. Nie obramowywać każdej komórki osobno.

Kolejność sekcji w prawej kolumnie:

1. Nazwa zajęć.
2. Nazwa kierunku w kolorze kierunku oraz neutralny tekst typu zajęć.
3. Sala, budynek i prowadzący.
4. Status kolizji, jeśli występuje.
5. Notatki, jeśli występują.

Pionowy pasek przy krawędzi karty używa koloru kierunku dopasowanego do kontrastu co najmniej 3:1 z tłem karty. Nazwa kierunku jest pogrubionym tekstem w tym samym odcieniu, przyciemnionym w motywie jasnym i rozjaśnionym w ciemnym tak, aby miał kontrast co najmniej 4,5:1 z tłem karty; zapisany kolor kierunku się nie zmienia. Nie kolorować całej karty według kierunku. Typ zajęć pozostaje neutralnym tekstem obok nazwy kierunku. Kolor kierunku zawsze występuje razem z jego nazwą, więc nie jest jedynym nośnikiem informacji.

Każda notatka to jeden wiersz: ikona i pełna treść, bez etykiety tekstowej i bez tła. Notatka do zajęć ma ikonę notatki w kolorze akcentu, a notatka do terminu ikonę kalendarza w kolorze zmiany; ten sam zapis obowiązuje w „Planie” dla każdego dnia. Jeśli istnieją oba rodzaje notatek, pokazać dwa osobne wiersze. Pełną nazwę rodzaju notatki czyta czytnik ekranu i pokazują pola w szczegółach terminu. Kolizja zachowuje pomarańczowy wiersz ostrzegawczy z ikoną i zakresem nakładania, a w drugiej linii nazwą drugich zajęć („Z: {nazwa}”, kilka nazw po przecinku).

Ikona stanu pod godzinami: odwołane (kalendarz z krzyżykiem, kolor błędu), zmienione lub przeniesione (kalendarz z ołówkiem, kolor zmiany), jednorazowe (cyfra 1 w kwadracie, kolor zajęć jednorazowych). Ikona nie ma własnego opisu, bo stan czyta opis całej karty. Odwołane zajęcia mają dodatkowo przekreśloną nazwę i pasek w kolorze błędu. Szczegóły terminu pokazują stan ikoną i słowem tylko dla terminu odwołanego, zmienionego, przeniesionego albo jednorazowego (termin jednorazowy zawsze ma „Jednorazowe”) i opisują zmianę: poprzednią i nową salę, poprzednie i nowe godziny albo datę bazową i nową datę. Zwykły termin nie ma wiersza stanu. Kolizję szczegóły pokazują tym samym wierszem ostrzegawczym co karta. Jedyną akcją główną szczegółów jest „Zmień termin” albo „Przywróć termin”; zapis notatek to akcje drugorzędne.

Karta musi zachować wspólne wyrównanie wierszy, poprawne zawijanie długich nazw i notatek, kontrast oraz czytelność przy szerokości 320 dp. Semantyka czytnika ekranu ma przekazywać godziny, nazwę, status („Odwołane”, „Zmienione”, „Jednorazowe”), kierunek, typ, tydzień A/B, metadane, kolizję i zakres każdej notatki w logicznej kolejności i pomijać puste części. Odwołanej karty nie przyciemnia się w całości: stan pokazują przekreślenie nazwy, ikona odwołania pod godzinami i pasek w kolorze błędu. Kolumna godzin ma co najmniej 48 dp i rośnie z rozmiarem czcionki, aby godzina nigdy nie była ucięta.

Przykładowy element:

```text
08:00–09:30
Programowanie
Laboratorium, Informatyka
Sala L204, dr Jan Kowalski
```

Po przekroczeniu północy ekran pokazuje plan nowego dnia przy ponownym otwarciu lub wznowieniu aplikacji. Odczyt aktualnej daty nie wymaga ciągłego działania aplikacji w tle.

## Ekran „Plan”

Ekran oferuje dwa równorzędne sposoby przeglądania planu: **Lista** i **Kalendarz**. Ostatnio wybrany sposób może być zapamiętany lokalnie. Ekran nie ma nagłówka treści; jego nazwę pokazuje zaznaczona pozycja paska nawigacji, a przełącznik widoku stoi na górze.

### Widok „Lista”

Widok listy na telefonie składa się z:

- wyboru dnia: poniedziałek–niedziela, z dwuliterowymi skrótami („Pn” do „Nd”) jak w nagłówku kalendarza;
- przesuwania między dniami gestem;
- przechodzenia między tygodniami oraz powrotu do bieżącego tygodnia;
- daty i oznaczenia A/B widocznego przy przeglądanym tygodniu;
- akcji „Zmień tydzień A/B”, z wyborem zakresu „Tylko ten tydzień” lub „Od tego tygodnia”; gdy filtr „Wszystkie” obejmuje kilka kalendarzy, wiersz tygodnia nie jest akcją i pokazuje „Wybierz kierunek w polu „Kierunek”, aby zmienić tydzień.”; nazwa tygodnia stoi nad źródłem korekty, a przy dużej czcionce „Zmień” przechodzi do osobnej linii;
- pola wyboru „Kierunek” z opcją „Wszystkie” i poszczególnymi kierunkami, widocznego bez rozwijania dodatkowej sekcji;
- listy zajęć posortowanej według godziny;
- oznaczeń wykrytych kolizji.

Plan uwzględnia aktualny semestr, automatyczne lub ręcznie skorygowane oznaczenie A/B, zmiany pojedynczych wystąpień oraz zajęcia jednorazowe. Ręcznie skorygowany tydzień jest wyraźnie oznaczony; użytkownik może usunąć korektę i wrócić do automatycznego wyniku.

### Widok „Kalendarz”

Widok kalendarza pokazuje jeden miesiąc i zawiera:

- przejście do poprzedniego i następnego miesiąca;
- powrót do bieżącej daty;
- oznaczenie zajęć w każdym dniu znacznikami w kolorze kierunku dopasowanym do kontrastu 3:1 z tłem: wypełniona kropka dla zwykłego terminu i pierścień dla terminu zmienionego lub przeniesionego; kolory stanów nie występują, bo mogłyby się pokryć z kolorem wybranym dla kierunku (decyzja z 2026-09-28); przy 1 do 3 zajęciach znaczniki mają 8 dp, przy 4 i 5 zajęciach 6 dp, a od 6 zajęć dzień pokazuje cztery znaczniki 6 dp i znak plus; pod siatką legenda z nazwami kierunków i pierścieniem „Zmieniony termin”;
- czytelne oznaczenie dni z odwołanymi, zmienionymi lub jednorazowymi zajęciami;
- wybór dnia i listę jego aktywnych zajęć pod kalendarzem;
- pole „Pokaż odwołane” pod legendą i obrysowaną akcję z ikoną plusa „Dodaj termin jednorazowy” pod nagłówkiem wybranego dnia, nad listą jego zajęć; obie są widoczne bez rozwijania, a akcja nie znika pod długą listą.

### Dni wolne

Decyzja użytkownika z 2026-10-09, reguły w `DOMAIN.md`, sekcja „Dni wolne”. Wygląd wymaga wariantu do akceptacji przed implementacją.

Krok pierwszy (I-81):

- dialog „Dodaj dni wolne”: zakres dat w `DateRangePicker` z Material 3, opcjonalna nazwa i wybór kalendarzy, domyślnie wszystkie kalendarze semestru;
- wejście z kalendarza „Planu”: akcja „Oznacz jako wolny” pod nagłówkiem wybranego dnia, obok „Dodaj termin jednorazowy”, otwiera dialog z tym dniem jako zakresem;
- wejście z konfiguracji semestru: ekran „Dni wolne” obok korekt tygodni i kalendarzy, z listą zakresów (nazwa, daty, kalendarze), edycją i usuwaniem;
- „Dzisiaj” w dniu wolnym pokazuje „Dzień wolny” i nazwę zamiast „Dziś bez zajęć”, z zajęciami jednorazowymi i przeniesionymi, jeśli są;
- kalendarz „Planu” wyróżnia dni wolne bez znaczników pominiętych zajęć, a lista pokazuje w takim dniu komunikat z nazwą;
- widget pokazuje „Wolne” i nazwę.

Krok drugi (I-82):

- akcja „Dodaj święta ustawowe” na ekranie „Dni wolne”: lista polskich świąt ustawowych w zakresie kalendarza, z datami ruchomymi liczonymi lokalnie, z checkboxami przed dodaniem;
- w dialogu dni wolnych obejmujących cały tydzień pole „Po przerwie zacznij od tygodnia” (A albo B), które tworzy korektę „Od tego tygodnia”.

Kalendarz pokazuje wynik `ScheduleResolver`, dlatego musi być zgodny z ekranem „Dzisiaj”, listą planu i widgetem. Odwołane zajęcia mogą pozostać widoczne jako przekreślone tylko wtedy, gdy użytkownik włączy opcję „Pokaż odwołane”. Lista odwołanych terminów pochodzi z domeny (`cancelledOccurrences`), stosuje ten sam filtr kierunku co plan i pomija odwołania z dni, w które zajęcia po edycji już się nie odbywają. Licznik zajęć dnia liczy tylko zajęcia, które się odbywają. Domyślnie kalendarz pokazuje plan aktywny.

## Dodawanie i edycja zajęć

Tytuł „Dodaj zajęcia” albo „Edytuj zajęcia” jest w górnym pasku; treść zaczyna się krótkim opisem. Formularz kończą akcje „Dodaj do planu” (przy edycji „Zapisz zajęcia”) i „Anuluj”.

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
- grupa - pole opcjonalne;
- notatka do zajęć - opcjonalna, wspólna dla każdego wystąpienia tego wpisu;
- notatka do terminu - opcjonalna, przypięta do jednego terminu zajęć; po przeniesieniu terminu jest widoczna razem z nim w nowej dacie.

Podczas dodawania notatki użytkownik wybiera zakres: „Do tych zajęć” albo „Tylko do tego terminu”. Notatkę do konkretnego wystąpienia można dodać z ekranu szczegółów zajęć na ekranie „Dzisiaj” lub „Plan”. Jeśli istnieją oba typy, aplikacja pokazuje je osobno i nie nadpisuje notatki wspólnej.

Prowadzący jest opcjonalnym tekstem zapisanym przy zajęciach. Aplikacja nie prowadzi osobnej bazy prowadzących. Formularz powinien walidować, że nazwa, kierunek, godzina rozpoczęcia i zakończenia są uzupełnione, a godzina zakończenia jest późniejsza od rozpoczęcia. Zajęcia przechodzące przez północ są nieprawidłowe.

Jeśli edycja dnia, cyklu, daty albo kierunku sprawia, że zmiany terminów lub notatki do terminów przestaną być widoczne, formularz przed zapisem pokazuje dialog „Część danych przestanie być widoczna” z ich liczbą oraz akcjami „Anuluj” i „Zapisz”. Dane zostają zachowane (`DOMAIN.md`). Zmiana samych godzin, sali ani nazwy nie wywołuje dialogu.

Plan pobrany z Dysku Google nie zastępuje danych, dopóki otwarty jest formularz planu, dialog usuwania semestru albo ekran podglądu z niezapisanym szkicem lub otwartym potwierdzeniem. Przy połączonym koncie ostrzeżenie importu JSON dodaje, że zaimportowany plan trafi też na Dysk Google i na pozostałe połączone telefony. Aplikacja zastosuje go po zamknięciu formularza (`SYNC.md`, „Przebieg synchronizacji”).

Po usunięciu zajęć aplikacja powinna wymagać potwierdzenia. Edycja i usuwanie muszą aktualizować widget.

## Widget

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

### Układ i zachowanie widgetu

Zmiany mają poprawić hierarchię informacji, czytelność i wykorzystanie dostępnego miejsca. Widget zachowuje jasne tło, wysoki kontrast i prosty układ. Nie dodawać zdjęć, gradientów, cieni ani ozdobnych ikon.

#### Nagłówek i wiersze zajęć

1. Pokazać datę jako główny tekst nagłówka o rozmiarze 16 sp.
2. Pokazać tydzień A/B jako małą etykietę z delikatnym tłem.
3. Umieścić liczbę zajęć obok etykiety tygodnia albo wyrównać ją do prawej strony nagłówka.
4. Dodać do wiersza zajęć wąski pasek w kolorze kierunku. Zachować tekstową nazwę kierunku, ponieważ kolor nie może być jedynym nośnikiem informacji.
5. Użyć stałej kolumny czasu. Obok niej pokazać nazwę zajęć oraz osobny wiersz metadanych z salą i prowadzącym.
6. Pokazać kolizję jako krótki wiersz ostrzegawczy o czytelnym kontraście. Notatkę pokazać jak na karcie w aplikacji: osobny wiersz z ikoną i treścią, bez etykiety tekstowej i tła. Notatka do zajęć używa ikony notatki, a notatka do terminu ikony kalendarza.
7. Oddzielić zajęcia subtelnymi separatorami. Nie umieszczać każdego zajęcia w osobnej karcie.
8. Dopasować tło do systemowego promienia widgetów Androida.

#### Układ responsywny

1. Traktować widget jako kompaktowy, gdy ma mniej niż 240 dp szerokości albo mniej niż 160 dp wysokości.
2. W wariancie kompaktowym pokazać przewijaną listę dzisiejszych zajęć. Pokazać nazwę, czas i salę, pominąć prowadzącego oraz ikonę notatki, ale zachować alert kolizji.
3. Jeśli wariant kompaktowy nie mieści wszystkich zajęć, użyć przewijanej listy zamiast tekstu „Jeszcze {liczba}”.
4. W wariancie rozszerzonym pokazać przewijaną listę dzisiejszych zajęć wraz z salą, prowadzącym, kolizją oraz treścią notatek. Jeśli istnieją oba rodzaje notatek, pokazać dwa osobne wiersze.
5. Wiersze powinny wykorzystać pełną szerokość widgetu. Wariant rozszerzony ma używać większych odstępów i pełniejszych metadanych, a nie tylko zwiększać wysokość listy.
6. Ograniczać prowadzącego i lokalizację wielokropkiem. Nie ucinać czasu. Nazwa zajęć może zająć dwa wiersze, jeśli pozwala na to wysokość wariantu.
7. Ograniczyć maksymalny rozmiar widgetu do 360 na 420 dp, aby launcher nie tworzył nadmiernie pustego układu.
8. Dodać statyczny podgląd używany przez systemowy wybór widgetów.

#### Zachowanie i odświeżanie

1. Zachować kliknięcie całego widgetu i każdego wiersza prowadzące do ekranu „Dzisiaj”. Nie dodawać osobnych akcji w tej wersji.
2. Ustawić `updatePeriodMillis` na 60 minut. Natychmiastowe odświeżanie po zmianie danych nadal realizować przez obserwację Room.
3. Przy każdym odświeżeniu pobierać bieżącą datę przez `Clock` i używać wspólnego `ActivePlanProvider`.
4. Sprawdzić zimny i ciepły start po kliknięciu widgetu. Trasa docelowa nie może zależeć od ekranu otwartego wcześniej w aplikacji.
5. Nie gwarantować aktualizacji dokładnie o północy i nie dodawać dokładnych alarmów ani stałego procesu w tle.

#### Odbiór

Na launcherze Androida 12 lub nowszego sprawdzić:

- mały, duży i pośredni rozmiar oraz zmianę rozmiaru w obu osiach;
- jasny i ciemny motyw;
- brak semestru, pusty dzień, jedno i wiele zajęć;
- długą nazwę zajęć, sali oraz prowadzącego;
- kolizję i notatkę;
- odświeżenie po zmianie danych bez ponownego dodawania widgetu;
- otwarcie ekranu „Dzisiaj” po zimnym i ciepłym starcie.

Kryterium zakończenia: żaden wariant nie ucina czasu, nie nakłada tekstów i nie wymaga koloru do zrozumienia informacji. Duży wariant wykorzystuje dodatkowe miejsce na pełniejsze metadane i czytelniejsze odstępy.

### Wygląd widgetu

Zmiana ma poprawić hierarchię i atrakcyjność widgetu bez zwiększania liczby informacji ani pogarszania czytelności. Ostateczne odcienie, odstępy i gęstość zatwierdzić po porównaniu na launcherze w jasnym i ciemnym motywie.

1. Wydzielić nagłówek subtelnie odmiennym neutralnym tłem. Data pozostaje informacją główną. W drugim wierszu pokazać zwykły tekst „Tydzień A/B, {liczba zajęć}” oraz czerwony tekst liczby kolizji, gdy liczba jest większa od zera. Widget nie używa pilli (decyzja z 2026-09-28, zastępuje wcześniejszy wariant z pillami tygodnia i kolizji).
2. Zachować spokojne neutralne tło całego widgetu i zaokrąglony kontener. Nie tworzyć osobnej pełnej karty dla każdego wpisu, ponieważ ograniczałoby to miejsce na plan.
3. Połączyć pionowy pasek kierunku z wizualną linią czasu. Dopuszczalna jest mała kropka przy początku przedziału i cienka linia w pełnym kolorze kierunku. Czas pozostaje w stałej kolumnie i nie może być ucinany.
4. Nazwę kierunku oznaczyć jego kolorem, jako krótki kolorowy tekst. Nazwa zawsze towarzyszy kolorowi.
5. Rozdzielić kierunek od lokalizacji. W pierwszym wierszu metadanych pokazać kierunek, w następnym najważniejszą lokalizację. Prowadzącego pokazywać tylko w wariantach, w których mieści się bez wypierania czasu, nazwy, kierunku, sali lub kolizji.
6. Zmniejszyć wizualny ciężar kolizji przy zajęciach. Zamiast dużego bloku użyć jasnego pomarańczowego tła ostrzegawczego z cienkim pomarańczowym paskiem, w tych samych kolorach co wiersz kolizji na karcie w aplikacji, i jednej linii „Kolizja {zakres}” albo „Kolizje: {zakresy}”. Kolor błędu nie występuje, bo kolizja nie jest winą użytkownika (`ARCHITECTURE.md`). Widget nie pokazuje nazwy drugich zajęć, bo ma mało miejsca; nazwę pokazują karta i szczegóły terminu w aplikacji (decyzja użytkownika z 2026-09-28). Zakres musi pozostać dostępny bez polegania na kolorze.
7. Separator renderować wyłącznie między zajęciami. Wewnątrz wpisu budować hierarchię przez odstępy, wagę tekstu i role kolorów.
8. Dodać prezentacyjne stany „Teraz” i „Następne” jako pogrubiony tekst w kolorze akcentu, bez tła, tylko wtedy, gdy mieszczą się w danym progu rozmiaru. Zakończone zajęcia można lekko przygasić. Stan wynika z czasu odczytanego przez wstrzyknięty `Clock` podczas odświeżenia i nie może sugerować aktualizacji co minutę.
9. Zachować role kolorów: kolor kierunku (pasek dopasowany do kontrastu 3:1 z tłem) dla osi czasu i nazwy kierunku, pomarańczowy kolor ostrzeżenia dla alertu kolizji, taki sam jak na karcie zajęć w aplikacji, i czerwony dla liczby kolizji w nagłówku (decyzja użytkownika z 2026-09-28, `ARCHITECTURE.md`), niebieski lub indygo dla tygodnia A/B oraz neutralny dla godzin, lokalizacji, prowadzącego i zakończonych zajęć. Kolor akcentu w wierszu mają tylko „Teraz” i „Następne”.
10. Dla każdego progu rozmiaru ustalić jawnie widoczne metadane, maksymalną liczbę linii oraz obecność stanów „Teraz” i „Następne”. Nie polegać na przypadkowym przycinaniu przez `RemoteViews`.

Odbiór na launcherze obejmuje mały, pośredni i duży rozmiar, oba motywy, brak kolizji, jedną i kilka kolizji, długie nazwy, trwające, następne i zakończone zajęcia oraz brak danych. Porównać co najmniej dwa warianty odcieni nagłówka i intensywności tła kolizji.

Kryterium zakończenia: użytkownik najpierw odczytuje datę, czas i nazwę zajęć, następnie kierunek oraz lokalizację, a dopiero później szczegóły kolizji. Żaden wariant nie ucina czasu, nie wymaga koloru do zrozumienia stanu ani nie oddaje kolizji większej powierzchni niż podstawowym informacjom o zajęciach.

## Ustawienia i dane

Główny ekran ustawień ma służyć do szybkiego odczytu i przejścia do właściwego obszaru. Nie umieszczać na nim rozbudowanych formularzy, list zarządzania ani rozwijanych bloków. Topbar „Ustawienia” jest jedynym nagłówkiem strony. Usunąć lokalną etykietę „USTAWIENIA”, nagłówek „Semestry i wygląd” oraz opis powtarzający zakres ekranu.

Ekran główny dzieli ustawienia na sekcje:

1. „Plan”:
   - wybór aktywnego semestru, ponieważ wpływa na ekrany, widget i powiadomienia;
   - pozycja „Zarządzaj semestrami” prowadząca do osobnego ekranu;
   - pozycja „Kierunki” prowadząca do listy globalnych kierunków.
2. „Wygląd”:
   - pozycja „Motyw” z bieżącą wartością „Systemowy”, „Jasny” albo „Ciemny”; dopóki jest to jedyne ustawienie wyglądu, wybór może pozostać na ekranie głównym.
3. „Powiadomienia”:
   - pozycja „Powiadomienia o kolizjach” z wartością „Włączone” albo „Wyłączone”;
   - drugi wiersz podsumowania pokazuje ustawioną godzinę i wyprzedzenie, np. „20:00 dzień wcześniej, 30 min przed zajęciami”;
   - pozycja prowadzi do osobnego ekranu ustawień powiadomień.
4. „Dane”:
   - pozycja „Kopia zapasowa i import” prowadząca do osobnego ekranu danych;
   - pozycja „Synchronizacja Google” z podsumowaniem „Wyłączona”, adresem konta albo „Wymaga działania”, prowadząca do osobnego ekranu. Bez konta ekran wyjaśnia, gdzie trafi plan, i ma akcję „Połącz konto Google”. Po połączeniu pokazuje konto, czas ostatniej synchronizacji, komunikat wymagający działania (z akcją „Połącz ponownie” albo „Wybierz wersję”), akcje „Synchronizuj teraz” i „Wyłącz synchronizację” oraz listę „Poprzednie wersje” z eksportem do JSON. Wybór wersji pokazuje dla telefonu i Dysku datę ostatniej zmiany („Zmieniony dziś o 12:20”, bez danych „Data zmiany nieznana”). Gdy aplikacja zna wspólny plan z ostatniej synchronizacji, dialog pokazuje „Co się różni (N)” z trzema pierwszymi różnicami (nazwa zajęć albo wpisu, co się zmieniło, wartość na telefonie i na Dysku) i akcję „Wybierz zmiany”; bez wspólnego planu pokazuje liczbę kierunków, semestrów i zajęć po obu stronach. Ekran „Wybierz zmiany” listuje wszystkie różnice z wyborem „Telefon” albo „Dysk” przy każdej; zajęcia zmienione na obu telefonach mają osobny wybór przy każdym zmienionym polu, na przykład sala z Dysku i notatka do zajęć z telefonu; domyślnie nic nie jest wybrane, licznik „Wybrano X z N” i „Zapisz plan” są aktywne dopiero przy pełnym wyborze. Wybory, które się wykluczają, pokazują komunikat błędu i obramowanie dotkniętych pozycji. Zapis zachowuje obie poprzednie wersje w „Poprzednie wersje” i wysyła wynik na Dysk (decyzja użytkownika z 2026-10-08, I-76, I-77). Wyłączenie pyta, czy zachować kopię na Dysku.
5. „Aktualizacje”:
   - wiersz „Sprawdź aktualizacje” uruchamia ręczne sprawdzenie i przed pierwszym sprawdzeniem nie ma drugiej linii, a potem pokazuje w niej wynik: „Sprawdzanie...”, „Masz najnowszą wersję”, „Dostępna wersja {wersja}”, „Wymaga nowszego Androida” albo „Nie udało się sprawdzić”; w trakcie sprawdzania wiersz jest nieaktywny;
   - gdy jest dostępna wersja albo trwa lub zakończyło się jej pobieranie, pod nim pojawia się wiersz „Aktualizacja do {wersja}” z ikoną przejścia, prowadzący do ekranu „Aktualizacja”; wynik sprawdzenia nie otwiera tego ekranu sam;
   - wiersz z przełącznikiem „Sprawdzaj przy uruchomieniu” i jednym zdaniem opisu pod nazwą.
6. „O aplikacji”:
   - zwarty wiersz pokazuje zainstalowaną wersję i prowadzi do ekranu „O aplikacji”.

Sekcje umieścić w neutralnych kontenerach i rozdzielić odstępem 16 dp. Wiersze tej samej sekcji mogą używać subtelnych separatorów. Każdy wiersz pokazuje nazwę, bieżącą wartość lub krótkie podsumowanie i ikonę przejścia, jeśli otwiera ekran podrzędny. Wiersz bez wartości do podsumowania, na przykład „Kierunki” albo „Kopia zapasowa i import”, pokazuje samą nazwę; opis nie powtarza nazwy innymi słowami. Ikony Material są pomocnicze i nie zastępują tekstu. Nie nadawać wszystkim pozycjom wagi przycisku głównego. Wiersze z akcją sięgają krawędzi karty, więc podświetlenie po naciśnięciu obejmuje cały wiersz; tekst ma 16 dp marginesu poziomego i 12 dp pionowego, a wiersz co najmniej 56 dp wysokości. Pola wyboru w tej samej karcie zachowują ten sam margines.

Ekran „Semestry” zawiera listę semestrów, wybór aktywnego, konfigurację, usuwanie i akcję „Dodaj semestr”. Układ od góry (decyzja użytkownika z 2026-09-28): pole wyboru aktywnego semestru; neutralny komunikat bez tytułu „Konfiguracja przypisań, kalendarzy i korekt należy do wybranego semestru.”; nagłówek „Lista semestrów”; obrysowany przycisk z ikoną plusa „Dodaj semestr”; karty semestrów z akcjami „Konfiguruj” i „Usuń” oraz oznaczeniem „Aktywny” przy aktywnym semestrze, bo przy przewiniętej liście pole wyboru znika z ekranu. Semestr nieaktywny pokazuje samą nazwę i akcje. Konfiguracja przypisań kierunków, wspólnych lub osobnych kalendarzy i korekt A/B pozostaje częścią przepływu wybranego semestru. Otwarcie konfiguracji nie zmienia aktywnego semestru, a powrót z ekranów „Kierunki”, „Kalendarze” i „Korekty tygodni” zachowuje niezapisane zmiany nazwy i dat. Lista globalnych kierunków i kolorów jest dostępna z sekcji „Plan” ustawień. Usunięcie kierunku z semestru wymaga potwierdzenia w dialogu, który podaje liczbę usuwanych zajęć i informuje, że znikną też ich notatki i zmiany terminów, a kierunek zostanie w innych semestrach. Usunięcie korekty tygodnia i nieużywanego kalendarza też wymaga potwierdzenia w dialogu z akcją „Usuń” w kolorze błędu. Wszystkie potwierdzenia usunięcia, także semestru w ustawieniach i zajęć w szczegółach terminu („Usuń zajęcia”), używają tego samego dialogu: tytuł, treść, przycisk tekstowy „Anuluj” i akcja usuwająca w kolorze błędu, zablokowane w trakcie usuwania (I-59). W formularzu korekty można wybrać dowolny dzień; aplikacja zapisuje poniedziałek jego tygodnia i pokazuje pod polem „Korekta obejmuje tydzień od poniedziałku {data}.”. „Dodaj semestr” nie jest główną akcją całych ustawień.

Kolor kierunku wybiera się w kreatorze, na ekranach dodawania i edycji kierunku w semestrze oraz w edycji kierunku w ustawieniach tym samym komponentem (decyzja użytkownika z 2026-09-28): dwa podglądy paska i nazwy kierunku, w motywie jasnym i ciemnym, dokładnie tak, jak pokaże je aplikacja; suwak „Odcień”, który zmienia tylko odcień, z torem czystych odcieni całego koła barw; suwak „Jasność”, który zmienia tylko jasność, z torem od czarnego przez czysty odcień do białego; pole „Kod koloru” (`#RRGGBB`), które przyjmuje każdy poprawny kod. Aplikacja zapisuje dokładnie wybrany kolor, a przy wyświetlaniu dopasowuje go do tła bieżącego motywu: pasek karty, kropki, znaczniki kalendarza i pasek widgetu do kontrastu co najmniej 3:1, a nazwę kierunku do 4,5:1. Gdy kolor wymaga dopasowania, pod podglądem pojawia się „W motywie jasnym kolor będzie ciemniejszy, aby był czytelny.” albo „W motywie ciemnym kolor będzie jaśniejszy, aby był czytelny.”. Kolory zapisane wcześniej zostają bez zmian, dopóki użytkownik ich nie zmieni.

Ekran „Kierunki” w ustawieniach pokazuje globalne kierunki: kropkę w kolorze kierunku, nazwę (powtórzone nazwy z numerem) i ikonę przejścia. Wiersz otwiera ekran „Edytuj kierunek” z nazwą, kolorem, „Zapisz kierunek” i „Anuluj”. Zmiana dotyczy wszystkich semestrów, planu i widgetu. Pusta nazwa jest odrzucana. Ekran „Edytuj kierunek” ma osobną akcję „Usuń kierunek” w kolorze błędu (decyzja użytkownika z 2026-10-08, I-73). Kierunek nieprzypisany do żadnego semestru usuwa się po potwierdzeniu w dialogu „Usunąć kierunek?”. Kierunek przypisany ma nieaktywną akcję i wyjaśnienie: „Kierunek jest używany w semestrach: {nazwy}. Aby go usunąć, najpierw usuń go z tych semestrów na ekranie Kierunki semestru.” Kaskadowego usuwania nie ma. Kropka koloru stoi też obok nazwy kierunku w wierszach ekranu „Kierunki” semestru i w opcjach pola „Istniejący kierunek”.

Każdy wiersz ekranu „Kierunki” w ustawieniach ma checkbox włączający i wyłączający kierunek (decyzja użytkownika z 2026-10-09, I-79). Wybór dotyczy kierunku we wszystkich semestrach i jest zapisany lokalnie na telefonie, jak aktywny semestr: nie trafia do eksportu ani synchronizacji. Wyłączony kierunek znika z „Dzisiaj”, „Planu” (także z opcji pola „Kierunek”), widgetu i powiadomień i nie jest liczony w kolizjach ani okienkach. Formularz zajęć, ekrany kierunków, semestry i eksport nadal widzą wszystkie kierunki. Zmiana od razu odświeża widget i przelicza alarmy powiadomień. Filtr „Kierunek” na „Planie” pozostaje osobnym, chwilowym wyborem tego jednego ekranu.

Szczegóły przyjęte przez użytkownika 2026-10-09 (makiety w artefakcie „MAK: dni wolne”, strona „Decyzje”):

- wiersz ma checkbox po lewej z obszarem dotyku 48 dp, a reszta wiersza (kropka koloru, nazwa, strzałka) dalej otwiera „Edytuj kierunek”; ukryty kierunek ma pod nazwą tekst „Ukryty na tym telefonie”; opis ekranu brzmi „Odznacz kierunek, aby ukryć jego zajęcia na tym telefonie. Nazwa i kolor są wspólne dla wszystkich semestrów.”;
- formularz zajęć pozwala wybrać ukryty kierunek; opcja ma dopisek „ukryty”, a po jej wyborze pod polem stoi „Ten kierunek jest ukryty na tym telefonie. Zajęcia zapiszą się, ale nie pojawią się w planie, dopóki go nie włączysz.”;
- gdy wszystkie kierunki aktywnego semestru są ukryte, „Dzisiaj” i „Plan” pokazują zamiast listy zajęć neutralny komunikat „Wszystkie kierunki są ukryte na tym telefonie.” z akcją „Kierunki” prowadzącą do ekranu „Kierunki”;
- gdy ukryta jest część kierunków aktywnego semestru, „Dzisiaj” pokazuje pod nazwą semestru wiersz z ikoną „Ukryty kierunek: {nazwa}” albo „Ukryte kierunki: {nazwy}”;
- przy pierwszym pobraniu planu z Dysku i przy ręcznym imporcie wybór ukrytych kierunków jest czyszczony, bo numery kierunków pochodzą wtedy z innego planu; przy kolejnych synchronizacjach zostają tylko numery kierunków, które nadal istnieją.

Ekran „Kierunki” semestru (decyzja użytkownika z 2026-09-28) pokazuje nagłówek „Przypisane kierunki”, pod nim obrysowany przycisk z ikoną plusa „Dodaj kierunek” i karty kierunków: kropka koloru, nazwa, kalendarz („wspólny” albo „osobny”) oraz akcje „Edytuj” i „Usuń”. „Dodaj kierunek” otwiera osobny ekran z formularzem: nowy albo istniejący kierunek, nazwa, kolor, wspólne albo osobne daty i tygodnie, akcje „Dodaj kierunek” i „Anuluj”. „Edytuj” otwiera ekran „Edytuj kierunek”, od góry: pole „Nazwa kierunku”; sekcja „Kalendarz w tym semestrze” (wybór kalendarza, gdy jest ich kilka, i „Rozdziel kalendarz” przy kalendarzu wspólnym; zmiana zapisuje się od razu po potwierdzeniu); paleta „Kolor kierunku”; informacja, że nazwa i kolor zmienią się we wszystkich semestrach; akcje „Zapisz kierunek” i „Anuluj”, które dotyczą nazwy i koloru.

Na Androidzie 13 i nowszym włączenie powiadomień przy braku zgody prosi o zgodę systemową. Na Androidzie 12 taka zgoda nie istnieje, więc aplikacja otwiera systemowe ustawienia powiadomień aplikacji, a po powrocie odświeża stan „Zablokowane przez system”.

Ekran „Powiadomienia” dzieli ustawienia na sekcje w neutralnych kontenerach, rozdzielone odstępem 16 dp: „Kolizje w planie” z głównym przełącznikiem, „Dzień wcześniej” z przełącznikiem powiadomienia wieczornego i godziną oraz „Przed zajęciami” z przełącznikiem i wyprzedzeniem. Sekcje obu rodzajów są widoczne tylko przy włączonej funkcji, a godzina albo wyprzedzenie tylko przy włączonym danym rodzaju. Każde włączenie to wiersz z przełącznikiem. Przy włączonej funkcji pod sekcjami tekst pomocniczy informuje, że Android może opóźnić powiadomienie o kilkanaście minut.

Ekrany podrzędne ustawień rozdzielają komunikaty, pola, wiersze i przyciski odstępem co najmniej 12 dp. Komunikat informacyjny ma 16 dp paddingu poziomego, 12 dp pionowego i tekst co najmniej 12 sp.

Ekran „Dane” zawiera eksport, import oraz opis skutków pełnego zastąpienia danych. Globalny próg długości okienka, domyślnie 30 minut, umieścić w ustawieniach planu. Pod nim stoi pole wyboru „Minimalna przerwa” z opcjami 0, 5, 10 i 15 min, domyślnie 0 min, i opisem „Zajęcia z krótszą lub równą przerwą są kolizją.” (decyzja użytkownika z 2026-10-09, I-80, reguła w `DOMAIN.md`, sekcja „Kolizje”). Oba ustawienia zostają na ekranie głównym ustawień, bez osobnego ekranu. Jeśli przybędzie więcej ustawień planu niezwiązanych z semestrem, wydzielić dla nich osobny ekran zamiast rozbudowywać ekran główny.

Ekran „O aplikacji” zaczyna się nagłówkiem z logo, nazwą „Mój Akademicki Kalendarz”, wierszem „Autor: r3tza”, wierszem z zainstalowaną wersją i krótkim opisem celu aplikacji zgodnym z `README.md`. Dopóki wersja zaczyna się od 0, nagłówek zawiera komunikat „Wersja przed pełnym wydaniem” z uprzedzeniem o możliwych błędach i zachętą do kopii zapasowej. Pod nim są sekcje „Możliwości” (lista funkcji z `README.md`, punktory w kolorze neutralnym), „Dane i prywatność” (dane na telefonie; przy włączonej kopii Google i blokadzie ekranu Android dołącza plan i ustawienia do zaszyfrowanej kopii i przywraca je po ponownej instalacji, I-14; podstawowe funkcje działają bez konta i sieci; sieć służy aktualizacjom z GitHuba, a plan trafia do sieci tylko po włączeniu synchronizacji Google) oraz „Ostatnie zmiany”. Hierarchię budują nagłówki sekcji i odstępy; tylko lista „Ostatnie zmiany” stoi na karcie (I-55). „Ostatnie zmiany” pokazują najwyżej trzy znane wydania od najnowszego, rozdzielone separatorem: nagłówek „Wersja {wersja}”, datę w formie „19 września 2026”, ikonę i słowo „Zainstalowana” przy bieżącej wersji i zmiany jako listę punktowaną. Historia pochodzi z notatek wydań w APK (`STACK.md`, sekcja „Wydania i licencja”); wydanie bez zmian odczuwalnych dla użytkownika pokazuje „Pomniejsze poprawki”. Wersja zainstalowana, której nie ma w historii, nie tworzy pustej pozycji. Przy dopasowaniu wersji aplikacja pomija przyrostek `-debug`. Ekran nie zawiera akcji aktualizacji.

Ekran „Aktualizacja” otwierają wiersz „Aktualizacja do {wersja}” w ustawieniach i akcja „Zobacz” na banerze „Dzisiaj”. Pokazuje dostępną wersję i jej notatki jako listę punktowaną, po jednej zmianie w linii („Brak informacji”, gdy wydanie nie ma notatek), akcję „Pobierz aktualizację”, postęp z akcją „Anuluj”, prośbę o zgodę systemową, akcję „Zainstaluj” i wynik instalacji. Błędy braku miejsca, przerwanego pobierania, uszkodzonego pliku, obcego pakietu, starszej wersji i innego podpisu są krótkie i nie pokazują wyjątków. Po braku miejsca, przerwanym pobieraniu, uszkodzonym pliku, anulowanej i nieudanej instalacji ekran oferuje „Pobierz ponownie”; po obcym pakiecie, starszej wersji i innym podpisie ponowienie nie jest oferowane. Jeśli ekran otwarto bez dostępnej wersji, na przykład po ponownym uruchomieniu procesu, pokazuje „Brak informacji o nowej wersji.” i akcję „Sprawdź teraz”.

Automatyczne sprawdzanie jest domyślnie wyłączone. Przy wyłączonym przełączniku opis brzmi „Nowe wersje nie pojawią się same.”, a przy włączonym „Sprawdzanie raz dziennie.”; informacja o połączeniu z GitHubem należy do polityki prywatności (decyzja użytkownika z 2026-10-08, I-74). Po włączeniu aplikacja sprawdza wersję przy uruchomieniu najwyżej raz na 24 godziny, bez pracy w tle i bez komunikatu o braku sieci. Dostępna wersja pokazuje na „Dzisiaj” neutralny baner z przyciskami tekstowymi „Nie teraz” i „Zobacz” wewnątrz banera, wyrównanymi do prawej; pominięta wersja pozostaje ukryta, a wyższy `versionCode` pokazuje się ponownie.

Ekrany podrzędne mają własne trasy w jednym `NavHost`, przewidywalny systemowy powrót i tytuł w topbarze. Stan ekranu głównego po powrocie nie może się resetować ani automatycznie otwierać innej sekcji.

Eksport i import używają lokalnego pliku JSON z polem `schemaVersion`. Eksport zapisuje wersję 3 formatu. Import przyjmuje wersje 2 i 3, a plik z wyższą wersją odrzuca komunikatem „Plik pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.” (I-83); w pliku w wersji 2 notatki do przeniesionych terminów są przepinane na datę oryginalną tą samą regułą co migracja bazy. Zgodność z wersją 1 nie jest wymagana.

Użytkownik wybiera plik przez systemowy wybór dokumentu. Format zawiera globalne kierunki, semestry, przypisania, kalendarze akademickie, zajęcia z tekstem prowadzącego, korekty, notatki i zmiany wystąpień, a od I-81 także dni wolne.

Import powinien:

- sprawdzić poprawność struktury pliku;
- pokazać podsumowanie danych przed zapisaniem;
- ostrzec, że operacja zastąpi wszystkie lokalne dane;
- po potwierdzeniu wykonać pełne zastąpienie w jednej transakcji;
- przy błędzie pozostawić dotychczasową bazę bez zmian.

## Odbiór na urządzeniu

Odbiór na urządzeniu jest odrębnym kryterium od kompilacji testów Android.

- Uruchomić test migracji Room i sprawdzić otwarcie bazy z wersji 1 oraz zachowanie danych.
- Sprawdzić rozdzielanie i ponowne łączenie kalendarzy, wybór kalendarza korekty oraz plan dwóch kierunków z różnych uczelni.
- Sprawdzić import poprawnego i błędnego pliku, anulowanie oraz rollback po błędzie zapisu.
- Sprawdzić zgodę na powiadomienia, alarm wieczorny i przed zajęciami, restart, zmianę czasu, anulowanie kolizji i kliknięcie powiadomienia.
- Sprawdzić zimny start w motywie jasnym, ciemnym i systemowym, także gdy motyw aplikacji różni się od systemowego: ekran startowy i ekran ładowania mają kolor motywu aplikacji, a „Dzisiaj” nie pokazuje stanu pustego przed danymi.
- Sprawdzić tablet (O-08) w pionie, w poziomie i w podzielonym ekranie: boczny pasek nawigacji od 600 dp, dolny pasek poniżej, treść nie szersza niż 640 dp, „Dzisiaj” w dwóch kolumnach od 840 dp, wybór daty i godziny w całości i po polsku, zachowanie bieżącego ekranu i wpisanych danych po obrocie.
- Sprawdzić nawigację i powrót systemowy, insety, TalkBack, klawiaturę, focus, motyw ciemny, długie treści i szerokość 320 dp. Potwierdzić, że po błędzie formularz zachowuje dane i pokazuje bezpieczny komunikat.
- Sprawdzić widget na launcherze w małym, pośrednim i dużym rozmiarze, w obu motywach, po zmianie danych i po kliknięciu. Zweryfikować jego stany puste, kolizje i długie nazwy.
