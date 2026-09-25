# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest pięć poprawek z audytu z 2026-09-25, w kolejności wagi. I-20 (drobne poprawki) zostaje w `QUEUE.md` jako następne zadanie. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Potwierdzenie usunięcia kierunku z semestru (I-15)

Problem: `SemesterViewModel.deleteCourse` od razu woła `SemesterRepository.deleteSemesterProgram`. Klucze obce z `ON DELETE CASCADE` usuwają wtedy wszystkie zajęcia przypisania wraz z notatkami i zmianami wystąpień.

Granice: nie zmieniaj kaskady w bazie ani reguły usuwania nieużywanego kalendarza w `deleteSemesterProgram`. Zmiana dotyczy tylko potwierdzenia.

Kolejność:

1. `ClassDao`: zapytanie `SELECT COUNT(*) FROM classes WHERE semester_program_id = :assignmentId`. `SemesterRepository`: `suspend fun countClassesForAssignment(assignmentId: Long): Int` w interfejsie i w `RoomSemesterRepository`; uzupełnij `FakeSemesterRepository` i `FakeRepository` w testach.
2. `SemesterScreen.kt`: model `CourseDeletionUi(assignmentId: String, programName: String, classCount: Int)` i pole `pendingCourseDeletion: CourseDeletionUi?` w `SemesterScreenUiState`.
3. `SemesterViewModel`: `requestCourseDeletion(id)` czyta liczbę zajęć i ustawia `pendingCourseDeletion`; `cancelCourseDeletion()` czyści stan; `confirmCourseDeletion()` wykonuje dotychczasową treść `deleteCourse` dla zapamiętanego przypisania i czyści stan po sukcesie. Publiczne `deleteCourse` usuń albo zmień w funkcję prywatną. Zachowaj `sessionToken` i obsługę błędów.
4. Dialog w stylu `ReconnectCalendarDialog`: tytuł „Usunąć kierunek z semestru?”, treść „Kierunek {nazwa} zostanie usunięty z tego semestru razem z {liczba zajęć} oraz ich notatkami i zmianami terminów. Kierunek pozostanie dostępny w innych semestrach. Tej operacji nie można cofnąć.”, akcje „Anuluj” i „Usuń” w kolorze błędu (`MaterialTheme.colorScheme.error`). Przy zerze zajęć pomiń część o zajęciach. Liczbę odmień: 1 zajęcia, 2 do 4 zajęcia, 5 i więcej zajęć; użyj istniejącej funkcji `classCountLabel`, jeśli pasuje, albo dodaj małą funkcję odmiany.
5. `SemesterRoutes.kt` i `SemesterScreen.kt`: „Usuń” przy kierunku woła `requestCourseDeletion`; podłącz potwierdzenie i anulowanie.

Przypadki brzegowe: kierunek bez zajęć; dwukrotne dotknięcie „Usuń” w dialogu (blokada przez `isDeletingCourse`); błąd zapisu (dialog zostaje, komunikat błędu); opuszczenie ekranu w trakcie (token sesji).

Weryfikacja: `SemesterViewModelTest`: `requestCourseDeletion` niczego nie usuwa, `cancelCourseDeletion` czyści stan, `confirmCourseDeletion` usuwa przypisanie; `SemesterScreenTest`: dialog z liczbą zajęć przy 320 dp i brak usunięcia po „Anuluj”. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: usunięcie kierunku z semestru wymaga potwierdzenia z liczbą usuwanych zajęć, a testy przechodzą.

## 2. Wybór istniejącego kierunku w kreatorze (I-16)

Problem: krok „Dodaj kierunek” w `SetupWizard` ma tylko pole nazwy i kolor, a `SetupViewModel.saveConfiguration` wysyła `StudyProgramRecord(id = 0)`, więc każdy nowy semestr tworzy nowy globalny kierunek.

Granice: wzoruj się na ekranie „Kierunki” (`CoursesBlock` w `SemesterScreen.kt`: wybór „Nowy kierunek” albo „Wybierz istniejący” i pole wyboru programu). Nie zmieniaj ekranu „Kierunki”. Wybrany istniejący kierunek nie może zmienić nazwy ani koloru, bo jest współdzielony (`ARCHITECTURE.md`, sekcja 7).

Kolejność:

1. `SetupViewModel`: obserwuj `semesterRepository.observeStudyPrograms()` i wystaw w `SetupWizardUiState` listę `programOptions` (identyfikator, nazwa, kolor). Dodaj `programMode` (nowy albo istniejący, domyślnie nowy; tryb istniejący dostępny tylko przy niepustej liście) i `selectedProgramId`.
2. `CourseStep` w `SetupWizard.kt`: gdy są globalne kierunki, pokaż wybór trybu jak na ekranie „Kierunki”. W trybie istniejącym pokaż `MakSelectField` z kierunkami, ukryj nazwę i paletę kolorów i dodaj informację, że kierunek jest współdzielony między semestrami.
3. `saveConfiguration`: w trybie istniejącym waliduj wybór („Wybierz kierunek.”) i wyślij rekord wybranego kierunku z jego bieżącą nazwą i kolorem z bazy. `saveSetupConfiguration` wywoła wtedy `update` z niezmienionymi wartościami; repozytorium zostaje bez zmian.
4. Powrót z kroku 3 do kroku 2 zachowuje wybrany tryb i kierunek.

