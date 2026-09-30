# Synchronizacja planu przez Google Drive

Status: projekt uproszczony, decyzja użytkownika z 2026-09-30. Zastępuje wcześniejszy projekt ze scalaniem pojedynczych wpisów, historią wersji, stabilnymi identyfikatorami i ochroną każdego formularza. Użytkownik uznał go za nadmiarowy dla tej aplikacji. Poprzedni kod jest zachowany lokalnie w gałęzi `backup/google-sync-merge-design`, a opis decyzji w `LOG.md`.

## Cel i zakres

Jeden plan na kilku telefonach tej samej osoby przez jej konto Google. Plan pozostaje dostępny i edytowalny bez internetu. Synchronizacja jest domyślnie wyłączona. Użytkownik może ją wybrać w kreatorze albo później w ustawieniach; brak sieci lub błąd logowania nie przerywa lokalnej konfiguracji.

Synchronizacja obejmuje cały plan: globalne kierunki, semestry, przypisania kierunków, kalendarze, zajęcia, korekty tygodni, zmiany wystąpień i oba rodzaje notatek. Aktywny semestr, motyw, powiadomienia i pozostałe ustawienia telefonu pozostają lokalne.

Plan jest przechowywany w ukrytym folderze danych aplikacji na Dysku Google (zakres `drive.appdata`), bez dodatkowego hasła. Nie przedstawiać tej kopii jako szyfrowanej od końca do końca. Systemowa kopia Androida i ręczny eksport JSON pozostają osobnymi funkcjami.

## Plik na Dysku

- Jeden plik `mak-plan.json` w `appDataFolder` w formacie ręcznego eksportu JSON (`ExportSnapshot`, bieżąca wersja schematu). Wszystkie semestry mają w nim `isActive = false`, bo wybór aktywnego semestru jest lokalny.
- Pobrany plik przechodzi walidację `ExportImporter`, tę samą co ręczny import. Plik uszkodzony, za duży (ponad 8 MiB) albo w nieznanym formacie zatrzymuje synchronizację z komunikatem; plan lokalny zostaje bez zmian.

## Stan na telefonie

Poza systemową kopią Androida (`noBackupFilesDir`): konto (`sub` i adres e-mail), identyfikator pliku na Dysku, suma MD5 pliku z ostatniej udanej synchronizacji, skrót planu lokalnego z tej chwili, czas ostatniej synchronizacji i ostatni błąd wymagający działania. Po przywróceniu kopii Androida lub ponownej instalacji tego stanu nie ma, więc aplikacja wymaga ponownego połączenia konta.

## Przebieg synchronizacji

Zmiana lokalna oznacza, że skrót SHA-256 planu (kanoniczny eksport bez aktywnego semestru) różni się od zapamiętanego. Zmiana zdalna oznacza, że MD5 pliku na Dysku różni się od zapamiętanej.

| Sytuacja | Działanie |
|---|---|
| Brak pliku na Dysku | Wyślij plan z telefonu. |
| Tylko zmiana lokalna | Wyślij plan z telefonu. |
| Tylko zmiana zdalna | Zastosuj plan z Dysku. |
| Zmiany po obu stronach | Zapytaj: „Zachować plan z tego telefonu czy z Dysku?”. |
| Brak zmian | Nic nie rób. |

Pierwsze połączenie (brak zapamiętanego stanu): pusty telefon pobiera plan; pusty plik lub jego brak powoduje wysłanie; identyczne plany tylko zapisują stan; w pozostałych przypadkach pada to samo pytanie o wybór wersji.

Zasady:

1. Przed zastąpieniem planu lokalnego albo pliku na Dysku odrzucona wersja trafia do lokalnego archiwum. Archiwum przechowuje 10 ostatnich wersji z datą i źródłem; ekran synchronizacji pozwala wyeksportować każdą z nich przez systemowy wybór pliku. Dzięki temu żaden przebieg nie usuwa planu bez możliwości odzyskania.
2. Zastosowanie planu z Dysku to jedna transakcja `replaceAll`. Aktywny semestr zostaje, jeśli semestr o tym numerze istnieje w nowym planie; inaczej aktywny staje się pierwszy semestr, jak po usunięciu aktywnego.
3. Dopóki otwarty jest formularz edycji planu, pobrany plan nie jest stosowany. Aplikacja zastosuje go po zamknięciu formularza. Formularze nie mają osobnych ostrzeżeń o zmianie danych.
4. Tuż przed wysłaniem aplikacja ponownie odczytuje metadane pliku. Jeśli MD5 zmieniło się od początku przebiegu, przebieg zaczyna się od nowa.
5. Drive nie ma warunkowego zapisu pliku. Gdy dwa telefony wyślą plan niemal jednocześnie, zostaje wersja zapisana później. Drugi telefon przy kolejnym przebiegu pobierze ją, a swoją wersję zachowa w archiwum. To świadome ograniczenie.
6. Ręczny import JSON jest zwykłą zmianą lokalną.

## Konto i praca w tle

- Google Identity `AuthorizationClient`, bez własnego backendu i bez sekretu OAuth w APK. Zakresy: `drive.appdata`, `openid`, `email`. Trwałą tożsamością konta jest `sub` z endpointu userinfo; adres e-mail jest etykietą. Token pozostaje w pamięci, nigdy w plikach, logach, stanie UI ani kopii Androida. Po odpowiedzi 401 token jest usuwany z pamięci podręcznej Usług Google Play i ponawiany najwyżej raz.
- WorkManager: jednorazowa praca po zmianie planu i po otwarciu aplikacji, okresowa co 60 minut, wymagana sieć i wykładniczy backoff. Brak zgody, pytanie o wersję i niepoprawny plik wymagają działania w aplikacji; przejściowe błędy sieci są ponawiane.
- Wyłączenie zatrzymuje pracę w tle i zostawia plan na telefonie. Osobny wybór pozwala zachować plik na Dysku albo go usunąć, z ostrzeżeniem, że inny połączony telefon może wysłać plan ponownie.

## Interfejs

Istniejące komponenty Compose, bez nowego języka wizualnego.

- W sekcji „Dane” ustawień wiersz „Synchronizacja Google”. Ekran pokazuje konto, stan, czas ostatniej synchronizacji i błąd wymagający działania oraz akcje „Połącz konto Google”, „Synchronizuj teraz” i „Wyłącz synchronizację”.
- Pytanie o wybór wersji pokazuje dla obu stron liczbę semestrów i zajęć oraz informację, że odrzucona wersja trafi do archiwum.
- Lista archiwum pokazuje datę i źródło wersji oraz akcję eksportu.
- Stan pusty bez planu ma pod „Skonfiguruj plan” akcję „Pobierz plan z konta Google”, która otwiera ekran synchronizacji. Pobrany plan z semestrem kończy stan pusty bez przechodzenia przez kreator. Kreator się nie zmienia.

## Testy

Testy JVM koordynatora z atrapą transportu i planu: każdy wiersz tabeli, pierwsze połączenie, zmiana pliku między odczytem a wysłaniem, archiwum przed każdym zastąpieniem, zachowanie aktywnego semestru, niepoprawny plik bez zmian lokalnych i wstrzymanie przy otwartym formularzu. Transportu Drive i OAuth nie testuje się atrapą serwera; potwierdza je odbiór na prawdziwym koncie (I-69, `STACK.md`, sekcja „Konfiguracja synchronizacji Google”).
