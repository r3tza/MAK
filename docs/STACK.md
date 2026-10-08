# MAK: stos technologiczny

## 1. Status dokumentu

- Źródło prawdy dla narzędzi i wersji: ten plik.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-30.

## 2. Język i środowisko uruchomieniowe

- Kotlin 2.3.20.
- Android, `minSdk` 31, `targetSdk` 36 i `compileSdk` 37.2.
- Android Gradle Plugin 9.4.1 i Gradle 9.7.1.
- Compose BOM 2026.09.00, Material Icons Extended, Room 2.8.5, Lifecycle 2.11.0, Navigation 2.10.2, KSP 2.3.12, Glance 1.2.0, Core KTX 1.19.1, `kotlinx.coroutines` 1.11.0 i `kotlinx.serialization` 1.11.0.
- Stabilny zestaw zaktualizowano 2026-09-21, a 2026-09-26 podniesiono Core KTX do 1.19.1 i Navigation do 2.10.2. Kotlin 2.3.20 jest zweryfikowany przez Koin compiler plugin 1.2.1 i zgodny z KSP 2.3.12; linii 2.4.x nie użyto, bo KSP 2.3.12 jest zbudowany przeciw Kotlin 2.3.20. `compileSdk` podniesiono do 37.2, ponieważ nowe AndroidX wymagają API 37; `targetSdk` pozostaje 36, aby nie zmieniać zachowania działania.
- Narzędzia aktualizujemy do najnowszych stabilnych, wzajemnie zgodnych wersji. Nie wybieramy wersji eksperymentalnej tylko dlatego, że jest najnowsza.
- Deprecacje `androidx.compose.ui.test.junit4.createComposeRule` pozostają; migrację do `v2.createComposeRule` wykonamy osobno, ponieważ zmienia dyspozytor testów.

## 3. Warstwa aplikacji

- Jetpack Compose do budowy interfejsu.
- Jetpack Glance do widgetu.
- Koin 4.2.2 (BOM `io.insert-koin:koin-bom`) z Koin compiler plugin 1.2.1 (`io.insert-koin.compiler.plugin`) do składania zależności i ViewModeli. Obowiązuje constructor injection; ViewModele są pobierane wyłącznie na granicy hostów przez `viewModel()`/`koinViewModel`, a `get()` i `koinInject()` nie występują w ViewModelach, domenie ani komponentach ekranów. `Clock` i `ActivePlanProvider` są jednym `@Single` w grafie i nie mają domyślnych wartości w `OccurrenceViewModel`, `ScheduleViewModel`, `TodayViewModel` ani `WidgetPlanLoader`, więc brak bindingu zatrzymuje kompilację. Widget pobiera `SemesterRepository`, `ScheduleRepository`, `ActivePlanProvider` i `Clock` z Koin, bez rzutowania na `MakApplication`. Klasy tworzone przez Koin (`@KoinViewModel`, `@Single`, `@Factory`) nie mają domyślnych wartości parametrów konstruktora dla zależności z grafu: compiler plugin zostawia wtedy wartość domyślną zamiast wstrzyknąć binding, bez błędu kompilacji (w 0.2.0 `UpdateViewModel` dostał w ten sposób atrapę pobierania i instalacji). Testy przekazują atrapy jawnie. Graf jest walidowany podczas kompilacji, a `KoinGraphTest` rozwiązuje zależności i wszystkie ViewModele na urządzeniu oraz sprawdza, że `UpdateViewModel` dostaje zależności z grafu.
- Systemowe powiadomienia Androida planowane przybliżonymi alarmami `AlarmManager`. Aplikacja nie wymaga dokładnych alarmów ani ciągłego serwisu w tle.
- Lokalny font Inter.

## 4. Dane i przechowywanie

- Room jako warstwa dostępu do danych.
- SQLite jako lokalna baza danych.
- Preferences DataStore 1.2.1 do trwałych ustawień, w tym motywu.
- `kotlinx.serialization` do importu i eksportu JSON.
- Brak własnego backendu. Sieć obsługuje aktualizacje GitHub oraz opcjonalną synchronizację przez Google Drive (decyzja z 2026-09-30): jeden plik planu w formacie eksportu JSON, `HttpsURLConnection`, zgoda przez Google Identity `AuthorizationClient` i praca w tle WorkManager. Stan synchronizacji należy do `noBackupFilesDir`; token pozostaje w pamięci.

### Konfiguracja synchronizacji Google

