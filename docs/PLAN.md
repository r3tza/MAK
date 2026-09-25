# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest pięć poprawek z audytu z 2026-09-23. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

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

## 5. Notatka do wystąpienia podąża za terminem (I-09)

Zależność: krok 1 (I-08) musi być skończony, bo notatka korzysta z tej samej tożsamości wystąpienia.

Decyzja: `OccurrenceNote.occurrenceDate` oznacza datę oryginalną terminu (`DOMAIN.md`, sekcja o `OccurrenceNote`). Nazwy kolumny `occurrence_date` i pola JSON `occurrenceDate` zostają; zmienia się ich znaczenie.

Wspólna reguła przepinania istniejących notatek jest jedną czystą funkcją w `data/repository/OccurrenceNoteRemap.kt`. Przyjmuje proste modele notatek (id, zajęcia, data, treść), zmian (zajęcia, data oryginalna, rodzaj, data docelowa) i zajęć (id, dzień tygodnia, cykl) i zwraca notatki do aktualizacji oraz do usunięcia. Używają jej migracja i import; nie kopiuj reguły.

Reguła dla notatki N zajęć C z datą T:

1. Znajdź zmiany `MODIFIED` zajęć C z `targetDate == T` i `originalDate != T`.
2. Brak takich zmian albo więcej niż jedna: notatka zostaje bez zmian.
3. Dokładnie jedna zmiana z datą oryginalną D: jeśli C nie jest `ONCE`, dzień tygodnia C jest równy dniowi tygodnia T i C nie ma zmiany z `originalDate == T`, to w dniu T może istnieć zwykły termin C. Przypadek jest niejednoznaczny, więc notatka zostaje przy T. W pozostałych przypadkach notatka dostaje datę D.
4. Jeśli po przepięciu kilka notatek ma te same zajęcia i datę, połącz je w jedną: zostaje wiersz o najmniejszym `id`, treści są łączone pustą linią w kolejności rosnącej daty sprzed przepięcia, a pozostałe wiersze są usuwane. Treść nie może zginąć.

Kolejność:

1. Funkcja przepinania z testami JVM dla wszystkich punktów reguły.
2. `domain/ScheduleResolver.kt`, `render`: szukaj notatki po `classItem.id to originalDate` zamiast `actualDate`.
3. `ui/occurrence/OccurrenceViewModel.kt`: `buildDetails` szuka notatki po dacie oryginalnej, a `noteDate` przyjmuje `built.baseDate`. Zapis i usunięcie notatki używają daty oryginalnej.
4. Room v3: w `AppDatabase` podnieś `version` do 3, dodaj `MIGRATION_2_3` w `Migrations.kt`, zarejestruj ją obok `MIGRATION_1_2` i zapisz wyeksportowany `3.json`. Schemat tabel się nie zmienia; migracja czyta wiersze `occurrence_notes`, `occurrence_changes` i `classes`, woła funkcję przepinania i wykonuje `UPDATE` oraz `DELETE` w transakcji migracji. `day_of_week` jest liczbą ISO (poniedziałek to 1).
5. Eksport i import: `ExportSchema.VERSION` = 3. `ExportImporter.prepare` przyjmuje wersje 2 i 3; dla wersji 2 po walidacji przepina notatki tą samą funkcją, a potem sprawdza unikalność `(semester_id, class_id, occurrence_date)`. Inne wersje są odrzucane jak dotąd. Komunikat o nieobsługiwanej wersji ma wymieniać obie obsługiwane wersje.
6. Sprawdź `DemoDataSeeder`: notatka do przeniesionego terminu ma mieć datę oryginalną.
7. Dokumentacja: w `ARCHITECTURE.md`, sekcja 7, zmień opis schematu Room na wersję 3 i dopisz migrację z v2; w `LOG.md` dodaj wpis o wykonaniu razem z decyzją o imporcie wersji 2 i 3.

Przypadki brzegowe:

- notatka dodana do terminu, potem termin przeniesiony: po zmianie kodu widoczna w nowej dacie bez migracji;
- termin odwołany i przywrócony: notatka wraca;
- notatka niewidoczna po przeniesieniu (pod datą oryginalną) i notatka dopisana po przeniesieniu (pod datą docelową): po migracji jedna notatka z obiema treściami;
- przeniesienie na ten sam dzień tygodnia innego tygodnia: notatka zostaje przy T, zgodnie z punktem 3 reguły;
- zajęcia jednorazowe: bez zmian.

Weryfikacja:

- testy JVM funkcji przepinania, `ScheduleResolverTest` (notatka po przeniesieniu i po przywróceniu), `OccurrenceViewModelTest` (zapis notatki przeniesionego terminu trafia pod datę oryginalną), `ExportImporterTest` (plik w wersji 2 z przeniesioną notatką, plik w wersji 3, odrzucenie wersji 1), `JsonExportCodecTest` dla `schemaVersion` 3;
- `RoomMigrationTest`: migracja v2 do v3 na zachowanych danych dla przepięcia, przypadku niejednoznacznego i scalenia; łańcuch v1 do v3;
- `gradlew.bat test` i `gradlew.bat compileDebugAndroidTestKotlin`. Uruchomienie migracji na urządzeniu dopisz do odbioru O-01.

Kryterium zakończenia: notatka do wystąpienia jest widoczna przy przeniesionym terminie, istniejące dane i pliki w wersji 2 są przepinane tą samą regułą, żadna treść notatki nie ginie, a testy przechodzą.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`: I-12 wymaga najpierw osobnego planu, a I-14 czeka na decyzję użytkownika. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
