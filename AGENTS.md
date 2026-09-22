# Zasady pracy agentów

Te zasady obowiązują w całym repozytorium. Szczegóły produktu znajdują się w dokumentacji, a nie w tym pliku.

## Źródła prawdy

- `docs/PRODUCT.md` - cel, zakres i kryteria MVP.
- `docs/DOMAIN.md` - reguły planu, kalendarzy, wystąpień i kolizji.
- `docs/FEATURES.md` - zachowanie ekranów, widgetu i scenariusze odbioru.
- `docs/ARCHITECTURE.md` — granice systemu, terminologia i zasady interfejsu.
- `docs/STACK.md` — języki, narzędzia, środowisko, testy i odrzucone alternatywy.
- `docs/CHANGELOG.md` — zmiany wydane użytkownikom.
- `docs/LOG.md` — 20 ostatnich wpisów o decyzjach i zmianach; starsze wpisy są w `docs/log_archive/`.
- `docs/PLAN.md` - najwyżej pięć najbliższych kroków wykonawczych.
- `docs/QUEUE.md` — bieżące zadania, zależności i oddzielne statusy implementacji oraz odbioru.
- `docs/MAP.md` - mapa dokumentów według rodzaju zadania.
- `docs/WORKFLOW.md` — kolejność pracy i warunki zakończenia zadania.
- `docs/KNOWN_ISSUES.md` — potwierdzone braki i otwarte scenariusze odbioru.
- `docs/WRITING.md` — zasady pisania dokumentacji i tekstów dla użytkownika.
- `README.md` — punkt wejścia do dokumentacji; nie zastępuje źródeł prawdy.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Identyfikatory i komentarze w kodzie: angielski.

## Gdzie czytać

Zacznij od `docs/MAP.md`, potem otwórz tylko dokumenty i kod dotyczące zadania. Jeśli mapa nie wystarcza, znajdź właściwą sekcję przez `rg` i popraw mapę.

## Aktualny punkt pracy

Aktualny status i zależności są w `docs/QUEUE.md`. `docs/PLAN.md` zawiera tylko najbliższe kroki. Wymagania i scenariusze odbioru są w `docs/PRODUCT.md`, `docs/DOMAIN.md` i `docs/FEATURES.md`. Odróżniaj gotowy kod od potwierdzonego odbioru na urządzeniu. Kompilacja testów Android nie oznacza ich uruchomienia.

## Pisanie

Stosuj `docs/WRITING.md` do dokumentacji i tekstów dla użytkownika, chyba że bezpośrednia instrukcja użytkownika albo zaakceptowana zasada produktu stanowi inaczej.

Nie używaj w nowych dokumentach, tekstach interfejsu ani odpowiedziach znaku `·`, em dash `—`, emotek ani ozdobników i schematycznych zwrotów kojarzonych z AI-slop. Wybieraj zwykłą interpunkcję i konkretne sformułowania.

Zasady interfejsu z `docs/ARCHITECTURE.md` są kryteriami akceptacji dla każdego widoku. Sprawdzaj je razem z zachowaniem funkcjonalnym.

## Interfejs

- Przed zmianą interfejsu przeczytaj zasady UI w `docs/ARCHITECTURE.md`, wymagania danego widoku w `docs/FEATURES.md` i prześledź aktualne komponenty Compose.
- Dokumenty są źródłem prawdy dla zakresu i zachowania, a działająca aplikacja jest źródłem bieżącego wyglądu.
- Jeśli zaakceptowana zmiana istotnie wpływa na nawigację, układ, formularze albo widget, zaktualizuj odpowiedni opis i testy.
- Odtwarzaj zaakceptowane zachowanie przy użyciu komponentów i wzorców Compose.

## Decyzje

Przeczytaj odpowiednie sekcje `docs/ARCHITECTURE.md` i `docs/STACK.md` przed zmianą struktury, modułów, narzędzi lub metodologii.

Nie traktuj propozycji z rozmowy jako decyzji, dopóki użytkownik jej nie zaakceptuje i nie zostanie zapisana w odpowiednim pliku.

Nie implementuj elementów oznaczonych jako pytania otwarte lub poza zakresem bez decyzji użytkownika.

