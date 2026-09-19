# MAK — architektura

## 1. Status dokumentu

- Źródło prawdy dla architektury: ten plik.
- Zasady pracy agentów: `AGENTS.md`.
- Uzasadnienia i odrzucone alternatywy: `JOURNAL.md`.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Język identyfikatorów i komentarzy w kodzie: angielski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-19.
- Propozycja z rozmowy staje się decyzją po zaakceptowaniu i zapisaniu w odpowiednim pliku.

## 2. Cel

MAK (Mobilny Akademicki Kalendarz) to lekka aplikacja na Androida do lokalnego zarządzania planem zajęć. Obsługuje wiele kierunków, wiele odizolowanych semestrów, naprzemienne tygodnie A/B, zajęcia jednorazowe, kolizje, powiadomienia oraz widget z dzisiejszym planem.

Nazwa produktu to **MAK**. Rozwinięcie „Mobilny Akademicki Kalendarz” jest wyjaśnieniem skrótu, nie drugą nazwą. W interfejsie, na launcherze i w dokumentacji używamy MAK.

Aplikacja działa offline. Użytkownik nie tworzy konta i nie korzysta z backendu.

## 3. Zakres i terminologia

Główne pojęcia:

- **kierunek** — grupa zajęć oznaczona nazwą i kolorem;
- **zajęcia** — pojedynczy wpis planu z nazwą, terminem, kierunkiem i opcjonalnymi danymi;
- **semestr** — odizolowany zakres dat, konfiguracja tygodni A/B oraz własny plan zajęć;
- **aktywny semestr** — semestr wybrany w ustawieniach, którego plan pokazują ekrany, widget i powiadomienia;
- **tydzień A/B** — oznaczenie całego tygodnia kalendarzowego, od poniedziałku do niedzieli, według którego wybierane są zajęcia;
- **korekta tygodnia** — ręczne oznaczenie jednego tygodnia albo ustawienie oznaczenia, od którego tygodnie znów naprzemiennie się zmieniają;
- **notatka do zajęć** — notatka wspólna dla wszystkich wystąpień danego wpisu zajęć;
- **notatka do wystąpienia** — notatka przypięta do jednego konkretnego terminu zajęć w wybranej dacie;
- **zmiana wystąpienia** — odwołanie, przeniesienie lub zmiana danych jednego konkretnego terminu zajęć cyklicznych;
- **zajęcia jednorazowe** — dodatkowy wpis obowiązujący tylko w jednej dacie, używany między innymi do odrabiania zajęć;
- **plan aktywny** — zestaw zajęć obowiązujących dla wskazanej daty po zastosowaniu semestru, tygodnia A/B, zmian wystąpień i zajęć jednorazowych;
- **kolizja** — nakładanie się godzin dwóch aktywnych zajęć tego samego dnia.

Nazwy „kierunek”, „zajęcia”, „semestr”, „aktywny semestr”, „tydzień A/B”, „korekta tygodnia”, „notatka do zajęć”, „notatka do wystąpienia”, „zmiana wystąpienia”, „zajęcia jednorazowe”, „plan aktywny” i „kolizja” mają stałe znaczenie w dokumentacji oraz interfejsie.

Każdy semestr jest osobnym kontenerem danych. Kierunki, prowadzący, zajęcia, korekty tygodni, notatki i zmiany wystąpień należą do jednego semestru i nie są automatycznie współdzielone z innymi semestrami. Użytkownik może dodać, skonfigurować, wybrać albo usunąć semestr w ustawieniach. Usunięcie semestru wymaga potwierdzenia i usuwa jego dane.

## 4. Zasady projektowania interfejsu

### Czytelność ponad dekorację

Interfejs ma szybko odpowiadać na pytania: jakie zajęcia są dziś, co wymaga działania i jaki jest stan planu. Preferowane są karty, fakty, odznaki, sekcje i wiersze zamiast długich bloków tekstu.

### Stały język wizualny

Wspólne prymitywy, przewidywalne odstępy, jawny grid, powtarzalne akcje i udokumentowane wyjątki mają pierwszeństwo przed ręcznym dopieszczaniem każdej funkcji osobno.

### Dostępność jako część projektu

Klawiatura, focus, semantyczne etykiety, kontrast, `reduced motion`, małe ekrany i brak obciętych akcji są kryteriami akceptacji. Dostępność należy uwzględniać podczas projektowania każdego widoku.

