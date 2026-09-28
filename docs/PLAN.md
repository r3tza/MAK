# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

Kroki pochodzą z audytu interfejsu z 2026-09-28 (`LOG.md`). Odbiór na telefonie (O-07, I-40, I-49) wykonuje użytkownik; listy kontrolne są w odpowiednich wierszach `QUEUE.md`.

## Zasady wspólne dla wszystkich kroków

- Wykonuj kroki w podanej kolejności. Każdy krok kończy się kompilującym się kodem, zielonymi testami i osobnym commitem (`feat:` albo `fix:`).
- Przed zmianą przeczytaj wskazane pliki w całości. Numery linii w opisie są orientacyjne; szukaj po nazwie funkcji albo tekście.
- Nie zmieniaj niczego poza opisanym zakresem. Jeśli opis nie pasuje do kodu, zatrzymaj się i zapisz bloker w `QUEUE.md`.
- Testy JVM: `gradlew.bat test`. Testy Compose jednej klasy: `gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.today.TodayScreenTest"` (podmień nazwę klasy). Wymagają uruchomionego emulatora (`adb devices`).
- Zrzuty na emulatorze `Medium_Phone` (gęstość 420): `gradlew.bat installDebug`, potem `adb exec-out screencap -p > plik.png`. Szerokość 320 dp: `adb shell wm size 840x1866`, powrót: `adb shell wm size reset`. Motyw ciemny: `adb shell cmd uimode night yes`, powrót: `night no`. Dane demonstracyjne ma wersja debug.
- Po zmianie `docs/*.md` uruchom `python scripts/check_map.py`.
- Przed commitem wykonaj przegląd redukcyjny z `WORKFLOW.md`.

## 1. Odchudź górę ekranów „Dzisiaj” i „Plan” (I-56)

Cel: pierwsze zajęcia widać wyżej, a ekran nie powtarza informacji. Pliki: `ui/components/MakComponents.kt`, `ui/today/TodayScreen.kt`, `ui/schedule/ScheduleScreen.kt`, `ui/setup/SetupWizard.kt`, `ui/edit/ClassEditScreen.kt` i testy Compose tych ekranów (`androidTest/.../TodayScreenTest.kt`, `ScheduleScreenTest.kt`, `SetupWizardTest.kt`).

