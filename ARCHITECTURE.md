# MAK — architektura

## 1. Status dokumentu

- Źródło prawdy dla architektury: ten plik.
- Zasady pracy agentów: `AGENTS.md`.
- Uzasadnienia i odrzucone alternatywy: `JOURNAL.md`.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Język identyfikatorów i komentarzy w kodzie: angielski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-21.
- Propozycja z rozmowy staje się decyzją po zaakceptowaniu i zapisaniu w odpowiednim pliku.

## 2. Cel

MAK (Mobilny Akademicki Kalendarz) to lekka aplikacja na Androida do lokalnego zarządzania planem zajęć. Obsługuje wiele kierunków, wiele odizolowanych semestrów, naprzemienne tygodnie A/B, zajęcia jednorazowe, kolizje, powiadomienia oraz widget z dzisiejszym planem.

Nazwa produktu to **MAK**. Rozwinięcie „Mobilny Akademicki Kalendarz” jest wyjaśnieniem skrótu, nie drugą nazwą. W interfejsie, na launcherze i w dokumentacji używamy MAK.

Aplikacja działa offline. Użytkownik nie tworzy konta i nie korzysta z backendu.

## 3. Zakres i terminologia

Główne pojęcia:

- **kierunek** — trwały kierunek studiów oznaczony nazwą i kolorem, który może występować w wielu semestrach;
- **przypisanie kierunku** — połączenie kierunku z semestrem i kalendarzem akademickim;
- **kalendarz akademicki** — wspólny zestaw dat semestru, rytmu A/B i korekt tygodni używany przez jeden lub kilka kierunków;
- **zajęcia** — pojedynczy wpis planu z nazwą, terminem, kierunkiem i opcjonalnymi danymi;
- **semestr** — nazwany kontener aktywnego planu, kierunków i ich kalendarzy akademickich;
- **aktywny semestr** — semestr wybrany w ustawieniach, którego plan pokazują ekrany, widget i powiadomienia;
- **tydzień A/B** — oznaczenie całego tygodnia kalendarzowego, od poniedziałku do niedzieli, według którego wybierane są zajęcia;
- **korekta tygodnia** — ręczne oznaczenie jednego tygodnia albo ustawienie oznaczenia, od którego tygodnie znów naprzemiennie się zmieniają;
- **notatka do zajęć** — notatka wspólna dla wszystkich wystąpień danego wpisu zajęć;
- **notatka do wystąpienia** — notatka przypięta do jednego konkretnego terminu zajęć w wybranej dacie;
- **zmiana wystąpienia** — odwołanie, przeniesienie lub zmiana danych jednego konkretnego terminu zajęć cyklicznych;
- **zajęcia jednorazowe** — dodatkowy wpis obowiązujący tylko w jednej dacie, używany między innymi do odrabiania zajęć;
- **plan aktywny** — zestaw zajęć obowiązujących dla wskazanej daty po zastosowaniu semestru, tygodnia A/B, zmian wystąpień i zajęć jednorazowych;
- **kolizja** — nakładanie się godzin dwóch aktywnych zajęć tego samego dnia, także zajęć należących do różnych kierunków.

Nazwy „kierunek”, „przypisanie kierunku”, „kalendarz akademicki”, „zajęcia”, „semestr”, „aktywny semestr”, „tydzień A/B”, „korekta tygodnia”, „notatka do zajęć”, „notatka do wystąpienia”, „zmiana wystąpienia”, „zajęcia jednorazowe”, „plan aktywny” i „kolizja” mają stałe znaczenie w dokumentacji oraz interfejsie.

Każdy semestr jest osobnym kontenerem planu. Globalne kierunki mogą być przypisane do wielu semestrów. `SemesterProgram` łączy kierunek z semestrem i wskazuje `AcademicCalendar`. Kilka kierunków może współdzielić jeden kalendarz, a kierunek z innej uczelni może używać własnego. Daty, rytm A/B i korekty zawsze są współdzielone razem. Zajęcia, notatki i zmiany wystąpień pozostają odizolowane w ramach przypisania kierunku do semestru.

## 4. Zasady projektowania interfejsu

### Czytelność ponad dekorację

Interfejs ma szybko odpowiadać na pytania: jakie zajęcia są dziś, co wymaga działania i jaki jest stan planu. Preferowane są karty, fakty, odznaki, sekcje i wiersze zamiast długich bloków tekstu.