### Praktyczne mobile-first

Dokumentacja i testy interfejsu muszą obejmować szerokości 320–390 px, obsługę dotyku, długie nazwy, arkusze mobilne i brak poziomego przewijania.

### Uczciwość wobec stanu systemu

Interfejs nie pokazuje akcji, która zakończy się przewidywalnym błędem. Data, oznaczenie tygodnia A/B, źródło ręcznej korekty i kolizje mają być jawne i jednoznaczne.

### Konfiguracja początkowa

Jeśli nie ma jeszcze semestru, aplikacja otwiera kreator pierwszej konfiguracji. Kreator prowadzi przez utworzenie semestru i co najmniej jednego kierunku, a następnie pozwala przejść do dodawania zajęć. Ten sam wzorzec służy do dodawania kolejnych semestrów.

### Spokojny, funkcjonalny styl

Interfejs obsługuje motyw jasny, ciemny i systemowy. Używa lokalnego fontu Inter, nie korzysta z zewnętrznych CDN-ów i stosuje animacje oszczędnie.

### Precyzyjny język po polsku

Komunikaty są krótkie i konkretne. Nazwy pojęć pozostają stałe. Powiadomienia są bezosobowe i pozbawione ozdobników. Dwujęzyczność pozostaje poza bieżącym zakresem, dlatego polski jest świadomym priorytetem.

### Szacunek dla pracy użytkownika

Formularze nie kasują wpisanych wartości. Dialogi prawidłowo zwracają focus. Stany puste, błędy i ładowanie korzystają z tego samego modelu widoku co pełne dane.

## 5. Zasada modularności

Projekt dzielimy na małe, wymienne części, ponieważ funkcje i wygląd będą regularnie przebudowywane na podstawie bieżącego feedbacku. Granice między danymi, logiką domenową, ekranami i widgetem mają ograniczać koszt zmiany oraz pozwalać zastąpić jedną część bez przepisywania pozostałych.

Każda funkcja powinna mieć własną, czytelną odpowiedzialność i komunikować się z innymi częściami przez proste modele lub interfejsy. Logika obliczania planu nie może zależeć od komponentów UI, a widget nie może powielać reguł `ScheduleResolver`.

Pakiety w jednym module Gradle: `data`, `domain`, `ui`, `widget`, `export`. ViewModele żyją przy ekranach w `ui`. Nie tworzymy wielu osobnych modułów Gradle bez konkretnej potrzeby, ponieważ zwiększyłyby koszt przebudowy i konfiguracji. Nowy moduł Gradle powstaje dopiero wtedy, gdy ma niezależny cykl zmian, testów albo wyraźną granicę zależności.

## 6. Skala i model użycia

MAK jest aplikacją do użytku własnego, działającą lokalnie na jednym urządzeniu i dla jednego użytkownika. Minimalna wersja Androida to 31 (Android 12). Nie projektujemy jej pod setki użytkowników, współbieżność, multi-tenancy, rozproszony backend, limity API ani skalowanie serwerowe.

Priorytetem są szybkie zmiany, poprawność danych lokalnych, łatwe testowanie i czytelny interfejs. Wydajność optymalizujemy dla planu jednego użytkownika i rozsądnej liczby zajęć w semestrze.

## 7. Kształt systemu

Aplikacja składa się z lokalnej warstwy danych, logiki domenowej, ekranów Compose, powiadomień systemowych i widgetu Glance.

