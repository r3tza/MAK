# Znane problemy interfejsu

Audyt czytelności i dostępności wykonany 2026-09-19 na podstawie kodu Compose i `mockup.html`.

## Mocne strony

- Hierarchia ekranu jest spójna: nagłówek, podtytuł, karta podsumowania, sekcje i karty zajęć.
- Ekrany mają stany puste, komunikaty błędów i stan ładowania.
- Plan pokazuje tekstowe statusy, takie jak „Odwołane”, „Zmienione” i „Jednorazowe”. Kolor nie jest jedynym oznaczeniem karty zajęć.
- Kod przewiduje motyw jasny, ciemny i systemowy.
- Mockup ma widoczny focus dla pól i przycisków oraz obsługę `prefers-reduced-motion`. Te zasady trzeba przenieść do Compose.

## Problemy do naprawy

### 1. P1: Zastąpić ręczne wpisywanie dat i godzin pickerami

**Problem:** Użytkownik wpisuje daty w formacie `RRRR-MM-DD` oraz godziny jako zwykły tekst. Dotyczy to kreatora, konfiguracji semestru, korekt tygodni, zajęć jednorazowych i zmian pojedynczego wystąpienia.

**Dowody:**

- `ui/setup/SetupWizard.kt`: pola „Od” i „Do” używają `MakField`.
- `ui/semester/SemesterScreen.kt`: pola zakresu semestru i „Poniedziałek tygodnia” używają `MakField`.
- `ui/edit/ClassEditScreen.kt`: pola godzin i „Data zajęć” używają `MakField`.
- `ui/occurrence/OccurrenceDetailsScreen.kt`: data oraz godziny zmiany wystąpienia używają `MakField`.
- `ui/components/MakComponents.kt`: `MakField` jest oparte na `BasicTextField`, bez typu daty lub czasu.

**Ryzyko:** Format jest podatny na literówki i wymaga pamiętania kolejności roku, miesiąca i dnia. Błąd pojawia się dopiero po zatwierdzeniu formularza.

**Kierunek naprawy:** Używać `DatePickerDialog` dla dat oraz `TimePickerDialog` dla godzin. Ograniczyć wybór daty do zakresu aktywnego semestru, gdy pole tego wymaga. Pokazywać użytkownikowi lokalny, czytelny format, a w stanie zachować `LocalDate` i `LocalTime`.

### 2. P1: Zastąpić wpisywanie koloru paletą

**Problem:** Kolor kierunku jest wpisywany jako kod szesnastkowy.

**Dowody:**

- `ui/setup/SetupWizard.kt`: „Kolor kierunku, opcjonalnie” używa `MakField`.
- `ui/semester/SemesterScreen.kt`: „Kolor kierunku, opcjonalnie” używa `MakField`.
- `ui/MakViewModel.kt`: kolor jest zapisywany jako dowolny tekst, a pusty tekst zastępuje się `#137b71`.
- `ui/components/MakComponents.kt`: parser koloru zwraca `null` dla niepoprawnej wartości, bez komunikatu dla użytkownika.

**Ryzyko:** Użytkownik może zapisać kolor, który nie zadziała albo będzie miał słaby kontrast. Kod nie pokazuje podglądu wybranego koloru.

**Kierunek naprawy:** Pokazać ograniczoną paletę nazwanych kolorów jako wybór jednokrotny. Każda próbka powinna mieć etykietę, stan zaznaczenia i tekstową nazwę dostępną dla czytnika ekranu. Opcjonalny własny kolor można zostawić wyłącznie jako funkcję zaawansowaną z walidacją kontrastu.

### 3. P1: Zapewnić minimalny obszar dotyku 48 dp

**Problem:** Własne kontrolki mają rozmiary mniejsze niż wymagane 48 dp i nie ustawiają jawnego minimalnego obszaru dotyku.

**Dowody:**

