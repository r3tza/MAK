# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

## 1. Domknij przypadki błędów aktualizacji (O-07)

Główny przebieg aktualizacji z aplikacji do `v0.2.2` działa, co użytkownik potwierdził 2026-09-28.

1. Wyłącz sieć i potwierdź czytelny błąd ręcznego sprawdzania.
2. Odmów zgody na instalowanie z tego źródła, wróć do aplikacji i potwierdź możliwość ponowienia.
3. Anuluj pobieranie i sprawdź usunięcie pliku częściowego.
4. Użyj „Nie teraz” i potwierdź ukrycie banera tylko dla wersji 0.2.2.
5. Potwierdź zachowanie planu, notatek i ustawień po aktualizacji, jeśli nie sprawdzono tego w pierwszym przebiegu.

Kryterium zakończenia: wszystkie przypadki O-07 są potwierdzone na telefonie.

## 2. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 3. Sprawdź instrukcję instalacji u znajomego (I-49)

`v0.2.2` jest opublikowane, a aktualizacja z aplikacji działa na telefonie użytkownika.

1. Poproś znajomego o instalację `v0.2.2` według sekcji „Instalacja” w `README.md`.
2. Zanotuj niejasne kroki i popraw instrukcję.

Kryterium zakończenia: instalacja u znajomego przebiegła według README, a niejasne kroki są poprawione.

## 4. Rozdziel role komunikatów (I-54)

1. Zastąp jeden styl `MakNoteBanner` jawnymi rolami neutralną, ostrzegawczą i błędu.
2. Przypisz każde obecne użycie według znaczenia treści.
3. Zwykłe instrukcje pokaż bez akcentowego tła.
4. Dodaj testy Compose ról w obu motywach.

Kryterium zakończenia: kolor komunikatu ma stałe znaczenie, a informacja neutralna nie wygląda jak ostrzeżenie ani główna akcja.

## 5. Ogranicz karty na ekranie „O aplikacji” (I-55)

1. Zachowaj nagłówki i odstępy sekcji.
2. Usuń osobne karty z sekcji „Możliwości” oraz „Dane i prywatność”.
3. Zachowaj wspólną granicę historii wydań, ponieważ grupuje listę wersji.
4. Porównaj ekran przy 320 dp w obu motywach.

Kryterium zakończenia: hierarchia pozostaje czytelna bez obramowania każdej sekcji, a lista wydań nadal jest jednoznaczną grupą.

Klasy szerokości i obrót tabletów (I-45) są następnym zadaniem po powyższym planie.
