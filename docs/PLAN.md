# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

Krok 1 realizuje ciemniejszą kartę podsumowania (I-63, decyzja z 2026-09-29). Kroki 2 do 4 realizują tryb tabletowy w wariancie A (I-45 do I-47, decyzja z 2026-09-27, `ARCHITECTURE.md`, sekcja „Praktyczne mobile-first”; wpis „Tryb tabletowy, wariant A” w `log_archive/2026.md`). Odbiór na telefonie i tablecie (O-05 do O-09, I-40, I-49) wykonuje użytkownik; listy kontrolne są w odpowiednich wierszach `QUEUE.md`.

## Zasady wspólne dla wszystkich kroków

- Krok 1 wykonaj na gałęzi `task/I-63-dark-summary-card` z osobnym pull requestem. Kroki 2 do 4 wykonaj po kolei na jednej gałęzi `task/I-45-tablet-mode` z jednym pull requestem. Obie gałęzie zaczynaj od aktualnego `origin/main`. Każdy krok kończy się kompilującym się kodem, zielonymi testami i osobnym commitem (`feat:`). Gałąź i pull request prowadź według `WORKFLOW.md`, sekcja „Gałąź i pull request”; tytuł pull requesta ma format tematu commita, z prefiksem.
- Przed zmianą przeczytaj wskazane pliki w całości. Numery linii są orientacyjne; szukaj po nazwie funkcji albo tekście.
- Nie zmieniaj niczego poza opisanym zakresem. Drobne różnice między opisem a kodem rozwiąż zgodnie z celem kroku i opisz w treści commita. Zatrzymaj się i zapisz bloker w `QUEUE.md` tylko wtedy, gdy nie da się ustalić zamierzonego zachowania.
- Układ telefonu (szerokość okna poniżej 600 dp) nie może się zmienić w krokach 2 do 4. Zrzuty „Dzisiaj”, „Planu”, szczegółów terminu i ustawień przy 411 dp i 320 dp przed zmianą i po niej muszą być takie same.
- Polecenia testów, zrzutów i symulacji dużego ekranu są w `STACK.md`, sekcja „Narzędzia” (między innymi `adb shell wm size` i `wm density`). Testy na urządzeniu czyszczą dane wersji debug, więc zrzuty „przed zmianą” rób przed testami na urządzeniu. Jeśli przebieg testów przerwie błąd emulatora (na przykład „Can't find service: package”), uruchom emulator ponownie i powtórz.
- Nowe testy mają wykrywać realny błąd (`STACK.md`, sekcja 5, „Sposób testowania”). W komentarzach kodu nie wpisuj identyfikatorów zadań. Po zmianie `docs/*.md` uruchom `py scripts/check_map.py`.

## 1. Przyciemnij kartę podsumowania w motywie ciemnym (I-63)

Cel: w motywie ciemnym karta „Twój plan na dziś” przestaje być najjaśniejszym elementem ekranu, a czerwona i zielona liczba mają kontrast co najmniej 4,5:1. Motyw jasny zostaje bez zmian. Wzór: makieta C w artefakcie „MAK: audyt interfejsu, przed i po”.

Pliki: `ui/theme/Color.kt`, `ui/components/MakComponents.kt` (`MakSummaryCard`, `SummaryAlert`, `SummaryOk`, `isDarkSurface`), nowy test `app/src/test/java/dev/retza/mak/ui/components/SummaryCardColorsTest.kt`, `ui/components/CourseColors.kt` (istniejąca funkcja `contrastRatio`, do przeczytania).

1. W `Color.kt` dodaj `MakSummaryStartDark = Color(0xFF2C3F94)` i `MakSummaryEndDark = Color(0xFF1C2A6A)`.
2. W `MakComponents.kt` dodaj `internal fun summaryCardGradient(dark: Boolean): List<Color>`: dla `false` zwraca `listOf(MakSummaryStart, MakSummaryEnd)`, dla `true` zwraca `listOf(MakSummaryStartDark, MakSummaryEndDark)`. Zmień `SummaryAlert` i `SummaryOk` z `private` na `internal`, aby test mógł je odczytać.
3. W `MakSummaryCard` odczytaj `val dark = isDarkSurface()`. Gradient bierz z `summaryCardGradient(dark)`. W motywie jasnym zostaw obecny `shadow(11.dp, ...)`. W motywie ciemnym nie rysuj cienia, tylko krawędź `border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))` w tym samym miejscu łańcucha modyfikatorów co `clip`. Treść, rozmiary i semantyka karty zostają bez zmian.
4. Test JVM `SummaryCardColorsTest` (kontrast licz przez `contrastRatio` z `CourseColors.kt`, na kolorach jako `Int` ARGB, na przykład `color.toArgb()`):
   - `lightThemeKeepsCurrentGradient`: `summaryCardGradient(false)` równa się `listOf(MakSummaryStart, MakSummaryEnd)`;
   - `darkGradientKeepsNumbersReadable`: dla obu kolorów `summaryCardGradient(true)` biały tekst ma co najmniej 9:1, a `SummaryAlert` i `SummaryOk` co najmniej 4,5:1.
5. Dokumentacja: w `QUEUE.md` zmień status I-63 na `odbiór otwarty` z opisem zrealizowanych zmian i testów; w `KNOWN_ISSUES.md` przenieś pozycję o karcie podsumowania z „Otwartych problemów” do „Wymagają odbioru na urządzeniu”.

Przypadki brzegowe: motyw systemowy ciemny i motyw ciemny wybrany w aplikacji przy jasnym systemie (oba mają dać ciemny wariant, bo `isDarkSurface` czyta motyw aplikacji); dzień bez zajęć („Dziś bez zajęć”, trzy zera); kolizje większe od zera (czerwona liczba).

Weryfikacja: `gradlew.bat test`, `TodayScreenTest` na emulatorze; zrzuty „Dzisiaj” przy 320 dp w motywie jasnym i ciemnym przed zmianą i po niej (jasny ma być identyczny).

Kryterium zakończenia: w motywie ciemnym karta ma gradient `#2C3F94` do `#1C2A6A` z jasną krawędzią i bez cienia; w motywie jasnym zrzut jest identyczny jak przed zmianą; dwa testy przechodzą.

## 2. Dodaj klasy szerokości okna i obrót na tabletach (I-45)

Cel: aplikacja zna klasę szerokości okna z jednego źródła, a urządzenie o najkrótszym boku od 600 dp może się obracać. Telefon zostaje w pionie. Ten krok nie zmienia jeszcze żadnego układu.

Pliki: nowy `ui/WindowWidth.kt`, `MainActivity.kt`, `AndroidManifest.xml` (do przeczytania, bez zmian), nowy test `app/src/test/java/dev/retza/mak/ui/WindowWidthTest.kt`, wzór odczytu okna: `useHorizontalTimePicker` i jego użycie w `ui/components/MakFormControls.kt`.

1. W `ui/WindowWidth.kt` dodaj:
   - `enum class MakWidthClass { Compact, Medium, Expanded }`;
   - `internal fun makWidthClassFor(width: Dp): MakWidthClass`: poniżej 600 dp `Compact`, od 600 dp do poniżej 840 dp `Medium`, od 840 dp `Expanded`;
   - `val LocalMakWidthClass = staticCompositionLocalOf { MakWidthClass.Compact }`;
   - `internal fun shouldLockPortrait(smallestScreenWidthDp: Int): Boolean = smallestScreenWidthDp < 600`;
   - stałe progów jako `private val` w tym pliku, bez powtarzania liczb 600 i 840 w innych miejscach.
2. W `MainActivity.onCreate`, przed `setContent`, ustaw `requestedOrientation`: `ActivityInfo.SCREEN_ORIENTATION_PORTRAIT`, gdy `shouldLockPortrait(resources.configuration.smallestScreenWidthDp)`, w przeciwnym razie `ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED`. Manifest zostaje z `screenOrientation="portrait"`, żeby telefon nie pokazał ani jednej klatki w poziomie.
3. W `setContent`, wewnątrz `MAKTheme`, odczytaj szerokość okna tak jak `MakFormControls` odczytuje wysokość: `with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }`, i udostępnij `CompositionLocalProvider(LocalMakWidthClass provides makWidthClassFor(width))` dla całej zawartości `MakLoadingGate`.
4. Test JVM `WindowWidthTest`:
   - `widthClassUsesMaterialBreakpoints`: 599 dp `Compact`, 600 dp `Medium`, 839 dp `Medium`, 840 dp `Expanded`;
   - `onlySmallDevicesStayInPortrait`: `shouldLockPortrait(599)` jest prawdą, `shouldLockPortrait(600)` fałszem.
5. Dokumentacja: w `ARCHITECTURE.md`, sekcja „Praktyczne mobile-first”, usuń zdanie „Do czasu ukończenia I-45 kod blokuje pion na wszystkich urządzeniach z Androidem 12 do 15.” i dopisz, że klasy szerokości liczy `makWidthClassFor` z szerokości okna, a ekrany czytają je z `LocalMakWidthClass`. W `FEATURES.md`, sekcja „Nawigacja”, zmień zdanie o orientacji na: „Telefon działa w orientacji pionowej, a urządzenie o najkrótszym boku od 600 dp w obu orientacjach (`ARCHITECTURE.md`, „Praktyczne mobile-first”).”

Przypadki brzegowe: telefon obrócony poziomo (układ zostaje pionowy); tablet w podzielonym ekranie o szerokości okna poniżej 600 dp (klasa `Compact`, choć urządzenie może się obracać); odtworzenie aktywności po obrocie zachowuje bieżący ekran i wpisane dane.

Weryfikacja: `gradlew.bat test`, pełny `gradlew.bat connectedDebugAndroidTest`. Na emulatorze wyłącz automatyczny obrót (`adb shell settings put system accelerometer_rotation 0`) i wymuś poziom (`adb shell settings put system user_rotation 1`): przy domyślnym rozmiarze aplikacja zostaje w pionie; po `adb shell wm size 1600x2560`, `adb shell wm density 320` i ponownym uruchomieniu aplikacji obraca się do poziomu. Na koniec `user_rotation 0`, `wm size reset` i `wm density reset`.

Kryterium zakończenia: progi są tylko w `WindowWidth.kt`; telefon zostaje w pionie, symulowany tablet się obraca; dwa testy przechodzą; wygląd bez zmian.

## 3. Dodaj boczny pasek nawigacji od 600 dp (I-46)

Cel: przy szerokości okna od 600 dp ekrany główne („Dzisiaj” i „Plan”) mają boczny pasek nawigacji zamiast dolnego. Pozycje, role i semantyka są takie same jak w dolnym pasku.

Pliki: `ui/components/MakComponents.kt` (`MakNavBar`, `MakNavButton`), `ui/MakNavHostApp.kt` (`MakApp`), `ui/WindowWidth.kt`, `test/.../ui/WindowWidthTest.kt`, `androidTest/.../ui/MakNavigationChromeTest.kt`.

1. W `WindowWidth.kt` dodaj `enum class MakNavigationLayout { BottomBar, Rail, None }` i `internal fun makNavigationLayout(widthClass: MakWidthClass, isRootRoute: Boolean): MakNavigationLayout`: poza ekranami głównymi `None`; na ekranach głównych `BottomBar` dla `Compact`, `Rail` dla `Medium` i `Expanded`.
2. W `MakComponents.kt` zmień `private fun RowScope.MakNavButton` na zwykłą `private fun MakNavButton` (szerokość przekazuje wywołujący przez `modifier`; w `MakNavBar` zostaje `Modifier.weight(1f)`). Dodaj `fun MakNavRail(todaySelected: Boolean, planSelected: Boolean, onToday: () -> Unit, onPlan: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier)`: `Column` o szerokości 88 dp i pełnej wysokości, tło `surface`, linia 1 dp w kolorze `outlineVariant` przy prawej krawędzi (rysowana jak górna linia w `MakNavBar`), odstęp 12 dp od góry, przyciski „Dzisiaj”, „Plan” i „Dodaj” jeden pod drugim z odstępem 12 dp, każdy o szerokości 72 dp. „Dodaj” ma rolę przycisku, pozostałe rolę karty, tak jak w dolnym pasku.
3. W `MakApp`:
   - odczytaj `val navigationLayout = makNavigationLayout(LocalMakWidthClass.current, isRoot)`;
   - wydziel obecną lambdę `onAdd` z `MakNavBar` do lokalnej `val onAddClass`, aby obie belki używały tej samej;
   - `bottomBar` pokazuje `MakNavBar` tylko przy `MakNavigationLayout.BottomBar`;
   - treść `Scaffold` owiń w `Row(Modifier.fillMaxSize().padding(padding))`: przy `MakNavigationLayout.Rail` najpierw `MakNavRail`, potem `NavHost` z `Modifier.weight(1f).fillMaxHeight()` zamiast dzisiejszego `Modifier.fillMaxSize().padding(padding)`.
4. Testy:
   - JVM w `WindowWidthTest`: `railReplacesBottomBarFromMediumOnRootScreens` (wszystkie trzy klasy na ekranie głównym i `None` poza nim);
   - Compose w `MakNavigationChromeTest`: `navRailKeepsRolesSelectionAndActions` przy szerokości 700 dp: „Dzisiaj” i „Plan” mają rolę `Tab`, zaznaczona pozycja ma `selected`, „Dodaj” ma rolę `Button`, kliknięcie każdej pozycji wywołuje właściwą akcję.

5. Dokumentacja: w `FEATURES.md`, sekcja „Dolny pasek”, dopisz, że od 600 dp szerokości okna te same pozycje są w bocznym pasku po lewej stronie, a dolnego paska nie ma.

Przypadki brzegowe: przejście z ekranu głównego do szczegółów terminu na tablecie (pasek znika, jest strzałka wstecz); obrót z 411 dp do szerokości tabletu i z powrotem przy otwartym „Planie” (ekran i wybrany tydzień zostają, bo `NavController` i ViewModele przetrwają odtworzenie); dostęp z klawiatury (każda pozycja paska to jeden przystanek focusu).

Weryfikacja: `gradlew.bat test`, `MakNavigationChromeTest` na emulatorze; zrzuty „Dzisiaj” i „Planu” przy 411, 700 i 800 dp (`wm size` i `wm density` według `STACK.md`) w motywie jasnym i ciemnym.

Kryterium zakończenia: przy 411 dp układ bez zmian; przy 700 i 800 dp boczny pasek na ekranach głównych i brak dolnego; oba testy przechodzą.

## 4. Ogranicz szerokość treści i rozłóż „Dzisiaj” na dwie kolumny (I-47)

Cel: od 600 dp treść ekranów ma najwyżej 640 dp i jest wyśrodkowana, „Dzisiaj” od 840 dp ma dwie kolumny (podsumowanie i lista) o łącznej szerokości najwyżej 1040 dp, a dialogi `MakDialog` mają najwyżej 560 dp i przewijają się w niskim oknie.

Pliki: `ui/WindowWidth.kt`, `ui/components/MakComponents.kt` (`MakScreenContent`, `MakDialog`), `ui/occurrence/OccurrenceDetailsScreen.kt` (`OccurrenceBottomActions`), `ui/today/TodayScreen.kt`, `ui/TodayScheduleRoutes.kt` (`todayRoute`), `androidTest/.../ui/components/` (nowy `MakAdaptiveLayoutTest.kt`), `androidTest/.../ui/today/TodayScreenTest.kt`.

1. W `WindowWidth.kt` dodaj `val MakContentMaxWidth = 640.dp`, `val MakTwoColumnMaxWidth = 1040.dp` i `val MakDialogMaxWidth = 560.dp`.
2. `MakScreenContent` dostaje parametr `maxWidth: Dp = MakContentMaxWidth`. Zewnętrzny `Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter)` przyjmuje `modifier` wywołującego (z przewijaniem albo `weight`), a wewnątrz dzisiejszy `Column` dostaje `Modifier.widthIn(max = maxWidth).fillMaxWidth()` przed obecnym `padding`. Dzięki temu przewijanie działa na całej szerokości ekranu, a treść jest wyśrodkowana. Wywołania ekranów się nie zmieniają.
3. `OccurrenceBottomActions` stoi poza `MakScreenContent`: owiń jego `Column` w `Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter)` i dodaj `widthIn(max = MakContentMaxWidth)`, aby przycisk „Zmień termin” miał szerokość treści.
4. `MakDialog`: do `Column` dodaj `widthIn(max = MakDialogMaxWidth)` i `verticalScroll(rememberScrollState())`. Szerokość i wygląd na telefonie zostają takie same.
5. `TodayScreen` dostaje parametr `twoColumns: Boolean = false`. `todayRoute` przekazuje `twoColumns = LocalMakWidthClass.current == MakWidthClass.Expanded`. Gdy `twoColumns && state.hasActiveSemester`:
   - `MakScreenContent(maxWidth = MakTwoColumnMaxWidth)`;
   - nagłówek daty i baner aktualizacji na pełnej szerokości, jak dziś;
   - pod nimi `Row` z odstępem 24 dp: lewa kolumna o szerokości 360 dp z kartą podsumowania (bez dolnego odstępu `MakSpacing.xl`), prawa `Modifier.weight(1f)` z nagłówkiem „Zajęcia” i listą albo stanem pustym;
   - w pozostałych przypadkach układ jest dzisiejszy, jednokolumnowy.
   Kolejność czytania przez TalkBack zostaje: data, podsumowanie, „Zajęcia”, karty.
6. Testy Compose:
   - w `MakAdaptiveLayoutTest`: `screenContentIsCenteredAndLimitedTo640Dp` (w oknie 1000 dp szerokość treści to 640 dp, odstępy po bokach równe z tolerancją 1 dp, a przy 411 dp treść ma pełną szerokość minus dzisiejszy padding); `dialogIsLimitedTo560DpAndScrolls` (w oknie 1000 x 400 dp szerokość dialogu nie przekracza 560 dp, a jego zawartość ma akcję przewijania);
   - w `TodayScreenTest`: `wideTodayShowsSummaryBesideClasses` (przy `twoColumns = true` i 1100 dp lewa krawędź pierwszej karty zajęć jest na prawo od prawej krawędzi karty podsumowania, a obie zaczynają się na podobnej wysokości); `wideTodayWithoutSemesterStaysSingleColumn`.
7. Dokumentacja: w `QUEUE.md` zmień status I-45, I-46 i I-47 na `odbiór otwarty` z opisem zmian, testów i zrzutów; odbiór należy do O-08, który obejmuje też układ poziomy wyboru godziny z I-44. W `FEATURES.md`, sekcja „Ekran „Dzisiaj””, dopisz jedno zdanie o dwóch kolumnach od 840 dp.

Przypadki brzegowe: tablet w pionie 800 dp (klasa `Medium`: jedna kolumna 640 dp); tablet w poziomie 1280 dp (dwie kolumny 1040 dp, wyśrodkowane); podzielony ekran poniżej 600 dp (układ telefonu); duża czcionka 2,0 w dwóch kolumnach (karta podsumowania nie ucina etykiet); dialog wyboru godziny w niskim oknie (zachowanie z I-44 bez zmian); dzień bez zajęć w dwóch kolumnach.

Weryfikacja: `gradlew.bat test`, pełny `gradlew.bat connectedDebugAndroidTest`; zrzuty przy 411, 700, 800 i 1280 dp (poziomo) w obu motywach: „Dzisiaj”, „Plan”, szczegóły terminu, ustawienia, dialog zmiany terminu; zrzut przy 411 dp i 320 dp identyczny z zrzutem sprzed kroku 2.

Kryterium zakończenia: od 600 dp treść ma najwyżej 640 dp i jest wyśrodkowana; od 840 dp „Dzisiaj” ma dwie kolumny do 1040 dp; dialog ma najwyżej 560 dp i przewija się; układ telefonu bez zmian; testy przechodzą; I-45 do I-47 mają status `odbiór otwarty`.
