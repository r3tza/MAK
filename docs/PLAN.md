# Plan

Gałąź `task/I-111-tomorrow-plan-and-ci` (decyzja użytkownika z 2026-10-21, `LOG.md`). Kroki wykonuj po kolei; każdy kończy się kompilującym się kodem, zielonymi testami i jednym commitem. Nie wypychaj gałęzi bez polecenia użytkownika.

Zasady dla wykonawcy:

- Windows: `gradlew.bat`, Python przez `py` (`STACK.md`, sekcja „Narzędzia”). Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py` (kod 0).
- Czytaj tylko pliki wymienione w kroku. Jeśli nazwa w kodzie różni się od planu, zachowaj cel kroku i opisz różnicę w commicie.
- Emulator: `Medium_Phone_2` z 2048 MiB (`Medium_Phone_A12` z `STACK.md` już nie istnieje, `KNOWN_ISSUES.md`). `connectedDebugAndroidTest` odinstalowuje aplikację; przed ręcznym sprawdzeniem uruchom `gradlew.bat installDebug`.
- Teksty dla użytkownika piszesz dokładnie tak, jak w planie. Bez znaków `·`, `—` i strzałek.
- Po kroku zaktualizuj wiersz zadania w `QUEUE.md` (status i krótkie „Zrobione: główne klasy i testy”).

## 1. Wybierz pokazywany dzień i pokaż jutro na „Dzisiaj” (I-111)

Cel: 2 godziny po końcu ostatnich dzisiejszych zajęć, a w dniu bez zajęć o 18:00, ekran „Dzisiaj” pokazuje plan następnego dnia kalendarzowego (`FEATURES.md`, sekcja „Plan na jutro”). Ustawienie jest polem `PlanDisplaySettings`, domyślnie włączonym; przełącznik w interfejsie dodaje krok 3.

Pliki:

- `app/src/main/java/dev/retza/mak/domain/PlanDisplaySettings.kt`
- nowy `app/src/main/java/dev/retza/mak/domain/TomorrowPlan.kt`
- `app/src/main/java/dev/retza/mak/settings/SettingsPreferences.kt`
- `app/src/main/java/dev/retza/mak/settings/DataStoreSettingsPreferences.kt`
- `app/src/test/java/dev/retza/mak/settings/InMemorySettingsPreferences.kt`
- `app/src/androidTest/java/dev/retza/mak/notifications/CollisionAlarmSchedulerTest.kt` (zawiera własną implementację `SettingsPreferences`)
- `app/src/main/java/dev/retza/mak/ui/today/TodayViewModel.kt`
- `app/src/main/java/dev/retza/mak/ui/today/TodayScreen.kt`
- nowy `app/src/test/java/dev/retza/mak/domain/TomorrowPlanTest.kt`
- `app/src/test/java/dev/retza/mak/ui/today/TodayViewModelTest.kt`

Zmiany:

1. W `PlanDisplaySettings` dodaj ostatnie pole `val showTomorrowInEvening: Boolean = true` z komentarzem `// "Plan na jutro wieczorem" (I-111); stored on this phone only.`
2. Utwórz `TomorrowPlan.kt` w pakiecie `dev.retza.mak.domain`:
   ```kotlin
   /** Classes show tomorrow this long after the last class of today (decision of 2026-10-21, I-111). */
   val TomorrowSwitchDelay: Duration = Duration.ofHours(2)

   /** A day without classes shows tomorrow from this time. */
   val NoClassesSwitchTime: LocalTime = LocalTime.of(18, 0)

   // Two hours after 22:00 is already the next day, so there is no switch.
   private val LastSwitchableClassEnd: LocalTime = LocalTime.of(22, 0)

   fun tomorrowSwitchTime(todayOccurrences: List<PlannedOccurrence>): LocalTime? {
       val lastEnd = todayOccurrences.maxOfOrNull { it.endTime } ?: return NoClassesSwitchTime
       return if (lastEnd.isBefore(LastSwitchableClassEnd)) lastEnd.plus(TomorrowSwitchDelay) else null
   }

   fun shownPlanDate(today: LocalDate, now: LocalTime, switchTime: LocalTime?, enabled: Boolean): LocalDate =
       if (enabled && switchTime != null && !now.isBefore(switchTime)) today.plusDays(1) else today
   ```
   Importy: `java.time.Duration`, `java.time.LocalDate`, `java.time.LocalTime`.
