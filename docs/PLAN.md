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

## 4. Dodaj klasy szerokości i obrót tabletów (I-45)

1. Dodaj `MakWidthClass` z progami 600 i 840 dp, liczonymi z szerokości okna.
2. Udostępnij klasę przez `LocalMakWidthClass` bez kopiowania progów w ekranach.
3. Zdejmij blokadę pionu w `MainActivity`, gdy najkrótszy bok ma co najmniej 600 dp.
4. Dodaj testy JVM progów i decyzji o orientacji.
5. Sprawdź, że telefon zostaje w pionie, a symulowany tablet może się obracać.

Kryterium zakończenia: progi mają jedno źródło, telefon zachowuje blokadę pionu, a tablet obsługuje obie orientacje.
