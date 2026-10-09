# MAK: historia wydań

Techniczna historia wydań dla agentów. Notatki dla użytkowników są w `app/src/main/assets/release_notes.json`.

## 0.3.2, 2026-10-09

- Zajęcia tego samego dnia z przerwą nie dłuższą niż minimalna przerwa (0 do 20 min, domyślnie 0) są kolizją `Collision.NoBreak` z opisem „Bez przerwy o 09:45” albo „Przerwa 5 min o 09:45” (I-80). Próg okienka ma opcje od 20 min, więc przerwa nie jest jednocześnie kolizją i okienkiem; zapisane 15 min jest odczytywane jako 20.
- Dane planu i `PlanDisplaySettings` płyną razem z `ActivePlanSource` do widoków, widgetu i powiadomień; widget odświeża się też po zmianie ustawienia.
- Plik eksportu albo plan na Dysku z wyższą `schemaVersion` daje komunikat o nowszej wersji MAK; synchronizacja zatrzymuje się z problemem `NEWER_REMOTE_PLAN` i akcją „Zaktualizuj” (I-83).

## 0.3.1, 2026-10-08

- Naprawiono „Dodaj” w pasku nawigacji i w kreatorze: `openClassEditor` przechodził do wzorca `MakRoutes.Edit`, więc Navigation przekazywała `{classId}` i `{date}` jako wartości, a edytor wracał po nieudanym otwarciu zajęć. Trasę buduje teraz `newClassRoute` (I-78).

## 0.3.0, 2026-10-08

- Dodano opcjonalną synchronizację całego planu przez Google Drive jednym plikiem w folderze danych aplikacji (I-68), odebraną na dwóch klientach (I-69).
- Przy konflikcie wersji dialog pokazuje różnice i daty zmian, a ekran „Wybierz zmiany” pozwala wybrać wersję przy każdej różnicy, dla zajęć pole po polu (I-76, I-77).
- Naprawiono sześć błędów synchronizacji, między innymi walidację eksportu JSON, ochronę transakcji Room przy rozpoczęciu edycji i limit 8 MiB wysyłki (I-70). Pobieranie blokują tylko rzeczywiste szkice, a limit zapytań Drive jest odróżniany od innych błędów 403.
- Globalny kierunek bez przypisania do semestru można usunąć (I-73); przełącznik sprawdzania aktualizacji nie ma opisu w stanie włączonym (I-74).
- Szczegóły terminu odświeżają się po zapisie zmian w formularzu zajęć (I-75).
- `MakNoteBanner` rysuje akcje sam i ma jeden neutralny wygląd (I-71); akcje odłożenia i wyjścia w dialogach i kreatorze są przyciskami tekstowymi, a gest wstecz cofa kreator o jeden krok (I-72).

## 0.2.3, 2026-09-29

- Dodano tryb tabletowy: klasy szerokości okna, obrót od 600 dp najkrótszego boku, boczny pasek nawigacji, limit szerokości treści i dwie kolumny „Dzisiaj” od 840 dp (I-45, I-46, I-47).
- Systemowa kopia zapasowa obejmuje tylko bazę planu i ustawienia, a kopia w chmurze wymaga szyfrowania (I-14).
- Uproszczono „Dzisiaj” i „Plan”: filtr kierunku jest jednym polem wyboru, bez zbędnych nagłówków (I-56).
- Znaczniki kalendarza mają kolor kierunku, pierścień dla zmienionego terminu i plus przy wielu zajęciach (I-57); numer dnia ma stałe 13 sp i równe wysokości komórek (I-62).
- Karta i szczegóły terminu pokazują, z którymi zajęciami termin koliduje (I-58).
- Kierunki semestru mają osobny formularz edycji (I-60), a kolor wybiera się suwakami odcienia i jasności (I-61).
- Jeden wspólny dialog usuwania i separator na karcie zajęć (I-59), sekcje „O aplikacji” bez kart (I-55), ciemniejsza karta podsumowania w motywie ciemnym (I-63).
- Wspólna obsługa błędów zapisu w ViewModelach (I-64).

## 0.2.2, 2026-09-28

- Karty zajęć pokazują kierunek jako kolorowy tekst, stan jako ikonę, a notatki jako wiersze z ikoną i treścią, bez statycznych pilli (I-53).
- Widget nie używa pilli dla tygodnia, liczby zajęć ani fazy zajęć. W rozszerzonych rozmiarach pokazuje treść obu rodzajów notatek z właściwymi ikonami (I-53).
- Usunięto pozostałe statyczne pille i strzałki z interfejsu oraz tekstów dla użytkownika (I-53).
- W notatkach wydania przypomniano o możliwości dodania wielu kierunków podczas pierwszej konfiguracji (I-50).

## 0.2.1, 2026-09-28

- Naprawiono wstrzykiwanie zależności `UpdateViewModel`: w 0.2.0 Koin zostawiał atrapy pobierania, weryfikacji i instalacji z wartości domyślnych konstruktora (I-52).
- Kreator pozwala dodać kolejne kierunki ze wspólnym albo osobnym kalendarzem tygodni (I-50).
- Notatki dla użytkowników mają jedno źródło w `release_notes.json`; workflow wypełnia `notes` w `update.json` i opis wydania (I-51).
- Wiersze ustawień mają ramkę focusu, a historia wydań nie łamie daty przy dużej czcionce.

## 0.2.0, 2026-09-27

Pierwsze publiczne wydanie w GitHub Releases.

- Dodano aktualizacje z poziomu aplikacji: ręczne lub automatyczne sprawdzanie, pobieranie, weryfikację i instalację.
- Dodano widget z planem na dziś oraz powiadomienia o kolizjach dzień wcześniej i przed zajęciami.
- Dodano osobne kalendarze tygodni dla kierunków, liczenie okienek i import kopii zapasowej z podglądem.
- Dodano edycję nazw i kolorów kierunków.
- Notatka do terminu zostaje przy terminie po jego przeniesieniu.
- Nowe logo, ekran startowy i uporządkowane ustawienia; lepsza czytelność przy dużej czcionce i obsługa klawiatury.

## 0.1.0, 2026-09-19

- Dodano lokalną bazę Room i odizolowane semestry.
- Dodano kreator konfiguracji, kierunki i formularz zajęć z walidacją godzin.
- Dodano widoki Dzisiaj, Plan i kalendarz miesięczny.
- Dodano tygodnie A/B, ręczne korekty i filtrowanie po kierunku.
- Dodano edycję zajęć, zajęcia jednorazowe oraz zmiany pojedynczych terminów.
- Dodano notatki wspólne i notatki dla wybranego terminu.
- Dodano eksport wszystkich semestrów do pliku JSON z wersją schematu.
