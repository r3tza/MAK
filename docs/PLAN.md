# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium pozostaje prywatne do jawnej zgody użytkownika na I-41.

## 1. Uruchom testy urządzenia aktualizatora (I-37)

1. Zabezpiecz dane z emulatora, na którym jest release o `versionCode` 200, albo uruchom osobny emulator testowy.
2. Uruchom `gradlew.bat connectedDebugAndroidTest` na urządzeniu, na którym można zainstalować wariant debug o `versionCode` 1.
3. Potwierdź `KoinGraphTest`, ekran „O aplikacji” i baner „Dzisiaj” przy 320 dp.
4. Sprawdź oba motywy, dużą czcionkę, focus i opisy TalkBack.

Kryterium zakończenia: testy urządzenia przechodzą, ekran nie ma obciętych akcji, a wyniki są zapisane w `QUEUE.md`.

## 2. Wykonaj lokalny scenariusz instalacji N do N+1 (I-39)

1. Zbuduj dwa podpisane APK tym samym kluczem i z kolejnymi `versionCode`.
2. Zainstaluj N, utwórz plan, oba rodzaje notatek i zmień ustawienia.
3. Podaj aktualizatorowi lokalny artefakt N+1 przez wstrzykiwane granice testowe, sprawdź sumę, pakiet, podpis i zgodę systemową.
4. Potwierdź aktualizację oraz zachowanie danych. Powtórz odmowę zgody i anulowanie.

Kryterium zakończenia: lokalna aktualizacja działa, dane zostają zachowane, a plik APK znika po końcowym wyniku instalacji.

## 3. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 4. Bramka publicznego repozytorium i wydania (I-41)

Bloker: jawna zgoda użytkownika po zakończeniu poprawek prezentacyjnych.

1. Sprawdź historię i bieżące pliki pod kątem sekretów oraz prywatnych danych.
2. Potwierdź weryfikację dwuetapową konta i zmień widoczność repozytorium na publiczną.
3. Utwórz tag `v0.2.0`, sprawdź szkic i ręcznie opublikuj wydanie.
4. Zainstaluj `v0.2.0` na telefonie i utwórz dane odbiorowe.

Kryterium zakończenia: publiczne `v0.2.0` jest dostępne, a aplikacja wykrywa je przez produkcyjny adres.

## 5. Pełny odbiór aktualizacji na telefonie (O-07)

1. Opublikuj `v0.2.1` z nowym `versionCode` i tym samym certyfikatem.
2. Sprawdź ręczne oraz automatyczne wykrycie, baner, pobranie, zgodę i instalację.
3. Sprawdź brak sieci, odmowę zgody, anulowanie pobierania i „Nie teraz”.
4. Potwierdź zachowanie planu, notatek i ustawień po aktualizacji.

Kryterium zakończenia: O-07 jest potwierdzony na telefonie, a I-41 może otrzymać status „gotowe”.
