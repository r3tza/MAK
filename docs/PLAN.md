# Plan

Najbliższy zakres: I-113, plan na polecenie użytkownika z 2026-11-15. Implementacja nie jest rozpoczęta. Poprzedni zakres I-112 jest wydany w 0.5.0; otwarte odbiory pozostają w `QUEUE.md`.

Gałąź `task/I-113-update-install-and-notifications` od `origin/main` z 2026-11-19. Na tej samej gałęzi użytkownik planuje kolejne zmiany aktualizatora, w tym powiadomienie o dostępnej aktualizacji; ich zakres wymaga osobnej decyzji i nie należy do tego planu. Nie wypychaj gałęzi bez polecenia użytkownika.

Zasady dla wykonawcy:

- Windows: `gradlew.bat`, Python przez `py` (`STACK.md`, sekcja „Narzędzia”). Po zmianie plików `docs/*.md` uruchom `py scripts/check_map.py` (kod 0).
- Czytaj tylko pliki wymienione w kroku. Jeśli nazwa w kodzie różni się od planu, zachowaj cel kroku i opisz różnicę w commicie.
- Emulator: AVD telefonu dostępny na tym komputerze, z 2048 MiB (`STACK.md`, sekcja „Narzędzia”). `connectedDebugAndroidTest` odinstalowuje aplikację; przed ręcznym sprawdzeniem uruchom `gradlew.bat installDebug`.
- Teksty dla użytkownika piszesz dokładnie tak, jak w planie. Bez znaków `·`, `—` i strzałek.
- Po kroku zaktualizuj wiersz zadania w `QUEUE.md` (status i krótkie „Zrobione: główne klasy i testy”).

## 1. Zachowaj APK po anulowaniu instalacji przez trzy minuty (I-113)

Cel: po anulowaniu systemowej instalacji pozwól ponowić ją z pobranego APK przez 180 sekund od otrzymania wyniku anulowania. Ponowienie ma działać bez pobierania APK i bez sprawdzania wydania w sieci. Przed przekazaniem pliku instalatorowi ponownie użyj istniejącej weryfikacji. Po upływie limitu albo utracie pliku zaoferuj pobranie. Zachowaj obecny układ ekranu.

Założenie zakresu: szybkie ponowienie w czasie życia `UpdateViewModel`, także po wyjściu z ekranu i powrocie, jeśli ViewModel nadal istnieje. Odtwarzanie stanu po śmierci procesu pozostaje poza tym planem. Nie dodawaj zapisu metadanych cache, zależności, alarmu ani zadania w tle. Oczekiwanie na pierwszą instalację i nadawanie zgody systemowej nie uruchamiają limitu. Nie zmieniaj obsługi nieudanej instalacji: nadal usuwa plik i oferuje „Pobierz ponownie”.

Pliki do przeczytania: `FEATURES.md` (ekran „Aktualizacja”), `ARCHITECTURE.md` (sekcje 4, 5 i aktualizacje w sekcji 7), `STACK.md` (sekcje 5 i 6), `WRITING.md` oraz pliki wymienione poniżej. Nie zmieniaj bazy, eksportu, synchronizacji ani notatek wydania.

Pliki do zmiany:

- `app/src/main/java/dev/retza/mak/update/UpdateInstaller.kt`;
- `app/src/main/java/dev/retza/mak/update/UpdateViewModel.kt`;
- `app/src/main/java/dev/retza/mak/update/ApkDownloader.kt`;
- `app/src/main/java/dev/retza/mak/ui/settings/UpdateScreen.kt`;
- `app/src/test/java/dev/retza/mak/update/UpdateViewModelTest.kt`;
- `app/src/androidTest/java/dev/retza/mak/ui/settings/UpdateScreenTest.kt`;
- `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/LOG.md`, `docs/QUEUE.md` i `docs/PLAN.md`.

Zmiany w kolejności wykonania:

1. W `FEATURES.md` zastąp regułę bezwarunkowego „Pobierz ponownie” po anulowaniu instalacji regułą ponowienia przez 180 sekund. W opisie aktualizacji w `ARCHITECTURE.md` dopisz cykl życia pliku i ograniczenie do działającego procesu. W `LOG.md` zapisz źródło zmiany: zlecenie użytkownika z 2026-11-15 dotyczące zaplanowania przechowania APK; szczegóły techniczne są założeniami tego planu. Przy implementacji odnotuj wykonanie na zlecenie użytkownika. Zachowaj limit 20 wpisów, przenosząc najstarszy do `docs/log_archive/2026.md`, jeśli potrzeba.
2. W `UpdateInstallReceiver.onReceive` pozostaw APK przy `STATUS_FAILURE_ABORTED`. Sukces i pozostałe błędy nadal usuwają plik; `STATUS_PENDING_USER_ACTION` go zachowuje. Zmień też bezwarunkowe kasowanie pliku przy każdym zdarzeniu w `UpdateViewModel`. Zmiana jednego z tych miejsc nie wystarczy.
3. W `UpdateViewModel` zapamiętaj wraz z `verifiedFile` tożsamość pobrania (`versionCode`, `sha256`) i termin `retryUntilMillis`. Dodaj `canRetryInstallation: Boolean = false` do `UpdateUiState`. Przy `InstallEvent.Cancelled` i istniejącym pliku ustaw termin na `clock.millis() + 180_000`, status `InstallCancelled` i możliwość ponowienia. Użyj jednego anulowalnego `Job` z `delay`, aby po terminie wyłączyć możliwość ponowienia również na otwartym ekranie. Każde kolejne anulowanie rozpoczyna nowy limit.
4. W `installUpdate()` blokuj podwójne wywołanie podczas weryfikacji i instalacji. Dla ponowienia wymagaj `clock.millis() < retryUntilMillis`, istniejącego pliku i tożsamości zgodnej z `availableUpdate`. Przy braku pliku, przekroczonym terminie lub innym wydaniu wyczyść odniesienie i możliwość ponowienia; nie pobieraj automatycznie. Ponownie wywołaj `verifier.verify(file, info.sha256)` w korutynie i dopiero po `Valid` uruchom instalator. Istniejące błędy weryfikacji zachowują swoje komunikaty. Brak pliku i błąd odczytu obsłuż jawnie, bez wyjątku na ekranie. Przy powrocie ze zgody przez `resumeInstallationAfterPermission()` sprawdź limit ponownie.
5. Po rozpoczęciu ponownej instalacji anuluj poprzedni timer, aby nie zmieniał stanu `Installing`. Sukces, błąd, nowe pobranie i zmiana dostępnego wydania unieważniają poprzedni termin. Nowa suma kontrolna nie może zostać połączona ze starym APK. Jeśli termin minął podczas weryfikacji, nie uruchamiaj instalatora. Po sukcesie usuń plik od razu; po wygaśnięciu usuń go przy najbliższej operacji aktualizatora. W `HttpApkDownloader.download()` przed nowym pobraniem sprzątnij także pozostałe APK w prywatnym katalogu `cache/updates`, aby pliki osierocone po śmierci procesu nie narastały. Nie usuwaj innych katalogów cache.
6. W `UpdateScreen` pozostaw komunikat „Instalacja została anulowana.”. Przy `InstallCancelled` i `canRetryInstallation` pokaż istniejący przycisk wtórny „Zainstaluj ponownie” z akcją `onInstall`. Po wygaśnięciu lub utracie pliku pokaż „Pobierz ponownie” z akcją `onDownload`. Nie dodawaj odliczania, karty ani ikony. Pozostałe stany błędów zachowują obecne akcje.
7. Dodawaj testy razem ze zmianą logiki. Po weryfikacji zaktualizuj status I-113 w `QUEUE.md`, zapisz wykonanie i wyniki w `LOG.md`, a tutaj oznacz wykonany krok. Kod i dokumentację utrwal jednym commitem `fix: retain downloaded update after cancelled installation`. Bez odbioru na urządzeniu status pozostaje `odbiór otwarty`.

Testy do dodania lub poprawienia:

- W `UpdateViewModelTest` rozszerz konstruktor pomocniczy o wstrzykiwane `downloader`, `verifier`, `installer`, `installEvents` i `clock`. Użyj tymczasowego pliku APK, sterowanego zegara i schedulera korutyn, bez sieci i rzeczywistego oczekiwania.
- Sprawdź anulowanie i ponowienie przed terminem: ten sam plik, ponowna weryfikacja, jedno pobranie i ponowne wywołanie instalatora. Sprawdź granicę: 179 999 ms pozwala ponowić, 180 000 ms już nie, także gdy przycisk był widoczny przed kliknięciem.
- Sprawdź plik usunięty przez system oraz plik zmieniony po pierwszej weryfikacji: brak instalacji, możliwość pobrania dla braku pliku i istniejący błąd weryfikacji dla uszkodzenia.
- Sprawdź nowe wydanie podczas oczekiwania, kolejne anulowanie odnawiające limit i poprzedni timer kończący się podczas ponownej instalacji. Sprawdź jedno wywołanie instalatora przy podwójnym kliknięciu podczas weryfikacji.
- Sprawdź, że sukces i błąd instalacji usuwają plik, a zgoda przed pierwszą instalacją nie zużywa limitu. Granice czasu testuj w ViewModelu, bez powielania ich w Compose.
- W `UpdateScreenTest` dodaj test zmiany `canRetryInstallation` na tym samym ekranie: „Zainstaluj ponownie” wywołuje tylko `onInstall`, a po wygaśnięciu „Pobierz ponownie” wywołuje tylko `onDownload`. Rozszerz `show` o `onInstall`.

Przypadki brzegowe: dokładnie 180 sekund; usunięty lub uszkodzony plik; śmierć procesu (bez odtworzenia); brak ViewModelu przy wyniku anulowania (plik sprzątany przy następnym pobraniu); wielokrotne anulowanie; wyjście z ekranu; zgoda systemowa; podwójne kliknięcie; zmiana wersji albo sumy kontrolnej; timer podczas weryfikacji i instalacji. Sprzątanie dotyczy tylko plików aktualizacji.

Weryfikacja:

1. Uruchom `gradlew.bat --offline :app:testDebugUnitTest --tests "dev.retza.mak.update.UpdateViewModelTest" --tests "dev.retza.mak.update.ApkVerifierTest"`. Nie uruchamiaj pełnego zestawu aplikacji.
2. Uruchom `gradlew.bat --offline connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=dev.retza.mak.ui.settings.UpdateScreenTest"` na jednym dostępnym AVD telefonu z 2048 MiB RAM. Zapisz nazwę AVD i wynik. Sama kompilacja nie wystarcza.
3. Wykonaj zrzuty ekranu aktualizacji przed zmianą i po niej przy 320 dp w obu motywach. Po zmianie pokaż oba warianty akcji po anulowaniu. Sprawdź focus, obszar dotyku, powiększony tekst i brak nowych ozdobników. Do zrzutów stanów można użyć testu Compose; nie dodawaj przełącznika do aplikacji.
4. Na urządzeniu z nowszym, zgodnie podpisanym APK anuluj rzeczywiste okno instalatora, wyłącz sieć i ponów przed limitem. Potwierdź otwarcie instalatora bez pobierania. Po kolejnym anulowaniu zaczekaj do końca limitu i potwierdź „Pobierz ponownie”. Sprawdź zgodę na nieznane aplikacje oraz sprzątanie po sukcesie i przy następnym pobraniu. Jeśli brak odpowiedniego APK, zapisz ten konkretny brak odbioru. Atrapa zdarzenia nie potwierdza działania receivera.
5. Uruchom `py scripts/check_map.py` i przejrzyj diff. Wymagany kod zakończenia kontroli dokumentacji: 0.

Kryterium zakończenia: anulowanie rzeczywistej instalacji zachowuje APK i przez 180 sekund pozwala ponowić instalację bez sieci. Po terminie, utracie pliku albo zmianie wydania stary APK nie jest instalowany. Testy JVM i Compose przechodzą, zrzuty potwierdzają poprawny przycisk, a cykl życia pliku jest sprawdzony na urządzeniu. Oddziel gotową implementację od niewykonanego odbioru.
