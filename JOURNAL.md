# MAK — dziennik decyzji

## 2026-09-20: Otwarcie szczegółów czeka na dane semestru

- Fakty: `OccurrenceViewModel` uruchamiał niezależną subskrypcję danych z początkową wartością `null`, a `open()` od razu odczytywał `.value`. Jeśli Room jeszcze nie zdążył wyemitować danych, otwarcie kończyło się pustym ekranem bez ponowienia. Dwa szybkie otwarcia mogły też wyścignąć się nawzajem.
- Decyzja: `open()` uruchamia zadanie, które czeka na pierwsze niepuste dane aktywnego semestru, a każde kolejne otwarcie anuluje poprzednie. Dzięki temu wygrywa ostatnie żądanie, a brak danych nie kończy działania bezpowrotnie.
- Powód: Dane semestru pochodzą z asynchronicznego przepływu Room, więc granica otwarcia nie może zależeć od tego, czy pierwsza emisja już dotarła.
- Weryfikacja: Dodano testy `openWaitsForFirstSemesterDataEmission` i `lastOpenWinsWhenDataArrivesLate` z bramkowanym repozytorium oraz `advanceUntilIdle()` po otwarciu w pozostałych przypadkach. `clean test compileDebugAndroidTestKotlin lintDebug assembleDebug` przechodzi.

## 2026-09-20: Wydzielenie `OccurrenceViewModel`

- Fakty: Logika szczegółów wystąpienia, edycji terminu i obu notatek mieszkała w `MakViewModel` razem z nawigacją, planem, semestrem i ustawieniami.
- Decyzja: Utworzono `OccurrenceViewModel` z zależnościami `MakRepository`, `ActivePlanProvider` i `FeedbackSink`. Przeniesiono do niego otwieranie i budowanie szczegółów, edycję daty, godzin i sali, walidację, zapis zmiany lub przeniesienia, odwołanie, przywracanie oraz obie notatki ze stanami zapisu i błędów. `MakViewModel` tymczasowo deleguje te operacje i nadal decyduje o powrocie do planu po usunięciu zajęć. Czyste funkcje pozostają w domenie, a `OccurrenceViewModel` jest tworzony w `MainActivity` i przekazywany do fabryki `MakViewModel`.
- Powód: Granica ViewModelu dla szczegółów oddziela logikę i stan od nadrzędnego koordynatora, co przygotowuje etap 5 bez przebudowy nawigacji w tym samym commicie.
- Odrzucone: Zmiana tras i podłączenie feedbacku operacji w tym etapie.
- Weryfikacja: Testy notatek przeniesiono do `OccurrenceViewModelTest` i dodano otwarcie prawidłowego wystąpienia, błędne argumenty oraz zapis zmiany po edycji draftu. Wspólny `FakeMakRepository` i `MainDispatcherRule` obsługują oba testy ViewModelu. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą po czyszczeniu.

## 2026-09-20: Granica modeli szczegółów wystąpienia

- Fakty: `openOccurrence` parsował identyfikator trasy `classId:date` wewnątrz ViewModelu, a modele szczegółów nie miały jawnego typu argumentów otwarcia. Trzeba było też potwierdzić, że publiczny stan szczegółów nie wystawia encji Room ani stanu nawigacji.
- Decyzja: Dodano `OccurrenceArgs(classId, date)` w `ui/occurrence` z `parse` i `toRouteId`. Jawne przeciążenie `openOccurrence(OccurrenceArgs)` jest granicą otwarcia, a `openOccurrence(String)` tylko parsuje i deleguje, więc trasy i zachowanie pozostały bez zmian. `buildOccurrenceDetails` przyjmuje `OccurrenceArgs`. Nie tworzono `OccurrenceViewModel`, to etap 4.
- Powód: Jednoznaczne argumenty oddzielają identyfikator trasy od logiki szczegółów i przygotowują wydzielenie ViewModelu bez zmiany nawigacji.
- Weryfikacja: Dodano `OccurrenceArgsTest` dla parsowania i round-tripu oraz `OccurrenceDetailsModelsTest` pilnujący, że `OccurrenceDetailsUiState` nie zawiera typów warstwy danych ani `MakDestination`. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Inwentaryzacja `MakViewModel` i kontroler feedbacku

- Fakty: `MakViewModel` łączył dziewięć grup odpowiedzialności, a kanał feedbacku był jego polem i dzielił cykl życia ViewModelu.
- Decyzja: Zapisano w `plan.md` inwentaryzację przypisującą każdą publiczną metodę i pole do przyszłego właściciela: `ScheduleViewModel`, `OccurrenceViewModel`, `ClassEditViewModel`, `SemesterViewModel`, `SetupViewModel`, `SettingsViewModel`, cienki stan nadrzędny albo feedback. Dodano interfejs `FeedbackSink` oraz aplikacyjny `FeedbackController`, który jest jedynym właścicielem `Channel<UiFeedback>`, udostępnia `Flow<UiFeedback>` i implementuje `FeedbackSink`. `MakApplication` tworzy kontroler, `MainActivity` przekazuje `feedback` do `MakApp`, a `MakViewModel` otrzymuje `FeedbackSink`. Operacje repozytorium nie emitują jeszcze komunikatów.
- Powód: Infrastruktura feedbacku ma działać poza cyklem życia ViewModelu, a refaktor ma pozostać etapowy i nie łączyć wielu przepływów w jednym commicie.
- Odrzucone: Jednorazowe przepisanie ViewModelu i dalsze trzymanie kanału feedbacku w ViewModelu.
- Weryfikacja: Dodano `FeedbackControllerTest` dla kolejki i jednokrotnej obsługi, zaktualizowano `MakViewModelTest`, a `UiFeedbackTest` nadal pokrywa cztery warianty i czas wyświetlania. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Poprawka focusu snackbara

