# MAK: architektura

## 1. Status dokumentu

- Źródło prawdy dla architektury: ten plik.
- Zasady pracy agentów: `../AGENTS.md`.
- Uzasadnienia i odrzucone alternatywy: `LOG.md` i `log_archive/`.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Język identyfikatorów i komentarzy w kodzie: angielski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-27.
- Propozycja z rozmowy staje się decyzją po zaakceptowaniu i zapisaniu w odpowiednim pliku.
- Dokument opisuje zaakceptowaną architekturę i zasady interfejsu. Cel i zakres są w `PRODUCT.md`, reguły planu w `DOMAIN.md`, dokładne zachowanie ekranów w `FEATURES.md`, a bieżące statusy w `QUEUE.md`.

## 2. Cel

MAK (Mój Akademicki Kalendarz) to lekka aplikacja na Androida do lokalnego zarządzania planem zajęć. Obsługuje wiele kierunków, wiele odizolowanych semestrów, naprzemienne tygodnie A/B, zajęcia jednorazowe, kolizje, powiadomienia oraz widget z dzisiejszym planem.

Nazwa produktu to **MAK**. Rozwinięcie „Mój Akademicki Kalendarz” jest wyjaśnieniem skrótu, nie drugą nazwą. W interfejsie, na launcherze i w dokumentacji używamy MAK; pełna nazwa pojawia się na ekranie ładowania.

Plan działa w pełni offline. Sieć służy aktualizacjom GitHub Releases oraz opcjonalnej synchronizacji po połączeniu konta Google. Użytkownik nie tworzy konta MAK; własnego backendu nie ma. Rozszerzenie synchronizacji zlecono 2026-09-30.

## 3. Zakres i terminologia

Główne pojęcia:

- **kierunek**: trwały kierunek studiów oznaczony nazwą i kolorem, który może występować w wielu semestrach;
- **przypisanie kierunku**: połączenie kierunku z semestrem i kalendarzem akademickim;
- **kalendarz akademicki**: wspólny zestaw dat semestru, rytmu A/B i korekt tygodni używany przez jeden lub kilka kierunków;
- **zajęcia**: pojedynczy wpis planu z nazwą, terminem, kierunkiem i opcjonalnymi danymi;
- **semestr**: nazwany kontener aktywnego planu, kierunków i ich kalendarzy akademickich;
- **aktywny semestr**: semestr wybrany w ustawieniach, którego plan pokazują ekrany, widget i powiadomienia;
- **tydzień A/B**: oznaczenie całego tygodnia kalendarzowego, od poniedziałku do niedzieli, według którego wybierane są zajęcia;
- **korekta tygodnia**: ręczne oznaczenie jednego tygodnia albo ustawienie oznaczenia, od którego tygodnie znów naprzemiennie się zmieniają;
- **notatka do zajęć**: notatka wspólna dla wszystkich wystąpień danego wpisu zajęć;
- **notatka do terminu**: notatka przypięta do jednego konkretnego terminu zajęć (w kodzie `OccurrenceNote`); po przeniesieniu terminu przechodzi razem z nim;
- **zmiana wystąpienia**: odwołanie, przeniesienie lub zmiana danych jednego konkretnego terminu zajęć cyklicznych;
- **zajęcia jednorazowe**: dodatkowy wpis obowiązujący tylko w jednej dacie, używany między innymi do odrabiania zajęć;
- **plan aktywny**: zestaw zajęć obowiązujących dla wskazanej daty po zastosowaniu semestru, tygodnia A/B, zmian wystąpień i zajęć jednorazowych;
- **kolizja**: nakładanie się godzin dwóch aktywnych zajęć tego samego dnia, także zajęć należących do różnych kierunków.

Nazwy „kierunek”, „przypisanie kierunku”, „kalendarz akademicki”, „zajęcia”, „semestr”, „aktywny semestr”, „tydzień A/B”, „korekta tygodnia”, „notatka do zajęć”, „notatka do terminu”, „zmiana wystąpienia”, „zajęcia jednorazowe”, „plan aktywny” i „kolizja” mają stałe znaczenie w dokumentacji oraz interfejsie.

Każdy semestr jest osobnym kontenerem planu. Globalne kierunki mogą być przypisane do wielu semestrów. `SemesterProgram` łączy kierunek z semestrem i wskazuje `AcademicCalendar`. Kilka kierunków może współdzielić jeden kalendarz, a kierunek z innej uczelni może używać własnego. Daty, rytm A/B i korekty zawsze są współdzielone razem. Zajęcia, notatki i zmiany wystąpień pozostają odizolowane w ramach przypisania kierunku do semestru.

## 4. Zasady projektowania interfejsu

### Czytelność ponad dekorację

Interfejs ma szybko odpowiadać na pytania: jakie zajęcia są dziś, co wymaga działania i jaki jest stan planu. Preferowane są krótkie fakty, nazwane sekcje, wiersze i karty grupujące rzeczywiste obiekty zamiast długich bloków tekstu.

Każdy widoczny element musi przekazywać informację, budować hierarchię, wskazywać stan, wspierać nawigację, pomagać wykonać zadanie, dawać głębię lub rytm albo budować rozpoznawalność aplikacji. Jeśli dwa warianty są równie czytelne i użyteczne, wybieramy prostszy.

Zaakceptowane elementy wyglądu nie podlegają redukcji bez nowej decyzji użytkownika: gradientowa karta podsumowania „Dzisiaj” z nagłówkiem (w motywie ciemnym z ciemniejszym gradientem, I-63), znak maku i animacja startu, cienie karty zajęć i zaznaczonej opcji przełącznika widoku, pionowy i poziome separatory karty zajęć oraz kolory kierunków. Przegląd redukcyjny ich nie zgłasza.

Hierarchię budujemy najpierw typografią, odstępami, wyrównaniem i kontrastem tekstu. Kontener, obramowanie, kolor lub ikona są kolejnym środkiem, gdy sama struktura nie wystarcza. Karta oznacza rzeczywisty obiekt albo grupę, która potrzebuje wspólnej granicy. Nie zamykamy każdej sekcji w osobnej karcie.

Ikona musi ułatwiać rozpoznanie działania lub informacji. Jeden typ informacji używa tej samej ikony w całej aplikacji. Nie dodajemy ikon do nagłówków, etykiet ani przycisków wyłącznie jako dekoracji.

Ograniczamy przeciążenie poznawcze. Opcje grupujemy w nazwane sekcje, a rozbudowane lub rzadkie przepływy przenosimy na osobne ekrany. Karty, wiersze, krótkie podsumowania i jawny grid stosujemy wtedy, gdy pomagają porównać kilka informacji. Układu tabelarycznego nie dodajemy, jeśli nie poprawia skanowania.

