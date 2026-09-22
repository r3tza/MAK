# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest jeden. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Uporządkować główne ustawienia i dodać osobne ekrany (I-05)

Granice: `FEATURES.md`, sekcja „Ustawienia i dane”; `ARCHITECTURE.md`, sekcja „Hierarchia ustawień”. Nie zmieniaj logiki eksportu, importu ani powiadomień, przenosisz tylko wejścia i upraszczasz ekran. Widgetu (I-06) nie ruszaj.

Obecny stan: `SettingsScreen` ma lokalną etykietę „USTAWIENIA”, nagłówek „Semestry i wygląd”, wybór semestru, motyw, próg okienka, rozwijane bloki „dane” i „powiadomienia” oraz akcje usuwania i konfiguracji semestru.

Kolejność:

1. Usuń lokalną etykietę i nagłówek strony; jedynym nagłówkiem zostaje topbar. Ekran główny dziel na neutralne sekcje rozdzielone 16 dp i wiersze z nazwą, bieżącą wartością i ikoną przejścia.
2. Sekcja „Plan”: wybór aktywnego semestru oraz wiersz „Zarządzaj semestrami” do osobnej trasy. Próg okienka zostaje ustawieniem planu (na razie na ekranie głównym).
3. Sekcja „Wygląd”: motyw z bieżącą wartością.
4. Sekcja „Powiadomienia”: wiersz z wartością „Włączone”/„Wyłączone” i podsumowaniem godziny oraz wyprzedzenia, prowadzący do nowej trasy „Powiadomienia”; przenieś tam główny przełącznik, przełączniki obu rodzajów, godzinę i wyprzedzenie.
5. Sekcja „Dane”: wiersz „Kopia zapasowa i import” prowadzący do nowej trasy „Dane”; przenieś tam eksport, import i opis zastąpienia. Podgląd importu zostaje osobnym ekranem.
6. Sekcja „O aplikacji”: zwarty wiersz z wersją.
7. Dodaj trasy `settings/semesters`, `settings/notifications` i `settings/data` w `SettingsRoutes.kt` i `MakRoutes.kt` z tytułami w topbarze. Ekran „Semestry” zawiera listę, wybór aktywnego, konfigurację, usuwanie i „Dodaj semestr”. Powrót systemowy działa, a stan ekranu głównego nie resetuje się.

Przypadki brzegowe:

- brak semestrów: sekcja „Plan” pokazuje akcję dodania semestru, bez pustych wierszy;
- powiadomienia wyłączone: wartość „Wyłączone” i brak podsumowania godzin;
- brak zgody systemowej: wiersz pokazuje „Zablokowane przez system”;
- 320 dp, motyw ciemny i TalkBack.

Weryfikacja:

- Uruchom `gradlew.bat test`, `gradlew.bat compileDebugAndroidTestKotlin`, `gradlew.bat lintDebug` i `gradlew.bat assembleDebug`.
- W `SettingsScreenTest` sprawdź sekcje, brak lokalnego nagłówka, przejścia do tras i 320 dp.
- Odbiór wyglądu na urządzeniu należy do O-05.

Kryterium zakończenia: główny ekran nie ma rozwijanych formularzy ani powtórzonego nagłówka, a „Semestry”, „Powiadomienia” i „Dane” są osobnymi trasami w jednym `NavHost`.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