Po zaakceptowanej zmianie architektury, stosu lub zasad pracy zaktualizuj właściwy dokument i dodaj wpis do `docs/LOG.md`. Utrzymuj tam najwyżej 20 wpisów; starsze przenoś do `docs/log_archive/`. `docs/CHANGELOG.md` aktualizuj dopiero po wydaniu zmiany użytkownikom.

## Preferencje projektowe użytkownika

Traktuj te zasady jako wskazówki przy proponowaniu rozwiązań. Szczegóły produktu i zaakceptowane wyjątki z `docs/PRODUCT.md`, `docs/DOMAIN.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/STACK.md` i `docs/LOG.md` mają pierwszeństwo.

### Architektura

- Dziel system według odpowiedzialności, stabilnych obszarów danych i przepływów użytkownika. Nie twórz podziału dla każdej tabeli, funkcji ani pliku.
- Nie skupiaj stanu i operacji całej aplikacji w jednym ViewModelu, repozytorium, hoście ani komponencie delegującym.
- Dodawaj abstrakcję, warstwę lub moduł Gradle wtedy, gdy tworzy wyraźną granicę, usuwa duplikację reguł albo realnie zmniejsza koszt przyszłych zmian.
- Preferuj rozwiązania zgodne ze współczesnymi praktykami platformy, ale uwzględniaj skalę projektu. Unikaj zarówno prowizorycznych skrótów utrudniających rozwój, jak i infrastruktury projektowanej bez konkretnej potrzeby.
- Przekazuj zależności jawnie przez konstruktor. Ogranicz kontener DI do composition rootu i hostów wymaganych przez framework.
- Utrzymuj logikę domenową niezależnie od UI, bazy i frameworka. Jedna reguła biznesowa ma jedno źródło używane przez wszystkich konsumentów.
- Pozostaw nawigację właścicielowi platformowego stosu nawigacji. ViewModel może emitować jednorazowy efekt po sukcesie, ale nie powinien utrzymywać drugiej kopii bieżącej trasy.
- Operacje wieloetapowe wykonuj atomowo. Błąd nie może zostawić częściowych danych ani usunąć pracy użytkownika.
- Preferuj rozwiązania lokalne, energooszczędne i łatwe w utrzymaniu. Nie dodawaj usług działających stale w tle bez potwierdzonej potrzeby.
- Aktualizuj narzędzia do najnowszych stabilnych, wzajemnie zgodnych wersji. Nie wybieraj wersji eksperymentalnej wyłącznie dlatego, że jest najnowsza.
- Duże refaktory dziel na małe etapy o jawnej kolejności, odpowiedzialności, kryterium zakończenia i stanie możliwym do sprawdzenia. Plan ma być wykonalny także przez słabszego agenta bez odgadywania intencji.

### Interfejs

- Stawiaj czytelność, szybkie skanowanie i hierarchię informacji ponad dekorację.
- Ograniczaj przeciążenie poznawcze. Grupuj opcje w nazwane sekcje, a rozbudowane lub rzadkie przepływy przenoś na osobne ekrany.
- Preferuj karty, wiersze, krótkie podsumowania, pille i jawny grid, gdy pomagają porównać kilka informacji. Nie dodawaj tabelarycznego układu, jeśli nie poprawia skanowania.
- Traktuj padding, rytm pionowy, wyrównanie i separację komponentów jako kryteria jakości. Przyciski i kontenery nie mogą wyglądać na przypadkowo sklejone.
- Buduj na Material 3 i istniejących wzorcach platformy. Własny komponent dodaj wtedy, gdy odtwarza zaakceptowany wzorzec, którego standardowy komponent nie realizuje wystarczająco dobrze.
- Nadawaj kolorom stałe znaczenie. Powierzchnie pozostawiaj głównie neutralne, a akcentów używaj dla informacji, kategorii, ostrzeżeń i działań o rzeczywistej wadze.
- Nie używaj koloru jako jedynego nośnika informacji. Dodaj etykietę, ikonę, kształt albo treść semantyczną.
- Nie używaj koloru akcentowego wyłącznie do pokazania zwykłego stanu komponentu, jeśli tekst, ikona i semantyka wystarczają.
- Zachowuj jawne działania użytkownika. Nie otwieraj automatycznie kreatora, formularza ani innego przepływu, jeśli stan pusty z jasną akcją daje użytkownikowi większą kontrolę.
- Projektuj od szerokości 320 dp. Brak obciętych akcji, poziomego przewijania, utraty kontrastu i nieczytelnego zawijania jest częścią kryterium akceptacji.
- Traktuj dostępność, motyw ciemny, focus, klawiaturę, TalkBack i stan błędu jako część projektu, nie jako końcowy audyt.
- Przy istotnej zmianie wyglądu najpierw opisz wariant i role informacji. Po akceptacji zapisz decyzję, zaimplementuj ją i porównaj na zrzucie albo urządzeniu przed utrwaleniem drobnych szczegółów wizualnych.