### Stały język wizualny

Wspólne prymitywy, przewidywalne odstępy, jawny grid, powtarzalne akcje i udokumentowane wyjątki mają pierwszeństwo przed ręcznym dopieszczaniem każdej funkcji osobno.

Kolor ma stałe znaczenie. Powierzchnie są głównie neutralne, a akcent służy informacji, kategorii, ostrzeżeniu i działaniu o rzeczywistej wadze. Kolor nie jest jedynym nośnikiem informacji: towarzyszy mu etykieta, ikona, kształt albo treść semantyczna. Akcentu nie używamy wyłącznie do pokazania zwykłego stanu komponentu, jeśli tekst, ikona i semantyka wystarczają.

Duże sekcje ekranu rozdziela odstęp 16 dp, powiązane elementy wewnątrz sekcji 12 dp, a krótką etykietę od jej wartości 8 dp. Przycisk pełnej szerokości ma co najmniej 12 dp wolnego miejsca nad i pod nim. Elementy sterujące nie mogą wizualnie stykać się z sąsiednimi kontenerami. Tekst w interfejsie ma co najmniej 12 sp; 11 sp dopuszczamy wyłącznie dla tekstu widgetu. Granice kontrolek (pola, opcje wyboru, pola wyboru, przyciski ikon i przyciski obrysowane) używają `outline` o kontraście co najmniej 3:1 z tłem; karty, separatory i ramki dekoracyjne używają jaśniejszego `outlineVariant`. Statyczne etykiety stanu, kategorii i wersji nie są pillami: pokazuje je tekst, ikona z tekstem albo kropka koloru, a w motywie ciemnym ich kolor jest rozjaśniony do czytelnego kontrastu. Dotyczy to także widgetu. Akcja usuwająca w wierszu używa koloru błędu. Pola wyboru używają rozwijanego pola Material 3 z tą samą etykietą przesuwaną nad ramkę co pola tekstowe. Dwa powiązane pola stoją obok siebie tylko wtedy, gdy każde ma co najmniej połowę z 340 dp; na węższym ekranie stoją jedno pod drugim. Ekran podrzędny nie powtarza tytułu z górnego paska ani nie ma osobnego przycisku powrotu; wraca strzałka w pasku i systemowy gest wstecz. Formularz może mieć akcję „Anuluj” obok akcji zapisu (decyzja z 2026-09-28): porzuca wpisane zmiany i nie jest przyciskiem powrotu. Gdy akcje stoją w jednym wierszu, „Anuluj” jest pierwsza, a akcja zapisu na końcu wiersza; gdy stoją jedna pod drugą, akcja zapisu jest na górze (decyzja z 2026-09-29). Kreator ma akcję „Wstecz”, bo nie pokazuje strzałki powrotu w górnym pasku. Akcje wyjścia z kroku kreatora („Wstecz”, „Wróć do ustawień”) są przyciskami tekstowymi wyrównanymi do lewej pod pozostałymi akcjami kroku; główna akcja kroku jest wypełniona, a równorzędne wybory obramowane (decyzja użytkownika z 2026-10-08, I-72). Systemowy gest wstecz w kreatorze działa jak widoczna akcja wyjścia: w krokach 2 i 3 oraz w formularzu kolejnego kierunku cofa o krok, w kroku 1 wraca do ustawień, gdy krok pokazuje „Wróć do ustawień”, a w przeciwnym razie zamyka kreator. Podczas zapisu gest nic nie robi. Dialog `MakDialog` sam rysuje swoje akcje (decyzja użytkownika z 2026-10-08, I-72): akcja wyjścia („Anuluj”, „Później”, „Zamknij”) jest zawsze przyciskiem tekstowym. Dialog z formularzem ma ją w wierszu przy prawej krawędzi przed wypełnioną akcją zapisu. Dialog z kilkoma wyborami układa akcje jedna pod drugą: główna wypełniona, wybory obramowane, wyjście na końcu po lewej. Jedyna akcja wyjścia też stoi po lewej. Komunikat nie powtarza tytułu z górnego paska i nie zastępuje nagłówka sekcji. Komunikat ma jedną z trzech ról: neutralną (bez ikony), ostrzegawczą (ikona w kolorze ostrzeżenia) albo błędu (ikona w kolorze błędu). Wszystkie role mają to samo neutralne tło z ramką i ten sam kolor tekstu; rolę oddaje ikona wyrównana do pierwszego wiersza (decyzja użytkownika z 2026-10-08). Akcja dotycząca komunikatu jest w nim przyciskiem tekstowym po prawej, a główna akcja ekranu stoi pod komunikatem jako przycisk pełnej szerokości. Komponent przyjmuje najwyżej akcję główną i akcję zamknięcia i sam rysuje je jako przyciski tekstowe. Kolor akcentu nie jest tłem komunikatu.

Karta zajęć i zaznaczona opcja przełącznika Lista/Kalendarz mają subtelny cień obok obramowania. Cień daje głębię i jest zaakceptowanym wyjątkiem od zasady ograniczania środków wizualnych (decyzja użytkownika z 2026-09-28); przegląd redukcyjny go nie zgłasza.

Sekcja rozwijana zachowuje neutralne tło `surfaceContainer` albo `surfaceContainerLow` w obu stanach. Rozwinięcie wskazują tekst, kierunek ikony i semantyka, nie stała zmiana na kolor akcentowy. Nagłówek i treść pozostają jednym kontenerem ze wspólnym kształtem oraz subtelnym obramowaniem. Treść ma 16 dp wewnętrznego paddingu.

### Material 3 jako podstawa konstrukcji

Interfejs budujemy na komponentach i zasadach Material 3. Dopuszczalne są własne kolory, typografia, kształty, karty, nawigacja i układ, jeśli zachowują semantykę oraz przewidywalne zachowanie komponentów Material 3.

Własne komponenty stosujemy tylko wtedy, gdy są potrzebne do odtworzenia zaakceptowanego wzorca. Każdy taki komponent musi zachować etykiety semantyczne, obszar dotyku co najmniej 48 dp (w siatce siedmiu dni, czyli w pasku tygodnia i kalendarzu miesiąca, co najmniej 40 dp szerokości i 48 dp wysokości, bo siedem kolumn po 48 dp nie mieści się przy 320 dp), obsługę focusu i klawiatury, kontrast, motyw jasny i ciemny oraz poprawne działanie na szerokości 320 dp. Pola, listy wyboru, przyciski, pola wyboru, opcje jednokrotnego wyboru, dialogi i nawigację zastępujemy własnym rozwiązaniem tylko po sprawdzeniu tych kryteriów. Własna kontrolka ma jeden cel focusu: `clickable` albo `selectable` bez dodatkowego `focusable()`, a `onFocusChanged` rysujące ramkę stoi w łańcuchu przed nim. Klikalne elementy przyjmują focus tylko w trybie klawiatury, więc testy focusu przełączają `InputModeManager` na `InputMode.Keyboard`.

