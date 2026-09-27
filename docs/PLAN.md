# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Kroki 1 do 4 wprowadzają tryb tabletowy w wariancie A (decyzja użytkownika z 2026-09-27, `ARCHITECTURE.md`, „Praktyczne mobile-first”), a krok 5 domyka audyt kodu (I-26). Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

**Duży ekran na emulatorze.** Bez tabletu symuluj go na emulatorze telefonu: `adb shell wm size 2560x1600` i `adb shell wm density 320` (najkrótszy bok 800 dp, poziomo), `adb shell wm size 1600x2560` (pionowo), a układ pośredni przez `adb shell wm size 1400x2000` z `wm density 320` (700 dp). Po sprawdzeniu zawsze przywróć `adb shell wm size reset` i `adb shell wm density reset`. Klasy szerokości: kompaktowa poniżej 600 dp, średnia od 600 do 839 dp, rozszerzona od 840 dp (progi Material 3).

## 1. Klasy szerokości i orientacja zależna od urządzenia (I-45)

1. Nowy plik `app/src/main/java/dev/retza/mak/ui/MakWindowSize.kt`: `enum class MakWidthClass { Compact, Medium, Expanded }`, czysta funkcja `widthClassFor(widthDp: Float): MakWidthClass` z progami 600 i 840 oraz `val LocalMakWidthClass = staticCompositionLocalOf { MakWidthClass.Compact }`. Nie dodawaj biblioteki `material3-adaptive`; progi liczy własna funkcja, bo aplikacja potrzebuje tylko szerokości.
2. W tym samym pliku czysta funkcja `lockToPortrait(smallestScreenWidthDp: Int): Boolean`, która zwraca `true` poniżej 600.
3. `MakApp` w `MakNavHostApp.kt`: owiń zawartość w `BoxWithConstraints` i przekaż `CompositionLocalProvider(LocalMakWidthClass provides widthClassFor(maxWidth.value))`. Klasa ma wynikać z szerokości okna, nie z typu urządzenia, więc działa też w podzielonym ekranie.
4. `AndroidManifest.xml`: zostaw `android:screenOrientation="portrait"` (telefon nie mignie w poziomie przy starcie). W `MainActivity.onCreate`, przed `setContent`, gdy `lockToPortrait(resources.configuration.smallestScreenWidthDp)` zwraca `false`, ustaw `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED`. Składany telefon po rozłożeniu odtwarza aktywność, więc warunek liczy się ponownie.
5. Test JVM `app/src/test/java/dev/retza/mak/ui/MakWindowSizeTest.kt`: 599,9 dp daje `Compact`, 600 dp `Medium`, 839,9 dp `Medium`, 840 dp `Expanded`; `lockToPortrait(599) == true`, `lockToPortrait(600) == false`.
6. Uaktualnij akapit o orientacji w `ARCHITECTURE.md` („Praktyczne mobile-first”) do stanu wdrożonego i zamknij I-43 odwołaniem do I-45 w `QUEUE.md`.

Przypadki brzegowe: telefon w trybie podzielonego ekranu zostaje w pionie; tablet w podzielonym ekranie o szerokości poniżej 600 dp dostaje układ kompaktowy.

Sprawdzenie: `gradlew.bat test`. Na emulatorze z symulacją 800 dp obróć ekran (`adb shell settings put system accelerometer_rotation 0` i `user_rotation 1`): aplikacja się obraca. Bez symulacji aplikacja zostaje w pionie. Przywróć `user_rotation 0`.

Kryterium zakończenia: testy przechodzą, a na emulatorze telefon zostaje w pionie, a symulowany tablet się obraca.

## 2. Boczny pasek nawigacji od 600 dp (I-46)

1. W `MakComponents.kt` dodaj `MakNavRail` z tymi samymi pozycjami, ikonami, rolami i semantyką co `MakNavBar` („Dzisiaj” i „Plan” jako `Role.Tab` z `selected`, „Dodaj” jako `Role.Button`). Użyj `MakNavButton` w pionowej kolumnie o szerokości 88 dp, tło `surface`, pionowa linia w kolorze `outlineVariant` po stronie treści, odstęp 8 dp między pozycjami, górny odstęp 12 dp. Pasek nie obsługuje insetów sam; dostaje je od `Scaffold` z `contentWindowInsets = WindowInsets.safeDrawing`.
2. `MakApp`: dla `LocalMakWidthClass.current != Compact` nie pokazuj `bottomBar`. Treść `Scaffold` ułóż w `Row`: najpierw `MakNavRail` (tylko gdy `isRoot`, jak dolny pasek), potem `NavHost` z `Modifier.weight(1f)`. `NavHost` ma zawsze tę samą pozycję w kompozycji, żeby zmiana warunku nie zerowała stosu ekranów.
3. Przejścia po kliknięciu pozycji używają tych samych funkcji co `MakNavBar` (`openRoot`, `addAction`).
4. Test Compose w `app/src/androidTest/java/dev/retza/mak/ui/MakNavigationChromeTest.kt`: `MakNavRail` przy wysokości 360 dp pokazuje trzy pozycje, „Plan” ma stan wybrania, „Dodaj” nie ma stanu wybrania, każda pozycja ma co najmniej 48 dp wysokości i jeden przystanek Tab (tryb klawiatury jak w `MakFormControlsTest`).
5. Opisz pasek boczny w `FEATURES.md`, sekcja „Nawigacja”.