Synchronizacja używa `play-services-auth` 22.0.0 oraz WorkManager 2.12.0. Wersje sprawdzono 2026-09-30 w [dokumentacji autoryzacji Androida](https://developer.android.com/identity/authorization) i [wykazie wydań WorkManager](https://developer.android.com/jetpack/androidx/releases/work). WorkManager planuje jednorazowe próby i pracę co 60 minut z warunkiem sieci, przez własny `WorkerFactory`; domyślny initializer zastępuje konfiguracja hosta. Testy JVM sprawdzają koordynator z atrapą transportu; rzeczywiste konto i Drive należą do I-69.

Użytkownik potwierdził brak projektu Google Cloud i odłożył czynności wymagające jego udziału. Przed I-69 wykonaj:

1. Utwórz lub wybierz jeden projekt w Google Cloud i włącz Drive API. Oba klienty Android korzystają z tego samego projektu.
2. Skonfiguruj zgodę, odbiorców i konta testowe w Google Auth Platform. Opublikuj dostępny opis aplikacji oraz politykę danych według `PRIVACY.md` (obowiązuje od wersji 0.3.0). Branding w Google Auth Platform: nazwa `MAK`, polityka prywatności https://r3tza.github.io/MAK/PRIVACY.html, strona główna https://github.com/r3tza/MAK, bez logo, bo logo wymaga weryfikacji marki. Stały adres polityki to https://r3tza.github.io/MAK/PRIVACY.html: GitHub Pages publikuje folder `docs/` z gałęzi `main` tego repozytorium, a `docs/_config.yml` ogranicza stronę do `PRIVACY.md`. W Google Auth Platform podaj ten adres.
3. Dodaj klienta OAuth Android dla `dev.retza.mak.debug` z SHA-1 certyfikatu lokalnego APK debug oraz klienta dla `dev.retza.mak` z SHA-1 istniejącego certyfikatu release. Odcisk odczytuje `apksigner verify --print-certs <APK>`. APK z CI lub innego komputera może mieć inny certyfikat debug. Nie twórz nowego klucza release do tego zadania.
4. Żądaj tylko `https://www.googleapis.com/auth/drive.appdata`, `openid` i `email`. Token przechodzi przez userinfo, a trwałą tożsamością jest `sub`. Adres e-mail służy do etykiety i wyboru konta Androida. W APK nie ma sekretu OAuth ani backendowego kodu wymiany tokenów. Szczegóły: [AuthorizationClient](https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient), [OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect) i [folder danych aplikacji](https://developers.google.com/workspace/drive/api/guides/appdata).
5. Na dwóch klientach wykonaj I-69: pierwsze połączenie pustego i niepustego telefonu, zmiana po jednej stronie, zmiana po obu stronach z wyborem wersji, praca offline, odnowienie tokenu, cofnięta zgoda, wyłączenie z zachowaniem i z usunięciem kopii oraz ponowne połączenie. Sprawdź, że oba klienty widzą ten sam plik w `appDataFolder` i że Drive zwraca `md5Checksum` po wysłaniu. Używaj najwyżej jednego emulatora naraz z 2048 MiB RAM; drugi klient to telefon fizyczny albo osobny AVD uruchomiony po całkowitym zatrzymaniu pierwszego. Każdy AVD zachowuje własne lokalne dane i ma 2048 MiB RAM, a oba korzystają ze wspólnego stanu Drive.

Lokalne testy nie wymagają projektu Google ani tokenów. Samo skompilowanie SDK i przejście atrap nie potwierdza konfiguracji OAuth ani zachowania serwera.

## 5. Testy i jakość

- Testy automatycznego obliczania tygodni A/B, korekt pojedynczych i przyszłych, aktywnego planu oraz kolizji.
- Testy rozdzielenia notatki wspólnej od notatki przypiętej do konkretnej daty.
- Testy odwołania, zmiany, przeniesienia i przywrócenia pojedynczego wystąpienia oraz zajęć jednorazowych.
- Test zgodności wyniku dla widoku listy, kalendarza, ekranu „Dzisiaj” i widgetu.
- Eksport schematu Room od pierwszej wersji i testowanie kolejnych migracji na zachowanych danych.
- Testy interfejsu dla szerokości 320–390 px.
- Testy kompozycji Glance przez `glance-testing` i `glance-appwidget-testing` 1.2.0.
- Testy JVM widgetu obejmują wariant kompaktowy i rozszerzony, pełną listę bez stopki „Jeszcze {liczba}”, pusty dzień, brak semestru, datę poza semestrem, błąd odczytu, kolizję, notatkę, długie metadane i zgodność kolejności z `ActivePlanProvider`.
- Sprawdzenie obsługi klawiatury, focusu, etykiet semantycznych, kontrastu, motywu ciemnego, `reduced motion` i dotyku.
- Sprawdzenie braku poziomego przewijania oraz obciętych akcji.
- Weryfikacja, że ekran i widget pokazują ten sam aktywny plan.
- Walidacja pełnej konfiguracji Koin podczas kompilacji. Jeśli typy Androida nie są objęte compiler pluginem, dodać uzupełniający test uruchomienia modułów.
- Kontrola spójności dokumentów: `python3 scripts/test_check_map.py` po zmianie skryptu oraz `python3 scripts/check_map.py` na repozytorium; zakres i ograniczenia są w sekcji Narzędzia.

### Sposób testowania

Agenci sprawdzają działanie aplikacji testami, które da się uruchomić lokalnie. Uruchamiaj tylko testy zmienionego zakresu, zwykle przez filtrowanie klas testowych; pełny zestaw aplikacji wymaga osobnego polecenia użytkownika. Przykład JVM: `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.sync.DrivePlanTransportTest"`. Przykład jednej klasy Android: `gradlew.bat --offline connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.settings.SyncViewModelTest"`. Priorytet ma logika domenowa na JVM. Ten sam `ActivePlanProvider` jest źródłem planu dla listy, kalendarza, ekranu „Dzisiaj”, widgetu i powiadomień, więc testy widoków nie powielają reguł planu. Nie piszemy testów rozstrzygających pytania, które nadal są otwarte w `ARCHITECTURE.md`.

Każdy test ma wykrywać błąd, który może realnie wystąpić (decyzja użytkownika z 2026-09-29). Nie piszemy testów, które:

- sprawdzają logikę atrapy zamiast kodu aplikacji, na przykład regułę repozytorium odtworzoną w `FakeRepository`; tę regułę sprawdza test Room;
- tworzą obiekt i odczytują z powrotem jego pola albo przepisują stałe;
- powtarzają regułę sprawdzoną już w teście domeny albo w innym teście tego samego komponentu; na wyższym poziomie zostaje jeden test połączenia, jeśli wywołanie może się pomylić;
- zapisują dokładne wartości projektu, na przykład punkty krzywej animacji, zamiast niezmiennika lub stanu końcowego.

Wartość mają przypadki brzegowe i ścieżki błędów: podwójne kliknięcie, anulowanie w trakcie zapisu, błąd zapisu, dane spoza zakresu, granice progów i odmiany liczebników (na przykład 12 i 22). Powtarzalną logikę, na przykład odmianę liczebników, trzymamy w jednej funkcji z jednym testem.

**JVM.** Czysty Kotlin i `java.time`, bez Compose i Room:

- `WeekCalculator`: semestr od środka tygodnia, A/B, data poza semestrem, `ONE_WEEK`, `FROM_WEEK`, nakładanie korekt;
- `ScheduleResolver`: cykle, `ONCE`, odwołanie, zmiana, przeniesienie, przywrócenie, rozdział notatki wspólnej od notatki do daty;
- `CollisionDetector`: nakładka jest kolizją, stykanie godzin nie jest; kolizje po zmianach wystąpień;
- eksport JSON: `schemaVersion` i round-trip modelu;
- walidacja: nazwa, kierunek, godziny; koniec później niż start; zajęcia przechodzące przez północ są odrzucane;
- prezenter widgetu, z testem, że loader woła ten sam `ActivePlanProvider`.

**Room.** Warstwa bazy jest cienka, więc nie ma testów CRUD dla każdego DAO. Utrzymujemy test trwałości semestru, korekty, `OccurrenceChange` i `OccurrenceNote`. Przy zmianie schematu dochodzi test migracji na zachowanych danych. Operacje wieloetapowe, w tym import i konfigurację, sprawdza test rollbacku. Testy DAO i Glance nie powielają testów domeny.

**Compose.** Emulator jest wolny, więc zostaje kilka przebiegów z wstrzykniętą datą zamiast `LocalDate.now()`:

- brak aktywnego semestru;
- ekran „Dzisiaj” po dodaniu semestru, kierunku i zajęć;
- odwołanie i przywrócenie jednego terminu;
- notatka do zajęć kontra notatka do terminu;
- szerokość 320 dp: brak poziomego przewijania, akcje widoczne.

Kontrast, `reduced motion` i motyw ciemny sprawdzamy w kodzie oraz na emulatorze lub urządzeniu, dopóki nie ma stałego urządzenia w CI.

## 6. Narzędzia

- Android Studio i Gradle do budowania istniejącego projektu aplikacji.
- Git do historii zmian.
- Python 3 ze standardową biblioteką do kontroli dokumentacji: `scripts/check_map.py` sprawdza lokalne odnośniki, format `PLAN.md`, spójność `QUEUE.md` i limit `LOG.md`, a `scripts/test_check_map.py` (`unittest`) pokrywa jego przypadki. Skrypt nie używa sieci, nie uruchamia Gradle i nic nie zapisuje.
- Lokalna baza danych i pliki JSON bez usług zewnętrznych.
- Na Windowsie interpreter Pythona nazywa się zwykle `python`; polecenia zapisane z `python3` uruchamiaj wtedy przez `python`. Jeśli `python` otwiera Microsoft Store albo zwraca „nie znaleziono Python”, użyj programu uruchamiającego `py`.
- Duży ekran bez tabletu: na emulatorze telefonu `adb shell wm size 2560x1600` i `adb shell wm density 320` dają najkrótszy bok 800 dp (tablet poziomo), `wm size 1600x2560` tablet pionowo, a `wm size 1400x2000` z tą samą gęstością 700 dp. Po sprawdzeniu przywróć `adb shell wm size reset` i `adb shell wm density reset`. Emulator ma język angielski, więc nadaje się też do sprawdzania polskich tekstów niezależnych od języka telefonu.
- Gradle wymaga Android SDK: pliku `local.properties` z `sdk.dir` poza repozytorium albo zmiennej `ANDROID_HOME`. Domyślna lokalizacja na Windowsie to `%LOCALAPPDATA%\Android\Sdk`.
- Testy Compose i Room uruchamia `gradlew.bat connectedDebugAndroidTest` na emulatorze albo urządzeniu. Sama kompilacja (`compileDebugAndroidTestKotlin`) nie zastępuje uruchomienia. Jedną klasę testów uruchamia `gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.today.TodayScreenTest"`.
- Uruchamiaj najwyżej jeden emulator naraz i ustawiaj mu 2048 MiB RAM. Dla odbioru dwóch klientów użyj telefonu fizycznego albo dwóch AVD uruchamianych sekwencyjnie po całkowitym zatrzymaniu poprzedniego; każdy AVD ma własne dane lokalne i współdzieli stan Drive.
- Zrzuty i testy Compose na emulatorze `Medium_Phone_A12` (decyzja użytkownika z 2026-10-08, bo na `Medium_Phone` brakuje miejsca na APK debug; gęstość 420, `adb` z `%LOCALAPPDATA%\Android\Sdk\platform-tools`): `gradlew.bat installDebug`, potem `adb shell screencap -p /sdcard/s.png` i `adb pull /sdcard/s.png plik.png`. Na tym obrazie `adb exec-out screencap` daje czarny ekran. `Medium_Phone` ma dane z odbioru synchronizacji; nie czyść go bez zgody użytkownika. Szerokość 320 dp: `adb shell wm size 840x1866`, powrót: `adb shell wm size reset`. Motyw ciemny: `adb shell cmd uimode night yes`, powrót: `night no`. Skala czcionki 2,0: `adb shell settings put system font_scale 2.0`, powrót: `1.0`. Wersja debug ma dane demonstracyjne. Porównuj zrzuty przed zmianą i po niej w tych samych ustawieniach.
- W Git Bash na Windowsie długi skrypt z polskimi znakami zapisz do pliku i uruchom przez `py plik.py`. Przekazany przez heredoc bywa przekłamany.
- Wersja release ma wyłączoną minifikację. Przed włączeniem `isMinifyEnabled` trzeba dodać reguły R8 dla `kotlinx.serialization` i klas eksportu JSON, inaczej import i eksport przestaną działać.

## 7. Środowisko

Repozytorium zawiera aplikację Android, konfigurację Gradle, lokalną bazę Room i testy. Aplikacja działa lokalnie na urządzeniu. Planowane testy zewnętrzne nie wymagają infrastruktury serwerowej.

### Wydania i licencja (decyzja z 2026-09-27)

- Repozytorium `r3tza/MAK` jest publiczne od 2026-09-27. Przed zmianą widoczności sprawdzono historię i bieżące pliki pod kątem sekretów i prywatnych danych. Pliki kluczy podpisu są wykluczone w `.gitignore`. Kod ma licencję Apache 2.0 (`LICENSE`), a właścicielem praw w `NOTICE` jest `r3tza`. Licencja nie obejmuje nazwy „MAK” ani ikony. Historia gita zostaje bez zmian, razem z adresem e-mail autora w commitach.
- Zmiany trafiają na `main` tylko przez pull request (decyzja użytkownika z 2026-09-28). Workflow `.github/workflows/checks.yml` uruchamia na każdym pull requeście kontrolę dokumentacji i testy JVM (`gradlew test`); ochrona gałęzi `main` wymaga jego zielonego wyniku. Testy Compose zostają lokalne, na emulatorze. Osobnej gałęzi `dev` nie ma: stan wydania wyznacza tag.
- Wydanie budują GitHub Actions po wypchnięciu tagu `v<major>.<minor>.<patch>`. Workflow uruchamia testy i tworzy szkic GitHub Release; użytkownik publikuje go ręcznie po sprawdzeniu plików. Wydaniem jest zawsze wersja release, bo wersja debug wczytuje dane demonstracyjne.
- Wypchnięcie taga, push, otwarcie pull requesta i publikacja wydania wymagają wyraźnego polecenia użytkownika; zielone kontrole lokalne same nie upoważniają do publikacji.
- Jeden klucz podpisu release na zawsze. Klucz generuje użytkownik lokalnie; jest przechowywany w sekretach GitHuba i w dwóch kopiach poza nim. Nie trafia do repozytorium ani do rozmowy z agentem. Utrata klucza uniemożliwia aktualizację bez odinstalowania aplikacji i utraty lokalnych danych.
- `versionCode` rośnie z każdym wydaniem i jest wyliczany z tagu. Wydanie z niższym albo równym `versionCode` jest odrzucane.
- Wariant debug ma `applicationIdSuffix = ".debug"` i może być zainstalowany obok podpisanego wydania. Dzięki temu testy urządzenia nie wymagają obniżania `versionCode`, zmiany klucza wydania ani usuwania danych użytkownika.
- GitHub Release zawiera APK i plik `update.json` z wersją, adresem APK, sumą SHA-256 i notatkami. Aplikacja czyta tylko ten plik, bez API GitHuba i bez tokenu.
- Notatki wydań dla użytkowników mają jedno źródło: `app/src/main/assets/release_notes.json` (decyzja z 2026-09-27). Plik zawiera tekst `minorChanges` i listę wydań od najnowszego z wersją, datą i listą zmian odczuwalnych dla użytkownika. Aplikacja czyta go w „O aplikacji”, a workflow bierze wpis dla wersji z tagu do pola `notes` w `update.json` (jedna zmiana w linii) i do opisu wydania na GitHubie. Pusta lista zmian oznacza tekst `minorChanges` („Pomniejsze poprawki”). Brak wpisu dla wersji z tagu przerywa workflow przed budowaniem. `docs/CHANGELOG.md` zostaje techniczną historią dla agentów.
- Szkice nie są widoczne pod `releases/latest`, więc aktualizator widzi wydanie dopiero po ręcznej publikacji. Aplikacja nie dostaje tokenu GitHuba. Do czasu pierwszego opublikowanego wydania (I-49) `update.json` zwraca 404, a ręczne sprawdzenie pokazuje „Nie udało się sprawdzić”.

## 8. Odrzucone alternatywy

- Backend i Firebase: odrzucone, ponieważ plan ma działać w pełni bez sieci.
- Obowiązkowe konta i własny backend pozostają odrzucone. Wcześniejsze odrzucenie opcjonalnej synchronizacji Google zastępuje polecenie użytkownika z 2026-09-30; działanie opisuje `SYNC.md`.
- Ciągły serwis w tle i odświeżanie widgetu co minutę: odrzucone z powodu zużycia baterii.
- Zewnętrzne CDN-y: odrzucone; aplikacja używa lokalnych zasobów.
- Aktualizacje przez Google Play (Play In-App Updates): odrzucone, bo aplikacja nie jest dystrybuowana w Google Play.
- WorkManager do okresowego sprawdzania aktualizacji w tle: odrzucony na rzecz sprawdzenia przy uruchomieniu aplikacji, które nie wymaga pracy w tle, nowej zależności ani zgody na powiadomienia.
- Obtainium jako jedyny sposób aktualizacji: odrzucone, bo wymaga od mało technicznych użytkowników instalacji i konfiguracji drugiej aplikacji.
- Gałąź `dev` z przenoszeniem zmian na `main` przy wydaniu: odrzucona, bo stan wydania wyznacza tag, użytkownicy korzystają z GitHub Releases, a druga długo żyjąca gałąź wymaga stałego scalania w obie strony i łatwo o pull request do złej gałęzi.
- Osobne publiczne repozytorium tylko na wydania: odrzucone, bo główne repozytorium zostanie upublicznione przed pierwszym wydaniem dla znajomych, a osobne wymagałoby dodatkowego tokenu w workflow.
- Licencje MIT i GPL 3.0: odrzucone. MIT nie wyklucza wprost prawa do nazwy i znaku; GPL 3.0 utrudnia forkom połączenie z zamkniętymi Usługami Google Play przy ewentualnej synchronizacji.
