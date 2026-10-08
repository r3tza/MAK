# MAK - log projektu

Najnowsze wpisy są u góry. Czytaj kilka ostatnich przy rozpoczynaniu pracy. Trwałe reguły są w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md`, `ARCHITECTURE.md` i `STACK.md`. Archiwum jest zapisem historii, nie źródłem bieżącego statusu.

## 2026-10-08: Jeden styl komunikatów i dialog wyboru wersji (I-69)

- Decyzja użytkownika: wariant B z porównania trzech wersji. Wszystkie role `MakNoteBanner` mają neutralne tło z ramką; ostrzeżenie i błąd różni tylko kolorowa ikona przy pierwszym wierszu. Akcja komunikatu jest przyciskiem tekstowym w nim, główna akcja ekranu stoi pod nim na pełną szerokość (`ARCHITECTURE.md`, sekcja 4). Odrzucony wariant A zmieniał tylko baner synchronizacji i zostawiał dwa style ostrzeżeń.
- Odbiór I-69 wykazał, że dialog wyboru wersji nie liczył kierunków, więc telefon z samymi kierunkami wyglądał na pusty. Decyzja użytkownika: dialog pokazuje dwie karty, „Ten telefon” i „Dysk Google”, z liczbą kierunków, semestrów i zajęć; ekran synchronizacji pokazuje kółko postępu przy karcie konta podczas pracy.
- Decyzja użytkownika: regułę akcji komunikatu wymusza kod (I-71). `MakNoteBanner` przyjmuje tylko akcję główną i akcję zamknięcia i sam rysuje je jako przyciski tekstowe. Hierarchię przycisków w dialogach i kreatorze („Później”, „Anuluj”, „Wstecz” jako przyciski tekstowe) użytkownik przyjął na później jako I-72; formularze zostają bez zmian.
- Decyzja użytkownika: usuwanie globalnych kierunków wchodzi do zakresu jako I-73 (dotąd poza zakresem w `FEATURES.md`). Brak tej funkcji wyszedł przy odbiorze I-69, gdy kierunki z danych przykładowych nie dały się usunąć. Zachowanie dla kierunku przypisanego do semestru czeka na decyzję.
- Decyzja użytkownika: przy konflikcie wersji dialog pokazuje daty i różnice (I-76), a ekran „Wybierz zmiany” pozwala przy każdej różnicy wybrać wersję z telefonu albo z Dysku (I-77). Domyślnie nic nie jest wybrane; zapis wymaga wyboru przy wszystkich różnicach. Zmienia to decyzję z 2026-09-30 tylko w zakresie ręcznego wyboru; automatyczne scalanie, historia wersji i projekt z gałęzi `backup/google-sync-merge-design` pozostają odrzucone. Język bez potocznych uproszczeń. Użytkownik zaakceptował też ustalenia planu (`PLAN.md`, kroki 2 do 4): bez zapisanej bazy wspólnego planu dialog oferuje tylko wybór całej wersji; dwa niezależnie dodane wpisy o tym samym numerze są osobnymi pozycjami, a zachowany wpis z Dysku dostaje nowy numer; wykluczające się wybory blokują zapis z wyjaśnieniem, bez automatycznej poprawki.
- Decyzja użytkownika: usunąć opis przełącznika automatycznego sprawdzania aktualizacji w stanie włączonym (I-74), bo informacje o połączeniu z GitHubem należą do polityki prywatności.

## 2026-10-02: Zasady testów i publikacji oraz poprawki synchronizacji (I-70)

- Fakty: Sześć poprawek zlecił użytkownik; subagenci Luna je zaimplementowali, a rodzic zrecenzował i zaakceptował. Kontrole Task 1/2: `JsonExportCodecTest` 5/5, Room 7/7, `PlanEditingTest` 3/3 i pięć klas edytorów 103/103; Task 3: 48 testów JVM, 10 testów `SyncViewModelTest` i `lintDebug`. Prawdziwe OAuth i Drive nie były testowane; I-69 pozostaje zablokowane do konfiguracji Google Cloud i udziału użytkownika.
- Decyzja użytkownika: testować tylko zmieniony zakres bez pełnego zestawu aplikacji; uruchamiać najwyżej jeden emulator naraz z 2048 MiB RAM; nie wykonywać push, nie otwierać pull requestów ani nie publikować bez wyraźnego polecenia. Zasady zapisano w `AGENTS.md`, `STACK.md` i `WORKFLOW.md`. PLAN I-69 opisuje telefon fizyczny albo dwa osobne stany klienta na AVD uruchamianych sekwencyjnie, po całkowitym zatrzymaniu poprzedniego emulatora; oba korzystają ze wspólnego stanu Drive. Nie zmieniono klasyfikacji HTTP 403 ani pytań o prywatność.
- Decyzja agenta na zlecenie użytkownika: zamknąć I-70 po recenzji oraz zachować I-69 jako osobny odbiór rzeczywistego konta. Wyniki i granice weryfikacji zapisano w `QUEUE.md` i `KNOWN_ISSUES.md`.


Limit: 20 wpisów datowanych. Przy dodaniu kolejnego przenieś najstarszy do `log_archive/<rok>.md` w tym samym commicie. Zachowaj treść i kolejność archiwizowanych wpisów.

## 2026-09-30: Opcjonalna synchronizacja Google jednym plikiem (I-68, I-69)

- Decyzja użytkownika: opcjonalna synchronizacja planu przez konto Google jako rozszerzenie po MVP (`PRODUCT.md`, `STACK.md`, `ARCHITECTURE.md`). Cały plan jest jednym plikiem w formacie eksportu JSON w ukrytym folderze aplikacji na Dysku. Zmiana tylko po jednej stronie jest przyjmowana, a zmiana po obu stronach wymaga wyboru wersji telefonu albo Dysku. Odrzucona wersja trafia do lokalnego archiwum z eksportem. Pobrany plan czeka na zamknięcie otwartego formularza. Szczegóły: `SYNC_PROPOSAL.md`.
- Odrzucone przez użytkownika jako nadmiar dla tej aplikacji: wcześniejszy projekt agenta ze scalaniem pojedynczych wpisów, historią niezmiennych wersji, stabilnymi identyfikatorami w osobnej tabeli, ręcznym dopasowaniem przy pierwszym połączeniu i ochroną każdego formularza przed zmianą w tle. Dodawał około 11 tys. linii; jego kod jest zachowany lokalnie w gałęzi `backup/google-sync-merge-design`.
- Koszt: zmiany z dwóch telefonów wprowadzone między synchronizacjami nie łączą się. Drive nie ma warunkowego zapisu, więc przy niemal równoczesnym wysłaniu wygrywa późniejszy zapis, a druga wersja zostaje w archiwum telefonu.
- Wejście do synchronizacji: wiersz „Synchronizacja Google” w sekcji „Dane” oraz akcja „Pobierz plan z konta Google” w stanie pustym. Propozycja agenta zamiast osobnego kroku kreatora, zaakceptowana przez użytkownika 2026-10-01.
- Decyzja użytkownika o testach: ograniczyć je do przypadków brzegowych i testów wnoszących realną wartość (`STACK.md`, sekcja 5).
- Ograniczenie: użytkownik potwierdził brak projektu Google Cloud. Rzeczywiste logowanie i dwa telefony to I-69.

## 2026-09-29: Mniej gałęzi, pull requestów i commitów

- Decyzja użytkownika: małe zadanie dołącza do najbliższego większego na tej samej gałęzi, także gdy nie jest z nim ściśle powiązane (na przykład I-63 z trybem tabletowym). Plan trafia do gałęzi, która go wykonuje. Commit obejmuje logiczną część zadania razem z dokumentami, których dotyczy; drobnych poprawek dokumentów nie commituje się osobno, a pull request ma zwykle od jednego do pięciu commitów (`AGENTS.md`, sekcja „Git”; `WORKFLOW.md`, „Gałąź i pull request”).
- Powód: od wydania 0.2.2 przybyło około 60 commitów, choć zakres zmieściłby się w 10 do 20; osobne gałęzie i pull requesty na drobne zmiany i same plany zapychają repozytorium i utrudniają przegląd historii.

## 2026-09-29: Wspólna obsługa błędów zapisu (I-64)

- Decyzja użytkownika: 27 akcji zapisu w sześciu ViewModelach korzysta z jednej funkcji `launchUiOperation`, a testy ViewModeli sprawdzają tylko własną logikę akcji (`ARCHITECTURE.md`, sekcja 5). I-14, przegląd testów i I-64 trafiają do jednego pull requesta na gałęzi `task/backup-tests-and-ui-operation`.
- Fakty: próba na `SemesterViewModel.saveSemester` przed resztą przeniesień pokazała, że wszystkie dotychczasowe testy przechodzą bez zmian, a mutacje funkcji i akcji wskazały, które testy pilnują przepływu. Wykryła też błędy planu: test podwójnego zapisu semestru i kierunku chroni przed drugim efektem zamknięcia ekranu, więc został; `finally` z warunkiem sesji trzeba kopiować dosłownie; błąd zapisu z poprzedniej sesji semestru nie miał testu, więc go dodano.
- Wynik: testy JVM 391 przed zmianą i 373 po niej, pełne testy na urządzeniu 112 z 112.
- Odrzucone: przeniesienie blokady podwójnego wywołania i komunikatu sukcesu do wspólnej funkcji, bo zmieniało kolejność komunikatu i efektu nawigacji i ukrywało pomyłki w przypisaniu flag; bazowa klasa ViewModelu zamiast funkcji rozszerzającej.

## 2026-09-29: Testy tylko z realną wartością

- Decyzja użytkownika: każdy test ma wykrywać błąd, który może realnie wystąpić. Kryteria są w `STACK.md`, sekcja 5, „Sposób testowania”.
- Przegląd: testów JVM jest 391 zamiast 422, a testów Compose 112 zamiast 118. Usunięto testy, które sprawdzały logikę atrapy repozytorium, tautologie modeli danych, dokładne wartości krzywych animacji albo powtarzały inny test; przypadki testów częściowo pokrywających się dołączono do testu, który zostaje. Testy ochrony przed podwójnym zapisem, anulowaniem i błędem zapisu zostały, bo każda akcja ma w kodzie osobną ścieżkę; ich uproszczenie wymaga wspólnej funkcji zapisu (I-64).
- Znaleziony błąd: widget i powiadomienie miały własną odmianę liczebników i dla 22 do 24 pisały „22 kolizji” i „22 zajęć”. Używają teraz wspólnej `polishPlural`.

## 2026-09-29: Jawna systemowa kopia zapasowa (I-14)

- Decyzja użytkownika: systemowa kopia zapasowa Androida zostaje włączona i opisana w „Dane i prywatność”. Reguły kopii są listą dozwolonych plików (baza planu i plik ustawień), a kopia w chmurze działa tylko z szyfrowaniem end-to-end (`ARCHITECTURE.md`, sekcja 8, punkt 14).
- Fakty: próba na emulatorze z lokalnym transportem kopii potwierdziła, że Android przywraca dane przy instalacji z pliku APK, a nie tylko ze Sklepu Play; dokumentacja Androida mówi to samo. Szablonowe reguły kopiowały też identyfikatory widgetów i zaplanowanych alarmów, które należą do jednego urządzenia. Porównywalne aplikacje (Tasks.org, Loop Habit Tracker, AnkiDroid, Fossify Calendar, Signal) mają kopię włączoną i różnią się zakresem kopiowanych danych.
- Powód: mało techniczni odbiorcy rzadko robią ręczny eksport JSON, a kopia systemowa chroni plan przy resecie lub reinstalacji bez pracy po ich stronie. Lista dozwolonych plików sprawia, że przyszły stan synchronizacji domyślnie nie trafi do kopii, więc kopia nie koliduje z opcjonalną synchronizacją.
- Odrzucone: wyłączenie kopii (`allowBackup="false"`); przełącznik kopii w ustawieniach aplikacji, bo użytkownik steruje kopią w ustawieniach Androida.

## 2026-09-29: Decyzje po makietach audytu interfejsu (I-63)

- Decyzja użytkownika po przeglądzie makiet w artefakcie „MAK: audyt interfejsu, przed i po”: karta podsumowania „Dzisiaj” dostaje w motywie ciemnym ciemniejszy gradient `#2C3F94` do `#1C2A6A` z krawędzią 1 dp w bieli o kryciu 8% (I-63, `FEATURES.md`, sekcja „Ekran Dzisiaj”). Przyciski ikon w górnym pasku zachowują obramowanie, a przełącznik widoku na „Planie” zachowuje obecny wygląd z cieniem.
- Powód: w motywie ciemnym karta była najjaśniejszym elementem ekranu, a czerwona i zielona liczba miały na niej kontrast 3,3:1 i 3,5:1; nowy wariant daje 5,5:1 i 5,8:1. Usunięcie ramek w pasku było zmianą estetyczną, która tworzyłaby dwa style przycisków ikon, a zmiana przełącznika nie poprawiała kontrastu stanu zaznaczenia.
- Odrzucone: przyciski ikon bez ramek w górnym pasku; przełącznik widoku z jasnym tłem akcentu i znacznikiem wyboru zamiast cienia.