- Fakty: `onFocusChanged` stał po `focusable()`, więc mógł nie obserwować właściwego węzła fokusu i obramowanie fokusu nie działało.
- Decyzja: Ustawić `onFocusChanged` przed `focusable()` i dodać test Compose, który żąda focusu akcją semantyczną i sprawdza `assertIsFocused`.
- Weryfikacja: `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Wspólny system feedbacku

- Fakty: Aplikacja nie miała wspólnego kanału komunikatów ani własnego snackbara, więc każda operacja musiałaby sama decydować o sposobie informowania użytkownika.
- Decyzja: Dodano model `UiFeedback` z wariantami `Success`, `Error`, `Warning` i `Info` oraz buforowany `Channel<UiFeedback>` w ViewModelu wystawiony jako `Flow`. W głównym `Scaffold` działa jeden `MakSnackbarHost` poza `NavHost`, więc komunikaty przetrwają zmianę trasy. Warianty różnią się ikoną, kolorem kontenera i opisem semantycznym; sukces i informacja używają krótkiego czasu, błąd i ostrzeżenie długiego, a kolejne komunikaty czekają na zwolnienie miejsca bez nakładania.
- Powód: Jedno miejsce emisji i jeden host upraszczają podłączenie operacji w etapie 6 i gwarantują spójny wygląd oraz obsługę dostępności.
- Odrzucone: Systemowe Toasty, osobny snackbar na ekran, kolor jako jedyny nośnik znaczenia.
- Weryfikacja: Test JVM sprawdza mapowanie wariantów na czas, przekazanie komunikatu i jednokrotną obsługę przez kanał, a test ViewModelu emisję przez `feedback`. Test Compose hosta przy 320 dp sprawdza cztery warianty, kolejność i opis semantyczny. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Na tym etapie operacje repozytorium nie emitują komunikatów; zrobi to etap 6.

## 2026-09-20: Ochrona edycji notatki podczas zapisu

- Fakty: Pola notatek pozostają edytowalne w trakcie zapisu. Blok sukcesu bezwarunkowo zastępował draft wartością wysłaną, więc tekst dopisany podczas operacji znikał.
- Decyzja: Po sukcesie aktualizować zawsze zapisaną wartość, ale draft zastępować tylko wtedy, gdy nadal odpowiada wysłanej wartości. Jeśli użytkownik zmienił pole w trakcie, jego nowy tekst zostaje, a akcja zapisu pozostaje aktywna dla kolejnej zmiany.
- Powód: Trwający zapis nie może usuwać pracy wykonanej w międzyczasie, a zapisana wartość i tak pochodzi z udanej operacji repozytorium.
- Weryfikacja: `MakViewModelTest` na JVM sprawdza zapis, usunięcie pustą wartością, błąd repozytorium oraz edycję pola podczas trwającego zapisu dla obu notatek, z użyciem bramkowanego repozytorium i `Dispatchers.setMain`. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Dwie notatki na ekranie szczegółów

- Fakty: Ekran szczegółów pokazywał notatkę wspólną tylko jako tekst, a notatkę wystąpienia w jednym polu z osobnym przyciskiem usuwania i powrotem do planu po zapisie.
- Decyzja: Blok notatek zawiera dwa niezależne pola: „Notatka dla wszystkich terminów” (aktualizuje `classNote` bazowych zajęć) oraz „Notatka tylko dla tej daty” (zapisuje lub usuwa `OccurrenceNote`). Każde pole ma własny opis zakresu i akcję zapisu. Puste pole usuwa notatkę, a zapis jest nieaktywny, gdy wartość jest zgodna z zapisaną lub nie ma czego usunąć. Flagi `isSavingSharedNote` i `isSavingOccurrenceNote` blokują tylko właściwą akcję. Po sukcesie ekran pozostaje na szczegółach i aktualizuje właściwą notatkę bez wpływu na drugą.
- Powód: Użytkownik ma zarządzać obiema notatkami z jednego ekranu bez wchodzenia w edycję całych zajęć, a niezapisana wartość drugiego pola nie może zniknąć.
- Weryfikacja: `noteContentChanged` traktuje pusty i brakujący tekst tak samo, z testem JVM. Test Compose sprawdza obecność obu pól i niezależne wywołanie obu akcji zapisu przy 320 dp. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Poprawki po recenzji edycji terminu

- Fakty: Klasyfikacja przeniesienia opierała się na poprzednim stanie edycji, więc powrót do daty bazowej z nową godziną był błędnie uznawany za przeniesienie. Wyczyszczenie sali nie miało efektu, bo `newRoom = null` uruchamiało wartość bazową. Dialog można było zamknąć podczas zapisu.
- Decyzja: `Moved` wynika z porównania z datą bazową, a `Modified` z tej samej daty przy zmienionych godzinach lub sali. Pusta sala jest zapisywana jako jawne nadpisanie `""`, jeśli zajęcia bazowe mają salę, a `null` pozostaje przy pustej sali bazowej. Pusty tekst jest normalizowany do `null` na granicy prezentacji i porównania w `OccurrenceSlot`, resolverze, mapowaniu `ClassItemUi` oraz prezenterze widgetu. Dialog podczas `isSaving` ignoruje `onDismissRequest`, a ViewModel także odrzuca próbę zamknięcia.
- Powód: Status ma zależeć od relacji do terminu bazowego, sala ma dać się jawnie usunąć bez migracji schematu, a trwający zapis nie może zgubić wpisanych wartości.
- Weryfikacja: `OccurrenceEditTest` obejmuje powrót do daty bazowej z nową godziną, ponowne przeniesienie, jawną modyfikację sali i brak zmiany przy pustych salach. `ScheduleResolverTest` potwierdza brak sali po nadpisaniu i powrót `L204` po przywróceniu. Test Compose sprawdza blokadę zamknięcia dialogu przy `isSaving = true`. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą.

## 2026-09-20: Edycja terminu oparta na różnicy i jeden dialog

- Fakty: Ekran szczegółów miał osobny formularz w treści oraz wybór między „Zmień tylko ten termin” i „Przenieś ten termin”. Kolizja technicznej różnicy należała do użytkownika. Po każdej operacji ekran opuszczał szczegóły.
- Decyzja: Dodano czystą funkcję `decideOccurrenceEdit`, która normalizuje szkic i zwraca `NoChange`, `Modified`, `Moved` albo `Restored`. Edycja odbywa się w jednym dialogu „Edytuj ten termin” z akcjami „Anuluj” i „Zapisz”. `saveSelectedOccurrenceChange` sam wybiera zapis modyfikacji, zapis przeniesienia albo usunięcie zmiany. Po sukcesie ekran odczytuje świeże dane przez repozytorium i pozostaje na szczegółach. `restoreSelectedOccurrence` i `cancelSelectedOccurrence` także odświeżają szczegóły zamiast wracać do planu.
- Powód: Użytkownik nie musi znać wewnętrznego rozróżnienia, a pusta operacja nie tworzy rekordu ani pilla. Natychmiastowa aktualizacja opiera się na Room jako źródle prawdy.
- Weryfikacja: Test JVM `OccurrenceEditTest` pokrywa brak zmiany, zmianę godzin, sali i daty, jednoczesną zmianę oraz powrót do wartości bazowych. Testy Compose sprawdzają dialog i wariant przywracania przy 320 dp. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Etapy 4-7 sekcji 1.6 pozostają do wykonania, a odbiór na urządzeniu pozostaje wymagany.

## 2026-09-20: Poprawki po recenzji ekranu szczegółów terminu

- Fakty: `OccurrenceBottomActions` dodawał własny `navigationBarsPadding`, mimo że dolny inset pochodzi z nadrzędnego `Scaffold` i `NavHost`. Test ekranu szukał tekstu `Notatka wspólna`, który nie jest osobnym węzłem, bo ekran renderuje `Notatka do zajęć: Notatka wspólna`.
- Decyzja: Usunięto `navigationBarsPadding` i jego import, pozostawiając padding `MakSpacing`. Test ekranu sprawdza pełny tekst notatki, brak „Pokaż zmiana terminu”, zniknięcie „Zmień termin” po otwarciu formularza oraz wariant przywracania z wywołaniem akcji. Test topbara używa rzeczywistego `MakActionMenu` i osobno liczy wywołania „Odwołaj termin”, „Edytuj bazowe zajęcia” i „Usuń zajęcia”.
- Weryfikacja: `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Odbiór na emulatorze albo urządzeniu pozostaje wymagany.

