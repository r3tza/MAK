# MAK: stos technologiczny

## 1. Status dokumentu

- Źródło prawdy dla narzędzi i wersji: ten plik.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-22.

## 2. Język i środowisko uruchomieniowe

- Kotlin 2.3.20.
- Android, `minSdk` 31, `targetSdk` 36 i `compileSdk` 37.2.
- Android Gradle Plugin 9.4.1 i Gradle 9.7.1.
- Compose BOM 2026.09.00, Material Icons Extended, Room 2.8.5, Lifecycle 2.11.0, Navigation 2.10.2, KSP 2.3.12, Glance 1.2.0, Core KTX 1.19.1, `kotlinx.coroutines` 1.11.0 i `kotlinx.serialization` 1.11.0.
- Stabilny zestaw zaktualizowano 2026-09-21, a 2026-09-26 podniesiono Core KTX do 1.19.1 i Navigation do 2.10.2. Kotlin 2.3.20 jest zweryfikowany przez Koin compiler plugin 1.2.1 i zgodny z KSP 2.3.12; linii 2.4.x nie użyto, bo KSP 2.3.12 jest zbudowany przeciw Kotlin 2.3.20. `compileSdk` podniesiono do 37.2, ponieważ nowe AndroidX wymagają API 37; `targetSdk` pozostaje 36, aby nie zmieniać zachowania działania.
- Deprecacje `androidx.compose.ui.test.junit4.createComposeRule` pozostają; migrację do `v2.createComposeRule` wykonamy osobno, ponieważ zmienia dyspozytor testów.

## 3. Warstwa aplikacji

- Jetpack Compose do budowy interfejsu.
- Jetpack Glance do widgetu.
- Koin 4.2.2 (BOM `io.insert-koin:koin-bom`) z Koin compiler plugin 1.2.1 (`io.insert-koin.compiler.plugin`) do składania zależności i ViewModeli. Obowiązuje constructor injection; ViewModele są pobierane wyłącznie na granicy hostów przez `viewModel()`/`koinViewModel`, a `get()` i `koinInject()` nie występują w ViewModelach, domenie ani komponentach ekranów. `Clock` i `ActivePlanProvider` są jednym `@Single` w grafie i nie mają domyślnych wartości w `OccurrenceViewModel`, `ScheduleViewModel`, `TodayViewModel` ani `WidgetPlanLoader`, więc brak bindingu zatrzymuje kompilację. Widget pobiera `SemesterRepository`, `ScheduleRepository`, `ActivePlanProvider` i `Clock` z Koin, bez rzutowania na `MakApplication`. Graf jest walidowany podczas kompilacji, a `KoinGraphTest` rozwiązuje zależności i wszystkie ViewModele na urządzeniu.
- Systemowe powiadomienia Androida planowane przybliżonymi alarmami `AlarmManager`. Aplikacja nie wymaga dokładnych alarmów ani ciągłego serwisu w tle.
- Lokalny font Inter.

## 4. Dane i przechowywanie

- Room jako warstwa dostępu do danych.
- SQLite jako lokalna baza danych.
- Preferences DataStore 1.2.1 do trwałych ustawień, w tym motywu.
- `kotlinx.serialization` do importu i eksportu JSON.
- Brak backendu, kont i synchronizacji sieciowej.

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

## 6. Narzędzia

- Android Studio i Gradle do budowania istniejącego projektu aplikacji.
- Git do historii zmian.
- Python 3 ze standardową biblioteką do kontroli dokumentacji: `scripts/check_map.py` sprawdza lokalne odnośniki, format `PLAN.md`, spójność `QUEUE.md` i limit `LOG.md`, a `scripts/test_check_map.py` (`unittest`) pokrywa jego przypadki. Skrypt nie używa sieci, nie uruchamia Gradle i nic nie zapisuje.
- Lokalna baza danych i pliki JSON bez usług zewnętrznych.
- Na Windowsie interpreter Pythona nazywa się zwykle `python`; polecenia zapisane z `python3` uruchamiaj wtedy przez `python`.
- Gradle wymaga Android SDK: pliku `local.properties` z `sdk.dir` poza repozytorium albo zmiennej `ANDROID_HOME`. Domyślna lokalizacja na Windowsie to `%LOCALAPPDATA%\Android\Sdk`.
- Testy Compose i Room uruchamia `gradlew.bat connectedDebugAndroidTest` na emulatorze albo urządzeniu. Sama kompilacja (`compileDebugAndroidTestKotlin`) nie zastępuje uruchomienia.
- Wersja release ma wyłączoną minifikację. Przed włączeniem `isMinifyEnabled` trzeba dodać reguły R8 dla `kotlinx.serialization` i klas eksportu JSON, inaczej import i eksport przestaną działać.

## 7. Środowisko

Repozytorium zawiera aplikację Android w wersji 0.1, konfigurację Gradle, lokalną bazę Room i testy. Aplikacja działa lokalnie na urządzeniu. Planowane testy zewnętrzne nie wymagają infrastruktury serwerowej.

## 8. Odrzucone alternatywy

- Backend i Firebase: odrzucone, ponieważ aplikacja ma działać całkowicie offline.
- Konta użytkowników i synchronizacja w chmurze: odrzucone, ponieważ zwiększyłyby zakres oraz wymagania dotyczące danych.
- Ciągły serwis w tle i odświeżanie widgetu co minutę: odrzucone z powodu zużycia baterii.
- Zewnętrzne CDN-y: odrzucone; aplikacja używa lokalnych zasobów.
