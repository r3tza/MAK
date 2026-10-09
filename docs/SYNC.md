# Synchronizacja planu przez Google Drive

Opis działającej synchronizacji (I-68, odbiór na prawdziwym koncie w I-69, wybór zmian w I-76 i I-77). Uproszczony projekt jednym plikiem przyjął użytkownik 2026-09-30 zamiast wcześniejszego ze scalaniem pojedynczych wpisów, historią wersji, stabilnymi identyfikatorami i ochroną każdego formularza; tamten kod jest zachowany lokalnie w gałęzi `backup/google-sync-merge-design`, a uzasadnienie w `LOG.md`. Zmiana zachowania synchronizacji wymaga aktualizacji tego pliku.

## Cel i zakres

Jeden plan na kilku telefonach tej samej osoby przez jej konto Google. Plan pozostaje dostępny i edytowalny bez internetu. Synchronizacja jest domyślnie wyłączona. Użytkownik włącza ją w ustawieniach, a bez planu także akcją „Pobierz plan z konta Google” w stanie pustym ekranu „Dzisiaj”. Kreator jej nie oferuje; brak sieci lub błąd logowania nie przerywa lokalnej konfiguracji.

Synchronizacja obejmuje cały plan: globalne kierunki, semestry, przypisania kierunków, kalendarze, zajęcia, korekty tygodni, zmiany wystąpień i oba rodzaje notatek. Aktywny semestr, motyw, powiadomienia i pozostałe ustawienia telefonu pozostają lokalne.

Plan jest przechowywany w ukrytym folderze danych aplikacji na Dysku Google (zakres `drive.appdata`), bez dodatkowego hasła. Nie przedstawiać tej kopii jako szyfrowanej od końca do końca. Systemowa kopia Androida i ręczny eksport JSON pozostają osobnymi funkcjami.

## Plik na Dysku

- Jeden plik `mak-plan.json` w `appDataFolder` w formacie ręcznego eksportu JSON (`ExportSnapshot`, bieżąca wersja schematu). Wszystkie semestry mają w nim `isActive = false`, bo wybór aktywnego semestru jest lokalny.
- Pobrany plik przechodzi walidację `ExportImporter`, tę samą co ręczny import. Plik uszkodzony, za duży (ponad 8 MiB) albo w nieznanym formacie zatrzymuje synchronizację z komunikatem; plan lokalny zostaje bez zmian. Numer wersji jest sprawdzany przed odczytem reszty pliku, bo nowsza wersja może mieć pola, których starsza aplikacja nie zna. Plik z wyższą wersją schematu niż obsługiwana dostaje osobny komunikat „Plan na Dysku pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.” z akcją „Zaktualizuj”, która otwiera ekran „Aktualizacja” (I-83, decyzja użytkownika z 2026-10-09). To samo ostrzeżenie z tą samą akcją stoi na „Dzisiaj” i na ekranie synchronizacji.

## Stan na telefonie

Poza systemową kopią Androida (`noBackupFilesDir`): konto (`sub` i adres e-mail), suma MD5 pliku z ostatniej udanej synchronizacji, skrót planu lokalnego z tej chwili, informacja, czy ostatnim przesłaniem było wysłanie, czas ostatniej synchronizacji, ostatni błąd wymagający działania i oczekujące pytanie o wersję. Po przywróceniu kopii Androida lub ponownej instalacji tego stanu nie ma, więc aplikacja wymaga ponownego połączenia konta.

## Przebieg synchronizacji

Zmiana lokalna oznacza, że skrót SHA-256 planu (kanoniczny eksport bez aktywnego semestru) różni się od zapamiętanego. Zmiana zdalna oznacza, że MD5 pliku na Dysku różni się od zapamiętanej.

| Sytuacja | Działanie |
|---|---|
| Brak pliku na Dysku | Wyślij plan z telefonu. |
| Tylko zmiana lokalna | Wyślij plan z telefonu. |
| Tylko zmiana zdalna | Zastosuj plan z Dysku. |
| Zmiany po obu stronach | Zapytaj: „Zachować plan z tego telefonu czy z Dysku?”. Przy znanym wspólnym planie można też wybrać stronę przy każdej różnicy („Wybierz zmiany”). |
| Brak zmian | Nic nie rób. |