Przypadki brzegowe: ekrany podrzędne (formularz, szczegóły, ustawienia) nie pokazują paska bocznego, tak jak dziś nie pokazują dolnego; w poziomie na telefonie ten krok nic nie zmienia, bo telefon jest w pionie.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`. Zrzuty z symulacją 800 dp w pionie i poziomie oraz z 700 dp: „Dzisiaj” i „Plan” mają pasek z lewej, dolnego paska nie ma; przy 411 dp (bez symulacji) jest dolny pasek.

Kryterium zakończenia: test przechodzi, a zrzuty pokazują właściwy pasek dla każdej szerokości bez utraty bieżącego ekranu po obrocie symulowanego tabletu.

## 3. Szerokość treści i dwie kolumny na „Dzisiaj” (I-47)

1. `MakScreenContent` w `MakComponents.kt`: dodaj parametr `maxWidth: Dp = MakContentMaxWidth` (640 dp). Zewnętrzny `Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter)` przyjmuje przekazany `modifier` (także `verticalScroll`), a wewnętrzna `Column` ma `widthIn(max = maxWidth).fillMaxWidth()` i dotychczasowy padding. Na telefonie nic się nie zmienia, bo ekran jest węższy niż 640 dp.
2. `TodayScreen`: przy `LocalMakWidthClass.current == Expanded` użyj `MakScreenContent(maxWidth = 1040.dp)` i `Row` z odstępem 24 dp. Lewa kolumna (`weight(0.42f)`): nagłówek z datą i karta podsumowania. Prawa (`weight(0.58f)`): `MakRowTitle` „Zajęcia” i lista kart albo stan pusty. Przy `Compact` i `Medium` układ zostaje jednokolumnowy.
3. „Plan”: zostaje jednokolumnowy z szerokością 640 dp; kalendarz miesiąca mieści wtedy komórki około 80 dp.
4. Dialogi `MakDialog`: dodaj `widthIn(max = 560.dp)` i `verticalScroll`, żeby długi dialog mieścił się w niskim oknie.
5. Testy Compose: w `TodayScreenTest` przy szerokości 1000 dp i `LocalMakWidthClass provides Expanded` karta podsumowania i pierwsza karta zajęć są widoczne, a lewa krawędź karty zajęć jest na prawo od prawej krawędzi karty podsumowania; w `SettingsScreenTest` przy szerokości 1000 dp szerokość sekcji „Plan” nie przekracza 640 dp.
6. Opisz szerokość treści i układ „Dzisiaj” w `FEATURES.md`.

Przypadki brzegowe: długa nazwa zajęć w prawej kolumnie zawija się; brak aktywnego semestru na szerokim ekranie pokazuje stan pusty w prawej kolumnie z akcją „Skonfiguruj plan”.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest` i zrzuty z symulacją 800 dp w pionie i poziomie dla „Dzisiaj”, „Planu”, formularza zajęć i ustawień.

Kryterium zakończenia: testy przechodzą, żadna linia tekstu formularza ani ustawień nie jest szersza niż 640 dp, a „Dzisiaj” w poziomie na tablecie pokazuje podsumowanie i zajęcia obok siebie.

## 4. Wybór daty i godziny po polsku i w niskim oknie (I-44)

