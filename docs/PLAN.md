# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Kolejność ustalił użytkownik 2026-09-27: najpierw aktualizacje w aplikacji, bo mechanizm aktualizacji musi być w pierwszej wersji, którą dostaną znajomi, a tryb tabletowy po nich. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

Repozytorium pozostaje prywatne podczas I-36 do I-40 i poprawek prezentacyjnych. Aplikacja nie dostaje tokenu GitHuba, więc prawdziwy adres `releases/latest` zacznie działać dopiero po upublicznieniu repozytorium i ręcznym opublikowaniu wydania. Do tego czasu granice sieci, pobierania i instalacji muszą być wstrzykiwalne, a testy używają fałszywych źródeł, kontrolowanego zegara i lokalnych artefaktów. Nie publikuj taga ani wydania w krokach 1 do 5.

## 1. Model, walidacja i sprawdzanie `update.json` (I-36)

Granica: nowy pakiet `dev.retza.mak.update`; model i reguły pozostają czystym Kotlinem, a implementacja HTTP jest jedyną częścią zależną od sieci.

1. Dodaj `UpdateInfo` z `@Serializable`: `versionCode: Int`, `versionName: String`, `apkUrl: String`, `sha256: String`, `minSdk: Int`, `notes: String = ""`. Parser używa `Json { ignoreUnknownKeys = true }`.
2. Dodaj `parseUpdateInfo(bytes)` z wynikami `Valid` i `Invalid`. Odrzuć uszkodzony JSON, liczby mniejsze od 1, pustą nazwę wersji, adres bez `https`, host inny niż dokładnie `github.com` oraz SHA-256 inny niż 64 znaki szesnastkowe. Nie akceptuj `github.com.evil.example` ani `objects.githubusercontent.com`.
3. Dodaj `compareUpdate(info, installedVersionCode, deviceSdk)` z wynikami `UpToDate`, `Available` i `RequiresNewerAndroid`. Starszy albo równy `versionCode` nigdy nie jest aktualizacją.
4. Dodaj `UpdateSource.fetch(maxBytes)` oraz `HttpUpdateSource`: GET, przekierowania, timeout połączenia i odczytu po 10 s, `Dispatchers.IO`, kod 200 i limit 64 KB odczytany jako najwyżej `maxBytes + 1`. `CancellationException` zawsze przechodzi dalej.
5. Dodaj `UpdateChecker` z wynikami `Checked`, `InvalidFile` i `NetworkError`, stały adres `https://github.com/r3tza/MAK/releases/latest/download/update.json` oraz `android.permission.INTERNET`. Nie podłączaj jeszcze Koin ani UI.
6. Testy JVM obejmują: nowszą, równą i starszą wersję; nowszą wymagającą wyższego SDK; nieznane pola; pustą nazwę; liczby niedodatnie; błędny JSON, URL i SHA-256; przekroczony rozmiar; kod HTTP inny niż 200; `IOException`; przekierowanie i anulowanie.

Sprawdzenie: `gradlew.bat test lintDebug`.

Kryterium zakończenia: I-36 ma status „gotowe”, testy przechodzą, lint nie ma nowych ostrzeżeń, a model, parser i porównanie nie importują Androida ani Compose.

## 2. Ekran „O aplikacji” i ręczne sprawdzanie (I-37)

Granica: osobna trasa, ekran i `UpdateViewModel`; nie dodawaj logiki aktualizacji do `SettingsViewModel` ani `TodayViewModel`.

1. Uzupełnij `FEATURES.md`, następnie dodaj `MakRoutes.SettingsAbout`, tytuł „O aplikacji” i przejście z sekcji ustawień. Wiersz na ekranie głównym pokazuje zainstalowaną wersję, a szczegóły są na osobnym ekranie.
2. Dodaj małą granicę odczytu informacji o zainstalowanej aplikacji. Implementacja Android odczytuje `versionName`, długi `versionCode`, nazwę pakietu i SDK urządzenia z systemu; testy ViewModelu dostają wartości jawnie.
3. `UpdateViewModel` obsługuje stany: bez wyniku, sprawdzanie, najnowsza wersja, dostępna wersja z notatkami, wersja wymagająca nowszego Androida, nieprawidłowy plik i błąd sieci. Ręczne „Sprawdź teraz” zawsze wykonuje nowe sprawdzenie i pokazuje błąd zamiast go ukrywać.
4. Przy prywatnym repo prawdziwe sprawdzenie może zakończyć się błędem sieci. Jest to oczekiwane do I-41; odbiór funkcji wykonuj przez fałszywy `UpdateSource`, bez tokenu i bez specjalnego trybu w kodzie produkcyjnym.
5. Dodaj rejestrację Koin, testy JVM stanów ViewModelu, test Compose przy 320 dp dla wszystkich komunikatów i `KoinGraphTest`. Sprawdź focus, TalkBack, motyw ciemny, długie notatki i ponowienie po błędzie.