### Stały język wizualny

Wspólne prymitywy, przewidywalne odstępy, jawny grid, powtarzalne akcje i udokumentowane wyjątki mają pierwszeństwo przed ręcznym dopieszczaniem każdej funkcji osobno.

### Material 3 jako podstawa konstrukcji

Interfejs budujemy na komponentach i zasadach Material 3. Dopuszczalne są własne kolory, typografia, kształty, karty, nawigacja i układ, jeśli zachowują semantykę oraz przewidywalne zachowanie komponentów Material 3.

Własne komponenty stosujemy tylko wtedy, gdy są potrzebne do odtworzenia zaakceptowanego wzorca. Każdy taki komponent musi zachować etykiety semantyczne, obszar dotyku co najmniej 48 dp, obsługę focusu i klawiatury, kontrast, motyw jasny i ciemny oraz poprawne działanie na szerokości 320 dp. Pola, listy wyboru, przyciski, pola wyboru, opcje jednokrotnego wyboru, dialogi i nawigację zastępujemy własnym rozwiązaniem tylko po sprawdzeniu tych kryteriów.

### Dostępność jako część projektu

Klawiatura, focus, semantyczne etykiety, kontrast, `reduced motion`, małe ekrany i brak obciętych akcji są kryteriami akceptacji. Dostępność należy uwzględniać podczas projektowania każdego widoku.

### Praktyczne mobile-first

Dokumentacja i testy interfejsu muszą obejmować szerokości 320–390 px, obsługę dotyku, długie nazwy, arkusze mobilne i brak poziomego przewijania.

### Uczciwość wobec stanu systemu

Interfejs nie pokazuje akcji, która zakończy się przewidywalnym błędem. Data, oznaczenie tygodnia A/B, źródło ręcznej korekty i kolizje mają być jawne i jednoznaczne.

### Kolizja nie jest winą użytkownika

Kolizja godzin jest informacją o tym, że zajęcia z dwóch kierunków nakładają się w planie. Nie jest błędem użytkownika ani sugestią, że powinien zmienić własne dane. Aplikacja ma ostrzec, wskazać zajęcia i pokazać zakres nakładania, ale nie proponuje zmiany terminu i nie zmienia go automatycznie. Decyzja o kontakcie z uczelnią, opuszczeniu zajęć albo ręcznym przeniesieniu terminu należy do użytkownika.

Na karcie zajęć kolizja używa neutralnego stylu ostrzegawczego i pokazuje dokładny zakres nakładania. Nie używa koloru błędu ani komunikatu sugerującego winę użytkownika.

Gradientowe podsumowanie ekranu „Dzisiaj” pokazuje w trzech równych kolumnach liczbę zajęć, unikalnych kolizji i okienek. Liczba kolizji jest czerwona, gdy jest większa od zera, oraz zielona, gdy wynosi zero. Kolor opisuje stan planu i nie zmienia neutralnego sposobu opisywania kolizji na kartach zajęć.

Karta zajęć używa dwukolumnowej siatki z osobną kolumną godzin oraz sekcjami danych, statusu i notatek. Pełny kolor kierunku występuje na pasku karty, a jego jaśniejszy wariant na pillu z nazwą kierunku. Cała karta pozostaje neutralna. Kolizja używa pomarańczowego stylu ostrzegawczego, notatka wspólna niebieskiego lub indygo pilla „Notatka do zajęć”, a notatka pojedynczego wystąpienia fioletowego pilla „Notatka na dziś”. Kolor zawsze występuje razem z etykietą tekstową.

### Gęstość ekranu planu

Ekran „Plan” grupuje zakres dat, nawigację tygodnia, oznaczenie A/B i źródło korekty w jednej sekcji. Akcja zmiany tygodnia A/B znajduje się przy tej informacji, a nie w odłączonym menu. Zwinięte filtry pokazują aktywny kierunek. Karty zajęć rozdzielają nazwę, kierunek i typ, metadane, kolizję oraz notatkę na czytelne wiersze. Układ nie może ukrywać pierwszych zajęć przez nadmiernie wysokie elementy sterujące.

### Konfiguracja początkowa