## 2026-09-20: Osobne ekrany kierunków i korekt tygodni

- Fakty: Konfiguracja semestru pokazuje wyłącznie formularz danych, a zarządzanie kierunkami i korektami tygodni odbywa się na trasach `semester/{semesterId}/courses` oraz `semester/{semesterId}/week-overrides`. Formularz korekty pojawia się dopiero po akcji „Dodaj” albo „Edytuj”, a zapis i usunięcie nie opuszczają już ekranu podrzędnego. Sekcja rozwijana tworzy jeden kontener, w którym przycisk i treść mają wspólne obramowanie, a stan jest widoczny przez ikonę kierunku i opis „Rozwinięte” albo „Zwinięte”.
- Decyzja: Formularz korekty steruje polem `isOpen` w `WeekOverrideFormUiState`, a robocze wartości formularza pozostają w stanie ViewModelu. Użycia `MakExpandableSection` w `ClassEditScreen`, `ScheduleScreen` i `SettingsScreen` zostają rozwinięciem w miejscu, ponieważ ich treść jest krótka i potrzebna w kontekście bieżącego widoku.
- Weryfikacja: Dodano testy Compose dla szerokości 320 dp i motywu ciemnego przy 390 dp oraz test semantyki sekcji rozwijanej. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Odbiór na emulatorze albo urządzeniu pozostaje wymagany.


## 2026-09-20: Podrzędne ekrany konfiguracji semestru

- Fakty: Konfiguracja semestru pokazuje teraz krótkie pozycje „Kierunki” i „Korekty tygodni” z licznikami zamiast rozwijanych list.
- Decyzja: Dodano trasy `semester/{semesterId}/courses` oraz `semester/{semesterId}/week-overrides`. Oba ekrany korzystają z istniejącego `SemesterScreenUiState` i operacji ViewModelu.
- Weryfikacja: Test tras sprawdza identyfikator semestru i mapowanie do ekranu semestru. Test Compose dla szerokości 320 dp sprawdza nawigacyjne wiersze i brak tekstu „Pokaż kierunki”. `test` oraz `compileDebugAndroidTestKotlin` przechodzą.

## 2026-09-20: Stałe akcje ekranu szczegółów terminu