- `MakIconButton` ma `size(35.dp)`.
- `MakRoundButton` ma `size(32.dp)`.
- `MakPrimaryAction` ma wysokość `42.dp`.
- `MakSecondaryAction` ma wysokość `40.dp`.
- `MakField` i `MakSelectField` mają minimalną wysokość `40.dp`.
- `MakCheckbox`, filtry kierunków i wiersze wyboru opierają obszar aktywny na małych elementach wizualnych i paddingu bez minimum 48 dp.

**Ryzyko:** Elementy są trudniejsze do trafienia dotykiem i mogą być niewygodne dla osób z ograniczoną precyzją ruchu.

**Kierunek naprawy:** Zachować mniejszy rozmiar wizualny, ale zapewnić co najmniej 48 dp obszaru aktywnego. Dla prostych kontrolek użyć komponentów Material 3. Dla własnych komponentów dodać sprawdzalny focus, semantykę i obsługę klawiatury.

### 4. P1: Powiązać etykiety, wartości i błędy z polami

**Problem:** Etykieta `MakField` jest osobnym tekstem, a właściwe pole to `BasicTextField`. `MakSelectField` działa podobnie. Błędy są renderowane obok lub pod całym wierszem dwóch pól.

**Dowody:**

- `MakField` nie przekazuje etykiety do semantyki pola.
- `MakSelectField` nie opisuje semantycznie etykiety, wartości, stanu rozwinięcia ani błędu.
- W `SetupWizard.kt`, `SemesterScreen.kt` i `ClassEditScreen.kt` błędy dwóch pól w jednym wierszu są wyświetlane dopiero pod wierszem.

**Ryzyko:** Czytnik ekranu może odczytać wartość bez nazwy pola. Użytkownik widzący formularz może nie wiedzieć, czy błąd dotyczy początku, końca, godziny rozpoczęcia czy godziny zakończenia.

**Kierunek naprawy:** Użyć `OutlinedTextField` lub dodać do własnego pola pełną semantykę: etykietę, wartość, `isError` i komunikat błędu. Renderować błąd bezpośrednio przy właściwym polu. Nie polegać wyłącznie na kolorze obramowania.

### 5. P1: Dodać widoczny focus i obsługę klawiatury do własnych kontrolek

**Problem:** Własne przyciski, filtry, checkbox, wybór dnia i pola wyboru używają `clickable` albo `selectable`, ale kod nie definiuje spójnego wizualnego stanu focusu. Mockup ma `focus-visible`, aplikacja Compose nie ma odpowiadającego mechanizmu w tych komponentach.

**Dowody:** `MakIconButton`, `MakRoundButton`, `MakFilterRow`, `MakCheckbox`, `MakNavBar` i `MakSelectField` w `ui/components/MakComponents.kt`.

**Ryzyko:** Użytkownik klawiatury lub przełącznika może nie wiedzieć, który element jest aktywny. Nie ma też gwarancji przewidywalnej kolejności przechodzenia po kontrolkach.

**Kierunek naprawy:** Dodać testowalny stan focusu, logiczną kolejność focusu i akcję klawiatury dla każdego elementu interaktywnego. Sprawdzić dialogi, w tym powrót focusu do elementu otwierającego.

### 6. P2: Usunąć poziomy scroll filtrów na małych ekranach

**Problem:** Filtry kierunków są przewijane poziomo.

**Dowody:**

- `MakFilterRow` używa `horizontalScroll`.
- `mockup.html` używa `overflow:auto` dla `.filter-row`.
- Architektura wymaga braku poziomego przewijania na szerokości 320 dp.

**Ryzyko:** Część filtrów jest ukryta poza ekranem, a użytkownik nie dostaje pełnego obrazu dostępnych opcji.

**Kierunek naprawy:** Użyć rozwijanego wyboru kierunku, arkusza wyboru albo zawijania elementów do kolejnych wierszy. Sprawdzić zachowanie dla długich nazw kierunków.