1. `MakSectionHeader`: usuń parametr `eyebrow` i gałąź rysującą nadtytuł. Zostaw górny odstęp `Spacer(Modifier.height(MakSpacing.sm))`. W wywołaniach usuń argument: `TodayScreen` (`"Dzisiaj"`), `SetupWizard` (`"Konfiguracja początkowa"`), `ClassEditScreen` (`null`).
2. `MakSummaryCard` zostaje bez zmian, razem z nagłówkiem „Twój plan na dziś” albo „Dziś bez zajęć” (decyzja użytkownika z 2026-09-28).
3. `MakRowTitle`: zmień `meta: String` na `meta: String? = null`. Tekst `meta` rysuj tylko, gdy nie jest pusty. W `TodayScreen` wywołaj `MakRowTitle(title = "Zajęcia")`. Pozostałe wywołania zostają.
4. `ScheduleScreen`: usuń `Text("Plan zajęć")`. `MakViewSwitch` dostaje `Modifier.padding(top = MakSpacing.sm, bottom = MakSpacing.xs)`.
5. `ScheduleFilterSection`: usuń stan `expanded`, `focused`, wiersz z ikoną `FilterList` i strzałką. Funkcja rysuje tylko `MakSelectField(label = "Kierunek", value = selectedLabel, options = filters, onSelected = { onFilterSelected(it.id) }, optionLabel = { labelById[it.id].orEmpty() }, modifier = Modifier.padding(bottom = MakSpacing.sm))`. Zostaw `distinctLabels`. W `DaySelector` zmień dolny odstęp z 17 dp na `MakSpacing.md`. Usuń nieużywane importy.
6. `WeekTypeBadge`: zmień tekst „Wybierz kierunek w filtrach, aby zmienić tydzień.” na „Wybierz kierunek w polu „Kierunek”, aby zmienić tydzień.”. Znajdź ten tekst w testach (`rg "w filtrach"`) i zaktualizuj.
7. `CalendarView`: usuń `MakExpandableSection` „opcje kalendarza” i stan `showCalendarOptions`. Pod legendą umieść `MakCheckbox(label = "Pokaż odwołane", ...)` z `Modifier.padding(bottom = MakSpacing.md)`. Stan pusty dnia ma tekst „Brak zajęć w tym dniu.”. Pod listą kart albo pod stanem pustym dodaj `MakSecondaryAction(text = "Dodaj jednorazowe", onClick = onAddOneOff, modifier = Modifier.padding(top = MakSpacing.md))`.
8. Testy Compose:
   - `TodayScreenTest`: nie istnieją teksty „DZISIAJ” i „Od najwcześniejszego”; nagłówek karty i etykiety „Zajęcia”, „Kolizje”, „Okienka” nadal są widoczne. Asercje nagłówka karty (`isHeading`) zostają.
   - `ScheduleScreenTest`: „Plan zajęć” nie istnieje; filtr to pole „Kierunek” z długą nazwą kierunku jako wartością (zastąp testy klikające „Filtry”; wzór wyboru opcji weź z testu pola motywu w `SettingsScreenTest`); w widoku kalendarza „Pokaż odwołane” i „Dodaj jednorazowe” są dostępne bez rozwijania.
   - `SetupWizardTest`: usuń asercję „KONFIGURACJA POCZĄTKOWA”.

Przypadki brzegowe: brak aktywnego semestru na „Dzisiaj” (karta podsumowania się nie pokazuje, stan pusty z „Skonfiguruj plan” zostaje); jeden kierunek (filtr pokazuje się tak samo jak dotąd, warunek `filters.isNotEmpty()` bez zmian); duża czcionka przy 320 dp.

Weryfikacja: `gradlew.bat test`, trzy klasy testów Compose, zrzuty „Dzisiaj”, „Plan” (lista i kalendarz) przy 320 dp w obu motywach. Przed zmianą przy 320 dp pierwsza karta listy „Plan” zaczynała się około 1310 px od góry zrzutu 840x1866.

Kryterium zakończenia: brak nadtytułów, podpisu „Od najwcześniejszego” i nagłówka „Plan zajęć”, a nagłówek karty podsumowania zostaje; filtr to jedno pole wyboru; opcje kalendarza nie są zwinięte; pierwsza karta listy „Plan” przy 320 dp zaczyna się co najmniej 70 px (około 27 dp) wyżej niż przed zmianą; testy przechodzą.

## 2. Oznacz zmiany w kalendarzu kształtem, nie kolorem (I-57)

Cel: kolor znacznika oznacza wyłącznie kierunek, więc nie kłóci się z kolorem wybranym przez użytkownika. Zmieniony lub przeniesiony termin ma pierścień zamiast wypełnionej kropki. Znacznik ma kontrast co najmniej 3:1 z tłem w obu motywach. Reguła jest w `ARCHITECTURE.md`, sekcja „Gęstość ekranu planu”, i w `FEATURES.md`, sekcja „Widok „Kalendarz””. Pliki: `ui/components/CourseColors.kt`, `ui/components/PresentationModels.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/schedule/ScheduleScreen.kt`, testy `test/.../ui/components/CourseColorsTest.kt` i `test/.../ui/schedule/ScheduleViewModelTest.kt`.