Sprawdzenie: `gradlew.bat test lintDebug connectedDebugAndroidTest`.

Kryterium zakończenia: użytkownik może wejść na „O aplikacji”, odczytać wersję i uruchomić ręczne sprawdzenie; I-37 ma status „gotowe”, a zależność od prywatnego GitHuba jest jawnie odłożona do I-41.

## 3. Pobieranie i weryfikacja APK (I-38)

Granica: pobieranie pliku, weryfikacja czysta oraz adapter Android do metadanych APK. Instalacja systemowa nie należy do tego kroku.

1. Dodaj wstrzykiwalny `ApkDownloader`. Implementacja HTTP zapisuje do `cacheDir/updates/*.part`, raportuje liczbę pobranych bajtów, obsługuje anulowanie, timeouty i przekierowania, a po sukcesie atomowo zmienia nazwę na `.apk`.
2. Przed pobraniem użyj `Content-Length`, gdy jest dostępne, oraz `StatFs` z zapasem na zapis. Przy odpowiedzi bez długości kontroluj miejsce i liczbę bajtów podczas strumieniowania. Błąd, anulowanie i zamknięcie aplikacji nie mogą zostawić pliku `.part`.
3. Czyste funkcje obliczają SHA-256 i rozstrzygają: zgodność sumy, pakiet `dev.retza.mak`, `versionCode` wyższy od zainstalowanego oraz zgodność zestawu certyfikatów. Adapter Android odczytuje archiwum przez `getPackageArchiveInfo` z `GET_SIGNING_CERTIFICATES` i porównuje je z podpisem zainstalowanej aplikacji.
4. Rozszerz `UpdateViewModel` i ekran o „Pobierz”, postęp, „Anuluj” oraz krótkie błędy: brak miejsca, przerwane pobieranie, uszkodzony plik, obcy pakiet, starsza wersja i inny podpis. Plik niezgodny jest od razu usuwany.
5. Testy JVM używają plików tymczasowych i fałszywego downloadera. Test na emulatorze sprawdza odczyt pakietu, wersji i certyfikatu z APK, ale nie wymaga dostępu do prywatnego GitHuba.

Sprawdzenie: `gradlew.bat test lintDebug connectedDebugAndroidTest`.

Kryterium zakończenia: poprawny lokalny artefakt przechodzi wszystkie kontrole, każdy błąd usuwa plik częściowy albo niezgodny, a I-38 ma status „gotowe”.

## 4. Instalacja przez Androida (I-39)

Granica: adapter `PackageInstaller`, obsługa wyniku systemowego i zgody na nieznane aplikacje. Nie dodawaj własnego instalatora ani stałego procesu w tle.

1. Dodaj `REQUEST_INSTALL_PACKAGES` i granicę `UpdateInstaller`. Implementacja tworzy sesję `PackageInstaller`, kopiuje zweryfikowany APK, zatwierdza sesję i obsługuje `STATUS_PENDING_USER_ACTION`, powodzenie, odmowę, błąd i przerwanie.
2. Gdy `canRequestPackageInstalls()` jest fałszywe, ekran pokazuje jednozdaniowe wyjaśnienie i dopiero po akcji użytkownika otwiera `ACTION_MANAGE_UNKNOWN_APP_SOURCES` dla pakietu MAK. Po powrocie ponownie sprawdza zgodę; nie rozpoczyna instalacji bez niej.
3. Zapisz tylko minimalny stan potrzebny do obsługi aktywnej sesji. Po powodzeniu, błędzie albo anulowaniu usuń APK; osierocone pliki z poprzedniego procesu usuń przy następnym starcie.
4. Testy JVM obejmują przejścia stanów i mapowanie kodów wyniku, a testy Android obsługę zgody oraz kontrakt odbiornika. Na emulatorze zainstaluj lokalnie podpisane N, utwórz dane, zaktualizuj do N+1 tym samym kluczem i potwierdź zachowanie planu, notatek i ustawień. Pełny przebieg pobrania z GitHuba i instalacji z aplikacji pozostaje odbiorem I-41 po upublicznieniu repo.