### Dostępność jako część projektu

Klawiatura, focus, semantyczne etykiety, kontrast, `reduced motion`, małe ekrany i brak obciętych akcji są kryteriami akceptacji. Dostępność należy uwzględniać podczas projektowania każdego widoku.

Stan pokazujemy tylko wtedy, gdy może przyjąć co najmniej dwie znaczące wartości i znajomość bieżącej wartości pomaga podjąć decyzję albo wykonać działanie. Nie powtarzamy oczywistego lub stałego stanu. Stan wymagany wprost przez `FEATURES.md` albo inną zasadę tego dokumentu zostaje, na przykład źródło oznaczenia tygodnia A/B i zera w podsumowaniu dnia bez zajęć. Powtórzenie stanu jest też dozwolone, gdy jego pierwsze wystąpienie może zniknąć z ekranu, na przykład przy przewijanej liście, albo gdy stan dotyczy konkretnego wiersza, jak „Aktywny” na liście semestrów.

Czytelność zapewniamy przy wyświetlaniu, a nie przez ograniczanie wyboru użytkownika. Przykładem jest kolor kierunku: użytkownik wybiera dowolny kolor, a aplikacja dopasowuje go do tła przy rysowaniu.

Animacja przedstawia przejście, postęp, zmianę stanu, informację zwrotną albo relację przestrzenną. Nie dodajemy stałego ruchu, pulsowania ani ruchu otoczenia wyłącznie po to, aby ekran wyglądał na aktywny. Animacja oczekiwania jest dozwolona tylko podczas rzeczywistego oczekiwania i respektuje systemową skalę animacji.

### Praktyczne mobile-first

Dokumentacja i testy interfejsu muszą obejmować szerokości 320–390 px, obsługę dotyku, długie nazwy, arkusze mobilne i brak poziomego przewijania.

Telefon działa tylko w pionie, a tablet w obu orientacjach (decyzje z 2026-09-27: blokada pionu I-43, tryb tabletowy w wariancie A I-44 do I-47). Manifest blokuje pion, a `MainActivity` zdejmuje blokadę, gdy najkrótszy bok ekranu ma co najmniej 600 dp; Android 16 i tak ignoruje blokadę na takich ekranach przy `targetSdk` 36. Układ zależy od szerokości okna, nie od typu urządzenia: poniżej 600 dp układ telefonu z dolnym paskiem, od 600 dp boczny pasek nawigacji, od 840 dp „Dzisiaj” w dwóch kolumnach. Klasy szerokości liczy `makWidthClassFor` z szerokości okna, a ekrany czytają je z `LocalMakWidthClass`. Treść ekranów ma najwyżej 640 dp szerokości (na „Dzisiaj” w dwóch kolumnach 1040 dp) i jest wyśrodkowana. Dwa panele (lista i szczegóły obok siebie) są poza zakresem wariantu A. Aktywność jest odtwarzana przy obrocie, zmianie motywu, czcionki, języka i rozmiaru okna oraz po zakończeniu procesu, więc stan ekranów musi to przetrwać.

### Uczciwość wobec stanu systemu

Interfejs nie pokazuje akcji, która zakończy się przewidywalnym błędem. Data, oznaczenie tygodnia A/B, źródło ręcznej korekty i kolizje mają być jawne i jednoznaczne.

### Kolizja nie jest winą użytkownika

Kolizja godzin jest informacją o tym, że zajęcia z dwóch kierunków nakładają się w planie. Nie jest błędem użytkownika ani sugestią, że powinien zmienić własne dane. Aplikacja ma ostrzec, pokazać zakres nakładania i wskazać drugie zajęcia wszędzie, gdzie jest na to miejsce; widget pokazuje co najmniej zakres. Nie proponuje zmiany terminu i nie zmienia go automatycznie. Decyzja o kontakcie z uczelnią, opuszczeniu zajęć albo ręcznym przeniesieniu terminu należy do użytkownika.

Na karcie zajęć kolizja używa neutralnego stylu ostrzegawczego i pokazuje dokładny zakres nakładania oraz nazwę drugich zajęć. Szczegóły terminu pokazują tę samą informację. Widget pokazuje tylko zakres, bo ma mało miejsca. Zapis zakresu i nazwy drugich zajęć pochodzą z funkcji domenowych wspólnych dla aplikacji i widgetu. Nie używa komunikatu sugerującego winę użytkownika.

Kolor kolizji zależy od roli (decyzja użytkownika z 2026-09-28). Kolizja pokazana jako ostrzeżenie przy zajęciach, czyli wiersz na karcie, blok w szczegółach terminu i alert w widgecie, używa pomarańczowego koloru ostrzeżenia, nigdy koloru błędu. Liczba kolizji w podsumowaniu i nagłówku, czyli na karcie podsumowania „Dzisiaj” i w nagłówku widgetu, jest czerwona, gdy jest większa od zera, bo opisuje stan całego dnia.

Gradientowe podsumowanie ekranu „Dzisiaj” pokazuje w trzech równych kolumnach liczbę zajęć, unikalnych kolizji i okienek. Liczba kolizji jest czerwona, gdy jest większa od zera, oraz zielona, gdy wynosi zero. Kolor opisuje stan planu i nie zmienia pomarańczowego sposobu opisywania kolizji przy zajęciach.

Karta zajęć używa dwukolumnowej siatki z osobną kolumną godzin oraz sekcjami danych, statusu i notatek. Kolor kierunku występuje na pasku karty, dopasowany do kontrastu co najmniej 3:1 z tłem (`courseShapeColor`), a nazwa kierunku jest tekstem w tym samym odcieniu dopasowanym do kontrastu 4,5:1 (`courseTextColor`). Obie funkcje są jedynym źródłem tej reguły dla karty, kalendarza, list kierunków, widgetu i podglądu palety. Aplikacja zapisuje kolor wybrany przez użytkownika bez zmian i dopasowuje go tylko przy wyświetlaniu (decyzja z 2026-09-28). Cała karta pozostaje neutralna i nie używa pilli. Stan terminu (odwołane, zmienione, jednorazowe) to ikona pod godzinami, kolizja używa pomarańczowego wiersza ostrzegawczego, a notatki to wiersze z ikoną: notatka do zajęć i notatka do terminu mają różne ikony. Te same nazwy mają pola notatek w szczegółach terminu i formularzu zajęć. Kolor zawsze występuje razem z etykietą tekstową.

### Gęstość ekranu planu