### 7. P2: Poprawić układ akcji na szerokości 320 dp

**Problem:** Długie akcje są układane obok siebie, a `MakSecondaryAction` obcina tekst do jednej linii.

**Dowody:**

- `SettingsScreen.kt` umieszcza „Konfiguruj semestr” i „Dodaj semestr” w jednym wierszu.
- `OccurrenceDetailsScreen.kt` oraz `SemesterScreen.kt` mają podobne wiersze akcji.
- `MakSecondaryAction` ustawia `maxLines = 1` i `TextOverflow.Ellipsis`.

**Ryzyko:** Na małym ekranie użytkownik zobaczy skróconą nazwę akcji albo będzie musiał zgadywać jej znaczenie.

**Kierunek naprawy:** Układać akcje pionowo, gdy dostępna szerokość jest mała. Nie obcinać nazw akcji, które rozróżniają operacje. Dodać test z szerokością 320 dp i długimi nazwami.

### 8. P2: Zwiększyć kontrast tekstu pomocniczego

**Problem:** Czas zakończenia zajęć używa `MakTimeMuted` `#78849A` na jasnym i ciemnym tle. Kontrast wynosi około 3,77:1, czyli mniej niż 4,5:1 wymagane dla zwykłego tekstu WCAG AA.

**Dowody:**

- `ui/theme/Color.kt`: `MakTimeMuted = Color(0xFF78849A)`.
- `ui/components/MakComponents.kt`: czas zakończenia jest renderowany jako tekst 11 sp.
- Ten sam kolor jest używany także w motywie ciemnym.

**Kierunek naprawy:** Użyć osobnych tokenów dla motywu jasnego i ciemnego. Sprawdzić kontrast wszystkich tekstów 9-13 sp, placeholderów, odznak i tekstu na kolorach kierunków.

### 9. P2: Nie używać samego koloru do informacji w kalendarzu

**Problem:** Kropki w kalendarzu przekazują typ lub zmianę przez kolor, ale nie mają własnej semantyki. `CalendarMarkerUi.contentDescription` istnieje w modelu, lecz `CalendarDay` przekazuje do `MakDot` tylko kolor.

**Ryzyko:** Użytkownik czytnika ekranu nie dowie się, co oznaczają kropki. Użytkownik z zaburzeniami widzenia barw nie rozróżni typów oznaczeń.

**Kierunek naprawy:** Umieścić opis markerów w opisie dnia, dodać tekstową legendę albo zastąpić część informacji tekstowymi odznakami. Zachować liczbę zajęć i statusy w opisie dostępności dnia.

### 10. P2: Oznaczyć operacje destrukcyjne jednoznacznie

**Problem:** Potwierdzenie usunięcia semestru i zajęć używa `MakPrimaryAction`, czyli tego samego wizualnego stylu co zwykłe zapisanie formularza.

**Dowody:**

- `SettingsScreen.kt`: przycisk „Usuń” w dialogu jest `MakPrimaryAction`.
- `OccurrenceDetailsScreen.kt`: przycisk „Usuń zajęcia” w dialogu jest `MakPrimaryAction`.
- `MakSecondaryAction` ma obsługę `destructive`, ale `MakPrimaryAction` jej nie ma.

**Ryzyko:** Użytkownik może pomylić akcję nieodwracalną z głównym zatwierdzeniem.

**Kierunek naprawy:** Użyć dedykowanego stylu destrukcyjnego, zachować wyraźne „Anuluj” i „Usuń”, a operację destrukcyjną umieścić jako ostatnią i jednoznacznie opisaną.

## Kolejność prac

1. Pickery dat i godzin oraz paleta kolorów.
2. Semantyka pól, błędów, focusu i obszary dotyku.
3. Układ 320 dp i usunięcie poziomego scrollowania.
4. Kontrast, markery kalendarza i akcje destrukcyjne.