1. `CourseColors.kt`: wydziel pętlę z `courseTextColor` do `fun courseColorOn(color: Int, surface: Int, minContrast: Double): Int` (ta sama logika: kolor z wymaganym kontrastem zostaje, inaczej przyciemnianie na jasnym tle i rozjaśnianie na ciemnym). `courseTextColor(color, surface)` woła `courseColorOn(color, surface, 4.5)`. Dodaj `fun courseMarkerColor(color: Int, surface: Int): Int = courseColorOn(color, surface, 3.0)`. Zachowanie `courseTextColor` się nie zmienia.
2. `PresentationModels.kt`: w `CalendarMarkerUi` usuń `colorToken`, dodaj `val isChanged: Boolean = false`. Usuń `enum class CalendarMarkerColor`.
3. `ScheduleViewModel.kt`: przy budowie `markers` ustaw `isChanged = it.occurrenceChange != null` zamiast `colorToken`. Usuń funkcję `markerColor`. Odwołane terminy nie trafiają do `occurrences`, więc pierścień oznacza zmianę albo przeniesienie. Legenda (`calendarLegend`) zostaje bez zmian.
4. `ScheduleScreen.kt`:
   - usuń prywatną funkcję `markerColor(marker)` i importy `CalendarMarkerColor`, `MakOrangeMark`, `MakTeal`, jeśli przestaną być używane;
   - dodaj prywatną funkcję `@Composable courseMarkerTint(hex: String?): Color`: gdy `parseHexColor(hex)` zwraca `null`, zwróć `MaterialTheme.colorScheme.onSurfaceVariant`; w przeciwnym razie `Color(courseMarkerColor(parsed.toArgb(), MaterialTheme.colorScheme.background.toArgb()))`;
   - dodaj prywatny komponent `CalendarMarker(color: Color, changed: Boolean)` o rozmiarze 8 dp: gdy `changed == false` wypełnione koło (`background(color, CircleShape)`), gdy `true` pierścień (`border(1.5.dp, color, CircleShape)`) bez wypełnienia;
   - `CalendarDay`: wiersz znaczników ma wysokość 8 dp i odstęp 2 dp; każdy znacznik to `CalendarMarker(color = if (day.isSelected) onPrimary else courseMarkerTint(marker.colorHex), changed = marker.isChanged)`;
   - `CalendarLegend`: kierunki pokazują `CalendarMarker(courseMarkerTint(item.colorHex), changed = false)`, pozycja „Zmieniony termin” `CalendarMarker(MaterialTheme.colorScheme.onSurfaceVariant, changed = true)`.
5. Testy JVM:
   - `CourseColorsTest`: dla odcieni co 15 stopni i jasności 0 i 1 (`courseColorFrom`) oraz dla starego koloru spoza zakresu `#334FCE` wynik `courseMarkerColor` ma kontrast co najmniej 3,0 z `#FFFFFF` i z `#10192B`; kolor, który już ma 3:1, zostaje bez zmian;
   - `ScheduleViewModelTest`: dzień z terminem zmienionym przez `OccurrenceChange` ma znacznik z `isChanged = true`, zwykły termin `false` (wzór budowy danych weź z istniejącego testu kalendarza w tym pliku).

Przypadki brzegowe: kierunek bez koloru (neutralny znacznik); wybrany dzień (oba kształty w `onPrimary`); więcej niż trzy zajęcia w dniu (nadal najwyżej trzy znaczniki); 320 dp (trzy znaczniki po 8 dp i dwa odstępy mieszczą się w komórce 40 dp).

Weryfikacja: `gradlew.bat test`, `ScheduleScreenTest`, zrzuty kalendarza w obu motywach. W danych demonstracyjnych przeniesiony termin 1 października ma pierścień, a kropki kierunku „Zarządzanie” są wyraźnie widoczne w motywie ciemnym.

Kryterium zakończenia: w kalendarzu nie występuje kolor błędu ani inny kolor stanu; zmiana jest oznaczona pierścieniem w kolorze kierunku; legenda pokazuje pierścień przy „Zmieniony termin”; testy przechodzą.

## 3. Pokaż, z czym koliduje termin, i uprość szczegóły terminu (I-58)