Pierwsze połączenie (brak zapamiętanego stanu): pusty telefon pobiera plan; pusty plik lub jego brak powoduje wysłanie; identyczne plany tylko zapisują stan; w pozostałych przypadkach pada to samo pytanie o wybór wersji.

Zasady:

1. Wersja, która mogła przepaść, trafia przed zastąpieniem do lokalnego archiwum: plan odrzucony przy wyborze wersji (z telefonu albo z Dysku) oraz plan telefonu pobierany po jego własnym wysłaniu, bo inny telefon mógł ten plik nadpisać. Plan, który telefon tylko przyjął od drugiego telefonu, nie trafia do archiwum. Archiwum przechowuje 10 ostatnich wersji z datą i źródłem; ekran synchronizacji pozwala wyeksportować każdą z nich przez systemowy wybór pliku.
2. Zastosowanie planu z Dysku to jedna transakcja `replaceAll`, która najpierw sprawdza, że plan lokalny się nie zmienił. Po pierwszej synchronizacji numery wpisów są wspólne dla telefonów, więc aktywny semestr zostaje, jeśli semestr o tym numerze istnieje. Przy pierwszym pobraniu numery pochodzą z innego telefonu: aktywny staje się semestr, którego kalendarz obejmuje dzisiejszy dzień, a bez takiego semestr o najpóźniejszym początku.
3. Pobrany plan nie jest stosowany, dopóki trwa edycja: otwarty formularz zajęć, kierunku, dodania kierunku albo kreator, dialog usuwania semestru, a na ekranach podglądu (szczegóły terminu, semestr, kierunki, korekty, kalendarze) tylko niezapisany szkic albo otwarte potwierdzenie. Sam pozostawiony ekran podglądu niczego nie wstrzymuje. Edycja kończy się 5 sekund po zamknięciu ekranu, więc odtworzenie aktywności (obrót, motyw, czcionka) jej nie przerywa. Aplikacja zastosuje plan po zakończeniu edycji. Formularze nie mają osobnych ostrzeżeń o zmianie danych.
4. Tuż przed wysłaniem aplikacja ponownie odczytuje metadane pliku. Jeśli MD5 zmieniło się od początku przebiegu, przebieg zaczyna się od nowa.
5. Drive nie ma warunkowego zapisu pliku. Gdy dwa telefony wyślą plan niemal jednocześnie, zostaje wersja zapisana później. Drugi telefon przy kolejnym przebiegu pobierze ją, a swoją wersję zachowa w archiwum. To świadome ograniczenie.
6. Ręczny import JSON jest zwykłą zmianą lokalną. Przy połączonym koncie ostrzeżenie importu mówi, że plan trafi też na Dysk i na pozostałe telefony.
7. „Wyłącz i usuń kopię z Dysku” usuwa każdą kopię pliku planu, także duplikat po dwóch równoczesnych pierwszych wysłaniach.

## Konto i praca w tle

- Google Identity `AuthorizationClient`, bez własnego backendu i bez sekretu OAuth w APK. Zakresy: `drive.appdata`, `openid`, `email`. Trwałą tożsamością konta jest `sub` z endpointu userinfo; adres e-mail jest etykietą. Token pozostaje w pamięci, nigdy w plikach, logach, stanie UI ani kopii Androida. Po odpowiedzi 401 token jest usuwany z pamięci podręcznej Usług Google Play i ponawiany najwyżej raz.
- WorkManager: jednorazowa praca po zmianie planu, po otwarciu aplikacji i po zamknięciu formularza, okresowa co 60 minut, wymagana sieć i wykładniczy backoff. Przejściowe błędy sieci (brak połączenia, HTTP 429 i 5xx, HTTP 403 z przyczyną `rateLimitExceeded` albo `userRateLimitExceeded`, odpowiedź Dysku, której nie da się odczytać) są ponawiane bez komunikatu. Brak zgody lub uprawnień prowadzi do „Połącz ponownie”. Inne konto Google niż połączone, brak miejsca na Dysku (HTTP 403 `storageQuotaExceeded`, komunikat o zwolnieniu miejsca), inna odmowa dostępu do Dysku (HTTP 403), niepoprawny plik, inny trwały błąd i pytanie o wersję są zapisywane jako stan wymagający działania. Przyczynę 403 aplikacja odczytuje z pola `errors[].reason` odpowiedzi Dysku (decyzja użytkownika z 2026-10-08). Kodowanie planu i zapis plików synchronizacji odbywają się poza wątkiem głównym. Token jest używany ponownie do czasu odrzucenia przez Dysk.
- Wyłączenie zatrzymuje pracę w tle i zostawia plan na telefonie. Osobny wybór pozwala zachować plik na Dysku albo go usunąć, z ostrzeżeniem, że inny połączony telefon może wysłać plan ponownie.

