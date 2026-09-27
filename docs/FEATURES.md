# Funkcje i zachowanie interfejsu

Ten dokument opisuje docelowe zachowanie ekranów i widgetu. Bieżące braki są w `KNOWN_ISSUES.md`, status zadań w `QUEUE.md`, a najbliższe kroki w `PLAN.md`.

## Nawigacja

Aplikacja działa w orientacji pionowej; obrót telefonu nie zmienia układu (`ARCHITECTURE.md`, „Praktyczne mobile-first”).

### Start aplikacji

Przy uruchomieniu Android 12+ pokazuje systemowy ekran startowy z ikoną aplikacji. Aplikacja przytrzymuje go, dopóki nie odczyta zapisanego motywu, najwyżej 1 s. Potem, do czasu wczytania aktywnego planu, widać ekran ładowania z pełną nazwą „Mój Akademicki Kalendarz” na tle motywu; znika on najpóźniej po 2 s i nie wraca po obrocie ekranu. Ekran „Dzisiaj” nie pokazuje stanu „Brak aktywnego semestru”, zanim baza odpowie. Wybrany w aplikacji motyw jest przekazywany systemowi (`UiModeManager.setApplicationNightMode`), więc ekran startowy i tło okna mają ten sam motyw co aplikacja. Ekran startowy pokazuje logo maku na białym kole ikony w obu motywach z animacją „Rozkwit”: najpierw makówka, potem pięć płatków kolejno wyrasta ze środka z lekkim obrotem (1,6 s). Przy zimnym starcie aplikacja przytrzymuje ekran startowy do końca animacji, licząc od startu procesu; plan wczytuje się w tym czasie w tle (decyzja z 2026-09-27). Start w działającym procesie nie czeka. Ekran ładowania pokazuje to samo logo w tym samym miejscu i rozmiarze (240 dp), a pod nim pełną nazwę; podczas czekania jaśniejsza fala obiega płatki co 1,5 s. Przy wyłączonych animacjach systemu (skala animacji 0) oba ekrany pokazują statyczny kwiat.

### Dolny pasek

Dolny pasek zawiera trzy pozycje:

- **Dzisiaj** - zajęcia z bieżącego dnia;
- **Plan** - tygodniowy plan zajęć;
- **Dodaj** - formularz nowych zajęć.

Ustawienia są dostępne z menu w prawym górnym rogu. Kliknięcie zajęć na ekranie „Dzisiaj” lub „Plan” otwiera ekran szczegółów i edycji.

Jeśli aplikacja nie ma jeszcze semestru, ekran „Dzisiaj” pokazuje stan pusty z przyciskiem „Skonfiguruj plan”. Kreator otwiera się po tej akcji i tworzy pierwszy semestr, kierunek, kalendarz oraz przypisanie. Ustawienia pozwalają później dodawać, wybierać, konfigurować i usuwać semestry.

Gdy istnieją już globalne kierunki, krok „Dodaj kierunek” w kreatorze pozwala wybrać „Nowy kierunek” albo „Wybierz istniejący”. Wybrany istniejący kierunek zachowuje nazwę i kolor, a kreator nie tworzy jego kopii. Po zapisie kroku wybór kierunku jest zablokowany do końca kreatora, bo zmiana zmieniłaby nazwę współdzielonego kierunku albo zostawiła w semestrze drugie przypisanie; nazwę i kolor kierunku utworzonego w kreatorze nadal można poprawić. Kolejne kierunki dodaje się w ustawieniach semestru.

Semestr utworzony w kreatorze staje się aktywny tylko wtedy, gdy dzisiejsza data mieści się w jego kalendarzu albo gdy nie ma aktywnego semestru (`DOMAIN.md`). W przeciwnym razie komunikat po zapisie brzmi „Utworzono semestr i kierunek. Aktywny semestr się nie zmienił.”, a krok „Dodaj zajęcia” pokazuje informację „Semestr zapisany” oraz akcje „Ustaw jako aktywny i dodaj zajęcia”, „Zakończ” (powrót do ustawień) i „Wstecz”. Formularz zajęć zapisuje do aktywnego semestru, więc kreator nie otwiera go dla semestru nieaktywnego.

## Ekran „Dzisiaj”

To domyślny ekran otwierany po uruchomieniu aplikacji.

Powinien pokazywać:

- dzień tygodnia i pełną datę;
- gradientową kartę podsumowania z nagłówkiem „Twój plan na dziś”, a w dniu bez zajęć „Dziś bez zajęć”, oraz trzema równymi kolumnami „Zajęcia”, „Kolizje” i „Okienka”; każda kolumna ma etykietę u góry i liczbę poniżej;
- czerwony kolor liczby kolizji, gdy jest większa od zera, oraz zielony, gdy wynosi zero; pozostałe liczby zachowują neutralny kolor;
- wszystkie aktywne zajęcia w kolejności od najwcześniejszego;
- kolor kierunku;
- oznaczenie wykrytych kolizji;
- stan pusty, gdy danego dnia nie ma zajęć.

