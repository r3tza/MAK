# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie jest jeden. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 1. Neutralny stan sekcji rozwijanych i odstępy (I-04)

Granice: `ARCHITECTURE.md`, sekcja „Stały język wizualny”. Nie zmieniaj logiki ekranów ani widgetu (I-06) ani kart zajęć i podsumowania „Dzisiaj”.

Obecny stan: `MakExpandableSection` w `MakComponents.kt` używa `secondaryContainer` po rozwinięciu. Odstępy między sekcjami i wokół przycisków pełnej szerokości nie są ujednolicone.

Kolejność:

1. W `MakExpandableSection` użyj `surfaceContainer` albo `surfaceContainerLow` w obu stanach. Rozwinięcie pokazują tekst „Ukryj”/„Pokaż”, kierunek ikony i semantyka, bez zmiany tła na kolor akcentowy. Nagłówek i treść zostają jednym kontenerem ze wspólnym kształtem i subtelnym obramowaniem, a treść ma 16 dp wewnętrznego paddingu.
2. Doprowadź odstępy do `ARCHITECTURE.md`: duże sekcje 16 dp, powiązane elementy 12 dp, etykieta od wartości 8 dp, przycisk pełnej szerokości co najmniej 12 dp nad i pod, bez stykania się kontrolek z sąsiednimi kontenerami. Popraw tylko realne naruszenia w `MakComponents.kt` i na ekranach korzystających z tych komponentów.
3. Zachowaj semantykę, focus, klawiaturę, kontrast, motyw ciemny i brak poziomego przewijania przy 320 dp.

Przypadki brzegowe:

- rozwinięta sekcja z długą treścią i długą etykietą;
- treść bez akcji oraz treść z przyciskiem pełnej szerokości;
- motyw jasny i ciemny.

Weryfikacja:

- Uruchom `gradlew.bat test` i `gradlew.bat compileDebugAndroidTestKotlin`.
- W teście Compose sprawdź rozwinięcie i zwinięcie sekcji oraz brak poziomego przewijania przy 320 dp. Koloru tła nie da się sprawdzić bez urządzenia, więc odbiór wizualny należy do O-05.

Kryterium zakończenia: rozwinięta sekcja nie zmienia tła na kolor akcentowy, odstępy odpowiadają `ARCHITECTURE.md`, a sekcja działa przy 320 dp.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
