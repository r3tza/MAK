# Zasady pracy agentów

Te zasady obowiązują w całym repozytorium. Szczegóły produktu, architektury i procesu są w dokumentacji; ten plik mówi, jak z niej korzystać.

## Źródła prawdy

- `docs/PRODUCT.md` - cel, zakres i kryteria MVP.
- `docs/DOMAIN.md` - reguły planu, kalendarzy, wystąpień i kolizji.
- `docs/FEATURES.md` - zachowanie ekranów, widgetu i scenariusze odbioru.
- `docs/ARCHITECTURE.md` - granice systemu, terminologia, zasady interfejsu i kodu, zasady widgetu i powiadomień.
- `docs/STACK.md` - języki, narzędzia, środowisko, zakres i sposób testowania, odrzucone alternatywy.
- `docs/WORKFLOW.md` - kolejność pracy, planowanie, statusy i warunki zakończenia zadania.
- `docs/CHANGELOG.md` - techniczna historia wydań dla agentów.
- `app/src/main/assets/release_notes.json` - notatki wydań dla użytkowników: aplikacja, `update.json` i opis wydania na GitHubie.
- `docs/LOG.md` - 20 ostatnich wpisów o decyzjach i zmianach; starsze wpisy są w `docs/log_archive/`.
- `docs/PLAN.md` - najwyżej pięć najbliższych kroków wykonawczych.
- `docs/QUEUE.md` - bieżące zadania, zależności i oddzielne statusy implementacji oraz odbioru.
- `docs/MAP.md` - mapa dokumentów według rodzaju zadania.
- `docs/KNOWN_ISSUES.md` - potwierdzone braki i otwarte scenariusze odbioru.
- `docs/WRITING.md` - zasady pisania dokumentacji i tekstów dla użytkownika.
- `README.md` - punkt wejścia do dokumentacji; nie zastępuje źródeł prawdy.
- `CONTRIBUTING.md` - zasady zgłoszeń dla ludzi; projekt nie przyjmuje pull requestów od innych osób.
- Język dokumentacji: polski. Język odpowiedzi dla użytkownika: polski. Identyfikatory i komentarze w kodzie: angielski.

## Gdzie czytać

Zacznij od `docs/MAP.md`, potem otwórz tylko dokumenty i kod dotyczące zadania. Jeśli mapa nie wystarcza, znajdź właściwą sekcję przez `rg` i popraw mapę.

Po zmianie `README.md`, `AGENTS.md`, `CLAUDE.md`, `CONTRIBUTING.md` albo plików `docs/*.md` uruchom `python3 scripts/check_map.py`. Po zmianie samego skryptu uruchom też `python3 scripts/test_check_map.py`. Kod 0 jest wymagany. Na Windowsie zamiast `python3` użyj `python` albo `py` (`docs/STACK.md`, sekcja „Narzędzia”). Nie dodawaj do tej kontroli pakietów Pythona, sieci, Gradle ani zapisu plików.

## Aktualny punkt pracy

Aktualny status i zależności są w `docs/QUEUE.md`, a najbliższe kroki w `docs/PLAN.md`. Wymagania i scenariusze odbioru są w `docs/PRODUCT.md`, `docs/DOMAIN.md` i `docs/FEATURES.md`. Odróżniaj gotowy kod od potwierdzonego odbioru na urządzeniu. Kompilacja testów Android nie oznacza ich uruchomienia.

Kroki w `docs/PLAN.md` muszą być wykonalne przez słabszego agenta bez zgadywania. Pisz je według szablonu z `docs/WORKFLOW.md`, sekcja „Planowanie”. Niejasność wymagającą decyzji zapisz jako bloker.

## Decyzje

Przeczytaj odpowiednie sekcje `docs/ARCHITECTURE.md` i `docs/STACK.md` przed zmianą struktury, modułów, narzędzi lub metodologii.

Nie traktuj propozycji z rozmowy jako decyzji, dopóki użytkownik jej nie zaakceptuje i nie zostanie zapisana w odpowiednim pliku. Pytanie użytkownika o Twoje zdanie nie jest akceptacją.

Nie implementuj elementów oznaczonych jako pytania otwarte lub poza zakresem bez decyzji użytkownika.

Nowa ogólna zasada nie unieważnia wcześniejszej decyzji zapisanej w `docs/FEATURES.md`, `docs/ARCHITECTURE.md` albo `docs/LOG.md`. Jeśli przyjęty element jest z nią sprzeczny, opisz sprzeczność jako pytanie do użytkownika, a nie jako błąd do naprawy. Przy sprzeczności między dokumentami nie rozstrzygaj jej sam: zgłoś ją z cytatami i zapisz w `docs/KNOWN_ISSUES.md`.

