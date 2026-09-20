# MAK — plan aplikacji

## 1. Cel i zakres

MAK (Mobilny Akademicki Kalendarz) to lekka aplikacja mobilna na Androida do zarządzania planem zajęć, szczególnie przy studiowaniu na dwóch lub większej liczbie kierunków.

Główne założenia:

- szybki podgląd zajęć na dziś;
- prosty plan tygodniowy;
- obsługa wielu kierunków;
- automatyczne tygodnie A/B z ręcznymi korektami;
- wykrywanie kolizji;
- widget na ekranie głównym;
- powiadomienia systemowe;
- działanie całkowicie offline;
- małe zużycie baterii;
- brak kont, logowania, backendu i synchronizacji w chmurze.

Po jednorazowym skonfigurowaniu planu użytkownik powinien korzystać głównie z ekranu „Dzisiaj” i widgetu.

## 1.1. Stan wdrożenia

Stan na 2026-09-19:

- Warstwa danych, kalkulator tygodni A/B, resolver planu, kolizje, Room i eksport JSON są zaimplementowane i objęte testami JVM.
- Kreator pierwszej konfiguracji, semestry, kierunki, zajęcia, notatki, zmiany pojedynczych wystąpień, ekran „Dzisiaj”, plan, kalendarz i ustawienia są dostępne w aplikacji Compose.
- Nawigacja korzysta z `NavHost` i back stacku. Ekrany podrzędne mają argumenty tras, tytuł topbara i przewidywalny powrót przez przycisk oraz systemowy back.
- Edge-to-edge korzysta z insetów Material 3. Topbar i dolna nawigacja uwzględniają bezpieczny obszar ekranu.
- Daty i godziny są wybierane przez pickery Material 3 z ograniczeniami zakresu, akcją semantyczną i przywracaniem focusu. Kolory kierunków wybiera się z nazwanej palety.
- Własne kontrolki mają minimalny obszar dotyku 48 dp, semantykę, widoczny focus i ikony Material. Odstępy ekranów korzystają z tokenów `MakSpacing`.
- Dodano testy tras, pickerów, topbara i układu dla szerokości 320 dp. Testy JVM oraz kompilacja testów Android przechodzą.
- Build debug zawiera bezpieczny seed demonstracyjny dla pustej bazy, aby można było od razu obejrzeć wszystkie główne stany interfejsu.
- Kod widgetu ma loader, presenter, stany puste i błędu, układ responsywny Glance oraz odświeżanie po zmianach bazy.

Pozostaje do wykonania:

- uruchomienie testów instrumentacyjnych i wizualna kontrola insetów na emulatorze lub urządzeniu;
- sprawdzenie TalkBacka, klawiatury, gestu wstecz i motywu ciemnego w rzeczywistym środowisku Androida;
- ręczna kontrola widgetu na launcherze: dodanie, zmiana rozmiaru, motywy, otwarcie aplikacji i odświeżenie po zmianie danych;
- import JSON zaplanowany na wersję 0.3.

## 1.2. Plan porządkowania architektury

Zmiany należy wprowadzać stopniowo podczas rozwoju wersji 0.2 i 0.3. Nie wymagają podziału projektu na osobne moduły Gradle ani dodania frameworka wstrzykiwania zależności.

Status: wspólny `ActivePlanProvider` oraz mapowanie Room poza ViewModelem są zaimplementowane. Pozostałe punkty realizować podczas zmian odpowiednich przepływów.

Kolejność prac:

1. Przed implementacją widgetu wydzielić wspólny `ActivePlanProvider`. Komponent przyjmuje dane domenowe i datę, wywołuje `ScheduleResolver` oraz w razie potrzeby `CollisionDetector`, a następnie zwraca aktywny plan. Mapowanie danych Room na modele domenowe przenieść z ViewModelu do wspólnej granicy danych. Kod nie może zależeć od Compose ani Glance. Ekran „Dzisiaj”, plan, kalendarz i widget mają korzystać z tej samej ścieżki obliczeń.
2. Ustawić `NavController` jako jedyne źródło bieżącej trasy. ViewModel może zgłaszać jednorazowy zamiar przejścia po zapisie, usunięciu albo zakończeniu konfiguracji, ale nie przechowuje kopii aktualnej trasy.
3. Rozdzielać `MakViewModel` według przepływów podczas zmian odpowiednich ekranów. Docelowy podział obejmuje `ScheduleViewModel`, `ClassEditViewModel`, `OccurrenceViewModel`, `SemesterViewModel`, `SetupViewModel` i `SettingsViewModel`. Stan nadrzędny może koordynować aktywny semestr i ustawienia wspólne, ale nie zawiera logiki formularzy poszczególnych ekranów.
4. Nie wystawiać typów Room w publicznym stanie UI. `SemesterWithData` i encje pozostają po stronie danych albo prywatnego składania stanu. Ekrany otrzymują modele prezentacyjne i identyfikatory potrzebne do akcji.
5. Zapisy obejmujące kilka rekordów wykonywać atomowo. Dotyczy to co najmniej zapisu zajęć z nowym prowadzącym, utworzenia semestru z pierwszym kierunkiem, zmiany aktywnego semestru po usunięciu oraz przyszłego importu. Import zapisuje cały plik albo nie zmienia bazy.
6. Wprowadzić wspólny stan operacji zapisu: `Saving`, `Saved`, `ValidationError` i `StorageError`. Błąd nie może zamknąć formularza ani usunąć wpisanych wartości. Komunikat wskazuje użytkownikowi pole albo operację, której dotyczy.
7. Zapisać trwałe preferencje, w tym motyw, poza pamięcią ViewModelu. Stan nawigacyjny, wybrana data, filtry i robocze wartości formularza powinny przetrwać odtworzenie procesu przy użyciu `SavedStateHandle` albo równoważnego mechanizmu.

Kryteria zakończenia porządkowania:

- widoki Compose i widget otrzymują ten sam wynik planu dla tej samej daty oraz danych;
- domena nie zależy od Room, Compose ani Glance;
- systemowy back i odtworzenie procesu nie rozjeżdżają trasy ze stanem ekranu;
- awaria operacji wieloetapowej nie zostawia częściowo zapisanych danych;
- błąd zapisu pozostawia formularz otwarty z zachowanymi wartościami;
- ponowne utworzenie procesu przywraca trwałe preferencje i istotny stan roboczy.

### Obecne zadanie: etapowy refaktor `MakViewModel`

Refaktor należy wykonać przed podłączeniem feedbacku do wszystkich operacji z etapu 6 sekcji 1.6. Nie przepisywać całego ViewModelu jednocześnie. Każdy etap ma kończyć się kompilującym stanem, testami odpowiednimi do zmiany i osobnym commitem.

Status: etapy 1-2 zrealizowane. Etap 1 to inwentaryzacja odpowiedzialności zapisana poniżej, etap 2 wydziela `FeedbackSink` i aplikacyjny `FeedbackController`. Etapy 3-8 pozostają do wykonania.

#### Etap 1: inwentaryzacja odpowiedzialności

1. Przypisać pola i metody `MakViewModel` do grup: dane wspólne, nawigacja, plan i kalendarz, szczegóły wystąpienia, edycja zajęć, semestr, konfiguracja początkowa, ustawienia i feedback.
2. Określić przyszłego właściciela każdej publicznej metody. Docelowi właściciele to `ScheduleViewModel`, `ClassEditViewModel`, `OccurrenceViewModel`, `SemesterViewModel`, `SetupViewModel`, `SettingsViewModel` albo cienki stan nadrzędny.
3. W tym etapie nie przenosić kodu i nie zmieniać zachowania aplikacji.

Kryterium etapu: każda publiczna metoda ma wskazanego przyszłego właściciela, a kod aplikacji pozostaje bez zmian.

#### Inwentaryzacja odpowiedzialności `MakViewModel` (etap 1)

Pola i metody według grupy oraz przyszłego właściciela:

1. Dane wspólne i cykl życia (cienki stan nadrzędny): pola `repository`, `clock`, `activePlanProvider`, `today`, `controls`, `semesters`, `activeSemesterData`, `uiState` oraz `refreshToday`.
2. Nawigacja: `navigate` i pola trasy `destination`. Przyszły właściciel to host nawigacji i `NavController`, nie ViewModel przepływu.
3. Plan i kalendarz (`ScheduleViewModel`): `selectScheduleView`, `changeWeek`, `selectScheduleDay`, `selectCourseFilter`, `changeMonth`, `selectCalendarDay`, `setShowCancelled`, `saveVisibleWeekOverride`, `clearVisibleWeekOverride`, `openNewClassForSelectedCalendarDay` oraz pola `schedule`, `scheduleDate`, `calendarMonth`, `calendarDate`, `scheduleView`, `courseFilterId`, `showCancelled`.
4. Szczegóły wystąpienia (`OccurrenceViewModel`): `openOccurrence`, `updateOccurrence`, `updateOccurrenceDraft`, `openOccurrenceEditDialog`, `dismissOccurrenceEditDialog`, `requestClassDeletion`, `cancelClassDeletion`, `deleteSelectedClass`, `cancelSelectedOccurrence`, `restoreSelectedOccurrence`, `updateSharedNoteDraft`, `updateOccurrenceNoteDraft`, `saveSharedNote`, `saveOccurrenceNote`, `saveSelectedOccurrenceChange` oraz pola `occurrence`, `selectedClassId`, `selectedOccurrenceDate`, `selectedNoteDate`, `occurrenceDraft`.
5. Edycja zajęć (`ClassEditViewModel`): `openNewClass`, `openEditClass`, `updateEditor`, `saveClass` oraz pola `editor`, `editorClassId`.
6. Semestr (`SemesterViewModel`): `openSemesterConfiguration`, `updateSemester`, `saveSemesterConfiguration`, `newWeekOverride`, `editWeekOverride`, `cancelWeekOverrideEdit`, `saveWeekOverride`, `deleteWeekOverride`, `addCourse`, `deleteCourse` oraz pola `semester`, `semesterDraft`, `semesterEditId`.
7. Konfiguracja początkowa (`SetupViewModel`): `updateSetup`, `setupNext`, `setupBack`, `finishSetup`, `cancelSetup`, `startSemesterSetup` oraz pola `setup`, `forceSetup`.
8. Ustawienia (`SettingsViewModel`): `selectSemester`, `requestSemesterDeletion`, `cancelSemesterDeletion`, `confirmSemesterDeletion`, `selectTheme`, `exportJson` oraz pola `settings`, `themeId`, `semesterToDeleteId`.
9. Feedback (`FeedbackController`): `publishFeedback` i `feedback`. Etap 2 przenosi kanał do kontrolera, a `MakViewModel` otrzymuje `FeedbackSink`.

Metody koordynujące kilka grup:

- `finishSetup`, `cancelSetup` i `startSemesterSetup` zmieniają stan konfiguracji oraz nawigację, więc `SetupViewModel` zgłosi jednorazowy zamiar przejścia, a trasę zmieni host.
- `confirmSemesterDeletion` wybiera kolejny aktywny semestr albo otwiera kreator, więc `SettingsViewModel` potrzebuje operacji nadrzędnej na aktywnym semestrze.
- `selectSemester` i `selectTheme` dotyczą stanu wspólnego, więc aktualizacja przejdzie przez cienki stan nadrzędny.
- `activePlan`, `buildToday` i `buildSchedule` korzystają ze wspólnego `ActivePlanProvider`, a po podziale każdy ViewModel złoży własny stan z tej samej ścieżki obliczeń.

#### Etap 2: kontroler feedbacku niezależny od ViewModelu

1. Dodać interfejs `FeedbackSink` z operacją publikacji `UiFeedback`.
2. Dodać aplikacyjny `FeedbackController`, który jest jedynym właścicielem `Channel<UiFeedback>(Channel.BUFFERED)`, udostępnia `Flow<UiFeedback>` i implementuje `FeedbackSink`.
3. Przenieść kanał feedbacku z `MakViewModel` do kontrolera i przekazać jego `Flow` do `MakSnackbarHost`.
4. Tymczasowo przekazać `FeedbackSink` do `MakViewModel`, bez podłączania operacji repozytorium.
5. Zachować kolejkę, jednokrotną obsługę oraz cztery warianty komunikatów.

Kryterium etapu: infrastruktura feedbacku działa bez własności i cyklu życia `MakViewModel`.

