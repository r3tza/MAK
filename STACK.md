# MAK — stos technologiczny

## 1. Status dokumentu

- Źródło prawdy dla narzędzi i wersji: ten plik.
- Język dokumentacji: polski.
- Język odpowiedzi dla użytkownika: polski.
- Ostatnia zaakceptowana aktualizacja: 2026-09-20.

## 2. Język i środowisko uruchomieniowe

- Kotlin 2.2.10.
- Android, `minSdk` 31, `targetSdk` 36 i `compileSdk` 36.1.
- Android Gradle Plugin 9.1.1 i Gradle 9.7.1.
- Compose BOM 2024.09.00, Material Icons Extended, Room 2.8.5, Lifecycle 2.10.0, Navigation 2.9.5, KSP 2.3.12 i Glance 1.2.0.

## 3. Warstwa aplikacji

- Jetpack Compose do budowy interfejsu.
- Jetpack Glance do widgetu.
- Systemowe powiadomienia Androida; zdarzenia i treść pozostają do ustalenia.
- Lokalny font Inter.

## 4. Dane i przechowywanie

- Room jako warstwa dostępu do danych.
- SQLite jako lokalna baza danych.
- `kotlinx.serialization` do importu i eksportu JSON.
- Brak backendu, kont i synchronizacji sieciowej.

## 5. Testy i jakość

- Testy automatycznego obliczania tygodni A/B, korekt pojedynczych i przyszłych, aktywnego planu oraz kolizji.
- Testy rozdzielenia notatki wspólnej od notatki przypiętej do konkretnej daty.
- Testy odwołania, zmiany, przeniesienia i przywrócenia pojedynczego wystąpienia oraz zajęć jednorazowych.
- Test zgodności wyniku dla widoku listy, kalendarza, ekranu „Dzisiaj” i widgetu.
- Eksport schematu Room od pierwszej wersji i testowanie kolejnych migracji na zachowanych danych.
- Testy interfejsu dla szerokości 320–390 px.
- Sprawdzenie obsługi klawiatury, focusu, etykiet semantycznych, kontrastu, motywu ciemnego, `reduced motion` i dotyku.
- Sprawdzenie braku poziomego przewijania oraz obciętych akcji.
- Weryfikacja, że ekran i widget pokazują ten sam aktywny plan.

## 6. Narzędzia

- Android Studio i Gradle, gdy powstanie projekt aplikacji.
- Git do historii zmian.
- Lokalna baza danych i pliki JSON bez usług zewnętrznych.

## 7. Środowisko

Repozytorium zawiera aplikację Android w wersji 0.1, konfigurację Gradle, lokalną bazę Room i testy. Aplikacja jest przeznaczona do użytku własnego na jednym urządzeniu; nie wymaga infrastruktury do obsługi setek użytkowników.

## 8. Odrzucone alternatywy

- Backend i Firebase — odrzucone, ponieważ aplikacja ma działać całkowicie offline.
- Konta użytkowników i synchronizacja w chmurze — odrzucone, ponieważ zwiększyłyby zakres oraz wymagania dotyczące danych.
- Ciągły serwis w tle i odświeżanie widgetu co minutę — odrzucone z powodu zużycia baterii.
- Zewnętrzne CDN-y — odrzucone; aplikacja używa lokalnych zasobów.