3. W `SettingsPreferences` dodaj `suspend fun setShowTomorrowInEvening(enabled: Boolean)` pod `setMinimumBreakMinutes`.
4. W `DataStoreSettingsPreferences`:
   - klucz `private val showTomorrowInEveningKey = booleanPreferencesKey("show_tomorrow_in_evening")` obok pozostałych kluczy;
   - w `planDisplay` pole `showTomorrowInEvening = stored[showTomorrowInEveningKey] ?: true`;
   - implementacja `setShowTomorrowInEvening` przez `dataStore.edit { preferences -> preferences[showTomorrowInEveningKey] = enabled }`.
5. W `InMemorySettingsPreferences` i w implementacji w `CollisionAlarmSchedulerTest.kt` dodaj `setShowTomorrowInEvening`, które kopiuje bieżące `PlanDisplaySettings` z nowym polem (wzoruj się na `setGapThresholdMinutes` w tym samym pliku).
6. W `TodayUiState` (`TodayScreen.kt`) dodaj pole `val isTomorrow: Boolean = false`.
7. W `TodayViewModel`:
   - zamień `private val date = MutableStateFlow(LocalDate.now(clock))` na `private val now = MutableStateFlow(LocalDateTime.now(clock))`, a w `refreshToday()` ustaw `now.value = LocalDateTime.now(clock)`; `combine` używa `now`;
   - `buildToday(inputs, now: LocalDateTime)`: `val today = now.toLocalDate()`, `val todayPlan = activePlanProvider.resolve(inputs, today)`, `val day = shownPlanDate(today, now.toLocalTime(), tomorrowSwitchTime(todayPlan.schedule.occurrences), inputs.display.showTomorrowInEvening)`, `val plan = if (day == today) todayPlan else activePlanProvider.resolve(inputs, day)`, `val isTomorrow = day != today`; dalej buduj stan z `plan` i `day` jak dotąd;
   - `dateLabel`: przy `isTomorrow` `"Jutro, " + day.format(todayTitleFormatter)` (bez wielkiej litery po przecinku), w przeciwnym razie jak dotąd;
   - ustaw `isTomorrow = isTomorrow` i `emptyMessage = if (isTomorrow) "Nie masz jutro zajęć." else "Nie masz dziś zajęć."`.
8. W `TodaySummary` (`TodayScreen.kt`) tytuł karty: `state.freeDay != null` daje „Dzień wolny”; przy `classCount == 0` „Jutro bez zajęć” albo „Dziś bez zajęć”; w pozostałych przypadkach „Twój plan na jutro” albo „Twój plan na dziś”, zależnie od `state.isTomorrow`.

Testy:

- `TomorrowPlanTest`: brak zajęć daje 18:00; ostatnie zajęcia do 15:30 dają 17:30; koniec 21:59 daje 23:59; koniec 22:00 daje `null`; `shownPlanDate` zwraca dziś przed momentem przełączenia, jutro dokładnie w nim i po nim, dziś przy `enabled = false` i przy `switchTime = null`. Wystąpienia buduj tak jak w `GapCounterTest.kt` albo `ActivePlanProviderTest.kt` (przeczytaj, jak tworzą `PlannedOccurrence`).
- `TodayViewModelTest` (przeczytaj `FakeRepository` i `MutableClock` w tym pliku): zegar przed końcem ostatnich zajęć plus 2 h pokazuje dziś; po `clock.advanceTo(...)` za ten moment i `refreshToday()` `dateLabel` zaczyna się od „Jutro, ”, `isTomorrow` jest `true`, a `items` to zajęcia jutra; przy `showTomorrowInEvening = false` (przez `setShowTomorrowInEvening(false)` w `InMemorySettingsPreferences`) w tym samym momencie nadal dziś.

Przypadki brzegowe: dzień bez zajęć i dzień wolny bez zajęć przełączają o 18:00; ukryte kierunki są już pominięte w `occurrences`, więc nie wpływają na moment; jutro poza semestrem pokazuje stan, jaki `buildToday` daje dla tej daty; po północy `today` to nowy dzień, więc ekran wraca do dzisiejszego planu.

Weryfikacja: `gradlew.bat testDebugUnitTest --tests "dev.retza.mak.domain.TomorrowPlanTest" --tests "dev.retza.mak.ui.today.TodayViewModelTest" --tests "dev.retza.mak.domain.ActivePlanProviderTest"` oraz `gradlew.bat compileDebugAndroidTestKotlin`.

