# MAK - log projektu

Najnowsze wpisy są u góry. Czytaj kilka ostatnich przy rozpoczynaniu pracy. Trwałe reguły są w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md`, `ARCHITECTURE.md` i `STACK.md`. Archiwum jest zapisem historii, nie źródłem bieżącego statusu.

Limit: 20 wpisów datowanych. Przy dodaniu kolejnego przenieś najstarszy do `log_archive/<rok>.md` w tym samym commicie. Zachowaj treść i kolejność archiwizowanych wpisów.

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

## 2026-09-27: Tylko orientacja pionowa (I-43)

- Fakty: W poziomie na telefonie górny i dolny pasek zajmują około 40% wysokości, „Plan” pokazuje zajęcia dopiero po przewinięciu kontrolek, a wybór godziny i daty jest ucięty.
- Decyzja: Użytkownik zdecydował 2026-09-27, że `MainActivity` działa tylko w pionie, bo z układu poziomego korzystałoby niewiele osób. Ostrzeżenia lint o blokadzie orientacji są wyciszone w manifeście.
- Powód: Poprawienie układu poziomego i dialogów kosztowałoby więcej niż daje.
- Ograniczenia: Na urządzeniach od 600 dp Android 16 ignoruje blokadę przy `targetSdk` 36. Odtwarzanie aktywności przy zmianie motywu, czcionki i języka nadal występuje, więc poprawka z I-26 pozostaje potrzebna.
- Odrzucone: osobny układ poziomy; blokada tylko na części ekranów.

## 2026-09-27: Focus, duża czcionka i odwołane terminy (I-24, I-25, I-27 do I-29)

- Fakty: Audyt interfejsu wykazał dwa przystanki Tab na każdej własnej kontrolce, ucinanie godziny przy skali czcionki 1,3, słowa łamane w środku przy skali 2,0, brak statusu w opisie karty dla TalkBack i odwołane terminy liczone w ViewModelu bez filtra kierunku.
- Decyzja: Własna kontrolka ma jeden cel focusu (`clickable` albo `selectable`), a `onFocusChanged` stoi przed nim; zasada jest w `ARCHITECTURE.md`. Kolumna godzin karty rośnie z czcionką. Etykiety karty podsumowania zmniejszają się do 12 sp, a przy skali 2,0 mogą mieć wielokropek, bo pełną etykietę czyta czytnik ekranu. Wiersz tygodnia przy szerokości poniżej 180 sp przenosi „Zmień” do osobnej linii. Odwołane terminy liczy domena (`cancelledOccurrences`) tą samą regułą terminu co resolver. Wiersz tygodnia nie jest akcją, gdy nie ma jednego kalendarza do korekty.
- Powód: `ARCHITECTURE.md` wymaga klawiatury, 320 dp, braku obciętych informacji i jednego źródła reguł planu.
- Odrzucone: zmniejszanie etykiet poniżej 12 sp; osobny komponent paska dla „Dodaj”; ukrywanie „Zmień” przy dużej czcionce.
- Weryfikacja: 347 testów JVM, 83 testy urządzenia, lint bez nowych ostrzeżeń; zrzuty na emulatorze przy 320 dp i skalach 1,0, 1,3 i 2,0 oraz przejście klawiszem Tab.

## 2026-09-27: Logo maku i animacja startu (I-42)

- Fakty: Aplikacja miała szablonową ikonę Androida, a ekran ładowania z I-30 pokazywał tylko pełną nazwę. Użytkownik chciał logo w kształcie maku, od skrótu nazwy.
- Decyzja: Po trzech rundach wariantów w artefakcie „Logo MAK: warianty maku” użytkownik wybrał 2026-09-27 wariant M (pięć czerwonych płatków z pofalowanym brzegiem, ciemna makówka, białe tło) i animację „Rozkwit”. Ikona adaptacyjna ma białe tło, kwiat na pierwszym planie i osobną warstwę monochromatyczną, w której przerwy między płatkami i wokół makówki daje zmniejszenie płatków i wycięcie. Ekran startowy używa `windowSplashScreenAnimatedIcon` z białym kołem ikony w obu motywach. `MakPoppyLogo` rysuje ten sam kwiat w Compose na ekranie ładowania, w miejscu i rozmiarze ikony ekranu startowego, z pętlą przezroczystości płatków, którą wyłącza skala animacji 0. Szablonowe ikony bitmapowe usunięto, bo przy `minSdk` 31 launcher używa ikony adaptacyjnej.
- Powód: Wzorem były ikony popularnych aplikacji: jeden duży znak bez drobnych detali. Na ciemnym tle makówka zlewała się z tłem, stąd białe koło. Animacja korzysta tylko z obrotu, skali i przezroczystości, więc ma jedno źródło kształtu w XML i w Compose.
- Odrzucone: warianty z boku kwiatu i z łodygą, bo łodyga znika w małym rozmiarze; wydłużanie ekranu startowego do końca animacji, zgodnie z decyzją z I-30; Lottie jako nowa zależność.

## 2026-09-27: Pełna nazwa i ekran ładowania (I-30)

- Fakty: Przy starcie „Dzisiaj” przez chwilę pokazywało „Brak aktywnego semestru”, a przy motywie ciemnym systemowy ekran startowy i pierwsza klatka były jasne.
- Decyzja: Rozwinięcie skrótu zmienia się na „Mój Akademicki Kalendarz”; MAK pozostaje nazwą w interfejsie i na launcherze (decyzja użytkownika z 2026-09-27). Do czasu wczytania planu aplikacja pokazuje ekran ładowania z pełną nazwą, który później zastąpi logo albo animacja logo. Systemowy ekran startowy czeka na odczyt motywu, a wybrany motyw trafia do `UiModeManager.setApplicationNightMode`, więc system rysuje ekran startowy w motywie aplikacji. Limity: 1 s na motyw, 2 s na ekran ładowania.
- Powód: Stan pusty przed odpowiedzią bazy wprowadza w błąd (`ARCHITECTURE.md`, „Uczciwość wobec stanu systemu”), a jasna klatka w motywie ciemnym razi. Minimalny czas wyświetlania nie jest potrzebny.
- Odrzucone: biblioteka `core-splashscreen`, bo minimalne API to 31 i wystarcza `OnPreDrawListener`; sztuczne wydłużanie ekranu ładowania; osobny ekran ładowania na każdej trasie.

## 2026-09-27: Wydania na GitHubie, licencja i aktualizacje w aplikacji (I-34 do I-41)

- Fakty: Aplikacja trafia do kilku mało technicznych znajomych poza Google Play. Bez mechanizmu aktualizacji każda nowa wersja wymaga ręcznego przesłania pliku APK. Przegląd historii gita (186 commitów) nie wykazał kluczy, haseł, plików podpisu ani prywatnych danych; dane demonstracyjne są fikcyjne.
- Decyzja: Plan działa w pełni bez sieci, a aplikacja łączy się wyłącznie z GitHubem w celu sprawdzenia i pobrania aktualizacji, bez wysyłania danych planu (`PRODUCT.md`). Repozytorium `r3tza/MAK` będzie publiczne, na licencji Apache 2.0 z `r3tza` w `NOTICE`; historia i adres e-mail w commitach zostają bez zmian. Wydania budują GitHub Actions z tagu, z APK i `update.json` w GitHub Release (`STACK.md`). Aktualizator sprawdza wersję ręcznie w „O aplikacji” albo automatycznie przy uruchomieniu najwyżej raz na 24 godziny, pokazuje baner na „Dzisiaj”, weryfikuje plik i instaluje go przez `PackageInstaller` (`ARCHITECTURE.md`, punkt „Aktualizacje”). Użytkownik zaakceptował te decyzje 2026-09-27. Synchronizacja przez konto Google pozostaje możliwym przyszłym rozszerzeniem, a rejestracja w Android Developer Console jest odłożona.
- Powód: Apache 2.0 jest licencją reszty stosu, wprost nie daje prawa do nazwy i znaku i nie utrudnia przyszłej integracji z Usługami Google Play. Sprawdzanie przy uruchomieniu nie wymaga pracy w tle, nowej zależności ani zgody na powiadomienia, a aplikacja z planem zajęć jest otwierana często.
- Odrzucone: WorkManager z powiadomieniem, Play In-App Updates, Obtainium jako jedyny sposób aktualizacji, osobne repozytorium na wydania, licencje MIT i GPL 3.0 oraz przepisywanie historii gita.

## 2026-09-27: Konfiguracja semestru bez aktywacji (I-23)

- Fakty: Otwarcie ekranu „Semestr” ustawiało ten semestr jako aktywny, więc konfiguracja semestru dodanego z wyprzedzeniem przełączała „Dzisiaj”, widget i powiadomienia. Powrót z podekranów kasował niezapisany formularz.
- Decyzja: Użytkownik wybrał 2026-09-27 konfigurację bez aktywacji. Aktywny semestr zmienia tylko wybór w ustawieniach albo kreator (`DOMAIN.md`). Trasy semestru wołają `openIfNeeded`, a wejście z listy semestrów `open`.
- Powód: Zgodność z decyzją I-18: dodanie lub edycja przyszłego semestru nie może przełączać bieżącego planu.
- Odrzucone: pytanie o aktywację przy każdym otwarciu nieaktywnego semestru, bo dodaje dialog do rzadkiej czynności bez korzyści dla danych.

## 2026-09-27: Granice kontrolek, nazwy notatek i wyjątki rozmiaru (I-31, I-32, I-33)

- Fakty: Audyt interfejsu z 2026-09-27 wykazał kontrast granic kontrolek 1,22:1 w jasnym i 1,45:1 w ciemnym motywie, dwie pary nazw tych samych notatek oraz dni w siatce tygodnia i kalendarza poniżej 48 dp i tekst widgetu 10 sp.
- Decyzja: `outline` rysuje granice kontrolek i ma kontrast co najmniej 3:1 (`#808A9E` w jasnym, `#707D99` w ciemnym motywie); karty, separatory i ramki dekoracyjne używają `outlineVariant` (`#E4E9F1`, `#38445B`). Ramka przycisku ikony i pola wyboru ma 1 dp, a focus 2 dp w kolorze `primary`. Notatki nazywają się wszędzie „Notatka do zajęć” i „Notatka do terminu”, bo notatka należy do terminu i przechodzi z nim po przeniesieniu (`DOMAIN.md`). Komórka siatki siedmiu dni ma co najmniej 40 dp szerokości i 48 dp wysokości; widget używa tekstu co najmniej 11 sp. Użytkownik zaakceptował wariant granic, ujednolicenie nazw i wyjątki 2026-09-27.
- Powód: WCAG 1.4.11 wymaga 3:1 dla granic kontrolek. „Notatka na dziś” była błędna przy innych dniach w „Planie”. Siedem kolumn po 48 dp nie mieści się w 288 dp treści przy 320 dp; 40 dp odpowiada komórkom wyboru daty Material 3. Widget ma mało miejsca, a 10 sp było za małe.
- Odrzucone: „Notatka do daty”, bo po przeniesieniu terminu notatka nie należy do daty; przewijana siatka dni; tekst widgetu 12 sp bez sprawdzenia układu na launcherze.