Definicja okienka i reguła jego liczenia są w `DOMAIN.md`, sekcja „Okienka”. Użytkownik może zmienić próg globalnie w ustawieniach.

`TodayViewModel` umieszcza liczbę zajęć, unikalnych kolizji i okienek w stanie widoku. Karta zachowuje obecny gradient i nie pokazuje ozdobnej ikony. Nagłówek karty jest pogrubiony, większy od etykiet kolumn (17 sp), w pełni biały i oznaczony semantycznie jako nagłówek. Przy braku zajęć nagłówek brzmi „Dziś bez zajęć” zamiast „Twój plan na dziś”, a karta pokazuje trzy wartości równe zero, bez dodatkowego tekstu pod liczbami. Pionowe separatory między kolumnami są wyraźnie widoczne na gradiencie (biel 50% krycia, 40 dp wysokości). Układ musi zachować czytelność i semantykę przy szerokości 320 dp oraz w motywie jasnym i ciemnym. Etykiety kolumn są w pełni białe, mieszczą się w jednej linii i przy dużej czcionce zmniejszają się do 12 sp; przy skali 2,0 dopuszczalny jest wielokropek, a pełną etykietę czyta czytnik ekranu. Podpis „Od najwcześniejszego” przechodzi pod tytuł listy, gdy nie mieści się obok.

### Struktura karty zajęć

Kartę zajęć zbudować jako czytelną siatkę z dwiema głównymi kolumnami. Lewa kolumna ma stałą szerokość i pokazuje godzinę rozpoczęcia oraz zakończenia. Prawa zawiera dane zajęć. Kolumny rozdziela subtelny pionowy separator. W prawej części użyć cienkich poziomych separatorów między informacjami podstawowymi, statusem kolizji i notatkami. Nie obramowywać każdej komórki osobno.

Kolejność sekcji w prawej kolumnie:

1. Nazwa zajęć.
2. Pill z nazwą kierunku oraz neutralny tekst typu zajęć.
3. Sala, budynek i prowadzący.
4. Status kolizji, jeśli występuje.
5. Notatki, jeśli występują.

Pionowy pasek przy krawędzi karty używa pełnego koloru kierunku. Pill z nazwą kierunku używa jaśniejszego wariantu tego samego koloru i tekstu o sprawdzonym kontraście. Nie kolorować całej karty według kierunku. Typ zajęć pozostaje neutralnym tekstem obok pilla. Kolor kierunku zawsze występuje razem z jego nazwą, więc nie jest jedynym nośnikiem informacji.

Notatkę wspólną oznaczyć niebieskim lub indygo pillem „Notatka do zajęć”. Notatkę pojedynczego terminu oznaczyć fioletowym pillem „Notatka do terminu”; ta sama etykieta obowiązuje w „Planie” dla każdego dnia. Treść wyświetlić obok etykiety albo pod nią, bez zamykania całej długiej treści w pillu. Jeśli istnieją oba rodzaje notatek, pokazać dwa osobne wiersze. Kolizja zachowuje pomarańczowy styl ostrzegawczy. Kierunek, kolizja i oba rodzaje notatek mają osobne role kolorystyczne oraz jawne etykiety tekstowe.

Karta musi zachować wspólne wyrównanie wierszy, poprawne zawijanie długich nazw i notatek, kontrast oraz czytelność przy szerokości 320 dp. Semantyka czytnika ekranu ma przekazywać godziny, nazwę, status („Odwołane”, „Zmienione”, „Jednorazowe”), kierunek, typ, tydzień A/B, metadane, kolizję i zakres każdej notatki w logicznej kolejności i pomijać puste części. Odwołanej karty nie przyciemnia się w całości: stan pokazują przekreślenie nazwy, pill „Odwołane” i pasek w kolorze błędu. Kolumna godzin ma co najmniej 48 dp i rośnie z rozmiarem czcionki, aby godzina nigdy nie była ucięta.

Przykładowy element:

```text
08:00–09:30
Programowanie
Laboratorium, Informatyka
Sala L204, dr Jan Kowalski
```

Po przekroczeniu północy ekran pokazuje plan nowego dnia przy ponownym otwarciu lub wznowieniu aplikacji. Odczyt aktualnej daty nie wymaga ciągłego działania aplikacji w tle.

## Ekran „Plan”

Ekran oferuje dwa równorzędne sposoby przeglądania planu: **Lista** i **Kalendarz**. Ostatnio wybrany sposób może być zapamiętany lokalnie.

### Widok „Lista”

Widok listy na telefonie składa się z:

- wyboru dnia: poniedziałek–niedziela, z dwuliterowymi skrótami („Pn” do „Nd”) jak w nagłówku kalendarza;
- przesuwania między dniami gestem;
- przechodzenia między tygodniami oraz powrotu do bieżącego tygodnia;
- daty i oznaczenia A/B widocznego przy przeglądanym tygodniu;
- akcji „Zmień tydzień A/B”, z wyborem zakresu „Tylko ten tydzień” lub „Od tego tygodnia”; gdy filtr „Wszystkie” obejmuje kilka kalendarzy, wiersz tygodnia nie jest akcją i pokazuje „Wybierz kierunek w filtrach, aby zmienić tydzień.”; nazwa tygodnia stoi nad źródłem korekty, a przy dużej czcionce „Zmień” przechodzi do osobnej linii;
- filtrów: „Wszystkie” oraz poszczególne kierunki;
- listy zajęć posortowanej według godziny;
- oznaczeń wykrytych kolizji.

Plan uwzględnia aktualny semestr, automatyczne lub ręcznie skorygowane oznaczenie A/B, zmiany pojedynczych wystąpień oraz zajęcia jednorazowe. Ręcznie skorygowany tydzień jest wyraźnie oznaczony; użytkownik może usunąć korektę i wrócić do automatycznego wyniku.

### Widok „Kalendarz”

Widok kalendarza pokazuje jeden miesiąc i zawiera:

- przejście do poprzedniego i następnego miesiąca;
- powrót do bieżącej daty;
- oznaczenie zajęć w każdym dniu kropkami w kolorze kierunku, a zmienionych terminów kropką w kolorze błędu; pod siatką legenda z nazwami kierunków i pozycją „Zmieniony termin”, bo kolor nie może być jedynym nośnikiem informacji;
- czytelne oznaczenie dni z odwołanymi, zmienionymi lub jednorazowymi zajęciami;
- wybór dnia i listę jego aktywnych zajęć pod kalendarzem;
- akcję dodania nowych zajęć jednorazowych dla wybranej daty.

Kalendarz pokazuje wynik `ScheduleResolver`, dlatego musi być zgodny z ekranem „Dzisiaj”, listą planu i widgetem. Odwołane zajęcia mogą pozostać widoczne jako przekreślone tylko wtedy, gdy użytkownik włączy opcję „Pokaż odwołane”. Lista odwołanych terminów pochodzi z domeny (`cancelledOccurrences`), stosuje ten sam filtr kierunku co plan i pomija odwołania z dni, w które zajęcia po edycji już się nie odbywają. Licznik zajęć dnia liczy tylko zajęcia, które się odbywają. Domyślnie kalendarz pokazuje plan aktywny.

## Dodawanie i edycja zajęć

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
6. Pokazać kolizję i notatkę jako krótkie etykiety tekstowe o czytelnym kontraście.
7. Oddzielić zajęcia subtelnymi separatorami. Nie umieszczać każdego zajęcia w osobnej karcie.
8. Dopasować tło do systemowego promienia widgetów Androida.

#### Układ responsywny

1. Traktować widget jako kompaktowy, gdy ma mniej niż 240 dp szerokości albo mniej niż 160 dp wysokości.
2. W wariancie kompaktowym pokazać przewijaną listę dzisiejszych zajęć. Pokazać nazwę, czas i salę, pominąć prowadzącego oraz osobną etykietę notatki, ale zachować alert kolizji.
3. Jeśli wariant kompaktowy nie mieści wszystkich zajęć, użyć przewijanej listy zamiast tekstu „Jeszcze {liczba}”.
4. W wariancie rozszerzonym pokazać przewijaną listę dzisiejszych zajęć wraz z salą, prowadzącym i statusem kolizji albo notatki.
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
   - pozycja „Kopia zapasowa i import” prowadząca do osobnego ekranu danych.
5. „O aplikacji”:
   - informacja o wersji jako zwarty wiersz; osobny ekran dodać dopiero wraz z licencjami albo większą liczbą informacji.

Sekcje umieścić w neutralnych kontenerach i rozdzielić odstępem 16 dp. Wiersze tej samej sekcji mogą używać subtelnych separatorów. Każdy wiersz pokazuje nazwę, bieżącą wartość lub krótkie podsumowanie i ikonę przejścia, jeśli otwiera ekran podrzędny. Ikony Material są pomocnicze i nie zastępują tekstu. Nie nadawać wszystkim pozycjom wagi przycisku głównego.