Jeśli nie ma semestru, aplikacja pokazuje stan pusty z przyciskiem „Skonfiguruj plan”. Kreator otwiera się wyłącznie po jawnej akcji użytkownika. Prowadzi przez utworzenie semestru, pierwszego kierunku, kalendarza akademickiego i ich powiązania, a następnie pozwala przejść do dodawania zajęć.

### Spokojny, funkcjonalny styl

Interfejs obsługuje motyw jasny, ciemny i systemowy. Używa lokalnego fontu Inter, nie korzysta z zewnętrznych CDN-ów i stosuje animacje oszczędnie.

### Precyzyjny język po polsku

Komunikaty są krótkie i konkretne. Nazwy pojęć pozostają stałe. Powiadomienia są bezosobowe i pozbawione ozdobników. Dwujęzyczność pozostaje poza bieżącym zakresem, dlatego polski jest świadomym priorytetem.

### Szacunek dla pracy użytkownika

Formularze nie kasują wpisanych wartości. Dialogi prawidłowo zwracają focus. Stany puste, błędy i ładowanie korzystają z tego samego modelu widoku co pełne dane.

## 5. Zasada modularności

Projekt dzielimy na małe, wymienne części, ponieważ funkcje i wygląd będą regularnie przebudowywane na podstawie bieżącego feedbacku. Granice między danymi, logiką domenową, ekranami i widgetem mają ograniczać koszt zmiany oraz pozwalać zastąpić jedną część bez przepisywania pozostałych.

Każda funkcja powinna mieć własną, czytelną odpowiedzialność i komunikować się z innymi częściami przez proste modele lub interfejsy. Logika obliczania planu nie może zależeć od komponentów UI, a widget nie może powielać reguł `ScheduleResolver`.

Wspólny `ActivePlanProvider` składa aktywny plan dla daty z modeli domenowych, wywołuje `ScheduleResolver` i w razie potrzeby `CollisionDetector`. Mapowanie danych Room na modele domenowe znajduje się poza ViewModelem. Provider nie zależy od Compose ani Glance, a domena nie zależy od Room. Ekrany i widget korzystają z tej samej ścieżki obliczeń.

Pakiety w jednym module Gradle: `data`, `domain`, `ui`, `widget`, `export`. ViewModele żyją przy ekranach w `ui`. Nie tworzymy wielu osobnych modułów Gradle bez konkretnej potrzeby, ponieważ zwiększyłyby koszt przebudowy i konfiguracji. Nowy moduł Gradle powstaje dopiero wtedy, gdy ma niezależny cykl zmian, testów albo wyraźną granicę zależności.

Koin 4.2 składa graf zależności na granicy aplikacji po aktualizacji projektu do zgodnego stabilnego zestawu narzędzi. Klasy otrzymują zależności przez konstruktor; nie pobierają ich z globalnego kontenera. `get()` i `koinInject()` mogą wystąpić wyłącznie w definicjach Koin albo na granicy hosta, który pobiera ViewModel. Compiler plugin sprawdza pełną konfigurację podczas kompilacji.

`NavController` jest jedynym źródłem bieżącej trasy. ViewModel może zgłaszać jednorazowy zamiar nawigacji po zakończeniu operacji, ale nie przechowuje kopii aktualnej trasy. Publiczny stan UI zawiera modele prezentacyjne i potrzebne identyfikatory, a nie encje Room ani relacje bazy.

ViewModele rozdzielamy według przepływów ekranów. Ekrany „Dzisiaj” i „Plan” mają osobne `TodayViewModel` i `ScheduleViewModel`; formularze, szczegóły, semestr, kreator oraz ustawienia zachowują własne ViewModele. ViewModel ekranu składa tylko jego stan i nie deleguje akcji przez nadrzędny ViewModel.

Po wydzieleniu przepływów `MakViewModel` nie pozostaje wspólnym kontenerem stanów ekranów. Jeśli uruchomienie aplikacji nadal wymaga właściciela stanu, zastępuje go mały `AppViewModel`, który rozpoznaje gotowość danych i potrzebę uruchomienia albo wznowienia konfiguracji. `AppViewModel` nie przechowuje kopii trasy, modeli ekranów, motywu, filtrów ani formularzy. Jeśli po usunięciu tych odpowiedzialności nie ma własnego stanu, należy usunąć nadrzędny ViewModel.

