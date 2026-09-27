# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium jest publiczne od 2026-09-27; wydanie `v0.2.0` jest następnym krokiem po I-40.

## 1. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 2. Pierwsze publiczne wydanie (I-49)

Warunek spełniony 2026-09-27: I-39 potwierdziło, że aktualizacja podpisana tym samym kluczem zachowuje dane.

1. Utwórz tag `v0.2.0`, sprawdź w szkicu APK i `update.json` (zgodna suma SHA-256), a potem ręcznie opublikuj wydanie.
2. Sprawdź, że `https://github.com/r3tza/MAK/releases/latest/download/update.json` zwraca plik bez logowania.
3. Zainstaluj `v0.2.0` na telefonie według sekcji „Instalacja” w `README.md` i utwórz dane odbiorowe.

Kryterium zakończenia: publiczne `v0.2.0` jest dostępne, instrukcja instalacji działa, a aplikacja sprawdza wersję przez produkcyjny adres.

## 3. Pełny odbiór aktualizacji na telefonie (O-07)

1. Opublikuj `v0.2.1` z nowym `versionCode` i tym samym certyfikatem.
2. Sprawdź ręczne oraz automatyczne wykrycie, baner, ekran „Aktualizacja”, pobranie, zgodę i instalację.
3. Sprawdź brak sieci, odmowę zgody, anulowanie pobierania i „Nie teraz”.
4. Potwierdź zachowanie planu, notatek i ustawień po aktualizacji.

Kryterium zakończenia: O-07 jest potwierdzony na telefonie, a I-49 może otrzymać status „gotowe”.
