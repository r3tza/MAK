# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Kolejność ustalił użytkownik 2026-09-27: najpierw aktualizacje w aplikacji, bo mechanizm aktualizacji musi być w pierwszej wersji, którą dostaną znajomi, a tryb tabletowy po nich. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Model i reguły pliku `update.json` (I-36)

Czysty Kotlin w nowym pakiecie `dev.retza.mak.update`, bez sieci i bez Androida. Wymagania: `QUEUE.md` (I-36), `ARCHITECTURE.md` (punkt „Aktualizacje”) i `STACK.md` (sekcja „Wydania i licencja”).

1. `UpdateInfo` z `@Serializable`: `versionCode: Int`, `versionName: String`, `apkUrl: String`, `sha256: String`, `minSdk: Int`, `notes: String = ""`. Nieznane pola ignoruj (`Json { ignoreUnknownKeys = true }`).
2. `parseUpdateInfo(bytes: ByteArray): UpdateParseResult` z wariantami `Valid(UpdateInfo)` i `Invalid`. Plik jest `Invalid`, gdy JSON jest uszkodzony, `versionCode` albo `minSdk` nie są dodatnie, `versionName` jest pusta, `apkUrl` nie ma schematu `https` albo hosta dokładnie `github.com`, a `sha256` nie ma 64 znaków `0-9a-f` (wielkość liter bez znaczenia).
3. `compareUpdate(info: UpdateInfo, installedVersionCode: Int, deviceSdk: Int): UpdateAvailability` z wariantami `UpToDate` (ta sama albo starsza wersja), `Available(info)` i `RequiresNewerAndroid(minSdk)` (nowsza wersja, ale `minSdk > deviceSdk`).
4. Testy JVM `app/src/test/java/dev/retza/mak/update/UpdateInfoTest.kt`: nowsza, ta sama i starsza wersja; niezgodne `minSdk`; uszkodzony JSON; adres `http://`; obca domena (`github.com.evil.example`, `objects.githubusercontent.com`); ujemny `versionCode`; zła suma SHA-256.

Sprawdzenie: `gradlew.bat test`.

Kryterium zakończenia: testy przechodzą, a pakiet `update` nie importuje klas Androida.

## 2. Pobieranie `update.json` (I-36)

1. `fun interface UpdateSource { suspend fun fetch(maxBytes: Int): ByteArray }`.
2. `HttpUpdateSource(url: URL)`: `HttpURLConnection` z metodą GET, `connectTimeout` i `readTimeout` po 10 s i `instanceFollowRedirects = true`, bo GitHub przekierowuje plik z wydania. Odczyt najwyżej `maxBytes + 1` bajtów jak w `readBackupFile`; przekroczenie limitu i kod HTTP inny niż 200 zgłaszają `IOException`. Praca na `Dispatchers.IO`.
3. Stałe w `update/UpdateConfig.kt`: `UPDATE_JSON_URL = "https://github.com/r3tza/MAK/releases/latest/download/update.json"` i `MAX_UPDATE_JSON_BYTES = 64 * 1024`.
4. `UpdateChecker(source: UpdateSource, installedVersionCode: Int, deviceSdk: Int)` z `suspend fun check(): UpdateCheckResult` i wynikami `Checked(UpdateAvailability)`, `InvalidFile` oraz `NetworkError`. `CancellationException` przechodzi dalej.
5. W `AndroidManifest.xml` dodaj `android.permission.INTERNET`.
6. Nie podłączaj jeszcze `UpdateChecker` do Koin ani do interfejsu; to zakres I-37.
7. Testy JVM `app/src/test/java/dev/retza/mak/update/UpdateCheckerTest.kt` z fałszywym `UpdateSource`: poprawny plik z nowszą wersją, plik za duży, `IOException`, uszkodzony plik.

Sprawdzenie: `gradlew.bat test lintDebug`.

Kryterium zakończenia: testy przechodzą, lint nie ma nowych ostrzeżeń, a I-36 ma w `QUEUE.md` status „gotowe”.

## Po tych krokach

Rozpisz I-37 (ekran „O aplikacji”), I-38 (pobieranie i weryfikacja APK), I-39 (instalacja) i I-40 (automatyczne sprawdzanie) według kryteriów z `QUEUE.md` i punktu „Aktualizacje” w `ARCHITECTURE.md`. I-34 (klucz podpisu i wersja release) jest gotowe; I-35 (workflow wydań) zaczyna się po upublicznieniu repozytorium; I-41 (pierwsze wydanie) na końcu. Potem tryb tabletowy: I-45, I-46 i I-47 z kryteriami w `QUEUE.md`; symulację dużego ekranu na emulatorze opisuje `STACK.md`. I-42 (logo i animacja startu) czeka na odbiór na urządzeniu, a I-14 na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
