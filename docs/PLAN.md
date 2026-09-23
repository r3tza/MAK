# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są cztery poprawki z audytu z 2026-09-23. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Unikalny identyfikator wystąpienia (I-08)

Problem: `PlannedOccurrence.id` ma postać `classId:date`, gdzie `date` to data faktyczna. Termin przeniesiony na dzień, w którym te same zajęcia mają zwykły termin, dostaje ten sam identyfikator. `CollisionDetector` pomija wtedy parę (`first.id == second.id`), `collisionRanges` i powiadomienia łączą oba terminy, a `OccurrenceViewModel.buildDetails` może otworzyć niewłaściwą zmianę.

Zasada: wystąpienie identyfikują zajęcia i data oryginalna, czyli data wynikająca z planu cyklicznego. Tak samo robi iCalendar (`RECURRENCE-ID`). Para jest unikalna, bo zajęcia mają najwyżej jeden zwykły termin w danej dacie, a `occurrence_changes` ma unikalny indeks `(semester_id, class_id, original_date)`. Dla zajęć `ONCE` data oryginalna jest równa dacie zajęć. Identyfikatory rekordów Room się nie zmieniają.

Granice: nie zmieniaj schematu Room, klucza notatek do wystąpienia (to I-09) ani formatu trasy `classId:data`. Zmienia się tylko znaczenie daty w trasie: od teraz to data oryginalna.

Kolejność:

1. `domain/Models.kt`: `PlannedOccurrence.id` zwraca `"${classItem.id}:$originalDate"`. Dopisz krótki komentarz, że identyfikator opiera się na dacie oryginalnej.
2. `ui/occurrence/OccurrenceArgs.kt`: zmień nazwę pola `date` na `originalDate`, format trasy zostaje bez zmian.
3. `ui/occurrence/OccurrenceViewModel.kt`, `buildDetails`: szukaj zmiany wyłącznie po `it.originalDate == args.originalDate`. Usuń warunek `it.targetDate == displayDate`. Data faktyczna pozostaje `change?.targetDate ?: originalDate`.
4. `OccurrenceViewModel.saveOccurrenceChange`: po zapisie wołaj `reload(..., original)` zamiast `reload(..., decision.slot.date)`, bo trasa wskazuje teraz datę oryginalną.
5. Sprawdź pozostałych konsumentów `PlannedOccurrence.id`: `collisionKey`, `collisionRanges`, `collisionNotificationGroups`, `CollisionAlarmReceiver`, `WidgetPresenter`, `PlanMapping` i nawigację `openOccurrence`. Nie powinny wymagać zmian poza testami. Jeśli któryś odczytuje datę z identyfikatora, przełącz go na pole `date` albo `originalDate` wystąpienia.

Przypadki brzegowe:

- cotygodniowe zajęcia w poniedziałek, termin z 6.10 przeniesiony na 13.10 na inną godzinę: dwa wystąpienia 13.10 z różnymi identyfikatorami i kolizja, gdy godziny się nakładają;
- zmiana godziny bez zmiany daty: identyfikator bez zmian;
- zajęcia jednorazowe;
- odwołany termin: brak wystąpienia, szczegóły po trasie z datą oryginalną pokazują stan „Odwołane”;
- po aktualizacji aplikacji stare alarmy mają identyfikatory w starym formacie. `MY_PACKAGE_REPLACED` odświeża alarmy, a alarm, którego identyfikatory nie pasują do bieżącego planu, niczego nie pokazuje. Nie dodawaj osobnej migracji alarmów.

Weryfikacja:

- `ScheduleResolverTest`: przeniesienie na dzień ze zwykłym terminem daje dwa wystąpienia o różnych identyfikatorach.
- `CollisionDetectorTest`: kolizja między tymi dwoma wystąpieniami jest wykrywana.
- `OccurrenceViewModelTest`: dla daty 13.10 trasa zwykłego terminu i trasa przeniesionego terminu (data 6.10) otwierają różne szczegóły.
- Popraw `OccurrenceArgsTest` i testy, które zakładały datę faktyczną w identyfikatorze.
- Uruchom `gradlew.bat test` i `gradlew.bat compileDebugAndroidTestKotlin`.

Kryterium zakończenia: w aktywnym planie żadnego dnia nie ma dwóch wystąpień o tym samym identyfikatorze, a opisany przypadek jest pokryty testami JVM.

## 2. Zegar zgodny z bieżącą strefą systemu (I-10)

Problem: `AppModule.provideClock()` zwraca jeden obiekt `Clock.systemDefaultZone()` na cały proces. Taki zegar zapamiętuje strefę z chwili utworzenia, więc po zmianie strefy odbiornik `TIMEZONE_CHANGED` przelicza alarmy w starej strefie.

Kolejność:

1. Dodaj w `di` małą klasę `SystemZoneClock : Clock()`: `getZone()` zwraca `ZoneId.systemDefault()` przy każdym wywołaniu, `instant()` zwraca `Instant.now()`, a `withZone(zone)` zwraca `Clock.system(zone)`.
2. `AppModule.provideClock()` zwraca `SystemZoneClock()`. Nie zmieniaj konstruktorów klas, które przyjmują `Clock`.
3. `DemoDataSeeder` zostaw bez zmian: używa zegara jednorazowo przy starcie.

