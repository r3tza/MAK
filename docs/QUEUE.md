# Kolejka prac

Ten plik jest źródłem bieżącego statusu zadań. `PLAN.md` wybiera najwyżej pięć najbliższych kroków, a `WORKFLOW.md` wyjaśnia znaczenie statusów. Wymagania są w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md` i `ARCHITECTURE.md`. Kolejność jest zalecana, chyba że użytkownik wskaże inne zadanie. Nie zmieniaj statusu na `gotowe` na podstawie samej kompilacji testów Android.

| ID | Status | Zadanie i kryterium zakończenia | Zależność |
|---|---|---|---|
| I-01 | w toku | Podzielić `MakRepository` według granic danych, przenieść konsumentów i zachować atomowy import. Granice: `ARCHITECTURE.md`; kroki: `PLAN.md`. Zrobione: `ScheduleRepository` z `observeActivePlanData(semesterId)` i metodami zajęć, notatek i zmian na typach `ClassRecord`, `OccurrenceNoteRecord`, `OccurrenceChangeRecord`; aktywny semestr składa fasada `MakRepository.observeActiveSemester()`; `MakRepository` deleguje metody planu do `ScheduleRepository`; grupa odczytu planu (`TodayViewModel`, `WidgetPlanLoader`, `MakTodayWidget`, powiadomienia, `MakApplication`) zdjęta z `MakRepository`: aktywny semestr z `SemesterRepository`, plan z `ScheduleRepository`. Zrobione też: `SemesterRepository` (`RoomSemesterRepository`) z typami `SemesterRecord`, `StudyProgramRecord`, `AcademicCalendarRecord`, `SemesterProgramRecord`, `WeekOverrideRecord`; `RoomMakRepository` wymaga obu repozytoriów bez wartości domyślnej. `ScheduleViewModel`, `OccurrenceViewModel`, `ClassEditViewModel`, `SemesterViewModel` i `SetupViewModel` przeszły na `SemesterRepository` i `ScheduleRepository` bez `SemesterWithData`. Pozostaje `PlanBackupService`, przepięcie ustawień, `AppViewModel` i seedera, usunięcie fasady oraz kroki dokumentacyjne. | brak |
| I-02 | do implementacji | Pokazać trzy wartości na „Dzisiaj”, policzyć okienka ze wszystkich kierunków i zapisać globalny próg. Reguła: `DOMAIN.md`; układ: `FEATURES.md`; kroki: `PLAN.md`. | brak |
| I-03 | do implementacji | Rozdzielić metadane i oba rodzaje notatek na kartach zajęć. `FEATURES.md`: Struktura karty zajęć; kroki: `PLAN.md`. | brak |
| I-07 | do implementacji | Dodać tylko do odczytu `scripts/check_map.py` dla linków dokumentacji, limitów planu i logu oraz spójności kolejki. Kroki: `PLAN.md`. | brak |
| I-04 | do implementacji | Poprawić odstępy i neutralny stan sekcji rozwijanych. `ARCHITECTURE.md`: Stały język wizualny. | brak |
| I-05 | do implementacji | Uporządkować główne ustawienia i dodać osobne ekrany. `FEATURES.md`: Ustawienia i dane. | I-02 dla ustawienia progu okienka |
| I-06 | do implementacji | Dokończyć układ i wygląd widgetu, potem porównać warianty na launcherze. `FEATURES.md`: Widget. | brak |
| O-01 | odbiór otwarty | Uruchomić migrację Room v1 do v2 na urządzeniu i potwierdzić zachowanie danych. `FEATURES.md`: Odbiór na urządzeniu. | brak |
| O-02 | odbiór otwarty | Sprawdzić rozdzielanie i łączenie kalendarzy oraz plan dwóch kierunków. `FEATURES.md`: Odbiór na urządzeniu. | brak |
| O-03 | odbiór otwarty | Sprawdzić import, anulowanie i rollback na urządzeniu. `FEATURES.md`: Odbiór na urządzeniu. | brak |
| O-04 | odbiór otwarty | Sprawdzić zgodę, alarmy, restart i kliknięcie powiadomienia. `FEATURES.md`: Odbiór na urządzeniu. | brak |
| O-05 | odbiór otwarty | Sprawdzić nawigację, TalkBack, klawiaturę, focus, motywy i 320 dp. `FEATURES.md`: Odbiór na urządzeniu. | brak |
| O-06 | odbiór otwarty | Sprawdzić bazowy widget na launcherze i odświeżanie po zmianach danych. `FEATURES.md`: Odbiór na urządzeniu. | brak |

Odbiór zmian z I-02 do I-06 należy do kryterium tych zadań. O-06 dotyczy istniejącego bazowego widgetu; nie zastępuje odbioru docelowego wyglądu z I-06.

## Pomysły do rozważenia

Poniższe propozycje nie są zatwierdzonymi zadaniami. Przed przeniesieniem do planu trzeba ustalić zakres i kryteria zakończenia.

- `scripts/check_text.py`: kontrola zasad pisania z `WRITING.md` dla nowych lub zmienianych tekstów. Przed wdrożeniem zdecydować, jak traktować historyczne dokumenty i istniejące naruszenia, aby skrypt nie blokował każdej zmiany z powodu starej treści.
- CI: po sprawdzeniu stabilności `check_map.py` rozważyć uruchamianie go przy zmianach dokumentacji. Jeśli powstanie `check_text.py`, dołączyć go dopiero po ustaleniu zakresu. Nie dodawać hooka ani obowiązkowej bramki CI w kroku I-07.