Cel: karta i szczegóły terminu wskazują drugie zajęcia kolizji (`ARCHITECTURE.md`, „Kolizja nie jest winą użytkownika”); szczegóły nie pokazują oczywistego stanu; formularz zajęć ma tytuł tylko w górnym pasku. Pliki: `domain/CollisionRanges.kt`, `widget/WidgetPresenter.kt`, `ui/schedule/ScheduleCollisionLabels.kt`, `ui/PlanMapping.kt`, `ui/components/PresentationModels.kt`, `ui/components/MakComponents.kt`, `ui/today/TodayViewModel.kt`, `ui/schedule/ScheduleViewModel.kt`, `ui/occurrence/OccurrenceDetailsModels.kt`, `ui/occurrence/OccurrenceViewModel.kt`, `ui/occurrence/OccurrenceDetailsScreen.kt`, `ui/MakNavHostApp.kt`, `ui/edit/ClassEditScreen.kt`.

1. Domena: w `CollisionRanges.kt` dodaj `data class CollisionPartner(val start: LocalTime, val end: LocalTime, val otherName: String)` i `fun collisionPartners(collisions: Collection<Collision>): Map<String, List<CollisionPartner>>`. Przenieś tu logikę z `widgetConflicts` w `WidgetPresenter.kt`: każda kolizja dodaje wpis dla `first.id` (partner `second`) i `second.id` (partner `first`), bez duplikatów, sortowanie po początku, końcu i nazwie. `widgetConflicts` mapuje wynik `collisionPartners` na `WidgetConflictUi`; usuń z widgetu klasę `WidgetConflictCandidate` i funkcję `addConflict`. Test JVM `CollisionPartnersTest` w `test/.../domain`: symetria, sortowanie, brak duplikatów, dwie kolizje jednego terminu. Istniejące testy widgetu muszą przejść bez zmian.
2. `ScheduleCollisionLabels.kt`: dodaj `internal fun conflictPartnerNames(collisions: Collection<Collision>): Map<String, String>`: nazwy partnerów z `collisionPartners`, bez powtórzeń, złączone `", "`.
3. `ClassItemUi`: dodaj `val conflictWith: String? = null`. `PlannedOccurrence.toUi(conflictLabel: String?, conflictWith: String? = null)` ustawia pole. W `TodayViewModel` (jedno wywołanie) i `ScheduleViewModel` (dwa wywołania: `items` i `calendarItems`) policz mapę `conflictPartnerNames(...)` z tych samych kolizji co `conflictLabels` i przekaż `names[it.id]`.
4. `MakComponents.kt`: wydziel blok kolizji z `ClassCard` do `@Composable internal fun MakConflictNote(label: String, partners: String?, modifier: Modifier = Modifier)`: ten sam kontener (`tertiaryContainer`, zaokrąglenie 8 dp, padding 8 i 4 dp), w pierwszym wierszu ikona `WarningAmber` 16 dp i pogrubiona etykieta, pod nią, gdy `partners != null`, tekst „Z: {partners}” 12 sp w `onTertiaryContainer` z lewym wcięciem 20 dp (szerokość ikony i odstępu). Tekst zawija się, bez limitu linii. `ClassCard` używa `MakConflictNote(item.conflictLabel, item.conflictWith)`. `classCardDescription` dopisuje po etykiecie kolizji „z: {conflictWith}”, gdy jest.
5. Szczegóły terminu:
   - `OccurrenceDetailsUiState`: dodaj `conflictLabel: String? = null` i `conflictWith: String? = null`;
   - `OccurrenceViewModel.buildDetails`: policz raz `val plan = activePlan(data, effectiveDate)`, użyj go też dla `weekLabel`; `val occurrenceId = "$classId:$originalDate"` (format `PlannedOccurrence.id`); `conflictLabel = conflictLabels(plan.collisions)[occurrenceId]`, `conflictWith = conflictPartnerNames(plan.collisions)[occurrenceId]`;
   - `OccurrenceDetailsScreen`: pod `StatusSummary` pokaż `MakConflictNote`, gdy `conflictLabel != null`, z odstępem 12 dp pod spodem.
