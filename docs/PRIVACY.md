# Polityka prywatności MAK

Wersja wstępna z 2026-10-08. Opisuje aplikację razem z opcjonalną synchronizacją Google, która nie należy jeszcze do opublikowanego wydania. Stały adres tej polityki: https://r3tza.github.io/MAK/PRIVACY.html.

## Najważniejsze

- MAK działa na telefonie. Plan i ustawienia możesz tworzyć i edytować bez konta i bez internetu.
- MAK nie ma własnego serwera ani kont MAK. Autor aplikacji nie otrzymuje Twojego planu ani danych konta.
- Aplikacja nie zawiera reklam, analityki ani narzędzi śledzących.
- Połączenia z internetem nawiązuje tylko w dwóch przypadkach opisanych niżej: synchronizacja z Google, gdy ją włączysz, i sprawdzanie aktualizacji na GitHubie.

## Dane na telefonie

MAK zapisuje na urządzeniu plan zajęć: kierunki, semestry, kalendarze akademickie, zajęcia, korekty tygodni, zmiany terminów i notatki. Mogą się w nich znaleźć wpisane przez Ciebie nazwy prowadzących, sale i budynki. Na telefonie są też ustawienia: motyw, aktywny semestr, powiadomienia o kolizjach i sprawdzanie aktualizacji.

Powiadomienia o kolizjach i widget powstają na telefonie z tych samych danych i nigdzie ich nie wysyłają.

Ręczny eksport JSON zapisuje plan w miejscu, które sam wybierzesz. Za taki plik odpowiadasz Ty.

## Opcjonalna synchronizacja Google

Synchronizacja jest domyślnie wyłączona. Włączasz ją przyciskiem „Połącz konto Google” w ustawieniach.

**Zakres dostępu.** Aplikacja prosi Google o trzy uprawnienia:

- dostęp do własnego, ukrytego folderu danych aplikacji na Twoim Dysku Google (`drive.appdata`); MAK nie widzi pozostałych plików na Dysku i nie ma do nich dostępu,
- identyfikator konta Google (`openid`), żeby rozpoznać, że łączysz to samo konto,
- adres e-mail (`email`), żeby pokazać, które konto jest połączone.

**Co trafia na Dysk.** W folderze aplikacji jest jeden plik z aktualnym planem: kierunki, semestry, kalendarze, zajęcia, korekty tygodni, zmiany terminów i notatki. Ustawienia aplikacji i aktywny semestr zostają na telefonie. Plik przechowuje Google na zasadach swojej usługi. MAK nie dodaje do synchronizacji własnego szyfrowania.

**Co zostaje na telefonie.** Aplikacja zapisuje:

- identyfikator i adres e-mail połączonego konta oraz stan synchronizacji, w tym czas ostatniej synchronizacji i czas ostatniej zmiany planu na telefonie,
- kopię planu z ostatniej udanej synchronizacji; służy do pokazania, co się różni, gdy plan zmieni się na dwóch telefonach,
- na czas otwartego pytania o wybór wersji kopię planu z Dysku, żeby pokazać różnice bez ponownego pobierania,
- archiwum do 10 ostatnich wersji planu, które synchronizacja zastąpiła i które mogłyby zostać utracone: wersji odrzuconej przy wyborze, obu poprzednich wersji przy wyborze pojedynczych zmian oraz planu wysłanego wcześniej z tego telefonu, gdy przychodzi nowszy z Dysku. Plan, który telefon tylko przyjął od innego telefonu, do archiwum nie trafia. Wersje z archiwum możesz wyeksportować.

Token dostępu Google jest krótkotrwały i aplikacja trzyma go tylko w pamięci. Stan synchronizacji, kopie planu i archiwum są wyłączone z systemowej kopii zapasowej Androida.

**Wyłączenie i usunięcie.** Wyłączenie synchronizacji zostawia plan na telefonie i usuwa z telefonu stan synchronizacji oraz kopie planu. Możesz przy tym zachować plik na Dysku albo go trwale usunąć. Inny telefon połączony z tym samym kontem może później wysłać plan ponownie. Archiwum poprzednich wersji znika po wyczyszczeniu danych aplikacji albo jej odinstalowaniu. Dostęp MAK do konta możesz też cofnąć w ustawieniach konta Google, na stronie [Połączenia z aplikacjami innych firm](https://myaccount.google.com/connections).

## Sprawdzanie aktualizacji

MAK może sprawdzać, czy na GitHubie jest nowa wersja aplikacji, i pobrać ją po Twojej decyzji. Ręczne sprawdzenie uruchamiasz w ustawieniach. Automatyczne sprawdzanie jest domyślnie wyłączone; po włączeniu działa przy uruchomieniu aplikacji, raz dziennie.

Zapytanie nie zawiera planu ani identyfikatora użytkownika. Jak przy każdym połączeniu z internetem, GitHub widzi adres IP urządzenia i podstawowe informacje o zapytaniu. Ich przetwarzanie opisuje polityka prywatności GitHuba.

## Systemowa kopia zapasowa Androida

Android może zapisać kopię bazy planu i ustawień aplikacji i odtworzyć ją po ponownej instalacji albo na nowym telefonie. Kopią w chmurze zarządzasz w ustawieniach Androida; jej przechowywanie zależy od dostawcy kopii. Po odtworzeniu kopii albo ponownej instalacji MAK wymaga ponownego połączenia konta Google przed synchronizacją.

## Zmiany i kontakt

O zmianach tej polityki informują notatki wydania aplikacji. Pytania o dane możesz zgłosić przez [zgłoszenia projektu MAK](https://github.com/r3tza/MAK/issues). Nie umieszczaj w publicznym zgłoszeniu swojego planu ani danych konta.
