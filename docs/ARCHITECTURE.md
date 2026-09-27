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

Plan działa w pełni offline. Jedynym połączeniem sieciowym jest sprawdzanie i pobieranie aktualizacji z GitHub Releases (sekcja 7). Użytkownik nie tworzy konta i nie korzysta z backendu.

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

Interfejs ma szybko odpowiadać na pytania: jakie zajęcia są dziś, co wymaga działania i jaki jest stan planu. Preferowane są karty, fakty, odznaki, sekcje i wiersze zamiast długich bloków tekstu.

### Stały język wizualny

Wspólne prymitywy, przewidywalne odstępy, jawny grid, powtarzalne akcje i udokumentowane wyjątki mają pierwszeństwo przed ręcznym dopieszczaniem każdej funkcji osobno.

Duże sekcje ekranu rozdziela odstęp 16 dp, powiązane elementy wewnątrz sekcji 12 dp, a krótką etykietę od jej wartości 8 dp. Przycisk pełnej szerokości ma co najmniej 12 dp wolnego miejsca nad i pod nim. Elementy sterujące nie mogą wizualnie stykać się z sąsiednimi kontenerami. Tekst w interfejsie ma co najmniej 12 sp; 11 sp dopuszczamy wyłącznie dla wersalikowych nadtytułów i dla tekstu widgetu. Granice kontrolek (pola, opcje wyboru, pola wyboru, przyciski ikon i przyciski obrysowane) używają `outline` o kontraście co najmniej 3:1 z tłem; karty, separatory i ramki dekoracyjne używają jaśniejszego `outlineVariant`. W motywie ciemnym pille i etykiety statusu używają przyciemnionego wariantu swojego koloru z jasnym tekstem, a nie jasnego tła z motywu jasnego. Akcja usuwająca w wierszu używa koloru błędu. Pola wyboru używają rozwijanego pola Material 3 z tą samą etykietą przesuwaną nad ramkę co pola tekstowe. Dwa powiązane pola stoją obok siebie tylko wtedy, gdy każde ma co najmniej połowę z 340 dp; na węższym ekranie stoją jedno pod drugim. Ekran podrzędny nie powtarza tytułu z górnego paska ani nie ma osobnego przycisku powrotu; wraca strzałka w pasku i systemowy gest wstecz.

Sekcja rozwijana zachowuje neutralne tło `surfaceContainer` albo `surfaceContainerLow` w obu stanach. Rozwinięcie wskazują tekst, kierunek ikony i semantyka, nie stała zmiana na kolor akcentowy. Nagłówek i treść pozostają jednym kontenerem ze wspólnym kształtem oraz subtelnym obramowaniem. Treść ma 16 dp wewnętrznego paddingu.

### Material 3 jako podstawa konstrukcji

Interfejs budujemy na komponentach i zasadach Material 3. Dopuszczalne są własne kolory, typografia, kształty, karty, nawigacja i układ, jeśli zachowują semantykę oraz przewidywalne zachowanie komponentów Material 3.

Własne komponenty stosujemy tylko wtedy, gdy są potrzebne do odtworzenia zaakceptowanego wzorca. Każdy taki komponent musi zachować etykiety semantyczne, obszar dotyku co najmniej 48 dp (w siatce siedmiu dni, czyli w pasku tygodnia i kalendarzu miesiąca, co najmniej 40 dp szerokości i 48 dp wysokości, bo siedem kolumn po 48 dp nie mieści się przy 320 dp), obsługę focusu i klawiatury, kontrast, motyw jasny i ciemny oraz poprawne działanie na szerokości 320 dp. Pola, listy wyboru, przyciski, pola wyboru, opcje jednokrotnego wyboru, dialogi i nawigację zastępujemy własnym rozwiązaniem tylko po sprawdzeniu tych kryteriów. Własna kontrolka ma jeden cel focusu: `clickable` albo `selectable` bez dodatkowego `focusable()`, a `onFocusChanged` rysujące ramkę stoi w łańcuchu przed nim. Klikalne elementy przyjmują focus tylko w trybie klawiatury, więc testy focusu przełączają `InputModeManager` na `InputMode.Keyboard`.