### Organizacja pracy

- Przy zadaniach polegających głównie na czytaniu, porównaniu lub audycie wielu plików użyj subagenta Luna, jeśli równoległy przegląd realnie skróci pracę. Nie deleguj małego, jednoznacznego odczytu, gdy koszt koordynacji będzie większy niż korzyść.

## Zmiany w kodzie

- Najpierw prześledź kod i przepływ danych, którego dotyczy zmiana.
- Używaj istniejących wzorców i bibliotek platformy przed dodaniem nowej abstrakcji lub zależności.
- Waliduj dane na granicach systemu i chroń dane użytkownika przed utratą.
- Dla nietrywialnej logiki zostaw najmniejszy sensowny test uruchamialny. Szczegóły: sekcja Testy.
- Traktuj dostępność, małe ekrany i stan błędu jako część implementacji.

## Widget

- Przed zmianą widgetu przeczytaj sekcję Widget w `docs/ARCHITECTURE.md` oraz wymagania w sekcji Widget `docs/FEATURES.md`.
- Realizuj etapy po kolei. Każdy etap pozostaw w stanie kompilującym się i sprawdzalnym bez zależności od kolejnego etapu.
- Do czasu etapu 11 widget czyta dane przez `MakRepository`, mapuje je przez wspólną granicę danych i oblicza plan przez `ActivePlanProvider`. Po podziale repozytoriów użyj nowej granicy bez zmiany reguł planu. Widget nie woła DAO ani `ScheduleResolver` bezpośrednio i nie kopiuje reguł z ViewModelu.
- Wstrzykuj `Clock`. Nie używaj `LocalDate.now()` bezpośrednio w loaderze, prezenterze ani testach widgetu.
- Room jest źródłem planu. Nie przechowuj kopii planu w preferencjach Glance ani wyłącznie w pamięci procesu.
- Odświeżaj wszystkie instancje po udanej zmianie danych na jednej wspólnej granicy. Nie wywołuj aktualizacji z każdego ekranu osobno i nie aktualizuj przed zakończeniem transakcji.
- Nie dodawaj ciągłego serwisu, dokładnych alarmów ani odświeżania co minutę. Okresowa aktualizacja jest zabezpieczeniem i może zostać opóźniona przez system.
- Używaj ograniczonego zestawu progów rozmiaru. Każdy próg ma jawny limit pozycji i stan pusty, bez poziomego przewijania oraz obciętych akcji.
- Kliknięcie widgetu otwiera jawnie ekran „Dzisiaj”. Szczegóły wystąpienia wymagają osobnej decyzji o kontrakcie deep linków.
- Stan błędu ma być krótki, bez surowych wyjątków, i pozwalać otworzyć aplikację.

## Powiadomienia

- Przed zmianą przeczytaj sekcję Powiadomienia w `docs/ARCHITECTURE.md` oraz odpowiednie decyzje w `docs/LOG.md` albo archiwum. Planista używa `ActivePlanProvider` i wstrzykniętego `Clock`. Nie licz kolizji ponownie w odbiorniku własną regułą.
- Zachowaj dwa rodzaje powiadomień, grupowanie kolizji, domyślnie wyłączoną funkcję i rozdział preferencji użytkownika od zgody systemowej. Używaj alarmów przybliżonych bez ciągłego serwisu i dostępu do dokładnych alarmów.
- Po zmianach danych i ustawień odnawiaj przyszłe alarmy na wspólnej granicy. Przy dostarczeniu sprawdź bieżący plan, zgodę, przełączniki i czas. Test planisty na JVM nie zastępuje odbioru `AlarmManager`, zgody i kliknięcia na urządzeniu.