## Interfejs

Istniejące komponenty Compose, bez nowego języka wizualnego.

- Gdy synchronizacja wymaga działania, ekran „Dzisiaj” pokazuje pod nagłówkiem ostrzeżenie prowadzące do ekranu synchronizacji: przy konflikcie wersji „Plan różni się na telefonie i na Dysku” z akcją „Wybierz wersję”, przy innym problemie jego opis z akcją „Otwórz”.
- W sekcji „Dane” ustawień wiersz „Synchronizacja Google”. Ekran pokazuje konto, stan, czas ostatniej synchronizacji i błąd wymagający działania oraz akcje „Połącz konto Google”, „Synchronizuj teraz” i „Wyłącz synchronizację”.
- Pytanie o wybór wersji pokazuje dla obu stron datę ostatniej zmiany (Dysk: `modifiedTime` pliku; telefon: czas ostatniego zapisu w tabelach planu, pomijany przez 2 s po podmianie planu przez synchronizację) oraz informację, że odrzucona wersja trafi do archiwum. Gdy istnieje wspólny plan z ostatniej synchronizacji (`base.json`), pokazuje trzy pierwsze różnice i akcję „Wybierz zmiany”; w przeciwnym razie liczbę kierunków, semestrów i zajęć.
- „Wybierz zmiany” (I-77): przy każdej różnicy wybór telefonu albo Dysku, bez wyboru domyślnego. Zajęcia zmienione po obu stronach różnią się polami: nazwa, kierunek, typ, termin (dzień, godziny, data i tygodnie razem), prowadzący, sala, budynek, grupa i notatka do zajęć są osobnymi różnicami (decyzja użytkownika z 2026-10-08). Pozostałe wpisy, w tym zmiany terminu, są wybierane w całości. Wpis obecny tylko po jednej stronie ma po drugiej „Brak”; wybór tej strony go pomija. Dwa wpisy dodane niezależnie z tym samym numerem są osobnymi różnicami, a zachowany wpis z Dysku dostaje nowy numer razem z odwołaniami. Wybory, które zostawiają wpis bez rodzica albo łamią reguły importu, niczego nie zmieniają i są opisane na ekranie. Zapis archiwizuje obie wersje, podmienia plan na telefonie i wysyła go; zmiana na Dysku w międzyczasie powoduje nowe pytanie. Kopia planu z Dysku na czas pytania leży w `pending.json`.
- Lista archiwum pokazuje datę i źródło wersji oraz akcję eksportu.
- Stan pusty bez planu ma pod „Skonfiguruj plan” akcję „Pobierz plan z konta Google”, która otwiera ekran synchronizacji. Pobrany plan z semestrem kończy stan pusty bez przechodzenia przez kreator. Kreator się nie zmienia.

## Testy

Testy JVM koordynatora z atrapą transportu i planu: każdy wiersz tabeli, pierwsze połączenie, zmiana pliku między odczytem a wysłaniem, reguła archiwum i jego limit, aktywny semestr przy pierwszym i kolejnym pobraniu, niepoprawny plik bez zmian lokalnych, zapis trwałego błędu, wstrzymanie przy otwartym formularzu. Testy JVM transportu: jeden token na wiele żądań, jedno odświeżenie po 401, nieczytelna odpowiedź jako błąd przejściowy, kształt żądań utworzenia i aktualizacji pliku, usunięcie duplikatów. Testy JVM rejestru edycji i mapowania wyników pracy w tle. Na emulatorze także: logika zgody w `SyncViewModel` i ciągłość edycji przy odtworzeniu ekranu. Na emulatorze: zastąpienie planu w prawdziwej transakcji Room, graf zależności synchronizacji oraz dialogi ekranu przy 320 dp i czcionce 2,0. Transportu Drive i OAuth nie testuje się atrapą serwera; potwierdza je odbiór na prawdziwym koncie (I-69, `STACK.md`, sekcja „Konfiguracja synchronizacji Google”).