## 2026-09-26: Drobne poprawki z audytu i wersje bibliotek (I-20)

- Fakty: Audyt z 2026-09-25 wskazał usuwanie korekty i kalendarza bez potwierdzenia, formularz korekty przyjmujący dowolny dzień, prośbę o nieistniejącą na Androidzie 12 zgodę `POST_NOTIFICATIONS`, nieaktualne Core KTX i Navigation oraz eksport w trybie `w`.
- Decyzja: Usuwanie korekty i nieużywanego kalendarza wymaga potwierdzenia w dialogu, bo snackbar nie obsługuje akcji cofnięcia. Data korekty jest zapisywana jako poniedziałek wybranego tygodnia. Na API poniżej 33 aplikacja otwiera systemowe ustawienia powiadomień aplikacji. Core KTX podniesiono do 1.19.1, Navigation do 2.10.2, a eksport otwiera plik trybem `wt`.
- Powód: Kotlin 2.4.20 nie został użyty, bo 2026-09-26 najnowsze wersje to Koin compiler plugin 1.2.1 i KSP 2.3.12, obie zweryfikowane dla Kotlin 2.3.20 (`STACK.md`). Aktualizację Kotlin trzeba wykonać razem z nowymi wersjami obu narzędzi.
- Odrzucone: Cofanie usunięcia w komunikacie, bo wymagałoby rozbudowy wspólnego mechanizmu komunikatów dla dwóch rzadkich akcji.

