# MAK - log projektu

Najnowsze wpisy są u góry. Czytaj kilka ostatnich przy rozpoczynaniu pracy. Trwałe reguły są w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md`, `ARCHITECTURE.md` i `STACK.md`. Archiwum jest zapisem historii, nie źródłem bieżącego statusu.

Limit: 20 wpisów datowanych. Przy dodaniu kolejnego przenieś najstarszy do `log_archive/<rok>.md` w tym samym commicie. Zachowaj treść i kolejność archiwizowanych wpisów.

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

## 2026-09-26: Blokada wyboru kierunku po zapisie kroku kreatora (I-16)

- Fakty: Kreator pamięta kierunek zapisany w kroku 2. Po powrocie do tego kroku zmiana trybu z „Wybierz istniejący” na „Nowy kierunek” zmieniłaby nazwę współdzielonego kierunku, a wybór innego istniejącego kierunku zostawiłby w semestrze drugie przypisanie. `PLAN.md` kazał nie zgadywać, co zrobić z pierwszym przypisaniem.
- Decyzja: Po zapisie kroku 2 wybór trybu i kierunku jest zablokowany do końca kreatora. Nazwę i kolor kierunku utworzonego w tym kreatorze nadal można poprawić. Kolejne kierunki dodaje się w ustawieniach semestru. Opis jest w `FEATURES.md`.
- Powód: Blokada nie zmienia danych innych semestrów i nie wymaga reguły zastępowania przypisania, które mogło już dostać zajęcia.
- Odrzucone: Zastępowanie pierwszego przypisania nowym, bo kaskada usunęłaby jego zajęcia; dopisywanie drugiego przypisania bez wiedzy użytkownika.

## 2026-09-26: Aktywacja nowego semestru i dane po edycji zajęć (I-18, I-19)

- Fakty: Audyt z 2026-09-25 wykazał, że semestr utworzony w kreatorze zawsze staje się aktywny, a zmiana dnia, cyklu albo daty zajęć ukrywa ich zmiany wystąpień i notatki przypięte do dotychczasowych dat.
- Decyzja: Nowy semestr staje się aktywny tylko wtedy, gdy dzisiejsza data mieści się w jego kalendarzu albo gdy nie ma aktywnego semestru; drugi warunek dodano, bo bez aktywnego semestru utworzony plan byłby niedostępny. Edycja zajęć zachowuje zmiany wystąpień i notatki, a formularz przed zapisem informuje, ile z nich przestanie być widocznych. Reguły zapisano w `DOMAIN.md`, a kroki w `PLAN.md` (kroki 4 i 5).
- Powód: Dodanie semestru z wyprzedzeniem nie może przełączać bieżącego planu. Utrata danych użytkownika bez jego wiedzy jest niedopuszczalna, a zachowane dane wracają po przywróceniu poprzedniego terminu.
- Odrzucone: Aktywacja każdego nowego semestru; pytanie o aktywację przy każdym dodaniu; usuwanie osieroconych zmian i notatek po potwierdzeniu; automatyczne przenoszenie ich na nowe daty, bo odwzorowanie starych dat na nowe jest niejednoznaczne przy zmianie cyklu.
- Poza zakresem: automatyczne przełączenie aktywnego semestru, gdy nadejdzie data nowego.

## 2026-09-25: Odtwarzanie formularza zajęć (I-12)

- Fakty: Żaden ViewModel nie używał `SavedStateHandle`, więc po zakończeniu procesu przez system wpisane dane formularzy znikały. Trasa edycji zajęć wołała `openEdit` przy każdym odtworzeniu ekranu, także po obrocie, i nadpisywała wpisane zmiany danymi z bazy.
- Decyzja: Użytkownik zawęził I-12 do formularza zajęć. `ClassEditViewModel` zapisuje wartości wpisane przez użytkownika i identyfikator edytowanych zajęć w `SavedStateHandle` i odtwarza je przy tworzeniu. Trasa woła `openEditIfNeeded`, które nie wczytuje zajęć ponownie, jeśli szkic dotyczy tych samych zajęć. Otwarcie z listy albo szczegółów nadal zaczyna od danych z bazy.
- Powód: Najbardziej prawdopodobny scenariusz utraty danych to przepisywanie planu z innej aplikacji podczas dodawania zajęć. Pozostałe formularze są krótkie, a pełne odtwarzanie wszystkich ekranów nie jest powszechną praktyką i zwiększyłoby koszt zmian.
- Odrzucone: Odtwarzanie wszystkich formularzy i przeniesienie ViewModeli na zakres tras w tym zadaniu; `Bundle` w `SavedStateHandle`, bo testy JVM nie sprawdzałyby zapisu.
- Weryfikacja: Testy JVM odtwarzają ViewModel z tego samego `SavedStateHandle` dla nowych zajęć i edycji oraz sprawdzają, że ponowne otwarcie trasy nie nadpisuje szkicu. Na emulatorze wpisane dane przetrwały `am kill` procesu w tle. `connectedDebugAndroidTest` obejmuje `KoinGraphTest` z nowym parametrem.

## 2026-09-25: Poprawki z audytu I-08 do I-13

- Fakty: Audyt z 2026-09-23 wykazał powtórzony identyfikator wystąpienia, notatkę znikającą po przeniesieniu terminu, zegar trzymający strefę z chwili startu, nieobsłużone wyjątki zapisu i pracy w tle oraz drobne braki.
- Decyzja: Wystąpienie identyfikują zajęcia i data oryginalna (I-08). Notatka do wystąpienia jest zapisana pod datą oryginalną; Room ma wersję 3, a migracja v2 do v3 i import pliku w wersji 2 przepinają istniejące notatki jedną funkcją `remapOccurrenceNotesToOriginalDates` (I-09). Eksport zapisuje wersję 3, import przyjmuje wersje 2 i 3; zastępuje to wcześniejszą decyzję o imporcie wyłącznie wersji 2, bo plik w wersji 2 ma komplet danych, a przepięcie jest regułą, nie zgadywaniem. `SystemZoneClock` odczytuje strefę przy każdym użyciu (I-10). Zapisy i praca w tle zgłaszają błąd albo go pomijają bez zamykania aplikacji, a `CancellationException` przechodzi dalej (I-11). Odbiornik przeliczania alarmów przyjmuje tylko akcje z manifestu, import ma limit 5 MB, „Plan” przechodzi na nowy dzień po powrocie, kod główny nie ma ostrzeżeń kompilatora, a dokumentacja opisuje środowisko Windows i warunek R8 (I-13).
- Powód: Poprawność kolizji, notatek i alarmów oraz odporność na błędy bez utraty danych.
- Odrzucone: UUID dla rekordów Room, bo aplikacja nie synchronizuje danych; zmiana nazwy kolumny `occurrence_date`; migracja alarmów zapisanych w starym formacie identyfikatora.
- Weryfikacja: `gradlew.bat test lintDebug connectedDebugAndroidTest` przechodzi na emulatorze Android 16: 286 testów JVM i 67 testów urządzenia, w tym migracja v2 do v3. Lint: 0 błędów, 7 ostrzeżeń (wersje bibliotek i SDK, grafika podglądu widgetu). Migracja na prawdziwych danych należy do O-01.

## 2026-09-23: Przegląd czytelności interfejsu

- Fakty: Przegląd na emulatorze w motywie jasnym i ciemnym oraz przy 320 dp wykazał tekst 9 do 11 sp w wielu miejscach, niewidoczną strzałkę pól wyboru (znak „▾”), małe liczby na karcie podsumowania, kolorowe kropki kalendarza bez legendy, wiersz zmiany tygodnia wyglądający jak nieaktywne pole, powtórzone tytuły i przyciski powrotu na ekranach semestru, odwróconą hierarchię w ustawieniach, dwa style pól formularza, daty ISO na ekranie „Kierunki” oraz jasne pille w motywie ciemnym.
- Decyzja: Użytkownik zaakceptował trzy pakiety poprawek. Typografia ma minimum 12 sp (11 sp tylko dla wersalikowych nadtytułów), liczby na karcie podsumowania 28 sp. Pola wyboru to rozwijane pole Material 3 z etykietą przesuwaną nad ramkę, jak pola tekstowe; dwa pola obok siebie przechodzą jedno pod drugie poniżej 340 dp. Ekrany semestru mają tylko tytuł w górnym pasku i krótki opis, bez przycisków „Wróć”. Szczegóły terminu nie mają zbędnego podtytułu ani przycisku „Zamknij”, a przyciski zapisu notatek pojawiają się dopiero po zmianie treści. Kropki kalendarza mają kolor kierunku i legendę. Wiersz tygodnia ma ikonę i słowo „Zmień”. Karta kierunku pokazuje kolor, a „Usuń” ma kolor błędu. Pille w motywie ciemnym są przyciemnione. Nazwa zajęć na karcie ma pełną szerokość, a kierunek, status i typ zawijają się w wierszu pod nią.
- Powód: Czytelność, zasada, że kolor nie jest jedynym nośnikiem informacji, jeden nagłówek na ekran i spójne formularze.
- Odrzucone: Globalny odstęp w `MakScreenContent`; własny komponent listy wyboru zamiast Material 3; ukrycie typu zajęć na karcie.
- Poprawki przy okazji: Pierwsze uruchomienie testów Compose na emulatorze wykazało, że dotknięcie pola daty albo godziny poza ikoną nie otwierało wyboru, bo pole tekstowe przechwytywało dotknięcie. Pola otwierają teraz wybór po dotknięciu w dowolnym miejscu. Część testów Compose zawierała błędy, których nie wykryto, bo testy były tylko kompilowane: szukały nadtytułu małymi literami, nie rozróżniały powtórzonych tekstów, pomijały przewinięcie i miały niespójny stan. Poprawiono je bez zmiany sprawdzanych reguł.
- Weryfikacja: `gradlew.bat test lintDebug connectedDebugAndroidTest` przechodzi na emulatorze Android 16: 261 testów JVM i 66 testów urządzenia. Ekrany obejrzano na emulatorze w obu motywach przy 320 dp i domyślnej szerokości. TalkBack należy do O-05.

## 2026-09-23: Odstępy ekranów ustawień i sekcje powiadomień

- Fakty: `MakScreenContent` nie ustawiał odstępów między elementami, a ekrany `Semestry`, `Dane`, `Powiadomienia` i podgląd importu ich nie dodawały, więc komunikaty, pola i przyciski stykały się ze sobą. Nagłówek tygodnia na ekranie „Plan” stykał się z oznaczeniem A/B. Komunikat informacyjny miał 10 sp tekstu i ciasny padding. Ekran powiadomień mieszał przełączniki obu rodzajów, a komunikat „Uwaga” zawierał zastępczy tekst „Treść i moment wysyłki zostaną ustalone.”.
- Decyzja: `MakScreenContent` przyjmuje `verticalArrangement`. Ekrany podrzędne ustawień używają odstępu 12 dp i 12 dp od górnego paska, ekran powiadomień 16 dp między sekcjami. Nagłówek tygodnia ma 12 dp odstępu od oznaczenia A/B. `MakNoteBanner` ma padding 16 i 12 dp oraz tekst 13 i 12 sp. Powiadomienia są podzielone na sekcje „Kolizje w planie”, „Dzień wcześniej” i „Przed zajęciami”; godzina i wyprzedzenie są widoczne tylko przy włączonym danym rodzaju. Zastępczy tekst zastąpiono komunikatem „Czas dostarczenia” o możliwym opóźnieniu przez Androida.
- Powód: Prośba użytkownika o większe odstępy i rozdzielenie kategorii powiadomień; zasady odstępów z `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
- Odrzucone: Globalny odstęp w `MakScreenContent` dla wszystkich ekranów, bo część ekranów ma już własne odstępy i zostałyby podwojone.
- Weryfikacja: `SettingsScreenTest` sprawdza sekcje powiadomień i ukrycie ustawień wyłączonego rodzaju przy 320 dp. `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi. Ekrany „Plan”, „Semestry”, „Dane” i „Powiadomienia” obejrzano na emulatorze w motywie ciemnym. Szerokość 320 dp i TalkBack na urządzeniu należą do O-05.

## 2026-09-23: Nagłówek i separatory podsumowania „Dzisiaj”

- Fakty: Podpis „Twój plan na dziś” miał 11 sp i 78% krycia, czyli był mniejszy od etykiet kolumn. Separatory liczb miały 25% krycia i ginęły na gradiencie, a „Dziś bez zajęć” było osobnym tekstem pod liczbami.
- Decyzja: Na prośbę użytkownika nagłówek karty ma 17 sp, pogrubienie, pełną biel i semantykę nagłówka. W dniu bez zajęć nagłówek brzmi „Dziś bez zajęć” zamiast „Twój plan na dziś”, a tekst pod liczbami usunięto. Separatory mają 50% krycia i 40 dp wysokości. `MakSummaryCard` przyjmuje `title`, a treść nagłówka wybiera `TodayScreen`.
- Powód: Nagłówek ma otwierać hierarchię karty, a stan pustego dnia jest najważniejszą informacją i nie powinien stać na końcu.
- Odrzucone: Zmiana gradientu, rozmiaru liczb i układu kolumn.
- Weryfikacja: `TodayScreenTest` sprawdza nagłówek z semantyką w obu stanach i brak drugiego tekstu. Odbiór wyglądu na urządzeniu należy do O-05.

## 2026-09-23: Notatka do wystąpienia podąża za terminem

- Fakty: Audyt z 2026-09-23 wykazał, że notatka do wystąpienia jest przypięta do daty faktycznej. Po przeniesieniu terminu zostaje pod starą datą i nie jest widoczna nigdzie. Identyfikator wystąpienia także opierał się na dacie faktycznej, więc termin przeniesiony na dzień zwykłego terminu tych samych zajęć miał ten sam identyfikator (I-08).
- Decyzja: Użytkownik zdecydował, że notatka podąża za przeniesionym terminem. `OccurrenceNote.occurrenceDate` oznacza datę oryginalną terminu, a wystąpienie identyfikują zajęcia i data oryginalna. Istniejące notatki przepina migracja Room v2 do v3 i import pliku w wersji 2, obie przez jedną funkcję. Przy niejednoznacznym przypadku notatka zostaje, a przy konflikcie treści są łączone, nie usuwane.
- Powód: Notatka opisuje konkretny termin zajęć, nie dzień w kalendarzu. Tożsamość przez datę oryginalną jest unikalna i odpowiada praktyce iCalendar (`RECURRENCE-ID`).
- Odrzucone: Pozostawienie notatki przy dacie zapisu; przenoszenie notatki w repozytorium przy każdej zmianie terminu; zmiana nazwy kolumny `occurrence_date`.
- Weryfikacja: Do wykonania w I-08 i I-09 według `PLAN.md`, kroki 1 i 5.

## 2026-09-22: Dokończenie układu widgetu (I-06)

- Fakty: Widget miał nagłówek, pasek kierunku, metadane, alert kolizji, etykietę notatki, separatory i warianty rozmiaru, ale nie pokazywał faz zajęć ani nie sygnalizował, co trwa i co jest następne, choć `ARCHITECTURE.md` dopuszcza różną prezentację zakończonych, trwających i następnych zajęć.
- Decyzja: `WidgetPresenter.present` przyjmuje teraz `now` z `Clock` i oznacza każde wystąpienie jako `Past`, `Current`, `Next` albo `Scheduled`. `WidgetPlanLoader` przekazuje `LocalTime.now(clock)`. Wiersz pokazuje etykietę „Teraz” dla trwających i „Następne” dla najbliższych, a zakończone mają neutralny kolor tekstu. Kolumna czasu ma jedną linię, żeby nie ucinać godziny. Nie zmieniano odświeżania, kliknięcia, rozmiarów ani źródła planu.
- Powód: Prezentacja zależna od czasu ma być czytelna, ale opierać się wyłącznie na czasie odczytanym przy odświeżeniu i nie obiecywać aktualizacji co minutę.
- Odrzucone: Odliczanie na żywo i dokładne alarmy; osobne akcje w wierszu; zmiana logiki odświeżania i rozmiarów; kopiowanie reguł planu.
- Weryfikacja: `WidgetPresenterTest.presenterMarksCurrentNextAndPastPhases` sprawdza fazy, a istniejące testy prezentera i kompozycji Glance przechodzą. `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi. Odbiór na launcherze należy do O-06.