### Dostępność jako część projektu

Klawiatura, focus, semantyczne etykiety, kontrast, `reduced motion`, małe ekrany i brak obciętych akcji są kryteriami akceptacji. Dostępność należy uwzględniać podczas projektowania każdego widoku.

### Praktyczne mobile-first

Dokumentacja i testy interfejsu muszą obejmować szerokości 320–390 px, obsługę dotyku, długie nazwy, arkusze mobilne i brak poziomego przewijania.

Telefon działa tylko w pionie, a tablet w obu orientacjach (decyzje z 2026-09-27: blokada pionu I-43, tryb tabletowy w wariancie A I-44 do I-47). Manifest blokuje pion, a `MainActivity` zdejmuje blokadę, gdy najkrótszy bok ekranu ma co najmniej 600 dp; Android 16 i tak ignoruje blokadę na takich ekranach przy `targetSdk` 36. Układ zależy od szerokości okna, nie od typu urządzenia: poniżej 600 dp układ telefonu z dolnym paskiem, od 600 dp boczny pasek nawigacji, od 840 dp „Dzisiaj” w dwóch kolumnach. Treść ekranów ma najwyżej 640 dp szerokości (na „Dzisiaj” w dwóch kolumnach 1040 dp) i jest wyśrodkowana. Dwa panele (lista i szczegóły obok siebie) są poza zakresem wariantu A. Aktywność jest odtwarzana przy obrocie, zmianie motywu, czcionki, języka i rozmiaru okna oraz po zakończeniu procesu, więc stan ekranów musi to przetrwać. Do czasu ukończenia I-45 kod blokuje pion na wszystkich urządzeniach z Androidem 12 do 15.

### Uczciwość wobec stanu systemu

Interfejs nie pokazuje akcji, która zakończy się przewidywalnym błędem. Data, oznaczenie tygodnia A/B, źródło ręcznej korekty i kolizje mają być jawne i jednoznaczne.

### Kolizja nie jest winą użytkownika

Kolizja godzin jest informacją o tym, że zajęcia z dwóch kierunków nakładają się w planie. Nie jest błędem użytkownika ani sugestią, że powinien zmienić własne dane. Aplikacja ma ostrzec, wskazać zajęcia i pokazać zakres nakładania, ale nie proponuje zmiany terminu i nie zmienia go automatycznie. Decyzja o kontakcie z uczelnią, opuszczeniu zajęć albo ręcznym przeniesieniu terminu należy do użytkownika.

Na karcie zajęć kolizja używa neutralnego stylu ostrzegawczego i pokazuje dokładny zakres nakładania. Nie używa koloru błędu ani komunikatu sugerującego winę użytkownika.

Gradientowe podsumowanie ekranu „Dzisiaj” pokazuje w trzech równych kolumnach liczbę zajęć, unikalnych kolizji i okienek. Liczba kolizji jest czerwona, gdy jest większa od zera, oraz zielona, gdy wynosi zero. Kolor opisuje stan planu i nie zmienia neutralnego sposobu opisywania kolizji na kartach zajęć.

Karta zajęć używa dwukolumnowej siatki z osobną kolumną godzin oraz sekcjami danych, statusu i notatek. Pełny kolor kierunku występuje na pasku karty, a nazwa kierunku jest tekstem w tym samym odcieniu dopasowanym do kontrastu 4,5:1 z tłem (`courseTextColor`, jedna funkcja dla karty i podglądu koloru). Cała karta pozostaje neutralna i nie używa pilli. Stan terminu (odwołane, zmienione, jednorazowe) to ikona pod godzinami, kolizja używa pomarańczowego wiersza ostrzegawczego, a notatki to wiersze z ikoną: notatka do zajęć i notatka do terminu mają różne ikony. Te same nazwy mają pola notatek w szczegółach terminu i formularzu zajęć. Kolor zawsze występuje razem z etykietą tekstową.

### Gęstość ekranu planu

