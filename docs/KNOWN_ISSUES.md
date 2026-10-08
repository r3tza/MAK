# Znane problemy

Stan na 2026-10-08. Ten rejestr obejmuje otwarte problemy potwierdzone przeglądem kodu oraz odbiór, którego jeszcze nie wykonano na urządzeniu. Docelowe zachowanie opisują `FEATURES.md`, `DOMAIN.md` i `ARCHITECTURE.md`, a zadania i statusy `QUEUE.md`. Naprawione problemy nie są tu powtarzane; ich historia jest w sekcji „Zakończone” w `QUEUE.md` i w `LOG.md`.

## Otwarte problemy

Poprawki z audytu synchronizacji I-70 zostały zakończone i zaakceptowane po recenzji 2026-10-02. Rzeczywiste OAuth i Drive przeszły odbiór I-69 na wersji debug 2026-10-08. Klient OAuth release jest skonfigurowany, ale nie był sprawdzony na podpisanym APK; użytkownik zamknął I-69 bez tego testu, więc pierwsze wydanie z synchronizacją wymaga sprawdzenia logowania. Pozostałe dwie kwestie z audytu interfejsu (obramowane przyciski ikon w górnym pasku i trzy style zaznaczenia na „Planie”) użytkownik zamknął bez zmian (`LOG.md`, 2026-09-29).

Synchronizacja Google (I-68) nie należy jeszcze do opublikowanego wydania. Projekt Google Cloud działa w trybie Testing, więc logować mogą się tylko konta z listy testowej; publikacja aplikacji w Google Auth Platform jest osobną decyzją przed wydaniem. Świadome ograniczenie projektu: Drive nie ma warunkowego zapisu, więc przy niemal równoczesnym wysłaniu z dwóch telefonów zostaje późniejsza wersja, a druga trafia do archiwum telefonu przy kolejnej synchronizacji. Szkic zajęć odtworzony po zakończeniu procesu aplikacji nie wstrzymuje pobrania planu; jeśli w tym czasie inny telefon zmienił plan, zapis szkicu może trafić do wpisu o tym samym numerze.

### Sprzeczności dokumentacji i pytania po audycie

Brak otwartych pytań (stan na 2026-10-08).

## Wymagają odbioru na urządzeniu

Poniższe pozycje mają gotowy kod; brakuje potwierdzenia na urządzeniu. Szczegóły scenariuszy są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”. Pozostałe odbiory użytkownik zamknął 2026-10-08, bo aplikacja działa u kilku osób.

1. **Systemowa kopia zapasowa (I-14).** Kod istnieje: lista dozwolonych plików w `data_extraction_rules.xml` (baza i ustawienia), kopia w chmurze tylko z szyfrowaniem, tekst w „Dane i prywatność”. Na emulatorze z lokalnym transportem kopii przywracanie po reinstalacji z APK działa, a bez szyfrowania nic nie trafia do kopii. Do potwierdzenia (O-09): kopia Google na telefonie z kontem i blokadą ekranu, reinstalacja z APK oraz przeniesienie na nowy telefon.
2. **Tryb tabletowy (I-45 do I-47).** Kod istnieje: klasy szerokości okna z progami 600 i 840 dp, obrót na urządzeniach o najkrótszym boku od 600 dp, boczny pasek nawigacji od 600 dp, treść najwyżej 640 dp, „Dzisiaj” w dwóch kolumnach od 840 dp i dialogi najwyżej 560 dp z przewijaniem. Na emulatorze z symulowanym dużym ekranem układ jest zgodny z wariantem A, a układ telefonu przy 411 i 320 dp jest identyczny jak przed zmianą. Do potwierdzenia (O-08): prawdziwy tablet albo telefon składany w pionie, w poziomie i w podzielonym ekranie, zachowanie ekranu i wpisanych danych po obrocie oraz układ poziomy wyboru godziny z I-44.
3. **Powiadomienia (O-04).** Do potwierdzenia: zgoda, alarmy, zachowanie po restarcie telefonu i otwarcie zajęć z powiadomienia.

## Zamknięte w kodzie, bez odbioru urządzeniowego

Pierwotny audyt z 2026-09-19 wskazywał ręczne wpisywanie dat i godzin, wpisywanie koloru jako kodu, zbyt małe obszary dotyku, brak semantyki pól i focusu, poziome filtry, układ akcji na 320 dp, kontrast godziny zakończenia, kolorowe markery bez opisu oraz niejednoznaczne akcje usuwania. Poprawki wdrożono w objętych zmianami kontrolkach i ekranach. Nie oznacza to jeszcze potwierdzenia całej aplikacji z TalkBackiem, klawiaturą i na małym ekranie urządzenia. Pozostałe akcje destrukcyjne wymagają przeglądu podczas odbioru.