## 2026-09-22: Uporządkowane ustawienia i osobne ekrany (I-05)

- Fakty: Ekran główny ustawień miał lokalną etykietę „USTAWIENIA”, nagłówek „Semestry i wygląd”, wybór semestru, motyw, próg okienka, rozwijane bloki „dane” i „powiadomienia” oraz akcje zarządzania semestrem. Wszystkie ustawienia były w jednym miejscu.
- Decyzja: Ekran główny pokazuje tylko sekcje „Plan”, „Wygląd”, „Powiadomienia”, „Dane” i „O aplikacji” w neutralnych kontenerach rozdzielonych 16 dp, bez lokalnego nagłówka i rozwijanych formularzy. Sekcja „Plan” ma wybór aktywnego semestru, wiersz „Zarządzaj semestrami” i próg okienka. Dodano osobne trasy `settings/semesters`, `settings/notifications` i `settings/data` oraz ekrany `SettingsSemestersScreen` (lista, wybór aktywny, konfiguracja, usuwanie, dodawanie), `SettingsNotificationsScreen` (przełączniki, godzina, wyprzedzenie, blokada systemowa) i `SettingsDataScreen` (eksport, import, opis zastąpienia). Wiersz powiadomień na ekranie głównym ma dwie linie: „Włączone”/„Wyłączone” oraz osobny wiersz z godziną i wyprzedzeniem, a przy blokadzie systemowej „Zablokowane przez system” bez godzin. Podgląd importu zostaje osobnym ekranem. Tytuły ekranów są w topbarze.
- Powód: Ekran główny ma służyć szybkiemu odczytowi i przejściu do właściwego obszaru, a rozbudowane formularze mają osobne trasy w jednym `NavHost`.
- Odrzucone: Rozwijane formularze na ekranie głównym; powtórzony nagłówek; zmiana logiki eksportu, importu i powiadomień; ruszanie widgetu (I-06).
- Weryfikacja: `SettingsScreenTest` sprawdza sekcje i przejścia na ekranie głównym, brak lokalnego nagłówka („Semestry i wygląd” i „USTAWIENIA” nie istnieją), dwie linie podsumowania powiadomień, ekran „Semestry”, ekran „Powiadomienia”, ekran „Dane” i podgląd importu przy 320 dp. `docs/KNOWN_ISSUES.md` opisuje istniejący kod i zostawia odbiór (O-05). `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi. Odbiór wyglądu i nawigacji na urządzeniu należy do O-05.

## 2026-09-22: Neutralna sekcja rozwijana i odstępy (I-04)

- Fakty: `MakExpandableSection` zmieniał tło nagłówka na `secondaryContainer` po rozwinięciu i używał `surfaceVariant` w treści, a przycisk „Wróć do ustawień” miał tylko 8 dp odstępu nad sobą.
- Decyzja: Cały kontener używa jednego neutralnego tła `surfaceContainerLow` w obu stanach, a treść ma 16 dp paddingu; rozwinięcie pokazują tekst „Ukryj”/„Pokaż”, kierunek ikony i semantyka, bez zmiany na kolor akcentowy. Tekst i ikona mają kolor `onSurfaceVariant`, a obramowanie `outlineVariant` (`primary` tylko przy focusie). Przycisk „Wróć do ustawień” w semestrze dostał 12 dp odstępu.
- Powód: Neutralne tło w obu stanach i przewidywalne odstępy wynikają z `ARCHITECTURE.md`, sekcja „Stały język wizualny”.
- Odrzucone: Tło kontenera akcentowego po rozwinięciu; zmiany odstępów poza realne naruszenia; ruszanie kart zajęć i podsumowania „Dzisiaj”.
- Weryfikacja: `SettingsScreenTest.expandableSectionTogglesAt320Dp` sprawdza rozwinięcie i zwinięcie przy 320 dp. `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi. Koloru tła nie da się sprawdzić bez urządzenia, więc odbiór wizualny należy do O-05.