Weryfikacja: test JVM zmienia `TimeZone.setDefault` i sprawdza, że `LocalDate.now(clock)` i `clock.zone` odpowiadają nowej strefie. W `finally` przywraca poprzednią strefę. Uruchom `gradlew.bat test`. Zmianę strefy na urządzeniu sprawdza O-04.

Kryterium zakończenia: żaden komponent nie trzyma strefy z chwili startu procesu, a test przechodzi.

## 3. Obsługa błędów zapisu i pracy w tle (I-11)

Problem: wyjątek z repozytorium (np. `require` przy nieistniejącym kalendarzu) w poniższych miejscach nie ma obsługi i zamyka aplikację bez komunikatu. `ARCHITECTURE.md` wymaga jawnego stanu zapisu.

Kolejność:

1. `OccurrenceViewModel.deleteSelectedClass`: `try` z ponownym rzuceniem `CancellationException`. Przy błędzie nie wysyłaj `CloseDetails`, ukryj potwierdzenie i opublikuj `UiFeedback("Nie udało się usunąć zajęć.", UiFeedbackKind.Error)`.
2. `ScheduleViewModel`: dodaj w konstruktorze `FeedbackSink`, tak jak w `OccurrenceViewModel`. W `saveVisibleWeekOverride` i `clearVisibleWeekOverride` obsłuż błąd komunikatami „Nie udało się zapisać korekty tygodnia.” i „Nie udało się usunąć korekty tygodnia.”. Zaktualizuj `KoinGraphTest`, jeśli wymaga zmian, i `ScheduleViewModelTest`.
3. `MakApplication`: w kolektorze wywołaj `scheduler.refresh()` w `try`, łap `Exception`, a `CancellationException` rzucaj dalej, żeby kolejne zmiany danych nadal odświeżały alarmy.
4. `CollisionAlarmReceiver.runAsync` i `NotificationRescheduleReceiver.onReceive`: łap `Exception` wokół bloku przed `finally { pendingResult.finish() }`.
5. `WidgetPlanLoader.load`: zamień `runCatching` na `try/catch`, który rzuca dalej `CancellationException`, a inne wyjątki zamienia na `WidgetUiState.Error`.

Weryfikacja: testy JVM z repozytorium testowym, które rzuca wyjątek, dla punktów 1 i 2 (komunikat błędu, brak efektu zamknięcia, zachowany stan) oraz test loadera widgetu dla `CancellationException`. Uruchom `gradlew.bat test` i `gradlew.bat compileDebugAndroidTestKotlin`.

Kryterium zakończenia: żadne `viewModelScope.launch` z zapisem w `ui` ani zadanie w tle nie zostawia wyjątku bez obsługi. Sprawdź to wyszukaniem `viewModelScope.launch` i `CoroutineScope(` w kodzie.

## 4. Drobne poprawki z audytu (I-13)

Każdy punkt to osobna mała zmiana. Punkt, którego nie wykonujesz, odrzuć z uzasadnieniem w `LOG.md`.

1. `NotificationRescheduleReceiver`: obsługuj tylko akcje z manifestu (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`), inne ignoruj. Usuwa to ostrzeżenie lint `UnsafeProtectedBroadcastReceiver`.
2. Import w `MainActivity`: przed `readBytes()` sprawdź rozmiar pliku. Limit 5 MB. Większy plik kończy się komunikatem „Plik jest za duży, aby był kopią MAK.” bez wczytywania do pamięci.
3. `ScheduleViewModel` ustala dzisiejszą datę raz w konstruktorze. Po powrocie do aplikacji następnego dnia „Plan” pokazuje wczorajszy tydzień. Odśwież datę przy `ON_RESUME` tak jak `TodayViewModel.refreshToday`, ale tylko wtedy, gdy użytkownik nie wybrał innej daty.
4. Ostrzeżenia kompilatora: `@OptIn(FlowPreview::class)` dla `debounce` w `MakApplication`, warunki zawsze prawdziwe w `ExportImporter.kt:211` i zbędne wywołania w `SettingsViewModel.kt:347-359`. Ostrzeżenia lint `ModifierParameter`, `UnusedResources` i `UseKtx` popraw, jeśli zmiana nie wpływa na zachowanie.
5. Dokumentacja środowiska w `STACK.md`, sekcja 6: na Windowsie interpreter nazywa się `python`, a Gradle wymaga `local.properties` z `sdk.dir` albo zmiennej `ANDROID_HOME`. Uzupełnij polecenia w `AGENTS.md` i `WORKFLOW.md`, żeby działały na obu nazwach interpretera.
6. Znak em dash w `AGENTS.md` i `ARCHITECTURE.md` zastąp zwykłą interpunkcją zgodnie z `WRITING.md`. Nie zmieniaj znaczenia zdań.
7. Wersja release: przed włączeniem `isMinifyEnabled` dodaj reguły R8 dla `kotlinx.serialization` i klas eksportu. Obecnie tylko zapisz to jako warunek w `STACK.md`; nie włączaj minifikacji w tym kroku.

Weryfikacja: `gradlew.bat test lintDebug` oraz `python scripts/check_map.py`. Porównaj liczbę ostrzeżeń lint z raportem sprzed zmiany (16).

Kryterium zakończenia: wszystkie punkty wykonane albo odrzucone z uzasadnieniem, bez nowych ostrzeżeń.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`: I-12 wymaga najpierw osobnego planu, a I-09 i I-14 czekają na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