Kryterium zakończenia: testy zielone, kompilacja testów Android bez błędów, commit `feat: show tomorrow on Today in the evening (I-111)`.

## 2. Pokaż jutro w widgecie i przełącz go alarmem (I-111)

Cel: widget pokazuje ten sam dzień co „Dzisiaj” i przełącza się jednym przybliżonym alarmem, bez pracy w tle (`ARCHITECTURE.md`, sekcja 7, „Zasady widgetu”; przeczytaj ją przed zmianą).

Pliki:

- `app/src/main/java/dev/retza/mak/widget/WidgetPresenter.kt`
- `app/src/main/java/dev/retza/mak/widget/WidgetUiState.kt`
- `app/src/main/java/dev/retza/mak/widget/WidgetPlanLoader.kt`
- `app/src/main/java/dev/retza/mak/widget/MakTodayWidget.kt`
- nowy `app/src/main/java/dev/retza/mak/widget/WidgetDaySwitch.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/dev/retza/mak/widget/WidgetPresenterTest.kt`
- `app/src/test/java/dev/retza/mak/widget/WidgetPlanLoaderTest.kt`

Zmiany:

1. `WidgetPresenter.present(date, semesterName, plan, now: LocalTime?, isTomorrow: Boolean = false)`: `dateLabel` przy `isTomorrow` to `"Jutro, " + widgetDateLabel(date)`; przy `now == null` każde wystąpienie ma `WidgetOccurrencePhase.Scheduled`, a `nextId` jest `null`. `EmptyDay` dostaje `isTomorrow`.
2. W `WidgetUiState.EmptyDay` dodaj `val isTomorrow: Boolean = false`. W `MakTodayWidget.kt` komunikat `EmptyDay` to „Jutro bez zajęć” przy `isTomorrow`, w przeciwnym razie „Brak zajęć na dziś”.
3. Utwórz `WidgetDaySwitch.kt`:
   ```kotlin
   /** Plans the single widget update at the moment it should show tomorrow; null cancels it. */
   fun interface WidgetDaySwitchScheduler {
       fun schedule(at: LocalDateTime?)
   }

   class AlarmWidgetDaySwitchScheduler(
       private val context: Context,
       private val clock: Clock
   ) : WidgetDaySwitchScheduler {
       override fun schedule(at: LocalDateTime?) {
           val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
           val pending = PendingIntent.getBroadcast(
               context,
               WIDGET_DAY_SWITCH_REQUEST_CODE,
               Intent(context, WidgetDaySwitchReceiver::class.java),
               PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
           )
           if (at == null) {
               alarmManager.cancel(pending)
           } else {
               // Inexact and without waking the phone: the widget is only seen on a lit screen.
               alarmManager.set(AlarmManager.RTC, at.atZone(clock.zone).toInstant().toEpochMilli(), pending)
           }
       }
   }

   class WidgetDaySwitchReceiver : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {
           val result = goAsync()
           CoroutineScope(Dispatchers.Default).launch {
               try {
                   MakTodayWidget().updateAll(context)
               } finally {
                   result.finish()
               }
           }
       }
   }

   private const val WIDGET_DAY_SWITCH_REQUEST_CODE = 7311
   ```
   Sprawdź, że kod 7311 nie jest użyty w `CollisionAlarmScheduler.kt`.
4. W `AndroidManifest.xml` obok innych odbiorników: `<receiver android:name=".widget.WidgetDaySwitchReceiver" android:exported="false" />`.
5. `WidgetPlanLoader` dostaje ostatni parametr `private val daySwitchScheduler: WidgetDaySwitchScheduler = WidgetDaySwitchScheduler {}`. W `present(inputs)`:
   - bez danych albo przy wszystkich kierunkach ukrytych wywołaj `daySwitchScheduler.schedule(null)` przed zwróceniem stanu;
   - `val now = LocalDateTime.now(clock)`, `val today = now.toLocalDate()`, plan dziś, `switchTime = tomorrowSwitchTime(...)`, `shown = shownPlanDate(today, now.toLocalTime(), switchTime, inputs.display.showTomorrowInEvening)`;
   - gdy `shown == today`: zaplanuj `today.atTime(switchTime)` tylko przy włączonym ustawieniu i `switchTime != null`, w przeciwnym razie `schedule(null)`; zwróć `presenter.present(today, name, planDziś, now.toLocalTime())`;
   - gdy `shown` to jutro: `schedule(null)` i `presenter.present(shown, name, activePlanProvider.resolve(inputs, shown), now = null, isTomorrow = true)`.