Ekran „Plan” grupuje zakres dat, nawigację tygodnia, oznaczenie A/B i źródło korekty w jednej sekcji. Akcja zmiany tygodnia A/B znajduje się przy tej informacji, a nie w odłączonym menu, i jest widoczna jako przycisk z ikoną edycji i słowem „Zmień”. Pojedynczy filtr, obecnie kierunku, to jedno pole wyboru, którego wartością jest aktywny kierunek; nie jest ukryty w dodatkowej sekcji rozwijanej. Ekran nie ma nagłówka treści, bo nazwę pokazuje pasek nawigacji. W kalendarzu kolor znacznika oznacza wyłącznie kierunek i jest kolorem kierunku dopasowanym do kontrastu 3:1 z tłem tą samą funkcją co pasek karty, a stan terminu oddaje kształt: wypełniona kropka to zwykły termin, pierścień to termin zmieniony lub przeniesiony. Kolory stanów nie występują w znacznikach, bo użytkownik może wybrać dla kierunku podobny odcień. Karty zajęć rozdzielają nazwę, kierunek i typ, metadane, kolizję oraz notatkę na czytelne wiersze. Układ nie może ukrywać pierwszych zajęć przez nadmiernie wysokie elementy sterujące.

### Hierarchia ustawień

Główny ekran ustawień pokazuje wyłącznie sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane”, „Aktualizacje” i „O aplikacji” oraz krótkie podsumowania bieżących wartości. Aktywny semestr, motyw i automatyczne sprawdzanie aktualizacji można zmienić bezpośrednio, a ręczne sprawdzenie uruchamia się z wiersza. Pobieranie i instalacja aktualizacji mają osobny ekran „Aktualizacja”, a ekran „O aplikacji” zawiera tylko opis i ostatnie zmiany. Zarządzanie semestrami, konfiguracja powiadomień oraz kopia zapasowa i import mają osobne ekrany. Rozbudowane formularze i listy nie rozwijają się na ekranie głównym. Topbar jest jedynym nagłówkiem strony, a akcje rzadkie nie otrzymują wagi głównej akcji całych ustawień.

### Konfiguracja początkowa

Jeśli nie ma semestru, aplikacja pokazuje stan pusty z przyciskiem „Skonfiguruj plan” i akcją „Pobierz plan z konta Google”. Kreator otwiera się wyłącznie po jawnej akcji użytkownika. Prowadzi przez utworzenie semestru, pierwszego kierunku, kalendarza akademickiego i ich powiązania, pozwala dodać kolejne kierunki ze wspólnymi albo osobnymi tygodniami A/B, a następnie przejść do dodawania zajęć.

### Spokojny, funkcjonalny styl

Interfejs obsługuje motyw jasny, ciemny i systemowy. Używa lokalnego fontu Inter, nie korzysta z zewnętrznych CDN-ów i stosuje animacje oszczędnie.

