# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

## 1. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 2. Wydaj `v0.2.1` z poprawką aktualizatora (I-52)

`v0.2.0` nie potrafi pobrać aktualizacji (I-52), więc 0.2.1 instaluje się ręcznie. Notatki wydań są w `app/src/main/assets/release_notes.json` (I-51).

1. Dodaj na początek listy `releases` wpis `0.2.1` z datą i zmianami odczuwalnymi dla użytkownika: naprawione pobieranie aktualizacji z aplikacji, kolejne kierunki w kreatorze. Tekst zatwierdza użytkownik. Uruchom `gradlew.bat test`.
2. Wypchnij `main`, utwórz tag `v0.2.1`, sprawdź w szkicu APK, `update.json` z wypełnionym `notes` i opis wydania, a potem opublikuj.
3. Na urządzeniach z 0.2.0 zainstaluj 0.2.1 ręcznie z GitHub Releases na istniejącą aplikację i sprawdź, że plan został.
4. Poproś znajomego o instalację według sekcji „Instalacja” w `README.md` i zanotuj niejasne kroki.

Kryterium zakończenia: 0.2.1 jest opublikowane z notatkami z pliku, a urządzenia do odbioru mają 0.2.1.

## 3. Pełny odbiór aktualizacji na telefonie (O-07)

1. Opublikuj `v0.2.2` z nowym `versionCode`, tym samym certyfikatem i wpisem w `release_notes.json`.
2. Sprawdź ręczne oraz automatyczne wykrycie, baner, ekran „Aktualizacja”, pobranie, zgodę i instalację.
3. Sprawdź brak sieci, odmowę zgody, anulowanie pobierania i „Nie teraz”.
4. Potwierdź zachowanie planu, notatek i ustawień po aktualizacji.

Kryterium zakończenia: O-07 jest potwierdzony na telefonie, a I-49 może otrzymać status „gotowe”.