6. W `MakTodayWidget.provideGlance` przekaż `daySwitchScheduler = AlarmWidgetDaySwitchScheduler(context, koin.get())`.

Testy:

- `WidgetPresenterTest`: przy `isTomorrow = true` `dateLabel` zaczyna się od „Jutro, ”, a żadna pozycja nie ma fazy `Next`, `Current` ani `Past`; `EmptyDay` ma `isTomorrow`.
- `WidgetPlanLoaderTest` (przeczytaj, jak plik buduje loader i dane): rejestrujący `WidgetDaySwitchScheduler` (lista wywołań); przed momentem przełączenia stan dotyczy dziś, a scheduler dostał dzisiejszą datę z godziną końca ostatnich zajęć plus 2 h; zegar po tym momencie daje stan jutra i `schedule(null)`; przy wyłączonym ustawieniu `schedule(null)` i dziś.

Przypadki brzegowe: wiele instancji widgetu dzieli jeden alarm (ten sam `PendingIntent`); zmiana planu odświeża widget, a ten planuje alarm od nowa; działająca sesja Glance po `updateAll` tylko się przerysowuje, więc stan zmieni się przy następnej sesji albo okresowej aktualizacji (akceptowane, `FEATURES.md`).

Weryfikacja: `gradlew.bat testDebugUnitTest --tests "dev.retza.mak.widget.*"`, `gradlew.bat compileDebugAndroidTestKotlin`. Na emulatorze: `gradlew.bat installDebug`, widget na ekranie głównym, `adb shell settings put global auto_time 0`, `adb shell cmd alarm set-time <ms>` na chwilę po momencie przełączenia, odczekaj do minuty i zrób zrzut widgetu; potem `adb shell settings put global auto_time 1`.

Kryterium zakończenia: testy zielone, zrzut widgetu z nagłówkiem „Jutro, …”, commit `feat: show tomorrow on the widget in the evening (I-111)`.

## 3. Dodaj przełącznik „Plan na jutro wieczorem” (I-111)

Cel: przełącznik w sekcji „Wygląd” ustawień (`FEATURES.md`, „Ustawienia i dane”, punkt 2) i dokumentacja zadania.

Pliki:

- `app/src/main/java/dev/retza/mak/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/dev/retza/mak/ui/settings/SettingsScreen.kt`
- `app/src/main/java/dev/retza/mak/ui/SettingsRoutes.kt`
- `app/src/test/java/dev/retza/mak/ui/settings/SettingsViewModelTest.kt`
- `app/src/androidTest/java/dev/retza/mak/ui/settings/SettingsScreenTest.kt`
- `docs/QUEUE.md`, `docs/KNOWN_ISSUES.md`

Zmiany:

1. `SettingsViewModel`: `PreferencesSnapshot` dostaje `showTomorrowInEvening` z `display.showTomorrowInEvening`; stan widoku ustawień (klasa z polem motywu, znajdź ją przez `theme` w `SettingsScreen.kt`) dostaje `val showTomorrowInEvening: Boolean = true`; nowa funkcja `fun setShowTomorrowInEvening(enabled: Boolean)` zapisuje przez `viewModelScope.launchUiOperation(feedbackSink = feedbackSink, errorMessage = "Nie udało się zapisać ustawienia.") { preferences.setShowTomorrowInEvening(enabled) }` (wzór: `setGapThresholdMinutes`, bez flagi zapisu).
2. `SettingsScreen`: nowy parametr `onShowTomorrowChanged: (Boolean) -> Unit = {}`; w `SettingsListSection("Wygląd")` pod polem „Motyw” wiersz `SettingsSwitchRow(title = "Plan na jutro wieczorem", checked = state.showTomorrowInEvening, onCheckedChange = onShowTomorrowChanged)` bez opisu.
3. `SettingsRoutes.kt`: `onShowTomorrowChanged = settingsViewModel::setShowTomorrowInEvening`.
4. `QUEUE.md`: I-111 „odbiór otwarty” z krótkim „Zrobione” i „Pozostaje sprawdzenie na telefonie użytkownika: przełączenie po zajęciach i o 18:00, widget, wyłączenie przełącznikiem.”. `KNOWN_ISSUES.md`, „Wymagają odbioru na urządzeniu”: dopisz I-111 do punktu 3.