- **Warstwa danych** (`data`) przechowuje kierunki, prowadzących, zajęcia, semestry, korekty tygodni, notatki i zmiany wystąpień w Room nad SQLite. Każdy rekord planu jest przypisany do jednego semestru. Granicą zapisu jest repozytorium. DAO nie wychodzą poza `data`. Eksport JSON nie należy do tej warstwy.
- **Warstwa domenowa** (`domain`) oblicza oznaczenie tygodnia A/B, aktywny plan dla daty i kolizje. `WeekCalculator` jest używany wewnątrz `ScheduleResolver`. Wynik resolvera zawiera plan aktywny, oznaczenie A/B i źródło korekty. `CollisionDetector` działa na już złożonym planie. Walidacja formularza to czysta funkcja wywoływana z ViewModelu.
- **Warstwa interfejsu** (`ui`) udostępnia kreator pierwszej konfiguracji, ekrany „Dzisiaj”, „Plan” w widoku listy lub kalendarza, formularze edycji i ustawienia. Ustawienia pozwalają zarządzać semestrami oraz wskazać aktywny semestr. ViewModel składa stan ekranu z repozytorium, wyniku `ScheduleResolver` i w razie potrzeby `CollisionDetector` albo eksportera. ViewModel nie woła DAO i nie liczy planu sam. Datę do testów wstrzykuje się (`Clock` albo `LocalDate`), nie `LocalDate.now()`.
- **Widget** (`widget`) korzysta z tej samej logiki aktywnego planu co aplikacja. Nie ma ViewModelu. Czyta repozytorium i woła `ScheduleResolver`.
- **Eksport i import** (`export`) zapisują oraz odczytują lokalny plik JSON z wersją schematu. UI wybiera plik przez systemowy wybór dokumentu. Pakiet `export` dostaje zrzut z repozytorium i zapisuje bajty. Domain nie zna `Uri`. Import w wersji 0.3 zostaje w tym samym pakiecie.
- **Powiadomienia** korzystają z lokalnych danych aktywnego semestru i systemowego mechanizmu Androida. Konkretne zdarzenia, treść i moment wysyłki pozostają do ustalenia.

Logika domenowa nie zależy od Compose. `ScheduleResolver` jest wspólnym punktem obliczania planu dla ekranów i widgetu.

## 8. Przepływ danych

1. Przy pierwszym uruchomieniu bez semestrów kreator tworzy pierwszy semestr i kierunek. Po zakończeniu użytkownik może dodać zajęcia.
2. Użytkownik wybiera aktywny semestr w ustawieniach albo zapisuje kierunek, zajęcia lub korektę tygodnia w wybranym semestrze.
3. Repozytorium zapisuje dane lokalnie przez Room.
4. `ScheduleResolver` wywołuje kalkulator tygodni i wyznacza A/B dla daty: automatycznie od pierwszego tygodnia aktywnego semestru, z uwzględnieniem korekt. Pierwszy tydzień zaczyna się w poniedziałek tygodnia zawierającego datę rozpoczęcia semestru.
5. Korekta pojedynczego tygodnia działa tylko w tym tygodniu. Korekta „od tego tygodnia” ustawia A lub B dla wskazanego tygodnia i rozpoczyna od niego nową naprzemienną sekwencję. Późniejsza korekta „od tego tygodnia” zastępuje ją od swojej daty. Korekta pojedyncza ma pierwszeństwo w swoim tygodniu.
6. `ScheduleResolver` wybiera zajęcia cykliczne właściwe dla daty i tygodnia A/B.
7. Resolver stosuje zmiany wystąpień: odwołane usuwa z aktywnego planu, zmienione zastępuje danymi dla jednej daty, a przeniesione usuwa z daty źródłowej i dodaje w dacie docelowej.
8. Resolver dodaje zajęcia jednorazowe oraz dołącza notatkę wspólną i notatkę przypiętą do konkretnego wystąpienia.
9. `CollisionDetector` sprawdza nakładanie aktywnych przedziałów czasu po zastosowaniu wszystkich zmian.
10. Lista dnia, kalendarz i widget prezentują wynik tego samego resolvera. Widget może pokazać skrót albo wskaźnik notatki, a pełna treść jest dostępna po otwarciu szczegółów.
11. Zmiana danych wywołuje odświeżenie widgetu bez ciągłego serwisu w tle. Okresowe odświeżenie stanowi zabezpieczenie; platforma może opóźnić aktualizację po północy.
12. Powiadomienia korzystają z aktywnego semestru, gdy zostaną zdefiniowane ich zdarzenia i treści.
13. Eksport: ekran wybiera miejsce pliku, pakiet `export` serializuje wszystkie semestry wraz z ich danymi. Import w 0.3 idzie tą samą ścieżką w odwrotną stronę, z potwierdzeniem przed nadpisaniem.

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

- Jakie konkretne wersje Kotlin, Compose, Room i Glance zostaną przypięte przy tworzeniu projektu Gradle?
- Jakie zdarzenia, treści i momenty wysyłki będą obsługiwane przez powiadomienia?