Przypadki brzegowe: brak globalnych kierunków (wybór trybu ukryty); powrót do kroku 2 po zapisie i zmiana z kierunku istniejącego na nowy: sprawdź w `saveSetupConfiguration`, czy semestr nie zostaje z dwoma przypisaniami, i jeśli zostaje, zapisz to jako bloker zamiast zgadywać; wznowienie konfiguracji semestru bez przypisań (`SetupSemesterResume`).

Weryfikacja: `SetupViewModelTest`: wybór istniejącego kierunku nie tworzy nowego rekordu kierunku; nowy kierunek działa jak dotąd; walidacja braku wyboru. `SetupWizardTest`: wybór trybu i ukrycie koloru przy 320 dp. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: dodanie semestru z kierunkiem, który już istnieje, nie tworzy duplikatu globalnego kierunku.

## 3. Wybór semestru i filtra kierunku po identyfikatorze (I-17)

Problem: `SettingsScreen.kt` (aktywny semestr na ekranie głównym i na ekranie „Semestry”) oraz `ScheduleFilterSection` w `ScheduleScreen.kt` przekazują do `MakSelectField` listę nazw i szukają wybranej pozycji przez `firstOrNull { it.name == name }` albo `it.label == label`.

Kolejność:

1. We wszystkich trzech miejscach przekaż do `MakSelectField` listę obiektów (`SemesterUi`, `ScheduleFilterUi`) z `optionLabel`, a w `onSelected` użyj identyfikatora wybranego obiektu.
2. Etykiety rozróżnialne: gdy kilka pozycji ma tę samą nazwę, dodaj numer w kolejności listy, np. „Semestr zimowy (2)”, tak jak etykiety kalendarzy na ekranie semestru. Wydziel małą funkcję `distinctLabels(names: List<String>): List<String>` w `ui/components` i użyj jej w obu ekranach.
3. Pozostałe wywołania z wyszukiwaniem po etykiecie (motyw, cykl w formularzu zajęć) mają unikalne etykiety, więc zostaw je. Nie dodawaj nowych miejsc wyszukujących po nazwie.

Przypadki brzegowe: jedna pozycja; wszystkie nazwy różne (etykiety bez numerów); dwa kierunki o tej samej nazwie w filtrze „Planu”.

Weryfikacja: test JVM `distinctLabels`; `SettingsScreenTest` i `ScheduleScreenTest`: dwie pozycje o tej samej nazwie, wybór drugiej przekazuje jej identyfikator. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: żadne pole wyboru semestru ani kierunku nie wyszukuje pozycji po nazwie, a powtórzone nazwy są rozróżnialne.

## 4. Aktywacja nowego semestru według daty (I-18)

Decyzja z 2026-09-26: semestr utworzony w kreatorze staje się aktywny tylko wtedy, gdy dzisiejsza data mieści się w jego kalendarzu albo gdy nie ma aktywnego semestru (`DOMAIN.md`, sekcja „Semestr i tygodnie A/B”). Drugi warunek jest konieczny, bo bez aktywnego semestru utworzony plan byłby niedostępny.

Kolejność:

1. Domena: czysta funkcja `shouldActivateNewSemester(today: LocalDate, calendarStart: LocalDate, calendarEnd: LocalDate, hasActiveSemester: Boolean): Boolean` z testami JVM (data w zakresie, na granicach, przed, po, brak aktywnego semestru).
2. `SemesterRepository.saveSetupConfiguration`: nowy parametr `activate: Boolean`. Przy `true` zachowaj dotychczasowe `clearActive` i aktywację; przy `false` zapisz nowy semestr jako nieaktywny i nie zmieniaj pozostałych. Przy wznowieniu istniejącego semestru (`semester.id != 0`) zachowaj jego obecny stan aktywności. Test w `RoomPersistenceTest`: zapis nieaktywnego semestru nie zmienia aktywnego.
3. `SetupViewModel`: wstrzyknij `Clock` (jest w grafie Koin), przed zapisem sprawdź `observeActiveSemester().first()` i zapamiętaj w stanie `activatedSemester: Boolean`.
4. Krok „Dodaj zajęcia” w kreatorze dla semestru nieaktywnego: zamiast „Dodaj zajęcia” pokaż komunikat „Semestr zapisany. Zacznie obowiązywać, gdy wybierzesz go jako aktywny w ustawieniach.” i przyciski „Ustaw jako aktywny i dodaj zajęcia” (woła `setActiveSemester`, potem dotychczasowe `addClass`) oraz „Zakończ”. Formularz zajęć zapisuje do aktywnego semestru, więc bez aktywacji nie wolno otwierać go z kreatora.
5. Komunikat po zapisie: „Utworzono semestr i kierunek” przy aktywacji, a bez niej „Utworzono semestr i kierunek. Aktywny semestr się nie zmienił.”.
6. Dokumentacja: `FEATURES.md`, opis kreatora i ekranu „Semestry”.