6. `StatusSummary`: nic nie rysuj, gdy `state.status == OccurrenceStatusUi.Scheduled` i `state.changeLines()` jest puste. Usuń import ikony `Event`, jeśli przestanie być używany.
7. `NotesBlock`: przyciski „Zapisz notatkę do zajęć” i „Zapisz notatkę do terminu” zmień z `MakPrimaryAction` na `MakSecondaryAction`. Jedyną akcją główną ekranu zostaje dolne „Zmień termin” albo „Przywróć termin”.
8. Tytuł formularza zajęć: w `MakApp` odczytaj stan formularza (`classEditViewModel.editor`, tę samą właściwość, której używa `classEditRoute` w `ClassOccurrenceRoutes.kt`) przez `collectAsStateWithLifecycle()`. Tytuł górnego paska to `editor.title` („Dodaj zajęcia” albo „Edytuj zajęcia”), gdy `currentRoute == MakRoutes.Edit`, w pozostałych przypadkach `titleForRoute(currentRoute)`. W `ClassEditScreen` zamień `MakSectionHeader` na `MakScreenIntro("Najpierw termin i przedmiot. Resztę możesz uzupełnić później.")`. „Anuluj” zostaje.
9. Testy: `CollisionPartnersTest` (JVM); `PlanMappingTest` z `conflictWith`; `OccurrenceViewModelTest`: dwa nakładające się terminy dają `conflictLabel` i nazwę drugiego w `conflictWith` (wzór danych weź z istniejących testów tej klasy); `OccurrenceDetailsScreenTest`: zwykły termin nie pokazuje „Zaplanowane”, termin z kolizją pokazuje etykietę i „Z: …”; `ScheduleScreenTest`: karta z `conflictWith` pokazuje „Z: …”, a opis karty zawiera „z: …”.

Przypadki brzegowe: termin kolidujący z dwoma zajęciami (obie nazwy w jednym wierszu, oddzielone przecinkiem); dwa terminy o tej samej nazwie (nazwa raz); termin przeniesiony na inny dzień (kolizje liczone dla daty docelowej); odwołany termin (brak kolizji); długa nazwa przy 320 dp (zawija się, bez obcięcia).

Weryfikacja: `gradlew.bat test`, testy Compose `OccurrenceDetailsScreenTest`, `ScheduleScreenTest`, `TodayScreenTest`; zrzuty karty z kolizją i szczegółów terminu przy 320 dp w obu motywach.

Kryterium zakończenia: karta, szczegóły terminu i widget biorą nazwy partnerów kolizji z jednej funkcji domenowej; szczegóły nie pokazują „Zaplanowane”; na ekranie szczegółów jest najwyżej jeden przycisk główny; tytuł formularza jest tylko w górnym pasku; testy przechodzą.

## 4. Rozdziel role komunikatów i uporządkuj ustawienia (I-54)

Cel: kolor komunikatu ma stałe znaczenie, komunikaty nie powtarzają tytułów, a ustawienia pokazują wartości zamiast opisów. Pliki: `ui/components/MakComponents.kt`, `ui/today/TodayScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/UpdateSettingsUi.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/UpdateScreen.kt`, `ui/settings/AboutScreen.kt`, `ui/semester/SemesterScreen.kt`, `ui/setup/SetupWizard.kt`, `ui/PlanMapping.kt`, `ui/edit/ClassEditScreen.kt`.

