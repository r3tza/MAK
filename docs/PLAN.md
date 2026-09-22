# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest jeden. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Dokończyć układ widgetu (I-06)

Granice: `FEATURES.md`, sekcja „Widget” („Mały widget”, „Duży widget”, „Układ i zachowanie widgetu”); `ARCHITECTURE.md`, sekcja Widget. Nie zmieniaj logiki planu ani powiadomień. Użyj `ActivePlanProvider` i wstrzykniętego `Clock`; nie kopiuj reguł i nie wołaj DAO.

Obecny stan: `MakTodayWidget`, `WidgetPresenter` i `WidgetPlanLoader` mają bazowy układ, separatory i licznik kolizji; duży wariant nie wykorzystuje w pełni miejsca, brakuje osobnego wiersza kierunku, lekkiego ostrzeżenia o kolizji, odrębnego nagłówka oraz stanów „Teraz” i „Następne”.

Kolejność:

1. Nagłówek: data jako główny tekst 16 sp, tydzień A/B jako mała etykieta z delikatnym tłem, liczba zajęć obok albo po prawej stronie.
2. Wiersz zajęć: stała kolumna czasu, pasek koloru kierunku, nazwa zajęć, osobny wiersz metadanych (sala, prowadzący) oraz tekstowa nazwa kierunku, bo kolor nie jest jedynym nośnikiem informacji.
3. Kolizja i notatka jako krótkie etykiety tekstowe o czytelnym kontraście; oddzielać zajęcia subtelnymi separatorami, bez kart na każde zajęcia.
4. Wariant kompaktowy (mniej niż 240 dp szerokości albo mniej niż 160 dp wysokości): przewijana lista z nazwą, czasem i salą; pominąć prowadzącego i osobną etykietę notatki, zachować alert kolizji. Bez tekstu „Jeszcze {liczba}”.
5. Wariant rozszerzony: przewijana lista z salą, prowadzącym i statusem kolizji albo notatki; większe odstępy i pełniejsze metadane, nie tylko wyższa lista.
6. Zachować jasne tło, wysoki kontrast, brak zdjęć, gradientów, cieni i ozdobnych ikon; dopasować tło do systemowego promienia widgetów. Pokazywać najwyżej „Następne: 10:15”, bez odliczania.

Przypadki brzegowe:

- brak aktywnego semestru, pusty dzień, data poza semestrem i błąd odczytu;
- jedna i wiele kolizji, notatka, długie nazwy oraz metadane;
- mały i duży rozmiar oraz zmiana rozmiaru;
- motyw jasny i ciemny.

Weryfikacja:

- Test prezentera widgetu na JVM dla obu wariantów, metadanych, kolizji, notatki i kolejności zgodnej z `ActivePlanProvider`.
- Sprawdź, że loader woła ten sam `ActivePlanProvider` i wstrzyknięty `Clock`.
- Uruchom `gradlew.bat test` i `gradlew.bat compileDebugAndroidTestKotlin`.
- Porównanie wariantów na launcherze i odbiór na urządzeniu należą do O-06.

Kryterium zakończenia: oba warianty pokazują dzisiejszy plan z hierarchią z `FEATURES.md`, bez ciągłego serwisu i bez powielania reguł planu.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
