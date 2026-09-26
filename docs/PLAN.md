# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są trzy kroki: edycja kierunków (I-21), wybór koloru z palety barw (I-22) i drobne poprawki z audytu z 2026-09-25 (I-20). Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Edycja globalnych kierunków (I-21)

Problem: nazwy i koloru kierunku nie da się zmienić po utworzeniu. Podpowiedź na ekranie semestru odsyła do edycji kierunku, której nie ma.

Granice: bez usuwania globalnych kierunków i bez zmian w bazie. Zapis używa istniejącego `SemesterRepository.saveStudyProgram`, który dla rekordu z identyfikatorem różnym od zera aktualizuje kierunek. Widget odświeża się sam przez `InvalidationTracker` na tabeli `study_programs`, a alarmy przez `observeActivePlanData`, więc nie dodawaj osobnego odświeżania.

Kolejność:

1. `ui/programs/StudyProgramsViewModel` (`@KoinViewModel`, zależności: `SemesterRepository`, `FeedbackSink`): lista `StudyProgramUi(id, name, color)` z `observeStudyPrograms`, stan edycji (`editingId`, `nameDraft`, `colorDraft`, `nameError`, `isSaving`), `openEdit(id)`, `updateName`, `updateColor`, `save()` i efekt `CloseEditor` po sukcesie. Pusta nazwa po przycięciu daje błąd „Podaj nazwę kierunku.” bez zapisu. Błąd zapisu publikuje „Nie udało się zapisać kierunku.” i zostawia szkic. Sukces publikuje „Zapisano kierunek”. `CancellationException` przekazuj dalej.
2. Trasy `settings/programs` i `settings/programs/{programId}` w `MakRoutes`, tytuły „Kierunki” i „Edytuj kierunek”. ViewModel twórz w `MainActivity` jak pozostałe i przekaż do `MakNavHostApp`.
3. Ekran listy: `MakScreenIntro` „Nazwa i kolor kierunku są wspólne dla wszystkich semestrów.”, wiersze z kropką 12 dp w kolorze kierunku, nazwą i ikoną przejścia, minimalna wysokość 48 dp, semantyka „{nazwa}, edytuj”. Stan pusty „Kierunki pojawią się po skonfigurowaniu planu.”.
4. Ekran edycji: pole „Nazwa kierunku”, obecna paleta kolorów (w kroku 2 zastąpi ją nowy komponent), „Zapisz kierunek” i „Anuluj”.
5. Ustawienia, sekcja „Plan”: `SettingsNavigationRow` „Kierunki” z wartością „Nazwy i kolory kierunków”, widoczny zawsze.
6. Kropka koloru obok nazwy: wydziel publiczny `MakColorDot(color: String)` w `ui/components`; użyj go w wierszu kierunku na ekranie „Kierunki” semestru (zamiast paska 4 dp) i jako `leadingIcon` opcji pola „Istniejący kierunek” na ekranie semestru i w kreatorze. W `MakSelectField` dodaj opcjonalny parametr `optionLeading: (@Composable (T) -> Unit)? = null` przekazywany do `DropdownMenuItem`.
7. Podpowiedź na ekranie semestru zmień na „Kierunek jest współdzielony między semestrami. Nazwę i kolor zmienisz w ustawieniach, w pozycji Kierunki.” i popraw test, który ją sprawdza.
8. Dokumentacja: `FEATURES.md`, sekcja ustawień.

Przypadki brzegowe: nazwa z samych spacji; kierunek usunięty w trakcie edycji (zapis zgłasza błąd); powtórzone nazwy na liście (numer z `distinctLabels`); długa nazwa zawija się bez obcinania kropki.

Weryfikacja: `StudyProgramsViewModelTest` (zapis, walidacja, błąd, jeden zapis przy dwukrotnym dotknięciu); test Compose listy i edycji przy 320 dp; `KoinGraphTest`. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: nazwę i kolor kierunku można zmienić z ustawień, zmiana jest widoczna we wszystkich semestrach, a kolor stoi obok nazwy kierunku w ustawieniach.

## 2. Wybór koloru z pełnej palety barw (I-22)

Wariant zaakceptowany przez użytkownika 2026-09-26: ciągły pasek odcienia, suwak jasności ograniczony do zakresu z kontrastem co najmniej 3:1 dla paska kierunku na jasnym i ciemnym tle, pole kodu szesnastkowego i podgląd.

Kolejność:

1. `ui/components/CourseColors.kt`, czysta logika bez Compose (wartości ARGB jako `Int`): `courseColorFrom(hue: Float, shade: Float): Int` dobiera jasność HSL przy nasyceniu 0,7 tak, aby luminancja względna WCAG wyniosła `0,13 + shade * 0,15` (zakres 0,13 do 0,28, bezpieczny wobec białej i ciemnej powierzchni); `hueAndShadeOf(color: Int)` zwraca odcień i jasność ograniczoną do zakresu; `isReadableCourseColor(color: Int)` sprawdza luminancję w zakresie; `parseCourseHex` i `courseHex`. Testy JVM: kontrast co najmniej 3:1 z `#FFFFFF` i z `#141218` dla odcieni co 15 stopni i obu krańców jasności; poprawny zapis i odczyt kodu; odrzucenie błędnego kodu.
2. `MakCourseColorPicker(selectedColor: String, onColorSelected: (String) -> Unit)` zamiast `MakColorPalette`: nagłówek „Kolor kierunku”; podgląd z paskiem 4 dp i pillem „Kierunek” w stylu pilla na karcie zajęć (udostępnij wewnętrznie `CoursePill`); pasek gradientu odcieni nad `Slider` odcienia (0 do 360) z opisem „Odcień”; `Slider` „Jasność” (0 do 1); pole „Kod koloru” (`#RRGGBB`). Kod spoza zakresu albo błędny pokazuje „Ten kolor będzie słabo widoczny. Wybierz inny odcień albo jasność.” i nie zmienia koloru. Suwaki mają `stateDescription` z wartością w procentach albo stopniach.
3. Zastąp paletę w kreatorze, na ekranie „Kierunki” semestru i w edycji z kroku 1. Usuń `MakColorPalette` i `MakColorPaletteOptions`, jeśli nie mają innych użyć.
4. Istniejące kolory spoza zakresu zostają zapisane bez zmian; picker ustawia suwaki na najbliższy dozwolony kolor dopiero po ruchu suwaka.
5. Dokumentacja: `FEATURES.md` (formularze kierunku) i wpis w `LOG.md`.

Przypadki brzegowe: odcień 0 i 360 dają ten sam kolor; szerokość 320 dp bez obcinania; motyw ciemny; TalkBack odczytuje nazwy suwaków; klawiatura zmienia suwaki strzałkami (standard `Slider`).

Weryfikacja: testy JVM z punktu 1; test Compose pickera przy 320 dp (pole kodu zmienia kolor, błędny kod pokazuje błąd); zrzut z emulatora w motywie jasnym i ciemnym. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: kolor kierunku wybiera się z ciągłej palety we wszystkich formularzach kierunku, a każdy wybrany kolor spełnia próg kontrastu.

## 3. Drobne poprawki z audytu z 2026-09-25 (I-20)

Każdy punkt to osobny commit.

1. Potwierdzenie usunięcia korekty tygodnia i nieużywanego kalendarza. Snackbar nie obsługuje akcji, więc użyj dialogów w stylu `CourseDeletionDialog`: `requestWeekOverrideDeletion`, `confirm...`, `cancel...` oraz to samo dla kalendarza. Treść korekty: „Korekta tygodnia od {data} zostanie usunięta. Rytm A/B wróci do automatycznego wyliczenia.” Treść kalendarza: „Kalendarz {zakres} nie jest używany przez żaden kierunek i zostanie usunięty razem ze swoimi korektami.” Akcja „Usuń” w kolorze błędu. Testy ViewModelu (brak usunięcia bez potwierdzenia) i Compose.
2. Data korekty: przy zmianie daty w formularzu normalizuj ją do poniedziałku tygodnia (`TemporalAdjusters.previousOrSame(MONDAY)`) i pokaż pod polem „Korekta obejmuje tydzień od poniedziałku {data}.”. Usuń błąd „Wybierz poniedziałek.”, który przestaje być osiągalny. Test ViewModelu dla środy.
3. Powiadomienia na Androidzie 12 i starszym: zgoda `POST_NOTIFICATIONS` istnieje od API 33. W `MainActivity` przy API poniżej 33 zamiast prośby o zgodę otwieraj `Settings.ACTION_APP_NOTIFICATION_SETTINGS` z `EXTRA_APP_PACKAGE`; przy API 33 i nowszym zostaw prośbę. Po powrocie do aplikacji stan `notificationsBlocked` odświeża się jak dotąd. Sprawdź lint (`InlinedApi` znika) i ręcznie na emulatorze API 33 lub nowszym, że zachowanie się nie zmieniło.
4. Aktualizacja `androidx.core:core-ktx` do 1.19.1 i `navigation-compose` do 2.10.2 w `gradle/libs.versions.toml`. Kotlin 2.4.20 tylko razem ze zgodnym KSP i pluginem Koin, zgodnie z `STACK.md`; jeśli zgodnych wersji nie ma, zostaw Kotlin i zapisz powód w `LOG.md`. Po zmianie pełne testy i lint.
5. Eksport: `contentResolver.openOutputStream(uri, "wt")`, aby nadpisanie istniejącego dokumentu obcinało plik.

Weryfikacja: `gradlew.bat test connectedDebugAndroidTest lintDebug`.

Kryterium zakończenia: każdy punkt wykonany albo odrzucony z uzasadnieniem w `LOG.md`.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`; I-14 czeka na decyzję o kopii zapasowej. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