Testy: `SettingsViewModelTest` sprawdza, że `setShowTomorrowInEvening(false)` zmienia stan na `false` i zapisuje w `InMemorySettingsPreferences`; `SettingsScreenTest` sprawdza, że wiersz „Plan na jutro wieczorem” jest widoczny i kliknięcie wywołuje `onShowTomorrowChanged(false)` przy stanie `true`.

Przypadki brzegowe: mały ekran 320 dp i skala czcionki 2,0 (tytuł zawija się, przełącznik zostaje widoczny); TalkBack czyta wiersz jako przełącznik (`SettingsSwitchRow` ma już `Role.Switch`).

Weryfikacja: `gradlew.bat testDebugUnitTest --tests "dev.retza.mak.ui.settings.SettingsViewModelTest"`, na emulatorze `gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.settings.SettingsScreenTest"`, `py scripts/check_map.py`, zrzut ustawień przy 411 i 320 dp.

Kryterium zakończenia: testy zielone, zrzuty zrobione, commit `feat: add the tomorrow plan switch to settings (I-111)` razem z dokumentacją.

## 4. Popraw drobiazgi I-109 i włącz Android Lint w CI (I-105)

Cel: kryteria I-109 i I-105 z `QUEUE.md`.

Pliki: `app/src/main/java/dev/retza/mak/ui/components/MakCourseColorPicker.kt` (linia z `parseCourseHex(DefaultCourseColor)!!`), `app/src/main/java/dev/retza/mak/ui/settings/SyncDifferenceLabels.kt` (linia `onPhone ?: onDrive!!`), `app/src/main/java/dev/retza/mak/ui/theme/Theme.kt`, pliki w `app/src/androidTest` z `MAKTheme(dynamicColor = false)` (około 20), `app/build.gradle.kts`, `.github/workflows/checks.yml`, `docs/STACK.md`, `docs/LOG.md`, `docs/QUEUE.md`.

Zmiany:

1. `MakCourseColorPicker.kt`: `checkNotNull(parseCourseHex(DefaultCourseColor)) { "DefaultCourseColor must be a valid #RRGGBB color" }`.
2. `SyncDifferenceLabels.kt`: `onPhone ?: checkNotNull(onDrive) { "A difference has a value on the phone or on Drive" }`.
3. `Theme.kt`: usuń parametr `dynamicColor` z `MAKTheme` (z adnotacją `@Suppress`). W `app/src/androidTest` zamień `MAKTheme(dynamicColor = false)` na `MAKTheme()`, na przykład `rg -l "MAKTheme\(dynamicColor = false\)" app/src/androidTest` i edycją każdego pliku.
4. `app/build.gradle.kts`, wewnątrz bloku `android { }`:
   ```kotlin
   lint {
       baseline = file("lint-baseline.xml")
       abortOnError = true
   }
   ```
   Uruchom `gradlew.bat updateLintBaseline`, potem `gradlew.bat lintDebug` (musi przejść). Plik `app/lint-baseline.xml` trafia do commitu.
5. `checks.yml`: po kroku „Run JVM tests” dodaj
   ```yaml
         - name: Run Android Lint
           run: bash ./gradlew lintDebug
   ```
6. `STACK.md`: w sekcji 5 zdanie, że `lintDebug` z bazą `app/lint-baseline.xml` przerywa budowę na nowych błędach, z poleceniem `gradlew.bat lintDebug`; w sekcji 6 przy opisie `checks.yml` dopisz krok lint. Wpis w `LOG.md` (agent na zlecenie użytkownika, I-105); przy 20 wpisach przenieś najstarszy (ostatni w pliku) na górę `docs/log_archive/2026.md`.

Testy i weryfikacja: `gradlew.bat testDebugUnitTest --tests "dev.retza.mak.ui.settings.SyncDifferenceLabelsTest"`, `gradlew.bat compileDebugAndroidTestKotlin`, `gradlew.bat lintDebug`, `py scripts/check_map.py`.

Przypadki brzegowe: jeśli `updateLintBaseline` nie istnieje w tej wersji AGP, usuń tymczasowo `abortOnError`, uruchom `gradlew.bat lintDebug` (utworzy bazę przy pierwszym uruchomieniu) i przywróć ustawienie.