## 2026-09-26: Edycja kierunków i kolor z pełnej palety (I-21, I-22)

- Fakty: Użytkownik poprosił o wybór koloru kierunku z palety barw zamiast sześciu stałych kolorów, o edycję nazwy i koloru utworzonych kierunków oraz o kolor widoczny obok nazwy kierunku w ustawieniach.
- Decyzja: Ustawienia dostają ekran „Kierunki” z listą globalnych kierunków i edycją nazwy oraz koloru. Kolor wybiera się ciągłym paskiem odcienia i suwakiem jasności ograniczonym do luminancji względnej od 0,18 do 0,27, co daje kontrast paska kierunku co najmniej 3:1 z jasnym tłem (`#FFFFFF`, `#F5F7FB`) i z kartami ciemnego motywu (`#202B40`, `#19243A`); zakres zawężono podczas implementacji, bo karty ciemnego motywu są jaśniejsze niż zakładał plan; pole kodu szesnastkowego obsługuje klawiaturę, a podgląd pokazuje pasek i pill. Wariant zaakceptował użytkownik 2026-09-26. Kroki są w `PLAN.md`.
- Powód: Stałe kolory nie wystarczają przy wielu kierunkach, a bez ograniczenia jasności część kolorów byłaby niewidoczna na tle aplikacji.
- Odrzucone: Dowolny kolor bez kontroli kontrastu; ostrzeżenie zamiast ograniczenia, bo zostawia nieczytelny kolor w planie i widgecie.