Ekran „Semestry” zawiera listę semestrów, wybór aktywnego, konfigurację, usuwanie i akcję „Dodaj semestr”. Konfiguracja przypisań kierunków, wspólnych lub osobnych kalendarzy i korekt A/B pozostaje częścią przepływu wybranego semestru. Otwarcie konfiguracji nie zmienia aktywnego semestru, a powrót z ekranów „Kierunki”, „Kalendarze” i „Korekty tygodni” zachowuje niezapisane zmiany nazwy i dat. Lista globalnych kierunków i kolorów jest dostępna z sekcji „Plan” ustawień. Usunięcie kierunku z semestru wymaga potwierdzenia w dialogu, który podaje liczbę usuwanych zajęć i informuje, że znikną też ich notatki i zmiany terminów, a kierunek zostanie w innych semestrach. Usunięcie korekty tygodnia i nieużywanego kalendarza też wymaga potwierdzenia w dialogu z akcją „Usuń” w kolorze błędu. W formularzu korekty można wybrać dowolny dzień; aplikacja zapisuje poniedziałek jego tygodnia i pokazuje pod polem „Korekta obejmuje tydzień od poniedziałku {data}.”. „Dodaj semestr” nie jest główną akcją całych ustawień.

Kolor kierunku wybiera się w kreatorze, na ekranie „Kierunki” semestru i w edycji kierunku tym samym komponentem: podgląd paska i pilla z nazwą kierunku, suwak „Odcień” z torem w kolorach całego koła barw, suwak „Jasność” z torem od najciemniejszego do najjaśniejszego wariantu bieżącego odcienia i pole „Kod koloru” (`#RRGGBB`). Suwaki dają tylko kolory z kontrastem paska co najmniej 3:1 na jasnym i ciemnym motywie. Wpisany kod spoza tego zakresu pokazuje „Ten kolor będzie słabo widoczny. Wybierz inny odcień albo jasność.” i nie zmienia koloru. Kolory zapisane wcześniej zostają bez zmian, dopóki użytkownik ich nie zmieni.

Ekran „Kierunki” w ustawieniach pokazuje globalne kierunki: kropkę w kolorze kierunku, nazwę (powtórzone nazwy z numerem) i ikonę przejścia. Wiersz otwiera ekran „Edytuj kierunek” z nazwą, kolorem, „Zapisz kierunek” i „Anuluj”. Zmiana dotyczy wszystkich semestrów, planu i widgetu. Pusta nazwa jest odrzucana. Usuwanie globalnych kierunków jest poza zakresem. Kropka koloru stoi też obok nazwy kierunku w wierszach ekranu „Kierunki” semestru i w opcjach pola „Istniejący kierunek”.

Na Androidzie 13 i nowszym włączenie powiadomień przy braku zgody prosi o zgodę systemową. Na Androidzie 12 taka zgoda nie istnieje, więc aplikacja otwiera systemowe ustawienia powiadomień aplikacji, a po powrocie odświeża stan „Zablokowane przez system”.

Ekran „Powiadomienia” dzieli ustawienia na sekcje w neutralnych kontenerach, rozdzielone odstępem 16 dp: „Kolizje w planie” z głównym przełącznikiem, „Dzień wcześniej” z przełącznikiem powiadomienia wieczornego i godziną oraz „Przed zajęciami” z przełącznikiem i wyprzedzeniem. Sekcje obu rodzajów są widoczne tylko przy włączonej funkcji, a godzina albo wyprzedzenie tylko przy włączonym danym rodzaju. Pod sekcjami komunikat „Czas dostarczenia” informuje, że Android może opóźnić powiadomienie o kilkanaście minut.

Ekrany podrzędne ustawień rozdzielają komunikaty, pola, wiersze i przyciski odstępem co najmniej 12 dp. Komunikat informacyjny ma 16 dp paddingu poziomego, 12 dp pionowego i tekst co najmniej 12 sp.

Ekran „Dane” zawiera eksport, import oraz opis skutków pełnego zastąpienia danych. Globalny próg długości okienka, domyślnie 30 minut, umieścić w ustawieniach planu. Jeśli przybędzie więcej ustawień planu niezwiązanych z semestrem, wydzielić dla nich osobny ekran zamiast rozbudowywać ekran główny.

Ekrany podrzędne mają własne trasy w jednym `NavHost`, przewidywalny systemowy powrót i tytuł w topbarze. Stan ekranu głównego po powrocie nie może się resetować ani automatycznie otwierać innej sekcji.

Eksport i import używają lokalnego pliku JSON z polem `schemaVersion`. Eksport zapisuje wersję 3 formatu. Import przyjmuje wersje 2 i 3; w pliku w wersji 2 notatki do przeniesionych terminów są przepinane na datę oryginalną tą samą regułą co migracja bazy. Zgodność z wersją 1 nie jest wymagana.

Użytkownik wybiera plik przez systemowy wybór dokumentu. Format zawiera globalne kierunki, semestry, przypisania, kalendarze akademickie, zajęcia z tekstem prowadzącego, korekty, notatki i zmiany wystąpień.

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