## 2026-09-29: Kolejność akcji w formularzu

- Decyzja użytkownika: w formularzu z akcjami w jednym wierszu „Anuluj” stoi przed akcją zapisu, a akcja zapisu jest na końcu wiersza; gdy akcje stoją jedna pod drugą, akcja zapisu jest na górze, a „Anuluj” pod nią. Reguła jest w `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
- Powód: I-59 ujednolicił kolejność w kodzie, bo formularz korekty tygodnia miał ją odwrotną niż pozostałe formularze. Zapisana reguła zapobiega powrotowi tej niespójności w nowych formularzach. Kolejność w wierszu odpowiada wzorcowi Material 3, w którym akcja potwierdzająca stoi na końcu.

## 2026-09-28: Angielskie nazwy i wspólne pull requesty dla małych zadań

- Decyzja użytkownika: nazwy gałęzi i tytuły pull requestów są po angielsku, a opisy pull requestów mogą pozostać po polsku. Większe zadanie ma własny pull request. Kilka małych, powiązanych zadań należy łączyć w jeden pull request zamiast tworzyć osobny dla każdej drobnej zmiany.
- Powód: spójne nazewnictwo repozytorium i mniej narzutu przy recenzowaniu niewielkich zmian.
- Warunek: wspólny pull request ma jeden czytelny zakres, a każde zawarte zadanie zachowuje własny status, kryteria i odpowiednie testy.

## 2026-09-28: Gałąź i pull request dla każdego zadania

- Decyzja użytkownika (wariant zaproponowany przez agenta): każde zadanie, także zmiana samej dokumentacji, powstaje na gałęzi `task/<ID>-<opis>` od `origin/main` i kończy się pull requestem do `main`. Agent sam wypycha gałąź i otwiera pull request, a scala go wyłącznie użytkownik przez squash. Workflow `checks.yml` uruchamia kontrolę dokumentacji i testy JVM na każdym pull requeście. Gałąź `main` ma ochronę: zmiany tylko przez pull request z zielonym `checks`, bez wyjątku dla administratora, bo agenci działają na poświadczeniach użytkownika.
- Powód: izolacja nieudanych kroków, jedno miejsce recenzji i testy przed scaleniem zamiast dopiero przy tagu wydania.
- Odrzucone: gałąź `dev` z przenoszeniem na `main` przy wydaniu (stan wydania wyznacza tag); bezpośrednie commity małych poprawek dokumentacji na `main` (użytkownik najpierw je dopuścił, potem wybrał pełną ochronę gałęzi).
- Ograniczenie: na komputerze użytkownika nie ma GitHub CLI (`gh`), więc do czasu jego instalacji i logowania agent podaje odnośnik do utworzenia pull requesta, a ochronę gałęzi użytkownik włącza w ustawieniach repozytorium.

## 2026-09-28: Zgłoszenia od ludzi i uporządkowanie AGENTS.md

- Decyzja użytkownika: publiczne repozytorium przyjmuje zgłoszenia błędów i pomysłów, ale nie pull requesty od innych osób. `CONTRIBUTING.md` jest po polsku i opisuje zgłoszenia, pomysły, brak pull requestów, forki na licencji Apache 2.0 oraz prywatność danych w zgłoszeniach. `.github/ISSUE_TEMPLATE/blad.yml` to formularz zgłoszenia błędu z wersją MAK, wersją Androida, krokami, wynikiem i zgodą na brak prywatnych danych. README odsyła do `CONTRIBUTING.md`, a `scripts/check_map.py` sprawdza też jego odnośniki.
- Decyzja użytkownika (wariant zaproponowany przez agenta): `AGENTS.md` zawiera tylko rdzeń zasad. Zasady widgetu i powiadomień przeszły do `ARCHITECTURE.md`, sekcja 7, bo `AGENTS.md` odsyłał do nieistniejących tam sekcji. Zakres i sposób testowania przeszły do `STACK.md`, sekcja 5, a polecenia testów pojedynczej klasy, zrzutów, szerokości 320 dp, motywu ciemnego i skali czcionki do sekcji 6. Preferencje projektowe użytkownika przeszły do `ARCHITECTURE.md`, sekcje 4 i 5, bez powtórzeń z istniejącymi zasadami; sprzeczne z zakazem pilli sformułowanie „preferuj pille” usunięto. Szablon kroku planu, podział refaktoru na etapy i proces istotnej zmiany wyglądu przeszły do `WORKFLOW.md`, sekcja „Planowanie”. Usunięto nieaktualne zdanie o przykładowych testach szablonu.
- Nowe zasady w `AGENTS.md`: wpis w logu podaje, kto podjął decyzję; pytanie użytkownika o zdanie agenta nie jest akceptacją (korekta użytkownika z 2026-09-28).

## 2026-09-28: Rewizja zasad po korektach użytkownika

- Fakty: Podczas planowania poprawek interfejsu nowe ogólne zasady (redukcja środków wizualnych, reguła powtarzania stanu, zakaz osobnego przycisku powrotu, „Kolizja nie jest winą użytkownika”, ograniczona paleta kolorów) kłóciły się z wcześniej przyjętymi elementami i z korektami użytkownika. Agent traktował przyjęte elementy jak naruszenia.
- Decyzja: `AGENTS.md` mówi, że nowa zasada nie unieważnia przyjętej decyzji, a sprzeczność między dokumentami agent zgłasza zamiast ją rozstrzygać. `ARCHITECTURE.md` dopuszcza role głębi, rytmu i rozpoznawalności, wymienia zaakceptowane elementy wyglądu, pozwala powtórzyć stan, który może zniknąć z ekranu albo dotyczy wiersza, zapewnia czytelność przy wyświetlaniu zamiast ograniczania wyboru, wyjaśnia „Wstecz” w kreatorze i rozdziela kolory kolizji: ostrzeżenie przy zajęciach jest pomarańczowe, liczba kolizji w podsumowaniu i nagłówku czerwona. Drugie zajęcia kolizji aplikacja wskazuje tam, gdzie jest miejsce, a widget pokazuje co najmniej zakres. `WORKFLOW.md` pomija zaakceptowane elementy w przeglądzie redukcyjnym i dodaje sprawdzenie brakującej informacji.
- Powód: Korekty użytkownika dotyczyły zarówno nadmiernej redukcji (nagłówek podsumowania, cienie, separator, „Aktywny”, „Anuluj”), jak i brakującej informacji (stan „Jednorazowe”, dni z wieloma zajęciami, nazwa drugich zajęć).

## 2026-09-28: Decyzje po audycie interfejsu i plan poprawek (I-54, I-56 do I-59)

- Fakty: Audyt kodu i przegląd na emulatorze (motyw jasny i ciemny, 320 dp) wykazał: nadmiar elementów nad pierwszymi zajęciami na „Dzisiaj” i w „Planie”, filtr kierunku ukryty w sekcji rozwijanej, akcję „Dodaj jednorazowe” schowaną w zwiniętych opcjach, kolor błędu dla każdej zmiany w kalendarzu, brak nazwy drugich zajęć przy kolizji, stan „Zaplanowane” przy zwykłym terminie, powtórzony tytuł formularza zajęć, banery powtarzające tytuły i niespójne dialogi, kolejność przycisków oraz odmianę liczebników.
- Decyzja użytkownika: nadtytuł „Dzisiaj” i podpis „Od najwcześniejszego” znikają, a nagłówek karty podsumowania zostaje; kolizja w widgecie jest czerwona (tło `errorContainer` i czerwony pasek, jak w obecnym kodzie); „Anuluj” w formularzach zostaje i `ARCHITECTURE.md` odróżnia je od przycisku powrotu; pionowy separator karty zajęć zostaje w specyfikacji i trafia do kodu. Tytuł formularza zajęć i kolory kalendarza użytkownik zostawił do wyboru agenta.
- Decyzja agenta: tytuł „Dodaj zajęcia” albo „Edytuj zajęcia” jest tylko w górnym pasku. W kalendarzu kolor znacznika oznacza wyłącznie kierunek (kontrast co najmniej 3:1 z tłem), a zmieniony lub przeniesiony termin ma pierścień zamiast kropki, bo kolor stanu mógłby się pokryć z kolorem wybranym dla kierunku. Filtr kierunku to jedno pole wyboru. Kolizja pokazuje nazwę drugich zajęć na karcie i w szczegółach, z jednej funkcji domenowej wspólnej z widgetem.
- Plan: `PLAN.md` ma pięć kroków (I-56, I-57, I-58, I-54, I-59) opisanych dla słabszego agenta; listy kontrolne odbioru O-07, I-40 i I-49 przeniesiono do `QUEUE.md`.
- Odrzucone: rozróżnianie stanów w kalendarzu kolorem (czerwony, fioletowy); trzy różne kształty dla odwołania, zmiany i terminu jednorazowego, bo przy znaczniku 8 dp są nieczytelne; usuwanie „Anuluj” z formularzy.
- Uzupełnienie tego samego dnia: użytkownik wybrał ekran „Semestry” z sekcjami „Aktywny semestr” i „Pozostałe semestry” oraz dialogiem wyboru aktywnego semestru, a na „Dzisiaj” przyciski tekstowe wewnątrz banera aktualizacji. Zasady złagodzono: przegląd redukcyjny w `WORKFLOW.md` zgłasza elementy do usunięcia zamiast je usuwać; stan wymagany przez `FEATURES.md` zostaje mimo reguły o stanie; komunikat nie powtarza tylko tytułu górnego paska; reguła filtra dotyczy pojedynczego filtra; wyjątek 11 sp dla nadtytułów usunięto. Plan dopuszcza drobne różnice między opisem a kodem, zamiast liczby pikseli porównuje zrzuty, obejmuje skalę czcionki 2,0 w kalendarzu i buduje identyfikator terminu jedną funkcją domenową.
- Decyzje po przeglądzie makiet „Podgląd poprawek MAK”: nowe „O aplikacji”, „Ustawienia”, „Powiadomienia”, baner aktualizacji, formularz zajęć, „Dzisiaj” i konfiguracja semestru przyjęte; szczegóły terminu pokazują też „Jednorazowe”; znaczniki kalendarza bez dopasowania kontrastu, a dzień z więcej niż trzema zajęciami ma dwa znaczniki i plus; cienie kart i przełącznika zostają; widget pokazuje tylko zakres kolizji, a aplikacja także nazwę drugich zajęć; ostrzeżenie na ekranie „Dane” pod przyciskami; ekran „Semestry” zachowuje pole wyboru i listę z oznaczeniem „Aktywny”, z neutralnym komunikatem, nagłówkiem „Lista semestrów” i „Dodaj semestr” nad listą (dialog wyboru odrzucony); ekran „Kierunki” semestru dostaje „Edytuj” i „Usuń” w kartach, osobny ekran dodawania i ekran edycji z kalendarzem, nazwą i kolorem (I-60). Decyzja agenta na prośbę użytkownika: akcje dodawania stoją nad listami jako obrysowane przyciski z ikoną plusa, bo pełny przycisk pod długą listą znika z widoku.
- Dalsze decyzje użytkownika tego dnia: znaczniki kalendarza 8 dp przy 1 do 3 zajęciach, 6 dp przy 4 i 5 zajęciach, a od 6 zajęć cztery znaczniki i plus (pięć znaczników z plusem nie mieści się w komórce 41 dp przy 320 dp); na ekranie „Edytuj kierunek” nazwa stoi na górze; suwak „Odcień” zmienia tylko odcień, a „Jasność” prowadzi od czarnego do białego (I-61). Sposób zapewnienia czytelności zaproponował agent na pytanie użytkownika, a użytkownik go przyjął: zapis dokładnego koloru i dopasowanie przy wyświetlaniu do 3:1 dla kształtów i 4,5:1 dla nazwy, z podglądem w obu motywach. Z tego powodu znaczniki kalendarza jednak dostają dopasowanie kontrastu, mimo wcześniejszej rezygnacji z podniesionego progu. Cienie karty zajęć i przełącznika widoku zapisano w `ARCHITECTURE.md` jako zaakceptowany wyjątek.
- Korekta użytkownika tego dnia: alert kolizji w widgecie jest pomarańczowy, w kolorach ostrzeżenia aplikacji, a nie czerwony, zgodnie z zasadą „Kolizja nie jest winą użytkownika”. Wcześniejsza decyzja o czerwonym alercie przestaje obowiązywać.
- Odrzucone: pięć znaczników z plusem; ostrzeżenie bez dopasowania koloru; blokada zapisu nieczytelnych kolorów.
- Otwarte: obramowanie przycisków ikon w górnym pasku, trzy style zaznaczenia na „Planie” i jasność karty podsumowania w motywie ciemnym (`KNOWN_ISSUES.md`, pozycja „Czekają na decyzję użytkownika”).

## 2026-09-28: Reguły redukcji interfejsu i audyt (I-54, I-55)

- Fakty: Zasady UI opisywały czytelność, stałe odstępy, dostępność i zakaz statycznych pilli, ale nie mówiły wprost, kiedy zrezygnować z karty, ikony, koloru albo animacji. Audyt kodu wykazał jeden styl `MakNoteBanner` dla neutralnych informacji, ostrzeżeń i błędów oraz osobną kartę dla każdej grupy ekranu „O aplikacji”.
- Decyzja: Każdy element wizualny musi przekazywać informację, budować hierarchię, wskazywać stan, wspierać nawigację albo działanie. Hierarchia najpierw używa typografii, odstępów, wyrównania i kontrastu. Karty grupują rzeczywiste obiekty, ikony wspierają rozpoznanie, status wpływa na decyzję, a animacja przedstawia rzeczywistą zmianę lub oczekiwanie. `WORKFLOW.md` wymaga przeglądu redukcyjnego przed zakończeniem zmiany UI.
- Wynik audytu: I-54 rozdziela role komunikatów, a I-55 ogranicza karty na ekranie „O aplikacji”. Gradient podsumowania, kolory kierunków i animacja podczas rzeczywistego ładowania są zaakceptowanymi elementami funkcjonalnymi. Nie znaleziono tekstów marketingowych, fontu monospace, dekoracyjnych ikon ani ciągłej animacji poza oczekiwaniem na dane. Oględziny na urządzeniu nadal należą do O-05 i O-06.

## 2026-09-28: Wydanie 0.2.2 i działająca aktualizacja (I-49, I-52, O-07)

- Fakty: Tag `v0.2.2` zbudował się poprawnie w GitHub Actions, a użytkownik opublikował wydanie. Użytkownik potwierdził, że aktualizacja z aplikacji do 0.2.2 działa.
- Decyzja: I-52 ma status `gotowe`. O-07 pozostaje otwarty dla niepotwierdzonych przypadków błędów i jawnego potwierdzenia zachowania danych. I-49 pozostaje w toku do sprawdzenia instalacji według README na drugim telefonie i domknięcia O-07.
- Weryfikacja: Przed tagiem przeszły testy JVM, kompilacja testów urządzenia, lint i `assembleDebug`; workflow wydania zakończył się sukcesem. Główny przebieg aktualizacji potwierdził użytkownik 2026-09-28.

## 2026-09-28: Treść notatek w widgetcie (I-53)

- Fakty: Rozszerzony widget pokazywał przy metadanych tylko ikonę, że zajęcia mają notatkę. Nie pokazywał treści ani nie rozróżniał notatki do zajęć od notatki do terminu, mimo wolnego miejsca w dużych rozmiarach.
- Decyzja: W rozszerzonym widgetcie każda notatka ma osobny wiersz z ikoną i treścią, jak na karcie w aplikacji. Notatka do zajęć używa ikony notatki, a notatka do terminu ikony kalendarza. Wariant kompaktowy pomija notatki, aby zachować czas i nazwę zajęć. Użytkownik zlecił wariant 2026-09-28.
- Weryfikacja: Test prezentera sprawdza treść notatki, a test kompozycji Glance oba rodzaje, ikony i treść. Odbiór rozmiarów i motywów na launcherze należy do O-06.

## 2026-09-28: Bez strzałek i pilli (I-53)

- Fakty: Opis zmian w szczegółach terminu używał strzałek („Sala: L205 → C12”), a `WRITING.md` sam opisywał wzór ostrzeżeń strzałkami. Poza kartą zajęć pille zostały przy aktywnym semestrze, w kalendarzach i korektach tygodni oraz w „O aplikacji”.
- Decyzja: `AGENTS.md` i `WRITING.md` zakazują strzałek obok `·` i em dash. `AGENTS.md` i `ARCHITECTURE.md` zakazują pilli i chipów jako statycznych etykiet; stan, kategorię i wersję pokazuje tekst, ikona z tekstem albo kropka koloru. Opis zmian terminu to zwykłe zdania („Sala zmieniona z L205 na C12”). „Aktywny” i „Zainstalowana” mają ikonę i tekst, korekta tygodnia ma tydzień w tytule, powtórzony tag tygodnia w kalendarzu zniknął, a wersja w „O aplikacji” jest tekstem. `MakTag` i nieużywane kolory pilli usunięto. Widget zostaje do osobnej decyzji. Użytkownik zlecił zmianę 2026-09-28.

## 2026-09-28: Karta zajęć bez pilli (I-53)

- Fakty: Karta pokazywała kierunek, stan i etykiety notatek w wypełnionych pillach. Kolor kierunku powtarzał się w pasku i pillu, etykiety notatek wyglądały jak przyciski, a przy skali czcionki 2,0 pille zajmowały pół szerokości karty. Przegląd innych aplikacji: kalendarze oznaczają kategorię paskiem albo kropką przy zwykłym tekście, Todoist używa linii metadanych z ikonami, a Material 3 przeznacza chipy do interakcji.
- Decyzja: Nazwa kierunku jako tekst w odcieniu kierunku z kontrastem 4,5:1, stan terminu jako ikona pod godzinami (odwołane także z przekreśleniem), notatki jako wiersze z ikoną bez etykiety. Opis zmiany terminu jest w szczegółach terminu. Kolizja zostaje pomarańczowym wierszem. Użytkownik wybrał wariant 2026-09-28 w artefakcie z pięcioma wariantami (`FEATURES.md`, `ARCHITECTURE.md`).
- Odrzucone: Wspólny blok notatek, jasne bloki dla każdej notatki, tagi obrysowe i podpisy nad notatkami.

## 2026-09-27: Błąd wstrzykiwania w aktualizatorze 0.2.0 (I-52)

- Fakty: Przy przejściu historii wydań na plik okazało się, że Koin compiler plugin zostawia wartości domyślne parametrów konstruktora zamiast wstrzykiwać bindingi, bez błędu kompilacji. `UpdateViewModel` w wydaniu 0.2.0 ma atrapę pobierania, weryfikacji i instalacji, własny `InstallEventStore` i `Clock.systemUTC()`. Sprawdzanie wersji działa, ale pobranie zawsze kończy się błędem. Testy JVM tego nie wykryły, bo same korzystały z wartości domyślnych, a `KoinGraphTest` sprawdzał tylko, że ViewModel się tworzy.
- Decyzja: Konstruktor `UpdateViewModel` nie ma wartości domyślnych, testy przekazują atrapy jawnie, a `KoinGraphTest` porównuje pola z instancjami z grafu. Zasada braku wartości domyślnych w klasach tworzonych przez Koin jest w `STACK.md` i `ARCHITECTURE.md`. Wydanie 0.2.1 z poprawką instaluje się ręcznie na 0.2.0, a pierwszy odbiór aktualizacji z aplikacji (O-07) obejmuje przejście z 0.2.1 na 0.2.2.