## 2026-09-22: Kontrola dokumentacji `scripts/check_map.py` (I-07)

- Fakty: Dokumenty rosły bez automatycznej kontroli linków, limitów `PLAN.md` i `LOG.md` oraz spójności tabeli `QUEUE.md`, więc łatwo było zostawić martwy odnośnik albo wpis po limicie.
- Decyzja: Dodano `scripts/check_map.py` używający wyłącznie standardowej biblioteki Pythona. Skrypt sprawdza lokalne odnośniki Markdown w `README.md`, `AGENTS.md`, `CLAUDE.md` i `docs/*.md` z uwzględnieniem wielkości liter, wymaga formatu `## N. tytuł (ID)` w `PLAN.md`, limitu pięciu kroków i istnienia kroków w `QUEUE.md`, wykrywa w `QUEUE.md` powtórzone identyfikatory, nieznane statusy i zależności do nieistniejących zadań, a w `LOG.md` przekroczenie 20 wpisów. Wypisuje plik i numer wiersza, zwraca 0 albo 1 i nic nie zapisuje. `scripts/test_check_map.py` pokrywa dziewięć przypadków na katalogach tymczasowych.
- Powód: Kontrola ma być tania, lokalna i bez dodatkowych pakietów, żeby wychwycić regresję dokumentacji przed commitem.
- Odrzucone: `scripts/check_text.py` i CI na tym etapie; uruchamianie Gradle albo sieci ze skryptu; zapisywanie plików przez skrypt.
- Narzędzia i mapa: `docs/STACK.md` opisuje Python 3 ze standardową biblioteką i oba skrypty bez sieci oraz Gradle, a `docs/MAP.md` wskazuje `scripts/check_map.py` i jego testy przy pytaniu o spójność dokumentów. `AGENTS.md` i `docs/WORKFLOW.md` wymagają uruchomienia skryptu po zmianie dokumentów.
- Weryfikacja: `python3 scripts/test_check_map.py` przechodzi (9 testów), a `python3 scripts/check_map.py` na bieżącym repozytorium zwraca kod 0. Nadmiar kroku planu i wpisu logu raportowany jest z numerem wiersza. Status I-07 to `gotowe`, a `PLAN.md` startuje teraz od I-04.

