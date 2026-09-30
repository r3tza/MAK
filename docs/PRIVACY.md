# Dane i prywatność MAK

Projekt opisu dla rozszerzenia synchronizacji, stan na 2026-09-30. Funkcja jest w implementacji i nie należy jeszcze do opublikowanego wydania. Przed uruchomieniem projektu Google opublikuj adres tego opisu razem z działającą wersją funkcji.

MAK zapisuje plan zajęć i ustawienia na urządzeniu. Możesz korzystać z planu i go edytować bez konta oraz bez internetu. Aplikacja nie ma własnego serwera ani kont MAK.

## Opcjonalna synchronizacja Google

Synchronizacja jest domyślnie wyłączona. Po wybraniu „Połącz konto Google” aplikacja prosi o dostęp do własnego, ukrytego folderu danych na Twoim Dysku Google oraz informacje potrzebne do rozpoznania wybranego konta. Nie wymaga dostępu do pozostałych plików na Dysku.

Na konto trafiają kierunki, semestry, kalendarze akademickie, zajęcia, korekty tygodni, zmiany terminów i notatki. Dane mogą obejmować wpisane przez Ciebie nazwy prowadzących, sale i budynki. Motyw, aktywny semestr i ustawienia powiadomień pozostają na telefonie.

W folderze aplikacji jest jeden plik z aktualnym planem. Folder jest przechowywany przez Google według zasad tej usługi. MAK nie zapewnia dodatkowego szyfrowania od końca do końca dla synchronizacji.

Aplikacja przechowuje na telefonie informację o wybranym koncie i stan synchronizacji. Krótkotrwały token dostępu jest używany w pamięci, bez zapisywania go w plikach lub kopii Androida. Gdy synchronizacja zastępuje plan na telefonie albo na Dysku, poprzednia wersja trafia do archiwum na telefonie (10 ostatnich wersji), skąd możesz ją wyeksportować. Stan synchronizacji i archiwum pozostają poza systemową kopią zapasową.

Wyłączenie synchronizacji pozostawia plan na telefonie. Możesz zachować kopię na koncie albo wybrać trwałe usunięcie pliku MAK z Dysku. Inny połączony telefon może później ponownie wysłać swoje dane. Samo wyłączenie nie usuwa archiwum poprzednich wersji; usuwa je wyczyszczenie danych aplikacji lub odinstalowanie. Wersje wyeksportowane do własnego pliku usuń osobno.

## Pozostałe połączenia i kopie

Sprawdzanie aktualizacji łączy się z GitHubem i nie wysyła planu. Automatyczne sprawdzanie jest domyślnie wyłączone.

Android może wykonywać systemową kopię bazy planu oraz ustawień i odtworzyć ją po reinstalacji lub na nowym telefonie. Kopia w chmurze wymaga szyfrowania obsługiwanego przez urządzenie. Jej ustawieniami zarządzasz w Androidzie. Ręczny eksport JSON zapisuje plan w wybranym przez Ciebie miejscu.

Po przywróceniu kopii Androida lub ponownej instalacji MAK wymaga ponownego połączenia konta przed synchronizacją. Brak zdalnej kopii sam nie usuwa lokalnego planu.

Pytania o dane możesz zgłosić przez [zgłoszenia projektu MAK](https://github.com/r3tza/MAK/issues). Nie umieszczaj w publicznym zgłoszeniu swojego planu ani danych konta.