Po zaakceptowanej zmianie architektury, stosu lub zasad pracy zaktualizuj właściwy dokument i dodaj wpis do `docs/LOG.md`. Wpis podaje, kto podjął decyzję: użytkownik albo agent na wyraźne zlecenie użytkownika. Utrzymuj w logu najwyżej 20 wpisów; starsze przenoś do `docs/log_archive/`. `docs/CHANGELOG.md` aktualizuj dopiero po wydaniu zmiany użytkownikom. Przed tagiem wydania dodaj wpis do `app/src/main/assets/release_notes.json`: tylko zmiany odczuwalne dla użytkownika, prostym językiem; bez takich zmian zostaw pustą listę, a aplikacja i workflow pokażą „Pomniejsze poprawki”. Tekst wpisu zatwierdza użytkownik.

## Pisanie

Stosuj `docs/WRITING.md` do dokumentacji i tekstów dla użytkownika, chyba że bezpośrednia instrukcja użytkownika albo zaakceptowana zasada produktu stanowi inaczej.

Nie używaj w nowych dokumentach, tekstach interfejsu ani odpowiedziach znaku `·`, em dash `—`, strzałek (`→`, `->`, `⇒`), emotek ani ozdobników i schematycznych zwrotów kojarzonych z AI-slop. Wybieraj zwykłą interpunkcję i konkretne sformułowania.

## Interfejs

- Zasady interfejsu z `docs/ARCHITECTURE.md`, sekcja 4, są kryteriami akceptacji każdego widoku. Obejmują preferencje projektowe użytkownika i listę zaakceptowanych elementów wyglądu. Sprawdzaj je razem z zachowaniem funkcjonalnym.
- Przed zmianą interfejsu przeczytaj tę sekcję, wymagania widoku w `docs/FEATURES.md` i prześledź aktualne komponenty Compose.
- Dokumenty są źródłem prawdy dla zakresu i zachowania, a działająca aplikacja jest źródłem bieżącego wyglądu.
- Jeśli zaakceptowana zmiana istotnie wpływa na nawigację, układ, formularze albo widget, zaktualizuj odpowiedni opis i testy.
- Odtwarzaj zaakceptowane zachowanie przy użyciu komponentów i wzorców Compose.
- Przy istotnej zmianie wyglądu postępuj według `docs/WORKFLOW.md`, sekcja „Planowanie”: najpierw wariant do akceptacji, potem implementacja i porównanie zrzutów.

## Zmiany w kodzie

- Najpierw prześledź kod i przepływ danych, którego dotyczy zmiana.
- Zasady kodu są w `docs/ARCHITECTURE.md`, sekcja 5. Zasady widgetu i powiadomień są w `docs/ARCHITECTURE.md`, sekcja 7, podsekcje „Zasady widgetu” i „Zasady powiadomień”. Przeczytaj właściwą podsekcję przed zmianą widgetu albo powiadomień.
- Używaj istniejących wzorców i bibliotek platformy przed dodaniem nowej abstrakcji lub zależności.
- Waliduj dane na granicach systemu i chroń dane użytkownika przed utratą.
- Traktuj dostępność, małe ekrany i stan błędu jako część implementacji.

## Testy

Pisz testy razem z logiką, nie jako osobny etap. Jeden test sprawdza jedną regułę z `docs/DOMAIN.md`, `docs/FEATURES.md` albo `docs/ARCHITECTURE.md`. Priorytet ma logika domenowa na JVM. Zakres testów, sposób ich pisania i polecenia są w `docs/STACK.md`, sekcja 5.

Po zmianie `WeekCalculator`, `ScheduleResolver`, `CollisionDetector`, eksportu JSON albo walidacji formularza uruchom `gradlew.bat test`. Nie czekaj na emulator.

## Git

Po zakończeniu zadania możesz samodzielnie utworzyć commit obejmujący jego logiczną zmianę. Wypychaj zmiany, twórz gałęzie i zmieniaj historię tylko na wyraźne polecenie użytkownika.

Nie dodawaj stopki `Co-authored-by:` przypisującej pracę agentowi.

Używaj krótkiego, konkretnego tematu w trybie rozkazującym i po angielsku. Dozwolone są prefiksy `feat:`, `fix:`, `chore:` i `docs:`.

## Zakres

Nie przywracaj alternatyw odrzuconych w `docs/STACK.md`, `docs/LOG.md` lub archiwum logu. Nie rozszerzaj zakresu poza `docs/PRODUCT.md`, aby pozornie domknąć pracę.

## Pliki narzędziowe

Zasady pracy trzymaj w `AGENTS.md`. `CLAUDE.md` zawiera wyłącznie import tego pliku. `CONTRIBUTING.md` jest dla ludzi i nie zawiera zasad pracy agentów.
