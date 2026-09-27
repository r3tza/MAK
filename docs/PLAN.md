# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Pełna lista i oddzielny status odbioru są w `QUEUE.md`. Repozytorium pozostaje prywatne do jawnej zgody użytkownika na I-41.

## 1. Przenieś aktualizacje do głównych ustawień (I-48)

Zakres: `SettingsScreen.kt`, `SettingsRoutes.kt`, `MakRoutes.kt`, `TodayScheduleRoutes.kt`, `AboutScreen.kt`, nowy `UpdateScreen.kt`, `ReleaseHistory.kt` i ich testy. Bez zmian w logice sprawdzania, pobierania i instalacji w `update`.

1. `releaseHistoryFor`: dopasuj wersję po `removeSuffix("-debug")`, nie dodawaj pustej pozycji dla nieznanej wersji, zwróć najwyżej trzy znane wydania. Zaktualizuj `ReleaseHistoryTest`.
2. Dodaj trasę `MakRoutes.SettingsUpdate = "settings/update"` z tytułem „Aktualizacja”. Przenieś do `UpdateScreen` wynik dostępnej wersji z notatkami oraz `UpdateDownloadActions`. Przy braku dostępnej wersji pokaż „Brak informacji o nowej wersji.” i „Sprawdź teraz”.
3. W `SettingsScreen` dodaj sekcję „Aktualizacje” przed „O aplikacji”: wiersz „Sprawdź aktualizacje” z podsumowaniem wyniku (teksty w `FEATURES.md`), nieaktywny w trakcie sprawdzania; warunkowy wiersz „Aktualizacja do {wersja}” z ikoną przejścia; przełącznik „Sprawdzaj przy uruchomieniu” z opisem. Wynik nie nawiguje sam.
4. „Zobacz” na banerze „Dzisiaj” prowadzi do `settings/update`.
5. `AboutScreen`: sekcja z nazwą, wersją, opisem i „Autor: r3tza” oraz „Ostatnie zmiany”. Usuń akcje aktualizacji i przełącznik.
6. Testy Compose: sekcja „Aktualizacje” (wiersz nieaktywny w trakcie, wiersz przejścia tylko przy dostępnej wersji), `UpdateScreen` (pobieranie, stan bez wersji), `AboutScreenTest` bez akcji, wszystko przy 320 dp. Uruchom `gradlew.bat test compileDebugAndroidTestKotlin lintDebug`.

Kryterium zakończenia: testy JVM i lint przechodzą, testy Compose się kompilują, zrzut ustawień, „Aktualizacji” i „O aplikacji” w obu motywach przy 320 dp bez obciętych akcji.

## 2. Uruchom testy urządzenia aktualizatora (I-37)

1. Zabezpiecz dane z emulatora, na którym jest release o `versionCode` 200, albo uruchom osobny emulator testowy.
2. Uruchom `gradlew.bat connectedDebugAndroidTest` na urządzeniu, na którym można zainstalować wariant debug o `versionCode` 1.
3. Potwierdź `KoinGraphTest`, sekcję „Aktualizacje” w ustawieniach, ekrany „Aktualizacja” i „O aplikacji” oraz baner „Dzisiaj” przy 320 dp.
4. Sprawdź oba motywy, dużą czcionkę, focus i opisy TalkBack.

Kryterium zakończenia: testy urządzenia przechodzą, ekran nie ma obciętych akcji, a wyniki są zapisane w `QUEUE.md`.

## 3. Wykonaj lokalny scenariusz instalacji N do N+1 (I-39)

1. Zbuduj dwa podpisane APK tym samym kluczem i z kolejnymi `versionCode`.
2. Zainstaluj N, utwórz plan, oba rodzaje notatek i zmień ustawienia.
3. Podaj aktualizatorowi lokalny artefakt N+1 przez wstrzykiwane granice testowe, sprawdź sumę, pakiet, podpis i zgodę systemową.
4. Potwierdź aktualizację oraz zachowanie danych. Powtórz odmowę zgody i anulowanie.

Kryterium zakończenia: lokalna aktualizacja działa, dane zostają zachowane, a plik APK znika po końcowym wyniku instalacji.

## 4. Odbierz automatyczne sprawdzanie i baner (I-40)

1. Włącz automatyczne sprawdzanie i uruchom aplikację ponownie.
2. Potwierdź, że brak sieci nie pokazuje komunikatu oraz że kolejna próba nie następuje przed upływem 24 godzin.
3. Sprawdź „Zobacz” i „Nie teraz” oraz ponowne pokazanie banera dla wyższego `versionCode`.
4. Obejrzyj przełącznik i baner w obu motywach, przy 320 dp i dużej czcionce.

Kryterium zakończenia: automat nie wykonuje nadmiarowych zapytań, błąd pozostaje cichy, a pominięcie dotyczy tylko jednej wersji.

## 5. Bramka publicznego repozytorium i wydania (I-41)

Bloker: jawna zgoda użytkownika po zakończeniu poprawek prezentacyjnych.

1. Sprawdź historię i bieżące pliki pod kątem sekretów oraz prywatnych danych.
2. Potwierdź weryfikację dwuetapową konta i zmień widoczność repozytorium na publiczną.
3. Utwórz tag `v0.2.0`, sprawdź szkic i ręcznie opublikuj wydanie.
4. Zainstaluj `v0.2.0` na telefonie i utwórz dane odbiorowe.

Kryterium zakończenia: publiczne `v0.2.0` jest dostępne, a aplikacja wykrywa je przez produkcyjny adres.