Kryterium zakończenia: `lintDebug` przechodzi lokalnie, I-105 i I-109 „gotowe do odbioru” w `QUEUE.md` (odbiór: zielone CI na pull requeście), commit `chore: run Android Lint in CI and fix review leftovers (I-105, I-109)`.

## 5. Uruchamiaj testy Room w CI i skróć wiersze kolejki I-108 (I-106)

Cel: kryteria I-106 i I-108 z `QUEUE.md`.

Pliki: `.github/workflows/checks.yml`, `docs/STACK.md`, `docs/WORKFLOW.md`, `docs/LOG.md`, `docs/QUEUE.md`.

Zmiany:

1. `checks.yml`: w `permissions` dodaj `pull-requests: read`. Dodaj dwa zadania pod `checks`:
   ```yaml
     data-changes:
       name: data-changes
       runs-on: ubuntu-latest
       outputs:
         data: ${{ steps.filter.outputs.data }}
       steps:
         - uses: actions/checkout@v7
         - id: filter
           uses: dorny/paths-filter@v3
           with:
             filters: |
               data:
                 - 'app/src/main/java/dev/retza/mak/data/**'
                 - 'app/schemas/**'

     room-tests:
       name: room-tests
       needs: data-changes
       if: needs.data-changes.outputs.data == 'true'
       runs-on: ubuntu-latest
       steps:
         - name: Check out repository
           uses: actions/checkout@v7
         - name: Set up Java
           uses: actions/setup-java@v6
           with:
             distribution: temurin
             java-version: "17"
         - name: Set up Gradle
           uses: gradle/actions/setup-gradle@v6
         - name: Enable KVM
           run: |
             echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
             sudo udevadm control --reload-rules
             sudo udevadm trigger --name-match=kvm
         - name: Run Room tests on an emulator
           uses: reactivecircus/android-emulator-runner@v2
           with:
             api-level: 34
             arch: x86_64
             ram-size: 2048M
             script: ./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.data.RoomMigrationTest,dev.retza.mak.data.RoomPersistenceTest"
   ```
2. Lokalnie uruchom to samo polecenie na emulatorze (`gradlew.bat connectedDebugAndroidTest` z tym filtrem); obie klasy muszą przejść.
3. `STACK.md`, sekcja 5: testy `RoomMigrationTest` i `RoomPersistenceTest` uruchamia w CI zadanie `room-tests` tylko przy zmianach w `data/` i `app/schemas/`; sekcja 6: opis zadania. Wpis w `LOG.md` (I-106, decyzja użytkownika z 2026-10-20), z przeniesieniem najstarszego wpisu do archiwum.
4. I-108: w `QUEUE.md` skróć wiersze I-81 do I-111: zostaw zadanie, kryterium, źródło decyzji, nazwy głównych klas i testów oraz to, co pozostało do odbioru; usuń opisy kroków ze „Zrobione:” i „Sprawdzone na emulatorze” do jednego zdania z datą. Nie zmieniaj statusów. W `WORKFLOW.md`, w akapicie o aktualizacji wiersza `QUEUE.md` (sekcja z „Zaktualizuj jeden wiersz w `QUEUE.md`”), dopisz tę zasadę zwięzłości; wpis w `LOG.md`.

Testy i weryfikacja: YAML musi być poprawny (`py -c "import yaml,sys; yaml.safe_load(open('.github/workflows/checks.yml'))"`, jeśli PyYAML jest dostępny; w przeciwnym razie sprawdź wcięcia ręcznie), lokalne testy Room zielone, `py scripts/check_map.py` z kodem 0. Ostateczne sprawdzenie zadania `room-tests` następuje na pull requeście; zgłoś to użytkownikowi jako otwarty odbiór.

Przypadki brzegowe: pull request bez zmian w `data/` pomija `room-tests` (zadanie ze statusem „skipped” nie blokuje ochrony gałęzi, bo wymagane jest tylko `checks`); zmiana w `app/schemas/` bez zmian w kodzie też uruchamia testy.

Kryterium zakończenia: I-106 i I-108 zaktualizowane w `QUEUE.md`, commit `chore: run Room tests in CI and shorten queue rows (I-106, I-108)`. Po tym kroku zgłoś użytkownikowi gotowość gałęzi do wypchnięcia i pull requesta.