Przypadki brzegowe: pierwszy semestr w aplikacji z datą w przyszłości (aktywny, bo nie ma innego); dzisiejsza data równa początkowi albo końcowi kalendarza (aktywny); „Dodaj semestr” w ustawieniach przy aktywnym bieżącym semestrze (nieaktywny); wznowienie konfiguracji semestru bez przypisań.

Poza zakresem: automatyczne przełączenie aktywnego semestru, gdy nadejdzie data nowego. Wymaga osobnej decyzji.

Weryfikacja: testy funkcji domenowej, `SetupViewModelTest` z `Clock.fixed` (z aktywacją i bez, przycisk aktywacji w kroku 3), test Room. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: dodanie semestru, którego kalendarz nie obejmuje dzisiejszej daty, nie zmienia aktywnego semestru, a pierwszy semestr zawsze jest aktywny.

## 5. Ostrzeżenie o ukrytych zmianach i notatkach przy edycji zajęć (I-19)

Decyzja z 2026-09-26: edycja zajęć zachowuje ich zmiany wystąpień i notatki, a formularz przed zapisem informuje, ile z nich przestanie być widocznych (`DOMAIN.md`, sekcja „Zmiany pojedynczych wystąpień”).

Kolejność:

1. Domena, jedna reguła terminu: wydziel z prywatnej `ScheduleResolver.isBaseOccurrence` publiczną funkcję, np. `ClassItem.hasBaseOccurrenceOn(date, calendar, overrides, weekCalculator)`, która sprawdza zakres kalendarza, dzień tygodnia, cykl A/B i datę zajęć jednorazowych. `ScheduleResolver` używa tej funkcji; testy resolvera przechodzą bez zmian.
2. Domena: `hiddenByClassEdit(before: ClassItem, after: ClassItem, beforeCalendar, afterCalendar, overrides, changes, notes): ClassEditImpact(changeCount, noteCount)`. Liczy zmiany wystąpień (po `originalDate`) i notatki do wystąpień (po `occurrenceDate`, czyli dacie oryginalnej) tych zajęć, których data jest terminem `before`, a nie jest terminem `after`. Kalendarz `after` wynika z nowego kierunku, bo zmiana kierunku może zmienić rytm A/B.
3. `ClassEditViewModel.save`: przy edycji istniejących zajęć policz wpływ z danych `activePlanData`. Gdy suma jest większa od zera i użytkownik nie potwierdził, ustaw w stanie `pendingEditImpact` i zakończ bez zapisu. `confirmSaveWithHiddenData()` zapisuje, `dismissEditImpact()` czyści stan. Nowe zajęcia i edycje bez wpływu zapisują się jak dotąd.
4. `ClassEditScreen`: dialog „Część danych przestanie być widoczna” z treścią „Nowy termin ukryje {n} zmian terminów i {m} notatek przypiętych do dotychczasowych dat. Dane zostaną zachowane i wrócą, jeśli przywrócisz poprzedni termin.”; pomiń zerową część, odmień liczebniki, akcje „Anuluj” i „Zapisz”.
5. Szkic w `SavedStateHandle` (I-12) nie obejmuje dialogu. Po odtworzeniu procesu dialog znika, a ponowny zapis pokaże go znowu.

Przypadki brzegowe: zmiana tylko godzin, sali albo nazwy (brak dialogu); zmiana z „co tydzień” na „tydzień A” (ukryte tylko terminy z tygodni B); zmiana dnia (wszystkie terminy); zmiana daty zajęć jednorazowych (notatka ze starej daty); zmiana kierunku na kierunek z innym kalendarzem; zmiany i notatki z dat poza semestrem (liczą się tylko, jeśli były terminem `before`).

Weryfikacja: testy JVM funkcji wpływu dla każdego przypadku brzegowego; `ClassEditViewModelTest`: dialog przed zapisem, brak zapisu bez potwierdzenia, zapis po potwierdzeniu, brak dialogu przy zmianie godzin; `ScheduleResolverTest` bez zmian. `gradlew.bat test connectedDebugAndroidTest`.

Kryterium zakończenia: edycja, która ukrywa zmiany albo notatki, wymaga potwierdzenia z ich liczbą, żadne dane nie są usuwane, a reguła terminu ma jedno źródło w domenie.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`: I-20 (drobne poprawki); I-14 czeka na decyzję o kopii zapasowej. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