## 2026-09-22: Karty zajęć z dwoma notatkami (I-03)

- Fakty: `ClassItemUi` miało jedno pole `note` (`occurrenceNoteBody ?: classNote`), a `ClassCard` pokazywał jedną notatkę bez etykiety; pill kierunku używał kontenera motywu, nie koloru kierunku.
- Decyzja: `ClassItemUi` ma osobne `classNote` i `occurrenceNote`, a `PlannedOccurrence.toUi` przepisuje obie bez `?:`. `ClassCard` zachowuje siatkę 48 dp i prawą kolumnę w kolejności: nazwa, pill kierunku i typ, metadane, cienki separator, kolizja, notatki. Notatka wspólna ma pill „Notatka do zajęć” (indygo) z treścią pod etykietą, notatka wystąpienia pill „Notatka na dziś” (fiolet) w osobnym wierszu. `CoursePill` używa jaśniejszego wariantu koloru kierunku jako tła i dobiera kolor tekstu po jasności tego tła, a pasek przy krawędzi nadal używa pełnego koloru. Między kolizją a notatkami jest drugi cienki separator, gdy występują oba rodzaje treści. `classCardDescription` odczytuje obie notatki z etykietami i nie spłaszcza ich.
- Powód: Oba rodzaje notatek są różne i muszą być rozróżnialne etykietą i kolorem, a karta ma mieć jawne sekcje i czytelną semantykę.
- Odrzucone: Scalanie notatek w jeden tekst; wybór jednej notatki operatorem `?:`; kolorowanie całej karty; zmiana widgetu (I-06).
- Weryfikacja: `PlanMappingTest` (obie notatki osobno, jedna, brak, blank, kolizja nie nadpisuje notatek), `ScheduleScreenTest` (etykiety obu notatek i nazwa kierunku przy 320 dp), `ScheduleViewModelTest` (odwołany termin zachowuje notatkę do daty) i `TodayScreenTest` (obie etykiety przy 320 dp). `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi. Odbiór wyglądu na urządzeniu należy do O-05.

## 2026-09-22: Podsumowanie „Dzisiaj” (I-02)

- Fakty: Ekran „Dzisiaj” pokazywał jedną wartość (liczba zajęć), karta miała ozdobną ikonę, a próg okienka nie istniał.
- Decyzja: Dodano domenowe `countGaps(occurrences, thresholdMinutes)` (łączenie nakładających się zajęć w bloki, okienko tylko gdy przerwa jest ściśle większa od progu, bez czasu przed pierwszym i po ostatnim) oraz `uniqueCollisionCount(collisions)` z kluczem pary wystąpień i zakresu nakładania. `WidgetPresenter` używa teraz `uniqueCollisionCount`, więc widget i ekran liczą kolizje tak samo. Próg zapisano w `SettingsPreferences` (`gap_threshold_minutes`, domyślnie 30, zakres 5..180). `TodayViewModel` wstrzykuje `SettingsPreferences` i wystawia `classCount`, `collisionCount` i `gapCount`; `MakSummaryCard` ma trzy równe kolumny „Zajęcia”, „Kolizje”, „Okienka” z etykietą nad liczbą, subtelnymi separatorami i bez ikony; liczba kolizji jest czerwona powyżej zera i zielona przy zerze, a przy braku zajęć w istniejącym semestrze karta pokazuje „Dziś bez zajęć” i trzy zera. Bez aktywnego semestru karta się nie pokazuje, a ekran zostaje w stanie pustym z „Skonfiguruj plan”. Każda kolumna ma semantykę scalającą etykietę i liczbę dla TalkBacka. Na obecnym `SettingsScreen` doszedł wybór progu 15, 20, 30, 45, 60 minut obok aktywnego semestru.
- Powód: Reguła okienka i kolizji ma jedno źródło w domenie, a próg jest globalny i trwały.
- Odrzucone: Liczenie okienek w Compose; drugi wzór klucza kolizji w widgecie; przebudowa całego ekranu ustawień (I-05) albo kart zajęć (I-03); nowa trasa ustawień.
- Weryfikacja: `GapCounterTest` i `CollisionCountTest` na JVM, `TodayViewModelTest` (liczby, próg, brak semestru) i `SettingsViewModelTest` (domyślne 30 i zapis 45). `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi; `TodayScreenTest` (kompilowany) sprawdza trzy etykiety, zera i brak ikony przy 320 dp oraz brak karty i „Dziś bez zajęć” bez aktywnego semestru. Odbiór wyglądu na urządzeniu należy do O-05.