#### Etap 3: granica modeli szczegółów wystąpienia

1. Umieścić modele używane wyłącznie przez szczegóły terminu w `ui/occurrence`.
2. Zachować `OccurrenceDetailsUiState` jako publiczny stan prezentacyjny bez `SemesterWithData`, encji Room, stanu innych ekranów i informacji o bieżącej trasie.
3. Dodać `OccurrenceArgs(classId, date)` jako jawne argumenty otwarcia szczegółów.
4. Nie zmieniać tekstów ani zachowania ekranu.

Kryterium etapu: modele szczegółów mają jedną odpowiedzialność i nie wystawiają typów warstwy danych.

#### Etap 4: wydzielenie `OccurrenceViewModel`

1. Utworzyć `OccurrenceViewModel` z zależnościami `MakRepository`, `ActivePlanProvider`, `FeedbackSink` oraz `Clock`, jeśli jest potrzebny w przepływie.
2. Przenieść do niego otwieranie i budowanie szczegółów, edycję daty, godzin i sali, walidację dialogu, zapis zmiany lub przeniesienia, przywracanie i odwoływanie terminu, obie notatki oraz ich stany zapisu i błędów.
3. Pozostawić czyste funkcje `decideOccurrenceEdit`, `noteContentChanged` i `occurrenceRoomOverride` w domenie.
4. Tymczasowo dopuścić delegację z `MakViewModel`, aby etap nie wymagał równoczesnej przebudowy nawigacji.
5. Przenieść istniejące testy notatek do `OccurrenceViewModelTest` i dodać przypadki otwarcia prawidłowego wystąpienia, błędnych argumentów oraz edycji draftu podczas zapisu.

Kryterium etapu: logika szczegółów działa w `OccurrenceViewModel`, a `MakViewModel` nie wykonuje jej samodzielnie.

#### Etap 5: nawigacja szczegółów oparta na `NavController`

1. Przekazywać `classId` i datę jako argumenty trasy szczegółów.
2. Podłączyć ekran bezpośrednio do stanu i akcji `OccurrenceViewModel`, a następnie usunąć delegację przez `MakViewModel`.
3. Nie przechowywać `MakDestination` w `OccurrenceViewModel`. Powrót i przejścia wykonuje `NavController`.
4. Jeśli operacja wymaga zamknięcia ekranu po sukcesie, emitować jednorazowy efekt nawigacyjny, zbierany przez hosta ekranu.
5. Potwierdzić, że systemowy back zamyka szczegóły, zapis notatki i edycja terminu pozostają na ekranie, a usunięcie bazowych zajęć zamyka go dokładnie raz.

Kryterium etapu: `NavController` jest jedynym źródłem bieżącej trasy dla szczegółów wystąpienia.

#### Etap 6: feedback dla wystąpień i notatek

1. Podłączyć komunikaty sukcesu dla zmiany, przeniesienia, przywrócenia i odwołania terminu oraz zapisu i usunięcia obu rodzajów notatek.
2. Emitować sukces dopiero po zakończeniu operacji repozytorium. Dla `NoChange` nie wykonywać zapisu i nie emitować komunikatu.
3. Przy wyjątku zachować formularz i draft, wyłączyć stan zapisywania, pozostawić błąd przy właściwym polu lub formularzu i wyemitować jeden bezpieczny komunikat błędu.
4. Nie emitować komunikatów przy zmianie draftu, otwarciu dialogu ani zwykłej nawigacji.
5. Testy ViewModelu mają sprawdzać dokładnie jedną emisję, właściwy tekst, brak emisji dla `NoChange`, kolejność zapisu i sukcesu oraz brak nawigacji po błędzie.

Kryterium etapu: przepływ wystąpienia realizuje swoją część etapu 6 sekcji 1.6 bez zależności od nadrzędnego ViewModelu.

#### Etap 7: usunięcie starego kodu wystąpienia

1. Usunąć z `MakViewModel` przeniesione pola, metody, importy i tymczasowe delegacje.
2. Usunąć `selectedClassId`, `selectedOccurrenceDate`, `selectedNoteDate` i `occurrenceDraft`, jeśli nie są już używane przez inne przepływy.
3. Sprawdzić oba hosty aplikacji, podglądy Compose i wywołania ekranu szczegółów.

Kryterium etapu: `MakViewModel` nie zna formularza, notatek ani stanu szczegółów wystąpienia.

#### Etap 8: kolejne ViewModele

1. Powtórzyć ten sam schemat kolejno dla `ClassEditViewModel`, `SemesterViewModel`, `SetupViewModel`, `SettingsViewModel` i `ScheduleViewModel`.
2. Dla każdego przepływu najpierw wydzielić modele i testy, następnie logikę, nawigację i feedback, a na końcu usunąć stary kod.
3. Nie przenosić dwóch dużych przepływów w jednym commicie.
4. Podłączanie feedbacku dla danego przepływu realizuje odpowiednią część etapu 6 sekcji 1.6.

Kryterium etapu: nadrzędny stan koordynuje wyłącznie dane wspólne, a logika formularzy i operacji należy do ViewModelu właściwego przepływu.

