# MAK - log projektu

Najnowsze wpisy są u góry. Czytaj kilka ostatnich przy rozpoczynaniu pracy. Trwałe reguły są w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md`, `ARCHITECTURE.md` i `STACK.md`. Archiwum jest zapisem historii, nie źródłem bieżącego statusu.

Limit: 20 wpisów datowanych. Przy dodaniu kolejnego przenieś najstarszy do `log_archive/<rok>.md` w tym samym commicie. Zachowaj treść i kolejność archiwizowanych wpisów.

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
- Otwarte: obramowanie przycisków ikon w górnym pasku, trzy style zaznaczenia na „Planie” i jasność karty podsumowania w motywie ciemnym (`KNOWN_ISSUES.md`, pozycja 10).

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

## 2026-09-27: Notatki wydań dla użytkowników (I-51)

- Fakty: Opis zmian jednej wersji był w historii w kodzie aplikacji, w `docs/CHANGELOG.md` i w opisie wydania na GitHubie, a `update.json` miał puste notatki. `CHANGELOG.md` zawiera zmiany techniczne, których użytkownik nie odczuwa.
- Decyzja: Jedno źródło notatek dla użytkowników w `app/src/main/assets/release_notes.json`. Czyta je aplikacja („O aplikacji”) oraz workflow (`update.json` i opis wydania). Pusta lista zmian oznacza „Pomniejsze poprawki”; brak wpisu dla wersji z tagu przerywa workflow. `CHANGELOG.md` zostaje techniczną historią dla agentów (`STACK.md`, `AGENTS.md`). Użytkownik zaakceptował wariant 2026-09-27.
- Odrzucone: `CHANGELOG.md` jako źródło (treść techniczna), opis wydania na GitHubie jako źródło (`update.json` powstaje przed jego edycją, historia w aplikacji wymagałaby sieci) oraz generowanie kodu Kotlina z pliku przy budowaniu.

## 2026-09-27: Kolejne kierunki w kreatorze (I-50)

- Fakty: Kreator tworzył tylko jeden kierunek i przechodził do zajęć. Osoba studiująca dwa kierunki musiała sama znaleźć dodawanie kierunków w ustawieniach semestru, a formularz zajęć pozwala wybrać tylko kierunki przypisane do semestru.
- Decyzja: Krok „Dodaj zajęcia” ma akcję „Dodaj kolejny kierunek” z wyborem tygodni A/B („Wspólne z pierwszym kierunkiem” albo „Osobne dla tego kierunku”). Zapis używa istniejących operacji `SemesterRepository`, bez nowej reguły domenowej. Kolejny kierunek dostaje kolor o odcieniu odległym od kierunków już dodanych. Użytkownik wybrał wariant z wyborem A/B 2026-09-27.
- Odrzucone: Samo zdanie z informacją o ustawieniach semestru oraz wariant bez wyboru tygodni A/B.

## 2026-09-27: Wydanie 0.2.0 (I-36, I-49)

- Fakty: Tag `v0.2.0` zbudował szkic w GitHub Actions, a użytkownik go opublikował. `update.json` pod produkcyjnym adresem ma `versionCode` 200, suma SHA-256 zgadza się z APK, a certyfikat jest kluczem wydań. Ręczne sprawdzenie w zainstalowanym `v0.2.0` pokazuje „Masz najnowszą wersję”. Workflow zapisuje w `update.json` puste pole `notes`.
- Decyzja: Przed tagiem dodano do aplikacji historię zmian 0.2.0, bo ekran „O aplikacji” czyta ją z kodu. `CHANGELOG.md` ma wpis 0.2.0. I-36 ma status `gotowe`, a I-49 `w toku` do sprawdzenia instrukcji u znajomego i wydania `v0.2.1`. Uzupełnienie `notes` w workflow jest krokiem przed `v0.2.1`.
- Powód: Pierwsza aktualizacja z aplikacji (O-07) wymaga opublikowanego wydania bazowego, a puste notatki pokazałyby użytkownikowi „Brak informacji”.

## 2026-09-27: Lokalny odbiór aktualizacji N do N+1 (I-39)

- Fakty: Wersja release pobiera `update.json` tylko z `releases/latest` na GitHubie i przyjmuje wyłącznie adresy github.com, więc lokalnego artefaktu N+1 nie da się podać aktualizatorowi bez zmiany kodu. Na emulatorze nie było wcześniejszego wydania z `versionCode` 200.
- Decyzja: I-39 sprawdza lokalnie to, co zagraża danym: podpis tym samym kluczem i zachowanie planu przy instalacji N+1 na N (`adb install -r`). Przebieg przez aplikację przechodzi do O-07 po wydaniu `v0.2.0`. Użytkownik zaakceptował podział i zbudował oba APK lokalnie 2026-09-27; klucz nie opuścił jego komputera.
- Wynik: 201 i 202 mają ten sam certyfikat, plan przetrwał aktualizację, a instalacja starszej wersji na nowszą jest odrzucana. Testowe wydanie odinstalowano z emulatora, aby nie blokowało `v0.2.0`.
- Odrzucone: Tagi testowe na GitHubie (zajmują numery wersji i zostawiają ślady w publicznym repozytorium) oraz testowy adres aktualizacji w kodzie wydania.

## 2026-09-27: Publiczne repozytorium (I-41, I-49)

- Fakty: Użytkownik zmienił widoczność `r3tza/MAK` na publiczną. Strona repozytorium odpowiada bez logowania, a `releases/latest/download/update.json` zwraca 404, bo nie ma opublikowanego wydania. Przegląd historii przed zmianą nie wykazał sekretów ani plików podpisu.
- Decyzja: I-41 obejmuje tylko upublicznienie i ma status `gotowe`. Wydanie `v0.2.0` i `v0.2.1` przeniesiono do nowego zadania I-49, od którego zależy O-07 (`QUEUE.md`, `PLAN.md`). `STACK.md` i `ARCHITECTURE.md` opisują repozytorium jako publiczne.
- Powód: Upublicznienie nie wymaga odbioru aktualizacji na urządzeniu, a wydanie znajomym wymaga zakończonego I-39.
- Uzupełnienie: Użytkownik potwierdził weryfikację dwuetapową konta GitHub 2026-09-27.

## 2026-09-27: Porządki w dokumentacji przed upublicznieniem

- Fakty: `QUEUE.md` mieszał 27 otwartych i 29 zakończonych zadań w jednej tabeli, a `KNOWN_ISSUES.md` powtarzał naprawione problemy z adnotacją „Naprawione”.
- Decyzja: Zakończone zadania są w osobnej tabeli „Zakończone” na końcu `QUEUE.md`; wiersz przechodzi tam po zmianie statusu na `gotowe` (`WORKFLOW.md`). `KNOWN_ISSUES.md` zawiera tylko otwarte problemy i brakujący odbiór. README ma instrukcję instalacji, a `.gitignore` wyklucza pliki kluczy podpisu. Użytkownik zlecił porządki 2026-09-27.
- Powód: Po upublicznieniu repozytorium czytelnik ma od razu widzieć bieżący stan, a historia zostaje w kolejce i logu.
- Odrzucone: Usunięcie zakończonych wierszy z `QUEUE.md`, bo otwarte zadania wskazują je jako zależności.

## 2026-09-27: Aktualizacje w głównych ustawieniach (I-48)

- Fakty: Ekran „O aplikacji” łączył wersję, ręczne sprawdzanie, pobieranie, historię zmian i przełącznik automatu. Na wariancie debug historia pokazywała pustą pozycję „0.1.0-debug” z „Brak informacji” nad wpisem 0.1.0.
- Decyzja: Ustawienia główne dostają sekcję „Aktualizacje” z wierszem „Sprawdź aktualizacje”, warunkowym wierszem „Aktualizacja do {wersja}” i przełącznikiem „Sprawdzaj przy uruchomieniu”. Pobieranie i instalacja mają osobny ekran „Aktualizacja”, do którego prowadzi też „Zobacz” na banerze „Dzisiaj”. „O aplikacji” zawiera nazwę, wersję, krótki opis, autora `r3tza` i najwyżej trzy znane wydania, bez pustej pozycji nieznanej wersji (`FEATURES.md`, `ARCHITECTURE.md`). Użytkownik zaakceptował wariant 2026-09-27.
- Powód: Częste akcje są dostępne bez wchodzenia na ekran opisu, a główne ustawienia nie rozwijają bloków pobierania i błędów.
- Odrzucone: Dialog pobierania otwierany z ustawień, link do kodu źródłowego i sekcja licencji na ekranie „O aplikacji”.
- Weryfikacja: testy JVM, lint i 95 testów urządzenia przechodzą; zrzuty ustawień w obu motywach i „O aplikacji” przy 320 dp na emulatorze. Wariant debug ma osobny pakiet, więc testy urządzenia działają obok wydania o `versionCode` 200.

## 2026-09-27: Implementacja aktualizacji w aplikacji (I-36 do I-40)

- Fakty: Repozytorium i wydania pozostają prywatne, więc produkcyjny adres GitHub zwróci błąd do czasu I-41.
- Zrealizowane: ręczne i automatyczne sprawdzanie, ekran „O aplikacji” z changelogiem trzech wersji, pobieranie i weryfikacja APK, instalacja przez `PackageInstaller`, zgoda systemowa oraz baner na „Dzisiaj”. Automatyczne sprawdzanie jest domyślnie wyłączone i działa najwyżej raz na 24 godziny.
- Weryfikacja: testy JVM i lint przechodzą. Testy urządzenia nie uruchomiły się, ponieważ emulator ma wersję 200 podpisaną kluczem release, a wariant debug ma niższy `versionCode` 1. Odbiór lokalnej aktualizacji N do N+1 i pełny O-07 pozostają otwarte.

## 2026-09-27: Prywatne repozytorium do bramki pierwszego wydania (I-36 do I-41)

- Fakty: Aplikacja ma pobierać `update.json` i APK anonimowo z GitHub Releases. Prywatne repozytorium wymaga uwierzytelnienia, a szkic wydania nie jest dostępny przez `releases/latest`. Użytkownik chce przed upublicznieniem wykonać jeszcze poprawki prezentacyjne.
- Decyzja: Repozytorium pozostaje prywatne podczas I-36 do I-40 i poprawek prezentacyjnych. Granice sieci, pobierania i instalacji są wstrzykiwalne, więc implementacja korzysta z fałszywych źródeł i lokalnych artefaktów. I-41 zaczyna się po jawnym potwierdzeniu gotowości: audyt historii, upublicznienie repozytorium, ręczna publikacja `v0.2.0` i `v0.2.1` oraz pełny odbiór O-07 na telefonie.
- Powód: Logika i interfejs nie wymagają publicznego hostingu podczas tworzenia, a kod oraz wygląd mogą zostać dopracowane przed udostępnieniem repozytorium i aplikacji.
- Odrzucone: Token prywatnego GitHuba w aplikacji; osobne publiczne repozytorium wydań; upublicznienie repozytorium przed poprawkami prezentacyjnymi.

## 2026-09-27: Szkic przed publikacją wydania (I-35)

- Fakty: Workflow wydań uruchamia testy, buduje i podpisuje APK oraz tworzy `update.json` po wypchnięciu taga. Bez dodatkowej bramki udany przebieg publikowałby wydanie od razu.
- Decyzja: Workflow tworzy szkic GitHub Release. Użytkownik sprawdza APK, `update.json`, sumę SHA-256 i opis, a następnie ręcznie publikuje wydanie.
- Powód: Test taga ma sprawdzić pełny proces i pliki bez publicznego udostępniania niedokończonej wersji. Ręczna publikacja pozostawia użytkownikowi ostatnią decyzję.
- Odrzucone: Natychmiastowa publikacja po samym przejściu testów; publiczne wydanie testowe `v0.1.0` przed ukończeniem mechanizmu aktualizacji.
- Weryfikacja: Tag testowy `v0.1.1` przeszedł testy JVM i zbudował podpisany APK w GitHub Actions. Szkic wydania zawierał `MAK-0.1.1.apk` i `update.json`, a zapisana suma SHA-256 była zgodna z pobranym APK.

## 2026-09-27: Polskie zasoby aktywności i kolejność prac (I-44, I-26)

- Fakty: Przy angielskim języku telefonu wybór daty pokazywał „Select date” i angielskie nazwy, a nagłówki dni tygodnia nakładały się na siebie. Teksty pochodziły z zasobów Material 3 w języku systemu.
- Decyzja: `MainActivity.attachBaseContext` nakłada polskie zasoby (`withAppLocale`), zamiast tłumaczyć tytuły pojedynczych komponentów; aplikacja jest tylko po polsku (`ARCHITECTURE.md`). Wybór godziny w oknie niższym niż 560 dp używa układu poziomego. Kolejność prac ustalona przez użytkownika: I-44 i I-26, potem aktualizacje w aplikacji (I-36 do I-41), potem tryb tabletowy (I-45 do I-47).
- Powód: Własne tytuły naprawiłyby tylko widoczne teksty; opisy przycisków dla czytnika ekranu i tryb wpisywania daty nadal byłyby w języku telefonu. Mechanizm aktualizacji musi być w pierwszej wersji dla znajomych.
- Odrzucone: własny tytuł i nagłówek `DatePicker` z polskim `Locale` tylko w stanie wyboru; ustawianie języka aplikacji przez `LocaleManager`, bo działa od Androida 13, a `minSdk` to 31.

## 2026-09-27: Tryb tabletowy, wariant A (I-44 do I-47)

- Fakty: Po blokadzie pionu użytkownik zapytał, jak robią to duże aplikacje, i poprosił o tryb tabletowy. Android 16 ignoruje blokadę orientacji na ekranach od 600 dp, a w poziomie wybór daty i godziny był ucięty.
- Decyzja: Użytkownik wybrał wariant A: telefon w pionie, tablet w obu orientacjach, boczny pasek nawigacji od 600 dp, treść najwyżej 640 dp, „Dzisiaj” w dwóch kolumnach od 840 dp, dialogi wyboru daty i godziny dopasowane do niskiego okna. Klasy szerokości liczy własna funkcja z progami Material 3, bez biblioteki `material3-adaptive`. Zakres zapisano w `PRODUCT.md` i `ARCHITECTURE.md`, kroki w `PLAN.md`.
- Powód: Poprawny układ na tabletach bez przebudowy nawigacji i danych.
- Odrzucone: wariant B z dwoma panelami (lista i szczegóły obok siebie) jako zbyt duża zmiana nawigacji na obecnym etapie; może wrócić jako osobne zadanie; biblioteka `material3-adaptive`, bo potrzebna jest tylko szerokość okna.

## 2026-09-27: Dłuższa animacja startu i znak w pasku (I-42)

- Fakty: Animacja „Rozkwit” trwała około 870 ms i przy szybkim starcie system mógł ją przerwać; użytkownik uznał ją za zbyt krótką. Pierwsza poprawka wydłużyła animację ekranu startowego do 1,6 s, ale po niej na moment pojawiał się ekran ładowania z pełną nazwą, czyli drugi, osobny ekran. W górnym pasku został stary znak z czterech kwadratów.
- Decyzja: Cała animacja (1,6 s) gra w jednym ekranie Compose. Systemowy ekran startowy ma pustą ikonę (`splash_empty.xml`), więc pokazuje tylko tło motywu; ekran ładowania najpierw wyświetla makówkę na białym kole, potem dorysowuje płatki (po 800 ms co 130 ms) i od początku rozwija pełną nazwę od środka. Pusty ekran startowy wybrał użytkownik, bo sama makówka przed animacją wyglądała jak zawieszony obraz. Animacja zaczyna się, gdy ekran startowy znika (bez systemowego wygaszania), gra raz na proces i zawsze do końca; potem ekran ładowania czeka na dane i znika przez przenikanie. Użytkownik wybrał 2026-09-27 czas 1,6 s, odtwarzanie przy każdym zimnym starcie i nazwę rozwijaną od początku animacji, co zmienia odrzucenie sztucznego wydłużania startu z I-30. Górny pasek pokazuje `MakPoppyMark`.
- Powód: Jeden ekran daje ciągły ruch bez mignięcia i pozwala animować nazwę, której ikona ekranu startowego nie może pokazać. Plan wczytuje się w tym czasie w tle.
- Odrzucone: przytrzymanie systemowego ekranu startowego z animowaną ikoną, bo po nim musiał pojawić się drugi ekran z nazwą; 2 s, bo przy codziennym otwieraniu planu może męczyć; pełna animacja tylko raz dziennie, bo wymaga zapisu daty w ustawieniach.
