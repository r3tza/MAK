# Propozycja opcjonalnej synchronizacji planu

Status: propozycja do decyzji użytkownika. Ten dokument nie zmienia zakresu MVP i nie upoważnia do implementacji. Obecne decyzje pozostają w `PRODUCT.md`, `DOMAIN.md`, `FEATURES.md`, `ARCHITECTURE.md` i `STACK.md`.

## Cel i zakres

Umożliwić korzystanie z jednego planu na kilku telefonach przez konto Google. Plan pozostaje dostępny i edytowalny bez internetu. Synchronizacja jest domyślnie wyłączona. Użytkownik może ją wybrać w kreatorze albo później w ustawieniach, bez przerywania lokalnej konfiguracji przy braku sieci lub błędzie logowania.

Synchronizacja obejmuje globalne kierunki, semestry, przypisania kierunków, kalendarze akademickie, zajęcia, korekty tygodni, zmiany wystąpień i oba rodzaje notatek. Aktywny semestr, motyw, powiadomienia i pozostałe ustawienia telefonu pozostają lokalne. Wybór aktywnego semestru w Room trzeba zachować przy zastosowaniu zmian z innego telefonu; jeśli wybrany semestr został usunięty, stosować istniejącą regułę wyboru innego semestru.

Plan ma być przechowywany w ukrytym folderze danych aplikacji na Dysku Google z zakresem `drive.appdata`. Użytkownik wybrał brak dodatkowego hasła. Nie przedstawiać tej kopii jako szyfrowanej od końca do końca. Systemowa kopia zapasowa Androida i ręczny eksport JSON pozostają osobnymi funkcjami.

## Zachowanie

1. Po zapisaniu zmiany spróbować synchronizacji. Ponowić ją po otwarciu aplikacji oraz orientacyjnie co godzinę w tle, gdy synchronizacja jest włączona. Android może opóźnić zadanie okresowe. Udostępnić ręczną próbę oraz pokazać stan oczekujących zmian, ostatnie udane wykonanie i błąd wymagający działania.
2. Przy pierwszym połączeniu pustego konta wysłać plan z telefonu. Gdy telefon nie ma planu, a konto go ma, pobrać plan. Gdy obie strony mają niezależne plany, pokazać podgląd i pozwolić ręcznie dopasować odpowiadające sobie semestry, kierunki, kalendarze i wpisy. Najpierw dopasować obiekty nadrzędne, potem zależne. Nie uznawać samej nazwy ani lokalnego numeru Room za tożsamość. Pokazać wynik przed zapisem i zachować możliwość odzyskania obu wersji.
3. Po połączeniu scalać niezależne zmiany automatycznie na podstawie wspólnej wersji bazowej. Zmiany różnych pól jednego wpisu scalać tylko wtedy, gdy nie rozrywają powiązanych wartości i wynik przechodzi walidację domenową. Tę samą wartość zmienioną na obu telefonach, edycję usuniętego wpisu, edycję dziecka usuniętego obiektu i wynik niespełniający reguł pokazać do ręcznego rozstrzygnięcia. Zachować obie wersje do czasu decyzji.
4. Po wyłączeniu synchronizacji zapytać, czy zachować kopię na Dysku. Jeśli użytkownik wybierze jej usunięcie, wyjaśnić, że usunięcie danych z folderu aplikacji jest trwałe. Pokazać dodatkowe ostrzeżenie, że inny podłączony telefon może ponownie przesłać plan, i pozwolić wykonać operację. Nie wymagać zgody ani odpowiedzi innych telefonów. Brak kopii zdalnej nigdy sam nie usuwa planu lokalnego.
5. Po przywróceniu systemowej kopii Androida lub ponownej instalacji traktować lokalny stan synchronizacji jako nieznany. Ponownie ustalić stan konta i obu planów. Nie uznawać przywróconej bazy za już zsynchronizowaną.

## Wzorce z istniejących aplikacji