Ekran „Plan” grupuje zakres dat, nawigację tygodnia, oznaczenie A/B i źródło korekty w jednej sekcji. Akcja zmiany tygodnia A/B znajduje się przy tej informacji, a nie w odłączonym menu, i jest widoczna jako przycisk z ikoną edycji i słowem „Zmień”. Zwinięte filtry pokazują aktywny kierunek. Karty zajęć rozdzielają nazwę, kierunek i typ, metadane, kolizję oraz notatkę na czytelne wiersze. Układ nie może ukrywać pierwszych zajęć przez nadmiernie wysokie elementy sterujące.

### Hierarchia ustawień

Główny ekran ustawień pokazuje wyłącznie sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane”, „Aktualizacje” i „O aplikacji” oraz krótkie podsumowania bieżących wartości. Aktywny semestr, motyw i automatyczne sprawdzanie aktualizacji można zmienić bezpośrednio, a ręczne sprawdzenie uruchamia się z wiersza. Pobieranie i instalacja aktualizacji mają osobny ekran „Aktualizacja”, a ekran „O aplikacji” zawiera tylko opis i ostatnie zmiany. Zarządzanie semestrami, konfiguracja powiadomień oraz kopia zapasowa i import mają osobne ekrany. Rozbudowane formularze i listy nie rozwijają się na ekranie głównym. Topbar jest jedynym nagłówkiem strony, a akcje rzadkie nie otrzymują wagi głównej akcji całych ustawień.

### Konfiguracja początkowa

Jeśli nie ma semestru, aplikacja pokazuje stan pusty z przyciskiem „Skonfiguruj plan”. Kreator otwiera się wyłącznie po jawnej akcji użytkownika. Prowadzi przez utworzenie semestru, pierwszego kierunku, kalendarza akademickiego i ich powiązania, pozwala dodać kolejne kierunki ze wspólnymi albo osobnymi tygodniami A/B, a następnie przejść do dodawania zajęć.

### Spokojny, funkcjonalny styl

Interfejs obsługuje motyw jasny, ciemny i systemowy. Używa lokalnego fontu Inter, nie korzysta z zewnętrznych CDN-ów i stosuje animacje oszczędnie.