1. Odmiana liczebników: utwórz `ui/PolishPlural.kt` z `internal fun polishPlural(count: Int, one: String, few: String, many: String): String` (1 daje `one`; ostatnia cyfra 2 do 4 poza 12 do 14 daje `few`; reszta `many`). Test JVM `PolishPluralTest`: 1, 2, 4, 5, 12, 14, 21, 22, 25, 112, 0. Zastąp nią prywatną kopię w `ClassEditScreen.kt`, obie funkcje `classCountLabel` (`PlanMapping.kt` i `SettingsViewModel.kt`, druga ma używać pierwszej) oraz tekst `"... kierunków"` w `SettingsViewModel` (formy: kierunek, kierunki, kierunków). W `SettingsViewModel` etykieta `firstWeekLabel` aktywnego semestru zaczyna się małą literą („pierwszy tydzień A”); w `SettingsScreen` usuń wtedy `replaceFirstChar`.
2. `MakNoteBanner`: dodaj `enum class MakNoteRole { Neutral, Warning, Error }` i wymagany parametr `role` (bez wartości domyślnej, aby każde użycie było świadomie przypisane). `title` staje się `String?`. Wygląd:
   - `Neutral`: tło `surfaceContainerLow`, obramowanie 1 dp `outlineVariant`, tytuł `onSurface`, treść `onSurfaceVariant`, bez ikony;
   - `Warning`: tło `tertiaryContainer`, tekst `onTertiaryContainer`, ikona `WarningAmber` 20 dp przed tekstem;
   - `Error`: tło `errorContainer`, tekst `onErrorContainer`, ikona `ErrorOutline` 20 dp.
   Ikona nie ma własnego opisu; wiersz ma `semantics(mergeDescendants = true)` z opisem zaczynającym się od „Ostrzeżenie: ” albo „Błąd: ” dla tych ról.
3. Przypisz każde użycie dokładnie tak:
   - `TodayScreen`, „Dostępna aktualizacja”: `Neutral`; pod banerem zamiast dwóch przycisków pełnej szerokości `Row` wyrównany do prawej z `MakTextAction("Nie teraz")` i `MakTextAction("Zobacz")`;
   - `SettingsSemestersScreen`, baner „Semestry”: usuń;
   - `SettingsNotificationsScreen`, „Zablokowane przez system”: `Warning`;
   - `SettingsNotificationsScreen`, „Czas dostarczenia”: usuń baner; `MakHelperText(state.notificationsDetails)` tylko przy włączonych powiadomieniach;
   - `SettingsDataScreen`: kolejność: „Eksportuj plan do JSON”, baner `Warning` bez tytułu z treścią „Import zastępuje wszystkie lokalne dane. Tej operacji nie można cofnąć.”, „Importuj plan z JSON”;
   - `ImportPreviewScreen`: `Warning`, tytuł „Zastąpisz wszystkie lokalne dane”, treść „Tej operacji nie można cofnąć.”;
   - `SemesterScreen`, „Różne kalendarze”: `Neutral`;
   - `CoursesBlock`, „Kierunek w semestrze”: usuń baner; w jego miejscu nagłówek `Text("Dodaj kierunek", style = titleMedium, fontWeight = SemiBold)` z `semantics { heading() }`;
   - `WeekOverrideForm`: usuń baner; nagłówek `Text(if (state.isEditing) "Edytuj korektę" else "Dodaj korektę")` w stylu `titleSmall`, pogrubiony, z `heading()`;
   - `AboutScreen`, „Wersja przed pełnym wydaniem”: `Warning`;
   - `UpdateScreen`, „Wymagana zgoda”: `Neutral`;
   - `UpdateScreen.DownloadError`: `Error`, bez tytułu;
   - `SetupWizard`, „Kierunek: …” i „Semestr zapisany”: `Neutral`.