## Testy

Agenci sprawdzają działanie aplikacji testami, które da się uruchomić lokalnie. Priorytet ma logika domenowa na JVM. Zakres wymagań testowych pozostaje w `docs/STACK.md`; ta sekcja mówi, jak je realizować.

Po zmianie `WeekCalculator`, `ScheduleResolver`, `CollisionDetector`, eksportu JSON albo walidacji formularza uruchom `gradlew.bat test`. Nie czekaj na emulator.

Pisz testy razem z logiką, nie jako osobny etap. Jeden test na jedną regułę z `docs/DOMAIN.md`, `docs/FEATURES.md` albo `docs/ARCHITECTURE.md`. Przykładowe testy szablonu usuń, gdy pojawią się prawdziwe.

### JVM

Pokryj czystym Kotlinem i `java.time`, bez Compose i Room:

- `WeekCalculator`: semestr od środka tygodnia, A/B, data poza semestrem, `ONE_WEEK`, `FROM_WEEK`, nakładanie korekt.
- `ScheduleResolver`: cykle, `ONCE`, odwołanie, zmiana, przeniesienie, przywrócenie, rozdział notatki wspólnej od notatki do daty.
- `CollisionDetector`: nakładka jest kolizją, stykanie godzin nie jest; kolizje po zmianach wystąpień.
- eksport JSON: `schemaVersion` i round-trip modelu.
- walidacja: nazwa, kierunek, godziny; koniec później niż start; zajęcia przechodzące przez północ są odrzucane.

Ten sam `ActivePlanProvider` jest źródłem planu dla listy, kalendarza, ekranu „Dzisiaj”, widgetu i powiadomień. Nie powielaj reguł w testach widoków.

Nie pisz testów rozstrzygających pytania, które nadal pozostają otwarte w `docs/ARCHITECTURE.md`.

### Room

Warstwa bazy jest cienka. Nie pisz testów CRUD dla każdego DAO.

Utrzymuj test trwałości semestru, korekty, `OccurrenceChange` i `OccurrenceNote`. Przy zmianie schematu dodaj test migracji na zachowanych danych. Operacje wieloetapowe, w tym import i konfigurację, sprawdzaj testem rollbacku. Nie powielaj testów domeny w testach DAO.

Nie powielaj testów domeny w warstwie Glance. Przetestuj prezenter widgetu na JVM i sprawdź, że loader woła ten sam `ActivePlanProvider`.

### Compose

Emulator jest wolny. Zostaw kilka przebiegów z wstrzykniętą datą, nie `LocalDate.now()`:

- brak aktywnego semestru;
- ekran „Dzisiaj” po dodaniu semestru, kierunku i zajęć;
- odwołanie i przywrócenie jednego terminu;
- notatka do zajęć kontra notatka do daty;
- szerokość 320 px: brak poziomego przewijania, akcje widoczne.

Kontrast, `reduced motion` i motyw ciemny sprawdzaj w kodzie oraz na emulatorze lub urządzeniu, dopóki nie ma stałego urządzenia w CI.

## Git

Po zakończeniu zadania możesz samodzielnie utworzyć commit obejmujący jego logiczną zmianę. Wypychaj zmiany, twórz gałęzie i zmieniaj historię tylko na wyraźne polecenie użytkownika.

Nie dodawaj stopki `Co-authored-by:` przypisującej pracę agentowi.

Używaj krótkiego, konkretnego tematu w trybie rozkazującym i po angielsku. Dozwolone są prefiksy `feat:`, `fix:`, `chore:` i `docs:`.

## Zakres

Nie przywracaj alternatyw odrzuconych w `docs/STACK.md`, `docs/LOG.md` lub archiwum logu. Nie rozszerzaj zakresu poza `docs/PRODUCT.md`, aby pozornie domknąć pracę.

## Pliki narzędziowe

Zasady pracy trzymaj w `AGENTS.md`. `CLAUDE.md` zawiera wyłącznie import tego pliku.
