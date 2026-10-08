# Produkt MAK

Ten dokument określa cel, zakres i kryteria MVP. Zachowanie poszczególnych ekranów opisuje `FEATURES.md`, reguły planu `DOMAIN.md`, a kolejność prac `PLAN.md`.

## Cel i zakres

MAK (Mój Akademicki Kalendarz) to lekka aplikacja mobilna na Androida do zarządzania planem zajęć, szczególnie przy studiowaniu na dwóch lub większej liczbie kierunków. Głównym urządzeniem jest telefon w pionie; na tabletach i rozłożonych telefonach składanych aplikacja działa w obu orientacjach z bocznym paskiem nawigacji i ograniczoną szerokością treści (decyzja z 2026-09-27, wariant A).

Główne założenia:

- szybki podgląd zajęć na dziś;
- prosty plan tygodniowy;
- obsługa wielu kierunków;
- automatyczne tygodnie A/B z ręcznymi korektami;
- wykrywanie kolizji;
- widget na ekranie głównym;
- powiadomienia systemowe;
- plan działa w pełni bez połączenia z internetem;
- sieć służy aktualizacjom z GitHuba oraz opcjonalnej synchronizacji po połączeniu konta Google (rozszerzenie zlecone 2026-09-30);
- małe zużycie baterii;
- brak własnego backendu i obowiązkowego konta. Opcjonalna synchronizacja Google jest rozszerzeniem po MVP, zleconym przez użytkownika 2026-09-30; projekt opisuje `SYNC_PROPOSAL.md`.

Zasada produktu (decyzja użytkownika z 2026-10-08): MAK nie jest aplikacją, która stawia prywatność ponad wszystko. To zwykła aplikacja, która będzie zyskiwać kolejne funkcje, także korzystające z sieci. Podstawowe funkcje (plan, ekran „Dzisiaj”, widget, powiadomienia i edycja planu) muszą działać bez internetu. Dokumentacja i teksty aplikacji opisują połączenia rzeczowo, bez przedstawiania braku sieci jako głównej cechy.

Aplikacja trafia do kilku znajomych poza Google Play. Wydania są publikowane w GitHub Releases, a aplikacja może je sprawdzić, pobrać i przekazać do instalacji systemowi. Zakres opisuje `ARCHITECTURE.md`, sekcja „Kształt systemu”, punkt „Aktualizacje”.

Po jednorazowym skonfigurowaniu planu użytkownik powinien korzystać głównie z ekranu „Dzisiaj” i widgetu.

## Kryteria ukończenia MVP

MVP można uznać za gotowe, gdy użytkownik potrafi:

1. utworzyć semestr i co najmniej dwa kierunki;
2. dodać zajęcia z pełnymi podstawowymi danymi;
3. zobaczyć właściwe zajęcia dla bieżącej daty i automatycznie wyliczonego tygodnia A/B;
4. zmienić oznaczenie jednego tygodnia, ustawić zmianę od wskazanego tygodnia oraz usunąć korektę;
5. przejść do dowolnego dnia i tygodnia w zakresie semestru;
6. filtrować plan po kierunku;
7. edytować i usuwać wpisy;
8. przełączyć plan między listą i kalendarzem oraz wybrać dzień miesiąca;
9. odwołać, zmienić, przenieść i przywrócić pojedynczy termin bez zmiany cyklu;
10. dodać jednorazowe zajęcia dla konkretnej daty;
11. dodać notatkę do wszystkich wystąpień zajęć;
12. dodać inną notatkę tylko do wybranej daty;
13. zamknąć i ponownie otworzyć aplikację bez utraty danych;
14. wyeksportować plan wraz z korektami, zmianami wystąpień i notatkami do pliku JSON;
15. korzystać z aplikacji bez połączenia z internetem;
16. obsłużyć podstawowe czynności na ekranie o szerokości 320–390 px, bez obciętych akcji i poziomego przewijania, z etykietami semantycznymi, widocznym focusem oraz bez utraty wpisanych danych;
17. obsłużyć te same czynności na tablecie w pionie i w poziomie, bez rozciągniętych linii tekstu i uciętych dialogów.

## Funkcje poza pierwszym zakresem

Na początku nie dodawać:

- obowiązkowego logowania i własnych kont użytkowników;
- backendu i Firebase;
- czatu, ocen i zadań domowych;
- map uczelni;
- automatycznego pobierania planu;
- ciągłego działania aplikacji w tle;
- ciągłego odświeżania widgetu.