4. Ekran „Powiadomienia”: zamień każdy `NotificationToggle` (dwie opcje radio) na wiersz z przełącznikiem, zgodnie z `FEATURES.md`. Użyj `SettingsListSection` i `SettingsSwitchRow`; parametr `details` w `SettingsSwitchRow` zmień na `String? = null`. Sekcje: „Kolizje w planie” z wierszem „Powiadomienia o kolizjach” (przy włączaniu przy blokadzie zachowaj wywołanie `onRequestNotificationPermission`), „Dzień wcześniej” z wierszem „Powiadomienie wieczorne” i polem godziny w `SettingsFieldItem`, „Przed zajęciami” z wierszem „Powiadomienie przed zajęciami” i polem wyprzedzenia. Usuń `NotificationToggle` i nieużywany `SettingsSection`.
5. Główne ustawienia: w `UpdateSettingsUi.kt` stan `Idle` ma pusty `checkSummary` (wiersz pokazuje sam tytuł); wiersze „Kierunki” oraz „Kopia zapasowa i import” mają pustą wartość; między wierszem „Kierunki” a polem „Próg okienka” dodaj `SettingsRowDivider()`. `SettingsRowText` już pomija puste linie.
6. Ekran „Semestry”: w `SemesterRow` usuń `StatusText("Aktywny")`; aktywny semestr pokazuje pole wyboru nad listą. Opis wiersza to np. „24 sie - 31 sty, pierwszy tydzień A, 2 kierunki, 5 zajęć”.
7. Konfiguracja semestru: `SemesterNavigationRow` pokazuje pod tytułem tylko liczbę z odmianą: „2 kierunki”, „1 korekta” (korekta, korekty, korekt), „2 kalendarze” (kalendarz, kalendarze, kalendarzy). Usuń parametr `description`. Przenieś `settingsRowFocus` z `SettingsScreen.kt` do `ui/components` jako `internal fun Modifier.makRowFocus()` i użyj go w wierszach ustawień oraz w `SemesterNavigationRow` (przed `clickable`, które dostaje `role = Role.Button`). Między wierszami nawigacji dodaj `HorizontalDivider` w `outlineVariant`.
8. Testy Compose: nowy `MakNoteBannerTest` w `androidTest/.../ui/components` sprawdza w obu motywach, że trzy role mają różne kolory tła i że `Warning` oraz `Error` mają opis z prefiksem. Zaktualizuj `SettingsScreenTest` (asercje „Aktywny”, „Czas dostarczenia”, banera „Semestry”, radia „Włączone”), `TodayScreenTest` (akcje banera aktualizacji), `SemesterScreenTest`, `UpdateScreenTest`, `AboutScreenTest`, `SetupWizardTest`. Wyszukuj teksty przez `rg`.

Przypadki brzegowe: powiadomienia włączone przy blokadzie systemowej (ostrzeżenie i akcja „Otwórz ustawienia aplikacji” zostają); import z błędem (`importErrorMessage` nadal w kolorze błędu); brak semestrów na ekranie „Semestry”; długie teksty komunikatów przy 320 dp.

Weryfikacja: `gradlew.bat test`, wymienione testy Compose, zrzuty ekranów „Powiadomienia”, „Dane”, „Semestry”, konfiguracji semestru i „Dzisiaj” z banerem przy 320 dp w obu motywach.

Kryterium zakończenia: `MakNoteBanner` nie używa `primaryContainer`; każde użycie ma rolę z listy; żaden komunikat nie powtarza tytułu paska ani nazwy wiersza; powiadomienia używają przełączników; odmiana liczebników ma jedno źródło z testem; testy przechodzą.

## 5. Ujednolić wzorce i usunąć drobne błędy (I-59)

Cel: te same czynności wyglądają tak samo, a dekoracyjne środki znikają. Obejmuje I-55. Wykonuj punkty po kolei, każdy może być osobnym commitem.