1. `MakDatePickerField` w `MakFormControls.kt`: `rememberDatePickerState` dostaje `locale = Locale.forLanguageTag("pl-PL")`, co daje polskie miesiące, dni tygodnia i tydzień od poniedziałku. `DatePicker` dostaje `title = { Text("Wybierz datę", ...) }` i własny `headline` z wybraną datą w formacie `fullDateFormatter` albo „Nie wybrano daty”, bo domyślne teksty pochodzą z języka systemu. Tryb wpisywania ręcznego wyłącz (`showModeToggle = false`), bo jego domyślne komunikaty są po angielsku.
2. Sprawdź nagłówki dni tygodnia na emulatorze w języku angielskim. Jeśli po zmianie języka litery nadal na siebie nachodzą, sprawdź, czy przyczyną jest czcionka z `Type.kt` w stylu `DatePickerDefaults`; ustaw wtedy dla dni tygodnia styl `labelMedium` przez `DatePickerDefaults.colors` i typografię `MaterialTheme` w dialogu. Zapisz przyczynę w `LOG.md`.
3. `MakTimePickerField`: wysokość okna poniżej 560 dp (z `LocalWindowInfo.current.containerSize` przeliczonego na dp) daje `layoutType = TimePickerLayoutType.Horizontal` i `DialogProperties(usePlatformDefaultWidth = false)` z `widthIn(max = 640.dp)`; w pozostałych przypadkach `TimePickerLayoutType.Vertical`. Na telefonie w pionie nic się nie zmienia.
4. Test Compose w `MakFormControlsTest` na emulatorze z językiem angielskim: po otwarciu wyboru daty z wartością 2026-09-21 widać „Wybierz datę” i „wrzesień 2026”, nie ma „Select date”; wybór godziny w oknie o wysokości 400 dp pokazuje przyciski „Anuluj” i „Wybierz”.

Przypadki brzegowe: zakres `minDate` i `maxDate` działa jak dotąd; wybór godziny w 24-godzinnym formacie bez „AM/PM”.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`, zrzuty wyboru daty i godziny w pionie na telefonie oraz w poziomie z symulacją 800 dp i w podzielonym ekranie.

Kryterium zakończenia: testy przechodzą, a oba dialogi mieszczą się w całości i są po polsku przy angielskim języku telefonu.

## 5. Drobne poprawki z audytu kodu (I-26)

1. `MainActivity.onCreate`: obsłuż `EXTRA_OPEN_TODAY` i `EXTRA_OPEN_PLAN_DATE` tylko wtedy, gdy `savedInstanceState == null`. `onNewIntent` zostaje bez zmian.
2. `OccurrenceViewModel.open`: gdy `buildDetails` zwraca `null`, ustaw nowe pole `OccurrenceDetailsUiState.notFound = true`; `OccurrenceDetailsScreen` pokazuje wtedy tylko `MakEmptyState("Nie znaleziono tych zajęć.")`.
3. `SettingsViewModel.setGapThresholdMinutes`: osobny zapis z flagą `isSavingGapThreshold` i komunikatem „Nie udało się zapisać progu okienka.” zamiast `saveNotifications`.
4. `CollisionAlarmScheduler`: prywatny `Mutex`; `refresh` w `mutex.withLock { }`; `cancelAll` jako `suspend fun` z tą samą blokadą i prywatne `cancelAllLocked()` wołane z `refresh`.
5. `RoomSemesterRepository.saveStudyProgramAssignment`: po przepięciu istniejącego przypisania na inny kalendarz usuń poprzedni, jeśli `calendars.countAssignments(previousCalendarId) == 0`.
6. Testy JVM: `OccurrenceViewModelTest` (nieistniejący `classId` daje `notFound`), `SettingsViewModelTest` (błąd zapisu progu). Testy urządzenia: `RoomPersistenceTest` (przepięcie usuwa nieużywany kalendarz z korektami, a używany zostaje) i nowy `app/src/androidTest/java/dev/retza/mak/notifications/CollisionAlarmSchedulerTest.kt` (baza Room w pamięci z kolizją, `SettingsPreferences` z włączonymi powiadomieniami w teście, stały `Clock`, dwa równoległe `refresh()`, zbiór „scheduled_ids” w `SharedPreferences` „collision_alarms” równy identyfikatorom z `CollisionNotificationPlanner.plan`, na końcu `cancelAll()`).

Przypadki brzegowe: `refresh` przy wyłączonych powiadomieniach nie zakleszcza się; zmiana motywu w ustawieniach po wejściu z powiadomienia nie przenosi ponownie na „Plan”.

Sprawdzenie: `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: wszystkie nowe testy przechodzą, a I-26 ma w `QUEUE.md` status „gotowe”.

## Po tych krokach

Odbiór trybu tabletowego na prawdziwym tablecie albo emulatorze tabletu należy do O-08. Następnie I-36 (sprawdzanie wersji; kryteria i szczegóły w `QUEUE.md`), a dalej aktualizacje w kolejności z `QUEUE.md`: I-34 zaczyna się, gdy użytkownik wygeneruje klucz podpisu; I-35 po I-34 i upublicznieniu repozytorium; dalej I-37 do I-41. I-42 (logo i animacja startu) czeka na odbiór na urządzeniu, a I-14 na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