- Fakty: Treść ekranu szczegółów terminu jest przewijana niezależnie od dolnego obszaru akcji. Akcja „Zmień termin” albo „Przywróć termin” oraz „Zamknij” pozostają dostępne przy dolnej krawędzi.
- Decyzja: Formularz zmiany terminu jest renderowany bezpośrednio pod notatką po aktywacji dolnej akcji. Po jego otwarciu dolny przycisk zmiany jest ukrywany, a przyciski zapisu i przeniesienia pozostają przy formularzu.
- Weryfikacja: Test Compose sprawdza szerokość 320 dp, widoczność dolnych akcji oraz otwarcie formularza. `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Odbiór runtime pozostaje wymagany.

## 2026-09-20: Akcje ekranu szczegółów w topbarze

- Fakty: `MakTopBar` obsługuje opcjonalny slot akcji. Dla trasy szczegółów terminu pokazuje menu „Więcej opcji” po prawej stronie przycisku cofania.
- Decyzja: Akcje odwołania terminu, edycji bazowych zajęć i usunięcia zajęć są budowane w `MakNavHostApp`, a ekran szczegółów nie renderuje menu w przewijanej treści.
- Weryfikacja: Test topbara sprawdza renderowanie przekazanej akcji przy szerokości 320 dp. `test` i `compileDebugAndroidTestKotlin` przechodzą.

## 2026-09-20: Pierwszy etap ekranu szczegółów terminu

- Fakty: Ekran szczegółów terminu nie pokazuje już lokalnej etykiety „TERMIN”. Nazwa zajęć i opis zmiany są prezentowane przez prywatny nagłówek.
- Decyzja: Notatka wspólna oraz notatka do wystąpienia są renderowane bezpośrednio po faktach, bez dodatkowego rozwijania.
- Weryfikacja: Dodano test Compose dla szerokości 320 dp. Test potwierdza widoczność długiej nazwy, obu notatek oraz brak tekstów „TERMIN” i „Pokaż notatkę”. `test` i `compileDebugAndroidTestKotlin` przechodzą.

## 2026-09-20: Separatory i licznik kolizji widgetu

- Fakty: Wpis listy widgetu zawiera teraz wiersz, odstęp, separator i końcowy odstęp w jednym komponencie `Column`. Separator występuje tylko między wpisami i ma stabilny znacznik testowy.
- Decyzja: Liczyć kolizje na podstawie unikalnej pary identyfikatorów wystąpień oraz zakresu części wspólnej. Ta sama kolizja przypisana do obu wpisów pozostaje jednym zdarzeniem.
- Interfejs: Nagłówek widgetu pokazuje liczbę kolizji obok liczby zajęć tylko wtedy, gdy liczba jest większa od zera. Licznik korzysta z koloru błędu i poprawnej polskiej odmiany.
- Weryfikacja: `test`, `compileDebugAndroidTestKotlin`, `lintDebug` i `assembleDebug` przechodzą. Test kompozycji sprawdza separatory między wpisami i ich brak po ostatnim wpisie. Kontrola na launcherze pozostaje wymagana.

## 2026-09-20: Modernizacja układu widgetu

- Fakty: Widget otrzymał nagłówek z datą 16 sp, etykietę tygodnia, licznik zajęć, pasek koloru kierunku, stałą kolumnę czasu, osobne metadane i krótkie etykiety kolizji oraz notatek. `SizeMode.Responsive` udostępnia osiem rozmiarów: `180x110`, `240x110`, `180x175`, `240x175`, `180x240`, `240x240`, `180x340` i `240x340 dp`. Zakres providera pozostaje od `180x110` do `360x420 dp`.
- Decyzja: Rozdzielić klasyfikację na oś wysokości i szerokości. Wysokość wybiera gęstość wierszy i dostępny obszar listy, a szerokość wybiera wariant wąski albo szeroki z prowadzącym, większymi limitami tekstu i dwiema liniami nazwy tylko dla dużego szerokiego układu. Lista przewijana renderuje wszystkie zajęcia, a kolor kierunku jest tylko oznaczeniem pomocniczym i zawsze towarzyszy mu nazwa tekstowa.
- Gęstość: Polityka układu ustala wysokość paska, liczbę linii, odstęp po separatorze i tryb statusu dla każdej kombinacji osi. Kompaktowy pokazuje alert kolizji, ale pomija osobną etykietę notatki. Średni pokazuje jedną etykietę i daje pierwszeństwo kolizji przed notatką. Duży pokazuje kolizję i notatkę razem. Liczba elementów wynika z przewijanej listy, a nie z polityki wiersza.
- Przewijanie: Dopuszczono wysokości `340 dp` i `maxResizeHeight="420dp"`. Nagłówek pozostaje poza `LazyColumn`, która renderuje wszystkie dzisiejsze wystąpienia ze stabilnymi identyfikatorami. Limit zajęć i stopka `Jeszcze N` nie są już częścią polityki ani renderowania.
- Kolizje: `WidgetConflictUi` przechowuje zakres oraz nazwę drugich zajęć. Presenter mapuje każdą kolizję na oba wystąpienia, usuwa duplikaty i sortuje je po czasie oraz nazwie. Pierwszy konflikt jest alertem z `errorContainer`, a notatka zachowuje neutralną etykietę.
- Powód: Duży wariant powinien wykorzystać dostępne miejsce na metadane bez tworzenia osobnego układu dla każdego wymiaru. Jedna stała kolumna czasu ułatwia szybkie porównanie zajęć, a statusy pozostają czytelne w motywie jasnym i ciemnym.
- Narzędzia: Wrapper używa Gradle 9.7.1 z oficjalną sumą SHA-256, walidacją adresu dystrybucji i limitem czasu sieci.
- Weryfikacja: Testy polityki obejmują osiem progów `Responsive`, niezależne osie, metadane wąskie i szerokie, konflikty i kompozycję `LazyColumn`. Kontrole Gradle po tej zmianie należy wykonać przed oznaczeniem etapu jako zakończonego. `--warning-mode all` wskazuje ostrzeżenie o notacji zależności przez obiekt `Project` używanej przez konfigurację lub plugin; skrypty projektu nie zawierają bezpośredniej zależności między modułami do przepisania. Odbiór na launcherze pozostaje wymagany.

## 2026-09-19: Implementacja widgetu dzisiejszego planu

- Fakty: Dodano `WidgetPlanLoader`, `WidgetPresenter`, modele stanów widgetu, responsywny `GlanceAppWidget` oraz receiver z metadanymi launchera. Loader korzysta z `MakRepository`, `ActivePlanProvider` i `Clock`, a widget otwiera ekran „Dzisiaj”.
- Decyzja: Widget pokazuje mały lub duży wariant zależnie od `SizeMode.Responsive`, ogranicza liczbę wierszy na podstawie wysokości, odświeża się po invalidacji tabel Room i co 30 minut jako zabezpieczenie zmiany dnia.
- Powód: Jedna ścieżka obliczeń zachowuje zgodność widgetu z ekranami Compose, a obserwacja bazy aktualizuje wszystkie instancje dopiero po zapisaniu danych.
- Odrzucone: Bezpośrednie wywołania DAO i `ScheduleResolver` z Glance; przechowywanie planu w stanie Glance; WorkManager; dokładne alarmy; ciągły serwis; osobny układ dla każdego rozmiaru.
- Wznowić decyzję tylko gdy: testy na urządzeniu wykażą problemy z progami rozmiaru, kontrastem albo opóźnieniem odświeżenia.

## 2026-09-19: Etapowa implementacja widgetów Glance

- Fakty: `ActivePlanProvider` jest gotowy, a wersja 0.2 obejmuje mały i duży widget, odświeżanie po zmianach oraz otwieranie ekranu „Dzisiaj”. Android może opóźniać aktualizacje okresowe i ogranicza częste działanie w tle.
- Decyzja: Implementować widget w ośmiu etapach: stan i loader, rejestracja, mały układ, duży układ, otwieranie aplikacji, odświeżanie, odporność oraz odbiór. Widget używa `MakRepository`, `ActivePlanProvider` i wstrzykniętego `Clock`. Aktualizacja po zapisie przechodzi przez jedną granicę niezależną od Glance, a okresowy sygnał działa nie częściej niż raz na godzinę.
- Powód: Etapy ograniczają ryzyko połączenia danych, ograniczeń `RemoteViews`, wielu rozmiarów i cyklu życia widgetu w jednej zmianie. Wspólna ścieżka planu chroni zgodność z ekranem „Dzisiaj”.
- Odrzucone: Osobny resolver widgetu; DAO wywoływane z widgetu; kopia planu w preferencjach Glance; ciągły serwis; dokładny alarm o północy; odświeżanie co minutę; osobny układ dla każdego możliwego wymiaru.
- Wznowić decyzję tylko gdy: pomiary na urządzeniu wykażą, że godzinne zabezpieczenie lub dwa progi rozmiaru nie zapewniają użytecznego wyniku.

## 2026-09-19: Usunięcie poglądowego mockupu

- Fakty: `mockup.html` służył wyłącznie jako poglądowy materiał podczas początkowego projektowania i nie jest źródłem prawdy dla produktu.
- Decyzja: Usunąć `mockup.html` oraz obowiązek jego przeglądania i aktualizowania. Dokumenty pozostają źródłem zakresu i zachowania, a działająca aplikacja Compose jest źródłem bieżącego wyglądu.
- Powód: Utrzymywanie drugiej implementacji interfejsu zwiększa koszt zmian i może pokazywać przebieg niezgodny z aplikacją.
- Odrzucone: Dalsze utrzymywanie mockupu jako referencji wizualnej albo wymaganej części zmian UI.
- Wznowić decyzję tylko gdy: powstanie osobny, świadomie utrzymywany proces projektowy z określonym źródłem makiet.

## 2026-09-19: Zagęszczenie ekranu „Plan” i neutralna prezentacja kolizji

- Fakty: Widok listy pokazuje osobno nagłówek, sterowanie tygodniem, kartę oznaczenia A/B, wybór dnia, filtry i odłączone menu z jedną akcją. Karta zajęć umieszcza kierunek, typ, salę i prowadzącego w jednym wierszu, a kolizję oznacza czerwonym tekstem bez zakresu czasu.
- Decyzja: Połączyć sterowanie tygodniem i oznaczenie A/B, otwierać korektę z odznaki tygodnia, usunąć odłączone menu, pokazywać aktywny filtr w zwiniętej sekcji, uporządkować metadane karty i prezentować kolizję neutralnie z dokładnym zakresem. Skrócić lokalny nagłówek ekranu, zwiększyć czytelność wyboru dnia i zachować obszary dotyku co najmniej 48 dp.
- Powód: Pierwsze zajęcia powinny pojawiać się wyżej, a stan tygodnia, filtr i kolizja mają być zrozumiałe bez otwierania dodatkowych ekranów.
- Odrzucone: Zmiana palety całej aplikacji; zmiana dolnej nawigacji; usunięcie odznaki kierunku; czerwone oznaczenie kolizji jako błędu; poziome przewijanie dni albo filtrów.
- Wznowić decyzję tylko gdy: testy na urządzeniu wykażą, że połączona sekcja nie mieści się na szerokości 320 dp albo zmieni się model nawigacji ekranu „Plan”.

## 2026-09-19: Plan porządkowania architektury przed wersjami 0.2 i 0.3

- Fakty: `MakViewModel` obsługuje większość przepływów aplikacji, mapowanie Room na domenę i składanie planu. Nawigacja przechowuje bieżący cel zarówno w ViewModelu, jak i w `NavController`. Widget oraz import zwiększą liczbę konsumentów planu i operacji wieloetapowych.
- Decyzja: Przed widgetem wydzielić wspólny `ActivePlanProvider` i mapowanie danych poza ViewModel. Ustawić `NavController` jako jedyne źródło bieżącej trasy. Rozdzielać ViewModele według przepływów, nie wystawiać typów Room w publicznym stanie UI, używać transakcji dla operacji wieloetapowych, pokazywać jawny stan zapisu oraz przygotować preferencje i stan roboczy do odtworzenia procesu.
- Powód: Ekrany i widget muszą korzystać z tej samej logiki planu, a rozwój wersji 0.2 i 0.3 nie powinien zwiększać sprzężenia jednego ViewModelu ani ryzyka częściowego zapisu danych.
- Odrzucone: Kopiowanie mapowania i wywołań resolvera do widgetu; utrzymywanie dwóch źródeł trasy; jednorazowe przepisanie całej aplikacji; podział na wiele modułów Gradle i dodanie frameworka DI bez konkretnej potrzeby.
- Wznowić decyzję tylko gdy: zakres widgetu, importu albo nawigacji zmieni granice odpowiedzialności lub obecny jeden moduł Gradle przestanie wystarczać.

## 2026-09-19: Wersje narzędzi dla 0.1

- Fakty: Projekt używa `compileSdk` 36.1. Najnowsze wydania części bibliotek wymagają `compileSdk` 37, a starszy KSP nie współpracuje poprawnie z wbudowaną obsługą Kotlin w AGP 9.
- Decyzja: Przypięto Kotlin 2.2.10, AGP 9.1.1, Gradle 9.3.1, Compose BOM 2024.09.00, Room 2.8.5, Lifecycle 2.10.0, Navigation 2.9.5, KSP 2.3.12 i Glance 1.2.0.
- Powód: Ten zestaw kompiluje wersję 0.1 na dostępnym SDK 36.1 i zachowuje zgodność z AGP 9.
- Wznowić decyzję tylko gdy: projekt przejdzie na `compileSdk` 37 albo aktualizacja usunie ograniczenia zgodności.

## 2026-09-19: Prefiks docs

- Decyzja: Dozwolony jest także prefiks `docs:` dla commitów dotyczących dokumentacji.
- Wznowić decyzję tylko gdy: użytkownik zmieni konwencję commitów.

## 2026-09-19: Autonomiczne commity po zakończeniu zadań

- Decyzja: Po zakończeniu zadania agent może samodzielnie utworzyć commit obejmujący jego logiczną zmianę. Wypychanie zmian, tworzenie gałęzi i zmiana historii nadal wymagają wyraźnego polecenia użytkownika.
- Wznowić decyzję tylko gdy: użytkownik zmieni zakres autonomii dotyczącej Git.

## 2026-09-19: Prefiksy commitów

- Decyzja: W tematach commitów można używać prefiksów `feat:`, `fix:` i `chore:`. Temat nadal ma być krótki, konkretny i napisany w trybie rozkazującym.
- Wznowić decyzję tylko gdy: użytkownik zmieni konwencję commitów.

## 2026-09-19: Zasada prostego stylu bez AI-slop

- Fakty: Użytkownik chce, aby nowe teksty nie używały środkowej kropki, em dash, emotek ani innych ozdobników mocno kojarzonych z AI-slop.
- Decyzja: Zakaz obowiązuje nowe dokumenty, teksty interfejsu i odpowiedzi. Należy używać zwykłej interpunkcji oraz konkretnych sformułowań.
- Powód: Prosty styl ma ograniczyć sztuczne sygnały tekstu generowanego automatycznie i ułatwić czytanie.
- Wznowić decyzję tylko gdy: użytkownik świadomie zaakceptuje wyjątek dla konkretnego formatu lub elementu interfejsu.

## 2026-09-19 — Izolowane semestry, kreator i powiadomienia

- Fakty: Użytkownik zdecydował, że zajęcia nie mogą przechodzić przez północ, pierwsza konfiguracja ma przebiegać przez kreator, a aplikacja ma obsługiwać wiele odizolowanych semestrów wybieranych w ustawieniach. Użytkownik potwierdził także obsługę powiadomień, ale nie określił jeszcze ich treści.
- Decyzja: Formularz odrzuca zajęcia, których godzina zakończenia nie jest późniejsza od rozpoczęcia w tym samym dniu. Kreator tworzy pierwszy semestr i kierunek. Każdy semestr ma własne kierunki, prowadzących, zajęcia, korekty, notatki i zmiany wystąpień. Ustawienia umożliwiają dodawanie, konfigurację, wybór i usuwanie semestrów. Powiadomienia są częścią zakresu aplikacji, a ich zdarzenia, treści i moment wysyłki pozostają otwarte.
- Powód: Izolacja semestrów chroni plany przed mieszaniem danych, kreator ogranicza liczbę decyzji przy pierwszym uruchomieniu, a walidacja jednego dnia upraszcza model czasu.
- Odrzucone: Zajęcia przechodzące przez północ; wspólny plan wielu semestrów; konfiguracja początkowa bez prowadzenia; ustalenie treści powiadomień bez decyzji o zdarzeniach.
- Wznowić decyzję tylko gdy: użytkownik zmieni regułę czasu zajęć, dopuści współdzielenie danych między semestrami albo określi konkretne scenariusze powiadomień.

## 2026-09-19 — Granice warstw, ViewModel i eksport

- Fakty: Użytkownik zaakceptował zalecenia: pakiety `data`, `domain`, `ui`, `widget`, `export` w jednym module Gradle; ViewModel składa stan z repozytorium i wyniku resolvera; `WeekCalculator` schowany w `ScheduleResolver`; eksport poza Room.
- Decyzja: Zapisano granice w `ARCHITECTURE.md`. ViewModel nie woła DAO i nie liczy planu. Widget nie ma ViewModelu. UI wybiera plik, `export` zapisuje JSON. Domain nie zna `Uri`. Datę wstrzykuje się do testów. Walidacja formularza jest czystą funkcją.
- Powód: Poprawność planu zostaje w jednym miejscu, baza zostaje cienkim magazynem, a eksport może rosnąć o import bez mieszania z DAO.
- Odrzucone: Osobne moduły Gradle na start; ViewModele w `domain`; `WeekCalculator` składany w każdym ekranie; eksport jako część `data` albo `data/export`.
- Wznowić decyzję tylko gdy: któraś warstwa dostanie niezależny cykl wdrażania albo import przestanie mieścić się w pakiecie `export`.

## 2026-09-19 — Minimalna wersja Androida

- Fakty: Użytkownik potwierdził, że `minSdk` wynosi 31. Projekt Gradle już to ustawia.
- Decyzja: Minimalna wersja Androida to 31 (Android 12). Zapis w `ARCHITECTURE.md` i `STACK.md`. Pytanie otwarte o minSdk zostaje zamknięte.
- Powód: API 31 daje `java.time` bez desugaringu i odpowiada telefonowi do użytku własnego.
- Odrzucone: Szersze wsparcie starszych wersji Androida.
- Wznowić decyzję tylko gdy: użytkownik będzie potrzebował uruchamiać MAK na urządzeniu poniżej API 31.

## 2026-09-19 — Testy dla agentów

- Fakty: Użytkownik chce, żeby agenci sami sprawdzali działanie aplikacji testami. Warstwa Room ma pozostać minimalna. Zapytał, czy przy cienkiej bazie potrzeba wielu testów DAO.
- Decyzja: Zasady testowania zapisano w `AGENTS.md`. Priorytet mają testy JVM `WeekCalculator`, `ScheduleResolver`, `CollisionDetector`, eksportu JSON i walidacji. Testy Room ograniczają się do odczytu po nowej instancji bazy oraz migracji przy rzeczywistej zmianie schematu. Testy Compose obejmują kilka przebiegów z wstrzykniętą datą. Widget nie ma osobnych testów Glance.
- Powód: Poprawność planu jest w resolverze, nie w DAO. Dużo testów bazy dublowałoby SQLite i spowalniało agentów bez ochrony dodatkowych reguł.
- Odrzucone: Pokrywanie CRUD każdego DAO oraz instrumentacja widgetu na starcie.
- Wznowić decyzję tylko gdy: warstwa danych przestanie być cienkim zapisem Room albo pojawi się stałe urządzenie w CI do testów UI.

## 2026-09-19 — Nazwa aplikacji

- Fakty: Produkt nosił roboczą nazwę DualPlan. Użytkownik zaakceptował nazwę MAK z rozwinięciem Mobilny Akademicki Kalendarz.
- Decyzja: Nazwa aplikacji to MAK. Rozwinięcie jest wyjaśnieniem skrótu, nie drugą nazwą. DualPlan przestaje być nazwą produktu.
- Powód: MAK jest krótkie na launcher, po polsku i nie sugeruje limitu dwóch kierunków.
- Odrzucone: DualPlan jako nazwa docelowa; traktowanie rozwinięcia jako osobnej nazwy w interfejsie.
- Wznowić decyzję tylko gdy: użytkownik zaakceptuje inną nazwę produktu albo osobną etykietę launchera.

## 2026-09-19 — Kalendarz i zmiany pojedynczych wystąpień

- Fakty: Użytkownik chce przeglądać plan jako kalendarz oraz obsługiwać odwołane zajęcia, odrabianie i inne odstępstwa od cyklu.
- Decyzja: Dodać na ekranie „Plan” widoki „Lista” i „Kalendarz”. Zmianę jednego terminu przechowywać jako `OccurrenceChange`, a dodatkowe spotkanie jako `ClassEntity` z `recurrence = ONCE`. Wszystkie widoki korzystają z `ScheduleResolver`.
- Powód: Bazowy plan pozostaje prosty, a odstępstwa można dodać lub usunąć bez przebudowy cyklu zajęć.
- Odrzucone: Edycja bazowych zajęć w celu odwołania jednego terminu oraz osobna logika planu dla kalendarza.
- Wznowić decyzję tylko gdy: potrzebne będą zmiany obejmujące dowolny zakres dat inny niż jeden termin albo „od teraz”.

## 2026-09-19 — Modularność i skala aplikacji

- Fakty: Funkcjonalności i wygląd będą często zmieniane po bieżącym feedbacku. Aplikacja jest przeznaczona do użytku własnego i działa lokalnie.
- Decyzja: Traktować modularność jako zasadę architektury. Utrzymywać wyraźne granice między danymi, logiką domenową, UI i widgetem, ale nie tworzyć wielu modułów Gradle bez konkretnej potrzeby. Nie projektować systemu pod setki użytkowników, współbieżność ani skalowanie serwerowe.
- Powód: Małe, wymienne części ułatwiają przebudowę i testowanie bez kosztu przedwczesnej infrastruktury.
- Odrzucone: Backend, multi-tenancy i podział na wiele modułów Gradle na starcie.
- Wznowić decyzję tylko gdy: aplikacja przestanie być jednoosobowa albo któraś część otrzyma niezależny cykl wdrażania.

## 2026-09-19 — Notatki do zajęć i wystąpień

- Fakty: Użytkownik chce zapisywać notatkę wspólną dla zajęć albo notatkę dotyczącą tylko wybranego terminu.
- Decyzja: Przechowywać `classNote` przy `ClassEntity` oraz osobny `OccurrenceNote` z `classId`, `occurrenceDate` i `body`. Oba typy są dostępne w MVP i trafiają do eksportu JSON.
- Powód: Notatka do jednej daty nie może zmieniać notatek pozostałych wystąpień tych samych zajęć.
- Odrzucone: Jedno pole `note` bez zakresu, ponieważ mieszałoby notatkę stałą z notatką do konkretnej daty.
- Wznowić decyzję tylko gdy: potrzebne będą wiele notatek tego samego typu albo notatki przypięte do innych elementów planu.

## 2026-09-18 — Automatyczne tygodnie A/B i ręczne korekty

- Fakty: Plan opisywał tygodnie parzyste i nieparzyste bez reguły dla semestru zaczynającego się w środku tygodnia. Użytkownik zażądał automatycznego wyliczania oraz zmiany pojedynczej i od wskazanego tygodnia w przyszłość.
- Decyzja: Używać oznaczeń A/B. Pierwszym tygodniem jest tydzień od poniedziałku zawierający początek semestru. Korekta pojedyncza zmienia tylko wybrany tydzień, a korekta przyszła ustawia nowe A/B i od niego rozpoczyna naprzemienną sekwencję. Pojedyncza korekta ma pierwszeństwo w swoim tygodniu; późniejsza korekta przyszła przejmuje kolejne tygodnie od swojej daty.
- Powód: Użytkownik może poprawić jednorazowe odstępstwo bez przesunięcia reszty planu albo trwale przesunąć rytm zajęć.
- Odrzucone: Obliczanie wyłącznie z numeru tygodnia ISO i ręczna zmiana każdego wpisu zajęć.
- Wznowić decyzję tylko gdy: uczelnia stosuje inny zakres tygodnia albo wymagany jest trzeci typ cyklu.

## 2026-09-18 — Ochrona danych i zasady widgetu

- Fakty: Plan odkładał eksport do wersji 0.3 i sugerował odświeżenie widgetu przy zmianie dnia. Android może opóźniać zadania w tle.
- Decyzja: Wprowadzić eksport JSON w wersji 0.1, zachować schemat Room i testować późniejsze migracje. Odświeżać widget po zmianach danych oraz okresowo, bez obietnicy aktualizacji dokładnie o północy.
- Powód: Plan wpisywany ręcznie wymaga wczesnej możliwości zapisania kopii, a harmonogram widgetu musi odpowiadać ograniczeniom platformy.
- Odrzucone: Obietnica punktualnego odświeżenia widgetu i częste budzenie aplikacji w tle.
- Wznowić decyzję tylko gdy: zmienią się wymagania dotyczące kopii danych albo powstanie funkcja wymagająca dokładnego alarmu.

## 2026-09-18 — Przygotowanie bazy repozytorium

- Fakty: Repozytorium zawierało plan produktu w `plan.md` i nie zawierało kodu aplikacji.
- Decyzja: Utworzyć pełną bazę dokumentacyjną bez kodu, zachować `plan.md`, używać polskiego w dokumentacji i odpowiedziach oraz angielskiego dla identyfikatorów i komentarzy.
- Powód: Dokumentacja ma od razu opisywać zaakceptowany zakres, stos i sposób pracy przed rozpoczęciem implementacji.
- Odrzucone: Tworzenie kodu, manifestu, konfiguracji Gradle i zależności na etapie przygotowania repozytorium.
- Wznowić decyzję tylko gdy: zmieni się zakres aplikacji, język dokumentacji lub sposób budowy projektu.

## 2026-09-18 — Zasady interfejsu jako kryteria akceptacji

- Fakty: Użytkownik przekazał osiem zasad dotyczących czytelności, spójności, dostępności, mobile-first, uczciwego stanu systemu, stylu, języka i ochrony pracy użytkownika.
- Decyzja: Umieścić te zasady w sekcji „Zasady projektowania interfejsu” w `ARCHITECTURE.md` i stosować je do każdego widoku.
- Powód: Zasady wpływają na architekturę interfejsu, testy i kryteria ukończenia, więc powinny być źródłem prawdy projektu.
- Odrzucone: Traktowanie dostępności i testów małych ekranów jako późniejszego audytu.
- Wznowić decyzję tylko gdy: użytkownik zaakceptuje nowy język interfejsu albo zmieni priorytety produktu.

## 2026-09-19 — Material 3 i standardy konstrukcji interfejsu

- Fakty: Użytkownik zaakceptował standardy branżowe i zasady Material 3 jako podstawę konstrukcji interfejsu.
- Decyzja: Budować interfejs na komponentach i zasadach Material 3. Własne kolory, typografia, kształty, karty, nawigacja i układ są dozwolone, jeśli zachowują semantykę, przewidywalne zachowanie oraz dostępność.
- Problem do rozwiązania: Przeprowadzić audyt własnych komponentów Compose pod kątem obszarów dotyku co najmniej 48 dp, semantyki, focusu, klawiatury, kontrastu, motywu ciemnego i szerokości 320 dp. Wynik ma trafić do kolejnych zmian UI i testów Compose.
- Powód: Material 3 zapewnia przewidywalne zachowanie komponentów i nie usuwa możliwości stosowania własnego języka wizualnego.
- Wznowić decyzję tylko gdy: zmieni się wersja Material 3 albo wymagania dostępności Androida.

## 2026-09-19 — Ikony Material w akcjach interfejsu

- Fakt: Akcje nawigacyjne i lokalne używały znaków Unicode, które różnią się wyglądem między fontami i mają słabszą przewidywalność dla użytkownika.
- Decyzja: Używać Material Icons Extended dla wstecz, ustawień, kalendarza, czasu, menu, nawigacji i dodawania.
- Powód: Ikony mają spójny optyczny rozmiar, wspierają kierunek RTL i mogą być dekoracyjne przy zachowaniu etykiety akcji w semantyce.
- Odrzucone: Zastępowanie ikon kolejnymi znakami Unicode.
- Wznowić decyzję tylko gdy: Material 3 zmieni zalecany pakiet ikon albo aplikacja otrzyma własny zestaw ikon.

## 2026-09-19 — Zamknięcie iteracji dopracowania frontendu

- Fakt: Zrealizowano migrację na `NavHost`, obsługę back stacku i argumentów tras, inserty edge-to-edge, topbar, przycisk wstecz, pickery, tokeny odstępów, ikony Material oraz testy Compose i JVM.
- Decyzja: Oznaczyć iterację dopracowania frontendu jako wykonaną w `plan.md`. Testy runtime na emulatorze pozostają osobnym krokiem weryfikacyjnym, ponieważ środowisko nie udostępnia `adb`.
- Powód: Kompilacja aplikacji, testy JVM i kompilacja testów Android potwierdzają poprawność na poziomie dostępnym lokalnie. Zachowanie systemowych insetów, TalkBacka i gestu wstecz wymaga rzeczywistego urządzenia.
- Odrzucone: Oznaczenie testów urządzeniowych jako wykonanych bez dostępu do emulatora.
- Wznowić decyzję tylko gdy: pojawi się emulator lub urządzenie do testów albo zmieni się zakres kolejnej wersji.

## 2026-09-19 — Dane demonstracyjne w buildzie debug

- Fakt: Pusta baza danych nie pozwalała szybko obejrzeć ekranów planu, kalendarza, zmian terminów i stanów kolizji.
- Decyzja: Przy uruchomieniu builda debug zasilić pustą bazę jednym semestrem demonstracyjnym. Seed jest pomijany, gdy istnieje dowolny semestr, i nie działa w buildzie release.
- Powód: Ułatwia to ręczny podgląd interfejsu bez tworzenia danych krok po kroku, jednocześnie chroniąc dane użytkownika.
- Odrzucone: Nadpisywanie bazy przy każdym uruchomieniu oraz dodawanie przykładowych danych do wydania produkcyjnego.
- Wznowić decyzję tylko gdy: pojawi się osobny tryb demonstracyjny albo ekran ręcznego usuwania danych przykładowych.

## 2026-09-19 — Kolizja jako ostrzeżenie, nie błąd użytkownika

- Fakt: Zajęcia z dwóch kierunków mogą zgodnie z rzeczywistym planem nakładać się godzinami.
- Decyzja: Traktować kolizję jako neutralną informację o ograniczeniu planu. Aplikacja wskazuje zajęcia i zakres nakładania, ale nie proponuje zmiany terminu ani nie zmienia go automatycznie.
- Powód: Kolizja wynika z planu uczelni, a nie z błędnego działania użytkownika. To użytkownik decyduje, jakie działanie podjąć.
- Odrzucone: Automatyczne rozwiązywanie kolizji, sugerowanie jednego terminu jako właściwego oraz komunikaty obciążające użytkownika.
- Wznowić decyzję tylko gdy: pojawi się osobna, wyraźnie uruchamiana funkcja wsparcia w kontakcie z uczelnią.