Logo to kwiat maku (wariant M, wybrany 2026-09-27): pięć czerwonych płatków `#E5402A` z pofalowanym brzegiem, rozdzielonych linią koloru tła, i ciemna makówka `#1B2236` na białym tle. Ikona adaptacyjna, ikona tematyczna, ekran startowy i ekran ładowania oraz znak obok nazwy w górnym pasku korzystają z tej samej ścieżki płatka w siatce 108 jednostek (`ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `MakPoppyLogo`, `MakPoppyMark`); systemowy ekran startowy ma pustą ikonę (`splash_empty.xml`). W pasku linie między płatkami mają kolor powierzchni paska. Czerwień logo jest kolorem znaku, a nie interfejsu: nie zastępuje akcentu ani koloru błędu. Animacja startu to „Rozkwit” (1,6 s) w jednym ekranie Compose: systemowy ekran startowy pokazuje tylko tło motywu, a ekran ładowania najpierw wyświetla makówkę na białym kole (`bloomSeedHeadScale`, `bloomIconCircleScale`), potem dorysowuje płatki (skala i obrót, `bloomPetalPose`) i rozwija pełną nazwę od środka (`nameRevealFraction`). Animacja zaczyna się po zniknięciu ekranu startowego, gra raz na proces (`StartupBloom`) i zawsze do końca; potem ekran ładowania czeka na dane i znika przez przenikanie. Pętla oczekiwania zmienia tylko przezroczystość płatków. Projekty wariantów są w artefakcie „Logo MAK: warianty maku”.

### Precyzyjny język po polsku

Komunikaty są krótkie i konkretne. Nazwy pojęć pozostają stałe. `MainActivity` używa polskich zasobów niezależnie od języka telefonu (`withAppLocale` w `attachBaseContext`), więc komponenty Material 3, w tym wybór daty i godziny oraz ich opisy dla czytnika ekranu, są po polsku. Powiadomienia są bezosobowe i pozbawione ozdobników. Dwujęzyczność pozostaje poza bieżącym zakresem, dlatego polski jest świadomym priorytetem.

### Szacunek dla pracy użytkownika

Formularze nie kasują wpisanych wartości. Dialogi prawidłowo zwracają focus. Nie otwieramy automatycznie kreatora, formularza ani innego przepływu, jeśli stan pusty z jasną akcją daje użytkownikowi większą kontrolę. Stany puste, błędy i ładowanie korzystają z tego samego modelu widoku co pełne dane.

## 5. Zasada modularności

Projekt dzielimy na małe, wymienne części, ponieważ funkcje i wygląd będą regularnie przebudowywane na podstawie bieżącego feedbacku. Dzielimy według odpowiedzialności, stabilnych obszarów danych i przepływów użytkownika, a nie dla każdej tabeli, funkcji czy pliku. Abstrakcję, warstwę albo moduł dodajemy wtedy, gdy tworzy wyraźną granicę, usuwa duplikację reguł albo realnie zmniejsza koszt przyszłych zmian. Wybieramy rozwiązania zgodne ze współczesnymi praktykami platformy, ale na miarę skali projektu: bez prowizorycznych skrótów utrudniających rozwój i bez infrastruktury projektowanej bez konkretnej potrzeby. Preferujemy rozwiązania lokalne, energooszczędne i łatwe w utrzymaniu; usług działających stale w tle nie dodajemy bez potwierdzonej potrzeby. Granice między danymi, logiką domenową, ekranami i widgetem mają ograniczać koszt zmiany oraz pozwalać zastąpić jedną część bez przepisywania pozostałych.

Każda funkcja powinna mieć własną, czytelną odpowiedzialność i komunikować się z innymi częściami przez proste modele lub interfejsy. Logika obliczania planu nie może zależeć od komponentów UI, a widget nie może powielać reguł `ScheduleResolver`.

Wspólny `ActivePlanProvider` składa aktywny plan dla daty z modeli domenowych, wywołuje `ScheduleResolver` i w razie potrzeby `CollisionDetector`. Mapowanie danych Room na modele domenowe znajduje się poza ViewModelem. Provider nie zależy od Compose ani Glance, a domena nie zależy od Room. Ekrany i widget korzystają z tej samej ścieżki obliczeń.

Pakiety w jednym module Gradle: `data`, `domain`, `ui`, `widget`, `export`. ViewModele żyją przy ekranach w `ui`. Nie tworzymy wielu osobnych modułów Gradle bez konkretnej potrzeby, ponieważ zwiększyłyby koszt przebudowy i konfiguracji. Nowy moduł Gradle powstaje dopiero wtedy, gdy ma niezależny cykl zmian, testów albo wyraźną granicę zależności.

Koin 4.2 składa graf zależności na granicy aplikacji po aktualizacji projektu do zgodnego stabilnego zestawu narzędzi. Klasy otrzymują zależności przez konstruktor bez wartości domyślnych dla zależności z grafu; nie pobierają ich z globalnego kontenera. `get()` i `koinInject()` mogą wystąpić wyłącznie w definicjach Koin albo na granicy hosta, który pobiera ViewModel. Compiler plugin sprawdza pełną konfigurację podczas kompilacji.

`NavController` jest jedynym źródłem bieżącej trasy. ViewModel może zgłaszać jednorazowy zamiar nawigacji po zakończeniu operacji, ale nie przechowuje kopii aktualnej trasy. Publiczny stan UI zawiera modele prezentacyjne i potrzebne identyfikatory, a nie encje Room ani relacje bazy.

ViewModele rozdzielamy według przepływów ekranów. Ekrany „Dzisiaj” i „Plan” mają osobne `TodayViewModel` i `ScheduleViewModel`; formularze, szczegóły, semestr, kreator oraz ustawienia zachowują własne ViewModele. ViewModel ekranu składa tylko jego stan i nie deleguje akcji przez nadrzędny ViewModel.

Po wydzieleniu przepływów `MakViewModel` nie pozostaje wspólnym kontenerem stanów ekranów. Jeśli uruchomienie aplikacji nadal wymaga właściciela stanu, zastępuje go mały `AppViewModel`, który rozpoznaje gotowość danych i potrzebę uruchomienia albo wznowienia konfiguracji. `AppViewModel` nie przechowuje kopii trasy, modeli ekranów, motywu, filtrów ani formularzy. Jeśli po usunięciu tych odpowiedzialności nie ma własnego stanu, należy usunąć nadrzędny ViewModel.

Trwałe preferencje zapisujemy poza pamięcią ViewModelu. Motyw zapisujemy w Preferences DataStore jako `ThemeMode` (`System`, `Light`, `Dark`), a nieznaną albo brakującą wartość traktujemy jako `System`. Odtworzenie po zakończeniu procesu przez system obejmuje formularz zajęć: jego szkic jest zapisany w `SavedStateHandle`, a ponowne otwarcie trasy edycji nie wczytuje zajęć z bazy, jeśli szkic dotyczy tych samych zajęć. Pozostałe formularze i dialogi nie odtwarzają szkicu po zakończeniu procesu.

Warstwa danych udostępnia `SemesterRepository` dla semestrów, aktywnego semestru, globalnych kierunków, przypisań i kalendarzy oraz `ScheduleRepository` dla zajęć, wystąpień, notatek i danych planu. `PlanBackupGateway` udostępnia snapshot i atomowe zastąpienie danych, a `PlanBackupService` w `export` łączy je z kodekiem JSON. Prowadzący jest opcjonalnym tekstem zajęć, bez osobnego repozytorium i kartoteki. `SettingsPreferences` pozostaje osobną granicą trwałych ustawień. Nie tworzymy repozytoriów dla każdej tabeli ani dla każdego ekranu. Publiczne kontrakty nie wystawiają encji Room ani relacji bazy.

Operacja obejmująca kilka zależnych zapisów ma jedną granicę transakcji w `data`. Jawny use case albo serwis koordynuje operację wieloetapową, a ViewModel wywołuje ją jako całość. UI otrzymuje jawny stan zapisu i zachowuje wartości formularza po błędzie. Import jest atomowy: zapisuje cały zaakceptowany plik albo nie zmienia bazy.

Akcja zapisu uruchamiana z interfejsu ma w ViewModelu własną blokadę podwójnego wywołania i flagę trwania, a obsługę błędu, anulowania, zmiany sesji i czyszczenia flagi przekazuje do `launchUiOperation` (`ui/feedback/UiOperation.kt`, decyzja z 2026-09-29, I-64). Komunikat sukcesu, efekty nawigacyjne i zmiany stanu po sukcesie akcja wykonuje sama, w swojej kolejności. Test ViewModelu sprawdza własną logikę akcji: walidację, wybór operacji repozytorium, stan i komunikat po sukcesie, stan po błędzie ustawiany przez `onError`, zmianę sesji tam, gdzie akcja jej używa, oraz podwójne wywołanie tylko wtedy, gdy powtórzenie utworzyłoby zduplikowane dane albo drugi efekt nawigacyjny.

## 6. Skala i model użycia

MAK jest aplikacją do użytku własnego, działającą lokalnie na jednym urządzeniu i dla jednego użytkownika. Minimalna wersja Androida to 31 (Android 12). Nie projektujemy jej pod setki użytkowników, współbieżność, multi-tenancy, rozproszony backend, limity API ani skalowanie serwerowe.

Priorytetem są szybkie zmiany, poprawność danych lokalnych, łatwe testowanie i czytelny interfejs. Wydajność optymalizujemy dla planu jednego użytkownika i rozsądnej liczby zajęć w semestrze.

## 7. Kształt systemu

Aplikacja składa się z lokalnej warstwy danych, logiki domenowej, ekranów Compose, powiadomień systemowych i widgetu Glance.

- **Warstwa danych** (`data`) przechowuje globalne kierunki, semestry, przypisania kierunków, kalendarze akademickie, zajęcia, korekty, notatki i zmiany wystąpień w Room nad SQLite. Zajęcia należą do `SemesterProgram`, korekty do `AcademicCalendar`, a prowadzący jest tekstem zajęć. `SemesterRepository` i `ScheduleRepository` są granicami odczytu i zapisu, a DAO nie wychodzą poza `data`. Warstwa udostępnia pełny snapshot kopii zapasowej oraz atomowe zastąpienie danych, ale nie koduje JSON i nie obsługuje `Uri`. Schemat Room ma wersję 3. Migracja z v1 do v2 zachowuje kierunki, zajęcia, prowadzących jako tekst, korekty, notatki i zmiany, a migracja z v2 do v3 nie zmienia tabel i przepina notatki do terminu na datę oryginalną przeniesionego terminu tą samą regułą co import pliku w wersji 2. `academic_calendars` i `week_overrides` niosą `semester_id` ze złożonymi kluczami obcymi, więc kalendarz ani korekta nie mogą należeć do innego semestru. Warstwa danych i ViewModele rozstrzygają już kalendarz dla każdego przypisania: `ScheduleViewModel`, `OccurrenceViewModel` i `ClassEditViewModel` biorą zakres dat z kalendarza właściwego kierunku, a nie z pierwszego kalendarza semestru. Warstwa danych udostępnia rozdzielenie kalendarza przypisania (kopia dat, rytmu i korekt w jednej transakcji), dodanie kierunku z osobnym kalendarzem, ponowne połączenie z innym kalendarzem semestru (bez scalania korekt) oraz usunięcie nieużywanego kalendarza z jego korektami. Osierocony kalendarz po ponownym połączeniu jest usuwany razem z korektami. Ekran semestru pokazuje pola dat tylko wtedy, gdy semestr ma jeden kalendarz; przy wielu pokazuje „Różne kalendarze" i wiersz „Kalendarze". Ekran „Kierunki" pozwala wybrać istniejący globalny kierunek albo utworzyć nowy, przypisać wspólny albo osobny kalendarz, rozdzielić kalendarz kierunku i połączyć go z innym kalendarzem po ostrzeżeniu. Wybrany istniejący kierunek nie pozwala zmienić nazwy ani koloru, bo są współdzielone między semestrami. Ekran „Kalendarze" listuje kalendarze z datami, rytmem i kierunkami oraz pozwala usuwać nieużywane. Korekty tygodni mają wybór kalendarza, gdy jest ich wiele. Semestr bez przypisań pozostaje stanem pustym.
- **Warstwa domenowa** (`domain`) oblicza oznaczenie tygodnia A/B, aktywny plan dla daty, kolizje, liczbę unikalnych kolizji i liczbę okienek. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Wynik resolvera zawiera plan aktywny, oznaczenie A/B i źródło korekty. `CollisionDetector` działa na już złożonym planie, a `uniqueCollisionCount` zwraca liczbę unikalnych par. `countGaps` liczy okienka: przerwę dłuższą niż globalny próg, domyślnie 30 minut, między połączonymi blokami aktywnych zajęć wszystkich kierunków aktywnego semestru, także gdy korzystają z różnych kalendarzy. Nie obejmuje czasu przed pierwszymi ani po ostatnich zajęciach. Walidacja formularza to czysta funkcja wywoływana z ViewModelu.
- **Warstwa interfejsu** (`ui`) udostępnia kreator pierwszej konfiguracji, ekrany „Dzisiaj”, „Plan” w widoku listy lub kalendarza, formularze edycji i ustawienia. Ustawienia pozwalają zarządzać semestrami oraz wskazać aktywny semestr. ViewModel składa stan ekranu z repozytorium, wyniku `ScheduleResolver` i w razie potrzeby `CollisionDetector` albo eksportera. ViewModel nie woła DAO i nie liczy planu sam. Datę do testów wstrzykuje się (`Clock` albo `LocalDate`), nie `LocalDate.now()`. Import kopii JSON uruchamia się z ustawień przez systemowy wybór pliku, a podgląd zawartości i ostrzeżenie o zastąpieniu są osobnym ekranem. ViewModel waliduje snapshot i po potwierdzeniu woła atomowe zastąpienie danych w repozytorium.
- **Widget** (`widget`) nie ma ViewModelu. Czyta dane przez repozytorium, mapuje je na modele domenowe i woła `ActivePlanProvider` z wstrzykniętą datą. Room pozostaje źródłem planu, a widget nie przechowuje jego kopii w preferencjach Glance. Nagłówek oddziela datę od zwartego podsumowania tygodnia, zajęć i kolizji. Wpis używa stałej kolumny czasu, kolorowej osi kierunku, osobnych wierszy kierunku i lokalizacji oraz zwartego ostrzeżenia o kolizji. Zakończone, trwające i następne zajęcia mogą różnić się prezentacją wyłącznie według czasu odczytanego przy odświeżeniu; widget nie obiecuje aktualizacji co minutę.
- **Eksport i import** (`export`) kodują oraz odczytują lokalny plik JSON z wersją schematu. UI wybiera plik przez systemowy wybór dokumentu. `PlanBackupService` albo dedykowany use case łączy kodek ze snapshotem i atomową operacją warstwy danych. Domain nie zna `Uri`.
- **Powiadomienia** korzystają z lokalnych danych aktywnego semestru i ostrzegają o kolizjach wieczorem dnia poprzedniego oraz przed rozpoczęciem najwcześniejszych zajęć w grupie kolizji. Wieczorem wysyłają jedno podsumowanie dnia, a przed zajęciami jeden alert na spójną grupę kolizji. Domyślna godzina wieczorna to 20:00, a domyślne wyprzedzenie to 30 minut. Funkcja jest domyślnie wyłączona. Oba rodzaje można osobno wyłączyć, a ich czas skonfigurować globalnie. Treść może podawać nazwy zajęć. Dostarczenie obsługuje `AlarmManager` przez alarmy przybliżone. Aplikacja prosi o zgodę na powiadomienia dopiero po włączeniu funkcji, nie żąda dostępu do dokładnych alarmów i nie obiecuje dostarczenia o dokładnej godzinie.
- **Aktualizacje** (`update`, decyzja z 2026-09-27, zadania I-34 do I-41 i I-49) sprawdzają, pobierają i przekazują do instalacji nowe wydanie z GitHub Releases. To połączenie nie wysyła danych planu; osobna opcjonalna synchronizacja Google wysyła je dopiero po połączeniu konta. Aplikacja pobiera plik `update.json` z ostatniego wydania, porównuje jego `versionCode` z zainstalowaną wersją, a po decyzji użytkownika pobiera APK do katalogu cache. Przed instalacją sprawdza sumę SHA-256, nazwę pakietu, wyższy `versionCode` i certyfikat podpisu zgodny z zainstalowaną aplikacją. Instalację wykonuje systemowy `PackageInstaller` z potwierdzeniem użytkownika; przy pierwszej aktualizacji aplikacja wyjaśnia i otwiera systemową zgodę na instalowanie nieznanych aplikacji. Zapytania nie zawierają danych planu ani identyfikatora użytkownika. Ręczne sprawdzenie i przełącznik automatu są w ustawieniach, w sekcji „Aktualizacje”; pobieranie i instalacja odbywają się na osobnym ekranie „Aktualizacja” (decyzja z 2026-09-27, I-48). Automatyczne sprawdzanie jest domyślnie wyłączone; po włączeniu działa tylko przy uruchomieniu aplikacji, najwyżej raz na 24 godziny według wstrzykniętego `Clock`, bez pracy w tle i bez powiadomień. Nowa wersja pokazuje baner na ekranie „Dzisiaj” z akcjami „Zobacz” i „Nie teraz”; „Nie teraz” ukrywa baner do kolejnej wersji. Brak sieci przy sprawdzeniu automatycznym nie pokazuje komunikatu. Logika porównania wersji, walidacji `update.json` i reguły 24 godzin jest czystym Kotlinem testowanym na JVM. Repozytorium jest publiczne od 2026-09-27. Testy JVM używają wstrzykiwanych źródeł i lokalnych artefaktów. Pełny przebieg sieciowy wymaga opublikowanego, nie roboczego wydania (I-49). Aplikacja nie obsługuje tokenu GitHuba.
- **Synchronizacja** (`sync`, decyzja z 2026-09-30, I-68) jest opcjonalna i domyślnie wyłączona. `SyncCoordinator` porównuje skrót planu (eksport bez aktywnego semestru) i sumę MD5 pliku na Dysku z wartościami z ostatniej synchronizacji, wysyła albo pobiera cały plan i pyta o wybór wersji, gdy zmieniły się obie strony. Zastosowanie pobranego planu to jedna transakcja `replaceAll`, która sprawdza, że plan lokalny się nie zmienił, i zachowuje aktywny semestr. `PlanEditTracker` przechowuje pod kluczami ekrany z niezapisanym szkicem albo otwartym potwierdzeniem i zwalnia klucz 5 sekund po zamknięciu ekranu; pobrany plan czeka, dopóki jakiś klucz istnieje. Koordynator pracuje na dispatcherze IO. Archiwum 10 wersji przyjmuje tylko plan odrzucony przy wyborze albo pobierany po własnym wysłaniu. Stan, konto i archiwum leżą w `noBackupFilesDir`; token pozostaje w pamięci i jest używany ponownie do odrzucenia przez Dysk. Trwałe błędy zapisuje stan wymagający działania, pokazywany także na „Dzisiaj”. WorkManager uruchamia próbę po zmianie planu, po otwarciu aplikacji, po zamknięciu formularza i co 60 minut.

Logika domenowa nie zależy od Compose ani Glance. `ActivePlanProvider` jest wspólnym punktem obliczania planu i kolizji dla ekranów oraz widgetu.

### Zasady widgetu

Przed zmianą widgetu przeczytaj też sekcję „Widget” w `FEATURES.md`.

- Realizuj etapy po kolei. Każdy etap pozostaw w stanie kompilującym się i sprawdzalnym bez zależności od kolejnego etapu.
- Widget czyta dane przez `SemesterRepository` i `ScheduleRepository`, mapuje je przez wspólną granicę danych i oblicza plan przez `ActivePlanProvider`. Widget nie woła DAO ani `ScheduleResolver` bezpośrednio i nie kopiuje reguł z ViewModelu.
- Wstrzykuj `Clock`. Nie używaj `LocalDate.now()` bezpośrednio w loaderze, prezenterze ani testach widgetu.
- Room jest źródłem planu. Nie przechowuj kopii planu w preferencjach Glance ani wyłącznie w pamięci procesu.
- Odświeżaj wszystkie instancje po udanej zmianie danych na jednej wspólnej granicy. Nie wywołuj aktualizacji z każdego ekranu osobno i nie aktualizuj przed zakończeniem transakcji.
- Nie dodawaj ciągłego serwisu, dokładnych alarmów ani odświeżania co minutę. Okresowa aktualizacja jest zabezpieczeniem i może zostać opóźniona przez system.
- Używaj ograniczonego zestawu progów rozmiaru. Każdy próg ma jawny limit pozycji i stan pusty, bez poziomego przewijania oraz obciętych akcji.
- Kliknięcie widgetu otwiera jawnie ekran „Dzisiaj”. Szczegóły wystąpienia wymagają osobnej decyzji o kontrakcie deep linków.
- Stan błędu jest krótki, bez surowych wyjątków, i pozwala otworzyć aplikację.
- Widget korzysta z kolorów dynamicznych systemu. Kolor o stałym znaczeniu, na przykład pomarańczowy alert kolizji, podawaj wprost przez `ColorProvider(day, night)` z wartościami z `Color.kt`, a nie przez `GlanceTheme.colors`.

### Zasady powiadomień

Przed zmianą powiadomień przeczytaj też odpowiednie decyzje w `LOG.md` albo archiwum logu.

- Planista używa `ActivePlanProvider` i wstrzykniętego `Clock`. Nie licz kolizji ponownie w odbiorniku własną regułą.
- Zachowaj dwa rodzaje powiadomień, grupowanie kolizji, domyślnie wyłączoną funkcję i rozdział preferencji użytkownika od zgody systemowej. Używaj alarmów przybliżonych bez ciągłego serwisu i dostępu do dokładnych alarmów.
- Po zmianach danych i ustawień odnawiaj przyszłe alarmy na wspólnej granicy. Przy dostarczeniu sprawdź bieżący plan, zgodę, przełączniki i czas.
- Test planisty na JVM nie zastępuje odbioru `AlarmManager`, zgody i kliknięcia na urządzeniu.

## 8. Przepływ danych

1. Przy pierwszym uruchomieniu bez semestrów aplikacja pokazuje stan pusty. Jawna akcja otwiera kreator, który atomowo tworzy semestr, pierwszy kierunek, kalendarz i przypisanie. Każdy kolejny kierunek z kreatora jest osobną atomową operacją tej samej granicy co dodanie kierunku w ustawieniach semestru.
2. Użytkownik wybiera aktywny semestr, przypina globalne kierunki oraz wybiera dla nich wspólny albo osobny kalendarz.
3. Repozytorium zapisuje dane lokalnie przez Room.
4. `ScheduleResolver` wywołuje kalkulator osobno dla kalendarza każdego przypisania kierunku i wyznacza A/B z uwzględnieniem jego korekt.
5. Korekta pojedynczego tygodnia działa tylko w tym tygodniu. Korekta „od tego tygodnia” ustawia A lub B dla wskazanego tygodnia i rozpoczyna od niego nową naprzemienną sekwencję. Późniejsza korekta „od tego tygodnia” zastępuje ją od swojej daty. Korekta pojedyncza ma pierwszeństwo w swoim tygodniu.
6. `ScheduleResolver` wybiera zajęcia cykliczne właściwe dla daty i kalendarza kierunku, a następnie łączy wyniki aktywnego semestru.
7. Resolver stosuje zmiany wystąpień: odwołane usuwa z aktywnego planu, zmienione zastępuje danymi dla jednej daty, a przeniesione usuwa z daty źródłowej i dodaje w dacie docelowej.
8. Resolver dodaje zajęcia jednorazowe oraz dołącza notatkę wspólną i notatkę przypiętą do konkretnego wystąpienia.
9. `CollisionDetector` sprawdza nakładanie aktywnych przedziałów czasu po zastosowaniu wszystkich zmian.
10. Lista dnia, kalendarz i widget prezentują wynik tego samego resolvera. Widget może pokazać skrót albo wskaźnik notatki, a pełna treść jest dostępna po otwarciu szczegółów.
11. Zmiana danych wywołuje odświeżenie widgetu bez ciągłego serwisu w tle. Okresowe odświeżenie stanowi zabezpieczenie; platforma może opóźnić aktualizację po północy.
12. Powiadomienia o kolizjach są planowane przybliżonymi alarmami wieczorem dnia poprzedniego i w oknie przed kolidującymi zajęciami. Zmiana danych lub ustawień zastępuje przyszłe alarmy aktualnym zestawem.
13. Eksport zapisuje pełny snapshot. Import pokazuje podgląd i po potwierdzeniu atomowo zastępuje wszystkie lokalne dane.
14. Systemowa kopia zapasowa Androida jest włączona (decyzja z 2026-09-29, I-14). `data_extraction_rules.xml` jest listą dozwolonych plików: baza `mak.db` z plikami `-wal` i `-shm` oraz plik ustawień `datastore/mak_settings.preferences_pb`, osobno dla kopii w chmurze i przenoszenia na nowy telefon. Każdy inny plik zostaje poza kopią, w tym identyfikatory widgetów, identyfikatory zaplanowanych alarmów i przyszły stan synchronizacji, bo należą do jednego urządzenia. Kopia w chmurze działa tylko z szyfrowaniem end-to-end, czyli przy ustawionej blokadzie ekranu (`disableIfNoEncryptionCapabilities`). Android przywraca dane przy każdej instalacji aplikacji, także z pliku APK. Ewentualna synchronizacja trzyma swój stan w `noBackupFilesDir` albo poza listą dozwolonych plików, aby przywrócona kopia nie udawała zsynchronizowanej wersji. Zmiana nazwy bazy albo pliku ustawień wymaga zmiany reguł; pilnuje tego test JVM `BackupRulesTest`.

Poza zakresem semestru aplikacja pokazuje jednoznaczny stan wymagający konfiguracji albo informację, że nie ma aktywnego semestru.

## 9. Poza zakresem

Pierwszy zakres nie obejmuje:

- logowania i kont użytkowników;
- własnego backendu i Firebase;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu;
- powiadomienia wymagające stałego działania aplikacji w tle;
- dwujęzycznego interfejsu;
- synchronizacji poza opcjonalnym kontem Google;
- skalowania dla wielu użytkowników i obsługi ruchu serwerowego.

## 10. Otwarte pytania

### Weryfikacja deweloperów i synchronizacja (propozycje)

Wydania, licencja i aktualizacje w aplikacji zostały zaakceptowane 2026-09-27 (`STACK.md`, sekcja „Wydania i licencja”, oraz punkt „Aktualizacje” w sekcji 7). Weryfikacja deweloperów pozostaje odłożona. Synchronizację zlecono 2026-09-30; projekt i granice wykonania zapisano w `PRODUCT.md`, `STACK.md`, `SYNC.md` i poniżej. Kontekst: aplikacja trafia do kilku mało technicznych znajomych, bez Google Play i bez płatnego konta dewelopera.

- **Weryfikacja deweloperów Androida.** Stan na 2026-09-27: obowiązek obowiązuje od 30 września 2026 w Brazylii, Indonezji, Singapurze i Tajlandii, a inne kraje obejmie „w 2027 roku i później”; daty dla Polski i UE nie ogłoszono. Po wejściu obowiązku instalacja i aktualizacja aplikacji niezarejestrowanej działa tylko przez `adb` albo „advanced flow” (opcje programisty, restart i 24 godziny czekania), więc wbudowany aktualizator przestałby działać. Rozwiązanie: darmowe konto „limited distribution” w Android Developer Console, dostępne na świecie od sierpnia 2026 (bez dokumentu tożsamości, weryfikacja dwuetapowa i profil płatności Google, do 20 autoryzowanych urządzeń, autoryzacja linkiem albo kodem QR ze zgodą właściciela urządzenia). Rejestracja nazwy pakietu wymaga odcisku SHA-256 klucza release z I-34. Użytkownik odłożył rejestrację 2026-09-27; wrócić do niej najpóźniej po ogłoszeniu daty dla Polski.
- **Synchronizacja z opcjonalnym kontem Google.** Decyzja użytkownika z 2026-09-30: cały plan jako jeden plik w formacie eksportu JSON w ukrytym folderze aplikacji na Dysku (`drive.appdata`). Zmiana tylko po jednej stronie jest przyjmowana, a zmiana po obu stronach wymaga wyboru wersji telefonu albo Dysku; odrzucona wersja trafia do lokalnego archiwum z eksportem. Od 2026-10-08 (decyzja użytkownika, I-76, I-77) użytkownik może też przy każdej różnicy ręcznie wybrać stronę: `planDifferences` porównuje wiersze po numerach względem wspólnego planu z ostatniej synchronizacji (`google-sync/base.json`), a `mergePlans` składa wynik i sprawdza odwołania oraz reguły importu. Bez wspólnego planu zostaje tylko wybór całej wersji. Pobrany plan czeka na zamknięcie otwartego formularza. Automatyczne scalanie, historia niezmiennych wersji i stabilne identyfikatory są odrzucone jako nadmiar dla tej aplikacji. Reguły: [SYNC.md](SYNC.md).
- **Wejście do synchronizacji.** Ustawienia, sekcja „Dane”, wiersz „Synchronizacja Google”, oraz stan pusty bez planu: akcja „Pobierz plan z konta Google” pod „Skonfiguruj plan”. Kreator się nie zmienia. Przełącznik automatycznego sprawdzania aktualizacji jako ostatni krok kreatora jest opcjonalną częścią I-40.
- **Kolejność.** Kod i testy lokalne to I-68; rzeczywiste konto Google i dwa telefony to I-69, które czeka na konfigurację projektu Google Cloud przez użytkownika.

## 11. Problemy do rozwiązania

- Audyt własnych komponentów Compose wykonano 2026-09-27 (wyniki w `KNOWN_ISSUES.md`, zadania I-27 do I-33; I-31 do I-33 rozstrzygnięto tego samego dnia). Pozostaje sprawdzenie z TalkBackiem na urządzeniu w ramach O-05.
