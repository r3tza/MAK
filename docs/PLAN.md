# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne, a `v0.2.0` opublikowano 2026-09-27.

## 1. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 2. Dokończ pierwsze wydanie i przygotuj `v0.2.1` (I-49)

`v0.2.0` jest opublikowane i sprawdzone (tag, `update.json`, suma SHA-256, certyfikat, instalacja na emulatorze).

1. Poproś znajomego o instalację `v0.2.0` według sekcji „Instalacja” w `README.md` i zanotuj niejasne kroki.
2. Uzupełnij pole `notes` w `update.json` w `.github/workflows/release.yml`, tak aby ekran „Aktualizacja” pokazywał zmiany zamiast „Brak informacji”. Źródło tekstu ustal przed zmianą (np. plik z notatkami wydania w repozytorium).
3. Dodaj wpis 0.2.1 do historii w `ReleaseHistory.kt`.

Kryterium zakończenia: instrukcja instalacji działa u znajomego, a workflow zapisuje notatki w `update.json`.

## 3. Pełny odbiór aktualizacji na telefonie (O-07)

1. Opublikuj `v0.2.1` z nowym `versionCode` i tym samym certyfikatem.
2. Sprawdź ręczne oraz automatyczne wykrycie, baner, ekran „Aktualizacja”, pobranie, zgodę i instalację.
3. Sprawdź brak sieci, odmowę zgody, anulowanie pobierania i „Nie teraz”.
4. Potwierdź zachowanie planu, notatek i ustawień po aktualizacji.

Kryterium zakończenia: O-07 jest potwierdzony na telefonie, a I-49 może otrzymać status „gotowe”.