Trwałe preferencje zapisujemy poza pamięcią ViewModelu. Motyw zapisujemy w Preferences DataStore jako `ThemeMode` (`System`, `Light`, `Dark`), a nieznaną albo brakującą wartość traktujemy jako `System`. Istotny stan roboczy przygotowujemy do odtworzenia procesu.

Warstwa danych udostępnia `SemesterRepository` dla semestrów, aktywnego semestru, globalnych kierunków, przypisań i kalendarzy oraz `ScheduleRepository` dla zajęć, wystąpień, notatek i danych planu. Prowadzący jest opcjonalnym tekstem zajęć, bez osobnego repozytorium i kartoteki. `SettingsPreferences` pozostaje osobną granicą trwałych ustawień. Nie tworzymy repozytoriów dla każdej tabeli ani dla każdego ekranu. Publiczne kontrakty nie wystawiają encji Room ani relacji bazy.

Operacja obejmująca kilka zależnych zapisów ma jedną granicę transakcji w `data`. Jawny use case albo serwis koordynuje operację wieloetapową, a ViewModel wywołuje ją jako całość. UI otrzymuje jawny stan zapisu i zachowuje wartości formularza po błędzie. Import jest atomowy: zapisuje cały zaakceptowany plik albo nie zmienia bazy.

## 6. Skala i model użycia

MAK jest aplikacją do użytku własnego, działającą lokalnie na jednym urządzeniu i dla jednego użytkownika. Minimalna wersja Androida to 31 (Android 12). Nie projektujemy jej pod setki użytkowników, współbieżność, multi-tenancy, rozproszony backend, limity API ani skalowanie serwerowe.

Priorytetem są szybkie zmiany, poprawność danych lokalnych, łatwe testowanie i czytelny interfejs. Wydajność optymalizujemy dla planu jednego użytkownika i rozsądnej liczby zajęć w semestrze.

## 7. Kształt systemu

Aplikacja składa się z lokalnej warstwy danych, logiki domenowej, ekranów Compose, powiadomień systemowych i widgetu Glance.

- **Warstwa danych** (`data`) przechowuje globalne kierunki, semestry, przypisania kierunków, kalendarze akademickie, zajęcia, korekty, notatki i zmiany wystąpień w Room nad SQLite. Zajęcia należą do `SemesterProgram`, korekty do `AcademicCalendar`, a prowadzący jest tekstem zajęć. `SemesterRepository` i `ScheduleRepository` są granicami odczytu i zapisu, a DAO nie wychodzą poza `data`. Warstwa udostępnia pełny snapshot kopii zapasowej oraz atomowe zastąpienie danych, ale nie koduje JSON i nie obsługuje `Uri`.
- **Warstwa domenowa** (`domain`) oblicza oznaczenie tygodnia A/B, aktywny plan dla daty, kolizje i okienka. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Wynik resolvera zawiera plan aktywny, oznaczenie A/B i źródło korekty. `CollisionDetector` działa na już złożonym planie. Okienko jest przerwą dłuższą niż globalny próg, domyślnie 30 minut, między połączonymi blokami aktywnych zajęć. Nie obejmuje czasu przed pierwszymi ani po ostatnich zajęciach. Walidacja formularza to czysta funkcja wywoływana z ViewModelu.
- **Warstwa interfejsu** (`ui`) udostępnia kreator pierwszej konfiguracji, ekrany „Dzisiaj”, „Plan” w widoku listy lub kalendarza, formularze edycji i ustawienia. Ustawienia pozwalają zarządzać semestrami oraz wskazać aktywny semestr. ViewModel składa stan ekranu z repozytorium, wyniku `ScheduleResolver` i w razie potrzeby `CollisionDetector` albo eksportera. ViewModel nie woła DAO i nie liczy planu sam. Datę do testów wstrzykuje się (`Clock` albo `LocalDate`), nie `LocalDate.now()`.
- **Widget** (`widget`) nie ma ViewModelu. Czyta dane przez repozytorium, mapuje je na modele domenowe i woła `ActivePlanProvider` z wstrzykniętą datą. Room pozostaje źródłem planu, a widget nie przechowuje jego kopii w preferencjach Glance.
- **Eksport i import** (`export`) kodują oraz odczytują lokalny plik JSON z wersją schematu. UI wybiera plik przez systemowy wybór dokumentu. `PlanBackupService` albo dedykowany use case łączy kodek ze snapshotem i atomową operacją warstwy danych. Domain nie zna `Uri`.
- **Powiadomienia** korzystają z lokalnych danych aktywnego semestru i ostrzegają o kolizjach wieczorem dnia poprzedniego oraz przed rozpoczęciem kolidujących zajęć. Domyślna godzina powiadomienia wieczornego to 20:00, a domyślne wyprzedzenie drugiego powiadomienia wynosi 30 minut. Oba rodzaje można osobno wyłączyć, a ich czas skonfigurować globalnie. Dostarczenie obsługuje `AlarmManager` przez alarmy przybliżone. Aplikacja nie żąda dostępu do dokładnych alarmów i nie obiecuje dostarczenia o dokładnej godzinie.

