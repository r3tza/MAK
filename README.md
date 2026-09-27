# MAK

MAK, czyli Mój Akademicki Kalendarz, to aplikacja na Androida do zarządzania planem zajęć. Powstała na potrzeby prywatnego użytku i jest rozwijana pod rzeczywisty plan autora oraz małej grupy znajomych. Kod jest publiczny, ale projekt nie jest komercyjną usługą ani produktem z gwarantowanym wsparciem.

## Możliwości

- plan kilku kierunków i semestrów;
- tygodnie A/B oraz własne kalendarze tygodni;
- widok „Dzisiaj”, lista tygodnia, kalendarz i widget;
- wykrywanie kolizji oraz liczenie okienek;
- odwołanie, zmiana lub przeniesienie pojedynczego terminu;
- osobne notatki do zajęć i konkretnego terminu;
- opcjonalne powiadomienia;
- eksport i import kopii zapasowej w JSON;
- aktualizacje podpisanego APK przez GitHub Releases.

Plan i dane użytkownika są przechowywane lokalnie. Podstawowe funkcje działają bez konta i połączenia z siecią. Sieć jest używana wyłącznie do sprawdzania i pobierania aktualizacji, jeżeli użytkownik uruchomi sprawdzenie lub włączy automat.

## Instalacja

Aplikacja wymaga Androida 12 lub nowszego. Nie ma jej w Google Play, więc instaluje się ją z pliku APK.

1. Na telefonie otwórz stronę [najnowszego wydania](https://github.com/r3tza/MAK/releases/latest) i w sekcji „Assets” pobierz plik `MAK-<wersja>.apk`.
2. Otwórz pobrany plik z powiadomienia albo z aplikacji „Pliki”.
3. Jeśli telefon zablokuje instalację, zezwól przeglądarce lub aplikacji „Pliki” na instalowanie nieznanych aplikacji. Android pokaże przejście do tego ustawienia.
4. Jeśli Google Play Protect wyświetli ostrzeżenie o nieznanej aplikacji, wybierz „Więcej szczegółów”, a potem „Zainstaluj mimo to”. Ostrzeżenie pojawia się, bo aplikacji nie ma w sklepie.
5. Po instalacji uruchom MAK i skonfiguruj plan przyciskiem „Skonfiguruj plan”.

Kolejne wersje instaluje się z aplikacji: „Ustawienia”, sekcja „Aktualizacje”, „Sprawdź aktualizacje”. Przy pierwszej aktualizacji Android poprosi o zgodę na instalowanie aplikacji z MAK. Aktualizacja zachowuje plan i notatki. Nie odinstalowuj aplikacji przed aktualizacją, bo usunięcie aplikacji usuwa jej dane. Przed większymi zmianami warto zrobić kopię zapasową: „Ustawienia”, „Kopia zapasowa i import”.

## Budowanie ze źródeł

- JDK 17;
- Android SDK zgodny z konfiguracją projektu.

Wariant debug można zbudować poleceniem:

```powershell
.\gradlew.bat assembleDebug
```

Wariant debug używa pakietu `dev.retza.mak.debug`, więc może działać obok podpisanego wydania `dev.retza.mak` bez usuwania jego danych. Wydania produkcyjne powstają w GitHub Actions z tagów w formacie `v<major>.<minor>.<patch>`.

Dokumentacja produktu, architektury i procesu pracy zaczyna się w [docs/MAP.md](docs/MAP.md).

## Stan projektu

Projekt jest aktywnie rozwijany. Pierwsze publiczne wydanie to 0.2.0; wersje przed 1.0 mogą zawierać błędy, dlatego warto regularnie robić kopię zapasową planu. Lista bieżących prac znajduje się w [docs/QUEUE.md](docs/QUEUE.md), a opis zmian w kolejnych wersjach na stronie [wydań](https://github.com/r3tza/MAK/releases) i w aplikacji, w „O aplikacji”.

## Licencja

Kod jest udostępniony na licencji Apache 2.0. Pełny tekst i informacja o prawach autorskich znajdują się w [LICENSE](LICENSE) oraz [NOTICE](NOTICE). Licencja nie udziela prawa do używania nazwy „MAK” ani ikony aplikacji. Zmodyfikowana dystrybucja musi używać innej nazwy, ikony i identyfikatora pakietu.

Font Inter jest udostępniony na licencji SIL Open Font License 1.1. Jej treść znajduje się w [THIRD_PARTY_LICENSES/Inter-OFL.txt](THIRD_PARTY_LICENSES/Inter-OFL.txt).
