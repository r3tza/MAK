# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Kroki 1 i 2 domykają audyt kodu z 2026-09-27 (I-26), a kroki 3 i 4 zaczynają aktualizacje w aplikacji (I-36). Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Przejścia z powiadomień, usunięte zajęcia i komunikat progu (I-26)

Punkty (1) do (3) z `QUEUE.md`. Każdy punkt to osobna, mała zmiana.

1. `MainActivity.onCreate`: obsłuż `EXTRA_OPEN_TODAY` i `EXTRA_OPEN_PLAN_DATE` tylko wtedy, gdy `savedInstanceState == null`. `onNewIntent` zostaje bez zmian. Po odtworzeniu aktywności przywrócony stos ekranów nie może zostać nadpisany przejściem z powiadomienia albo widgetu.
2. `OccurrenceViewModel.open`: gdy `buildDetails` zwraca `null` (zajęcia usunięte albo z innego semestru), ustaw w `OccurrenceDetailsUiState` nowe pole `notFound = true`. `OccurrenceDetailsScreen` pokazuje wtedy tylko `MakEmptyState("Nie znaleziono tych zajęć.")`. Menu górnego paska jest puste, bo `canCancelOccurrence`, `canEditBaseClass` i `canDeleteBaseClass` są `false`.
3. `SettingsViewModel.setGapThresholdMinutes`: nie używaj `saveNotifications`. Dodaj osobny zapis z flagą `isSavingGapThreshold` i komunikatem błędu „Nie udało się zapisać progu okienka.”.
4. Testy JVM: w `OccurrenceViewModelTest` otwarcie nieistniejącego `classId` daje `notFound = true`; w `SettingsViewModelTest` zmiana progu przy `preferences.failNextWrite = true` publikuje „Nie udało się zapisać progu okienka.”.

Przypadek brzegowy: trasa szczegółów otwarta ponownie po usunięciu zajęć pokazuje komunikat, a nie pusty ekran.

Sprawdzenie: `gradlew.bat test`. Na emulatorze otwórz aplikację z widgetu albo powiadomienia, przejdź na inny ekran i obróć urządzenie; aplikacja zostaje na bieżącym ekranie.

Kryterium zakończenia: nowe testy przechodzą, a obrót ekranu po wejściu z powiadomienia albo widgetu nie przenosi ponownie na „Plan” ani „Dzisiaj”.

## 2. Równoległe odświeżanie alarmów i osierocony kalendarz (I-26)

Punkty (4) i (5) z `QUEUE.md`.

1. `CollisionAlarmScheduler`: dodaj prywatny `Mutex` z `kotlinx.coroutines.sync`. `refresh` wykonuje całą treść w `mutex.withLock { }`. `cancelAll` zmień na `suspend fun` z tą samą blokadą i wydziel prywatne `cancelAllLocked()` wołane z wnętrza `refresh`, aby blokada nie była zakładana dwa razy. Popraw wywołania `cancelAll`, jeśli jakieś są poza klasą.
2. `RoomSemesterRepository.saveStudyProgramAssignment`: gdy istniejące przypisanie zmienia kalendarz, po `update` usuń poprzedni kalendarz, jeśli `calendars.countAssignments(previousCalendarId) == 0`, tak jak robi to `saveSetupConfiguration`.
3. Test urządzenia w `app/src/androidTest/java/dev/retza/mak/data/RoomPersistenceTest.kt`: przepięcie przypisania na nowy kalendarz usuwa nieużywany poprzedni kalendarz razem z jego korektami, a kalendarz używany przez inny kierunek zostaje. Punkt (5) dotyczy kodu Room, więc `FakeRepository` na JVM go nie sprawdza.
4. Test urządzenia `app/src/androidTest/java/dev/retza/mak/notifications/CollisionAlarmSchedulerTest.kt`: baza Room w pamięci z dwoma kolidującymi zajęciami w najbliższym tygodniu, `SettingsPreferences` zaimplementowane w teście z włączonymi powiadomieniami, stały `Clock`. Uruchom dwa `refresh()` równolegle (`async` na `Dispatchers.Default`) i sprawdź, że zbiór w `SharedPreferences` „collision_alarms” pod kluczem „scheduled_ids” równa się identyfikatorom z `CollisionNotificationPlanner.plan`. Na końcu wywołaj `cancelAll()`.

Przypadki brzegowe: `refresh` przy wyłączonych powiadomieniach woła `cancelAllLocked()` bez zakleszczenia; brak aktywnego semestru czyści alarmy.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: oba nowe testy urządzenia przechodzą, a I-26 ma w `QUEUE.md` status „gotowe”.

## 3. Model i reguły pliku `update.json` (I-36)

Czysty Kotlin w nowym pakiecie `dev.retza.mak.update`, bez sieci i bez Androida. Wymagania: `QUEUE.md` (I-36), `ARCHITECTURE.md` (punkt „Aktualizacje”) i `STACK.md` (sekcja „Wydania i licencja”).

1. `UpdateInfo` z `@Serializable`: `versionCode: Int`, `versionName: String`, `apkUrl: String`, `sha256: String`, `minSdk: Int`, `notes: String = ""`. Nieznane pola ignoruj (`Json { ignoreUnknownKeys = true }`).
2. `parseUpdateInfo(bytes: ByteArray): UpdateParseResult` z wariantami `Valid(UpdateInfo)` i `Invalid`. Plik jest `Invalid`, gdy JSON jest uszkodzony, `versionCode` albo `minSdk` nie są dodatnie, `versionName` jest pusta, `apkUrl` nie ma schematu `https` albo hosta dokładnie `github.com`, a `sha256` nie ma 64 znaków `0-9a-f` (wielkość liter bez znaczenia).
3. `compareUpdate(info: UpdateInfo, installedVersionCode: Int, deviceSdk: Int): UpdateAvailability` z wariantami `UpToDate` (ta sama albo starsza wersja), `Available(info)` i `RequiresNewerAndroid(minSdk)` (nowsza wersja, ale `minSdk > deviceSdk`).
4. Testy JVM `app/src/test/java/dev/retza/mak/update/UpdateInfoTest.kt`: nowsza, ta sama i starsza wersja; niezgodne `minSdk`; uszkodzony JSON; adres `http://`; obca domena (`github.com.evil.example`, `objects.githubusercontent.com`); ujemny `versionCode`; zła suma SHA-256.

Sprawdzenie: `gradlew.bat test`.

Kryterium zakończenia: testy przechodzą, a pakiet `update` nie importuje klas Androida.

## 4. Pobieranie `update.json` (I-36)

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

I-42 (logo i animacja startu) czeka na odbiór na urządzeniu. Następnie aktualizacje w kolejności z `QUEUE.md`: I-34 zaczyna się, gdy użytkownik wygeneruje klucz podpisu; I-35 po I-34 i upublicznieniu repozytorium; dalej I-37, I-38, I-39, I-40 i I-41. Przy przenoszeniu tych zadań do planu rozpisz je według kryteriów z `QUEUE.md` i punktu „Aktualizacje” w `ARCHITECTURE.md`. I-14 czeka na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