Po każdym etapie uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug` oraz sprawdzić, że commit nie zawiera niezwiązanych zmian. Pierwsze zadanie wykonawcze obejmuje wyłącznie etapy 1 i 2. Wydzielanie `OccurrenceViewModel` rozpoczyna się po zaakceptowaniu granicy `FeedbackController`.

## 1.3. Plan poprawy ekranu „Plan”

Zmiany dotyczą widoku listy na ekranie „Plan”. Widok kalendarza zachowuje obecny zakres funkcji. Zmiany wykonać przy użyciu komponentów Material 3 i istniejących tokenów `MakSpacing`.

Status: punkty 1-7 są zaimplementowane i mają testy kompilujące się dla Androida. Pozostaje uruchomienie testów instrumentacyjnych i ręczna weryfikacja z punktu 8 na emulatorze albo urządzeniu.

### 1. Skrócenie nagłówka

W `ScheduleScreen` zastąpić `MakSectionHeader(eyebrow = "Plan", title = "Twoje zajęcia")` zwartym nagłówkiem „Plan zajęć”. Nie wyświetlać osobnej etykiety „PLAN”, ponieważ aktywna pozycja dolnej nawigacji wskazuje bieżący ekran. Zachować globalny topbar z logo MAK i ustawieniami oraz dolną nawigację bez zmian. Odstęp między nagłówkiem i `MakViewSwitch` nie może przekraczać `MakSpacing.md`.

### 2. Wspólne sterowanie tygodniem

W `ScheduleScreen.kt` utworzyć prywatny komponent `WeekNavigationHeader`. Komponent zawiera:

- przycisk poprzedniego tygodnia o obszarze dotyku co najmniej 48 dp;
- wyśrodkowany zakres `weekRangeLabel` i drugą linię `weekSubtitle`;
- przycisk następnego tygodnia o obszarze dotyku co najmniej 48 dp;
- pod zakresem dwie zwarte informacje: `weekTypeLabel` oraz `weekSourceLabel`.

Całość umieścić w jednej sekcji. Usunąć osobny `MakNoteBanner` z widoku listy. `weekTypeLabel` i `weekSourceLabel` nie mogą wyglądać jak karta o tej samej wadze co lista zajęć. Użyć małych odznak albo jednego wiersza na tle `surfaceVariant` lub `secondaryContainer`. Tekst musi korzystać z kolorów `onSurfaceVariant` albo `onSecondaryContainer`.

Odznaka tygodnia ma być przyciskiem otwierającym istniejący `WeekCorrectionDialog`. Nadać jej opis dostępności „Zmień oznaczenie tygodnia, obecnie: {weekTypeLabel}, źródło: {weekSourceLabel}”. Zachować widoczny focus, obsługę klawiatury i minimum 48 dp obszaru aktywnego.

### 3. Usunięcie odłączonego menu

Usunąć `MakActionMenu` zawierające akcję „Zmień A/B” z `ListView`. Nie zostawiać osobnego przycisku z trzema kropkami. Jedynym wejściem do `WeekCorrectionDialog` w widoku listy jest odznaka tygodnia w `WeekNavigationHeader`. Jeśli w przyszłości pojawią się inne akcje tygodnia, umieścić je w tej samej sekcji sterowania tygodniem.

### 4. Czytelny stan filtrów

Zastąpić ogólną etykietę „Pokaż filtry” prywatnym komponentem `ScheduleFilterSection`. Nagłówek sekcji ma pokazywać:

- „Filtry”, gdy wybrano „Wszystkie”;
- „Filtry: {nazwa kierunku}”, gdy wybrano konkretny kierunek.

Dodać ikonę filtra oraz ikonę rozwinięcia i zwinięcia z Material Icons. Semantyka przycisku zawiera bieżący wybór oraz stan „Rozwinięte” albo „Zwinięte”. Po rozwinięciu pozostawić `MakSelectField` z pojedynczym wyborem kierunku. Po wybraniu kierunku zwinąć sekcję i pozostawić nazwę wyboru w nagłówku. Długą nazwę kierunku ograniczyć do jednej linii z wielokropkiem, bez poziomego przewijania.

### 5. Neutralna i konkretna informacja o kolizji

W `ClassItemUi` zastąpić `hasConflict: Boolean` polem `conflictLabel: String?`. Podczas budowy stanu zebrać z `CollisionDetector` zakresy nakładania dla każdego wystąpienia. Zakres formatować jako `HH:mm-HH:mm`.

Etykiety:

- jedna kolizja: „Kolizja 09:00-09:30”;
- kilka kolizji: „Kolizje: 09:00-09:30, 10:00-10:15”.

Zakresy posortować, usunąć duplikaty i przypisać do obu zajęć uczestniczących w kolizji. Nie obliczać zakresu ponownie w `ClassCard`.

W `ClassCard` pokazać `conflictLabel` z ikoną ostrzeżenia. Użyć `tertiaryContainer` i `onTertiaryContainer` albo innej pary tokenów spełniającej kontrast. Nie używać `error`, czerwonego tekstu ani sformułowania sugerującego błąd użytkownika. Pełna etykieta kolizji ma wejść do opisu semantycznego karty.

### 6. Układ informacji w karcie zajęć

Zachować godzinę rozpoczęcia i zakończenia w lewej kolumnie oraz nazwę zajęć jako pierwszy element prawej kolumny. Poniżej ułożyć informacje w tej kolejności:

1. odznaka kierunku i typ zajęć;
2. sala, budynek i prowadzący w osobnym tekście metadanych;
3. informacja o kolizji;
4. notatka.

Dodać `building: String?` do `ClassItemUi` i wypełniać je z `PlannedOccurrence`. Pomijać puste elementy metadanych. Gdy nie podano sali, zachować tekst „Sala niepodana”. Nie umieszczać odznaki kierunku oraz wszystkich metadanych w jednym wierszu. Nazwa zajęć może mieć dwie linie, metadane dwie linie, a notatka trzy linie. Karta nie może przewijać się poziomo przy szerokości 320 dp i długiej nazwie kierunku albo prowadzącego.

Pozostawić kolorowy pasek kierunku, lecz nie używać go jako jedynego oznaczenia. Pasek ma być przycięty tym samym kształtem co karta. Ograniczyć dekoracyjne obramowania do karty i elementów interaktywnych. Nie dodawać osobnego obramowania każdemu wierszowi metadanych.

### 7. Czytelność wyboru dnia

W `DaySelector` zwiększyć rozmiar skrótu dnia z 10 sp do co najmniej 11 sp, a numeru dnia z 13 sp do co najmniej 14 sp. Zachować siedem równych kolumn, minimum 48 dp wysokości aktywnego obszaru oraz tekstowy opis daty w semantyce. Kolor nieaktywnego skrótu musi spełniać kontrast dla zwykłego tekstu. Stan wybrany nadal używa `primary` i `onPrimary`. Nie dodawać poziomego przewijania.

### 8. Testy i weryfikacja

Dodać `ScheduleScreenTest` w `app/src/androidTest/java/dev/retza/mak/ui/schedule`. Testy mają używać wstrzykniętego `ScheduleUiState` i obejmować:

- szerokość 320 dp z długą nazwą kierunku, zajęć i prowadzącego;
- widoczność zakresu tygodnia, oznaczenia A/B i źródła korekty w jednej sekcji;
- otwarcie `WeekCorrectionDialog` przez odznakę tygodnia;
- brak osobnego menu z trzema kropkami w widoku listy;
- nagłówek filtra bez wyboru oraz po wybraniu konkretnego kierunku;
- kartę z dokładnym zakresem jednej kolizji oraz kartę z kilkoma zakresami;
- zachowanie kolejności: kierunek i typ, metadane, kolizja, notatka;
- motyw ciemny przy szerokości 390 dp;
- dostępne akcje klawiatury, widoczny focus i obszary dotyku co najmniej 48 dp.

W testach JVM dodać osobne przypadki mapowania jednej oraz wielu kolizji na etykiety obu zajęć. Zachować istniejące testy `CollisionDetector`, ponieważ to one rozstrzygają samo nakładanie przedziałów.

Po implementacji uruchomić `gradlew.bat test` oraz kompilację testów Android. Na emulatorze albo urządzeniu sprawdzić szerokości 320 i 390 dp, motyw jasny i ciemny, TalkBack, klawiaturę, gest wstecz oraz brak obciętych akcji.

### 9. Kryteria akceptacji

- Pierwsza karta zajęć jest widoczna wyżej niż w obecnym układzie przy tej samej wysokości ekranu.
- Sterowanie tygodniem, oznaczenie A/B i źródło korekty tworzą jedną sekcję.
- Widok listy nie pokazuje odłączonego przycisku z trzema kropkami.
- Zwinięty filtr pokazuje aktywny kierunek.
- Kolizja jest neutralną informacją i zawiera dokładny zakres czasu.
- Długie dane karty zawijają się bez poziomego przewijania i bez zasłaniania odznaki kierunku.
- Wszystkie akcje mają semantyczne etykiety, widoczny focus i obszar dotyku co najmniej 48 dp.
- Układ pozostaje czytelny w motywie jasnym i ciemnym przy szerokości 320-390 dp.

## 1.4. Plan poprawy ekranu szczegółów terminu

Zmiany dotyczą `OccurrenceDetailsScreen` oraz topbara aplikacji. Ekran ma pokazywać szczegóły, notatkę i formularz zmiany w jednej przewidywalnej kolejności. Akcje nawigacyjne i główne nie mogą znajdować się pomiędzy informacjami.

Postęp: etapy 1-7 są zaimplementowane. Dolne akcje uwzględniają bezpieczny obszar dokładnie raz, a testy Compose pokrywają notatkę, brak „Pokaż zmiana terminu”, przełączenie formularza, przywracanie terminu oraz wszystkie akcje menu topbara. Testy JVM, kompilacja testów Android, lint i assemble przechodzą. Odbiór na emulatorze lub urządzeniu pozostaje do wykonania.

### Etap 1: usunięcie powtórzonego nagłówka i rozwinięcie notatki

1. Pozostawić tekst „Termin” wyłącznie w globalnym topbarze.
2. Zastąpić `MakSectionHeader` prywatnym nagłówkiem ekranu pokazującym nazwę zajęć oraz opis „Zmiana dotyczy tylko wybranego wystąpienia zajęć.”.
3. Nie wyświetlać lokalnej etykiety `TERMIN` nad nazwą zajęć.
4. Usunąć `showNoteForm` oraz `MakExpandableSection` z etykietą „Pokaż notatkę”.
5. Renderować `NotesBlock` bezpośrednio po `Facts`, w stanie rozwiniętym od pierwszego wyświetlenia ekranu.
6. Zachować wspólną notatkę, pole notatki do wystąpienia oraz akcje zapisu i usunięcia bez zmiany ich działania.

Kryterium etapu: ekran kompiluje się, tekst „Termin” występuje tylko w topbarze, a notatka jest widoczna bez dodatkowego kliknięcia.

### Etap 2: obsługa akcji po prawej stronie topbara

1. Dodać do `MakTopBar` opcjonalny slot `actions` typu `@Composable RowScope.() -> Unit`.
2. Zachować obecną akcję ustawień na ekranach głównych. Jeśli przekazano slot, renderować go po prawej stronie topbara.
3. Dla trasy szczegółów terminu przekazać do slotu `MakActionMenu` z akcjami dostępnymi w stanie: „Odwołaj termin”, „Edytuj bazowe zajęcia” i „Usuń zajęcia”.
4. Nadać przyciskowi opis „Więcej opcji”. Zachować obszar dotyku 48 dp, focus i obsługę klawiatury.
5. Nie używać `Modifier.fillMaxWidth()` dla menu w topbarze. Wyrównać przycisk do prawej krawędzi z paddingiem 4 dp, symetrycznie do przycisku cofnięcia.
6. Usunąć `MakActionMenu` z przewijanej treści `OccurrenceDetailsScreen`.

Kryterium etapu: trzy kropki znajdują się na wysokości przycisku cofnięcia i nie zajmują miejsca w treści ekranu.

### Etap 3: podłączenie akcji topbara do nawigacji

1. W `MakNavHostApp` budować listę akcji tylko wtedy, gdy bieżąca trasa to `MakRoutes.Occurrence`.
2. Akcję edycji bazowych zajęć podłączyć do istniejącego `openEditClass` i przejścia do `editRoute`.
3. Akcję odwołania podłączyć do `cancelSelectedOccurrence`.
4. Akcję usunięcia podłączyć do `requestClassDeletion`. Sam dialog potwierdzenia nadal renderować w `OccurrenceDetailsScreen`.
5. Usunąć z parametrów `OccurrenceDetailsScreen` callbacki używane już wyłącznie przez topbar: `onEditBaseClass`, `onRequestDeleteBaseClass` i `onCancelOccurrence`.
6. Zaktualizować oba miejsca wywołujące ekran, w tym nieużywany przebieg legacy, aby cały moduł nadal się kompilował.

Kryterium etapu: każda pozycja menu wykonuje tę samą operację co wcześniej, a usunięcie nadal wymaga potwierdzenia.

### Etap 4: akcje przy dolnej krawędzi

1. Zastąpić przewijanie całego `MakScreenContent` nadrzędną `Column` wypełniającą ekran.
2. Umieścić `MakScreenContent` z `verticalScroll` i `Modifier.weight(1f)` jako przewijaną treść.
3. Dodać pod nią prywatny komponent `OccurrenceBottomActions` poza obszarem przewijania.
4. W stanie podstawowym pokazać w dolnym komponencie „Przywróć termin”, jeśli wystąpienie można przywrócić, albo „Zmień termin”, jeśli można je zmienić lub przenieść. „Zamknij” pozostaje drugą, zawsze dostępną akcją.
5. Ułożyć przyciski pionowo na pełną szerokość. Użyć poziomego paddingu `MakSpacing.lg`, odstępu `MakSpacing.sm` i dolnego paddingu uwzględniającego bezpieczny obszar przekazany przez nadrzędny `Scaffold`.
6. Po otwarciu formularza zmiany ukryć dolny przycisk „Zmień termin”, pozostawiając „Zamknij”. Akcje „Zapisz zmianę” i „Przenieś termin” pozostają częścią formularza.

Kryterium etapu: przewijanie treści nie przesuwa przycisku „Zamknij” ani podstawowej akcji terminu.

### Etap 5: uproszczenie formularza zmiany terminu

1. Zachować lokalny stan `showChangeForm`, domyślnie `false`.
2. Dolny przycisk „Zmień termin” ustawia `showChangeForm = true`.
3. Usunąć `MakExpandableSection` oraz tekst „Pokaż zmiana terminu”.
4. Gdy `showChangeForm` jest prawdziwy, renderować `OccurrenceForm` bezpośrednio pod blokiem notatki.
5. Zachować pickery daty i czasu, pole sali oraz przyciski „Zapisz zmianę” i „Przenieś termin”.
6. Nie zwijać automatycznie formularza po zmianie wartości. Nawigacja po udanym zapisie pozostaje odpowiedzialnością istniejącego ViewModelu.

Kryterium etapu: na ekranie nie ma tekstu „Pokaż zmiana terminu”, a formularz otwiera się wyłącznie przez dolny przycisk „Zmień termin”.

### Etap 6: testy i odbiór

1. Dodać test Compose dla ekranu z długą nazwą zajęć przy szerokości 320 dp.
2. Potwierdzić brak lokalnej etykiety `TERMIN`, „Pokaż notatkę” oraz „Pokaż zmiana terminu”.
3. Potwierdzić, że notatka i fakty są widoczne w przewijanej treści.
4. Potwierdzić, że „Zmień termin” i „Zamknij” pozostają w dolnym obszarze po przewinięciu treści.
5. Potwierdzić otwarcie formularza przez „Zmień termin” oraz widoczność „Zapisz zmianę” i „Przenieś termin”.
6. Dodać test topbara dla menu „Więcej opcji” i każdej dostępnej akcji.
7. Sprawdzić wariant przywracania, odwołany termin, brak uprawnień do zmiany oraz dialog usunięcia.
8. Uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.

Kryterium zakończenia: treść ma kolejność nazwa, opis, status, fakty, notatka i opcjonalny formularz. Menu znajduje się w topbarze, a podstawowe akcje pozostają przy dolnej krawędzi.

### Etap 7: poprawki po recenzji

1. W `OccurrenceBottomActions` usunąć `navigationBarsPadding()` oraz odpowiadający mu import. `NavHost` już otrzymuje bezpieczny dolny padding z nadrzędnego `Scaffold`, więc ponowne zastosowanie insetu niepotrzebnie odsuwa przyciski od dolnej krawędzi.
2. Zachować `OccurrenceBottomActions` poza przewijaną treścią oraz pozostawić jego poziomy i pionowy padding oparty na `MakSpacing`.
3. W `OccurrenceDetailsScreenTest` sprawdzać pełny tekst `Notatka do zajęć: Notatka wspólna` albo jawnie użyć dopasowania podciągu. Nie szukać dokładnego tekstu `Notatka wspólna`, ponieważ taki osobny węzeł nie jest renderowany.
4. Rozszerzyć test ekranu o brak tekstu `Pokaż zmiana terminu`.
5. Po otwarciu formularza potwierdzić, że dolny przycisk `Zmień termin` znika, a `Zamknij`, `Zapisz zmianę` i `Przenieś termin` pozostają widoczne.
6. Dodać przypadek `canRestoreOccurrence = true`, który potwierdza widoczność i działanie przycisku `Przywróć termin`.
7. Rozszerzyć test topbara przy szerokości 320 dp o rzeczywisty `MakActionMenu`. Sprawdzić opis `Więcej opcji`, widoczność pozycji `Odwołaj termin`, `Edytuj bazowe zajęcia` i `Usuń zajęcia` oraz osobne wywołanie każdej akcji.
8. Nie zmieniać zachowania ViewModelu, tras, operacji zapisu, sekcji 1.5 ani widgetu.
9. Uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.

Kryterium zakończenia etapu: dolne akcje uwzględniają bezpieczny obszar dokładnie raz, test notatki używa tekstu rzeczywiście renderowanego przez ekran, a testy pokrywają przełączenie formularza, przywracanie terminu i wszystkie akcje menu topbara.

## 1.5. Plan uproszczenia sekcji rozwijanych

Zmiany dotyczą przede wszystkim konfiguracji semestru. Rozbudowane funkcje zarządzania nie powinny rozwijać długich formularzy i list wewnątrz ekranu nadrzędnego. Osobny ekran jest domyślnym rozwiązaniem, gdy ujawniana treść ma własne akcje, formularz, listę elementów albo może znacząco zwiększyć wysokość widoku.

Postęp: etapy 1-6 są zaimplementowane. Trasy podrzędne semestru, osobne ekrany „Kierunki” oraz „Korekty tygodni”, formularz korekty otwierany akcją oraz ujednolicona sekcja rozwijana mają testy kompilujące się dla Androida i testy JVM. Pozostaje uruchomienie testów instrumentacyjnych i ręczna weryfikacja z punktu 6 na emulatorze albo urządzeniu.

### Etap 1: wydzielenie tras podrzędnych semestru

1. Dodać trasy `semester/{semesterId}/courses` oraz `semester/{semesterId}/week-overrides` do `MakRoutes`.
2. Dodać funkcje budujące obie trasy z identyfikatorem semestru. Nie przekazywać całego stanu przez argumenty nawigacji.
3. Przypisać tytuły topbara „Kierunki” oraz „Korekty tygodni” i włączyć na obu ekranach standardowy przycisk cofnięcia.
4. Zachować istniejący `semesterId` jako źródło wyboru danych. Wejście na trasę ma odtworzyć właściwy semestr także po odtworzeniu procesu aplikacji.
5. Nie tworzyć równoległego lokalnego źródła danych. Ekrany mają używać istniejącego stanu semestru i istniejących operacji ViewModelu.

Kryterium etapu: obie trasy można otworzyć bez utraty wyboru semestru, a systemowy gest wstecz wraca do konfiguracji tego samego semestru.

### Etap 2: uproszczenie ekranu konfiguracji semestru

1. Usunąć lokalne stany `showCourses` i `showOverrides` z `SemesterScreen`.
2. Usunąć oba użycia `MakExpandableSection` oraz teksty „Pokaż kierunki” i „Pokaż korekty tygodni”.
3. Pozostawić na ekranie formularz podstawowych danych semestru: nazwę, zakres dat, pierwszy tydzień oraz zapis.
4. Pod formularzem dodać dwie zwarte pozycje nawigacyjne:
   - „Kierunki” z liczbą zapisanych kierunków,
   - „Korekty tygodni” z liczbą zapisanych korekt.
5. Każda pozycja ma mieć czytelną nazwę, krótkie objaśnienie, opcjonalny licznik oraz ikonę przejścia w prawo. Cały wiersz powinien być klikalny i mieć obszar dotyku co najmniej 48 dp.
6. Aktywacja pozycji ma otwierać odpowiedni ekran podrzędny. Nie rozwijać treści na ekranie konfiguracji.
7. Zachować akcję powrotu do ustawień, dopóki globalny topbar i gest wstecz nie zapewnią jednoznacznego powrotu we wszystkich ścieżkach wejścia.

Kryterium etapu: konfiguracja semestru nie zwiększa wysokości po wybraniu „Kierunki” lub „Korekty tygodni”, a obie funkcje są dostępne przez jednoznaczne pozycje nawigacyjne.

### Etap 3: osobny ekran kierunków

1. Przenieść `CoursesBlock` do publicznego ekranu `SemesterCoursesScreen` albo równoważnie nazwanego komponentu w pakiecie semestru.
2. Na początku ekranu pokazać krótkie objaśnienie, że kierunki należą tylko do wybranego semestru.
3. Następnie pokazać listę kierunków, stan pusty oraz formularz dodawania z nazwą i wyborem koloru.
4. Zachować istniejące operacje dodawania i usuwania. Usunięcie nadal musi korzystać z obowiązujących zabezpieczeń danych.
5. Robocza nazwa i kolor nie mogą znikać po chwilowym przejściu aplikacji do tła ani po odtworzeniu ekranu. Stan formularza powinien należeć do ViewModelu albo `SavedStateHandle`, nie do lokalnego `remember`.
6. Długie nazwy mają zawijać się bez poziomego przewijania. Akcje nie mogą zostać obcięte przy szerokości 320 dp.

Kryterium etapu: pełne zarządzanie kierunkami odbywa się na osobnym ekranie, a powrót prowadzi do konfiguracji właściwego semestru.

### Etap 4: osobny ekran korekt tygodni

1. Przenieść `WeekOverridesSection`, listę korekt i formularz korekty do `SemesterWeekOverridesScreen` albo równoważnie nazwanego komponentu.
2. Na początku ekranu wyjaśnić krótko różnicę między zakresem „Tylko ten tydzień” i „Od tego tygodnia”.
3. Pokazać listę istniejących korekt, stan pusty i akcję „Dodaj korektę”.
4. Formularz dodawania lub edycji może pojawić się na tym ekranie, ponieważ bezpośrednio należy do zarządzanej listy. Powinien być widoczny po akcji „Dodaj” albo „Edytuj” i znajdować się bezpośrednio przy kontekście tej akcji.
5. Zachować edycję, usuwanie, anulowanie oraz istniejące reguły daty, typu tygodnia i zakresu korekty.
6. Stan edycji i wprowadzone wartości mają przetrwać odtworzenie ekranu. Powrót z aktywnym, niezapisanym formularzem nie może po cichu zapisać ani usunąć danych.

Kryterium etapu: lista i formularz korekt nie zajmują miejsca na ekranie podstawowych danych semestru, a wszystkie dotychczasowe operacje są dostępne na ekranie podrzędnym.

### Etap 5: wizualne powiązanie treści rozwijanej z przyciskiem

1. Przejrzeć pozostałe użycia `MakExpandableSection` i sklasyfikować każde z nich:
   - osobny ekran, gdy treść zawiera listę, większy formularz lub niezależny przepływ,
   - rozwinięcie w miejscu, gdy treść jest krótka, pomocnicza i potrzebna w kontekście bieżącego widoku,
   - dialog lub arkusz, gdy użytkownik wykonuje krótką, zamkniętą decyzję i powinien pozostać w tym samym miejscu.
2. Dla sekcji pozostawionych w miejscu zmienić wygląd `MakExpandableSection` tak, aby przycisk i treść tworzyły jeden komponent:
   - wspólny kształt i szerokość,
   - wyróżnione tło przycisku,
   - po rozwinięciu tło treści w innym odcieniu tego samego kontenera,
   - brak przerwy sugerującej, że treść jest niezależnym blokiem,
   - wspólne obramowanie albo ciągłość zaokrągleń.
3. Przycisk ma pokazywać ikonę kierunku rozwinięcia oraz stan „Rozwinięte” lub „Zwinięte” w semantyce.
4. Ponowne użycie przycisku zwija wyłącznie treść, którą bezpośrednio kontroluje. Nie może wpływać na sąsiednie sekcje.
5. Zachować kontrast w jasnym i ciemnym motywie, widoczny focus oraz minimalny obszar dotyku 48 dp.
6. Nie używać samej zmiany koloru jako jedynego sygnału powiązania lub stanu.

Kryterium etapu: każda treść rozwijana w miejscu jest wizualnie i semantycznie podpięta pod sterujący nią przycisk, a rozbudowane przepływy otwierają osobne ekrany.

### Etap 6: testy nawigacji i dostępności

1. Dodać test tras dla wejścia z konfiguracji semestru do kierunków i korekt oraz powrotu do tego samego semestru.
2. Dodać test odtworzenia trasy podrzędnej z `semesterId` bez wcześniejszego otwarcia ekranu nadrzędnego.
3. Dodać test ekranu konfiguracji potwierdzający brak tekstów „Pokaż kierunki” i „Pokaż korekty tygodni” oraz obecność dwóch pozycji nawigacyjnych z licznikami.
4. Zachować testy dodawania i usuwania kierunku oraz tworzenia, edycji i usuwania korekty po przeniesieniu komponentów.
5. Dla pozostałego `MakExpandableSection` dodać test rozwinięcia, zwinięcia, semantyki i ciągłości kontenera.
6. Sprawdzić szerokości 320 dp i 390 dp, motyw jasny i ciemny, obsługę TalkBack, klawiaturę, systemowy gest wstecz oraz zachowanie po odtworzeniu procesu.
7. Po implementacji uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.

Kryterium zakończenia: konfiguracja semestru pozostaje krótka, zarządzanie kierunkami i korektami ma własne ekrany, a każda zachowana sekcja rozwijana jasno wskazuje, który przycisk steruje jej treścią.

## 1.6. Plan poprawy edycji terminu, notatek i komunikatów zwrotnych

Zmiany upraszczają edycję pojedynczego wystąpienia, przywracają możliwość edycji notatki wspólnej i wprowadzają spójny feedback po operacjach. Edycja terminu odbywa się w jednym dialogu z jedną akcją zapisu. Po zapisaniu, odwołaniu albo przywróceniu użytkownik pozostaje na ekranie szczegółów i od razu widzi aktualny stan.

Status: etapy 1-5 są zaimplementowane. Edycja terminu korzysta z jednego dialogu, a decyzję o modyfikacji, przeniesieniu albo przywróceniu wybiera czysta funkcja `decideOccurrenceEdit`; przeniesienie liczy się względem daty bazowej, pusta sala jest jawnym nadpisaniem bez migracji Room, a dialog blokuje zamknięcie podczas zapisu. Ekran szczegółów pokazuje dwa niezależne pola notatek: wspólną dla wszystkich terminów (aktualizacja `classNote`) i tylko dla wybranej daty (zapis lub usunięcie `OccurrenceNote`). Puste pole usuwa notatkę, a zapis blokuje tylko właściwą akcję. Draft notatki nie jest nadpisywany, jeśli użytkownik zmieni go podczas trwającego zapisu. Wspólny system feedbacku ma model `UiFeedback`, buforowany `Channel` wystawiony jako `Flow` i jeden `MakSnackbarHost` w głównym `Scaffold` z ikoną, kontrastem, semantyką, kolejką i różnym czasem wyświetlania. Na tym etapie żadna operacja repozytorium nie emituje komunikatów. Etapy 6-7 pozostają do wykonania.

### Etap 1: model różnicy i walidacja terminu

1. Wydzielić czystą funkcję porównującą szkic z bazowym i aktualnym terminem.
2. Normalizować datę i godziny przez parsowanie, a salę przez `trim`; pustą salę traktować jak `null`.
3. Funkcja zwraca jeden z wyników: `NoChange`, `Modified`, `Moved` albo `Restored`.
4. Inna data oznacza `Moved`. Ta sama data przy zmianie godzin lub sali oznacza `Modified`. Wartości zgodne z bazowym terminem przy istniejącej zmianie oznaczają `Restored`.
5. Dane zgodne z aktualnym stanem oznaczają `NoChange` i nie mogą powodować zapisu rekordu `OccurrenceChange`.
6. Zachować walidację zakresu semestru i regułę, że godzina końca jest późniejsza od początku. Błędy przypisać do konkretnych pól.
7. Rozszerzyć stan szczegółów o bazową datę, godziny i salę, szkic formularza, błędy pól, widoczność dialogu oraz `isSaving`.

Kryterium etapu: logika potrafi jednoznacznie odróżnić brak zmiany, modyfikację, przeniesienie i przywrócenie bez dostępu do Compose ani Room.

### Etap 2: jeden dialog edycji terminu

1. Przycisk „Zmień termin” otwiera dialog „Edytuj ten termin”.
2. Dialog zawiera datę, godzinę rozpoczęcia, godzinę zakończenia i salę oraz opis „Zmiany dotyczą tylko tego terminu. Pozostałe wystąpienia zajęć pozostaną bez zmian.”.
3. Dialog ma wyłącznie akcje „Anuluj” i „Zapisz”. Usunąć akcje „Zapisz zmianę” i „Przenieś termin” oraz formularz wysuwany w treści ekranu.
4. Zastąpić callbacki `onChangeOccurrence` i `onMoveOccurrence` jednym `onSaveOccurrenceChange`.
5. Dla `NoChange` przycisk „Zapisz” pozostaje nieaktywny. Podczas operacji również jest nieaktywny, aby zapobiec podwójnemu zapisowi.
6. Błąd walidacji lub repozytorium pozostawia dialog otwarty z zachowanymi wartościami. Dialog zamyka się wyłącznie po udanym zapisie albo po „Anuluj”.
7. Dialog ma mieścić się przy szerokości 320 dp, przewijać własną treść przy małej wysokości i poprawnie współpracować z klawiaturą ekranową.

Kryterium etapu: użytkownik nie wybiera technicznej różnicy między zmianą i przeniesieniem, a pusta operacja nie tworzy pilla ani rekordu zmiany.

### Etap 3: zapis, przywracanie i natychmiastowy stan

1. `saveOccurrenceChange` sam wybiera zapis modyfikacji, zapis przeniesienia albo usunięcie istniejącej zmiany na podstawie wyniku z etapu 1.
2. Jedna operacja może jednocześnie zmienić datę, godziny i salę.
3. Po powodzeniu nie zmieniać trasy na ekran planu. Zamknąć dialog i natychmiast zaktualizować fakty, status oraz dostępne akcje na szczegółach.
4. Po `Modified` pokazać status „Zmienione”. Po `Moved` pokazać status „Przeniesione” wraz z datą bazową i nową datą.
5. `restoreSelectedOccurrence` usuwa zmianę, pozostaje na szczegółach, usuwa pill zmiany i zamienia „Przywróć termin” na „Zmień termin”.
6. Odwołanie terminu także pozostaje na szczegółach i po sukcesie pokazuje stan „Odwołane” oraz akcję przywracania.
7. Stan UI zaktualizować po sukcesie bez oczekiwania na ponowne wejście na ekran. Room nadal pozostaje źródłem prawdy i jego obserwacja musi potwierdzić ten sam wynik.

Kryterium etapu: każda udana operacja daje natychmiast widoczny rezultat, a przycisk przywracania nie pozostaje po usunięciu zmiany.

### Etap 4: dwie notatki na szczegółach

1. Nazwać blok „Notatki” i pokazać dwa oddzielne pola: „Notatka dla wszystkich terminów” oraz „Notatka tylko dla tej daty”.
2. Dodać `sharedNoteDraft` i operację zapisu notatki wspólnej przez aktualizację `classNote` bazowych zajęć.
3. Każde pole ma własną akcję zapisu i krótki opis zakresu. Nie wymaga przejścia do edycji wszystkich danych zajęć.
4. Pusta notatka wspólna usuwa `classNote`. Pusta notatka wystąpienia usuwa istniejący `OccurrenceNote`; jeśli notatki wcześniej nie było, zapis pozostaje nieaktywny.
5. Podczas zapisu blokować tylko akcję właściwego pola. Błąd zachowuje wpisany tekst.
6. Po sukcesie pozostać na szczegółach i odświeżyć właściwą wartość bez wpływu na drugą notatkę.

Kryterium etapu: użytkownik może z jednego ekranu niezależnie zapisać, zmienić i usunąć notatkę wspólną oraz notatkę dla wybranej daty.

### Etap 5: wspólny system komunikatów w aplikacji

1. Dodać model `UiFeedback(message, kind)`, gdzie `kind` przyjmuje `Success`, `Error`, `Warning` albo `Info`.
2. ViewModel udostępnia jednokierunkowy, buforowany strumień zdarzeń. Użyć `Channel<UiFeedback>` wystawionego jako `Flow`, aby pojedynczy komunikat został obsłużony tylko raz.
3. Dodać jeden `SnackbarHost` do głównego `Scaffold`. Nie używać systemowych Toastów.
4. Własny snackbar korzysta z Material 3 i rozróżnia wariant ikoną, tekstem oraz kolorem. Sukces używa zielonego kontenera, błąd `errorContainer`, ostrzeżenie kontrastowego koloru ostrzegawczego, a informacja `primaryContainer`.
5. Kolor nie jest jedynym nośnikiem znaczenia. Zapewnić semantyczny opis, kontrast, focus i poprawne działanie w jasnym oraz ciemnym motywie.
6. Sukcesy i informacje pokazywać krótko, a błędy i ostrzeżenia długo. Komunikaty kolejkować, bez nakładania.
7. Emitować sukces dopiero po zakończeniu operacji repozytorium. Wyjątek emituje błąd, nie zamyka formularza i nie uruchamia nawigacji przewidzianej dla sukcesu.

Kryterium etapu: komunikat jest widoczny po zmianie trasy, występuje dokładnie raz i jednoznacznie wskazuje wynik operacji bez polegania wyłącznie na kolorze.

### Etap 6: podłączenie feedbacku do operacji

1. Podłączyć komunikaty do jawnych operacji zapisu i usuwania: zajęć, semestrów, kierunków, korekt tygodni, zmian wystąpień, obu rodzajów notatek i eksportu.
2. Nie pokazywać komunikatów przy zwykłej nawigacji, zmianie dnia, filtrowaniu, otwarciu formularza ani zmianie roboczej wartości pola.
3. Dla terminu stosować komunikaty „Zmieniono termin”, „Przeniesiono termin”, „Przywrócono termin” i „Odwołano termin”.
4. Dla notatek stosować „Zapisano notatkę dla wszystkich terminów”, „Usunięto notatkę dla wszystkich terminów”, „Zapisano notatkę dla tej daty” i „Usunięto notatkę dla tej daty”.
5. Dla pozostałych operacji używać krótkich komunikatów opisujących rzeczywisty rezultat, na przykład „Dodano kierunek”, „Usunięto zajęcia” albo „Wyeksportowano dane”.
6. Błędy formułować według operacji, bez surowych wyjątków, na przykład „Nie udało się zapisać zmian”. Szczegóły techniczne mogą trafić do logu, ale nie do tekstu interfejsu.

Kryterium etapu: każda istotna operacja zapisu lub usunięcia ma komunikat sukcesu i bezpieczny komunikat błędu, bez nadmiarowych snackbarów podczas przeglądania aplikacji.

### Etap 7: testy i odbiór

1. Dodać testy JVM funkcji różnicy dla braku zmian, zmiany godzin, zmiany sali, zmiany daty, jednoczesnej zmiany daty i godzin oraz powrotu do wartości bazowych.
2. W testach ViewModelu potwierdzić brak zapisu dla `NoChange`, właściwą operację dla pozostałych wyników, pozostanie na szczegółach, natychmiastową zmianę statusu i emisję jednego feedbacku.
3. Sprawdzić przywracanie i odwołanie: aktualizację pilla, zmianę dostępnego przycisku oraz właściwy komunikat.
4. Dodać testy zapisu, aktualizacji i usuwania obu rodzajów notatek bez opuszczania szczegółów.
5. Test Compose obejmuje dialog przy szerokości 320 dp, klawiaturę, walidację, zachowanie wartości po błędzie, blokadę wielokrotnego zapisu i oba pola notatek.
6. Test hosta snackbarów obejmuje kolejkę, cztery warianty, semantykę, czas wyświetlania oraz zachowanie podczas zmiany trasy.
7. Uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.
8. Na emulatorze albo urządzeniu sprawdzić TalkBack, motyw jasny i ciemny, małą wysokość z otwartą klawiaturą, szybkie wielokrotne kliknięcia oraz widoczność feedbacku po każdej operacji.

Kryterium zakończenia: edycja terminu ma jeden zrozumiały przebieg, puste zmiany nie są zapisywane, przywracanie natychmiast aktualizuje ekran, oba rodzaje notatek są edytowalne, a wszystkie istotne operacje otrzymują spójny feedback wewnątrz aplikacji.

## 2. Technologie

- Kotlin;
- Jetpack Compose;
- Room nad SQLite;
- Jetpack Glance do widgetu;
- kotlinx.serialization do importu i eksportu JSON;
- `java.time` do dat, godzin i obliczania tygodni.

Aplikacja przechowuje wszystkie dane lokalnie. Nie wymaga dostępu do internetu.

## 3. Nawigacja

Dolny pasek zawiera trzy pozycje:

- **Dzisiaj** — zajęcia z bieżącego dnia;
- **Plan** — tygodniowy plan zajęć;
- **Dodaj** — formularz nowych zajęć.

Ustawienia są dostępne z menu w prawym górnym rogu. Kliknięcie zajęć na ekranie „Dzisiaj” lub „Plan” otwiera ekran szczegółów i edycji.

Jeśli aplikacja nie ma jeszcze semestru, zamiast ekranu „Dzisiaj” otwiera kreator pierwszej konfiguracji. Kreator tworzy pierwszy semestr i co najmniej jeden kierunek, a po zakończeniu prowadzi do dodawania zajęć. Ustawienia pozwalają później dodawać, wybierać, konfigurować i usuwać semestry.

## 4. Ekran „Dzisiaj”

To domyślny ekran otwierany po uruchomieniu aplikacji.

Powinien pokazywać:

- dzień tygodnia i pełną datę;
- liczbę zajęć;
- wszystkie aktywne zajęcia w kolejności od najwcześniejszego;
- kolor kierunku;
- oznaczenie kolizji, gdy ta funkcja zostanie dodana;
- stan pusty, gdy danego dnia nie ma zajęć.

Przykładowy element:

```text
08:00–09:30
Programowanie
Laboratorium · Informatyka
Sala L204 · dr Jan Kowalski
```

Po przekroczeniu północy ekran pokazuje plan nowego dnia przy ponownym otwarciu lub wznowieniu aplikacji. Odczyt aktualnej daty nie wymaga ciągłego działania aplikacji w tle.

## 5. Ekran „Plan”

Ekran oferuje dwa równorzędne sposoby przeglądania planu: **Lista** i **Kalendarz**. Ostatnio wybrany sposób może być zapamiętany lokalnie.

### Widok „Lista”

Widok listy na telefonie składa się z:

- wyboru dnia: poniedziałek–niedziela;
- przesuwania między dniami gestem;
- przechodzenia między tygodniami oraz powrotu do bieżącego tygodnia;
- daty i oznaczenia A/B widocznego przy przeglądanym tygodniu;
- akcji „Zmień tydzień A/B”, z wyborem zakresu „Tylko ten tydzień” lub „Od tego tygodnia”;
- filtrów: „Wszystkie” oraz poszczególne kierunki;
- listy zajęć posortowanej według godziny;
- oznaczeń kolizji, gdy ta funkcja zostanie dodana.

Plan uwzględnia aktualny semestr, automatyczne lub ręcznie skorygowane oznaczenie A/B, zmiany pojedynczych wystąpień oraz zajęcia jednorazowe. Ręcznie skorygowany tydzień jest wyraźnie oznaczony; użytkownik może usunąć korektę i wrócić do automatycznego wyniku.

### Widok „Kalendarz”

Widok kalendarza pokazuje jeden miesiąc i zawiera:

- przejście do poprzedniego i następnego miesiąca;
- powrót do bieżącej daty;
- oznaczenie liczby lub kolorów zajęć w każdym dniu;
- czytelne oznaczenie dni z odwołanymi, zmienionymi lub jednorazowymi zajęciami;
- wybór dnia i listę jego aktywnych zajęć pod kalendarzem;
- akcję dodania nowych zajęć jednorazowych dla wybranej daty.

Kalendarz pokazuje wynik `ScheduleResolver`, dlatego musi być zgodny z ekranem „Dzisiaj”, listą planu i widgetem. Odwołane zajęcia mogą pozostać widoczne jako przekreślone tylko wtedy, gdy użytkownik włączy opcję „Pokaż odwołane”. Domyślnie kalendarz pokazuje plan aktywny.

## 6. Dodawanie i edycja zajęć

Formularz powinien zawierać:

### Podstawowe dane

- nazwa przedmiotu;
- typ: wykład, ćwiczenia, laboratorium, projekt, seminarium lub inne;
- kierunek;
- prowadzący.

### Termin

- dzień tygodnia;
- godzina rozpoczęcia;
- godzina zakończenia;
- powtarzanie: co tydzień, tydzień A, tydzień B lub jednorazowo;
- konkretna data, gdy zajęcia są jednorazowe.

Formularz ogranicza zajęcia do jednego dnia kalendarzowego. Godzina zakończenia musi być późniejsza od rozpoczęcia; zajęcia przechodzące przez północ są odrzucane.

### Lokalizacja i dodatkowe dane

- sala;
- budynek;
- grupa — pole opcjonalne;
- notatka do zajęć — opcjonalna, wspólna dla każdego wystąpienia tego wpisu;
- notatka do wybranego wystąpienia — opcjonalna, przypięta do jednej konkretnej daty zajęć.

Podczas dodawania notatki użytkownik wybiera zakres: „Do tych zajęć” albo „Tylko do tego terminu”. Notatkę do konkretnego wystąpienia można dodać z ekranu szczegółów zajęć na ekranie „Dzisiaj” lub „Plan”. Jeśli istnieją oba typy, aplikacja pokazuje je osobno i nie nadpisuje notatki wspólnej.

Prowadzący zapisani wcześniej są proponowani podczas wpisywania. Formularz powinien walidować, że nazwa, kierunek, godzina rozpoczęcia i zakończenia są uzupełnione, a godzina zakończenia jest późniejsza od rozpoczęcia. Zajęcia przechodzące przez północ są nieprawidłowe.

Po usunięciu zajęć aplikacja powinna wymagać potwierdzenia. Edycja i usuwanie muszą aktualizować widget.

## 7. Semestr i tygodnie A/B

Ustawienia semestru są potrzebne od pierwszej wersji, ponieważ określają automatyczny rytm tygodni A/B.

Użytkownik może mieć wiele semestrów. Każdy semestr ma własne kierunki, prowadzących, zajęcia, korekty tygodni, notatki i zmiany wystąpień. Dane nie przechodzą między semestrami automatycznie. Użytkownik wybiera aktywny semestr w ustawieniach; ekrany, widget i powiadomienia korzystają wyłącznie z jego planu.

Model semestru:

- nazwa, np. „Semestr zimowy 2026/27”;
- data rozpoczęcia;
- data zakończenia;
- oznaczenie pierwszego tygodnia: A lub B.

Kalkulator tygodnia powinien:

1. sprawdzić, czy data mieści się w zakresie semestru;
2. znaleźć poniedziałek tygodnia zawierającego datę rozpoczęcia semestru;
3. obliczyć liczbę pełnych tygodni od tego poniedziałku i naprzemiennie przypisywać A/B;
4. zastosować ostatnią korektę „Od tego tygodnia” obowiązującą dla wskazanego tygodnia, licząc naprzemienność od jej daty;
5. jeśli istnieje korekta „Tylko ten tydzień”, zastosować ją zamiast wyniku z poprzedniego kroku.

Każda korekta dotyczy jednego semestru i wskazuje poniedziałek tygodnia, oznaczenie A/B oraz zakres. Zmiana pojedyncza nie wpływa na kolejny tydzień. Zmiana przyszła zaczyna nową sekwencję; następna zmiana przyszła może ją ponownie przestawić. Dla jednego tygodnia i zakresu obowiązuje najwyżej jedna korekta, którą można edytować lub usunąć. Tydzień A/B jest wspólny dla wszystkich kierunków; zmiany konkretnych wystąpień są odrębną funkcją.

Przykład: pierwszy tydzień semestru to A, więc następny to B. Jeśli trzeci tydzień zostanie jednorazowo oznaczony B, czwarty nadal będzie B według automatycznej sekwencji. Jeśli trzeci tydzień zostanie oznaczony B „Od tego tygodnia”, czwarty będzie A.

Poza zakresem semestru aplikacja powinna jasno pokazać, że nie ma aktywnego semestru. Nie należy opierać działania wyłącznie na numerze tygodnia ISO, ponieważ uczelniana numeracja może zaczynać się w innym miejscu.

Ustawienia semestrów umożliwiają dodanie, edycję, wybór i usunięcie semestru. Usunięcie wymaga potwierdzenia i usuwa dane tego semestru. Gdy użytkownik usunie aktywny semestr, aplikacja wybiera inny istniejący semestr albo otwiera kreator, jeśli nie ma już żadnego.

## 8. Kierunki

Model `Course`:

- `id`;
- `semesterId`;
- `name`;
- `color`.

Aplikacja nie jest ograniczona do dwóch kierunków. Kolor kierunku jest widoczny na liście zajęć, w filtrach i w widgetach.

## 9. Prowadzący

Model `Teacher`:

- `id`;
- `semesterId`;
- `name`.

Pola `email` i `academicTitle` można dodać później, ale nie są potrzebne w MVP. Warto zachować prowadzących jako osobną tabelę, aby autouzupełnianie nie tworzyło wielu kopii tej samej osoby.

## 10. Model danych zajęć

Model aplikacyjny `ClassItem` oraz odpowiadający mu `ClassEntity` powinien zawierać:

- `id`;
- `semesterId`;
- `name`;
- `type`;
- `courseId`;
- `teacherId` — opcjonalne;
- `dayOfWeek`;
- `startTime`;
- `endTime`;
- `room` — opcjonalne;
- `building` — opcjonalne;
- `group` — opcjonalne;
- `recurrence`;
- `date` — używane dla zajęć jednorazowych;
- `classNote` — opcjonalna notatka wspólna dla wszystkich wystąpień;

`recurrence` przyjmuje wartości:

```text
EVERY_WEEK
A_WEEK
B_WEEK
ONCE
```

W kodzie warto używać nazwy `ClassItem` zamiast samego `Class`, ponieważ `Class` koliduje znaczeniowo z podstawowym typem Kotlin/Java i utrudnia czytanie kodu.

Model korekty tygodnia `WeekOverride` zawiera `id`, `semesterId`, `weekStartDate` (poniedziałek), `weekType` (`A` lub `B`) oraz `scope` (`ONE_WEEK` albo `FROM_WEEK`). Korekty są przechowywane osobno od zajęć i obejmowane eksportem.

Model notatki do konkretnego wystąpienia `OccurrenceNote` zawiera:

- `id`;
- `classId`;
- `occurrenceDate` — konkretna data zajęć;
- `body`.

W bazie obowiązuje najwyżej jedna notatka do danego wystąpienia zajęć. Notatka do zajęć jest przechowywana przy `ClassEntity`, a `OccurrenceNote` pozostaje osobną tabelą, aby zmiana jednej daty nie modyfikowała pozostałych wystąpień.

## 11. Zmiany pojedynczych wystąpień

Stały wpis tygodniowy nie wystarcza do obsługi odwołanych i przeniesionych zajęć. Model `OccurrenceChange` opisuje zmianę jednego terminu zajęć cyklicznych:

- `id`;
- `classId`;
- `originalDate` — data wystąpienia wynikająca z planu cyklicznego;
- `kind`: `CANCELLED` albo `MODIFIED`;
- `targetDate` — używane przy przeniesieniu na inną datę;
- opcjonalnie nowa godzina rozpoczęcia i zakończenia, sala, budynek, prowadzący oraz notatka.

Z ekranu szczegółów konkretnego terminu użytkownik może wybrać:

- „Odwołaj ten termin”;
- „Zmień tylko ten termin”;
- „Przenieś ten termin”;
- „Przywróć termin”, jeśli wcześniej zapisano zmianę.

Odrabianie albo inne dodatkowe spotkanie jest zapisywane jako `ClassEntity` z `recurrence = ONCE` i konkretną datą. Formularz może skopiować nazwę, kierunek, prowadzącego i typ z istniejących zajęć, ale zapis pozostaje niezależny od cyklu.

Zmiany wystąpień są stosowane po rozwinięciu planu cyklicznego i przed wykrywaniem kolizji. Usunięcie albo edycja zmiany nie modyfikuje bazowego wpisu zajęć.

## 12. Kolizje

Kolizja występuje, gdy dwa aktywne zajęcia tego samego dnia mają przedziały czasu, które się nakładają. Dotyczy to także zajęć z dwóch różnych kierunków. Przedziały stykające się końcem i początkiem, np. 10:00–11:00 oraz 11:00–12:00, nie są kolizją.

Przykład:

```text
Programowanie  10:00–11:30
Matematyka     11:00–12:30
```

Wynik: kolizja trwająca 30 minut.

Kolizję należy oznaczyć przy obu zajęciach i pokazać jej czas trwania po wejściu w szczegóły. Jest to ostrzeżenie i informacja o ograniczeniu planu, a nie komunikat o winie użytkownika. Aplikacja nie proponuje zmiany terminu, nie wybiera rozwiązania za użytkownika i nie modyfikuje planu automatycznie. Użytkownik sam decyduje, czy skontaktować się z uczelnią, opuścić jedno z zajęć albo ręcznie zapisać zmianę wystąpienia.

## 13. Widget

Widget pokazuje dzisiejsze zajęcia bez otwierania aplikacji.

### Mały widget

Pokazuje datę oraz jedno lub dwa najbliższe zajęcia, np. godzinę, nazwę, kierunek i salę.

### Duży widget

Pokazuje pełną listę dzisiejszych zajęć z godzinami, kierunkami, salami i opcjonalnie prowadzącymi.

Kliknięcie widgetu otwiera ekran „Dzisiaj”.

Widget powinien otrzymywać żądanie odświeżenia po:

- zmianie danych planu;
- dodaniu, edycji lub usunięciu zajęć;
- zmianie semestru, kierunku lub korekty tygodnia A/B;
- zmianie rozmiaru albo konfiguracji widgetu;
- okresowym sygnale systemu jako zabezpieczeniu przy zmianie daty.

System może opóźnić odświeżenie po północy. Widget nie obiecuje zmiany dokładnie o 00:00; przy każdym odświeżeniu odczytuje aktualną datę i aktywny plan. Nie należy używać ciągłego serwisu w tle, dokładnych alarmów ani odświeżania co minutę. Tekst „Następne: 10:15” jest wystarczający i ogranicza zużycie baterii. Układ musi dostosować liczbę widocznych zajęć do faktycznego rozmiaru widgetu.

### 13.1. Etapowy plan implementacji widgetów

Każdy etap kończy się kompilującym przyrostem i testem logiki, którą da się uruchomić bez launchera. Nie rozpoczynać kolejnego etapu, jeśli poprzedni nie spełnia swoich kryteriów.

Status: etapy 1-7 mają implementację w kodzie, testy JVM i kompilację debug. Etap 8 wymaga jeszcze odbioru na emulatorze lub urządzeniu.

#### Etap 1: stan widgetu i wspólna ścieżka planu

1. Utworzyć pakiet `widget` w module `app`.
2. Dodać czyste modele `WidgetUiState` dla stanów: brak aktywnego semestru, data poza semestrem, brak zajęć, plan dostępny i błąd odczytu.
3. Utworzyć `WidgetPlanLoader`, który otrzymuje `MakRepository`, `ActivePlanProvider` i `Clock`. Loader odczytuje aktywny semestr, pobiera jego dane, mapuje je raz przez `toActivePlanData()` i oblicza plan dla `LocalDate.now(clock)`.
4. Nie wywoływać DAO ani `ScheduleResolver` bezpośrednio z widgetu. Widget korzysta z tej samej instancji logiki co ekrany przez `ActivePlanProvider`.
5. Utworzyć czysty `WidgetPresenter`, który zamienia `ActivePlan` na `WidgetUiState`. Presenter ustala kolejność zajęć, skrócone metadane, oznaczenie tygodnia, informację o kolizji i wskaźnik notatki.

Kryterium etapu: test JVM potwierdza zgodność identyfikatorów i kolejności zajęć z wynikiem `ActivePlanProvider` oraz wszystkie stany puste.

#### Etap 2: rejestracja i minimalny widget

1. Dodać zależności `androidx.glance:glance-appwidget` i `androidx.glance:glance-material3` z istniejącego katalogu wersji.
2. Utworzyć `TodayWidget` dziedziczący po `GlanceAppWidget` oraz `TodayWidgetReceiver` dziedziczący po `GlanceAppWidgetReceiver`.
3. Zarejestrować receiver w `AndroidManifest.xml` dla `APPWIDGET_UPDATE` i wskazać metadane providera.
4. Dodać `res/xml/today_widget_info.xml` z `initialLayout` biblioteki Glance, kategorią `home_screen`, zmianą rozmiaru w obu osiach oraz wartościami `targetCellWidth`, `targetCellHeight`, `minWidth`, `minHeight`, `minResizeWidth` i `minResizeHeight`.
5. Użyć `SizeMode.Responsive` z co najmniej dwoma nazwanymi progami rozmiaru: małym i dużym. Wartości progów zapisać w jednym miejscu i dobrać po sprawdzeniu launchera na Androidzie 12 lub nowszym.
6. Pierwsza wersja renderuje datę, oznaczenie tygodnia oraz jeden z jednoznacznych stanów pustych. Nie dodawać jeszcze listy zajęć.

Kryterium etapu: widget można dodać do ekranu głównego, zmienić jego rozmiar i zobaczyć poprawny stan dla pustej bazy oraz braku zajęć.

#### Etap 3: mały widget

1. Dla małego progu pokazać datę, tydzień A/B oraz przewijaną listę dzisiejszych zajęć. Dostępna wysokość steruje gęstością wierszy, nie liczbą pobranych pozycji.
2. Każdy wiersz zawiera godzinę rozpoczęcia, nazwę, kierunek i salę. Pomija puste metadane zamiast zostawiać separatory.
3. Długą nazwę zajęć ograniczyć do jednej linii, a drugorzędne informacje do jednej linii. Nie używać poziomego przewijania.
4. Kolor kierunku może być paskiem pomocniczym, ale nazwa kierunku pozostaje tekstem. Kolor nie może być jedyną informacją.
5. Jeśli zajęcia mają kolizję albo notatkę, pokazać krótki tekst lub dostępny wskaźnik. Pełną treść pozostawić ekranowi szczegółów w aplikacji.

Kryterium etapu: przy minimalnym rozmiarze wszystkie teksty mieszczą się bez nakładania, a brak miejsca ogranicza liczbę pozycji zamiast obcinać cały układ.

#### Etap 4: duży widget i rozmiary pośrednie

1. Dla dużego progu pokazać pełniejszą, przewijaną listę dzisiejszych zajęć. Próg rozmiaru steruje gęstością wierszy i metadanymi, a nie stałym limitem liczby wszystkich zajęć.
2. Duży wiersz zawiera godzinę, nazwę, kierunek, salę oraz opcjonalnie prowadzącego. Notatkę przedstawia wskaźnik, nie pełny wielowierszowy tekst.
3. Jeśli zajęć jest więcej niż mieści widoczny obszar, lista ma przewijać się w `LazyColumn`; nie dodawać stopki „Jeszcze {liczba}” ani nie ściskać wierszy.
4. Dla rozmiaru pośredniego użyć małego albo dużego wariantu wybranego przez `SizeMode.Responsive`. Nie tworzyć osobnego układu dla każdego możliwego wymiaru.
5. Sprawdzić promień tła widgetu, padding systemowy, motyw jasny i ciemny oraz kontrast małego tekstu.

Kryterium etapu: zmiana rozmiaru przełącza układ bez utraty daty, stanu tygodnia i najbliższych zajęć.

#### Etap 5: otwieranie aplikacji

1. Kliknięcie tła, nagłówka albo pustego stanu otwiera `MainActivity` na ekranie „Dzisiaj”.
2. Użyć jawnego intentu lub obsługiwanej akcji Glance. Trasa docelowa nie może zależeć od ostatnio otwartego ekranu aplikacji.
3. W pierwszej wersji kliknięcie wiersza także otwiera ekran „Dzisiaj”. Otwieranie szczegółów konkretnego wystąpienia dodać tylko po wprowadzeniu stabilnego kontraktu deep linków.
4. Wielokrotne szybkie kliknięcie nie tworzy kilku kopii aktywności w stosie.

Kryterium etapu: aplikacja uruchomiona z każdego wariantu widgetu pokazuje właściwą datę na ekranie „Dzisiaj”.

#### Etap 6: odświeżanie po zmianach i zmianie dnia

1. Wprowadzić niezależny od Glance interfejs żądania odświeżenia. Jego implementacja w `widget` wywołuje `TodayWidget().updateAll(context)`.
2. Wywoływać żądanie na jednej granicy po udanym zapisie danych wpływających na plan. Nie rozrzucać wywołań po ekranach i nie uruchamiać aktualizacji przed zakończeniem transakcji.
3. Odświeżać widget po zmianie zajęć, wystąpienia, notatki, semestru, aktywnego semestru, kierunku albo korekty tygodnia.
4. Przy każdym odświeżeniu odczytać datę przez wstrzyknięty `Clock`. Nie przechowywać bieżącej daty ani planu wyłącznie w pamięci procesu.
5. Ustawić `updatePeriodMillis` nie częściej niż raz na godzinę jako zabezpieczenie zmiany dnia. Nie dodawać WorkManagera, dokładnych alarmów ani osobnego serwisu, dopóki pomiary nie wykażą rzeczywistej potrzeby.
6. Zaakceptować opóźnienie systemowe. Tekst i dokumentacja nie obiecują aktualizacji dokładnie o północy.

Kryterium etapu: zapis w aplikacji aktualizuje wszystkie instancje widgetu, a okresowy sygnał odczytuje nową datę bez uruchamiania ciągłego procesu.

#### Etap 7: błędy, odporność i wydajność

1. Błąd odczytu danych pokazuje krótki stan „Nie udało się wczytać planu” i pozwala otworzyć aplikację. Nie wyświetla surowego wyjątku.
2. Brak aktywnego semestru prowadzi do aplikacji, gdzie użytkownik może przejść konfigurację.
3. Jedno odświeżenie mapuje `SemesterWithData` do `ActivePlanData` tylko raz. Nie obliczać kolizji dla dat ani wariantów rozmiaru, których widget nie prezentuje.
4. Wiele instancji widgetu może korzystać z tego samego obliczonego stanu dnia, jeśli nie mają osobnej konfiguracji.
5. Nie zapisywać planu użytkownika w preferencjach Glance. Room pozostaje źródłem danych.

Kryterium etapu: usunięcie semestru, pusta baza, błąd odczytu i ponowne utworzenie procesu nie pozostawiają starego lub pustego `RemoteViews` bez wyjaśnienia.

#### Etap 8: testy i odbiór

1. Testy JVM obejmują stany `WidgetUiState`, sortowanie, pełną listę pozycji, wskaźnik notatki, kolizję i datę poza semestrem.
2. Test integracyjny potwierdza, że loader korzysta z `ActivePlanProvider` i zwraca ten sam zestaw wystąpień co ekran „Dzisiaj” dla wstrzykniętej daty.
3. Nie powielać testów reguł tygodni A/B, zmian wystąpień i kolizji w testach Glance.
4. Uruchomić `gradlew.bat test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug`.
5. Na urządzeniu albo emulatorze sprawdzić dodanie, usunięcie i ponowne dodanie widgetu, mały i duży rozmiar, zmianę rozmiaru, motyw jasny i ciemny, pusty dzień, wiele zajęć, kolizję, notatkę oraz otwarcie aplikacji.
6. Zmienić dane planu przy widocznym widgetcie i potwierdzić aktualizację bez ponownego dodawania widgetu.

Kryterium zakończenia wersji 0.2: mały i duży widget pokazują plan z `ActivePlanProvider`, reagują na zmiany danych, ponownie odczytują datę, otwierają ekran „Dzisiaj” i pozostają czytelne we wszystkich zadeklarowanych rozmiarach.

### 13.2. Plan unowocześnienia widgetu

Zmiany mają poprawić hierarchię informacji, czytelność i wykorzystanie dostępnego miejsca. Widget zachowuje jasne tło, wysoki kontrast i prosty układ. Nie dodawać zdjęć, gradientów, cieni ani ozdobnych ikon.

Status: obecny widget działa na launcherze, ale duży wariant wykorzystuje przestrzeń jak wariant mały. Metadane są zbyt ciasne, a kolor kierunku zapisany w stanie widgetu nie jest wykorzystywany w układzie.

#### Etap 1: nagłówek i wiersze zajęć

1. Pokazać datę jako główny tekst nagłówka o rozmiarze 16 sp.
2. Pokazać tydzień A/B jako małą etykietę z delikatnym tłem.
3. Umieścić liczbę zajęć obok etykiety tygodnia albo wyrównać ją do prawej strony nagłówka.
4. Dodać do wiersza zajęć wąski pasek w kolorze kierunku. Zachować tekstową nazwę kierunku, ponieważ kolor nie może być jedynym nośnikiem informacji.
5. Użyć stałej kolumny czasu. Obok niej pokazać nazwę zajęć oraz osobny wiersz metadanych z salą i prowadzącym.
6. Pokazać kolizję i notatkę jako krótkie etykiety tekstowe o czytelnym kontraście.
7. Oddzielić zajęcia subtelnymi separatorami. Nie umieszczać każdego zajęcia w osobnej karcie.
8. Dopasować tło do systemowego promienia widgetów Androida.

#### Etap 2: układ responsywny

1. Traktować widget jako kompaktowy, gdy ma mniej niż 240 dp szerokości albo mniej niż 160 dp wysokości.
2. W wariancie kompaktowym pokazać przewijaną listę dzisiejszych zajęć. Pokazać nazwę, czas i salę, pominąć prowadzącego oraz osobną etykietę notatki, ale zachować alert kolizji.
3. Jeśli wariant kompaktowy nie mieści wszystkich zajęć, użyć przewijanej listy zamiast tekstu „Jeszcze {liczba}”.
4. W wariancie rozszerzonym pokazać przewijaną listę dzisiejszych zajęć wraz z salą, prowadzącym i statusem kolizji albo notatki.
5. Wiersze powinny wykorzystać pełną szerokość widgetu. Wariant rozszerzony ma używać większych odstępów i pełniejszych metadanych, a nie tylko zwiększać wysokość listy.
6. Ograniczać prowadzącego i lokalizację wielokropkiem. Nie ucinać czasu. Nazwa zajęć może zająć dwa wiersze, jeśli pozwala na to wysokość wariantu.
7. Ograniczyć maksymalny rozmiar widgetu do 360 na 420 dp, aby launcher nie tworzył nadmiernie pustego układu.
8. Dodać statyczny podgląd używany przez systemowy wybór widgetów.

#### Etap 3: zachowanie i odświeżanie

1. Zachować kliknięcie całego widgetu i każdego wiersza prowadzące do ekranu „Dzisiaj”. Nie dodawać osobnych akcji w tej wersji.
2. Ustawić `updatePeriodMillis` na 60 minut. Natychmiastowe odświeżanie po zmianie danych nadal realizować przez obserwację Room.
3. Przy każdym odświeżeniu pobierać bieżącą datę przez `Clock` i używać wspólnego `ActivePlanProvider`.
4. Sprawdzić zimny i ciepły start po kliknięciu widgetu. Trasa docelowa nie może zależeć od ekranu otwartego wcześniej w aplikacji.
5. Nie gwarantować aktualizacji dokładnie o północy i nie dodawać dokładnych alarmów ani stałego procesu w tle.

#### Etap 4: Gradle i dokumentacja

1. Zachować Gradle 9.7.1, jeśli pełny zestaw kontroli nadal przechodzi po zmianach widgetu.
2. Dodać oficjalną sumę SHA-256 dystrybucji, `validateDistributionUrl=true` oraz limit czasu pobierania do konfiguracji wrappera.
3. Uruchomić Gradle z `--warning-mode all`. Przypisać ostrzeżenia o przestarzałych API do kodu projektu albo użytych wtyczek.
4. Zaktualizować `STACK.md` i `JOURNAL.md`, aby zapisać wersję Gradle, wynik kontroli oraz powód aktualizacji.
5. Zaktualizować stan wdrożenia w tym pliku dopiero po zakończeniu kontroli na launcherze.

#### Testy i odbiór

Testy JVM powinny obejmować:

- wariant kompaktowy i rozszerzony;
- przewijaną listę wszystkich zajęć bez stopki „Jeszcze {liczba}”;
- pusty dzień, brak semestru, datę poza semestrem i błąd odczytu;
- kolizję, notatkę i długie metadane;
- zgodność kolejności zajęć z `ActivePlanProvider`.

Po implementacji uruchomić `gradlew.bat test compileDebugAndroidTestKotlin lintDebug assembleDebug`.

Na launcherze Androida 12 lub nowszego sprawdzić:

- mały, duży i pośredni rozmiar oraz zmianę rozmiaru w obu osiach;
- jasny i ciemny motyw;
- brak semestru, pusty dzień, jedno i wiele zajęć;
- długą nazwę zajęć, sali oraz prowadzącego;
- kolizję i notatkę;
- odświeżenie po zmianie danych bez ponownego dodawania widgetu;
- otwarcie ekranu „Dzisiaj” po zimnym i ciepłym starcie.

Kryterium zakończenia: żaden wariant nie ucina czasu, nie nakłada tekstów i nie wymaga koloru do zrozumienia informacji. Duży wariant wykorzystuje dodatkowe miejsce na pełniejsze metadane i czytelniejsze odstępy.

### 13.3. Plan separatorów i licznika kolizji widgetu

Zmiany naprawiają nakładanie separatora na treść elementu `LazyColumn` i dodają do nagłówka liczbę unikalnych kolizji. Zadania wykonywać kolejno. Każdy etap powinien kończyć się kompilującym przyrostem.

Postęp: etapy 1-4 są zaimplementowane i objęte testami JVM oraz testem kompozycji Glance. Etap 5 ma zakończoną walidację Gradle. Odbiór na launcherze pozostaje do wykonania.

#### Etap 1. Poprawna struktura elementu listy i separator

1. W `MakTodayWidget.kt` wydziel prywatny komponent pojedynczego elementu listy z jednym głównym `Column`.
2. Wewnątrz tego komponentu umieść kolejno:
   - istniejący `WidgetOccurrenceRow`,
   - odstęp `2.dp`,
   - poziomy separator o wysokości `1.dp` i pełnej dostępnej szerokości,
   - odstęp `2.dp`.
3. Użyj koloru `surfaceVariant` dla separatora, aby był widoczny, ale nie konkurował z informacją o kolizji.
4. Nie pokazuj separatora ani końcowego odstępu po ostatnim wpisie.
5. W `itemsIndexed` zwracaj wyłącznie ten jeden komponent. Kilka elementów najwyższego poziomu może zostać ułożonych przez Glance jeden na drugim, co obecnie powoduje kreski przechodzące przez godzinę i treść zajęć.
6. Zachowaj pionowy znacznik koloru zajęć, przewijanie listy i obsługę kliknięcia.

Kryterium zakończenia: poziomy separator znajduje się tylko między wpisami i nie przecina godziny, nazwy, metadanych ani komunikatu o kolizji.

#### Etap 2. Jednoznaczne liczenie kolizji

1. Dodaj `collisionCount: Int` do `WidgetUiState.Ready`.
2. Wylicz wartość w `WidgetPresenter` na podstawie `ActivePlan.collisions`, a nie przez sumowanie komunikatów przypisanych do wierszy.
3. Dla każdej kolizji zbuduj stabilny klucz zawierający:
   - posortowaną parę identyfikatorów wystąpień,
   - początek części wspólnej,
   - koniec części wspólnej.
4. Usuń duplikaty kluczy i użyj liczby pozostałych elementów jako `collisionCount`.
5. Jedna para zajęć nachodząca na siebie w jednym przedziale ma dawać jedną kolizję. Trzy różne pary mają dawać trzy kolizje.

Kryterium zakończenia: ta sama kolizja reprezentowana przy obu wpisach jest liczona tylko raz.

#### Etap 3. Licznik kolizji w nagłówku

1. Rozszerz `WidgetHeaderDetails` o parametr `collisionCount`.
2. Dla stanu `Ready` przekaż liczbę z modelu. Dla stanu pustego przekaż zero.
3. Gdy liczba jest większa od zera, pokaż ją obok liczby zajęć z odstępem `6.dp`.
4. Użyj koloru błędu, pogrubienia i rozmiaru `11.sp`. Nie stosuj kolejnego pilla, aby komunikat miał inną wagę wizualną niż oznaczenie tygodnia.
5. Zastosuj poprawne formy:
   - `1 kolizja`,
   - `2 kolizje`, `3 kolizje`, `4 kolizje`,
   - `5 kolizji` i pozostałe wartości.
6. Dla zera nie pokazuj żadnej informacji o kolizjach.
7. Zachowaj pojedynczy wiersz szczegółów nagłówka oraz istniejący pill tygodnia.

Kryterium zakończenia: nagłówek może pokazać na przykład `3 zajęcia` i `1 kolizja`, bez pogorszenia czytelności daty i oznaczenia tygodnia.

#### Etap 4. Test struktury listy

1. Dodaj stabilne znaczniki testowe separatorów zależne od indeksu wpisu.
2. W teście kompozycji Glance sprawdź, że separator występuje między pierwszym i drugim wpisem oraz nie występuje po ostatnim.
3. Zachowaj test potwierdzający obecność wszystkich elementów w przewijanej liście i brak tekstu `Jeszcze N`.
4. Test powinien wykrywać regresję, w której wiersz i separator ponownie stają się oddzielnymi elementami najwyższego poziomu nakładanymi przez kontener.

Kryterium zakończenia: test opisuje strukturę elementu listy, a nie tylko obecność tekstów na ekranie.

#### Etap 5. Test licznika i weryfikacja całości

1. Dodaj test prezentera, w którym jedna para nachodzących zajęć daje `collisionCount = 1`.
2. Dodaj test prezentera dla trzech unikalnych par kolizji.
3. Dodaj test nagłówka dla wartości zero oraz wartości dodatniej.
4. Zaktualizuj wszystkie konstruktory `WidgetUiState.Ready` w testach i podglądach.
5. Uruchom pełny zestaw testów Gradle po zakończeniu implementacji.
6. Sprawdź ręcznie mały i duży widżet w launcherze: przewijanie, położenie separatorów, licznik w nagłówku i komunikaty przy wpisach.
7. Po akceptacji uzupełnij `JOURNAL.md` i status wykonania tego planu.

Kryterium zakończenia: automatyczne testy potwierdzają unikalne liczenie kolizji i poprawną strukturę listy, a kontrola w launcherze potwierdza brak nakładających się kresek.

## 14. Ustawienia i dane

Ustawienia powinny zawierać:

- listę semestrów z możliwością dodania, wyboru, konfiguracji i usunięcia;
- konfigurację aktywnego semestru i korekty tygodni A/B;
- listę kierunków i ich kolorów;
- ustawienia powiadomień;
- eksport planu;
- import planu;
- informację o wersji aplikacji.

Eksport i import mogą używać lokalnego pliku JSON. Format powinien mieć pole `schemaVersion`, aby można było zmieniać model danych bez utraty zgodności ze starszymi eksportami.

Eksport jest dostępny od wersji 0.1. Użytkownik wybiera miejsce zapisu przez systemowy wybór dokumentu. Plik zawiera wszystkie semestry oraz przypisane do nich kierunki, prowadzących, zajęcia, korekty tygodni, notatki i zmiany wystąpień. Import pojawia się w wersji 0.3.

Import powinien:

- sprawdzić poprawność struktury pliku;
- pokazać podsumowanie danych przed zapisaniem;
- obsłużyć konflikt istniejących identyfikatorów;
- nie nadpisywać bieżącego planu bez potwierdzenia.

## 15. Modularność i orientacyjny podział kodu

Poniższy podział pokazuje odpowiedzialności. Nazwy plików i katalogów można dopasować podczas implementacji bez zmiany granic między danymi, logiką planu, interfejsem i widgetem.

```text
app/
├── data/
│   ├── database/
│   │   ├── AppDatabase
│   │   ├── ClassDao
│   │   ├── TeacherDao
│   │   ├── CourseDao
│   │   ├── SemesterDao
│   │   ├── WeekOverrideDao
│   │   ├── OccurrenceNoteDao
│   │   └── OccurrenceChangeDao
│   ├── entity/
│   │   ├── ClassEntity
│   │   ├── TeacherEntity
│   │   ├── CourseEntity
│   │   ├── SemesterEntity
│   │   ├── WeekOverrideEntity
│   │   ├── OccurrenceNoteEntity
│   │   └── OccurrenceChangeEntity
│   └── repository/
├── domain/
│   ├── WeekCalculator
│   ├── ScheduleResolver
│   └── CollisionDetector
├── ui/
│   ├── today/
│   ├── schedule/
│   ├── edit/
│   ├── settings/
│   └── components/
├── widget/
│   ├── TodayWidget
│   └── TodayWidgetReceiver
└── export/
```

Logika obliczania planu, tygodni i kolizji powinna być niezależna od Compose. Ułatwi to testowanie i zapewni spójne dane na ekranie oraz w widgetach. ViewModele należą do `ui`. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Pakiet `export` nie jest częścią Room. Szczegóły granic: `ARCHITECTURE.md`.

## 16. Kolejność wdrożenia

### Wersja 0.1 — działający plan offline

- podstawowa konfiguracja projektu;
- Room, eksport schematu bazy i przygotowanie testów przyszłych migracji;
- kierunki z kolorami;
- semestr, automatyczny kalkulator tygodni A/B oraz korekty pojedyncze i „Od tego tygodnia”;
- kreator pierwszej konfiguracji oraz zarządzanie wieloma odizolowanymi semestrami;
- dodawanie, edycja i usuwanie zajęć;
- zajęcia cotygodniowe oraz przypisane do tygodnia A lub B;
- ekran „Dzisiaj”;
- ekran „Plan” z nawigacją między tygodniami i oznaczeniem korekt;
- widok miesięcznego kalendarza z wyborem dnia;
- filtrowanie po kierunku;
- odwoływanie, zmiana i przenoszenie pojedynczego wystąpienia;
- dodawanie zajęć jednorazowych, w tym odrabiania;
- notatki wspólne dla zajęć i notatki przypięte do konkretnych wystąpień;
- eksport JSON z `schemaVersion` przez systemowy wybór pliku;
- walidacja formularza, stany puste i podstawowe wymagania dostępności.

### Wersja 0.2 — szybki dostęp

- widget mały;
- widget duży;
- odświeżanie widgetu po zmianach danych;
- otwieranie ekranu „Dzisiaj” po kliknięciu;
- ponowny odczyt daty przy odświeżaniu oraz okresowe odświeżenie jako zabezpieczenie;
- układ widgetu dostosowany do dostępnego rozmiaru.

Szczegółowa kolejność i kryteria znajdują się w sekcji 13.1.

### Wersja 0.3 — kolizje i import danych

- wykrywanie kolizji;
- import JSON;
- odczyt obsługiwanych wersji formatu eksportu podczas importu.

## 17. Kryteria ukończenia MVP

MVP można uznać za gotowe, gdy użytkownik potrafi:

1. utworzyć semestr i co najmniej dwa kierunki;
2. dodać zajęcia z pełnymi podstawowymi danymi;
3. zobaczyć właściwe zajęcia dla bieżącej daty i automatycznie wyliczonego tygodnia A/B;
4. zmienić oznaczenie jednego tygodnia, ustawić zmianę od wskazanego tygodnia oraz usunąć korektę;
5. przejść do dowolnego dnia i tygodnia w zakresie semestru;
6. filtrować plan po kierunku;
7. edytować i usuwać wpisy;
8. przełączyć plan między listą i kalendarzem oraz wybrać dzień miesiąca;
9. odwołać, zmienić, przenieść i przywrócić pojedynczy termin bez zmiany cyklu;
10. dodać jednorazowe zajęcia dla konkretnej daty;
11. dodać notatkę do wszystkich wystąpień zajęć;
12. dodać inną notatkę tylko do wybranej daty;
13. zamknąć i ponownie otworzyć aplikację bez utraty danych;
14. wyeksportować plan wraz z korektami, zmianami wystąpień i notatkami do pliku JSON;
15. korzystać z aplikacji bez połączenia z internetem;
16. obsłużyć podstawowe czynności na ekranie o szerokości 320–390 px, bez obciętych akcji i poziomego przewijania, z etykietami semantycznymi, widocznym focusem oraz bez utraty wpisanych danych.

## 18. Funkcje poza pierwszym zakresem

Na początku nie dodawać:

- logowania i kont użytkowników;
- backendu, Firebase i synchronizacji w chmurze;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu.

## 19. Sugestie projektowe

1. **Utrzymać semestr i korekty tygodni w MVP.** Rytm A/B wymaga punktu odniesienia, a ręczne zmiany muszą być niezależne od zmian pojedynczych wystąpień.
2. **Obsługiwać zmiany wystąpień bez modyfikowania cyklu.** Odwołanie, przeniesienie i odrabianie dotyczą konkretnej daty i nie mogą zmieniać bazowego wpisu zajęć.
3. **Rozdzielić warstwę danych od logiki harmonogramu.** Jeden `ScheduleResolver` powinien wyliczać aktywne zajęcia dla konkretnej daty. Z tego samego wyniku powinny korzystać ekran, kolizje i widget.
4. **Stosować spójne reguły dat.** Tygodnie zaczynają się w poniedziałek, a ekran „Dzisiaj” odczytuje datę przy otwarciu lub wznowieniu. Widget może aktualizować się z opóźnieniem narzuconym przez system.
5. **Zacząć od prostego formularza.** Prowadzący, sala, budynek, grupa i notatka mogą być opcjonalne, aby dodanie podstawowego zajęcia trwało kilka sekund.
6. **Dodać testy logiki domenowej.** Szczególnie sprawdzić początek semestru w środku tygodnia, pojedynczą korektę, zmianę od wskazanego tygodnia, nakładanie korekt, odrzucenie zajęć przechodzących przez północ i kolizje.
7. **Chronić plan od pierwszego wydania.** Eksport JSON i zachowane schematy Room pozwalają zabezpieczyć dane przed późniejszymi zmianami modelu.
8. **Utrzymać mały zakres pierwszego wydania.** Import i kolizje można wdrażać etapami, gdy podstawowy przepływ dodawania, zmiany i przeglądania planu będzie stabilny.
9. **Budować modułowo, ale bez przedwczesnego podziału na moduły Gradle.** Pakiety i interfejsy wystarczą do szybkich zmian, a osobny moduł Gradle warto dodać dopiero przy niezależnym cyklu życia części projektu.
10. **Rozdzielić notatkę wspólną od notatki do wystąpienia.** Dzięki temu dopisek „przynieść projektor” może dotyczyć wszystkich zajęć, a „kolokwium” tylko jednej daty.