- [Joplin](https://joplinapp.org/help/dev/spec/sync/) zapisuje dane lokalnie, śledzi stan poszczególnych elementów i szybko wysyła zmiany. Jego [obsługa konfliktów](https://joplinapp.org/help/apps/conflict/) zachowuje kopię do porównania. W MAK warto śledzić zmiany wpisów i zachować obie wersje konfliktu.
- [Anki](https://docs.ankiweb.net/syncing.html) rozróżnia przypadki pierwszego połączenia i ostrzega, gdy zawartość istnieje po obu stronach. MAK potrzebuje w tym przypadku ręcznego dopasowania wybranego przez użytkownika.
- [KeePass](https://keepass.info/help/v2/sync.html) unika automatycznego łączenia wszystkich pól, ponieważ może to utworzyć niespójny wpis. W MAK należy traktować razem zależne pola czasu, cyklu i daty, a każdy wynik scalenia sprawdzić.
- [Obsidian](https://obsidian.md/help/sync/setup) odróżnia odłączenie urządzenia od usunięcia danych z chmury i ostrzega przed połączeniem dwóch niepustych zbiorów. Jego [tryb pliku konfliktowego](https://obsidian.md/help/sync/troubleshoot) pokazuje wartość zachowania obu wersji do rozstrzygnięcia.

## Audyt obecnej aplikacji

Audyt dotyczy gotowości aplikacji do tej propozycji. Nie jest pełnym audytem wszystkich funkcji ani potwierdzeniem działania synchronizacji na urządzeniu.

P0 oznacza warunek konieczny przed wdrożeniem synchronizacji, a P1 oznacza ważną zmianę lub test w projekcie funkcji. Te oznaczenia nie opisują awarii obecnej aplikacji.

| Priorytet | Ustalenie | Wpływ na plan |
|---|---|---|
| P0 | `data/entity/Entities.kt` nadaje lokalne numery `Long` wszystkim ośmiu typom wpisów. `export/ExportSnapshot.kt` przenosi te numery do JSON. | Dwa niezależne telefony mogą nadać różnym wpisom ten sam numer. Przed scalaniem dodać trwałą tożsamość niezależną od lokalnego klucza Room, migrację danych i mapowanie relacji. |
| P0 | `data/repository/PlanBackupGateway.kt` ma `replaceAll`, które usuwa wszystkie wpisy i odtwarza je w transakcji. | Zachować tę operację dla ręcznego importu. Synchronizacja wymaga osobnego atomowego zastosowania scalonych zmian. Musi zachować lokalny wybór aktywnego semestru. |
| P0 | `PlanBackupGateway.snapshot()` odczytuje semestry i globalne kierunki w dwóch osobnych operacjach. | Objąć odczyt całego planu jedną transakcją przed użyciem go jako wersji do synchronizacji. Ponownie sprawdzić lokalny stan przed zastosowaniem pobranych zmian. |
| P0 | Obecna propozycja w `ARCHITECTURE.md` zakłada jeden nadpisywany JSON i wybór całej wersji przy konflikcie. | Zastąpić ją po decyzji użytkownika protokołem, który zachowuje równoczesne zapisy i umożliwia scalanie. Nie zakładać, że zwykła aktualizacja pliku na Dysku zapobiega utracie zmian. |
| P1 | `SemesterEntity.isActive` jest w tabeli planu, a eksport zawiera to pole. Aktywny semestr steruje ekranami, widgetem i powiadomieniami. | Pominąć ten wybór w danych współdzielonych. Przy aktualizacji planu odtworzyć wybór urządzenia po stabilnej tożsamości semestru. |
| P1 | `res/xml/data_extraction_rules.xml` obejmuje bazę i DataStore, lecz nie przyszły stan synchronizacji. | Metadane konta, wersję bazową i kolejkę zmian trzymać poza kopią Androida; przetestować przywrócenie bazy bez tych metadanych. |
| P1 | `export/ExportImporter.kt` waliduje obecny format kopii i relacje, a `export/PlanBackupService.kt` służy importowi zastępującemu plan. | Wersjonować format synchronizacji osobno od kopii JSON albo jawnie opisać ich zgodność. Walidować dane z Dysku przed atomowym zastosowaniem. Nie używać bezwarunkowego importu jako pobierania zmian. |
| P1 | Ręczny import JSON może zastąpić cały plan także po włączeniu synchronizacji. | Zaplanować, czy import tworzy lokalną zmianę wymagającą scalenia, czy na czas importu wstrzymuje synchronizację. Nie wysyłać bez sprawdzenia planu, który zastąpił import. |
| P1 | `ui/setup/SetupWizard.kt` ma dziś trzy kroki lokalnej konfiguracji. `ui/settings/SettingsScreen.kt` ma osobny ekran danych, a główne ustawienia mają ograniczoną hierarchię. | Dodać krótki, pomijalny wybór w kreatorze i osobny ekran synchronizacji dostępny z sekcji „Dane”. Przed implementacją istotnej zmiany interfejsu przygotować wariant do akceptacji. |
| P1 | `app/build.gradle.kts` i `gradle/libs.versions.toml` nie zawierają obecnie integracji Google Identity, Drive ani okresowej pracy synchronizacji. Manifest ma uprawnienie `INTERNET`. | Przed dodaniem zależności sprawdzić protokół, konfigurację OAuth i pracę w tle na docelowym sposobie dystrybucji APK. |

Kod i wymagania: [encje](../app/src/main/java/dev/retza/mak/data/entity/Entities.kt), [snapshot i import](../app/src/main/java/dev/retza/mak/data/repository/PlanBackupGateway.kt), [format eksportu](../app/src/main/java/dev/retza/mak/export/ExportSnapshot.kt), [reguły kopii Androida](../app/src/main/res/xml/data_extraction_rules.xml), [reguły domenowe](DOMAIN.md), [ustawienia i kreator](FEATURES.md), [granice architektury](ARCHITECTURE.md).

## Kolejność prac po akceptacji zakresu

1. **Potwierdź wykonalność zapisu na Dysku.** Skonfiguruj próbne konto i klientów OAuth dla certyfikatów debug oraz release. Sprawdź `drive.appdata`, dwa równoczesne zapisy, przerwanie i ponowienie operacji, opóźnionego klienta, usunięcie kopii oraz ponowne pojawienie się danych z innego telefonu. Porównaj wersjonowane, niezmienne zapisy z innym protokołem, który zachowuje obie gałęzie. Wybierz transport dopiero po teście. Brak bezpiecznego protokołu blokuje wdrożenie scalania.
2. **Przygotuj model danych.** Nadaj stabilne identyfikatory wszystkim synchronizowanym wpisom i przetestuj migrację istniejącej bazy. Oddziel lokalny wybór aktywnego semestru od treści współdzielonej. Dodaj spójny odczyt planu, stan wersji bazowej, zapis zmian i usunięć oraz granicę atomowego zastosowania scalenia. Zachowaj dotychczasowy import JSON i określ jego zachowanie przy włączonej synchronizacji.
3. **Zbuduj i przetestuj scalanie bez sieci.** Obsłuż nowe wpisy, niezależne edycje, te same pola, usunięcie z równoległą edycją, zmiany zależnych pól i relacji, pierwszy kontakt dwóch niezależnych planów oraz walidację wyniku. Konflikt nie może skasować żadnej wersji. Testy JVM mają obejmować także edycję lokalną podczas pobierania i ponowienie tej samej operacji.
4. **Dodaj połączenie z kontem i wymianę danych.** Wysyłaj i pobieraj tylko po wyraźnym włączeniu funkcji. Powiąż pracę w tle z wybranym kontem. Przerwij ją bez utraty lokalnych danych, gdy brakuje dostępu lub konto się zmieni. Przetestuj rzeczywiste logowanie, brak sieci, wygasłe uprawnienia i równoległą pracę dwóch telefonów.
5. **Dodaj interfejs i odbiór na urządzeniu.** Przygotuj do akceptacji wariant kreatora, ustawień, pierwszego dopasowania i ekranu konfliktu zgodny z `ARCHITECTURE.md`, sekcja 4. Pokaż stany oczekiwania, błędu i wyniku. Po zatwierdzonym scaleniu odśwież widget i przelicz powiadomienia. Sprawdź 320 dp, duży tekst, motyw ciemny, dostępność, przywrócenie kopii Androida, wyłączenie i usunięcie kopii z Dysku.

Każdy etap po wdrożeniu powinien mieć migrację lub test rollbacku stosowny do zmiany danych. Samo przejście testów JVM nie potwierdza działania OAuth, Dysku ani odbioru na dwóch urządzeniach.

## Otwarte warunki i pytania

- `PRODUCT.md` i `STACK.md` wykluczają dziś konta i synchronizację z zakresu. `ARCHITECTURE.md`, sekcja 10, opisuje ją jako niezaakceptowaną propozycję. Dopiero decyzja użytkownika może zmienić te ustalenia i uruchomić prace wykonawcze.
- Dokumentacja [Drive `files.update`](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/update) nie potwierdza w tym audycie bezpiecznego warunkowego nadpisania pojedynczego pliku. Mechanizm współbieżności wymaga próby z rzeczywistym kontem. Nie uznawać samego identyfikatora pliku lub czasu modyfikacji za gwarancję.
- [Folder danych aplikacji](https://developers.google.com/workspace/drive/api/guides/appdata) jest ukryty przed interfejsem Dysku i innymi aplikacjami, ale użytkownik może go usunąć. Trzeba sprawdzić skutki usunięcia, limity, porządkowanie starych wersji i reakcję starszej wersji MAK na nowy format.
- Nie potwierdzono w repo konfiguracji produkcyjnego projektu Google Cloud, ekranu zgody, klientów OAuth i publicznej polityki prywatności. Wymagania zewnętrzne należy zweryfikować przed etapem integracji.
- Reguły kopii Androida obejmują obecny DataStore. Jeśli wybór konta lub włączenie synchronizacji trafią do tego pliku, przywrócenie może błędnie uruchomić synchronizację. Projekt musi umieścić te dane poza kopią albo po przywróceniu wymagać ponownego połączenia.
- Nie ustalono okresu przechowywania wersji do odzyskania ani czasu, po którym można bezpiecznie usuwać zapis o skasowanym wpisie. Bez tej decyzji nie usuwać automatycznie historii potrzebnej do scalenia urządzenia, które długo było offline.

Po akceptacji zakresu przenieść wykonalne kroki do `PLAN.md` na gałęzi implementacyjnej, dodać zadanie i odbiór do `QUEUE.md`, a przyjęte reguły zapisać w właściwych dokumentach oraz `LOG.md`.
