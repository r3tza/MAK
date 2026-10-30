# Plan

Gałąź `task/I-111-tomorrow-plan-and-ci` (decyzja użytkownika z 2026-10-21, `LOG.md`). Kroki wykonuj po kolei; każdy kończy się kompilującym się kodem, zielonymi testami i jednym commitem. Nie wypychaj gałęzi bez polecenia użytkownika.

Zasady dla wykonawcy:

- Windows: `gradlew.bat`, Python przez `py` (`STACK.md`, sekcja „Narzędzia”). Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py` (kod 0).
- Czytaj tylko pliki wymienione w kroku. Jeśli nazwa w kodzie różni się od planu, zachowaj cel kroku i opisz różnicę w commicie.
- Emulator: `Medium_Phone_2` z 2048 MiB (`Medium_Phone_A12` z `STACK.md` już nie istnieje, `KNOWN_ISSUES.md`). `connectedDebugAndroidTest` odinstalowuje aplikację; przed ręcznym sprawdzeniem uruchom `gradlew.bat installDebug`.
- Teksty dla użytkownika piszesz dokładnie tak, jak w planie. Bez znaków `·`, `—` i strzałek.
- Po kroku zaktualizuj wiersz zadania w `QUEUE.md` (status i krótkie „Zrobione: główne klasy i testy”).

## 1. Popraw drobiazgi I-109 i włącz Android Lint w CI (I-105)

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

## 2. Uruchamiaj testy Room w CI i skróć wiersze kolejki I-108 (I-106)

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
