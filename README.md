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

## Wymagania i uruchomienie

- Android 12 lub nowszy, API 31;
- JDK 17;
- Android SDK zgodny z konfiguracją projektu.

Wariant debug można zbudować poleceniem:

```powershell
.\gradlew.bat assembleDebug
```

Wariant debug używa pakietu `dev.retza.mak.debug`, więc może działać obok podpisanego wydania `dev.retza.mak` bez usuwania jego danych. Wydania produkcyjne powstają w GitHub Actions z tagów w formacie `v<major>.<minor>.<patch>`.

Dokumentacja produktu, architektury i procesu pracy zaczyna się w [docs/MAP.md](docs/MAP.md).

## Stan projektu

Projekt jest aktywnie rozwijany. Przed pierwszym publicznym wydaniem trwają odbiory na urządzeniu i poprawki prezentacyjne. Lista bieżących prac znajduje się w [docs/QUEUE.md](docs/QUEUE.md), a zmiany wydane użytkownikom w [docs/CHANGELOG.md](docs/CHANGELOG.md).

## Licencja

Kod jest udostępniony na licencji Apache 2.0. Pełny tekst i informacja o prawach autorskich znajdują się w [LICENSE](LICENSE) oraz [NOTICE](NOTICE). Licencja nie udziela prawa do używania nazwy „MAK” ani ikony aplikacji. Zmodyfikowana dystrybucja musi używać innej nazwy, ikony i identyfikatora pakietu.

Font Inter jest udostępniony na licencji SIL Open Font License 1.1. Jej treść znajduje się w [THIRD_PARTY_LICENSES/Inter-OFL.txt](THIRD_PARTY_LICENSES/Inter-OFL.txt).
