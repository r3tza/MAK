# MAK: historia wydań

Techniczna historia wydań dla agentów. Notatki dla użytkowników są w `app/src/main/assets/release_notes.json`.

## 0.2.1, 2026-09-28

- Naprawiono wstrzykiwanie zależności `UpdateViewModel`: w 0.2.0 Koin zostawiał atrapy pobierania, weryfikacji i instalacji z wartości domyślnych konstruktora (I-52).
- Kreator pozwala dodać kolejne kierunki ze wspólnym albo osobnym kalendarzem tygodni (I-50).
- Notatki dla użytkowników mają jedno źródło w `release_notes.json`; workflow wypełnia `notes` w `update.json` i opis wydania (I-51).
- Wiersze ustawień mają ramkę focusu, a historia wydań nie łamie daty przy dużej czcionce.

## 0.2.0, 2026-09-27

Pierwsze publiczne wydanie w GitHub Releases.

- Dodano aktualizacje z poziomu aplikacji: ręczne lub automatyczne sprawdzanie, pobieranie, weryfikację i instalację.
- Dodano widget z planem na dziś oraz powiadomienia o kolizjach dzień wcześniej i przed zajęciami.
- Dodano osobne kalendarze tygodni dla kierunków, liczenie okienek i import kopii zapasowej z podglądem.
- Dodano edycję nazw i kolorów kierunków.
- Notatka do terminu zostaje przy terminie po jego przeniesieniu.
- Nowe logo, ekran startowy i uporządkowane ustawienia; lepsza czytelność przy dużej czcionce i obsługa klawiatury.

## 0.1.0, 2026-09-19

- Dodano lokalną bazę Room i odizolowane semestry.
- Dodano kreator konfiguracji, kierunki i formularz zajęć z walidacją godzin.
- Dodano widoki Dzisiaj, Plan i kalendarz miesięczny.
- Dodano tygodnie A/B, ręczne korekty i filtrowanie po kierunku.
- Dodano edycję zajęć, zajęcia jednorazowe oraz zmiany pojedynczych terminów.
- Dodano notatki wspólne i notatki dla wybranego terminu.
- Dodano eksport wszystkich semestrów do pliku JSON z wersją schematu.