1. Karta zajęć (`ClassCard`, decyzja użytkownika z 2026-09-28: separator zostaje): usuń `.shadow(...)` (obramowanie zostaje). Między kolumną godzin a kolumną danych dodaj `VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)`. Wewnętrzny `Row` dostaje `.fillMaxHeight()`, aby separator sięgał od góry do dołu treści. Zmiana zamyka rozbieżność O-05 z `FEATURES.md`, sekcja „Struktura karty zajęć”.
2. `ViewSwitchButton`: usuń `.shadow(...)`; zaznaczenie pokazują tło i obramowanie.
3. Dialogi usuwania: przenieś `ConfirmDeletionDialog` z `SemesterScreen.kt` do `ui/components` jako `MakConfirmDeletionDialog(title, text, confirmLabel = "Usuń", isDeleting, onConfirm, onCancel)` (Material 3 `AlertDialog`, przyciski tekstowe, potwierdzenie w kolorze błędu). Użyj go w `SemesterScreen` (trzy miejsca), w `DeleteSemesterDialog` w `SettingsScreen.kt` i w dialogu „Usuń zajęcia” w `OccurrenceDetailsScreen.kt` (`confirmLabel = "Usuń zajęcia"`). Treść i tytuły zostają. `MakDialog` zostaje dla dialogów z polami.
4. `WeekOverrideForm`: kolejność przycisków „Anuluj”, potem przycisk zapisu, jak w pozostałych formularzach.
5. `CalendarCard` w `SemesterScreen.kt`: daty przez `asCalendarDate()` zamiast surowego ISO.
6. Nagłówek wybranego dnia w kalendarzu zaczyna się wielką literą, jak w liście („Poniedziałek, 28 września 2026”). Zmień formatowanie `calendarSelectedDayLabel` w `ScheduleViewModel` i test, który sprawdza ten tekst.
7. Kreator: pole „Pierwszy tydzień” pokazuje opcje „Tydzień A” i „Tydzień B” jak w konfiguracji semestru; do `onFirstWeekChanged` przekazuj ostatni znak opcji.
8. „O aplikacji” (I-55): usuń `AboutCard` wokół nagłówka aplikacji oraz wokół sekcji „Możliwości” i „Dane i prywatność”; zostaw nagłówki sekcji i odstęp 24 dp. Kartę zachowuje tylko lista „Ostatnie zmiany”. Punktory `BulletList` mają kolor `onSurfaceVariant`. W `UpdateScreen` zastąp `AboutSection` nagłówkiem `titleMedium` z `heading()` i treścią bez karty; usuń `AboutSection`, jeśli jest nieużywana.
9. Widget: godzina nadchodzących zajęć w `WidgetOccurrenceRow` ma kolor `onBackground` zamiast `primary`; kolor akcentu zostaje tylko dla „Teraz” i „Następne”.
10. Martwy kod: usuń `SettingsInfoRow`, wersje `MakIconButton` i `MakRoundButton` z parametrem `symbol`, `MakSummaryStartDark` i `MakSummaryEndDark`, pole `TodayUiState.showPlanAction`, parametr `onOpenPlan` ekranu `TodayScreen` (i jego przekazywanie w `TodayScheduleRoutes.kt`), pole `TodayUiState.emptyTitle` oraz parametr `emptyTitle` w `MakStateMessage`. Przed usunięciem sprawdź `rg`, że nic poza testami z tego nie korzysta, i popraw testy.

Weryfikacja: `gradlew.bat test`, pełny `gradlew.bat connectedDebugAndroidTest`, zrzuty „Dzisiaj”, „Plan”, szczegółów terminu, „Semestry”, konfiguracji semestru i „O aplikacji” przy 320 dp w obu motywach; widget na launcherze emulatora.

Kryterium zakończenia: karta zajęć ma separator pionowy i nie ma cienia; dialogi usuwania mają jeden wzorzec; formularze mają tę samą kolejność przycisków; „O aplikacji” ma kartę tylko przy historii wydań; martwy kod usunięty; testy przechodzą. Po tym kroku I-54 do I-59 mają status `odbiór otwarty` z odbiorem w ramach O-05.

Klasy szerokości i obrót tabletów (I-45) są następnym zadaniem po powyższym planie.