Logo to kwiat maku (wariant M, wybrany 2026-09-27): pięć czerwonych płatków `#E5402A` z pofalowanym brzegiem, rozdzielonych linią koloru tła, i ciemna makówka `#1B2236` na białym tle. Ikona adaptacyjna, ikona tematyczna, ekran startowy i ekran ładowania oraz znak obok nazwy w górnym pasku korzystają z tej samej ścieżki płatka w siatce 108 jednostek (`ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `MakPoppyLogo`, `MakPoppyMark`); systemowy ekran startowy ma pustą ikonę (`splash_empty.xml`). W pasku linie między płatkami mają kolor powierzchni paska. Czerwień logo jest kolorem znaku, a nie interfejsu: nie zastępuje akcentu ani koloru błędu. Animacja startu to „Rozkwit” (1,6 s) w jednym ekranie Compose: systemowy ekran startowy pokazuje tylko tło motywu, a ekran ładowania najpierw wyświetla makówkę na białym kole (`bloomSeedHeadScale`, `bloomIconCircleScale`), potem dorysowuje płatki (skala i obrót, `bloomPetalPose`) i rozwija pełną nazwę od środka (`nameRevealFraction`). Animacja zaczyna się po zniknięciu ekranu startowego, gra raz na proces (`StartupBloom`) i zawsze do końca; potem ekran ładowania czeka na dane i znika przez przenikanie. Pętla oczekiwania zmienia tylko przezroczystość płatków. Projekty wariantów są w artefakcie „Logo MAK: warianty maku”.

### Precyzyjny język po polsku

Komunikaty są krótkie i konkretne. Nazwy pojęć pozostają stałe. `MainActivity` używa polskich zasobów niezależnie od języka telefonu (`withAppLocale` w `attachBaseContext`), więc komponenty Material 3, w tym wybór daty i godziny oraz ich opisy dla czytnika ekranu, są po polsku. Powiadomienia są bezosobowe i pozbawione ozdobników. Dwujęzyczność pozostaje poza bieżącym zakresem, dlatego polski jest świadomym priorytetem.

### Szacunek dla pracy użytkownika

Formularze nie kasują wpisanych wartości. Dialogi prawidłowo zwracają focus. Stany puste, błędy i ładowanie korzystają z tego samego modelu widoku co pełne dane.

## 5. Zasada modularności

Projekt dzielimy na małe, wymienne części, ponieważ funkcje i wygląd będą regularnie przebudowywane na podstawie bieżącego feedbacku. Granice między danymi, logiką domenową, ekranami i widgetem mają ograniczać koszt zmiany oraz pozwalać zastąpić jedną część bez przepisywania pozostałych.

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
- **Aktualizacje** (`update`, decyzja z 2026-09-27, zadania I-34 do I-41 i I-49) sprawdzają, pobierają i przekazują do instalacji nowe wydanie z GitHub Releases. To jedyne połączenie sieciowe aplikacji. Aplikacja pobiera plik `update.json` z ostatniego wydania, porównuje jego `versionCode` z zainstalowaną wersją, a po decyzji użytkownika pobiera APK do katalogu cache. Przed instalacją sprawdza sumę SHA-256, nazwę pakietu, wyższy `versionCode` i certyfikat podpisu zgodny z zainstalowaną aplikacją. Instalację wykonuje systemowy `PackageInstaller` z potwierdzeniem użytkownika; przy pierwszej aktualizacji aplikacja wyjaśnia i otwiera systemową zgodę na instalowanie nieznanych aplikacji. Zapytania nie zawierają danych planu ani identyfikatora użytkownika. Ręczne sprawdzenie i przełącznik automatu są w ustawieniach, w sekcji „Aktualizacje”; pobieranie i instalacja odbywają się na osobnym ekranie „Aktualizacja” (decyzja z 2026-09-27, I-48). Automatyczne sprawdzanie jest domyślnie wyłączone; po włączeniu działa tylko przy uruchomieniu aplikacji, najwyżej raz na 24 godziny według wstrzykniętego `Clock`, bez pracy w tle i bez powiadomień. Nowa wersja pokazuje baner na ekranie „Dzisiaj” z akcjami „Zobacz” i „Nie teraz”; „Nie teraz” ukrywa baner do kolejnej wersji. Brak sieci przy sprawdzeniu automatycznym nie pokazuje komunikatu. Logika porównania wersji, walidacji `update.json` i reguły 24 godzin jest czystym Kotlinem testowanym na JVM. Repozytorium jest publiczne od 2026-09-27. Testy JVM używają wstrzykiwanych źródeł i lokalnych artefaktów. Pełny przebieg sieciowy wymaga opublikowanego, nie roboczego wydania (I-49). Aplikacja nie obsługuje tokenu GitHuba.

Logika domenowa nie zależy od Compose ani Glance. `ActivePlanProvider` jest wspólnym punktem obliczania planu i kolizji dla ekranów oraz widgetu.

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

Poza zakresem semestru aplikacja pokazuje jednoznaczny stan wymagający konfiguracji albo informację, że nie ma aktywnego semestru.

## 9. Poza zakresem

Pierwszy zakres nie obejmuje:

- logowania i kont użytkowników;
- backendu, Firebase i synchronizacji w chmurze;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu;
- powiadomienia wymagające stałego działania aplikacji w tle;
- dwujęzycznego interfejsu;
- synchronizacji między urządzeniami;
- skalowania dla wielu użytkowników i obsługi ruchu serwerowego.

## 10. Otwarte pytania

- Systemowa kopia zapasowa Androida (I-14). Manifest ma `allowBackup="true"` z szablonowymi regułami, więc baza i ustawienia mogą trafiać do kopii Google. Do decyzji: wyłączyć kopię, dopuścić ją jawnie albo dodać przełącznik w ustawieniach. Do czasu decyzji nie zmieniamy manifestu ani reguł kopii. Jeśli powstanie synchronizacja opisana niżej, kopia systemowa nadal może chronić użytkowników, którzy się nie zalogują.


### Weryfikacja deweloperów i synchronizacja (propozycje)

Wydania, licencja i aktualizacje w aplikacji zostały zaakceptowane 2026-09-27 (`STACK.md`, sekcja „Wydania i licencja”, oraz punkt „Aktualizacje” w sekcji 7). Poniższe punkty nadal nie są decyzjami. Nie implementować ich przed akceptacją i zapisaniem w `PRODUCT.md`, `STACK.md` i tej sekcji. Kontekst: aplikacja trafia do kilku mało technicznych znajomych, bez Google Play i bez płatnego konta dewelopera.

- **Weryfikacja deweloperów Androida.** Stan na 2026-09-27: obowiązek obowiązuje od 30 września 2026 w Brazylii, Indonezji, Singapurze i Tajlandii, a inne kraje obejmie „w 2027 roku i później”; daty dla Polski i UE nie ogłoszono. Po wejściu obowiązku instalacja i aktualizacja aplikacji niezarejestrowanej działa tylko przez `adb` albo „advanced flow” (opcje programisty, restart i 24 godziny czekania), więc wbudowany aktualizator przestałby działać. Rozwiązanie: darmowe konto „limited distribution” w Android Developer Console, dostępne na świecie od sierpnia 2026 (bez dokumentu tożsamości, weryfikacja dwuetapowa i profil płatności Google, do 20 autoryzowanych urządzeń, autoryzacja linkiem albo kodem QR ze zgodą właściciela urządzenia). Rejestracja nazwy pakietu wymaga odcisku SHA-256 klucza release z I-34. Użytkownik odłożył rejestrację 2026-09-27; wrócić do niej najpóźniej po ogłoszeniu daty dla Polski.
- **Synchronizacja z opcjonalnym logowaniem Google.** Proponowany wariant: cały plan jako jeden plik JSON w ukrytym folderze aplikacji na Dysku (zakres `drive.appdata`, niewrażliwy). Synchronizacja wysyła plik przy zmianach tylko lokalnych, pobiera i atomowo zastępuje dane przy zmianach tylko zdalnych, a przy zmianach po obu stronach pyta, którą wersję zachować, i zapisuje drugą jako plik JSON do ręcznego importu. Scalanie pojedynczych rekordów jest odrzucone w tej propozycji, bo identyfikatory Room są nadawane lokalnie po kolei i kolidowałyby między urządzeniami. Wymagania: projekt Google Cloud z aplikacją OAuth w stanie produkcyjnym (w trybie testowym tokeny wygasają po 7 dniach), klienci OAuth dla certyfikatów debug i release, strona główna i polityka prywatności pod publicznym adresem (do potwierdzenia przy konfiguracji), Usługi Google Play na telefonie. Szacowany zakres: 6 do 8 etapów. Zmienia założenie „bez kont i chmury” w `PRODUCT.md` i odrzuconą alternatywę w `STACK.md`.
- **Kreator i ustawienia.** Propozycja logowania do synchronizacji jest na pierwszym ekranie kreatora, przed tworzeniem semestru, z wyborem „Mam plan na koncie Google” albo „Zacznij od nowa”, aby nowy telefon nie tworzył planu sprzecznego z zapisanym. Krok można pominąć, logowanie jest domyślnie wyłączone i później dostępne w ustawieniach. Przełącznik automatycznego sprawdzania aktualizacji jako ostatni krok kreatora jest opcjonalną częścią I-40.
- **Kolejność.** Synchronizacja jest osobnym projektem po zakończeniu I-34 do I-41 i I-49.

## 11. Problemy do rozwiązania

- Audyt własnych komponentów Compose wykonano 2026-09-27 (wyniki w `KNOWN_ISSUES.md`, zadania I-27 do I-33; I-31 do I-33 rozstrzygnięto tego samego dnia). Pozostaje sprawdzenie z TalkBackiem na urządzeniu w ramach O-05.