Sprawdzenie: `gradlew.bat test lintDebug connectedDebugAndroidTest`, następnie ręczny scenariusz N do N+1 na emulatorze.

Kryterium zakończenia: kod obsługuje wszystkie wyniki instalatora, lokalna aktualizacja zachowuje dane, I-39 ma status „gotowe”, a odbiór telefonu O-07 pozostaje otwarty.

## 5. Automatyczne sprawdzanie i baner „Dzisiaj” (I-40)

Granica: osobne `UpdatePreferences` nad tym samym DataStore i współdzielony `UpdateCoordinator`; nie rozbudowuj `SettingsPreferences`, `AppViewModel` ani `TodayViewModel` o reguły aktualizacji.

1. `UpdatePreferences` przechowuje: domyślnie wyłączone automatyczne sprawdzanie, czas ostatniej automatycznej próby i pominięty `versionCode`. Zapisuj czas tuż przed próbą, także gdy kończy się brakiem sieci, aby kolejne uruchomienia nie ponawiały ruchu. Jeśli bieżący czas jest wcześniejszy od zapisanego, pozwól na jedną próbę i zastąp znacznik bieżącym czasem; następne uruchomienie znów czeka 24 godziny. Pokryj tę regułę testem z cofniętym zegarem.
2. `UpdateCoordinator` udostępnia jeden stan ręcznemu ekranowi i banerowi. Przy starcie aplikacji sprawdza tylko po wczytaniu preferencji, tylko gdy przełącznik jest włączony i minęły 24 godziny. Nie używa WorkManagera, alarmu ani powiadomienia.
3. Ekran „O aplikacji” dostaje przełącznik i tekst: aplikacja łączy się tylko z GitHubem, nie wysyła planu, a GitHub widzi adres IP; po wyłączeniu informuje, że nowe wersje nie pojawią się same. Ręczne sprawdzenie omija limit 24 godzin.
4. `TodayScreen` dostaje osobny stan banera przez trasę lub koordynator, bez dodawania sieci do `TodayViewModel`. Baner ma „Zobacz” i „Nie teraz”; pominięta wersja jest ukryta, ale każda wersja z wyższym `versionCode` pojawia się ponownie. Błąd automatyczny pozostaje niewidoczny.
5. Testy JVM obejmują funkcję 24 godzin, wyłączenie, błąd sieci, cofnięcie zegara i pominięcie wersji. Test Compose przy 320 dp obejmuje baner, długą nazwę wersji, obie akcje, motyw ciemny i TalkBack. `KoinGraphTest` potwierdza jeden koordynator i osobne preferencje.

Sprawdzenie: `gradlew.bat test lintDebug connectedDebugAndroidTest`.

Kryterium zakończenia: ręczne i automatyczne sprawdzanie używają jednego źródła stanu, automatyka jest domyślnie wyłączona i nie ponawia próby częściej niż raz na 24 godziny, a I-40 ma status „gotowe”.

## Po tych krokach

Poprawki prezentacyjne mogą powstawać przed, pomiędzy albo po I-36 do I-40, bez upubliczniania repozytorium. I-41 zaczyna się dopiero po ich zakończeniu i jawnym potwierdzeniu użytkownika: sprawdź historię repozytorium pod kątem sekretów i prywatnych plików, potwierdź weryfikację dwuetapową, zmień widoczność repozytorium na publiczną, utwórz i ręcznie opublikuj `v0.2.0`, zainstaluj ją na telefonie, potem opublikuj `v0.2.1` i wykonaj pełny scenariusz O-07 z pobraniem oraz instalacją z aplikacji. Szkic wydania nie jest widoczny pod `releases/latest`; test zaczyna się dopiero po ręcznej publikacji. Do tego czasu I-41 pozostaje zablokowane decyzją o gotowości prezentacyjnej, nie przez implementację I-36 do I-40. Po aktualizacjach wykonaj I-45 do I-47 (tryb tabletowy); otwarte odbiory pozostają w `FEATURES.md`.