Logika domenowa nie zależy od Compose ani Glance. `ActivePlanProvider` jest wspólnym punktem obliczania planu i kolizji dla ekranów oraz widgetu.

## 8. Przepływ danych

1. Przy pierwszym uruchomieniu bez semestrów aplikacja pokazuje stan pusty. Jawna akcja otwiera kreator, który atomowo tworzy semestr, pierwszy kierunek, kalendarz i przypisanie.
2. Użytkownik wybiera aktywny semestr, przypina globalne kierunki oraz wybiera dla nich wspólny albo osobny kalendarz.
3. Repozytorium zapisuje dane lokalnie przez Room.
4. `ScheduleResolver` wywołuje kalkulator osobno dla kalendarza każdego przypisania kierunku i wyznacza A/B z uwzględnieniem jego korekt.
5. Korekta pojedynczego tygodnia działa tylko w tym tygodniu. Korekta „od tego tygodnia” ustawia A lub B dla wskazanego tygodnia i rozpoczyna od niego nową naprzemienną sekwencję. Późniejsza korekta „od tego tygodnia” zastępuje ją od swojej daty. Korekta pojedyncza ma pierwszeństwo w swoim tygodniu.
6. `ScheduleResolver` wybiera zajęcia cykliczne właściwe dla daty i kalendarza kierunku, a następnie łączy wyniki aktywnego semestru.
7. Resolver stosuje zmiany wystąpień: odwołane usuwa z aktywnego planu, zmienione zastępuje danymi dla jednej daty, a przeniesione usuwa z daty źródłowej i dodaje w dacie docelowej.
8. Resolver dodaje zajęcia jednorazowe oraz dołącza notatkę wspólną i notatkę przypiętą do konkretnego wystąpienia.
9. `CollisionDetector` sprawdza nakładanie aktywnych przedziałów czasu po zastosowaniu wszystkich zmian.
10. Lista dnia, kalendarz i widget prezentują wynik tego samego resolvera. Widget może pokazać skrót albo wskaźnik notatki, a pełna treść jest dostępna po otwarciu szczegółów.
11. Zmiana danych wywołuje odświeżenie widgetu bez ciągłego serwisu w tle. Okresowe odświeżenie stanowi zabezpieczenie; platforma może opóźnić aktualizację po północy.
12. Powiadomienia o kolizjach są planowane przybliżonymi alarmami wieczorem dnia poprzedniego i w oknie przed kolidującymi zajęciami. Zmiana danych lub ustawień zastępuje przyszłe alarmy aktualnym zestawem.
13. Eksport zapisuje pełny snapshot. Import pokazuje podgląd i po potwierdzeniu atomowo zastępuje wszystkie lokalne dane.

Poza zakresem semestru aplikacja pokazuje jednoznaczny stan wymagający konfiguracji albo informację, że nie ma aktywnego semestru.

## 9. Poza zakresem

Pierwszy zakres nie obejmuje:

- logowania i kont użytkowników;
- backendu, Firebase i synchronizacji w chmurze;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu;
- powiadomienia wymagające stałego działania aplikacji w tle;
- dwujęzycznego interfejsu;
- synchronizacji między urządzeniami;
- skalowania dla wielu użytkowników i obsługi ruchu serwerowego.

## 10. Otwarte pytania

Brak otwartych pytań blokujących obecny plan rozwoju.

## 11. Problemy do rozwiązania

- Dokończyć audyt własnych komponentów Compose względem Material 3 i zasad dostępności. Audyt ma objąć obszary dotyku o rozmiarze co najmniej 48 dp, semantykę, focus, klawiaturę, kontrast, motyw ciemny oraz szerokość 320 dp.
